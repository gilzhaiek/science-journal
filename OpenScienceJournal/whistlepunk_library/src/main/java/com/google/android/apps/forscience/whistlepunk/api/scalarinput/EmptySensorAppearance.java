package com.google.android.apps.forscience.whistlepunk.api.scalarinput;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.apps.forscience.javalib.Consumer;
import com.google.android.apps.forscience.whistlepunk.R;
import com.google.android.apps.forscience.whistlepunk.SensorAnimationBehavior;
import com.google.android.apps.forscience.whistlepunk.SensorAppearance;

class EmptySensorAppearance implements SensorAppearance {
    private static final int DEFAULT_DRAWABLE = R.drawable.ic_sensor_raw_white_24dp;

    @Override
    public String getName(Context context) {
        return "";
    }

    @Override
    public String getUnits(Context context) {
        return "";
    }

    @Override
    public Drawable getIconDrawable(Context context) {
        return context.getResources().getDrawable(DEFAULT_DRAWABLE);
    }

    @Override
    public String getShortDescription(Context context) {
        return null;
    }

    @Override
    public boolean hasLearnMore() {
        return false;
    }

    @Override
    public void loadLearnMore(Context context, Consumer<LearnMoreContents> onLoad) {
        throw new UnsupportedOperationException("I told you I don't have learnMore");
    }

    @Override
    public SensorAnimationBehavior getSensorAnimationBehavior() {
        return SensorAnimationBehavior.createDefault();
    }
}
