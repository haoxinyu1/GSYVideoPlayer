package com.shuyu.gsyvideoplayer.subtitle;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

public class GSYSubtitleView extends TextView {

    private GSYSubtitleStyle style = GSYSubtitleStyle.defaultStyle();

    public GSYSubtitleView(Context context) {
        super(context);
        init();
    }

    public GSYSubtitleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GSYSubtitleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setGravity(Gravity.CENTER);
        setIncludeFontPadding(false);
        setMaxLines(3);
        setEllipsize(TextUtils.TruncateAt.END);
        setClickable(false);
        setFocusable(false);
        applyStyle(style);
        showText("");
    }

    public void applyStyle(GSYSubtitleStyle style) {
        if (style == null) {
            return;
        }
        this.style = style;
        setTextColor(style.getTextColor());
        setTextSize(TypedValue.COMPLEX_UNIT_SP, style.getTextSizeSp());
        setBackgroundColor(style.getBackgroundColor());
        setShadowLayer(style.getShadowRadius(), style.getShadowDx(), style.getShadowDy(), style.getShadowColor());
        applyPosition(style);
    }

    public void showText(String text) {
        if (TextUtils.isEmpty(text)) {
            setText("");
            setVisibility(GONE);
        } else {
            setText(text);
            setVisibility(VISIBLE);
        }
    }

    private void applyPosition(GSYSubtitleStyle style) {
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (!(layoutParams instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
        int horizontalMargin = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            style.getHorizontalMarginDp(),
            getResources().getDisplayMetrics());
        int verticalMargin = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            style.getVerticalMarginDp(),
            getResources().getDisplayMetrics());
        int topMargin = 0;
        int bottomMargin = 0;
        int verticalGravity = style.getLayoutGravity() & Gravity.VERTICAL_GRAVITY_MASK;
        if (verticalGravity == Gravity.TOP) {
            topMargin = verticalMargin;
        } else if (verticalGravity == Gravity.BOTTOM) {
            bottomMargin = verticalMargin;
        }
        marginLayoutParams.leftMargin = horizontalMargin;
        marginLayoutParams.rightMargin = horizontalMargin;
        marginLayoutParams.topMargin = topMargin;
        marginLayoutParams.bottomMargin = bottomMargin;
        if (layoutParams instanceof FrameLayout.LayoutParams) {
            ((FrameLayout.LayoutParams) layoutParams).gravity = style.getLayoutGravity();
        }
        setLayoutParams(marginLayoutParams);
    }
}
