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
from typing import List

# Manual exceptions
# - DEVICE_VLOG in system/profile/avrcp/device.cc
# - DEVICE_LOG in system/profile/avrcp/device.cc

ostream_matcher = re.compile(r"""
(?<=[ ])                    # positive look-behind for space character
(                           # start of group 1
VLOG\([0-9]\) |
DEVICE_VLOG\([0-9]\) |
LOG\((FATAL|ERROR|WARNING|INFO|DEBUG)\)
)                           # end of group 1
(                           # start of group 3
[ \n]*?
<<
(.|\n)*?
)                           # end of group 3
(?=;)
""", flags=re.X)


ostream_levels = {
    'VLOG(0)': 'log::verbose',
    'VLOG(1)': 'log::verbose',
    'VLOG(2)': 'log::verbose',
    'VLOG(3)': 'log::verbose',
    'VLOG(4)': 'log::verbose',
    'VLOG(5)': 'log::verbose',
    'DEVICE_VLOG(0)': 'log::verbose',
    'DEVICE_VLOG(1)': 'log::verbose',
    'DEVICE_VLOG(2)': 'log::verbose',
    'DEVICE_VLOG(3)': 'log::verbose',
    'DEVICE_VLOG(4)': 'log::verbose',
    'LOG(VERBOSE)': 'log::verbose',
    'LOG(DEBUG)': 'log::debug',
    'LOG(INFO)': 'log::info',
    'LOG(WARNING)': 'log::warn',
    'LOG(ERROR)': 'log::error',
    'LOG(FATAL)': 'log::fatal',
}


printf_matcher = re.compile(r"""
(?<=[ ])                    # positive look-behind for space character
(                           # start of group 1
ALOG[A-Z] |
LOG_(FATAL|ALWAYS_FATAL|ERROR|WARN|INFO|DEBUG|VERBOSE)
)                           # end of group 1
\(
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
(.|\n)*?
)                           # end of args
\)
(?=;)
""", flags=re.X)


