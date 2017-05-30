package com.gamaliev.notes.common;

import android.os.Build;
import android.support.annotation.NonNull;
import android.view.View;

import com.gamaliev.notes.R;

import static com.gamaliev.notes.common.CommonUtils.circularRevealAnimationOff;
import static com.gamaliev.notes.common.CommonUtils.circularRevealAnimationOn;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class DialogFragmentUtils {

    /*
        Init
     */

    private DialogFragmentUtils() {}


    /*
        ...
     */

    /**
     * Add circular reveal animation to dialog view component, on Open/Close.
     * Work only with API >=21.
     * @param dialog            Dialog view component.
     * @param openAnimation     True if animation is open, false is close.
     * @param centerOfAnimation Center of animation, see:
     *                          {@link com.gamaliev.notes.common.CommonUtils#EXTRA_REVEAL_ANIM_CENTER_CENTER},
     *                          {@link com.gamaliev.notes.common.CommonUtils#EXTRA_REVEAL_ANIM_CENTER_TOP_END},
     */
    public static void initCircularRevealAnimation(
            @NonNull final View dialog,
            final boolean openAnimation,
            final int centerOfAnimation) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dialog.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v,
                                           int left, int top, int right, int bottom,
                                           int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    v.removeOnLayoutChangeListener(this);
                    if (openAnimation) {
                        circularRevealAnimationOn(
                                v.getRootView(),
                                centerOfAnimation,
                                v.getResources().getInteger(R.integer.circular_reveal_animation_value));
                    } else {
                        circularRevealAnimationOff(
                                v.getRootView(),
                                centerOfAnimation,
                                v.getResources().getInteger(R.integer.circular_reveal_animation_value));
                    }
                }
            });
        }
    }
}
