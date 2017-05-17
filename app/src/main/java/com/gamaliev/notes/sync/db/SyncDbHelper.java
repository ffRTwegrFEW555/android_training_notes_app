package com.gamaliev.notes.sync.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.gamaliev.notes.R;
import com.gamaliev.notes.model.SyncEntry;

import java.util.Date;
import java.util.Locale;

import static com.gamaliev.notes.common.CommonUtils.getStringDateFormatSqlite;
import static com.gamaliev.notes.common.CommonUtils.showToast;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_COLUMN_AMOUNT;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_COLUMN_FINISHED;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_COLUMN_STATUS;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_TABLE_NAME;
import static com.gamaliev.notes.common.db.DbHelper.getDbFailMessage;
import static com.gamaliev.notes.common.db.DbHelper.getReadableDb;
import static com.gamaliev.notes.common.db.DbHelper.getWritableDb;

/**
 * @author Vadim Gamaliev
 * <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class SyncDbHelper {

    /* Logger */
    private static final String TAG = SyncDbHelper.class.getSimpleName();

    /* ... */
    public static final int STATUS_OK       = 1;
    public static final int STATUS_ERROR    = 0;

    public static final int[] STATUS_TEXT = {
            R.string.activity_sync_item_status_success,
            R.string.activity_sync_item_status_error
    };


    /*
        Init
     */

    private SyncDbHelper() {}
    

    /*
        Methods
     */

    /**
     * Insert new entry in database.
     * @param entry Entry.
     */
    public static boolean insertEntry(
            @NonNull final Context context,
            @NonNull final SyncEntry entry) {

        try {
            final SQLiteDatabase db = getWritableDb(context);

            // Variables
            final String utcFinishedDate =
                    getStringDateFormatSqlite(
                            context,
                            entry.getFinished() == null ? new Date() : entry.getFinished(),
                            true);

            final Integer status = entry.getStatus() == null ? 0 : entry.getStatus();
            final Integer amount = entry.getAmount() == null ? 0 : entry.getAmount();

            // Content values
            final ContentValues cv = new ContentValues();
            cv.put(SYNC_COLUMN_FINISHED,    utcFinishedDate);
            cv.put(SYNC_COLUMN_STATUS,      status);
            cv.put(SYNC_COLUMN_AMOUNT,      amount);

            // Insert
            if (db.insert(SYNC_TABLE_NAME, null, cv) == -1) {
                final String error = String.format(Locale.ENGLISH,
                        "[ERROR] Insert entry {%s: %s, %s: %d, %s: %d}",
                        SYNC_COLUMN_FINISHED,   utcFinishedDate,
                        SYNC_COLUMN_STATUS,     status,
                        SYNC_COLUMN_AMOUNT,     amount);
                throw new SQLiteException(error);
            }

            // If ok
            return true;

        } catch (SQLiteException e) {
            Log.e(TAG, e.toString());
            showToast(context, getDbFailMessage(), Toast.LENGTH_SHORT);
            return false;
        }
    }

    /**
     * Get all entries from database, order by finish date.
     * @return Result cursor.
     */
    @Nullable
    public static Cursor getAll(@NonNull final Context context) {

        try {
            final SQLiteDatabase db = getReadableDb(context);

            // Make query and return cursor, if ok;
            return db.query(
                    SYNC_TABLE_NAME,
                    null,
                    null,
                    null,
                    null,
                    null,
                    SYNC_COLUMN_FINISHED);

        } catch (SQLiteException e) {
            Log.e(TAG, e.toString());
            showToast(context, getDbFailMessage(), Toast.LENGTH_SHORT);
            return null;
        }
    }
}
