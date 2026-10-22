package com.lecturecap.model;

import android.graphics.Rect;

public class OcrElement {
    private final String text;
    private final int left;
    private final int top;
    private final int right;
    private final int bottom;

    public OcrElement(String text, Rect rect) {
        this.text = text != null ? text.trim() : "";
        if (rect != null) {
            this.left = rect.left;
            this.top = rect.top;
            this.right = rect.right;
            this.bottom = rect.bottom;
        } else {
            this.left = 0;
            this.top = 0;
            this.right = 0;
            this.bottom = 0;
        }
    }

    public OcrElement(String text, int left, int top, int right, int bottom) {
        this.text = text != null ? text.trim() : "";
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public String getText() {
        return text;
    }

    public int getLeft() {
        return left;
    }

    public int getTop() {
        return top;
    }

    public int getRight() {
        return right;
    }

    public int getBottom() {
        return bottom;
    }

    public int getCenterX() {
        return (left + right) / 2;
    }

    public int getCenterY() {
        return (top + bottom) / 2;
    }

    public int getWidth() {
        return right - left;
    }

    public int getHeight() {
        return bottom - top;
    }

    @Override
    public String toString() {
        return "OcrElement{" +
                "text='" + text + '\'' +
                ", box=[" + left + "," + top + "," + right + "," + bottom + "]" +
                '}';
    }
}
