#!/usr/bin/env python3

# Copyright 2023 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import argparse
import dataclasses
from pathlib import Path
import random
import readline
import re
import socket
import sys
from textwrap import dedent
import traceback
from typing import List, Optional

log_matcher = re.compile(r"""
(?<=[ ])                    # positive look-behind for space character
log::
(?P<level>                  # start of level
verbose| debug| info| warn| error| fatal
)                           # end of level
\(
(?P<fmt>                    # start of fmt
(
\s*
\"
((?:[^\"\\]|\\.)*)
\"
)+
)                           # end of fmt
,?
(?P<args>                   # start of args
(.|\n)*?
)                           # end of args
\)
(?=;)
""", flags=re.X)


to_string_matcher = re.compile(r"""
(::)?
(std::)?
to_string
\(
((.|\n)*)
\)
""", flags=re.X)


stringprintf_matcher = re.compile(r"""
(::)?
(android::)?
(base::)?
StringPrintf
\(
\s*
(?P<format>                 # start of format
(
\s*
\"
((?:[^\"\\]|\\.)*)
\"
)+
)                           # end of format
,?
(?P<args>                   # start of args
((.|\n)*)
)                           # end of args
\)
""", flags=re.X)


format_matcher = re.compile("""\
(                                  # start of capture group 1
%                                  # literal "%"
(?:                                # first option
(?:[-+0 #]{0,5})                   # optional flags
(?:\d+|\*)?                        # width
(?:\.(?:\d+|\*))?                  # precision
(?:z|h|hh|l|ll|w|I|I32|I64)?       # size
[cCdiouxXeEfgGaAnpsSZ]             # type
) |                                # OR
%%)                                # literal "%%"
""", flags=re.X)


format_specifiers = {
    '%c': '{:c}',
    '%s': '{}',
    '%-12s': '{:<12s}',
    '%-22s': '{:<22s}',
    '%.32s': '{:32s}',
    '%zu': '{}',
    '%hd': '{}',
    '%hhd': '{}',
    '%02d': '{:02}',
    '%04d': '{:04}',
    '%d': '{}',
    '%i': '{}',
    '%ld': '{}',
    '%09ld': '{:09}',
    '%lld': '{}',
    '%hu': '{}',
    '%hhu': '{}',
    '%u': '{}',
    '%lu': '{}',
    '%llu': '{}',
    '%hx': '{:x}',
    '%#hx': '{:#x}',
    '%0hx': '{:0x}',
    '%02hx': '{:02x}',
    '%02hhx': '{:02x}',
    '%08hx': '{:08x}',
    '%x': '{:x}',
    '%X': '{:X}',
    '%#x': '{:#x}',
    '%#0x': '{:#0x}',
    '%#02x': '{:#02x}',
    '%0x': '{:0x}',
    '%2x': '{:2x}',
    '%2X': '{:2X}',
    '%02x': '{:02x}',
    '%02X': '{:02X}',
    '%03x': '{:03x}',
    '%4x': '{:4x}',
    '%04x': '{:04x}',
    '%04X': '{:04X}',
    '%4.4x': '{:4.4x}',
    '%06x': '{:06x}',
    '%08x': '{:08x}',
    '%08X': '{:08X}',
    '%lx': '{:x}',
    '%04lx': '{:04x}',
    '%16.16llx': '{:16.16x}',
    '%f': '{:f}',
    '%lf': '{:f}',
    '%.1f': '{:.1f}',
    '%.2f': '{:.2f}',
    '%.02f': '{:.02f}',
}


@dataclasses.dataclass
class Replacement:
    file: Path
    start: int
    end: int
    original_text: str
    replacement_text: str

    @property
    def clang_diagnostic(self):
        """Generate the diff as a yaml item compatible with
        clang-apply-replacements"""
        replacement_text = (self.replacement_text
            .replace("'", "''")
            .replace('\n', '\n          '))
        return dedent(
        r"""
          - DiagnosticName:  clang-diagnostic-error
            DiagnosticMessage:
              Message: 'convert log to fmtlib'
              FilePath: '{file}'
              FileOffset: {offset}
              Replacements:
                - FilePath: '{file}'
                  Offset: {offset}
                  Length: {length}
                  ReplacementText: '{text}'
            Level: Warning
            BuildDirectory: '{build}'""".format(file=self.file.resolve(), offset=self.start,
                   length=self.end - self.start,
                   text=replacement_text,
                   build=Path.cwd().resolve())[1:])


def unsplit_fmt_string(fmt: str) -> str:
    """Unsplit the printf format string and strip the quotes.

    e.g unsplit_printf_format('a " " b"') -> 'a  b'
    """
    matcher = re.compile('\\s*"(((?<=\\\\)"|[^"])*)"')
    start_pos = 0
    unsplit_fmt = ""
    while match := matcher.match(fmt, start_pos):
        unsplit_fmt += match.group(1)
        start_pos = match.end() + 1
    return unsplit_fmt


