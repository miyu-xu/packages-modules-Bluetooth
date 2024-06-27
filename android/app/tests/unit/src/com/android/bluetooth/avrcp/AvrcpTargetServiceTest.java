/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.bluetooth.avrcp;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.audio_util.Metadata;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class AvrcpTargetServiceTest {

    @Test
    public void testQueueUpdateData() {
        List<Metadata> firstQueue = new ArrayList<Metadata>();
        List<Metadata> secondQueue = new ArrayList<Metadata>();

        firstQueue.add(createEmptyMetadata());
        secondQueue.add(createEmptyMetadata());
        Assert.assertFalse(AvrcpTargetService.isQueueUpdated(firstQueue, secondQueue));

        secondQueue.add(createEmptyMetadata());
        Assert.assertTrue(AvrcpTargetService.isQueueUpdated(firstQueue, secondQueue));

        firstQueue.add(createEmptyMetadata());
        firstQueue.get(1).duration = "1";
        Assert.assertFalse(AvrcpTargetService.isQueueUpdated(firstQueue, secondQueue));

        secondQueue.get(1).title = "new title";
        Assert.assertTrue(AvrcpTargetService.isQueueUpdated(firstQueue, secondQueue));
    }

    private Metadata createEmptyMetadata() {
        Metadata.Builder builder = new Metadata.Builder();
        return builder.useDefaults().build();
    }
}