printf_levels = {
    'LOG_VERBOSE': 'log::verbose',
    'LOG_DEBUG': 'log::debug',
    'LOG_INFO': 'log::info',
    'LOG_WARN': 'log::warn',
    'LOG_ERROR': 'log::error',
    'LOG_FATAL': 'log::fatal',
    'LOG_ALWAYS_FATAL': 'log::fatal',
    'ALOGV': 'log::verbose',
    'ALOGD': 'log::debug',
    'ALOGI': 'log::info',
    'ALOGW': 'log::warn',
    'ALOGE': 'log::error',
}


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
\"
((?:[^\"\\]|\\.)*)
\"
,
((.|\n)*)
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
    '%02d': '{:02d}',
    '%04d': '{:04d}',
    '%d': '{}',
    '%i': '{}',
    '%ld': '{}',
    '%09ld': '{:09}',
    '%hu': '{}',
    '%hhu': '{}',
    '%u': '{}',
    '%lu': '{}',
    '%llu': '{}',
    '%hx': '{:x}',
    '%#hx': '{:#x}',
    '%0hx': '{:0x}',
    '%02hx': '{:02x}',
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


def strip_func_formatter(fmt: str) -> str:
    return re.sub(r'XXFUNCXX(\(\))?[:,]? ?', '', fmt)


def unsplit_printf_fmt(fmt: str) -> str:
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


def split_printf_args(args: str) -> List[str]:
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


def convert_format(fmt: str, args: str) -> (str, List[str]):
    fmt = unsplit_printf_fmt(fmt)
    args = iter(split_printf_args(args))

    format_string = ""
    format_args = []
    start_pos = 0

    for specifier in format_matcher.finditer(fmt):
        format_string += fmt[start_pos:specifier.start()]
        start_pos = specifier.end()

        if specifier.group(0) == '%%':
            format_string += "%"

        elif specifier.group(0) == '%p':
            format_string += "{}"
            arg = next(args)
            format_args.append(f"fmt::ptr({arg})")

        elif specifier.group(0) == '%s':
            # Remove __func__ from logs.
            # The pattern is not removed immediately but a placeholder is
            # left in for later cleanup.
            arg = next(args)
            if (arg == '__func__' or
                arg == '__FUNCTION__' or
                arg == '__PRETTY_FUNCTION__'):
                format_string += "XXFUNCXX"
            else:
                format_string += "{}"
                format_args.append(arg)

        elif specifier.group(0) == '%.*s':
            # The order of the precision and string parameters is reversed
            # with fmtlib formats: the string comes first.
            format_string += "{:.{}}"
            precision = next(args)
            format_args.append(next(args))
            format_args.append(precision)

        elif specifier.group(0) not in format_specifiers:
            raise Exception(f"Unknown format specifier {specifier.group(0)}")

        else:
            format_string += format_specifiers[specifier.group(0)]
            format_args.append(next(args))

    # Having leftover arguments could indicate that a formatter was missed,
    # or some other failure case.
    leftover_args = [a for a in args]
    if len(leftover_args) > 0:
        raise Exception(f"remaining argument after format string processing: {leftover_args}")

    format_string += fmt[start_pos:]
    format_string = strip_func_formatter(format_string)
    return (format_string, format_args)


def ostream_converter(match: re.Match) -> str:
    level = match.group(1)
    if level not in ostream_levels:
        raise Exception(f"unknown level {level} ({match.start()} - {match.end()})")

    params = match.group(3)
    format_string = ""
    format_args = []
    hex_format = False
    parts = params.split('<<')
    parts = [part.strip() for part in parts if part.strip()]

    for part in parts:
        # TODO: raw strings may be split on two lines
        # this is still valid but formatting will look weird
        if part[0] == '"' and part[-1] == '"':
            text = part[1:-1]
            text.replace('{', '{{')
            text.replace('}', '}}')
            format_string += text

        elif part == 'std::hex':
            hex_format = True

        elif part == 'std::endl':
            pass

        elif (part == '__func__' or
              part == '__FUNCTION__' or
              part == '__PRETTY_FUNCTION__'):
            format_string += "XXFUNCXX"

        elif match := to_string_matcher.match(part):
            format_string += "{}"
            format_args.append(match.group(3))

        elif match := stringprintf_matcher.match(part):
            (fmt, args) = convert_format(match.group(4), match.group(5))
            format_string += fmt
            format_args.extend(args)

        elif hex_format:
            format_string += "{:x}"
            format_args.append(part)
            hex_format = False

        elif (part.startswith("std::get") or
              part.startswith("std::bitset")):
            format_string += "{}"
            format_args.append(part)

        elif part.startswith("std::"):
            raise Exception(f"uncaught format specifier {part}")

        else:
            # TODO: strip unnecessary parens around format arg.
            format_string += "{}"
            format_args.append(part)

    format_string = strip_func_formatter(format_string)
    format_args = ''.join(", " + arg for arg in format_args)
    return f"{ostream_levels[level]}(\"{format_string}\"{format_args})"


def printf_converter(match: re.Match) -> str:
    level = match.group(1)
    if level not in printf_levels:
        raise Exception(f"unknown level {level} ({match.start()} - {match.end()})")

    (format_string, format_args) = convert_format(match.group('format'), match.group('args'))

    format_string = strip_func_formatter(format_string)
    format_args = ''.join(", " + arg for arg in format_args)
    return f"{printf_levels[level]}(\"{format_string}\"{format_args})"


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
            replacements.append(Replacement(
                file=path,
                start=byte_offset[match.start()],
                end=byte_offset[match.end()],
                original_text=match.group(),
                replacement_text=converter(match)))
        except Exception as exn:
            line, column = line_and_column(source, match.start())
            print(f"Exception raised at line {line}:{column} in file {path}", file=sys.stderr)
            traceback.print_exc()

    return replacements


def run(files: List[Path]):
    print("---")
    print(f"MainSourceFile: ''")
    print(f"Diagnostics:")
    for file in files:
        with open(file, 'r', encoding="utf-8") as f:
            print(f"reading {file}", file=sys.stderr)
            source = f.read()

        replacements = []
        replacements.extend(convert(file, source, ostream_matcher, ostream_converter))
        replacements.extend(convert(file, source, printf_matcher, printf_converter))

        for r in replacements:
            print(r.clang_diagnostic)
    print("...")

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
