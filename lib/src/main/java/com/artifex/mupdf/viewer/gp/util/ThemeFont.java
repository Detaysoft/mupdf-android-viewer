package com.artifex.mupdf.viewer.gp.util;

import android.content.Context;
import android.graphics.Typeface;

public class ThemeFont {

    private static ThemeFont instance;

    public ThemeFont() {

    }

    public static ThemeFont getInstance() {
        if (instance == null)
            instance = new ThemeFont();

        return instance;
    }

    public Typeface getLightFont(Context context) {
        try {
            return Typeface.createFromAsset(context.getAssets(), "fonts/Barlow-Light.ttf");
        } catch (Exception e) {
            return Typeface.DEFAULT;
        }
    }

    public Typeface getRegularFont(Context context) {
        try {
            return Typeface.createFromAsset(context.getAssets(), "fonts/Barlow-Regular.ttf");
        } catch (Exception e) {
            return Typeface.DEFAULT;
        }
    }

    public Typeface getMediumFont(Context context) {
        try {
            return Typeface.createFromAsset(context.getAssets(), "fonts/Barlow-Medium.ttf");
        } catch (Exception e) {
            return Typeface.DEFAULT;
        }
    }

    public Typeface getItalicFont(Context context) {
        try {
            return Typeface.createFromAsset(context.getAssets(), "fonts/Barlow-Italic.ttf");
        } catch (Exception e) {
            return Typeface.DEFAULT;
        }
    }

    public Typeface getLightItalicFont(Context context) {
        try {
            return Typeface.createFromAsset(context.getAssets(), "fonts/Barlow-LightItalic.ttf");
        } catch (Exception e) {
            return Typeface.DEFAULT;
        }
    }

    public Typeface getSemiBoldFont(Context context) {
        try {
            return Typeface.createFromAsset(context.getAssets(), "fonts/Barlow-SemiBold.ttf");
        } catch (Exception e) {
            return Typeface.DEFAULT;
        }
    }

    public Typeface getMediumItalicFont(Context context) {
        try {
            return Typeface.createFromAsset(context.getAssets(), "fonts/Barlow-MediumItalic.ttf");
        } catch (Exception e) {
            return Typeface.DEFAULT;
        }
    }

    public Typeface getSemiBoldItalicFont(Context context) {
        try {
            return Typeface.createFromAsset(context.getAssets(), "fonts/Barlow-SemiBoldItalic.ttf");
        } catch (Exception e) {
            return Typeface.DEFAULT;
        }
    }
}
