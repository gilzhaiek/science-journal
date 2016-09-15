/*
 *  Copyright 2016 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.google.android.apps.forscience.whistlepunk.api.scalarinput;

import android.content.Context;
import android.os.RemoteException;

import com.google.android.apps.forscience.javalib.Consumer;
import com.google.android.apps.forscience.whistlepunk.sensorapi.AbstractSensorRecorder;
import com.google.android.apps.forscience.whistlepunk.sensorapi.ScalarSensor;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorEnvironment;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorRecorder;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorStatusListener;
import com.google.android.apps.forscience.whistlepunk.sensorapi.StreamConsumer;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Sensor that receives data through the scalar input API.
 */
class ScalarInputSensor extends ScalarSensor {

    private final String mAddress;
    private final String mServiceId;
    private Consumer<AppDiscoveryCallbacks> mServiceFinder;
    private ScalarInputStringSource mStringSource;

    // TODO: find a way to reduce parameters?
    public ScalarInputSensor(
            String sensorId,
            Executor uiThreadExecutor,
            final Consumer<AppDiscoveryCallbacks> serviceFinder,
            final ScalarInputStringSource stringSource,
            String serviceId,
            String sensorAddress) {
        super(sensorId, uiThreadExecutor);
        mAddress = sensorAddress;
        mServiceId = serviceId;
        mServiceFinder = serviceFinder;
        mStringSource = stringSource;
    }

    @Override
    protected SensorRecorder makeScalarControl(final StreamConsumer c,
            SensorEnvironment environment,
            Context context, final SensorStatusListener listener) {
        return new AbstractSensorRecorder() {
            private ISensorConnector mConnector = null;

            @Override
            public void startObserving() {
                mServiceFinder.take(new AppDiscoveryCallbacks() {
                    @Override
                    public void onServiceFound(String serviceId, ISensorDiscoverer service) {
                        if (!Objects.equals(serviceId, mServiceId)) {
                            return;
                        }

                        try {
                            // TODO: generate correct value of settingsKey
                            String settingsKey = null;
                            mConnector = service.getConnector();
                            mConnector.startObserving(mAddress, makeObserver(c),
                                    makeListener(listener), settingsKey);
                        } catch (RemoteException e) {
                            complain(e);
                        }
                    }

                    private ISensorObserver makeObserver(final StreamConsumer c) {
                        return new ISensorObserver.Stub() {
                            @Override
                            public void onNewData(long timestamp, double data)
                                    throws RemoteException {
                                c.addData(timestamp, data);
                            }
                        };
                    }

                    private ISensorStatusListener makeListener(
                            final SensorStatusListener listener) {
                        return new ISensorStatusListener.Stub() {
                            // TODO(saff): test threading here!
                            @Override
                            public void onSensorConnecting() throws RemoteException {
                                runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        listener.onSourceStatus(getId(),
                                                SensorStatusListener.STATUS_CONNECTING);
                                    }
                                });
                            }

                            @Override
                            public void onSensorConnected() throws RemoteException {
                                runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        listener.onSourceStatus(getId(),
                                                SensorStatusListener.STATUS_CONNECTED);
                                    }
                                });
                            }

                            @Override
                            public void onSensorDisconnected() throws RemoteException {
                                runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        listener.onSourceStatus(getId(),
                                                SensorStatusListener.STATUS_DISCONNECTED);
                                    }
                                });
                            }

                            @Override
                            public void onSensorError(final String errorMessage)
                                    throws RemoteException {
                                runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        listener.onSourceError(getId(),
                                                SensorStatusListener.ERROR_UNKNOWN, errorMessage);
                                    }
                                });
                            }
                        };
                    }

                    @Override
                    public void onDiscoveryDone() {
                        // mConnector is null if no matching services were reported to
                        // onServiceFound.
                        if (mConnector == null) {
                            listener.onSourceError(getId(),
                                    SensorStatusListener.ERROR_FAILED_TO_CONNECT,
                                    mStringSource.generateCouldNotFindServiceErrorMessage(
                                            ScalarInputSensor.this.mServiceId));
                        }
                    }
                });
            }

            @Override
            public void stopObserving() {
                if (mConnector != null) {
                    try {
                        mConnector.stopObserving(mAddress);
                    } catch (RemoteException e) {
                        complain(e);
                    }
                    mConnector = null;
                }
            }

            private void complain(RemoteException e) {
                listener.onSourceError(getId(), SensorStatusListener.ERROR_UNKNOWN,
                        e.getMessage());
            }
        };
    }
}
