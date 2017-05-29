package com.gamaliev.notes.conflict;

import android.app.NotificationManager;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import com.gamaliev.notes.R;

import static com.gamaliev.notes.common.db.DbHelper.SYNC_CONFLICT_TABLE_NAME;
import static com.gamaliev.notes.common.db.DbHelper.getEntries;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class ConflictUtils {

    /* ... */
    private static final int EXTRA_ID_CONFLICT_STATUS_BAR_NOTIFICATION = 101;


    /*
        Init
     */

    private ConflictUtils() {}


    /*
        Status bar notification.
     */

    public static void checkConflictExistsAndShowStatusBarNotification(
            @NonNull final Context context) {

        if (checkConflictingExists(context)) {
            showConflictStatusBarNotification(context);
        } else {
            hideConflictStatusBarNotification(context);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static void checkConflictExistsAndHideStatusBarNotification(
            @NonNull final Context context) {

        if (!checkConflictingExists(context)) {
            hideConflictStatusBarNotification(context);
        }
    }

    private static void showConflictStatusBarNotification(
            @NonNull final Context context) {

        final NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context);

        builder .setContentTitle(context.getString(R.string.fragment_sync_notification_status_bar_conflict_exists_title))
                .setContentText(context.getString(R.string.fragment_sync_notification_status_bar_conflict_exists_body))
                .setSmallIcon(R.drawable.ic_warning_white_24dp)
                .setAutoCancel(true);

        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(EXTRA_ID_CONFLICT_STATUS_BAR_NOTIFICATION, builder.build());
    }

    public static void hideConflictStatusBarNotification(
            @NonNull final Context context) {

        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .cancel(EXTRA_ID_CONFLICT_STATUS_BAR_NOTIFICATION);
    }


    /*
        Check conflicting exists
     */

    public static boolean checkConflictingExists(
            @NonNull final Context context) {

        final Cursor cursor = getEntries(
                context,
                SYNC_CONFLICT_TABLE_NAME,
                null);

        if (cursor != null) {
            if (cursor.getCount() > 0) {
                return true;
            }
            cursor.close();
        }

        return false;
    }
}
