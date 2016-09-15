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
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.apps.forscience.whistlepunk.SensorAppearance;
import com.google.android.apps.forscience.whistlepunk.data.GoosciScalarInput;
import com.google.android.apps.forscience.whistlepunk.metadata.ExternalSensorSpec;
import com.google.common.base.Preconditions;
import com.google.common.html.HtmlEscapers;
import com.google.protobuf.nano.InvalidProtocolBufferNanoException;

public class ScalarInputSpec extends ExternalSensorSpec {
    public static final String TYPE = "ScalarInput";
    private static final String TAG = "ScalarInputSpec";

    private String mName;
    private GoosciScalarInput.ScalarInputConfig mConfig;

    public ScalarInputSpec(String sensorName, String serviceId, String address,
            SensorAppearanceResources ids) {
        mName = sensorName;
        mConfig = new GoosciScalarInput.ScalarInputConfig();
        mConfig.serviceId = Preconditions.checkNotNull(serviceId);
        mConfig.address = address;
        writeResourceIds(mConfig, ids);
    }

    // SAFF: test this!
    private void writeResourceIds(GoosciScalarInput.ScalarInputConfig config,
            SensorAppearanceResources ids) {
        if (ids != null) {
            config.iconId = ids.iconId;
            config.units = ids.units;
            config.shortDescription = ids.shortDescription;
        }
    }

    public ScalarInputSpec(String sensorName, byte[] config) {
        mName = sensorName;
        try {
            mConfig = GoosciScalarInput.ScalarInputConfig.parseFrom(config);
        } catch (InvalidProtocolBufferNanoException e) {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, "error parsing config", e);
            }
        }
    }

    @Override
    public SensorAppearance getSensorAppearance() {
        // TODO: better icon?
        return new EmptySensorAppearance() {
            @Override
            public String getName(Context context) {
                return mName;
            }

            @Override
            public Drawable getIconDrawable(Context context) {
                if (mConfig.iconId <= 0) {
                    return super.getIconDrawable(context);
                }
                try {
                    return getResources(context).getDrawable(mConfig.iconId);
                } catch (PackageManager.NameNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public String getUnits(Context context) {
                return mConfig.units;
            }

            @Override
            public String getShortDescription(Context context) {
                return mConfig.shortDescription;
            }

            private Resources getResources(Context context)
                    throws PackageManager.NameNotFoundException {
                return context.getPackageManager().getResourcesForApplication(getServiceId());
            }
        };
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getAddress() {
        return escape(getServiceId()) + "&" + escape(getSensorAddressInService());
    }

    public String getSensorAddressInService() {
        return mConfig.address;
    }

    private String escape(String string) {
        return HtmlEscapers.htmlEscaper().escape(string);
    }

    @Override
    public byte[] getConfig() {
        return getBytes(mConfig);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getServiceId() {
        return mConfig.serviceId;
    }

}
