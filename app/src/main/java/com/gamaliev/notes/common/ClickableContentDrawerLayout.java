package com.gamaliev.notes.common;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class ClickableContentDrawerLayout extends DrawerLayout {

    public ClickableContentDrawerLayout(@NonNull final Context context) {
        super(context);
    }

    public ClickableContentDrawerLayout(
            @NonNull final Context context,
            @NonNull final AttributeSet attrs) {

        super(context, attrs);
    }

    public ClickableContentDrawerLayout(
            @NonNull final Context context,
            @NonNull final AttributeSet attrs,
            final int defStyle) {

        super(context, attrs, defStyle);
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull final MotionEvent ev) {
        final View drawer = getChildAt(1);
        if (getDrawerLockMode(drawer) == LOCK_MODE_LOCKED_OPEN
                && ev.getRawX() > drawer.getWidth()) {
            return false;

        } else {
            return super.onInterceptTouchEvent(ev);
        }
    }
}
