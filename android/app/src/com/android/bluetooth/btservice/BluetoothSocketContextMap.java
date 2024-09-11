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

import android.util.ArraySet;
import android.util.Log;

import com.android.internal.annotations.GuardedBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public class BluetoothSocketContextMap {
    private static final String TAG = "BluetoothSocketContextMap";

    /** Connection class helps map connection UUIDs to socket context. */
    public static class Connection {
        public UUID connUuid;

        public int regId;
        public int protocol;
        public int appUid;

        Connection(UUID connUuid, int regId, int protocol, int appUid) {
            this.connUuid = connUuid;
            this.regId = regId;
            this.protocol = protocol;
            this.appUid = appUid;
        }
    }

    /** Application entry mapping registration IDs to appUids. */
    public static class App {
        public int regId;
        public int protocol;
        public int appUid;
        public String pkgName;

        /** Creates a new application context. */
        App(int regId, int protocol, int appUid, String pkgName) {
            this.regId = regId;
            this.protocol = protocol;
            this.appUid = appUid;
            this.pkgName = pkgName;
        }
    }

    private final Object mAppsLock = new Object();
    private final Object mConnectionsLock = new Object();

    /** Application list */
    @GuardedBy("mAppsLock")
    private List<App> mApps = new ArrayList<>();

    /** List of connected sockets */
    @GuardedBy("mConnectionsLock")
    private List<Connection> mConnections = new ArrayList<>();

    /** Add an entry to the application context list. */
    public App add(int regId, int protocol, int appUid, String pkgName) {
        synchronized (mAppsLock) {
            App app = new App(regId, protocol, appUid, pkgName);
            mApps.add(app);
            return app;
        }
    }

    /** Remove all applications for a given registration ID */
    public void removeApp(int regId) {
        synchronized (mAppsLock) {
            mApps.removeIf(app -> app.regId == regId);
        }
    }

    /** Add a new connection for a given application ID. */
    void addConnection(int regId, UUID connUuid) {
        synchronized (mConnectionsLock) {
            App entry = getByRegId(regId);
            if (entry != null) {
                mConnections.add(new Connection(connUuid, regId, entry.protocol, entry.appUid));
            }
        }
    }

    /** Remove all connections with the given connection UUID. */
    void removeConnection(UUID connUuid) {
        synchronized (mConnectionsLock) {
            mConnections.removeIf(conn -> conn.connUuid.equals(connUuid));
        }
    }

    /** Get an application context by registration ID. */
    public App getByRegId(int regId) {
        App app = getAppByPredicate(entry -> entry.regId == regId);
        if (app == null) {
            Log.e(TAG, "Context not found for regId " + regId);
        }
        return app;
    }

    private App getAppByPredicate(Predicate<App> predicate) {
        synchronized (mAppsLock) {
            // Intentionally using a for-loop over a stream for performance.
            for (App app : mApps) {
                if (predicate.test(app)) {
                    return app;
                }
            }
            return null;
        }
    }

    /** Get connection list by application Uid. */
    public List<Connection> getConnectionByApp(int appUid) {
        List<Connection> currentConnections = new ArrayList<Connection>();
        synchronized (mConnectionsLock) {
            for (Connection connection : mConnections) {
                if (connection.appUid == appUid) {
                    currentConnections.add(connection);
                }
            }
        }
        return currentConnections;
    }

    /** Get connection list by registration ID. */
    public List<Connection> getConnectionByregId(int regId) {
        List<Connection> currentConnections = new ArrayList<Connection>();
        synchronized (mConnectionsLock) {
            for (Connection connection : mConnections) {
                if (connection.regId == regId) {
                    currentConnections.add(connection);
                }
            }
        }
        return currentConnections;
    }

    /** Returns connected socket map with appUid and Connections */
    Map<Integer, List<Connection>> getConnectedSocketMap() {
        Map<Integer, List<Connection>> socketMap = new HashMap<Integer, List<Connection>>();
        Set<Integer> appUids = getAllAppsUids();
        synchronized (mConnectionsLock) {
            for (Integer appUid : appUids) {
                socketMap.put(appUid, getConnectionByApp(appUid));
            }
        }
        return socketMap;
    }

    /** Get all appUids from app and connection entry */
    public Set<Integer> getAllAppsUids() {
        Set<Integer> appUids = new ArraySet();
        synchronized (mAppsLock) {
            for (App app : mApps) {
                appUids.add(app.appUid);
            }
        }

        synchronized (mConnectionsLock) {
            for (Connection connection : mConnections) {
                appUids.add(connection.appUid);
            }
        }
        return appUids;
    }

    /** Erases all application context entries. */
    public void clear() {
        synchronized (mAppsLock) {
            mApps.clear();
        }

        synchronized (mConnectionsLock) {
            mConnections.clear();
        }
    }
}
