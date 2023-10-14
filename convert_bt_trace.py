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

# post-migration cleanup:
# - LogMsg()
# - profile_log_levels.cc
# - bt_trace.h
#   - appl_trace_level
#   - btm_cb.trace_level
#   - l2cb.l2cap_trace_level
#   - sdp_cb.trace_level
#   - rfc_cb.trace_level
#   - hh_cb.trace_level
#   - hd_cb.trace_level
#   - bnep_cb.trace_level
#   - pan_cb.trace_level
#   - a2dp_cb.trace_level
#   - avdtp_cb.TraceLevel()
#   - avct_cb.trace_level
#   - avrc_cb.trace_level
#   - smp_cb.trace_level
#   - btif_trace_level
#!/usr/bin/env python3

# bt_trace logger
bt_trace_matcher = re.compile(r"(?<= )?([A-Z]+)_TRACE_(ERROR|WARNING|API|EVENT|DEBUG|VERBOSE|NONE)")
bt_trace_levels = {
    'ERROR': 'ERROR',
    'WARNING': 'WARN',
    'API': 'INFO',
    'EVENT': 'INFO',
    'DEBUG': 'DEBUG',
    'VERBOSE': 'VERBOSE',
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


def bt_trace_converter(match: re.Match) -> str:
    level = match.group(2)
    if level not in bt_trace_levels:
        raise Exception(f"unknown BT_TRACE() level {level} ({match.start()} - {match.end()})")

    return f"LOG_{bt_trace_levels[level]}"


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
        replacements.extend(convert(file, source, bt_trace_matcher, bt_trace_converter))

        for r in replacements:
            print(r.clang_diagnostic)
    print("...")

def main():
    """Generate clang replacements for converting logs to fmtlib format.
    Apply the replacements with clang-apply-replacements.

    Tip: use fdfind to list files to convert: fdfind --regex ".*\\.(c|cc|cpp)$"
    """
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('files',
                        metavar='FILE',
                        nargs='+',
                        type=Path,
                        help='File to modify')
    run(**vars(parser.parse_args()))


if __name__ == '__main__':
    main()
