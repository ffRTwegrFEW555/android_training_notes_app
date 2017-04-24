package com.gamaliev.list.common;

import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.transition.ChangeBounds;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;

import com.gamaliev.list.R;

/**
 * @author Vadim Gamaliev
 * <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class CommonUtils {

    private CommonUtils() {}


    /*
        Animation
     */

    /**
     * Animates the elevation of the received view.<br>
     * Work only with API level 21 and more.<br>
     * See also: {@link android.view.ViewPropertyAnimator#z}
     * @param view      view, whose elevation is changes.
     * @param duration  duration of animation.
     * @param valueTo   the value to be animated to, in px.
     */
    public static void animateElevation(
            @NonNull final View view,
            final int duration,
            final int valueTo) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view    .animate()
                    .setDuration(duration)
                    .z(valueTo);
        }
    }

    /**
     * Animates the scale of the received view.
     * @param view      view, whose scale is changes.
     * @param valueTo   the value to be animated to.
     * @param duration  duration of animation.
     */
    public static void animateScaleXY(
            @NonNull final View view,
            final float valueTo,
            final long duration) {

        final ObjectAnimator animX = ObjectAnimator.ofFloat(view, "scaleX", valueTo);
        final ObjectAnimator animY = ObjectAnimator.ofFloat(view, "scaleY", valueTo);
        final AnimatorSet animSetXY = new AnimatorSet();
        animSetXY.playTogether(animX, animY);
        animSetXY.setDuration(duration).start();
    }

    /**
     * Smoothly changes background color from one to another of the received view.<br>
     * See also: {@link #adjustColorAlpha(int, float)}
     * @param view      view, whose background color is changes.
     * @param from      color from.
     * @param to        color to.
     * @param factor    factor of alpha (0.0-1.0 as 0-100%).
     * @param duration  duration of animation.
     */
    public static void shiftColor(
            @NonNull final View view,
            final int from,
            final int to,
            final float factor,
            final int duration) {

        final ObjectAnimator animator = ObjectAnimator.ofObject(
                view,
                "backgroundColor",
                new ArgbEvaluator(),
                factor < 0 ? from : adjustColorAlpha(from, factor),
                factor < 0 ? to : adjustColorAlpha(to, factor));

        animator
                .setDuration(duration)
                .start();
    }

    /**
     * Adjust alpha parameter of the received color.
     * @param color     color, whose alpha parameter is changes.
     * @param factor    factor of alpha (0.0-1.0 as 0-100%).
     * @return          color with adjusted alpha parameter.
     */
    public static int adjustColorAlpha(final int color, final float factor) {
        final int a = Math.round(Color.alpha(color) * factor);
        final int r = Color.red(color);
        final int g = Color.green(color);
        final int b = Color.blue(color);
        return Color.argb(a, r, g, b);
    }


    /*
        Notifications
     */

    /**
     * Plays a ringtone.<br>
     * See also:<br>
     * {@link RingtoneManager#getDefaultUri(int)}<br>
     * {@link RingtoneManager#getRingtone(Context, Uri)}
     *
     * @param context   context.
     * @param type      the ringtone type whose default should be returned.
     */
    public static void playSound(@NonNull final Context context, final int type) {
        final Uri uri = RingtoneManager.getDefaultUri(type);
        RingtoneManager
                .getRingtone(context, uri)
                .play();
    }

    /**
     * Makes a vibration.<br>
     * See also: {@link Vibrator#vibrate(long)}<br>
     *
     * @param context   context.
     * @param duration  duration of vibration.
     */
    public static void makeVibrate(@NonNull final Context context, final int duration) {
        final Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(duration);
    }

    /**
     * Shows a toast-message.<br>
     * See also: {@link Toast#makeText(Context, CharSequence, int)}<br>
     *
     * @param context   context.
     * @param message   message to show.
     * @param duration  duration of shows.
     */
    public static void showToast(
            @NonNull final Context context,
            @NonNull final String message,
            final int duration) {

        Toast   .makeText(context, message, duration)
                .show();
    }

    /**
     * Plays a ringtone and shows a toast-message.
     * See also:<br>
     * {@link #playSound(Context, int)}<br>
     * {@link #showToast(Context, String, int)}<br>
     *
     * @param context   context.
     * @param type      the ringtone type whose default should be returned.
     * @param message   message to show.
     * @param duration  duration of shows.
     */
    public static void playSoundAndShowToast(
            @NonNull final Context context,
            final int type,
            @NonNull final String message,
            final int duration) {

        playSound(context, type);
        showToast(context, message, duration);
    }


    /*
        Drawable
     */

    /**
     * Sets a background color to the received view.<br>
     * With border or not, depending on the version of the API
     *
     * @param context   context.
     * @param view      view, whose background color is set.
     * @param color     color.
     */
    public static void setBackgroundColorRectangleAPI(
            @NonNull final Context context,
            @NonNull final View view,
            final int color) {

        final Resources resources = context.getResources();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.getBackground().setColorFilter(color, PorterDuff.Mode.SRC);
            view.invalidate();

        } else {
            GradientDrawable g = new GradientDrawable();
            g.setStroke(
                    (int) resources.getDimension(R.dimen.activity_color_picker_box_border_width),
                    getResourceColorApi(context, R.color.color_white));
            g.setColor(color);
            view.setBackground(g);
        }
    }

    /**
     * Sets a background color to the received view.<br>
     * See also: {@link android.graphics.drawable.Drawable#setColorFilter(int, PorterDuff.Mode)}
     *
     * @param view      view, whose background color is set.
     * @param color     color.
     */
    public static void setBackgroundColor(@NonNull final View view, final int color) {
        view.getBackground().setColorFilter(color, PorterDuff.Mode.SRC);
    }

    /**
     * Get color from resources, depending on the api.
     * @param context       context.
     * @param resourceColor resource of color.
     * @return color.
     */
    public static int getResourceColorApi(
            @NonNull final Context context,
            final int resourceColor) {

        int color = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            color = context.getResources().getColor(resourceColor, null);
        } else {
            color = context.getResources().getColor(resourceColor);
        }
        return color;
    }

    /**
     * @return default color. See color resource with name "color_picker_default".
     */
    public static int getDefaultColor(@NonNull final Context context) {
        return getResourceColorApi(context, R.color.color_picker_default);
    }

    /**
     * Get shared transition animation object.
     * @return change bounds.
     */
    @NonNull
    public static ChangeBounds getChangeBounds(@NonNull final Context context) {
        ChangeBounds changeBounds = new ChangeBounds();
        changeBounds.setDuration(context.getResources()
                .getInteger(R.integer.shared_transition_animation_change_bounds_duration));
        changeBounds.setInterpolator(new OvershootInterpolator(0f));
        return changeBounds;
    }


    /*
        TODO: handle
      */

    @Nullable
    public static RippleDrawable getPressedColorRippleDrawable(
            final int normalColor,
            final int pressedColor) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new RippleDrawable(
                    getPressedColorSelector(normalColor, pressedColor),
                    getColorDrawableFromColor(normalColor), null);
        } else {
            return null;
        }
    }

    @NonNull
    private static ColorStateList getPressedColorSelector(
            final int normalColor,
            final int pressedColor) {

        return new ColorStateList(
                new int[][] {   new int[] {android.R.attr.state_pressed},
                                new int[] {android.R.attr.state_focused},
                                new int[] {android.R.attr.state_activated},
                                new int[] {}},
                new int[] {     pressedColor,
                                pressedColor,
                                pressedColor,
                                normalColor});
    }

    @NonNull
    private static ColorDrawable getColorDrawableFromColor(final int color) {
        return new ColorDrawable(color);
    }
}
