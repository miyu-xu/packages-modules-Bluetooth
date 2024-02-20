#!/usr/bin/env python2
# Copyright 2024 The Chromium OS Authors. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.
"""This module provides audio test data."""

import os

class AudioTestDataException(Exception):
    """Exception for audio test data."""
    pass

class AudioTestData(object):
    """Class to represent audio test data."""

    def __init__(self,
                 data_format=None,
                 path=None,
                 frequencies=None,
                 duration_secs=None):
        """Initializes an audio test file.

        Args:
            data_format: A dict containing data format including
                         file_type, sample_format, channel, and rate.
                         file_type: file type e.g. 'raw' or 'wav'.
                         sample_format: One of the keys in audio_data.SAMPLE_FORMAT.
                         channel: number of channels.
                         rate: sampling rate.
            path: The path to the file.
            frequencies: A list containing the frequency of each channel in
                         this file. Only applicable to data of sine tone.
            duration_secs: Duration of test file in seconds.

        Raises:
            AudioTestDataException if the path does not exist.
        """
        self.data_format = data_format
        if not os.path.exists(path):
            raise AudioTestDataException('Can not find path %s' % path)
        self.path = path
        self.frequencies = frequencies
        self.duration_secs = duration_secs
