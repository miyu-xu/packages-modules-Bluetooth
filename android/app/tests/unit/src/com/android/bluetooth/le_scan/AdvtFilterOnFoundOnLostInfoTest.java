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

package com.android.bluetooth.le_scan;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Test cases for {@link AdvtFilterOnFoundOnLostInfo}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class AdvtFilterOnFoundOnLostInfoTest {

    @Test
    public void advtFilterOnFoundOnLostInfoParams() {
        int clientIf = 0;
        int advPacketLen = 1;
        byte[] advPacket = new byte[] {0x02};
        int scanRspLen = 3;
        byte[] scanRsp = new byte[] {0x04};
        int filtIndex = 5;
        int advState = 6;
        int advInfoPresent = 7;
        String address = "00:11:22:33:FF:EE";
        int addressType = 8;
        int txPower = 9;
        int rssiValue = 10;
        int timeStamp = 11;
        byte[] resultByteArray = new byte[] {2, 4};

        AdvtFilterOnFoundOnLostInfo advtFilterOnFoundOnLostInfo =
                new AdvtFilterOnFoundOnLostInfo(
                        clientIf,
                        advPacketLen,
                        advPacket,
                        scanRspLen,
                        scanRsp,
                        filtIndex,
                        advState,
                        advInfoPresent,
                        address,
                        addressType,
                        txPower,
                        rssiValue,
                        timeStamp);

        assertThat(advtFilterOnFoundOnLostInfo.clientIf()).isEqualTo(clientIf);
        assertThat(advtFilterOnFoundOnLostInfo.advPacketLen()).isEqualTo(advPacketLen);
        assertThat(advtFilterOnFoundOnLostInfo.advPacket()).isEqualTo(advPacket);
        assertThat(advtFilterOnFoundOnLostInfo.scanRspLen()).isEqualTo(scanRspLen);
        assertThat(advtFilterOnFoundOnLostInfo.scanRsp()).isEqualTo(scanRsp);
        assertThat(advtFilterOnFoundOnLostInfo.filtIndex()).isEqualTo(filtIndex);
        assertThat(advtFilterOnFoundOnLostInfo.advState()).isEqualTo(advState);
        assertThat(advtFilterOnFoundOnLostInfo.advInfoPresent()).isEqualTo(advInfoPresent);
        assertThat(advtFilterOnFoundOnLostInfo.address()).isEqualTo(address);
        assertThat(advtFilterOnFoundOnLostInfo.addressType()).isEqualTo(addressType);
        assertThat(advtFilterOnFoundOnLostInfo.txPower()).isEqualTo(txPower);
        assertThat(advtFilterOnFoundOnLostInfo.rssiValue()).isEqualTo(rssiValue);
        assertThat(advtFilterOnFoundOnLostInfo.timeStamp()).isEqualTo(timeStamp);
        assertThat(advtFilterOnFoundOnLostInfo.getResult()).isEqualTo(resultByteArray);
    }
}
