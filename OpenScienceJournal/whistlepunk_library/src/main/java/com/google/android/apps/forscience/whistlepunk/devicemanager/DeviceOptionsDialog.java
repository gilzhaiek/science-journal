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

package com.google.android.apps.forscience.whistlepunk.devicemanager;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.apps.forscience.whistlepunk.AppSingleton;
import com.google.android.apps.forscience.whistlepunk.DataController;
import com.google.android.apps.forscience.whistlepunk.LoggingConsumer;
import com.google.android.apps.forscience.whistlepunk.R;
import com.google.android.apps.forscience.whistlepunk.WhistlePunkApplication;
import com.google.android.apps.forscience.whistlepunk.analytics.TrackerConstants;
import com.google.android.apps.forscience.whistlepunk.metadata.ExternalSensorSpec;
import com.google.common.base.Joiner;

/**
 * Presents dialog options for external devices.
 */
public class DeviceOptionsDialog extends DialogFragment {
    private static final String TAG = "DeviceOptionsDialog";
    private static final String KEY_EXPERIMENT_ID = "experiment_id";
    private static final String KEY_SENSOR_ID = "sensor_id";
    private static final String KEY_SETTINGS_INTENT = "settings_intent";

    /**
     * Object listening for options changing.
     */
    public interface DeviceOptionsListener {
        /**
         * Called when one sensor in the experiment is being replaced by another (which should be
         * displayed in the same place)
         *
         * @param oldSensorId
         * @param newSensorId
         */
        public void onExperimentSensorReplaced(String oldSensorId, String newSensorId);

        /**
         * Called when the user requests to remove the device from the experiment.
         */
        public void onRemoveDeviceFromExperiment(String experimentId, String sensorId);
    }

    private DataController mDataController;
    private DeviceOptionsViewController mViewController;

    public static DeviceOptionsDialog newInstance(String experimentId, String sensorId,
            PendingIntent externalSettingsIntent) {
        oopsLog("DeviceOptionsDialog#newInstance", "experimentId=" + experimentId,
                "sensorId=" + sensorId, "externalSettingsIntent=" + externalSettingsIntent);
        Bundle args = new Bundle();
        args.putString(KEY_EXPERIMENT_ID, experimentId);
        args.putString(KEY_SENSOR_ID, sensorId);
        args.putParcelable(KEY_SETTINGS_INTENT, externalSettingsIntent);

        DeviceOptionsDialog dialog = new DeviceOptionsDialog();
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        WhistlePunkApplication.getUsageTracker(getActivity()).trackScreenView(
                TrackerConstants.SCREEN_DEVICE_OPTIONS);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private static void oopsLog(String methodName, String... params) {
        // OOPS: delete!
        String paramString = Joiner.on(", " + "").join(params);
        Log.e("OOPS", methodName + "(" + paramString + ")");
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final String sensorId = getArguments().getString(KEY_SENSOR_ID);
        final PendingIntent settingsIntent = getArguments().getParcelable(KEY_SETTINGS_INTENT);
        oopsLog("DeviceOptionsDialog#onCreateDialog", "savedInstanceState=" + savedInstanceState,
                "settingsIntent=" + settingsIntent);
        DialogInterface.OnClickListener onOK;
        View view;
        if (settingsIntent == null) {
            setupControllers();
            mDataController.getExternalSensorById(sensorId, new LoggingConsumer<ExternalSensorSpec>(TAG,
                    "Load external sensor with ID = " + sensorId) {
                @Override
                public void success(ExternalSensorSpec sensor) {
                    mViewController.setSensor(sensorId, sensor, savedInstanceState);
                }
            });
            onOK = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mViewController.commit(getOptionsListener());
                }
            };
            view = mViewController.getView();
        } else {
            onOK = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            };

            view = LayoutInflater.from(getActivity()).inflate(R.layout.api_device_options_dialog,
                    null);
            view.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        settingsIntent.send();
                    } catch (PendingIntent.CanceledException e) {
                        // SAFF: what?
                        e.printStackTrace();
                    }
                }
            });
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setView(view)
                .setTitle(R.string.external_devices_settings_title)
                .setPositiveButton(R.string.external_devices_settings_ok, onOK)
                .setNegativeButton(R.string.external_devices_settings_forget,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                removeDeviceFromExperiment();
                            }
                        });
        return builder.create();
    }

    private void setupControllers() {
        mDataController = AppSingleton.getInstance(getActivity()).getDataController();
        mViewController = new DeviceOptionsViewController(getActivity(), mDataController,
                getExperimentId());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mViewController != null) {
            mViewController.onSaveInstanceState(outState);
        }
    }

    private void removeDeviceFromExperiment() {
        if (getActivity() instanceof DeviceOptionsListener) {
            ((DeviceOptionsListener) getActivity()).onRemoveDeviceFromExperiment(getExperimentId(),
                    getArguments().getString(KEY_SENSOR_ID));
        }
    }

    private String getExperimentId() {
        return getArguments().getString(KEY_EXPERIMENT_ID);
    }

    private DeviceOptionsListener getOptionsListener() {
        if (getActivity() instanceof DeviceOptionsListener) {
            return (DeviceOptionsListener) getActivity();
        } else {
            return null;
        }
    }
}
