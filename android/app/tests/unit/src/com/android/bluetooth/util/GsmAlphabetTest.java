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

import static org.junit.Assert.assertEquals;

import com.android.internal.telephony.uicc.IccUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class GsmAlphabetTest {

  private static final String sGsmExtendedChars = "{|}\\[~]\f\u20ac";

  @Test
  public void gsm7BitPackedToString() throws Exception {
    byte[] packed;
    StringBuilder testString = new StringBuilder(300);

    packed = com.android.internal.telephony.GsmAlphabet.stringToGsm7BitPacked(
            testString.toString());
    assertEquals(testString.toString(),
            GsmAlphabet.gsm7BitPackedToString(packed, 1, 0xff & packed[0], 0, 0, 0));

    // Check all alignment cases
    for (int i = 0; i < 9; i++, testString.append('@')) {
      packed = com.android.internal.telephony.GsmAlphabet.stringToGsm7BitPacked(
              testString.toString());
      assertEquals(testString.toString(),
              GsmAlphabet.gsm7BitPackedToString(packed, 1, 0xff & packed[0], 0, 0, 0));
    }

    // Test extended chars too
    testString.append(sGsmExtendedChars);
    packed = com.android.internal.telephony.GsmAlphabet.stringToGsm7BitPacked(
            testString.toString());
    assertEquals(testString.toString(),
            GsmAlphabet.gsm7BitPackedToString(packed, 1, 0xff & packed[0], 0, 0, 0));

    // Try 254 septets with 127 extended chars
    testString.setLength(0);
    for (int i = 0; i < (255 / 2); i++) {
      testString.append('{');
    }
    packed = com.android.internal.telephony.GsmAlphabet.stringToGsm7BitPacked(
            testString.toString());
    assertEquals(testString.toString(),
            GsmAlphabet.gsm7BitPackedToString(packed, 1, 0xff & packed[0], 0, 0, 0));

    // Reserved for extension to extension table (mapped to space)
    packed = new byte[]{(byte)(0x1b | 0x80), 0x1b >> 1};
    assertEquals(" ", GsmAlphabet.gsm7BitPackedToString(packed, 0, 2, 0, 0, 0));

    // Unmappable (mapped to character in default alphabet table)
    packed[0] = 0x1b;
    packed[1] = 0x00;
    assertEquals("@", GsmAlphabet.gsm7BitPackedToString(packed, 0, 2, 0, 0, 0));
    packed[0] = (byte)(0x1b | 0x80);
    packed[1] = (byte)(0x7f >> 1);
    assertEquals("\u00e0", GsmAlphabet.gsm7BitPackedToString(packed, 0, 2, 0, 0, 0));
  }

  @Test
  public void stringToGsm8BitPacked() throws Exception {
    byte unpacked[];
    unpacked = IccUtils.hexStringToBytes("566F696365204D61696C");
    assertEquals(IccUtils.bytesToHexString(unpacked),
            IccUtils.bytesToHexString(
                    GsmAlphabet.stringToGsm8BitPacked("Voice Mail")));

    unpacked = GsmAlphabet.stringToGsm8BitPacked(sGsmExtendedChars);
    // two bytes for every extended char
    assertEquals(2 * sGsmExtendedChars.length(), unpacked.length);
  }

  @Test
  public void stringToGsm8BitUnpackedField() throws Exception {
    byte unpacked[];
    // Test truncation of unaligned extended chars
    unpacked = new byte[3];
    GsmAlphabet.stringToGsm8BitUnpackedField(sGsmExtendedChars, unpacked,
            0, unpacked.length);

    // Should be one extended char and an 0xff at the end
    assertEquals(0xff, 0xff & unpacked[2]);
    assertEquals(sGsmExtendedChars.substring(0, 1),
            com.android.internal.telephony.GsmAlphabet.gsm8BitUnpackedToString(
                    unpacked, 0, unpacked.length));

    // Test truncation of normal chars
    unpacked = new byte[3];
    GsmAlphabet.stringToGsm8BitUnpackedField("abcd", unpacked,
            0, unpacked.length);

    assertEquals("abc", com.android.internal.telephony.GsmAlphabet.gsm8BitUnpackedToString(
            unpacked, 0, unpacked.length));

    // Test truncation of mixed normal and extended chars
    unpacked = new byte[3];
    GsmAlphabet.stringToGsm8BitUnpackedField("a{cd", unpacked,
            0, unpacked.length);

    assertEquals("a{", com.android.internal.telephony.GsmAlphabet.gsm8BitUnpackedToString(
            unpacked, 0, unpacked.length));

    // Test padding after normal char
    unpacked = new byte[3];
    GsmAlphabet.stringToGsm8BitUnpackedField("a", unpacked,
            0, unpacked.length);

    assertEquals("a", com.android.internal.telephony.GsmAlphabet.gsm8BitUnpackedToString(
            unpacked, 0, unpacked.length));

    assertEquals(0xff, 0xff & unpacked[1]);
    assertEquals(0xff, 0xff & unpacked[2]);

    // Test malformed input -- escape char followed by end of field
    unpacked[0] = 0;
    unpacked[1] = 0;
    unpacked[2] = GsmAlphabet.GSM_EXTENDED_ESCAPE;

    assertEquals("@@", com.android.internal.telephony.GsmAlphabet.gsm8BitUnpackedToString(
            unpacked, 0, unpacked.length));

    // non-zero offset
    assertEquals("@", com.android.internal.telephony.GsmAlphabet.gsm8BitUnpackedToString(
            unpacked, 1, unpacked.length - 1));

    // test non-zero offset
    unpacked[0] = 0;
    GsmAlphabet.stringToGsm8BitUnpackedField("abcd", unpacked,
            1, unpacked.length - 1);


    assertEquals(0, unpacked[0]);

    assertEquals("ab", com.android.internal.telephony.GsmAlphabet.gsm8BitUnpackedToString(
            unpacked, 1, unpacked.length - 1));

    // test non-zero offset with truncated extended char
    unpacked[0] = 0;

    GsmAlphabet.stringToGsm8BitUnpackedField("a{", unpacked,
            1, unpacked.length - 1);

    assertEquals(0, unpacked[0]);

    assertEquals("a", com.android.internal.telephony.GsmAlphabet.gsm8BitUnpackedToString(
            unpacked, 1, unpacked.length - 1));

    // Reserved for extension to extension table (mapped to space)
    unpacked[0] = 0x1b;
    unpacked[1] = 0x1b;
    assertEquals(" ", com.android.internal.telephony.GsmAlphabet.gsm8BitUnpackedToString(
            unpacked, 0, 2));

    // Unmappable (mapped to character in default or national locking shift table)
    unpacked[1] = 0x00;
    assertEquals("@", com.android.internal.telephony.GsmAlphabet.gsm8BitUnpackedToString(
            unpacked, 0, 2));
    unpacked[1] = 0x7f;
    assertEquals("\u00e0", com.android.internal.telephony.GsmAlphabet.gsm8BitUnpackedToString(
            unpacked, 0, 2));
  }
}
