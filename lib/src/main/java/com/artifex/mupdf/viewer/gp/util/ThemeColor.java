package com.artifex.mupdf.viewer.gp.util;

import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;

public class ThemeColor {
    private static ThemeColor instance;

    private int themeType = 1;
    private String foregroundColor = "#2980B9";

    private final String DARK_THEME_COLOR = "#222222";
    private final String LIGHT_THEME_COLOR = "#ffffff";
    private final String STRONG_DARK_THEME_COLOR = "#000000";
    private final String STRONG_LIGHT_THEME_COLOR = "#ffffff";

    public static final int DARK_THEME_TYPE = 1;


    public ThemeColor() {

    }

    public static ThemeColor getInstance() {
        if (instance == null) {
            instance = new ThemeColor();
        }
        return instance;
    }

    public int getThemeColor() {
        if (themeType == DARK_THEME_TYPE)
            return Color.parseColor(DARK_THEME_COLOR);
        return Color.parseColor(LIGHT_THEME_COLOR);
    }

    public int getOppositeThemeColor() {
        if (themeType == DARK_THEME_TYPE)
            return Color.parseColor(LIGHT_THEME_COLOR);
        return Color.parseColor(DARK_THEME_COLOR);
    }

    public int getStrongThemeColor() {
        if (themeType == DARK_THEME_TYPE)
            return Color.parseColor(STRONG_DARK_THEME_COLOR);
        return Color.parseColor(STRONG_LIGHT_THEME_COLOR);
    }

    public int getStrongOppositeThemeColor() {
        if (themeType == DARK_THEME_TYPE)
            return Color.parseColor(STRONG_LIGHT_THEME_COLOR);
        return Color.parseColor(STRONG_DARK_THEME_COLOR);
    }

    public void setThemeType(int themeType) {
        this.themeType = themeType;
    }

    public void setForegroundColor(String foregroundColor) {
        this.foregroundColor = foregroundColor;
    }


    public int getForegroundColor() {
        return Color.parseColor(foregroundColor);
    }

    private int getDisabledThemeColor() {
        String DISABLED_THEME_COLOR = "#939393";
        return Color.parseColor(DISABLED_THEME_COLOR);
    }

    ColorFilter getForegroundColorFilter() {
        int color = getForegroundColor();
        int red = (color & 0xFF0000) / 0xFFFF;
        int green = (color & 0xFF00) / 0xFF;
        int blue = color & 0xFF;
        float[] matrix = {0, 0, 0, 0, red
                , 0, 0, 0, 0, green
                , 0, 0, 0, 0, blue
                , 0, 0, 0, 1, 0};
        return new ColorMatrixColorFilter(matrix);
    }

    public ColorFilter getOppositeThemeColorFilter() {
        int color = getStrongOppositeThemeColor();
        int redTheme = (color & 0xFF0000) / 0xFFFF;
        int greenTheme = (color & 0xFF00) / 0xFF;
        int blueTheme = color & 0xFF;
        float[] matrixTheme = {0, 0, 0, 0, redTheme
                , 0, 0, 0, 0, greenTheme
                , 0, 0, 0, 0, blueTheme
                , 0, 0, 0, 1, 0};
        return new ColorMatrixColorFilter(matrixTheme);
    }

    ColorFilter getDisabledThemeColorFilter() {
        int color = getDisabledThemeColor();
        int redUnselected = (color & 0xFF0000) / 0xFFFF;
        int greenUnselected = (color & 0xFF00) / 0xFF;
        int blueUnselected = color & 0xFF;
        float[] matrixUnselected = {0, 0, 0, 0, redUnselected
                , 0, 0, 0, 0, greenUnselected
                , 0, 0, 0, 0, blueUnselected
                , 0, 0, 0, 1, 0};
        return new ColorMatrixColorFilter(matrixUnselected);
    }
}
