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
package com.android.bluetooth.btservice;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.rule.ServiceTestRule;

import com.android.bluetooth.btservice.BluetoothSocketContextMap.App;
import com.android.bluetooth.btservice.BluetoothSocketContextMap.Connection;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;
import java.util.UUID;

public class BluetoothSocketContextMapTest {
    private static final String APP_NAME = "com.android.what.a.name";
    private static final int APP_UID1 = 123;
    private static final int APP_UID2 = 987;
    private static final int REG_ID1 = 456;
    private static final int REG_ID2 = 654;
    private static final int PROTOCOL1 = 1;
    private static final int PROTOCOL2 = 2;
    private static final int CONNECTION_STATE1 = 1;
    private static final int CONNECTION_STATE2 = 2;
    private static final UUID RANDOM_UUID1 = UUID.randomUUID();
    private static final UUID RANDOM_UUID2 = UUID.randomUUID();

    @Rule public final ServiceTestRule mServiceRule = new ServiceTestRule();

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    @Test
    public void getAppMethods() {
        BluetoothSocketContextMap contextMap = getMapWithAppAndConnection();
        App app = contextMap.getByRegId(REG_ID1);
        assertThat(app.regId).isEqualTo(REG_ID1);
        assertThat(app.appUid).isEqualTo(APP_UID1);
        assertThat(app.protocol).isEqualTo(PROTOCOL1);
        assertThat(app.state).isEqualTo(CONNECTION_STATE1);

        app = contextMap.getByRegId(REG_ID2);
        assertThat(app.regId).isEqualTo(REG_ID2);
        assertThat(app.appUid).isEqualTo(APP_UID2);
        assertThat(app.protocol).isEqualTo(PROTOCOL2);
        assertThat(app.state).isEqualTo(CONNECTION_STATE2);
    }

    @Test
    public void getConnectionMethods() {
        BluetoothSocketContextMap contextMap = getMapWithAppAndConnection();
        List<Connection> conns = contextMap.getConnectionByregId(REG_ID1);
        assertThat(conns).hasSize(1);
        assertThat(conns.get(0).connUuid).isEqualTo(RANDOM_UUID1);
        assertThat(conns.get(0).regId).isEqualTo(REG_ID1);
        assertThat(conns.get(0).protocol).isEqualTo(PROTOCOL1);
        assertThat(conns.get(0).appUid).isEqualTo(APP_UID1);

        conns = contextMap.getConnectionByregId(REG_ID2);
        assertThat(conns).hasSize(1);
        assertThat(conns.get(0).connUuid).isEqualTo(RANDOM_UUID2);
        assertThat(conns.get(0).regId).isEqualTo(REG_ID2);
        assertThat(conns.get(0).protocol).isEqualTo(PROTOCOL2);
        assertThat(conns.get(0).appUid).isEqualTo(APP_UID2);

        conns = contextMap.getConnectionByApp(APP_UID1);
        assertThat(conns).hasSize(1);
        assertThat(conns.get(0).connUuid).isEqualTo(RANDOM_UUID1);
        assertThat(conns.get(0).regId).isEqualTo(REG_ID1);
        assertThat(conns.get(0).protocol).isEqualTo(PROTOCOL1);
        assertThat(conns.get(0).appUid).isEqualTo(APP_UID1);
        assertThat(contextMap.getConnectionByApp(APP_UID2)).hasSize(1);
        assertThat(contextMap.getConnectionByApp(123456)).isEmpty();
    }

    @Test
    public void clear() {
        BluetoothSocketContextMap contextMap = getMapWithAppAndConnection();
        contextMap.clear();
        assertThat(contextMap.getConnectedSocketMap()).isEmpty();
        assertThat(contextMap.getAllAppsUids()).isEmpty();
    }

    @Test
    public void removeMethods() {
        BluetoothSocketContextMap contextMap = getMapWithAppAndConnection();
        App app = contextMap.getByRegId(REG_ID1);
        assertThat(app).isNotNull();
        contextMap.removeApp(REG_ID1);
        app = contextMap.getByRegId(REG_ID1);
        assertThat(app).isNull();

        List<Connection> conns = contextMap.getConnectionByregId(REG_ID1);
        assertThat(conns).isNotEmpty();
        contextMap.removeConnection(RANDOM_UUID1);
        conns = contextMap.getConnectionByregId(REG_ID1);
        assertThat(conns).isEmpty();
    }

    private BluetoothSocketContextMap getMapWithAppAndConnection() {
        BluetoothSocketContextMap contextMap = new BluetoothSocketContextMap();
        contextMap.add(REG_ID1, PROTOCOL1, CONNECTION_STATE1, APP_UID1, APP_NAME);
        contextMap.add(REG_ID2, PROTOCOL2, CONNECTION_STATE2, APP_UID2, APP_NAME);
        contextMap.addConnection(REG_ID1, RANDOM_UUID1);
        contextMap.addConnection(REG_ID2, RANDOM_UUID2);
        assertThat(contextMap.getConnectedSocketMap()).isNotEmpty();
        assertThat(contextMap.getAllAppsUids()).isNotEmpty();
        return contextMap;
    }
}
