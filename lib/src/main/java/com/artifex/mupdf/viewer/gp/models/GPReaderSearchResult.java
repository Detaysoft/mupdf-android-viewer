package com.artifex.mupdf.viewer.gp.models;

/**
 * Created by p1025 on 16.08.2016.
 */

public class GPReaderSearchResult {

    private String text;
    private int page;

    public GPReaderSearchResult(){

    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}
