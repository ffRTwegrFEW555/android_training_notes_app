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

import com.gamaliev.list.R;
import com.gamaliev.list.common.DatabaseHelper;

import java.util.Date;
import java.util.Locale;
import java.util.Random;

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
    boolean insertEntry(@NonNull final ListEntry entry) {
        // Check nullability
        if (entry == null) {
            Log.e(TAG, "[ERROR] Insert entry, entry is null.");
            showToast(context, dbFailMessage, Toast.LENGTH_SHORT);
            return false;
        }

        SQLiteDatabase db = null;

        try {
            // Variables
            db = getWritableDatabase();
            final String name = entry.getName();
            final String description = entry.getDescription();
            final int color = entry.getColor() == null ? getDefaultColor(context) : entry.getColor();

            // Content values
            final ContentValues cv = new ContentValues();
            cv.put(LIST_ITEMS_COLUMN_NAME,                  name);
            cv.put(LIST_ITEMS_COLUMN_DESCRIPTION,           description);
            cv.put(LIST_ITEMS_COLUMN_COLOR,                 color);
            cv.put(LIST_ITEMS_COLUMN_MODIFIED_TIMESTAMP,   "time('now')");

            // Insert
            if (db.insert(LIST_ITEMS_TABLE_NAME, null, cv) == -1) {
                final String error = String.format(Locale.ENGLISH,
                        "[ERROR] Insert entry {%s: %s, %s: %s, %s: %d}",
                        LIST_ITEMS_COLUMN_NAME, name,
                        LIST_ITEMS_COLUMN_DESCRIPTION, description,
                        LIST_ITEMS_COLUMN_COLOR, color);
                throw new SQLiteException(error);
            }

            // If ok
            return true;

        } catch (SQLiteException e) {
            Log.e(TAG, e.getMessage());
            showToast(context, dbFailMessage, Toast.LENGTH_SHORT);
            return false;

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
            color = entry.getColor() == null ? getDefaultColor(context) : entry.getColor();

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
            order = ORDER_ADDING;
        }

        SQLiteDatabase db = null;

        try {
            db = getReadableDatabase();
            return db.query(LIST_ITEMS_TABLE_NAME, null, null, null, null, null,
                    order + (asc ? " ASC" : " DESC"));

        } catch (SQLiteException e) {
            Log.e(TAG, "[ERROR] Get entries: " + e.getMessage());
            showToast(context, dbFailMessage, Toast.LENGTH_SHORT);
            return null;

            // TODO: db.close -> cursor.close -> exception.
        /*} finally {
            if (db != null) {
                db.close();
            }*/
        }
    }

    // TODO: Handle
    @Nullable
    ListEntry getEntry(@NonNull Long id) {
        if (id == null) {
            return null;
        }

        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = getReadableDatabase();
            cursor = db.query(
                    LIST_ITEMS_TABLE_NAME,
                    null,
                    "_id = ?",
                    new String[] {id.toString()},
                    null, null, null);
            ListEntry entry = new ListEntry();
            if (cursor.moveToFirst()) {
                entry.setId(cursor.getInt(0));
                entry.setName(cursor.getString(1));
                entry.setDescription(cursor.getString(2));
                entry.setColor(cursor.getInt(3));
                return entry;
            }
            // TODO: add created date and modified date

        } catch (SQLiteException e) {
            Log.e(TAG, "[ERROR] Get entries: " + e.getMessage());
            showToast(context, dbFailMessage, Toast.LENGTH_SHORT);
            return null;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }

        return null;
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

    // TODO; handle
    boolean addMockEntries() {
        SQLiteDatabase db = null;

        try {
            db = getWritableDatabase();
            db.beginTransaction();
            final int itemNumbers = resources.getInteger(R.integer.mock_items_number);
            for (int i = 0; i < itemNumbers; i++) {
                // Content values
                Random random = new Random();
                final ContentValues cv = new ContentValues();
                cv.put(LIST_ITEMS_COLUMN_NAME,          resources.getString(R.string.mock_title));
                cv.put(LIST_ITEMS_COLUMN_DESCRIPTION,   resources.getString(R.string.mock_body));
                cv.put(LIST_ITEMS_COLUMN_COLOR,         random.nextInt());
                cv.put(LIST_ITEMS_COLUMN_MODIFIED_TIMESTAMP, "time('now')");

                // Insert
                if (db.insert(LIST_ITEMS_TABLE_NAME, null, cv) == -1) {
                    final String error = String.format(Locale.ENGLISH,
                            "[ERROR] Insert entry {%s: %s, %s: %s, %s: %d}",
                            LIST_ITEMS_COLUMN_NAME, "name",
                            LIST_ITEMS_COLUMN_DESCRIPTION, "description",
                            LIST_ITEMS_COLUMN_COLOR, 0);
                    throw new SQLiteException(error);
                }
            }
            db.setTransactionSuccessful();

            // If ok
            return true;

        } catch (SQLiteException e) {
            Log.e(TAG, "[ERROR] Add mock entries: " + e.getMessage());
            showToast(context, dbFailMessage, Toast.LENGTH_SHORT);
            return false;

        } finally {
            if (db != null) {
                db.endTransaction();
                db.close();
            }
        }
    }

    // TODO; handle
    boolean removeAllEntries() {
        SQLiteDatabase db = null;

        try {
            db = getWritableDatabase();
            db.beginTransaction();
            db.execSQL(SQL_LIST_ITEMS_DROP_TABLE);
            db.execSQL(SQL_LIST_ITEMS_CREATE_TABLE);
            db.setTransactionSuccessful();

            // If ok
            return true;

        } catch (SQLiteException e) {
            Log.e(TAG, "[ERROR] Remove all entries: " + e.getMessage());
            showToast(context, dbFailMessage, Toast.LENGTH_SHORT);
            return false;

        } finally {
            if (db != null) {
                db.endTransaction();
                db.close();
            }
        }
    }

}