def split_fmt_args(args: str) -> List[str]:
    """Split the string representing the input args while preserving well
    parenthesized arguments.

    e.g. split_printf_args("'a(b, c), b, c") -> ["a(b, c)", "b", "c"]
    """
    split_args = []
    start_index = 0
    nesting = 0
    for index in range(0, len(args)):
        match args[index]:
            case '(':
                nesting += 1
            case ')':
                nesting -= 1
            case ',':
                if nesting == 0:
                    split_args.append(args[start_index:index])
                    start_index = index + 1
            case _:
                pass

    last = args[start_index:]
    if last.strip():
        split_args.append(args[start_index:])

    return [arg.strip() for arg in split_args]


def sanitize_fmt_string(fmt: str) -> str:
    # Raise a warning if the __func__ formatter is not at the beginning of the
    # format string: manual intervention is probably required.
    if re.search('(?<!^)XXFUNCXX', fmt):
        print("WARN: __func__ found in the middle of the format string", file=sys.stderr)

    # Raise a warning if the __func__ formatter is not at the beginning of the
    # format string: manual intervention is probably required.
    if re.search('\n(?!$)', fmt):
        print("WARN: \\n found in the middle of the format string", file=sys.stderr)

    # Remove __func__ placeholders.
    fmt = re.sub(r'XXFUNCXX(\(\))? ?[:,-]? ?', '', fmt)

    # Strip ' ' at the beginning of the format string, and '\n' at the end.
    fmt = re.sub(r'^ +', '', fmt)
    fmt = re.sub(r'\n+$', '', fmt)
    fmt = re.sub(r' +$', '', fmt)

    return fmt


def sanitize_fmt_arg(arg: str) -> str:
    # Remove 'ADDRESS_TO_LOGGABLE_STR'.
    if match := re.fullmatch(r"ADDRESS_TO_LOGGABLE_(STR|CSTR)\(((.|\n)*?)\)(\.c_str\(\))?", arg):
        return match.group(2)

    # Remove '.c_str()' suffix.
    arg = re.sub(r'\.c_str\(\)$', '', arg)

    # Remove '+' prefix.
    arg = re.sub(r'^\+', '', arg)

    return arg


def log_converter(match: re.Match) -> Optional[str]:
    fmt_string = unsplit_fmt_string(match.group('fmt'))
    fmt_args = split_fmt_args(match.group('args'))

    new_fmt_string = sanitize_fmt_string(fmt_string)
    new_fmt_args = [sanitize_fmt_arg(arg) for arg in fmt_args]

    if new_fmt_string == fmt_string and new_fmt_args == fmt_args:
        return None

    level = match.group('level')
    new_fmt_args = ''.join(", " + arg for arg in new_fmt_args)

    return f"log::{level}(\"{new_fmt_string}\"{new_fmt_args})"


def line_and_column(source: str, offset: int) -> (int, int):
    line = 1
    column = 0
    for n in range(0, offset):
        if source[n] == '\n':
            line += 1
            column = 0
        else:
            column += 1
    return (line, column)


def codepoint_to_byte_offset(source: str) -> List[int]:
    """Return a mapping from the char offset to the byte offset in the source."""
    TWOBYTES = 0x80
    THREEBYTES = 0x800
    FOURBYTES = 0x10000

    mapping = []
    byte_offset = 0
    for character in source:
        mapping.append(byte_offset)
        codepoint = ord(character)
        byte_offset += 1
        for cue in (TWOBYTES, THREEBYTES, FOURBYTES):
            if codepoint >= cue:
                byte_offset += 1
            else:
                break
    return mapping


def convert(path: Path, source: str, matcher: re.Pattern, converter) -> List[Replacement]:
    start_pos = 0
    replacements = []
    byte_offset = codepoint_to_byte_offset(source)
    while match := matcher.search(source, start_pos):
        start_pos = match.end() + 1
        try:
            if replacement_text := converter(match):
                replacements.append(Replacement(
                    file=path,
                    start=byte_offset[match.start()],
                    end=byte_offset[match.end()],
                    original_text=match.group(),
                    replacement_text=replacement_text))
        except Exception as exn:
            line, column = line_and_column(source, match.start())
            print(f"Exception raised at line {line}:{column} in file {path}", file=sys.stderr)
            traceback.print_exc()

    return replacements


def run(files: List[Path]):
    print("---")
    print(f"MainSourceFile: ''")
    print(f"Diagnostics:")
    total_replacements = 0
    total_files = 0
    for file in files:
        with open(file, 'r', encoding="utf-8") as f:
            print(f"reading {file}", file=sys.stderr)
            source = f.read()

        replacements = convert(file, source, log_matcher, log_converter)
        total_replacements += len(replacements)
        total_files += 1 if len(replacements) > 0 else 0
        for r in replacements:
            print(r.clang_diagnostic)
    print("...")
    print(f"..generated {total_replacements} replacement(s) in {total_files} file(s)",
          file=sys.stderr)


def main():
    """Generate clang replacements for converting logs to fmtlib format.
    Apply the replacements with clang-apply-replacements ."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('files',
                        metavar='FILE',
                        nargs='+',
                        type=Path,
                        help='File to modify')
    run(**vars(parser.parse_args()))


if __name__ == '__main__':
    main()
