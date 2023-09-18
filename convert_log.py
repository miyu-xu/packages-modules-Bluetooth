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

# ostream format
vlog_matcher = re.compile(r"(?<= )?VLOG\(([0-9])\)(( |\n)*?<<(.|\n)*?);")
vlog_levels = {
    '0': 'VERBOSE',
    '1': 'VERBOSE',
    '2': 'VERBOSE',
    '3': 'VERBOSE',
    '4': 'VERBOSE',
    '5': 'VERBOSE',
}

# printf format
alog_matcher = re.compile(r"(?<= )ALOG(.*?)\(((.|\n)*?)\);")
alog_levels = {
    'V': 'VERBOSE',
    'D': 'DEBUG',
    'I': 'INFO',
    'W': 'WARNING',
    'E': 'ERROR',
}

# ostream format
log_matcher = re.compile(r"(?<= )LOG\((.*?)\)(( |\n)*?<<(.|\n)*?);")
log_levels = {
    'VERBOSE': 'VERBOSE',
    'DEBUG': 'DEBUG',
    'INFO': 'INFO',
    'WARNING': 'WARNING',
    'ERROR': 'ERROR',
    'FATAL': 'FATAL',
}

to_string_matcher = re.compile(r"(::)?(std::)?to_string\(((.|\n)*?)\)")
stringprintf_matcher = re.compile(r"(::)?(android::)?(base::)?StringPrintf\(\"((?:[^\"\\]|\\.)*)\", (.*?)\)")


printf_matcher=re.compile("""\
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

printf_formatters = {
    '%#hx': '{:#x}',
    '%#x': '{:#x}',
    '%x': '{:x}',
    '%s': '{}',
    '%d': '{}',
    '%02x': '{:02x}',
    '%04x': '{:04x}',
    '%09ld': '{:09}',
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
        replacement_text = self.replacement_text.replace("'", "''")
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


def convert_printf(fmt: str) -> str:
    format_string = ""
    start_pos = 0

    for formatter in printf_matcher.finditer(fmt):
        format_string += fmt[start_pos:formatter.start()]
        start_pos = formatter.end()

        if formatter.group(0) == '%%':
            format_string += "%"

        elif formatter.group(0) not in printf_formatters:
            raise Exception(f"Unknown printf formatter {formatter.group(0)}")

        else:
            format_string += printf_formatters[formatter.group(0)]

    format_string += fmt[start_pos:]
    return format_string


def convert_ostream(logger: str, params: str) -> str:
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
            format_string += convert_printf(match.group(4))
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

    return f"{logger}(\"{format_string}\"{format_args});"


def vlog_converter(match: re.Match) -> str:
    level = match.group(1)
    if level not in vlog_levels:
        raise Exception(f"unknown VLOG() level {level} ({match.start()} - {match.end()})")

    return convert_ostream(vlog_levels[level], match.group(2))


def log_converter(match: re.Match) -> str:
    level = match.group(1)
    if level not in log_levels:
        raise Exception(f"unknown LOG() level {level} ({match.start()} - {match.end()})")

    return convert_ostream(log_levels[level], match.group(2))


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
        replacements.extend(convert(file, source, vlog_matcher, vlog_converter))
        replacements.extend(convert(file, source, log_matcher, log_converter))

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
