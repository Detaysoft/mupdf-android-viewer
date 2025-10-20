package com.artifex.mupdf.viewer.gp.models;

public class GPNote {
    private final int id;
    private final int page; // 1-based
    private String text;
    private final long createdAt;

    public GPNote(int id, int page, String text, long createdAt) {
        this.id = id;
        this.page = page;
        this.text = text;
        this.createdAt = createdAt;
    }
    public int getId() {
        return id;
    }

    public int getPage() {
        return page;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public long getCreatedAt() {
        return createdAt;
    }
}


