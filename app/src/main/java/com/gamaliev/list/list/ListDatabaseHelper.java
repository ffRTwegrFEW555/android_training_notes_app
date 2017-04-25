package com.gamaliev.list.list;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.gamaliev.list.R;
import com.gamaliev.list.common.DatabaseHelper;

import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

import static com.gamaliev.list.common.CommonUtils.getDefaultColor;
import static com.gamaliev.list.common.CommonUtils.showToast;

/**
 * @author Vadim Gamaliev
 * <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class ListDatabaseHelper extends DatabaseHelper {

    private static final String TAG = ListDatabaseHelper.class.getSimpleName();

    public static final String ORDER_ADDED      = BASE_COLUMN_ID;
    public static final String ORDER_TITLE      = LIST_ITEMS_COLUMN_TITLE;
    public static final String ORDER_CREATED    = LIST_ITEMS_COLUMN_CREATED;
    public static final String ORDER_EDITED     = LIST_ITEMS_COLUMN_EDITED;
    public static final String ORDER_VIEWED     = LIST_ITEMS_COLUMN_EDITED;
    public static final String ORDER_DEFAULT    = ORDER_ADDED;


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
     * @param entry entry, contains title, description, color.
     */
    boolean insertEntry(@NonNull final ListEntry entry) {
        try (SQLiteDatabase db = getWritableDatabase()) {

            // Variables
            final String title = entry.getTitle();
            final String description = entry.getDescription();
            final int color = entry.getColor() == null ? getDefaultColor(context) : entry.getColor();

            // Content values
            final ContentValues cv = new ContentValues();
            cv.put(LIST_ITEMS_COLUMN_TITLE,         title);
            cv.put(LIST_ITEMS_COLUMN_DESCRIPTION,   description);
            cv.put(LIST_ITEMS_COLUMN_COLOR,         color);
            cv.put(LIST_ITEMS_COLUMN_EDITED,        "time('now')");

            // Insert
            if (db.insert(LIST_ITEMS_TABLE_NAME, null, cv) == -1) {
                final String error = String.format(Locale.ENGLISH,
                        "[ERROR] Insert entry {%s: %s, %s: %s, %s: %d}",
                        LIST_ITEMS_COLUMN_TITLE, title,
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
        }
    }

    /**
     * Update entry in database.
     * @param entry entry, must contains non-null id and title.
     */
    boolean updateEntry(@NonNull final ListEntry entry) {
        int id              = 0;
        String title        = null;
        String description  = null;
        int color           = 0;

        try (SQLiteDatabase db = getWritableDatabase()) {

            // Variables
            id          = entry.getId();
            title       = entry.getTitle();
            description = entry.getDescription();
            color       = entry.getColor() == null ? getDefaultColor(context) : entry.getColor();

            // Content values
            final ContentValues cv = new ContentValues();
            cv.put(LIST_ITEMS_COLUMN_TITLE,         title);
            cv.put(LIST_ITEMS_COLUMN_DESCRIPTION,   description);
            cv.put(LIST_ITEMS_COLUMN_COLOR,         color);
            cv.put(LIST_ITEMS_COLUMN_EDITED,        "time('now')");

            // Update
            final int updateResult = db.update(
                    LIST_ITEMS_TABLE_NAME,
                    cv,
                    BASE_COLUMN_ID + " = ?",
                    new String[]{Integer.toString(id)});
            if (updateResult == 0) {
                throw new SQLiteException("The number of rows affected is 0");
            }

            // If ok
            return true;

        } catch (SQLiteException e) {
            final String error = String.format(
                    Locale.ENGLISH,
                    "[ERROR] Update entry {id: %d, %s: %s, %s: %s, %s: %d}",
                    id,
                    LIST_ITEMS_COLUMN_TITLE, title,
                    LIST_ITEMS_COLUMN_DESCRIPTION, description,
                    LIST_ITEMS_COLUMN_COLOR, color);
            Log.e(TAG, error + ": " + e.getMessage());
            showToast(context, dbFailMessage, Toast.LENGTH_SHORT);
            return false;
        }
    }

    /**
     * Get all entries from database by order.
     *
     * @param order see
     *      {@link #ORDER_TITLE},
     *      {@link #ORDER_ADDED},
     *      {@link #ORDER_CREATED},
     *      {@link #ORDER_EDITED}.
     *              Default is {@link #ORDER_TITLE}.
     *
     * @param asc   ascending or descending of order. True if ascending, otherwise false;
     * @return cursor, with ordered entries.
     */
    // TODO: refactor
    @Nullable
    Cursor getAllEntries(
            @Nullable final String searchText,
            @Nullable final String[] searchColumns,
            @Nullable String order,
            final boolean asc) {

        // Local variables
        SQLiteDatabase db = null;
        String[] searchTextConvert = null;
        String searchColumnsConvert = null;

        // Order formation
        if (order == null) {
            order = ORDER_DEFAULT + (asc ? " ASC" : " DESC");
        } else {
            order += (asc ? " ASC" : " DESC");
        }


        if (searchText != null && searchColumns != null) {

            // Convert searchColumns array to String selection query.
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < searchColumns.length; i++) {
                if (i != 0) {
                    sb.append(" OR ");
                }
                sb.append(searchColumns[i]);
                sb.append(" LIKE ?");
            }
            searchColumnsConvert = sb.toString();

            // Convert searchText string to args array.
            searchTextConvert = new String[searchColumns.length];
            Arrays.fill(searchTextConvert, "%" + searchText + "%");
        }

        try {
            db = getReadableDatabase();
            return db.query(
                    LIST_ITEMS_TABLE_NAME,
                    null,
                    searchColumns   == null ? null : searchColumnsConvert,
                    searchText      == null ? null : searchTextConvert,
                    null,
                    null,
                    order);

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

    /**
     * Get filled object from database, with given id.
     * @param id id of entry.
     * @return filled object. See {@link com.gamaliev.list.list.ListEntry}
     */
    @Nullable
    ListEntry getEntry(@NonNull final Long id) {
        try (   SQLiteDatabase db = getReadableDatabase();
                Cursor cursor = db.query(
                        LIST_ITEMS_TABLE_NAME,
                        null,
                        BASE_COLUMN_ID + " = ?",
                        new String[] {id.toString()},
                        null, null, null)) {

            ListEntry entry = new ListEntry();
            if (cursor.moveToFirst()) {
                entry.setId(cursor.getInt(0));
                entry.setTitle(cursor.getString(1));
                entry.setDescription(cursor.getString(2));
                entry.setColor(cursor.getInt(3));
                return entry;
            }
            // TODO: add created date and edited date

        } catch (SQLiteException e) {
            Log.e(TAG, "[ERROR] Get entries: " + e.getMessage());
            showToast(context, dbFailMessage, Toast.LENGTH_SHORT);
        }

        return null;
    }

    /**
     * Delete entry from database.
     * @param id    id of entry to be deleted.
     * @return      true if success, otherwise false.
     */
    boolean deleteEntry(@NonNull final Integer id) {
        try (SQLiteDatabase db = getWritableDatabase()) {

            final int deleteResult = db.delete(
                    LIST_ITEMS_TABLE_NAME,
                    BASE_COLUMN_ID + " = ?",
                    new String[]{id.toString()});

            if (deleteResult == 0) {
                throw new SQLiteException("The number of rows affected is 0");
            }

            // If ok
            return true;

        } catch (SQLiteException e) {
            Log.e(TAG, "[ERROR] Delete entry: " + e.getMessage());
            showToast(context, dbFailMessage, Toast.LENGTH_SHORT);
            return false;
        }
    }

    /**
     * Add mock entries in list activity. See: {@link com.gamaliev.list.list.ListActivity}
     * @return true if ok, otherwise false.
     */
    boolean addMockEntries() {
        SQLiteDatabase db = null;

        // TODO: try-catch-with-resources and db.endTransaction?!
        try {
            db = getWritableDatabase();
            db.beginTransaction();
            addMockEntries(resources, db);
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


    /**
     * Add mock entries in list activity, with given params.<br>
     * See: {@link com.gamaliev.list.list.ListActivity}
     *
     * @param resources resources.
     * @param db        database.
     * @throws SQLiteException if insert error.
     */
    public static void addMockEntries(
            Resources resources,
            SQLiteDatabase db) throws SQLiteException {

        Random random = new Random();
        final int itemNumbers = resources.getInteger(R.integer.mock_items_number);

        for (int i = 0; i < itemNumbers; i++) {
            // Content values
            final ContentValues cv = new ContentValues();
            cv.put(LIST_ITEMS_COLUMN_TITLE,         resources.getString(R.string.mock_title));
            cv.put(LIST_ITEMS_COLUMN_DESCRIPTION,   resources.getString(R.string.mock_body));
            cv.put(LIST_ITEMS_COLUMN_COLOR,         Color.argb(
                            255,
                            random.nextInt(256),
                            random.nextInt(256),
                            random.nextInt(256)));
            cv.put(LIST_ITEMS_COLUMN_EDITED,        "time('now')");

            // Insert
            if (db.insert(LIST_ITEMS_TABLE_NAME, null, cv) == -1) {
                throw new SQLiteException("[ERROR] Add mock entries.");
            }
        }
    }

    /**
     * Delete all rows from list table. See: {@link com.gamaliev.list.list.ListActivity}
     * @return true if ok, otherwise false.
     */
    boolean removeAllEntries() {
        SQLiteDatabase db = null;

        // TODO: try-catch-with-resources and db.endTransaction?!
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
