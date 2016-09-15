package com.google.android.apps.forscience.whistlepunk;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.widget.ImageView;

public class Appearances {
    public static void applyDrawableToImageView(Drawable drawable, ImageView view, int color) {
        drawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        view.setImageDrawable(drawable);
    }

    public static String getSensorDisplayName(SensorAppearance appearance, Context context) {
        String units = appearance.getUnits(context);
        return TextUtils.isEmpty(units) ?
                appearance.getName(context) : String.format(context.getResources().getString(
                R.string.header_name_and_units), appearance.getName(context), units);
    }
}
