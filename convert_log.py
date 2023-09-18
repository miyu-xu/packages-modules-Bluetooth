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
    'VLOG(0)': 'VERBOSE',
    'VLOG(1)': 'VERBOSE',
    'VLOG(2)': 'VERBOSE',
    'VLOG(3)': 'VERBOSE',
    'VLOG(4)': 'VERBOSE',
    'VLOG(5)': 'VERBOSE',
    'DEVICE_VLOG(0)': 'VERBOSE',
    'DEVICE_VLOG(1)': 'VERBOSE',
    'DEVICE_VLOG(2)': 'VERBOSE',
    'DEVICE_VLOG(3)': 'VERBOSE',
    'DEVICE_VLOG(4)': 'VERBOSE',
    'LOG(VERBOSE)': 'VERBOSE',
    'LOG(DEBUG)': 'DEBUG',
    'LOG(INFO)': 'INFO',
    'LOG(WARNING)': 'WARNING',
    'LOG(ERROR)': 'ERROR',
    'LOG(FATAL)': 'FATAL',
}


printf_matcher = re.compile(r"""
(?<=[ ])                    # positive look-behind for space character
(                           # start of group 1
ALOG[A-Z] |
LOG_(FATAL|ERROR|WARN|INFO|DEBUG|VERBOSE)
)                           # end of group 1
\(
\s*
\"
((?:[^\"\\]|\\.)*)
\"
((.|\n)*?)
\)
(?=;)
""", flags=re.X)


printf_levels = {
    'LOG_VERBOSE': 'VERBOSE',
    'LOG_DEBUG': 'DEBUG',
    'LOG_INFO': 'INFO',
    'LOG_WARN': 'WARNING',
    'LOG_ERROR': 'ERROR',
    'LOG_FATAL': 'FATAL',
    'ALOGV': 'VERBOSE',
    'ALOGD': 'DEBUG',
    'ALOGI': 'INFO',
    'ALOGW': 'WARNING',
    'ALOGE': 'ERROR',
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
(?:h|l|ll|w|I|I32|I64)?            # size
[cCdiouxXeEfgGaAnpsSZ]             # type
) |                                # OR
%%)                                # literal "%%"
""", flags=re.X)


format_specifiers = {
    '%c': '{:c}',
    '%s': '{}',
    '%-22s': '{:<22s}',
    '%hd': '{}',
    '%d': '{}',
    '%i': '{}',
    '%09ld': '{:09}',
    '%hu': '{}',
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
    '%0x': '{:0x}',
    '%2X': '{:2X}',
    '%02x': '{:02x}',
    '%02X': '{:02X}',
    '%4x': '{:4x}',
    '%04x': '{:04x}',
    '%04X': '{:04X}',
    '%4.4x': '{:4.4x}',
    '%06x': '{:06x}',
    '%08x': '{:08x}',
    '%lx': '{:x}',
    '%04lx': '{:04x}',
    '%16.16llx': '{:16.16x}',
    '%f': '{:f}',
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


def convert_format(fmt: str) -> str:
    format_string = ""
    start_pos = 0

    for specifier in format_matcher.finditer(fmt):
        format_string += fmt[start_pos:specifier.start()]
        start_pos = specifier.end()

        if specifier.group(0) == '%%':
            format_string += "%"

        elif specifier.group(0) == '%p':
            format_string += "{}"
            # TODO wrap arg in fmt::ptr

        elif specifier.group(0) not in format_specifiers:
            raise Exception(f"Unknown format specifier {specifier.group(0)}")

        else:
            format_string += format_specifiers[specifier.group(0)]

    format_string += fmt[start_pos:]
    return format_string


def ostream_converter(match: re.Match) -> str:
    level = match.group(1)
    if level not in ostream_levels:
        raise Exception(f"unknown level {level} ({match.start()} - {match.end()})")

    params = match.group(3)
    format_string = ""
    format_args = ""
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

        elif match := to_string_matcher.match(part):
            format_string += "{}"
            format_args += ", " + match.group(3)

        elif match := stringprintf_matcher.match(part):
            format_string += convert_format(match.group(4))
            format_args += ", " + match.group(5)

        elif hex_format:
            format_string += "{:x}"
            format_args += ", " + part
            hex_format = False

        elif (part.startswith("std::get") or
              part.startswith("std::bitset")):
            format_string += "{}"
            format_args += ", " + part

        elif part.startswith("std::"):
            raise Exception(f"uncaught format specifier {part}")

        else:
            # TODO: strip unnecessary parens around format arg.
            format_string += "{}"
            format_args += ", " + part

    return f"{ostream_levels[level]}(\"{format_string}\"{format_args})"


def printf_converter(match: re.Match) -> str:
    level = match.group(1)
    if level not in printf_levels:
        raise Exception(f"unknown level {level} ({match.start()} - {match.end()})")

    format_string = convert_format(match.group(3))
    format_args = match.group(4)

    return f"{printf_levels[level]}(\"{format_string}\"{format_args})"


def convert(path: Path, source: str, matcher: re.Pattern, converter) -> List[Replacement]:
    start_pos = 0
    replacements = []
    while True:
        match = matcher.search(source, start_pos)
        if not match:
            break
        start_pos = match.end() + 1
        try:
            replacements.append(Replacement(
                file=path,
                start=match.start(),
                end=match.end(),
                original_text=match.group(),
                replacement_text=converter(match)))
        except Exception as exn:
            print(f"Exception raised at offset {match.start()} in file {path}", file=sys.stderr)
            traceback.print_exc()

    return replacements


def run(files: List[Path]):
    print("---")
    print(f"MainSourceFile: ''")
    print(f"Diagnostics:")
    for file in files:
        with open(file, 'r') as f:
            source = f.read()

        replacements = []
        #replacements.extend(convert(file, source, ostream_matcher, ostream_converter))
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
