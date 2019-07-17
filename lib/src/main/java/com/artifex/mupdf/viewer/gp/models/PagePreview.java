package com.artifex.mupdf.viewer.gp.models;

import android.graphics.Bitmap;

public class PagePreview {
    private int index;
    private Bitmap image;

    public PagePreview(int index, Bitmap image) {
        this.index = index;
        this.image = image;
    }

    public int getPageNumber() {
        return index + 1;
    }

    public int getIndex() {
        return index;
    }

    public Bitmap getImage() {
        return image;
    }
}
