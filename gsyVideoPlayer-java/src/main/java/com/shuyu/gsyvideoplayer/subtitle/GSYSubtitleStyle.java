package com.shuyu.gsyvideoplayer.subtitle;

import android.graphics.Color;
import android.view.Gravity;

public class GSYSubtitleStyle {

    private final int textColor;
    private final float textSizeSp;
    private final int backgroundColor;
    private final int shadowColor;
    private final float shadowRadius;
    private final float shadowDx;
    private final float shadowDy;
    private final int bottomMarginDp;
    private final int layoutGravity;
    private final int horizontalMarginDp;
    private final int verticalMarginDp;

    public GSYSubtitleStyle(Builder builder) {
        this.textColor = builder.textColor;
        this.textSizeSp = builder.textSizeSp;
        this.backgroundColor = builder.backgroundColor;
        this.shadowColor = builder.shadowColor;
        this.shadowRadius = builder.shadowRadius;
        this.shadowDx = builder.shadowDx;
        this.shadowDy = builder.shadowDy;
        this.bottomMarginDp = builder.bottomMarginDp;
        this.layoutGravity = builder.layoutGravity;
        this.horizontalMarginDp = builder.horizontalMarginDp;
        this.verticalMarginDp = builder.verticalMarginDp;
    }

    public static GSYSubtitleStyle defaultStyle() {
        return new Builder().build();
    }

    public int getTextColor() {
        return textColor;
    }

    public float getTextSizeSp() {
        return textSizeSp;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public int getShadowColor() {
        return shadowColor;
    }

    public float getShadowRadius() {
        return shadowRadius;
    }

    public float getShadowDx() {
        return shadowDx;
    }

    public float getShadowDy() {
        return shadowDy;
    }

    public int getBottomMarginDp() {
        return bottomMarginDp;
    }

    public int getLayoutGravity() {
        return layoutGravity;
    }

    public int getHorizontalMarginDp() {
        return horizontalMarginDp;
    }

    public int getVerticalMarginDp() {
        return verticalMarginDp;
    }

    public static class Builder {
        private int textColor = Color.WHITE;
        private float textSizeSp = 16;
        private int backgroundColor = Color.TRANSPARENT;
        private int shadowColor = Color.BLACK;
        private float shadowRadius = 3;
        private float shadowDx = 1;
        private float shadowDy = 1;
        private int bottomMarginDp = 56;
        private int layoutGravity = Gravity.BOTTOM;
        private int horizontalMarginDp = 16;
        private int verticalMarginDp = 56;

        public Builder setTextColor(int textColor) {
            this.textColor = textColor;
            return this;
        }

        public Builder setTextSizeSp(float textSizeSp) {
            this.textSizeSp = textSizeSp;
            return this;
        }

        public Builder setBackgroundColor(int backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        public Builder setShadow(int shadowColor, float radius, float dx, float dy) {
            this.shadowColor = shadowColor;
            this.shadowRadius = radius;
            this.shadowDx = dx;
            this.shadowDy = dy;
            return this;
        }

        public Builder setBottomMarginDp(int bottomMarginDp) {
            this.bottomMarginDp = bottomMarginDp;
            this.layoutGravity = Gravity.BOTTOM;
            this.verticalMarginDp = bottomMarginDp;
            return this;
        }

        public Builder setLayoutGravity(int layoutGravity) {
            this.layoutGravity = layoutGravity;
            return this;
        }

        public Builder setHorizontalMarginDp(int horizontalMarginDp) {
            this.horizontalMarginDp = horizontalMarginDp;
            return this;
        }

        public Builder setVerticalMarginDp(int verticalMarginDp) {
            this.verticalMarginDp = verticalMarginDp;
            this.bottomMarginDp = verticalMarginDp;
            return this;
        }

        public Builder setPosition(int layoutGravity, int horizontalMarginDp, int verticalMarginDp) {
            this.layoutGravity = layoutGravity;
            this.horizontalMarginDp = horizontalMarginDp;
            this.verticalMarginDp = verticalMarginDp;
            this.bottomMarginDp = verticalMarginDp;
            return this;
        }

        public GSYSubtitleStyle build() {
            return new GSYSubtitleStyle(this);
        }
    }
}
