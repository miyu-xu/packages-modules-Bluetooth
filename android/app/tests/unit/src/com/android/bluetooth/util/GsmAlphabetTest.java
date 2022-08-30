/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.bluetooth.util;

import static org.junit.Assert.assertThat;
import static com.google.common.truth.Truth.assertThat;

import com.android.internal.telephony.uicc.IccUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class GsmAlphabetTest {

  private static final String GSM_EXTENDED_CHARS = "{|}\\[~]\f\u20ac";

  @Test
  public void gsm7BitPackedToString() throws Exception {
    byte[] packed;
    StringBuilder testString = new StringBuilder(300);

    packed = com.android.internal.telephony.GsmAlphabet.stringToGsm7BitPacked(
            testString.toString());
    assertThat(testString.toString()).isEqualTo(
            GsmAlphabet.gsm7BitPackedToString(packed, 1, 0xff & packed[0], 0, 0, 0));

    // Check all alignment cases
    for (int i = 0; i < 9; i++, testString.append('@')) {
      packed = com.android.internal.telephony.GsmAlphabet.stringToGsm7BitPacked(
              testString.toString());
      assertThat(testString.toString()).isEqualTo(
              GsmAlphabet.gsm7BitPackedToString(packed, 1, 0xff & packed[0], 0, 0, 0));
    }

    // Test extended chars too
    testString.append(GSM_EXTENDED_CHARS);
    packed = com.android.internal.telephony.GsmAlphabet.stringToGsm7BitPacked(
            testString.toString());
    assertThat(testString.toString()).isEqualTo(
            GsmAlphabet.gsm7BitPackedToString(packed, 1, 0xff & packed[0], 0, 0, 0));

    // Try 254 septets with 127 extended chars
    testString.setLength(0);
    for (int i = 0; i < (255 / 2); i++) {
      testString.append('{');
    }
    packed = com.android.internal.telephony.GsmAlphabet.stringToGsm7BitPacked(
            testString.toString());
    assertThat(testString.toString()).isEqualTo(
            GsmAlphabet.gsm7BitPackedToString(packed, 1, 0xff & packed[0], 0, 0, 0));

    // Reserved for extension to extension table (mapped to space)
    packed = new byte[]{(byte)(0x1b | 0x80), 0x1b >> 1};
    assertThat(" ").isEqualTo(GsmAlphabet.gsm7BitPackedToString(packed, 0, 2, 0, 0, 0));

    // Unmappable (mapped to character in default alphabet table)
    packed[0] = 0x1b;
    packed[1] = 0x00;
    assertThat("@").isEqualTo(GsmAlphabet.gsm7BitPackedToString(packed, 0, 2, 0, 0, 0));
    packed[0] = (byte)(0x1b | 0x80);
    packed[1] = (byte)(0x7f >> 1);
    assertThat("\u00e0").isEqualTo(GsmAlphabet.gsm7BitPackedToString(packed, 0, 2, 0, 0, 0));
  }

  @Test
  public void stringToGsm8BitPacked() throws Exception {
    byte unpacked[];
    unpacked = IccUtils.hexStringToBytes("566F696365204D61696C");
    assertThat(IccUtils.bytesToHexString(unpacked)).isEqualTo(
            IccUtils.bytesToHexString(GsmAlphabet.stringToGsm8BitPacked("Voice Mail")));

    unpacked = GsmAlphabet.stringToGsm8BitPacked(GSM_EXTENDED_CHARS);
    // two bytes for every extended char
    assertThat(2 * GSM_EXTENDED_CHARS.length()).isEqualTo(unpacked.length);
  }

  @Test
  public void stringToGsm8BitUnpackedField() throws Exception {
    byte unpacked[];
    // Test truncation of unaligned extended chars
    unpacked = new byte[3];
    GsmAlphabet.stringToGsm8BitUnpackedField(GSM_EXTENDED_CHARS, unpacked,
            0, unpacked.length);

    // Should be one extended char and an 0xff at the end
    assertThat(0xff).isEqualTo(0xff & unpacked[2]);
    assertThat(GSM_EXTENDED_CHARS.substring(0, 1)).isEqualTo(
            com.android.internal.telephony.GsmAlphabet.gsm8BitUnpackedToString(
                    unpacked, 0, unpacked.length));

    // Test truncation of normal chars
    unpacked = new byte[3];
    GsmAlphabet.stringToGsm8BitUnpackedField("abcd", unpacked,
            0, unpacked.length);

    assertThat("abc").isEqualTo(com.android.internal.telephony.GsmAlphabet
            .gsm8BitUnpackedToString(unpacked, 0, unpacked.length));

    // Test truncation of mixed normal and extended chars
    unpacked = new byte[3];
    GsmAlphabet.stringToGsm8BitUnpackedField("a{cd", unpacked,
            0, unpacked.length);

    assertThat("a{").isEqualTo(com.android.internal.telephony.GsmAlphabet
            .gsm8BitUnpackedToString(unpacked, 0, unpacked.length));

    // Test padding after normal char
    unpacked = new byte[3];
    GsmAlphabet.stringToGsm8BitUnpackedField("a", unpacked,
            0, unpacked.length);

    assertThat("a").isEqualTo(com.android.internal.telephony.GsmAlphabet
            .gsm8BitUnpackedToString(unpacked, 0, unpacked.length));

    assertThat(0xff).isEqualTo(0xff & unpacked[1]);
    assertThat(0xff).isEqualTo(0xff & unpacked[2]);

    // Test malformed input -- escape char followed by end of field
    unpacked[0] = 0;
    unpacked[1] = 0;
    unpacked[2] = GsmAlphabet.GSM_EXTENDED_ESCAPE;

    assertThat("@@").isEqualTo(com.android.internal.telephony.GsmAlphabet
            .gsm8BitUnpackedToString(unpacked, 0, unpacked.length));

    // non-zero offset
    assertThat("@").isEqualTo(com.android.internal.telephony.GsmAlphabet
            .gsm8BitUnpackedToString(unpacked, 1, unpacked.length - 1));

    // test non-zero offset
    unpacked[0] = 0;
    GsmAlphabet.stringToGsm8BitUnpackedField("abcd", unpacked,
            1, unpacked.length - 1);


    assertThat(0).isEqualTo(unpacked[0]);

    assertThat("ab").isEqualTo(com.android.internal.telephony.GsmAlphabet
            .gsm8BitUnpackedToString(unpacked, 1, unpacked.length - 1));

    // test non-zero offset with truncated extended char
    unpacked[0] = 0;

    GsmAlphabet.stringToGsm8BitUnpackedField("a{", unpacked,
            1, unpacked.length - 1);

    assertThat(0).isEqualTo(unpacked[0]);

    assertThat("a").isEqualTo(com.android.internal.telephony.GsmAlphabet
            .gsm8BitUnpackedToString(unpacked, 1, unpacked.length - 1));

    // Reserved for extension to extension table (mapped to space)
    unpacked[0] = 0x1b;
    unpacked[1] = 0x1b;
    assertThat(" ").isEqualTo(com.android.internal.telephony.GsmAlphabet
            .gsm8BitUnpackedToString(unpacked, 0, 2));

    // Unmappable (mapped to character in default or national locking shift table)
    unpacked[1] = 0x00;
    assertThat("@").isEqualTo(com.android.internal.telephony.GsmAlphabet
            .gsm8BitUnpackedToString(unpacked, 0, 2));
    unpacked[1] = 0x7f;
    assertThat("\u00e0").isEqualTo(com.android.internal.telephony.GsmAlphabet
            .gsm8BitUnpackedToString(unpacked, 0, 2));
  }
}
