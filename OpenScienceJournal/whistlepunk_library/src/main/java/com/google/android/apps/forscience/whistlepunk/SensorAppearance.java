package com.google.android.apps.forscience.whistlepunk;

import android.content.Context;
import android.graphics.drawable.Drawable;

public interface SensorAppearance {
    String getName(Context context);

    String getUnits(Context context);

    Drawable getIconDrawable(Context context);

    SensorAnimationBehavior getSensorAnimationBehavior();

    String getShortDescription(Context context);

    boolean hasLearnMore();

    String getFirstLearnMoreParagraph(Context context);

    String getSecondLearnMoreParagraph(Context context);

    Drawable getLearnMoreDrawable(Context context);
}
