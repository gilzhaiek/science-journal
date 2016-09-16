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
 */package com.google.android.apps.forscience.whistlepunk.devicemanager;

import android.app.PendingIntent;
import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.apps.forscience.javalib.Consumer;
import com.google.android.apps.forscience.javalib.MaybeConsumer;
import com.google.android.apps.forscience.javalib.MaybeConsumers;
import com.google.android.apps.forscience.javalib.Success;
import com.google.android.apps.forscience.whistlepunk.DataController;
import com.google.android.apps.forscience.whistlepunk.LoggingConsumer;
import com.google.android.apps.forscience.whistlepunk.R;
import com.google.android.apps.forscience.whistlepunk.metadata.ExternalSensorSpec;
import com.google.common.base.Joiner;

import java.util.HashMap;
import java.util.Map;

/**
 * Remembers sensors that have been found during scanning, and can expose them by adding them
 * to a PreferenceCategory.
 */
public class ConnectableSensorRegistry {
    private static final String TAG = "ConSensorRegistry";

    private final DataController mDataController;
    private final Map<String, ExternalSensorDiscoverer> mDiscoverers;
    private final Map<String, ConnectableSensor> mSensors = new HashMap<>();
    private final Map<String, PendingIntent> mSettingsIntents = new HashMap<>();

    private int mKeyNum = 0;
    private Context mContext;

    public ConnectableSensorRegistry(DataController dataController,
            Map<String, ExternalSensorDiscoverer> discoverers, Context context) {
        mDataController = dataController;
        mDiscoverers = discoverers;
        mContext = context;
    }

    private static void oopsLog(String methodName, String... params) {
        // OOPS: delete!
        String paramString = Joiner.on(", " + "").join(params);
        Log.e("OOPS", methodName + "(" + paramString + ")");
    }

    public void showDeviceOptions(DeviceOptionsPresenter presenter, String experimentId,
            Preference preference, PendingIntent settingsIntent) {
        oopsLog("ConnectableSensorRegistry#showDeviceOptions", "presenter=" + presenter,
                "experimentId=" + experimentId, "preference=" + preference,
                "settingsIntent=" + settingsIntent);
        presenter.showDeviceOptions(experimentId, getSensor(preference).getConnectedSensorId(),
                settingsIntent);
    }

    public boolean getIsPairedFromPreference(Preference preference) {
        return getSensor(preference).isPaired();
    }

    public boolean startScanningInDiscoverers(final PreferenceCategory availableDevices) {
        Consumer<ExternalSensorDiscoverer.DiscoveredSensor> onEachSensorFound =
                new Consumer<ExternalSensorDiscoverer.DiscoveredSensor>() {
            @Override
            public void take(ExternalSensorDiscoverer.DiscoveredSensor ds) {
                onSensorFound(ds, availableDevices);
            }
        };
        boolean started = false;
        for (ExternalSensorDiscoverer discoverer : mDiscoverers.values()) {
            if (discoverer.startScanning(onEachSensorFound,
                    LoggingConsumer.expectSuccess(TAG, "Discovering sensors"),
                    availableDevices.getContext())) {
                started = true;
            }
        }

        return started;
    }

    private void onSensorFound(ExternalSensorDiscoverer.DiscoveredSensor ds,
            PreferenceCategory availableDevices) {
        String sensorKey = findSensorKey(ds.getSpec());

        if (sensorKey == null) {
            availableDevices.addPreference(
                    buildPreference(ConnectableSensor.disconnected(ds.getSpec()),
                            ds.getSettingsIntent()));
        } else {
            ConnectableSensor sensor = mSensors.get(sensorKey);
            if (!sensor.isPaired()) {
                if (availableDevices.findPreference(sensorKey) == null) {
                    availableDevices.addPreference(
                            makePreference(sensor, sensorKey, ds.getSettingsIntent()));
                }
            } else {
                // TODO: UI feedback
                mSettingsIntents.put(sensorKey, ds.getSettingsIntent());
            }
        }
    }

