package com.artifex.mupdf.viewer.gp.util;

import android.content.Context;
import android.graphics.drawable.Drawable;

public class ThemeIcon {

    private static ThemeIcon instance;

    private final static int NO_FILTER = 0;
    public final static int FOREGROUND_COLOR_FILTER = 1;
    public final static int THEME_COLOR_FILTER = 2;
    public final static int OPPOSITE_THEME_COLOR_FILTER = 3;
    public final static int DISABLED_THEME_COLOR_FILTER = 4;


    public ThemeIcon() {

    }

    public static ThemeIcon getInstance() {
        if (instance == null) {
            instance = new ThemeIcon();
        }
        return instance;
    }

    public Drawable paintIcon(Context context, int resourceId, int filterType) {

        Drawable icon = context.getResources().getDrawable(resourceId);

        switch (filterType) {
            case FOREGROUND_COLOR_FILTER:
                icon.setColorFilter(ThemeColor.getInstance().getForegroundColorFilter());
                return icon;

            case OPPOSITE_THEME_COLOR_FILTER:
                icon.setColorFilter(ThemeColor.getInstance().getOppositeThemeColorFilter());
                return icon;

            case DISABLED_THEME_COLOR_FILTER:
                icon.setColorFilter(ThemeColor.getInstance().getDisabledThemeColorFilter());
                return icon;

            case NO_FILTER:
                return icon;

            default:
                return icon;
        }
    }
}
