/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.bluetooth

import android.cts.statsdatom.lib.ConfigUtils
import android.cts.statsdatom.lib.DeviceUtils
import android.cts.statsdatom.lib.ReportUtils
import com.android.os.AtomsProto
import com.android.os.StatsLog
import com.android.tradefed.testtype.DeviceJUnit4ClassRunner
import com.android.tradefed.testtype.junit4.BaseHostJUnit4Test
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(DeviceJUnit4ClassRunner::class)
class MetricsTest : BaseHostJUnit4Test() {

  companion object {
    private const val TAG = "BluetoothMetricsTest"
    private const val TEST_APP_PKG_NAME = "android.bluetooth"
  }

  @Before
  fun setUp() {
    ConfigUtils.removeConfig(getDevice())
    ReportUtils.clearReports(getDevice())
  }

  @After fun tearDown() {}

  @Test
  fun aclMetricTest() {
    uploadAtomConfigAndTriggerTest("incomingClassicConnectionTest")
  }

  private fun uploadAtomConfigAndTriggerTest(testName: String): List<StatsLog.EventMetricData> {
    ConfigUtils.uploadConfigForPushedAtoms(
        getDevice(),
        TEST_APP_PKG_NAME,
        intArrayOf(AtomsProto.Atom.BLUETOOTH_ACL_CONNECTION_STATE_CHANGED_FIELD_NUMBER))

    DeviceUtils.runDeviceTests(getDevice(), TEST_APP_PKG_NAME, ".AclConnectionTest", testName)

    return ReportUtils.getEventMetricDataList(getDevice())
  }
}
