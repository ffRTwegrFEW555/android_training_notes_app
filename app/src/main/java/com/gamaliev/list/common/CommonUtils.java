package com.gamaliev.list.common;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.Toast;

import com.gamaliev.list.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

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
     * @param view      View, whose elevation is changes.
     * @param duration  Duration of animation.
     * @param valueTo   The value to be animated to, in px.
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
     * @param view      View, whose scale is changes.
     * @param valueTo   The value to be animated to.
     * @param duration  Duration of animation.
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
     * @param view      View, whose background color is changes.
     * @param from      Color from.
     * @param to        Color to.
     * @param factor    Factor of alpha (0.0-1.0 as 0-100%).
     * @param duration  Duration of animation.
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
     * @param color     Color, whose alpha parameter is changes.
     * @param factor    Factor of alpha (0.0-1.0 as 0-100%).
     * @return          Color with adjusted alpha parameter.
     */
    public static int adjustColorAlpha(final int color, final float factor) {
        final int a = Math.round(Color.alpha(color) * factor);
        final int r = Color.red(color);
        final int g = Color.green(color);
        final int b = Color.blue(color);
        return Color.argb(a, r, g, b);
    }


    /*
        Reveal circular animation
     */

    /**
     * Animate show circular reveal effect on given view. Work if API >= 21.
     * @param view Previously invisible view.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void circularRevealAnimationOn(@NonNull final View view) {
        // Get the center for the clipping circle.
        int cx = view.getWidth() / 2;
        int cy = view.getHeight() / 2;

        // Get the final radius for the clipping circle.
        float finalRadius = (float) Math.hypot(cx, cy);

        // Create the animator for this view (the start radius is zero).
        Animator anim =
                ViewAnimationUtils.createCircularReveal(view, cx, cy, 0, finalRadius);

        // Make the view visible and start the animation.
        view.setVisibility(View.VISIBLE);
        anim.start();
    }

    /**
     * Animate hide circular reveal effect on given view. Work if API >= 21.
     * @param view Previously visible view.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void circularRevealAnimationOff(@NonNull final View view) {

        /*
            Why the condition? Fix bug with change orientation.
            [FATAL ERROR] Cannot start animation with detached view.
        */
        if (view.isAttachedToWindow()) {

            // Get the center for the clipping circle.
            int cx = view.getWidth() / 2;
            int cy = view.getHeight() / 2;

            // Get the initial radius for the clipping circle.
            float initialRadius = (float) Math.hypot(cx, cy);

            // Create the animation (the final radius is zero).
            Animator anim =
                    ViewAnimationUtils.createCircularReveal(view, cx, cy, initialRadius, 0);

            // Make the view invisible when the animation is done.
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    view.setVisibility(View.INVISIBLE);
                }
            });

            // Start the animation.
            anim.start();
        }
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
     * @param context   Context.
     * @param type      The ringtone type whose default should be returned.
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
     * @param context   Context.
     * @param duration  Duration of vibration.
     */
    public static void makeVibrate(@NonNull final Context context, final int duration) {
        final Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(duration);
    }

    /**
     * Shows a toast-message.<br>
     * See also: {@link Toast#makeText(Context, CharSequence, int)}<br>
     *
     * @param context   Context.
     * @param message   Message to show.
     * @param duration  Duration of shows.
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
     * @param context   Context.
     * @param type      The ringtone type whose default should be returned.
     * @param message   Message to show.
     * @param duration  Duration of shows.
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
     * @param context   Context.
     * @param view      View, whose background color is set.
     * @param color     Color.
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
     * @param view  View, whose background color is set.
     * @param color Color.
     */
    public static void setBackgroundColor(@NonNull final View view, final int color) {
        view.getBackground().setColorFilter(color, PorterDuff.Mode.SRC);
    }

    /**
     * Get color from resources, depending on the api.
     * @param context       Context.
     * @param resourceColor Resource of color.
     * @return Color, associated with given resource.
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
     * @return Default color. See color resource with name "color_picker_default".
     */
    public static int getDefaultColor(@NonNull final Context context) {
        return getResourceColorApi(context, R.color.color_picker_default);
    }


    /*
        Date. ISO-8601.
     */

    /**
     * @param context   Context.
     * @param date      Date, whose will be converted to String in ISO-8610 format.
     * @return  String, representing a date in ISO-8601 format.<br>
     *          Example pattern: "yyyy-MM-dd'T'HH:mm:ssZ", "2017-04-22T21:25:35+05:00".
     */
    @NonNull
    public static String getStringDateISO8601(
            @NonNull final Context context,
            @NonNull final Date date) {
        return getDateFormatISO8601(context).format(date);
    }

    /**
     * @param context Context.
     * @return  DateFormat with ISO-8601 pattern.<br>
     *          Example pattern: "yyyy-MM-dd'T'HH:mm:ssZ", "2017-04-22T21:25:35+05:00".
     */
    @NonNull
    public static DateFormat getDateFormatISO8601(@NonNull final Context context) {
        return new SimpleDateFormat(
                context.getResources().getString(R.string.pattern_iso_8601),
                Locale.ENGLISH);
    }

    /**
     * @param context   Context.
     * @param date      Date, whose will be converted to String.
     * @param utc       If true, then set UTC time zone, otherwise original time zone.
     * @return  String, representing a UTC date sqlite format.<br>
     *          Example pattern: "YYYY-MM-DD HH:MM:SS", "2017-04-22 21:25:35".
     */
    @NonNull
    public static String getStringDateFormatSqlite(
            @NonNull final Context context,
            @NonNull final Date date,
            final boolean utc) {
        return getDateFormatSqlite(context, utc).format(date);
    }

    /**
     * @param context   Context.
     * @param utc       If true, then set UTC time zone, otherwise original time zone.
     * @return  DateFormat with sqlite pattern, convert UTC to localtime.<br>
     *          Example pattern: "YYYY-MM-DD HH:MM:SS", "2017-04-22 21:25:35".
     */
    @NonNull
    public static DateFormat getDateFormatSqlite(
            @NonNull final Context context,
            final boolean utc) {

        final DateFormat dateFormat = new SimpleDateFormat(
                context.getResources().getString(R.string.pattern_date_time_sqlite_format),
                Locale.ENGLISH);
        if (utc) {
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        return dateFormat;
    }
}