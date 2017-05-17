package com.gamaliev.notes.sync.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Random;

import static com.gamaliev.notes.common.db.DbHelper.SYNC_COLUMN_ACTION;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_COLUMN_AMOUNT;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_COLUMN_FINISHED;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_COLUMN_STATUS;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_TABLE_NAME;
import static com.gamaliev.notes.common.db.DbHelper.getWritableDb;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class SyncDbMockHelper {

    /* Logger */
    private static final String TAG = SyncDbMockHelper.class.getSimpleName();

    /* Mock data */
    @NonNull public static final int[] STATUS = {
            SyncDbHelper.STATUS_ERROR,
            SyncDbHelper.STATUS_OK
    };

    @NonNull public static final int[] ACTION = {
            SyncDbHelper.ACTION_ADDED_TO_SERVER,
            SyncDbHelper.ACTION_ADDED_TO_LOCAL,
            SyncDbHelper.ACTION_DELETED_FROM_SERVER,
            SyncDbHelper.ACTION_DELETED_FROM_LOCAL,
            SyncDbHelper.ACTION_UPDATED_ON_SERVER,
            SyncDbHelper.ACTION_UPDATED_ON_LOCAL,
            SyncDbHelper.ACTION_COMPLETE
    };

    private static final String[] MOCK_DATE = {
            "2017-05-10 21:25:35",
            "2017-05-09 21:25:35",
            "2017-05-08 21:25:35",
            "2017-05-07 21:25:35",
            "2017-05-06 21:25:35",
            "2017-05-05 21:25:35",
            "2017-05-04 21:25:35",
            "2017-05-03 21:25:35",
            "2017-05-02 21:25:35",
            "2017-05-01 21:25:35",
    };


    /*
        Init
     */

    private SyncDbMockHelper() {}


    /*
        Mock synchronization
     */

    /**
     * Add mock entries to synchronization journal.
     * @param context   Context.
     * @return          True if all ok, otherwise false.
     */
    public static boolean addMockSync(@NonNull final Context context) {

        final Random random = new Random();

        final SQLiteDatabase db = getWritableDb(context);
        try {
            //
            db.beginTransaction();

            for (int i = 0; i < 4; i++) {
                // Content values.
                final ContentValues cv = new ContentValues();
                cv.put(SYNC_COLUMN_FINISHED,    getRandomMockDate(random));
                cv.put(SYNC_COLUMN_ACTION,      String.valueOf(random.nextInt(ACTION.length)));
                cv.put(SYNC_COLUMN_STATUS,      String.valueOf(random.nextInt(STATUS.length)));
                cv.put(SYNC_COLUMN_AMOUNT,      String.valueOf(random.nextInt(1000) + 100));

                // Insert query.
                if (db.insert(SYNC_TABLE_NAME, null, cv) == -1) {
                    throw new SQLiteException("[ERROR] Add mock sync.");
                }
            }

            // If ok.
            db.setTransactionSuccessful();
            return true;

        } catch (SQLiteException e) {
            Log.e(TAG, e.toString());

        } finally {
            db.endTransaction();
        }

        return false;
    }

    /**
     * @param random Generator of pseudorandom numbers.
     * @return Random date from {@link #MOCK_DATE}
     */
    @NonNull
    private static String getRandomMockDate(@NonNull final Random random) {
        return MOCK_DATE[random.nextInt(MOCK_DATE.length)];
    }
}
