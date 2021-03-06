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

import static junit.framework.Assert.assertEquals;

import android.net.Uri;

import com.google.android.apps.forscience.javalib.Consumer;
import com.google.android.apps.forscience.whistlepunk.data.GoosciSensorLayout;
import com.google.android.apps.forscience.whistlepunk.metadata.BleSensorSpec;
import com.google.android.apps.forscience.whistlepunk.metadata.SensorTrigger;
import com.google.android.apps.forscience.whistlepunk.sensorapi.FakeBleClient;
import com.google.android.apps.forscience.whistlepunk.sensorapi.ManualSensor;
import com.google.android.apps.forscience.whistlepunk.sensorapi.MemorySensorEnvironment;
import com.google.android.apps.forscience.whistlepunk.sensorapi.RecordingSensorObserver;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorChoice;
import com.google.android.apps.forscience.whistlepunk.sensorapi.StubStatusListener;
import com.google.android.apps.forscience.whistlepunk.sensordb.InMemorySensorDatabase;

import org.junit.Test;

import java.util.Collections;

public class RecorderControllerTest {
    @Test
    public void multipleObservers() {
        final String sensorId = "sensorId";
        final ManualSensor sensor = new ManualSensor(sensorId, 100, 100);
        SensorRegistry registry = new SensorRegistry() {
            @Override
            public void withSensorChoice(String id, Consumer<SensorChoice> consumer) {
                assertEquals(sensorId, id);
                consumer.take(sensor);
            }
        };
        MemorySensorEnvironment env = new MemorySensorEnvironment(
                new InMemorySensorDatabase().makeSimpleRecordingController(),
                new FakeBleClient(null), new MemorySensorHistoryStorage());
        RecorderControllerImpl rc = new RecorderControllerImpl(null, registry, env,
                new RecorderListenerRegistry(), null, null);
        RecordingSensorObserver observer1 = new RecordingSensorObserver();
        RecordingSensorObserver observer2 = new RecordingSensorObserver();

        sensor.pushValue(0, 0);
        String id1 = rc.startObserving(sensorId, Collections.<SensorTrigger>emptyList(), observer1,
                new StubStatusListener(), null);
        sensor.pushValue(1, 1);
        String id2 = rc.startObserving(sensorId, Collections.<SensorTrigger>emptyList(), observer2,
                new StubStatusListener(), null);
        sensor.pushValue(2, 2);
        rc.stopObserving(sensorId, id1);
        sensor.pushValue(3, 3);
        rc.stopObserving(sensorId, id2);
        sensor.pushValue(4, 4);

        TestData.allPointsBetween(1, 2, 1).checkObserver(observer1);
        TestData.allPointsBetween(2, 3, 1).checkObserver(observer2);
    }

    @Test
    public void layoutLogging() {
        final String sensorId = "sensorId";
        final ManualSensor sensor = new ManualSensor(sensorId, 100, 100);
        SensorRegistry registry = new SensorRegistry() {
            @Override
            public void withSensorChoice(String id, Consumer<SensorChoice> consumer) {
                assertEquals(sensorId, id);
                consumer.take(sensor);
            }
        };
        MemorySensorEnvironment env = new MemorySensorEnvironment(
                new InMemorySensorDatabase().makeSimpleRecordingController(),
                new FakeBleClient(null), new MemorySensorHistoryStorage());
        RecorderControllerImpl rc = new RecorderControllerImpl(null, registry, env,
                new RecorderListenerRegistry(), null, null);

        GoosciSensorLayout.SensorLayout layout = new GoosciSensorLayout.SensorLayout();
        layout.sensorId = "aa:bb:cc:dd";
        layout.cardView = GoosciSensorLayout.SensorLayout.GRAPH;
        layout.audioEnabled = false;
        String loggingId = BleSensorSpec.TYPE;

        assertEquals("bluetooth_le|graph|audioOff", rc.getLayoutLoggingString(loggingId, layout));

        layout.cardView = GoosciSensorLayout.SensorLayout.METER;
        assertEquals("bluetooth_le|meter|audioOff", rc.getLayoutLoggingString(loggingId, layout));

        layout.audioEnabled = true;
        assertEquals("bluetooth_le|meter|audioOn", rc.getLayoutLoggingString(loggingId, layout));

        layout.sensorId = "AmbientLight";
        assertEquals("AmbientLight|meter|audioOn", rc.getLayoutLoggingString("AmbientLight",
                layout));
    }
}