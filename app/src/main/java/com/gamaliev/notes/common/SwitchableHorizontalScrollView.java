package com.gamaliev.notes.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

/**
 * HorizontalScrollView with the ability to turn the scroll on and off.
 * <p>{@link #setEnableScrolling(boolean)}     - set on/off scroll.
 * <p>{@link #isEnableScrolling()}             - get on/off scroll status.
 */
public class SwitchableHorizontalScrollView extends HorizontalScrollView {
    private boolean mEnableScrolling = true;

    /**
     * @return On/Off scroll status
     */
    public boolean isEnableScrolling() {
        return mEnableScrolling;
    }

    /**
     * @param enableScrolling True to enable scrolling, false to disable.
     */
    public void setEnableScrolling(final boolean enableScrolling) {
        mEnableScrolling = enableScrolling;
    }

    public SwitchableHorizontalScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public SwitchableHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SwitchableHorizontalScrollView(Context context) {
        super(context);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return isEnableScrolling() && super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return isEnableScrolling() && super.onTouchEvent(ev);
    }
}
