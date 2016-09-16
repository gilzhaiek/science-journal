package com.google.android.apps.forscience.whistlepunk;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.apps.forscience.javalib.Consumer;

public interface SensorAppearance {
    public interface LearnMoreContents {
        String getFirstParagraph();
        Drawable getDrawable();
        String getSecondParagraph();
    }

    String getName(Context context);

    String getUnits(Context context);

    Drawable getIconDrawable(Context context);

    SensorAnimationBehavior getSensorAnimationBehavior();

    String getShortDescription(Context context);

    boolean hasLearnMore();

    void loadLearnMore(Context context, Consumer<LearnMoreContents> onLoad);
}
