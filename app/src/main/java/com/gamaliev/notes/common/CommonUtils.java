package com.gamaliev.notes.common;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.gamaliev.notes.R;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_SYMBOL_DATE_SPLIT;

/**
 * @author Vadim Gamaliev
 * <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class CommonUtils {

    /* Logger */
    private static final String TAG = CommonUtils.class.getSimpleName();

    /* Extra */
    public static final int EXTRA_DATES_FROM_DATETIME               = 0;
    public static final int EXTRA_DATES_FROM_DATE_UTC_TO_LOCALTIME  = 1;
    public static final int EXTRA_DATES_FROM_DATE_LOCALTIME_TO_UTC  = 2;
    public static final int EXTRA_DATES_FROM_DATE                   = 3;
    public static final int EXTRA_DATES_TO_DATETIME                 = 4;
    public static final int EXTRA_DATES_TO_DATE_UTC_TO_LOCALTIME    = 5;
    public static final int EXTRA_DATES_TO_DATE_LOCALTIME_TO_UTC    = 6;
    public static final int EXTRA_DATES_TO_DATE                     = 7;
    public static final int EXTRA_DATES_BOTH                        = 8;

    /* Animation */
    public static final int EXTRA_REVEAL_ANIM_CENTER_CENTER         = 101;
    public static final int EXTRA_REVEAL_ANIM_CENTER_TOP_END        = 102;


    /*
        Init
     */

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
     * @param view              Animated view.
     * @param animationCenter   Center of animation start.
     * @param duration          Duration of the animation. If -1, then using default value.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void circularRevealAnimationOn(
            @NonNull final View view,
            final int animationCenter,
            final int duration) {

        int x = 0;
        int y = 0;
        float radius = 0;

        switch(animationCenter) {
            case EXTRA_REVEAL_ANIM_CENTER_CENTER:
                x = view.getWidth() / 2;
                y = view.getHeight() / 2;
                radius = (float) Math.hypot(x, y);
                break;

            case EXTRA_REVEAL_ANIM_CENTER_TOP_END:
                x = view.getWidth();
                y = 0;
                radius = (float) Math.hypot(view.getWidth(), view.getHeight());
                break;

            default:
                break;
        }

        //
        final Animator anim =
                ViewAnimationUtils.createCircularReveal(view, x, y, 0, radius);
        if (duration > 0) {
            anim.setDuration(duration);
        }

        // Make the view visible and start the animation.
        view.setVisibility(View.VISIBLE);
        anim.start();
    }

    /**
     * Animate hide circular reveal effect on given view. Work if API >= 21.
     * @param view              Animated view.
     * @param animationCenter   Center of animation end.
     * @param duration          Duration of the animation. If -1, then using default value.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void circularRevealAnimationOff(
            @NonNull final View view,
            final int animationCenter,
            final int duration) {

        /*
            Why the condition? Fix bug with change orientation.
            [FATAL ERROR] Cannot start animation with detached view.
        */
        if (view.isAttachedToWindow()) {

            int x = 0;
            int y = 0;
            float radius = 0;

            switch(animationCenter) {
                case EXTRA_REVEAL_ANIM_CENTER_CENTER:
                    x = view.getWidth() / 2;
                    y = view.getHeight() / 2;
                    radius = (float) Math.hypot(x, y);
                    break;

                case EXTRA_REVEAL_ANIM_CENTER_TOP_END:
                    x = view.getWidth();
                    y = 0;
                    radius = (float) Math.hypot(view.getWidth(), view.getHeight());
                    break;

                default:
                    break;
            }

            //
            final Animator anim =
                    ViewAnimationUtils.createCircularReveal(view, x, y, radius, 0);
            if (duration > 0) {
                anim.setDuration(duration);
            }

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
     * Shows a toast-message on Ui thread of given activity.<br>
     * See also: {@link Toast#makeText(Context, CharSequence, int)}<br>
     *
     * @param context   Context.
     * @param message   Message to show.
     * @param duration  Duration of shows.
     */
    public static void showToastRunOnUiThread(
            @NonNull final Context context,
            @NonNull final String message,
            final int duration) {

        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                showToast(context, message, duration);
            }
        });
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
                    (int) resources.getDimension(R.dimen.fragment_color_picker_box_border_width),
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

        int color;
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
     *          Example pattern: "yyyy-MM-dd'T'HH:mm:ssZZZZZ", "2017-04-22T21:25:35+05:00".
     */
    @NonNull
    public static String getStringDateISO8601(
            @NonNull final Context context,
            @NonNull final Date date) {
        return getDateFormatISO8601(context).format(date);
    }

    /**
     * @param context   Context.
     * @param utcDate   Date, whose will be converted to String in ISO-8610 format.
     * @return  String, representing a date in ISO-8601 format.<br>
     *          Example pattern: "yyyy-MM-dd'T'HH:mm:ssZZZZZ", "2017-04-22T21:25:35+05:00".
     */
    @Nullable
    public static String getStringDateISO8601(
            @NonNull final Context context,
            @NonNull final String utcDate) {

        final DateFormat df = getDateFormatSqlite(context, true);
        Date date = null;
        try {
            date = df.parse(utcDate);
        } catch (ParseException e) {
            Log.e(TAG, e.toString());
        }

        return getDateFormatISO8601(context).format(date);
    }

    /**
     * @param context   Context.
     * @param iso8601   String in ISO 8601 format.
     * @return          Date.
     */
    @Nullable
    public static Date getDateFromISO8601String(
            @NonNull final Context context,
            @NonNull final String iso8601) {

        final DateFormat df = getDateFormatISO8601(context);
        Date date = null;
        try {
            date = df.parse(iso8601);
        } catch (ParseException e) {
            Log.e(TAG, e.toString());
        }

        return date;
    }

    /**
     * @param context Context.
     * @return  DateFormat with ISO-8601 pattern.<br>
     *          Example pattern: "yyyy-MM-dd'T'HH:mm:ssZZZZZ", "2017-04-22T21:25:35+05:00".
     */
    @NonNull
    public static DateFormat getDateFormatISO8601(@NonNull final Context context) {
        return new SimpleDateFormat(
                context.getResources().getString(R.string.pattern_iso_8601),
                Locale.ENGLISH);
    }


    /*
        UTC <- -> Localtime
     */

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

    /**
     * Convert string from UTC-format to Localtime-format.
     * @param context   Context.
     * @param utcString String with UTC format.
     * @return          String with Localtime format.
     */
    @Nullable
    public static String convertUtcToLocal(
            @NonNull final Context context,
            @NonNull final String utcString) {

        try {
            // Parse UTC string to date.
            final Date parsedUtcDate = getDateFormatSqlite(context, true).parse(utcString);

            // Get localtime string from date and return.
            return getDateFormatSqlite(context, false).format(parsedUtcDate);

        } catch (ParseException e) {
            Log.e(TAG, e.toString());
        }

        return null;
    }

    /**
     * Convert string from Localtime-format to UTC-format.
     * @param context           Context.
     * @param localtimeString   String with Localtime format.
     * @return                  String with UTC format.
     */
    @Nullable
    public static String convertLocalToUtc(
            @NonNull final Context context,
            @NonNull final String localtimeString) {

        try {
            // Parse Localtime string to date.
            final Date parsedLocaltimeDate = getDateFormatSqlite(context, false).parse(localtimeString);

            // Get UTC string from date and return.
            return getDateFormatSqlite(context, true).format(parsedLocaltimeDate);

        } catch (ParseException e) {
            Log.e(TAG, e.toString());
        }

        return null;
    }

    /**
     * Get date in different formats.
     *
     * @param context           Context.
     * @param profileMap        Profile.
     * @param filterCategory    {@link com.gamaliev.notes.common.shared_prefs.SpFilterProfiles#SP_FILTER_CREATED},
     *                          {@link com.gamaliev.notes.common.shared_prefs.SpFilterProfiles#SP_FILTER_EDITED},
     *                          {@link com.gamaliev.notes.common.shared_prefs.SpFilterProfiles#SP_FILTER_VIEWED}.
     * @param fromToBothResult  EXTRA_DATES_*.
     * @return                  date in different formats.
     */
    @Nullable
    public static String getDateFromProfileMap(
            @NonNull final Context context,
            @NonNull final Map<String, String> profileMap,
            @NonNull final String filterCategory,
            final int fromToBothResult) {

        // Init.
        final String dates = profileMap.get(filterCategory);

        // Check
        if (TextUtils.isEmpty(dates)) {
            return null;
        }

        // Get both dates.
        final String[] datesBoth = dates.split(SP_FILTER_SYMBOL_DATE_SPLIT);
        final String dateFrom = datesBoth[0];
        final String dateTo = datesBoth[1];

        switch (fromToBothResult) {
            case EXTRA_DATES_FROM_DATETIME:
                return dateFrom;

            case EXTRA_DATES_FROM_DATE:
                return dateFrom.split(" ")[0];

            case EXTRA_DATES_FROM_DATE_UTC_TO_LOCALTIME:
                return convertUtcToLocal(context, dateFrom).split(" ")[0];

            case EXTRA_DATES_FROM_DATE_LOCALTIME_TO_UTC:
                return convertLocalToUtc(context, dateFrom).split(" ")[1];

            case EXTRA_DATES_TO_DATETIME:
                return dateTo;

            case EXTRA_DATES_TO_DATE:
                return dateTo.split(" ")[0];

            case EXTRA_DATES_TO_DATE_UTC_TO_LOCALTIME:
                return convertUtcToLocal(context, dateTo).split(" ")[0];

            case EXTRA_DATES_TO_DATE_LOCALTIME_TO_UTC:
                return convertLocalToUtc(context, dateTo).split(" ")[1];

            case EXTRA_DATES_BOTH:
                return dates;

            default:
                return null;
        }
    }


    /*
        Dialogs
     */

    /**
     * Creating standard information message dialog, with "OK" button.
     * @param context   Context.
     * @param title     Title of dialog.
     * @param message   Body message of dialog.
     */
    public static void showMessageDialog(
            @NonNull final Context context,
            @Nullable final String title,
            @NonNull final String message) {

        // Create builder.
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);

        // Set title, if exist.
        if (title != null) {
            dialogBuilder.setTitle(title);
        }

        // Set message.
        dialogBuilder.setMessage(message);

        // Init OK button.
        dialogBuilder.setPositiveButton(
                R.string.common_utils_message_dialog_ok_action,
                null);

        // Show dialog.
        dialogBuilder.show();
    }


    /*
        Permissions
     */

    /**
     * Checking permission.
     * If permission denied, then requesting permission from the user, then return result to given activity.
     *
     * @param activity      Activity.
     * @param permission    Checked permission. See: {@link android.Manifest.permission}
     * @param requestCode   Request code, whose will be returned to given activity.
     *                      See: {@link android.support.v4.app.ActivityCompat#requestPermissions(Activity, String[], int)}
     * @return True if permission granted, otherwise false.
     */
    public static boolean checkAndRequestPermissions(
            @NonNull final Activity activity,
            @NonNull final String permission,
            final int requestCode) {

        // Check write external storage permission.
        if (ContextCompat.checkSelfPermission(
                activity,
                permission)
                != PackageManager.PERMISSION_GRANTED) {

            // Request the permission.
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{permission},
                    requestCode);

            // If denied.
            return false;
        }

        // If granted.
        return true;
    }


    /*
        Threads
     */

    /**
     * Thread, contains {@link android.os.Handler}, whose running into {@link android.os.Looper}.
     */
    public static class LooperHandlerThread extends Thread {
        @Nullable private Handler mHandler;

        @Override
        public void run() {
            Looper.prepare();
            mHandler = new Handler();
            Looper.loop();
        }

        @Nullable
        public Handler getHandler() {
            return mHandler;
        }
    }

    public static Handler getMainHandler() {
        return new Handler(Looper.getMainLooper());
    }


    /*
        Input
     */

    public static void hideKeyboard(
            @NonNull final Context context,
            @NonNull final View view) {

        final InputMethodManager imm =
                (InputMethodManager) context.getSystemService(
                        context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}