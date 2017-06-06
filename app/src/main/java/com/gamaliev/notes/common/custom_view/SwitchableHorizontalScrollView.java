package com.gamaliev.notes.common.custom_view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

/**
 * HorizontalScrollView with the ability to turn the scroll on and off.<br>
 * {@link #setEnableScrolling(boolean)}     - set on/off scroll.<br>
 * {@link #isEnableScrolling()}             - get on/off scroll status.
 */
public class SwitchableHorizontalScrollView extends HorizontalScrollView {

    /* ... */
    private boolean mEnableScrolling = true;


    /*
        Init
     */

    public SwitchableHorizontalScrollView(
            @NonNull final Context context,
            @NonNull final AttributeSet attrs,
            final int defStyle) {

        super(context, attrs, defStyle);
    }

    public SwitchableHorizontalScrollView(
            @NonNull final Context context,
            @NonNull final AttributeSet attrs) {

        super(context, attrs);
    }

    public SwitchableHorizontalScrollView(@NonNull final Context context) {
        super(context);
    }


    /*
        Lifecycle
     */

    @Override
    public boolean onInterceptTouchEvent(final MotionEvent ev) {
        return isEnableScrolling() && super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent ev) {
        return isEnableScrolling() && super.onTouchEvent(ev);
    }


    /*
        ...
     */

    public boolean isEnableScrolling() {
        return mEnableScrolling;
    }

    public void setEnableScrolling(final boolean enableScrolling) {
        mEnableScrolling = enableScrolling;
    }
}
