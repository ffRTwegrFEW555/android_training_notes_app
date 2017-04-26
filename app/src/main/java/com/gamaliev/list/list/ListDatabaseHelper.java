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

    /* Logger */
    private static final String TAG = ListDatabaseHelper.class.getSimpleName();

    /* SQL */
    public static final String LIST_ORDER_ADDED     = BASE_COLUMN_ID;
    public static final String LIST_ORDER_TITLE     = LIST_ITEMS_COLUMN_TITLE;
    public static final String LIST_ORDER_CREATED   = LIST_ITEMS_COLUMN_CREATED;
    public static final String LIST_ORDER_EDITED    = LIST_ITEMS_COLUMN_EDITED;
    public static final String LIST_ORDER_VIEWED    = LIST_ITEMS_COLUMN_EDITED;
    public static final String LIST_ORDER_DEFAULT   = LIST_ORDER_ADDED;


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
     * @param entry Entry, contains title, description, color.
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
            Log.e(TAG, e.toString());
            showToast(context, dbFailMessage, Toast.LENGTH_SHORT);
            return false;
        }
    }

    /**
     * Update entry in database.
     * @param entry Entry, must contains non-null id.
     */
    boolean updateEntry(@NonNull final ListEntry entry) {
        try (SQLiteDatabase db = getWritableDatabase()) {

            // Variables
            final int id                = entry.getId();
            final String title          = entry.getTitle();
            final String description    = entry.getDescription();
            final int color             = entry.getColor() == null
                                            ? getDefaultColor(context)
                                            : entry.getColor();

            // Content values
            final ContentValues cv = new ContentValues();
            cv.put(LIST_ITEMS_COLUMN_TITLE,         title);
            cv.put(LIST_ITEMS_COLUMN_DESCRIPTION,   description);
            cv.put(LIST_ITEMS_COLUMN_COLOR,         color);
            cv.put(LIST_ITEMS_COLUMN_EDITED,        "datetime('now', 'utc')");

            // Update
            final int updateResult = db.update(
                    LIST_ITEMS_TABLE_NAME,
                    cv,
                    BASE_COLUMN_ID + " = ?",
                    new String[]{Integer.toString(id)});

            if (updateResult == 0) {
                throw new SQLiteException("[ERROR] The number of rows affected is 0");
            }

            // If ok
            return true;

        } catch (SQLiteException e) {
            Log.e(TAG, e.toString());
            showToast(context, dbFailMessage, Toast.LENGTH_SHORT);
            return false;
        }
    }


    // TODO: refactor
    // "datetime('%Y-%m-%d %H:%M:%S', 'utc')"
    /**
     * Get entries from database with specified parameters.
     * @return Result cursor.
     */
    @Nullable
    Cursor getEntries(
            @Nullable final String searchText,
            @Nullable final String[] searchColumns,
            @Nullable String order,
            final boolean asc) {

        // Local variables
        String[] searchTextConvert = null;
        String searchColumnsConvert = null;
        final String ascDesc = asc ? ORDER_ASCENDING : ORDER_DESCENDING;

        // Order formation
        if (order == null) {
            order = LIST_ORDER_DEFAULT + ascDesc;
        } else {
            order += ascDesc;
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
            // Open database.
            final SQLiteDatabase db = getReadableDatabase();

            // Get query and return cursor, if ok;
            return db.query(
                    LIST_ITEMS_TABLE_NAME,
                    null,
                    searchColumns == null ? null : searchColumnsConvert,
                    searchText == null ? null : searchTextConvert,
                    null,
                    null,
                    order);

        } catch (SQLiteException e) {
            Log.e(TAG, e.toString());
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
     * @param id    Id of entry.
     * @return      Filled object. See {@link com.gamaliev.list.list.ListEntry}
     */
    @Nullable
    ListEntry getEntry(@NonNull final Long id) {
        try (   SQLiteDatabase db = getReadableDatabase();
                Cursor cursor = db.query(
                        LIST_ITEMS_TABLE_NAME,
                        null,
                        BASE_COLUMN_ID + " = ?",
                        new String[] {id.toString()},
                        null,
                        null,
                        null)) {

            // Fill new entry.
            final ListEntry entry = new ListEntry();
            if (cursor.moveToFirst()) {
                final int indexId           = cursor.getColumnIndex(DatabaseHelper.BASE_COLUMN_ID);
                final int indexTitle        = cursor.getColumnIndex(DatabaseHelper.LIST_ITEMS_COLUMN_TITLE);
                final int indexDescription  = cursor.getColumnIndex(DatabaseHelper.LIST_ITEMS_COLUMN_DESCRIPTION);
                final int indexColor        = cursor.getColumnIndex(DatabaseHelper.LIST_ITEMS_COLUMN_COLOR);

                entry.setId(            cursor.getInt(      indexId));
                entry.setTitle(         cursor.getString(   indexTitle));
                entry.setDescription(   cursor.getString(   indexDescription));
                entry.setColor(         cursor.getInt(      indexColor));
                // TODO: add created date and edited date
                return entry;
            }

        } catch (SQLiteException e) {
            Log.e(TAG, e.toString());
            showToast(context, dbFailMessage, Toast.LENGTH_SHORT);
        }

        return null;
    }

    /**
     * Delete entry from database, with given id.
     * @param id    Id of entry to be deleted.
     * @return      True if success, otherwise false.
     */
    boolean deleteEntry(@NonNull final Integer id) {
        try (SQLiteDatabase db = getWritableDatabase()) {

            // Delete query.
            final int deleteResult = db.delete(
                    LIST_ITEMS_TABLE_NAME,
                    BASE_COLUMN_ID + " = ?",
                    new String[]{id.toString()});

            // If error.
            if (deleteResult == 0) {
                throw new SQLiteException("[ERROR] The number of rows affected is 0");
            }

            // If ok.
            return true;

        } catch (SQLiteException e) {
            Log.e(TAG, e.toString());
            showToast(context, dbFailMessage, Toast.LENGTH_SHORT);
            return false;
        }
    }

    /**
     * Add mock entries in list activity. See: {@link com.gamaliev.list.list.ListActivity}
     * @return True if ok, otherwise false.
     */
    boolean addMockEntries() {
        SQLiteDatabase db = null;

        // TODO: try-catch-with-resources and db.endTransaction?!
        try {
            // Open database.
            db = getWritableDatabase();

            // Begin transaction.
            db.beginTransaction();

            // Helper method for add entries.
            addMockEntries(resources, db);

            // Success transaction.
            db.setTransactionSuccessful();

            // If ok.
            return true;

        } catch (SQLiteException e) {
            Log.e(TAG, e.toString());
            showToast(context, dbFailMessage, Toast.LENGTH_SHORT);
            return false;

        } finally {
            if (db != null) {
                // End transaction and close database.
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
            @NonNull final Resources resources,
            @NonNull final SQLiteDatabase db) throws SQLiteException {

        final Random random = new Random();
        final int itemNumbers = resources.getInteger(R.integer.mock_items_number);

        for (int i = 0; i < itemNumbers; i++) {
            // Content values.
            final ContentValues cv = new ContentValues();
            cv.put(LIST_ITEMS_COLUMN_TITLE,         resources.getString(R.string.mock_title));
            cv.put(LIST_ITEMS_COLUMN_DESCRIPTION,   resources.getString(R.string.mock_body));
            cv.put(LIST_ITEMS_COLUMN_COLOR,         Color.argb(
                            255,
                            random.nextInt(256),
                            random.nextInt(256),
                            random.nextInt(256)));

            // Insert query.
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
            // Open database.
            db = getWritableDatabase();

            // Begin transaction.
            db.beginTransaction();

            // Exec SQL queries.
            db.execSQL(SQL_LIST_ITEMS_DROP_TABLE);
            db.execSQL(SQL_LIST_ITEMS_CREATE_TABLE);

            // Success transaction.
            db.setTransactionSuccessful();

            // If ok
            return true;

        } catch (SQLiteException e) {
            Log.e(TAG, e.toString());
            showToast(context, dbFailMessage, Toast.LENGTH_SHORT);
            return false;

        } finally {
            if (db != null) {
                // End transaction and close database.
                db.endTransaction();
                db.close();
            }
        }
    }
}
