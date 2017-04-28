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
import com.gamaliev.list.common.DatabaseQueryBuilder;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.gamaliev.list.common.CommonUtils.getDefaultColor;
import static com.gamaliev.list.common.CommonUtils.showToast;

/**
 * @author Vadim Gamaliev
 * <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class ListDatabaseHelper extends DatabaseHelper {

    /* Logger */
    private static final String TAG = ListDatabaseHelper.class.getSimpleName();


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

            // Add dates in ISO-8601 format.
            String currentDate = getNewDateISO8601(context);
            cv.put(LIST_ITEMS_COLUMN_CREATED,       currentDate);
            cv.put(LIST_ITEMS_COLUMN_EDITED,        currentDate);
            cv.put(LIST_ITEMS_COLUMN_VIEWED,        currentDate);

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
     * @param entry                 Entry, must contains non-null id.
     * @param editedViewedColumn    Which column should updated.
     *                              {@link #LIST_ITEMS_COLUMN_EDITED} or
     *                              {@link #LIST_ITEMS_COLUMN_VIEWED}.
     *                              If null, then default is {@link #LIST_ITEMS_COLUMN_EDITED}.
     */
    boolean updateEntry(
            @NonNull final ListEntry entry,
            @Nullable String editedViewedColumn) {

        if (editedViewedColumn == null) {
            editedViewedColumn = LIST_ITEMS_COLUMN_EDITED;
        }

        try (SQLiteDatabase db = getWritableDatabase()) {

            // Variables
            final long id               = entry.getId();
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
            cv.put(editedViewedColumn,              getNewDateISO8601(context));

            // Update
            final int updateResult = db.update(
                    LIST_ITEMS_TABLE_NAME,
                    cv,
                    BASE_COLUMN_ID + " = ?",
                    new String[]{Long.toString(id)});

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
    Cursor getEntries(@NonNull DatabaseQueryBuilder queryBuilder) {

        try {
            // Open database.
            final SQLiteDatabase db = getReadableDatabase();

            // Get query and return cursor, if ok;
            return db.query(
                    LIST_ITEMS_TABLE_NAME,
                    null,
                    queryBuilder.getSelectionResult(),
                    queryBuilder.getSelectionArgs(),
                    null,
                    null,
                    queryBuilder.getSortOrder());

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

        // Create new entry.
        final ListEntry entry = new ListEntry();

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
            if (cursor.moveToFirst()) {
                final int indexId           = cursor.getColumnIndex(DatabaseHelper.BASE_COLUMN_ID);
                final int indexTitle        = cursor.getColumnIndex(DatabaseHelper.LIST_ITEMS_COLUMN_TITLE);
                final int indexDescription  = cursor.getColumnIndex(DatabaseHelper.LIST_ITEMS_COLUMN_DESCRIPTION);
                final int indexColor        = cursor.getColumnIndex(DatabaseHelper.LIST_ITEMS_COLUMN_COLOR);
                final int indexCreated      = cursor.getColumnIndex(DatabaseHelper.LIST_ITEMS_COLUMN_CREATED);
                final int indexEdited       = cursor.getColumnIndex(DatabaseHelper.LIST_ITEMS_COLUMN_EDITED);
                final int indexViewed       = cursor.getColumnIndex(DatabaseHelper.LIST_ITEMS_COLUMN_VIEWED);

                entry.setId(            cursor.getLong(     indexId));
                entry.setTitle(         cursor.getString(   indexTitle));
                entry.setDescription(   cursor.getString(   indexDescription));
                entry.setColor(         cursor.getInt(      indexColor));

                // Dates in ISO-8601 format.
                DateFormat df = getDateFormatISO8601(context);
                try {
                    Date created    = df.parse(cursor.getString(indexCreated));
                    Date edited     = df.parse(cursor.getString(indexEdited));
                    Date viewed     = df.parse(cursor.getString(indexViewed));

                    entry.setCreated(created);
                    entry.setEdited(edited);
                    entry.setViewed(viewed);

                } catch (ParseException e) {
                    Log.e(TAG, e.toString());
                }
            }

            return entry;

        } catch (SQLiteException e) {
            Log.e(TAG, e.toString());
            showToast(context, dbFailMessage, Toast.LENGTH_SHORT);
            return null;
        }
    }

    /**
     * Delete entry from database, with given id.
     * @param id    Id of entry to be deleted.
     * @return      True if success, otherwise false.
     */
    boolean deleteEntry(@NonNull final Long id) {
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
            ListDatabaseMockHelper.addMockEntries(
                    resources.getInteger(R.integer.mock_items_number),
                    db);

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
     * @param context Context.
     * @return  String, representing a date in ISO-8601 format.<br>
     *          Example: "yyyy-MM-dd'T'HH:mm:ssXXX", "2017-04-22T21:25:35+05:00".
     */
    public static String getNewDateISO8601(@NonNull final Context context) {
        return getDateFormatISO8601(context).format(new Date());
    }

    /**
     * @param context Context.
     * @return  DateFormat with ISO-8601 pattern.<br>
     *          Example: "yyyy-MM-dd'T'HH:mm:ssXXX", "2017-04-22T21:25:35+05:00".
     */
    public static DateFormat getDateFormatISO8601(@NonNull final Context context) {
        return new SimpleDateFormat(
                context.getResources().getString(R.string.pattern_iso_8601),
                Locale.ENGLISH);
    }
}
