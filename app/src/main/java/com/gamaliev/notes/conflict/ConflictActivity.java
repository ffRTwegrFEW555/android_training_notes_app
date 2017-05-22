package com.gamaliev.notes.conflict;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.gamaliev.notes.R;

import static com.gamaliev.notes.common.db.DbHelper.SYNC_CONFLICT_TABLE_NAME;
import static com.gamaliev.notes.common.db.DbHelper.getEntries;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class ConflictActivity extends AppCompatActivity {

    /* Logger */
    private static final String TAG = ConflictActivity.class.getSimpleName();

    /* ... */
    private static final int EXTRA_ID_CONFLICT_STATUS_BAR_NOTIFICATION = 101;


    /*
        Init
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conflict);
        init();
    }

    private void init() {
        initToolbar();
    }

    private void initToolbar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.activity_conflict_toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }


    /*
        Intents
     */

    /**
     * Start intent.
     * @param context       Context.
     * @param requestCode   This code will be returned in onActivityResult() when the activity exits.
     */
    public static void startIntent(
            @NonNull final Context context,
            final int requestCode) {

        final Intent starter = new Intent(context, ConflictActivity.class);
        ((Activity) context).startActivityForResult(starter, requestCode);
    }

    public static PendingIntent getPendingIntentForStatusBarNotification(
            @NonNull final Context context) {

        final Intent intent = new Intent(context, ConflictActivity.class);
        return PendingIntent.getActivity(context, 0, intent, 0);
    }


    /*
        Status bar notification.
     */

    public static void checkConflictedExistsAndShowNotification(
            @NonNull final Context context) {

        final Cursor cursor = getEntries(
                context,
                SYNC_CONFLICT_TABLE_NAME,
                null);

        if (cursor != null) {
            if (cursor.getCount() > 0) {
                showConflictNotification(context);
            }
            cursor.close();
        }
    }

    private static void showConflictNotification(
            @NonNull final Context context) {

        final Context appContext = context.getApplicationContext();

        final NotificationManager manager =
                (NotificationManager) appContext
                        .getSystemService(Context.NOTIFICATION_SERVICE);
        final NotificationCompat.Builder builder =
                new NotificationCompat.Builder(appContext);

        builder .setContentTitle(appContext.getString(R.string.activity_sync_notification_status_bar_conflict_exists_title))
                .setContentText(appContext.getString(R.string.activity_sync_notification_status_bar_conflict_exists_body))
                .setSmallIcon(R.drawable.ic_warning_white_24dp)
                .setContentIntent(
                        ConflictActivity.getPendingIntentForStatusBarNotification(
                                appContext))
                .setAutoCancel(true);

        manager.notify(
                EXTRA_ID_CONFLICT_STATUS_BAR_NOTIFICATION,
                builder.build());
    }
}
