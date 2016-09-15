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

package com.google.android.apps.forscience.whistlepunk;

import com.google.android.apps.forscience.javalib.MaybeConsumer;
import com.google.android.apps.forscience.javalib.Success;
import com.google.android.apps.forscience.whistlepunk.metadata.ExternalSensorSpec;
import com.google.android.apps.forscience.whistlepunk.sensors.AccelerometerSensor;
import com.google.android.apps.forscience.whistlepunk.sensors.AmbientLightSensor;
import com.google.android.apps.forscience.whistlepunk.sensors.AmbientTemperatureSensor;
import com.google.android.apps.forscience.whistlepunk.sensors.BarometerSensor;
import com.google.android.apps.forscience.whistlepunk.sensors.DecibelSensor;
import com.google.android.apps.forscience.whistlepunk.sensors.MagneticRotationSensor;
import com.google.android.apps.forscience.whistlepunk.sensors.SineWavePseudoSensor;
import com.google.android.apps.forscience.whistlepunk.sensors.VideoSensor;
import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;

public class SensorAppearanceProviderImpl implements SensorAppearanceProvider {
    private static final String TAG = "SensorAppearance";

    private static final BuiltInSensorAppearance UNKNOWN_SENSOR_APPEARANCE =
            new BuiltInSensorAppearance(R.string.unknown_sensor, R.drawable.ic_sensors_white_24dp);

    private Map<String, SensorAppearance> mAppearances = new HashMap<>();

    private DataController mDataController;

    @Override
    public void loadAppearances(final MaybeConsumer<Success> onSuccess) {
        mDataController.getExternalSensors(new LoggingConsumer<Map<String, ExternalSensorSpec>>(TAG,
                "load external sensors from database") {
            @Override
            public void success(Map<String, ExternalSensorSpec> value) {
                for (Map.Entry<String, ExternalSensorSpec> entry : value.entrySet()) {
                    putExternalSensorAppearance(entry.getKey(), entry.getValue());
                }
                Success.succeeded(onSuccess);
            }
        });
    }

    public SensorAppearanceProviderImpl(DataController dataController) {
        mDataController = Preconditions.checkNotNull(dataController);
        // If we add new on-device sensors/icons, they need to be added here.

        // TODO: add these when the sensors are added to the adapter?
        putAppearance(AccelerometerSensor.Axis.X.getSensorId(), new BuiltInSensorAppearance(
                R.string.acc_x, R.drawable.ic_sensor_acc_x_white_24dp, R.string.acc_units,
                R.string.sensor_desc_short_acc_x, R.string.sensor_desc_first_paragraph_acc,
                R.string.sensor_desc_second_paragraph_acc, R.drawable.learnmore_acc,
                new SensorAnimationBehavior(R.drawable.accx_level_drawable,
                        SensorAnimationBehavior.TYPE_ACCELEROMETER_SCALE)));

        putAppearance(AccelerometerSensor.Axis.Y.getSensorId(), new BuiltInSensorAppearance(
                R.string.acc_y, R.drawable.ic_sensor_acc_y_white_24dp, R.string.acc_units,
                R.string.sensor_desc_short_acc_y, R.string.sensor_desc_first_paragraph_acc,
                R.string.sensor_desc_second_paragraph_acc, R.drawable.learnmore_acc,
                new SensorAnimationBehavior(R.drawable.accy_level_drawable,
                        SensorAnimationBehavior.TYPE_ACCELEROMETER_SCALE)));

        putAppearance(AccelerometerSensor.Axis.Z.getSensorId(), new BuiltInSensorAppearance(
                R.string.acc_z, R.drawable.ic_sensor_acc_z_white_24dp, R.string.acc_units,
                R.string.sensor_desc_short_acc_z, R.string.sensor_desc_first_paragraph_acc,
                R.string.sensor_desc_second_paragraph_acc, R.drawable.learnmore_acc,
                new SensorAnimationBehavior(R.drawable.accz_level_drawable,
                    SensorAnimationBehavior.TYPE_ACCELEROMETER_SCALE)));

        putAppearance(AmbientLightSensor.ID, new BuiltInSensorAppearance(R.string.ambient_light,
                R.drawable.ic_sensor_light_white_24dp, R.string.ambient_light_units,
                R.string.sensor_desc_short_light, R.string.sensor_desc_first_paragraph_light,
                R.string.sensor_desc_second_paragraph_light, R.drawable.learnmore_light,
                new SensorAnimationBehavior(R.drawable.ambient_level_drawable,
                    SensorAnimationBehavior.TYPE_RELATIVE_SCALE)));

        putAppearance(MagneticRotationSensor.ID, new BuiltInSensorAppearance(R.string.magnetic_rotation,
                R.drawable.ic_sensor_magnetometer_white_24dp, R.string.magnetic_rotation_units,
                R.string.sensor_desc_short_magnetic_rotation,
                R.string.sensor_desc_first_paragraph_magnetic_rotation,
                R.string.sensor_desc_second_paragraph_magnetic_rotation,
                R.drawable.learnmore_magnetometer,
                new SensorAnimationBehavior(R.drawable.magnetometer_level_drawable,
                        SensorAnimationBehavior.TYPE_ROTATION)));

        putAppearance(DecibelSensor.ID, new BuiltInSensorAppearance(R.string.decibel,
                R.drawable.ic_sensor_decibels_white_24dp, R.string.decibel_units,
                R.string.sensor_desc_short_decibel, R.string.sensor_desc_first_paragraph_decibel,
                R.string.sensor_desc_second_paragraph_decibel, R.drawable.learnmore_sound,
                new SensorAnimationBehavior(R.drawable.decibel_level_drawable,
                    SensorAnimationBehavior.TYPE_RELATIVE_SCALE)));

        putAppearance(BarometerSensor.ID, new BuiltInSensorAppearance(R.string.barometer,
                R.drawable.ic_sensor_barometer_white_24dp, R.string.barometer_units,
                R.string.sensor_desc_short_barometer,
                R.string.sensor_desc_first_paragraph_barometer,
                R.string.sensor_desc_second_paragraph_barometer, R.drawable.learnmore_barometer,
                new SensorAnimationBehavior(R.drawable.barometer_level_drawable,
                        SensorAnimationBehavior.TYPE_RELATIVE_SCALE)));

        putAppearance(AmbientTemperatureSensor.ID, new BuiltInSensorAppearance(
                R.string.ambient_temperature, R.drawable.ic_sensors_white_24dp,
                R.string.temperature_units, new SensorAnimationBehavior(
                R.drawable.bluetooth_level_drawable, SensorAnimationBehavior.TYPE_STATIC_ICON)));

        putAppearance(SineWavePseudoSensor.ID, new BuiltInSensorAppearance(R.string.sine_wave,
                R.drawable.ic_sensors_white_24dp));

        putAppearance(VideoSensor.ID, new BuiltInSensorAppearance(R.string.video_stream,
                R.drawable.ic_sensor_video_white_24dp));
    }

    private void putExternalSensorAppearance(String sensorId, ExternalSensorSpec sensor) {
        putAppearance(sensorId, sensor.getSensorAppearance());
    }

    private void putAppearance(String sensorId, SensorAppearance appearance) {
        mAppearances.put(sensorId, Preconditions.checkNotNull(appearance));
    }

    @Override
    public SensorAppearance getAppearance(String sensorId) {
        if (mAppearances.containsKey(sensorId)) {
            return mAppearances.get(sensorId);
        }

        // Completely unknown sensors get a default appearance here.
        return UNKNOWN_SENSOR_APPEARANCE;
    }
}
