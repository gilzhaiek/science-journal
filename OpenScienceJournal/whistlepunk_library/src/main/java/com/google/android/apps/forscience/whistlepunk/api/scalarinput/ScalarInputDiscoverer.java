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

import android.app.PendingIntent;
import android.content.Context;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.apps.forscience.javalib.Consumer;
import com.google.android.apps.forscience.javalib.FailureListener;
import com.google.android.apps.forscience.whistlepunk.AppSingleton;
import com.google.android.apps.forscience.whistlepunk.ExternalSensorProvider;
import com.google.android.apps.forscience.whistlepunk.R;
import com.google.android.apps.forscience.whistlepunk.devicemanager.ExternalSensorDiscoverer;
import com.google.android.apps.forscience.whistlepunk.metadata.ExternalSensorSpec;

import java.util.concurrent.Executor;

public class ScalarInputDiscoverer implements ExternalSensorDiscoverer {
    private static String TAG = "SIDiscoverer";
    public static final String SCALAR_INPUT_PROVIDER_ID =
            "com.google.android.apps.forscience.whistlepunk.scalarinput";
    private final Consumer<AppDiscoveryCallbacks> mServiceFinder;
    private final ScalarInputStringSource mStringSource;
    private final Executor mUiThreadExecutor;

    public ScalarInputDiscoverer(Consumer<AppDiscoveryCallbacks> serviceFinder,
            Context context) {
        this(serviceFinder, defaultStringSource(context), AppSingleton.getUiThreadExecutor());
    }

    private static ScalarInputStringSource defaultStringSource(final Context context) {
        return new ScalarInputStringSource() {
            @Override
            public String generateCouldNotFindServiceErrorMessage(String serviceId) {
                return context.getResources().getString(R.string.could_not_find_service_error,
                        serviceId);
            }
        };
    }

    public ScalarInputDiscoverer(
            Consumer<AppDiscoveryCallbacks> serviceFinder, ScalarInputStringSource stringSource,
            Executor uiThreadExecutor) {
        mServiceFinder = serviceFinder;
        mStringSource = stringSource;
        mUiThreadExecutor = uiThreadExecutor;
    }

    @Override
    public ExternalSensorProvider getProvider() {
        return new ScalarInputProvider(mServiceFinder, mStringSource, mUiThreadExecutor);
    }

    @Override
    public boolean startScanning(final Consumer<ExternalSensorSpec> onEachSensorFound,
            final FailureListener onScanError, final Context context) {
        mServiceFinder.take(new AppDiscoveryCallbacks() {
            @Override
            public void onServiceFound(String serviceId, final ISensorDiscoverer service) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Found service: " + serviceId);
                }
                try {
                    final ISensorConsumer.Stub sc = makeSensorConsumer(serviceId,
                            onEachSensorFound);
                    service.scanDevices(makeDeviceConsumer(service, sc));
                } catch (RemoteException e) {
                    onScanError.fail(e);
                }
            }

            @Override
            public void onDiscoveryDone() {
                // TODO: what to do here?  Somehow plumb back that we're done scanning?
            }
        });
        // TODO: should this ever be false?
        return true;
    }

    @NonNull
    private IDeviceConsumer.Stub makeDeviceConsumer(final ISensorDiscoverer service,
            final ISensorConsumer.Stub sc) {
        return new IDeviceConsumer.Stub() {
            @Override
            public void onDeviceFound(String deviceId, String name, PendingIntent settingsIntent)
                    throws RemoteException {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Found device: " + name);
                }
                service.scanSensors(deviceId, sc);
            }
        };
    }

    @NonNull
    private ISensorConsumer.Stub makeSensorConsumer(final String serviceId,
            final Consumer<ExternalSensorSpec> onEachSensorFound) {
        return new ISensorConsumer.Stub() {
            @Override
            public void onSensorFound(String sensorAddress, String name,
                    SensorAppearanceResources ids, PendingIntent settingsIntent)
                    throws RemoteException {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Found sensor: " + name);
                }
                onEachSensorFound.take(new ScalarInputSpec(name, serviceId, sensorAddress, ids));
            }
        };
    }

    @Override
    public void stopScanning() {
        // TODO: implement!
    }

}
