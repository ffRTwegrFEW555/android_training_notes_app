package com.gamaliev.list.list;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.gamaliev.list.common.DatabaseHelper;

import java.util.Date;
import java.util.Locale;

import static com.gamaliev.list.common.CommonUtils.showToast;

/**
 * @author Vadim Gamaliev
 * <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

final class ListDatabaseHelper extends DatabaseHelper {

    private static final String TAG = ListDatabaseHelper.class.getSimpleName();

    public static final String ORDER_ADDING         = "_id";
    public static final String ORDER_NAME           = LIST_ITEMS_COLUMN_NAME;
    public static final String ORDER_CREATED_DATE   = LIST_ITEMS_COLUMN_CREATED_TIMESTAMP;
    public static final String ORDER_MODIFIED_DATE  = LIST_ITEMS_COLUMN_MODIFIED_TIMESTAMP;


    /*
        Init
     */

    ListDatabaseHelper(@NonNull final Context context) {
        super(context);
    }

    ListDatabaseHelper(@NonNull final Context context,
                       @NonNull final String name,
                       @NonNull final SQLiteDatabase.CursorFactory factory,
                       final int version) {
        super(context, name, factory, version);
    }

    ListDatabaseHelper(@NonNull final Context context,
                       @NonNull final String name,
                       @NonNull final SQLiteDatabase.CursorFactory factory,
                       final int version,
                       @NonNull final DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
    }


    /*
        Methods
     */

    /**
     * Insert new entry in database.
     * @param entry entry, contains name, description, color.
     */
    private void insertEntry(@NonNull final ListEntry entry) {
        // Check nullability
        if (entry == null) {
            Log.e(TAG, "[ERROR] Insert entry, entry is null.");
            showToast(context, dbFailMessage, Toast.LENGTH_SHORT);
            return;
        }

        SQLiteDatabase db = null;

        try {
            // Variables
            db = getWritableDatabase();
            final String name = entry.getName();
            final String description = entry.getDescription();
            final int color = entry.getColor() == null ? getDefaultColor() : entry.getColor();

            // Content values
            final ContentValues cv = new ContentValues();
            cv.put(LIST_ITEMS_COLUMN_NAME,          name);
            cv.put(LIST_ITEMS_COLUMN_DESCRIPTION,   description);
            cv.put(LIST_ITEMS_COLUMN_COLOR,         color);

            // Insert
            if (db.insert(LIST_ITEMS_TABLE_NAME, null, cv) == -1) {
                final String error = String.format(Locale.ENGLISH,
                        "[ERROR] Insert entry {%s: %s, %s: %s, %s: %d}",
                        LIST_ITEMS_COLUMN_NAME, name,
                        LIST_ITEMS_COLUMN_DESCRIPTION, description,
                        LIST_ITEMS_COLUMN_COLOR, color);
                throw new SQLiteException(error);
            }

        } catch (SQLiteException e) {
            Log.e(TAG, e.getMessage());
            showToast(context, dbFailMessage, Toast.LENGTH_SHORT);

        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    /**
     * Update entry in database.
     * @param entry entry, must contains non-null id and name.
     */
    boolean updateEntry(@NonNull final ListEntry entry) {
        // Check nullability
        if (entry == null || entry.getId() == null || entry.getName() == null) {
            Log.e(TAG, "[ERROR] Update entry, id or name is null.");
            showToast(context, dbFailMessage, Toast.LENGTH_SHORT);
            return false;
        }

        SQLiteDatabase db = null;
        int id = 0;
        String name = null;
        String description = null;
        int color = 0;

        try {
            // Variables
            db = getWritableDatabase();
            id = entry.getId();
            name = entry.getName();
            description = entry.getDescription();
            color = entry.getColor() == null ? getDefaultColor() : entry.getColor();

            // Content values
            final ContentValues cv = new ContentValues();
            cv.put(LIST_ITEMS_COLUMN_NAME,                  name);
            cv.put(LIST_ITEMS_COLUMN_DESCRIPTION,           description);
            cv.put(LIST_ITEMS_COLUMN_COLOR,                 color);
            cv.put(LIST_ITEMS_COLUMN_MODIFIED_TIMESTAMP,   "time('now')");

            // Update
            final int updateResult = db.update(
                    LIST_ITEMS_TABLE_NAME,
                    cv,
                    "_id = ?",
                    new String[]{Integer.toString(id)});
            if (updateResult == 0) {
                throw new SQLiteException("The number of rows affected is 0");
            }

            // If ok
            db.close();
            return true;

        } catch (SQLiteException e) {
            final String error = String.format(
                    Locale.ENGLISH,
                    "[ERROR] Update entry {id: %d, %s: %s, %s: %s, %s: %d}",
                    id,
                    LIST_ITEMS_COLUMN_NAME, name,
                    LIST_ITEMS_COLUMN_DESCRIPTION, description,
                    LIST_ITEMS_COLUMN_COLOR, color);
            Log.e(TAG, error + ": " + e.getMessage());
            showToast(context, dbFailMessage, Toast.LENGTH_SHORT);
            return false;

        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    /**
     * Get all entries from database by order.
     *
     * @param order see
     *      {@link #ORDER_NAME},
     *      {@link #ORDER_ADDING},
     *      {@link #ORDER_CREATED_DATE},
     *      {@link #ORDER_MODIFIED_DATE}.
     *              Default is {@link #ORDER_NAME}.
     *
     * @param asc   ascending or descending of order. True if ascending, otherwise false;
     * @return cursor, with ordered entries.
     */
    @Nullable
    Cursor getAllEntries(@Nullable String order, final boolean asc) {
        if (order == null) {
            order = ORDER_NAME;
        }

        try {
            final SQLiteDatabase db = getReadableDatabase();
            return db.query(LIST_ITEMS_TABLE_NAME, null, null, null, null, null,
                    order + (asc ? " ASC" : " DESC"));

        } catch (SQLiteException e) {
            Log.e(TAG, "[ERROR] Get entries: " + e.getMessage());
            showToast(context, dbFailMessage, Toast.LENGTH_SHORT);
            return null;
        }
    }

    /**
     * Delete entry from database.
     * @param id    id of entry to be deleted.
     * @return      true if success, otherwise false.
     */
    boolean deleteEntry(@NonNull final Integer id) {
        // Check nullability
        if (id == null) {
            Log.e(TAG, "[ERROR] Delete entry, id is null.");
            showToast(context, dbFailMessage, Toast.LENGTH_SHORT);
            return false;
        }

        SQLiteDatabase db = null;

        try {
            db = getWritableDatabase();
            final int deleteResult = db.delete(LIST_ITEMS_TABLE_NAME, "_id = ?", new String[]{id.toString()});
            if (deleteResult == 0) {
                throw new SQLiteException("The number of rows affected is 0");
            }
            return true;

        } catch (SQLiteException e) {
            Log.e(TAG, "[ERROR] Delete entry: " + e.getMessage());
            showToast(context, dbFailMessage, Toast.LENGTH_SHORT);
            return false;

        } finally {
            if (db != null) {
                db.close();
            }
        }
    }
}