    private String findSensorKey(ExternalSensorSpec spec) {
        for (Map.Entry<String, ConnectableSensor> entry : mSensors.entrySet()) {
            if (entry.getValue().getSpec().isSameSensor(spec)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void setPairedSensors(PreferenceCategory availableDevices,
            PreferenceCategory pairedDevices, Map<String, ExternalSensorSpec> sensors) {
        for (Map.Entry<String, ExternalSensorSpec> entry : sensors.entrySet()) {
            ExternalSensorSpec sensor = entry.getValue();
            removeSensorWithSpec(availableDevices, sensor);

            ConnectableSensor newSensor = ConnectableSensor.connected(sensor, entry.getKey());
            Preference pref = buildPreference(newSensor, null);
            pref.setWidgetLayoutResource(R.layout.preference_external_device);
            pref.setSummary(sensor.getSensorAppearance().getName(pref.getContext()));
            pairedDevices.addPreference(pref);
        }

        removeMissingPairedSensors(sensors);
    }

    private void removeMissingPairedSensors(Map<String, ExternalSensorSpec> sensors) {
        for (String sensorKey : mSensors.keySet()) {
            ConnectableSensor sensor = mSensors.get(sensorKey);
            if (sensor.isPaired() && !sensors.containsKey(sensor.getConnectedSensorId())) {
                mSensors.put(sensorKey, ConnectableSensor.disconnected(sensor.getSpec()));
            }
        }
    }

    private void removeSensorWithSpec(PreferenceCategory availableDevices,
            ExternalSensorSpec sensor) {
        String sensorKey = findSensorKey(sensor);
        if (sensorKey != null) {
            removePref(availableDevices, sensorKey);
            mSensors.remove(sensorKey);
            mSettingsIntents.remove(sensorKey);
        }
    }

    private void removePref(PreferenceCategory category, String prefKey) {
        Preference preference = category.findPreference(prefKey);
        if (preference != null) {
            category.removePreference(preference);
        }
    }

    private Preference buildPreference(ConnectableSensor sensor, PendingIntent settingsIntent) {
        String key = "sensorKey" + (mKeyNum++);
        mSensors.put(key, sensor);
        return makePreference(sensor, key, settingsIntent);
    }

    @NonNull
    private Preference makePreference(ConnectableSensor sensor, String key,
            PendingIntent settingsIntent) {
        Preference pref = new Preference(mContext);
        pref.setTitle(sensor.getName());
        pref.setKey(key);
        mSettingsIntents.put(key, settingsIntent);
        return pref;
    }

    /**
     * Pairs to the sensor represented by the given preference, and adds it to the given experiment
     *
     * @param onAdded receives the connected ConnectableSensor that's been added to the
     *                experiment (or gets nothing if the sensor was already added).
     */
    public void addExternalSensorIfNecessary(final String experimentId, Preference preference,
            final MaybeConsumer<ConnectableSensor> onAdded) {
        preference.setEnabled(false);
        preference.setSummary(R.string.external_devices_pairing);
        ConnectableSensor connectableSensor = getSensor(preference);

        // TODO: probably shouldn't finish in these cases, instead go into sensor editing.

        final ExternalSensorSpec sensor = connectableSensor.getSpec();
        mDataController.addOrGetExternalSensor(sensor, MaybeConsumers.chainFailure(onAdded,
                new Consumer<String>() {
                    @Override
                    public void take(final String sensorId) {
                        mDataController.addSensorToExperiment(experimentId, sensorId,
                                new LoggingConsumer<Success>(TAG, "add sensor to experiment") {
                                    @Override
                                    public void success(Success value) {
                                        onAdded.success(
                                                ConnectableSensor.connected(sensor, sensorId));
                                    }
                                });
                    }
                }));
    }

    private ConnectableSensor getSensor(Preference preference) {
        return getSensor(preference.getKey());
    }

    @NonNull
    private ConnectableSensor getSensor(String key) {
        ConnectableSensor sensor = mSensors.get(key);
        if (sensor == null) {
            throw new IllegalArgumentException("No sensor found for key " + key);
        }
        return sensor;
    }

    public void stopScanningInDiscoverers() {
        for (ExternalSensorDiscoverer discoverer : mDiscoverers.values()) {
            discoverer.stopScanning();
        }
    }

    public PendingIntent getSettingsIntentFromPreference(Preference preference) {
        return mSettingsIntents.get(preference.getKey());
    }
}
