package com.gamaliev.notes.common;

import android.app.NotificationManager;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.gamaliev.notes.R;

import java.util.Random;

/**
 * Helper to create a progress notification.
 *
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class ProgressNotificationHelper {

    /* Logger */
    @NonNull private static final String TAG = ProgressNotificationHelper.class.getSimpleName();

    /* ... */
    @NonNull private final NotificationManager mManager;
    @NonNull private final NotificationCompat.Builder mBuilder;
    @NonNull private final String mComplete;

    private final int mId;
    private boolean mEnable;
    private boolean mFinished;


    /*
        Init
     */

    /**
     * To enable notification, you must use {@link #startTimerToEnableNotification}
     *
     * @param context  Context.
     * @param title    Title of notification.
     * @param text     Text of notification.
     * @param complete Complete text of notification.
     */
    public ProgressNotificationHelper(
            @NonNull final Context context,
            @NonNull final String title,
            @NonNull final String text,
            @NonNull final String complete) {

        mManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(context);
        mComplete = complete;

        final Random random = new Random();
        mId = random.nextInt();

        // Create notification.
        mBuilder.setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_import_export_white_24dp);
    }


    /*
        ...
     */

    /**
     * Set progress of bar notification. {@link #isEnable()} must be {@code true}.
     * @param max       Max possible value of progress.
     * @param progress  Current value of progress.
     */
    public void setProgress(
            @SuppressWarnings("SameParameterValue") final int max,
            final int progress) {

        if (isEnable()) {
            mBuilder.setProgress(max, progress, false);
            mManager.notify(mId, mBuilder.build());
        }
    }

    /**
     * Set continuing progress of bar notification. {@link #isEnable()} must be {@code true}.
     */
    @SuppressWarnings("WeakerAccess")
    public void setContinuingProgress() {
        if (isEnable()) {
            mBuilder.setProgress(0, 0, true);
            mManager.notify(mId, mBuilder.build());
        }
    }

    /**
     * Finish progress of bar notification, and show result text.
     * {@link #isEnable()} must be {@code true}.
     */
    public void endProgress() {
        mFinished = true;

        if (isEnable()) {
            mBuilder.setContentText(mComplete)
                    .setProgress(0, 0, false);

            mManager.cancel(mId);
            mManager.notify(mId, mBuilder.build());

            mEnable = false;
        }
    }

    /**
     * @param timeToStart Time to enable progress notification, in ms.
     */
    public void startTimerToEnableNotification(
            final int timeToStart,
            final boolean continuing) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(timeToStart);
                    if (!mFinished) {
                        setEnable(true);
                        if (continuing) {
                            setContinuingProgress();
                        } else {
                            setProgress(100, 0);
                        }
                    }

                } catch (InterruptedException e) {
                    Log.e(TAG, e.toString());
                }
            }
        }).start();
    }


    /*
        Setters and getters
    */

    @SuppressWarnings({"WeakerAccess", "SameParameterValue"})
    public void setEnable(final boolean enable) {
        mEnable = enable;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isEnable() {
        return mEnable;
    }
}

