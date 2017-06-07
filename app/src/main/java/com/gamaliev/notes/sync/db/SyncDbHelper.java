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

import com.gamaliev.notes.entity.SyncEntry;

import java.util.Date;
import java.util.Locale;

import static com.gamaliev.notes.common.CommonUtils.getStringDateFormatSqlite;
import static com.gamaliev.notes.common.CommonUtils.showToast;
import static com.gamaliev.notes.common.db.DbHelper.SQL_SYNC_CREATE_TABLE;
import static com.gamaliev.notes.common.db.DbHelper.SQL_SYNC_DROP_TABLE;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_COLUMN_ACTION;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_COLUMN_AMOUNT;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_COLUMN_FINISHED;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_COLUMN_STATUS;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_TABLE_NAME;
import static com.gamaliev.notes.common.db.DbHelper.getDbFailMessage;
import static com.gamaliev.notes.common.db.DbHelper.getReadableDb;
import static com.gamaliev.notes.common.db.DbHelper.getWritableDb;
import static com.gamaliev.notes.sync.SyncUtils.ACTION_NOTHING;

/**
 * @author Vadim Gamaliev
 * <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class SyncDbHelper {

    /* Logger */
    @NonNull private static final String TAG = SyncDbHelper.class.getSimpleName();


    /*
        Init
     */

    private SyncDbHelper() {}
    

    /*
        ...
     */

    /**
     * Insert sync entry in database.
     * @param context   Context.
     * @param entry     Sync entry.
     * @return {@code true} if ok, else {@code false}.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static boolean insertEntry(
            @NonNull final Context context,
            @NonNull final SyncEntry entry) {

        try {
            final SQLiteDatabase db = getWritableDb(context);
            if (db == null) {
                throw new SQLiteException(getDbFailMessage());
            }

            final String utcFinishedDate =
                    getStringDateFormatSqlite(
                            context,
                            entry.getFinished() == null ? new Date() : entry.getFinished(),
                            true);
            final Integer action = entry.getAction() == null ? ACTION_NOTHING : entry.getAction();
            final Integer status = entry.getStatus() == null ? 0 : entry.getStatus();
            final Integer amount = entry.getAmount() == null ? 0 : entry.getAmount();

            final ContentValues cv = new ContentValues();
            cv.put(SYNC_COLUMN_FINISHED,    utcFinishedDate);
            cv.put(SYNC_COLUMN_ACTION,      action);
            cv.put(SYNC_COLUMN_STATUS,      status);
            cv.put(SYNC_COLUMN_AMOUNT,      amount);

            if (db.insert(SYNC_TABLE_NAME, null, cv) == -1) {
                final String error = String.format(Locale.ENGLISH,
                        "[ERROR] Insert entry {%s: %s, %s: %d, %s: %d, %s: %d}",
                        SYNC_COLUMN_FINISHED,   utcFinishedDate,
                        SYNC_COLUMN_ACTION,     action,
                        SYNC_COLUMN_STATUS,     status,
                        SYNC_COLUMN_AMOUNT,     amount);
                throw new SQLiteException(error);
            }

            return true;

        } catch (SQLiteException e) {
            Log.e(TAG, e.toString());
            showToast(getDbFailMessage(), Toast.LENGTH_SHORT);
        }

        return false;
    }

    /**
     * Get all entries from database, order by finish date.
     * @return Result cursor.
     */
    @Nullable
    static Cursor getAll(@NonNull final Context context) {

        try {
            final SQLiteDatabase db = getReadableDb(context);
            if (db == null) {
                throw new SQLiteException(getDbFailMessage());
            }
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
            showToast(getDbFailMessage(), Toast.LENGTH_SHORT);
        }

        return null;
    }

    /**
     * Delete all rows from Sync journal table.
     * @return true if ok, otherwise false.
     */
    public static boolean clear(
            @NonNull final Context context) {

        try {
            final SQLiteDatabase db = getWritableDb(context);
            if (db == null) {
                throw new SQLiteException(getDbFailMessage());
            }

            db.beginTransaction();
            try {
                db.execSQL(SQL_SYNC_DROP_TABLE);
                db.execSQL(SQL_SYNC_CREATE_TABLE);
                db.setTransactionSuccessful();
                return true;

            } finally {
                db.endTransaction();
            }

        } catch (SQLiteException e) {
            Log.e(TAG, e.toString());
            showToast(getDbFailMessage(), Toast.LENGTH_SHORT);
        }

        return false;
    }
}
