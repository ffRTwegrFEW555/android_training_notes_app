package com.gamaliev.list.list;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.gamaliev.list.R;
import com.gamaliev.list.common.CommonUtils;
import com.gamaliev.list.common.DatabaseHelper;
import com.gamaliev.list.common.DatabaseQueryBuilder;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import static com.gamaliev.list.common.CommonUtils.EXTRA_DATES_FROM_DATE;
import static com.gamaliev.list.common.CommonUtils.EXTRA_DATES_TO_DATETIME;
import static com.gamaliev.list.common.CommonUtils.getDateFromProfileMap;
import static com.gamaliev.list.common.CommonUtils.getDefaultColor;
import static com.gamaliev.list.common.CommonUtils.getStringDateFormatSqlite;
import static com.gamaliev.list.common.CommonUtils.showToast;
import static com.gamaliev.list.common.DatabaseQueryBuilder.OPERATOR_BETWEEN;
import static com.gamaliev.list.common.DatabaseQueryBuilder.OPERATOR_EQUALS;
import static com.gamaliev.list.common.DatabaseQueryBuilder.OPERATOR_LIKE;
import static com.gamaliev.list.list.ListActivity.SEARCH_COLUMNS;
import static com.gamaliev.list.list.ListActivitySharedPreferencesUtils.SP_FILTER_ORDER;
import static com.gamaliev.list.list.ListActivitySharedPreferencesUtils.SP_FILTER_ORDER_ASC;

/**
 * @author Vadim Gamaliev
 * <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class ListDatabaseHelper extends DatabaseHelper {

    /* Logger */
    private static final String TAG = ListDatabaseHelper.class.getSimpleName();

    @NonNull private static final String[] datesColumns = {
            LIST_ITEMS_COLUMN_CREATED,
            LIST_ITEMS_COLUMN_EDITED,
            LIST_ITEMS_COLUMN_VIEWED
    };


    /*
        Init
     */

    public ListDatabaseHelper(@NonNull final Context context) {
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
    public boolean insertEntry(@NonNull final ListEntry entry) {
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

            String utcCreatedDate =
                    getStringDateFormatSqlite(
                            context,
                            entry.getCreated() == null ? new Date() : entry.getCreated(),
                            true);
            String utcEditedDate =
                    getStringDateFormatSqlite(
                            context,
                            entry.getEdited() == null ? new Date() : entry.getEdited(),
                            true);
            String utcViewedDate =
                    getStringDateFormatSqlite(
                            context,
                            entry.getViewed() == null ? new Date() : entry.getViewed(),
                            true);

            cv.put(LIST_ITEMS_COLUMN_CREATED,   utcCreatedDate);
            cv.put(LIST_ITEMS_COLUMN_EDITED,    utcEditedDate);
            cv.put(LIST_ITEMS_COLUMN_VIEWED,    utcViewedDate);

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
            cv.put(editedViewedColumn,              getStringDateFormatSqlite(context, new Date(), true));

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
    public Cursor getEntries(@NonNull final DatabaseQueryBuilder queryBuilder) {

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

                // Parse sqlite format (yyyy-MM-dd HH:mm:ss, UTC), in localtime.
                DateFormat df = CommonUtils.getDateFormatSqlite(context, true);
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
                    resources.getInteger(R.integer.mock_items_number_click),
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
     * @param context       Context.
     * @param constraint    Search text.
     * @param profileMap    Profile parameters.
     * @return              Cursor, with given params.
     */
    @NonNull
    Cursor getCursorWithParams(
            @NonNull final Context context,
            @Nullable final CharSequence constraint,
            @NonNull final Map<String, String> profileMap) {

        // Create and fill query builder for text search.
        final DatabaseQueryBuilder searchTextQueryBuilder = new DatabaseQueryBuilder();

        // Add text for search in 'Name' and 'Description' columns, if not empty or null.
        if (!TextUtils.isEmpty(constraint)) {
            searchTextQueryBuilder
                    .addOr( SEARCH_COLUMNS[0],
                            OPERATOR_LIKE,
                            new String[] {constraint.toString()})

                    .addOr( SEARCH_COLUMNS[1],
                            OPERATOR_LIKE,
                            new String[] {constraint.toString()});
        }

        // Create and fill query result builder.
        final DatabaseQueryBuilder resultQueryBuilder = new DatabaseQueryBuilder();

        // Add color filter, if not empty or null.
        if (!TextUtils.isEmpty(profileMap.get(FAVORITE_COLUMN_COLOR))) {
            resultQueryBuilder.addAnd(
                    FAVORITE_COLUMN_COLOR,
                    OPERATOR_EQUALS,
                    new String[]{profileMap.get(FAVORITE_COLUMN_COLOR)});
        }

        // For each column.
        for (int i = 0; i < datesColumns.length; i++) {

            // Add +1 day to dateTo.

            // Get dateTo from profile.
            final String dates = getDateFromProfileMap(
                    context,
                    profileMap,
                    datesColumns[i],
                    EXTRA_DATES_TO_DATETIME);

            if (TextUtils.isEmpty(dates)) {
                continue;
            }

            // Parse DateTo.
            final DateFormat df = CommonUtils.getDateFormatSqlite(context, false);
            Date dateUtc = null;
            try {
                dateUtc = df.parse(dates);
            } catch (ParseException e) {
                Log.e(TAG, e.toString());
            }

            // Add +1 day to dateTo.
            final Calendar newDateTo = Calendar.getInstance();
            newDateTo.setTime(dateUtc);
            newDateTo.add(Calendar.DATE, 1);

            // Create array for queryBuilder.
            final String[] datesArray = new String[2];
            datesArray[0] = getDateFromProfileMap(
                    context,
                    profileMap,
                    datesColumns[i],
                    EXTRA_DATES_FROM_DATE);
            datesArray[1] = getStringDateFormatSqlite(
                    context,
                    newDateTo.getTime(),
                    false);

            // Add viewed filter, if not empty or null.
            if (!TextUtils.isEmpty(profileMap.get(datesColumns[i]))) {
                resultQueryBuilder.addAnd(
                        datesColumns[i],
                        OPERATOR_BETWEEN,
                        datesArray);
            }
        }

        // Add search text inner filter, if not empty or null.
        if (!TextUtils.isEmpty(constraint)) {
            resultQueryBuilder.addAndInner(searchTextQueryBuilder);
        }

        // Set sort order.
        resultQueryBuilder.setOrder(profileMap.get(SP_FILTER_ORDER));
        resultQueryBuilder.setAscDesc(profileMap.get(SP_FILTER_ORDER_ASC));

        // Go-go-go.
        return this.getEntries(resultQueryBuilder);
    }
}
