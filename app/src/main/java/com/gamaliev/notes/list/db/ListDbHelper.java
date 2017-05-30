package com.gamaliev.notes.list.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.gamaliev.notes.common.CommonUtils;
import com.gamaliev.notes.common.ProgressNotificationHelper;
import com.gamaliev.notes.common.db.DbHelper;
import com.gamaliev.notes.common.db.DbQueryBuilder;
import com.gamaliev.notes.common.shared_prefs.SpFilterProfiles;
import com.gamaliev.notes.model.ListEntry;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import static com.gamaliev.notes.common.CommonUtils.EXTRA_DATES_FROM_DATE;
import static com.gamaliev.notes.common.CommonUtils.EXTRA_DATES_TO_DATETIME;
import static com.gamaliev.notes.common.CommonUtils.getDateFromProfileMap;
import static com.gamaliev.notes.common.CommonUtils.getDefaultColor;
import static com.gamaliev.notes.common.CommonUtils.getStringDateFormatSqlite;
import static com.gamaliev.notes.common.CommonUtils.showToast;
import static com.gamaliev.notes.common.db.DbHelper.BASE_COLUMN_ID;
import static com.gamaliev.notes.common.db.DbHelper.COMMON_COLUMN_SYNC_ID;
import static com.gamaliev.notes.common.db.DbHelper.FAVORITE_COLUMN_COLOR;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_COLOR;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_CREATED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_DESCRIPTION;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_EDITED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_IMAGE_URL;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_MANUALLY;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_TITLE;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_VIEWED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_TABLE_NAME;
import static com.gamaliev.notes.common.db.DbHelper.SQL_LIST_ITEMS_CREATE_TABLE;
import static com.gamaliev.notes.common.db.DbHelper.SQL_LIST_ITEMS_DROP_TABLE;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_CONFLICT_TABLE_NAME;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_DELETED_TABLE_NAME;
import static com.gamaliev.notes.common.db.DbHelper.deleteEntryWithSingle;
import static com.gamaliev.notes.common.db.DbHelper.getDbFailMessage;
import static com.gamaliev.notes.common.db.DbHelper.getEntries;
import static com.gamaliev.notes.common.db.DbHelper.getReadableDb;
import static com.gamaliev.notes.common.db.DbHelper.getWritableDb;
import static com.gamaliev.notes.common.db.DbHelper.insertEntryWithSingleValue;
import static com.gamaliev.notes.common.db.DbQueryBuilder.OPERATOR_BETWEEN;
import static com.gamaliev.notes.common.db.DbQueryBuilder.OPERATOR_EQUALS;
import static com.gamaliev.notes.common.db.DbQueryBuilder.OPERATOR_LIKE;
import static com.gamaliev.notes.list.ListFragment.SEARCH_COLUMNS;

/**
 * @author Vadim Gamaliev
 * <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class ListDbHelper {

    /* Logger */
    private static final String TAG = ListDbHelper.class.getSimpleName();

    @NonNull private static final String[] DATES_COLUMNS = {
            LIST_ITEMS_COLUMN_CREATED,
            LIST_ITEMS_COLUMN_EDITED,
            LIST_ITEMS_COLUMN_VIEWED
    };


    /*
        Init
     */

    private ListDbHelper() {}
    

    /*
        ...
     */

    /**
     * Insert new entry in database, or update if sync id flag is setting.
     * @param entry             Entry, contains title, description, color.
     * @param updateBySyncId    If true, then update current entry in database, by sync id.
     */
    public static boolean insertUpdateEntry(
            @NonNull final Context context,
            @NonNull final ListEntry entry,
            final boolean updateBySyncId) {

        try {
            final SQLiteDatabase db = getWritableDb(context);
            insertUpdateEntry(context, entry, db, updateBySyncId);
            return true;

        } catch (SQLiteException e) {
            Log.e(TAG, e.toString());
            showToast(context, getDbFailMessage(), Toast.LENGTH_SHORT);
            return false;
        }
    }

    /**
     * Insert new entry in database, or update if sync id flag is setting.
     * @param entry             Entry, contains title, description, color.
     * @param db                Opened writable database.
     * @param updateBySyncId    If true, then update current entry in database, by sync id.
     */
    public static void insertUpdateEntry(
            @NonNull final Context context,
            @NonNull final ListEntry entry,
            @NonNull final SQLiteDatabase db,
            final boolean updateBySyncId) throws SQLiteException {

        final String syncId         = (entry.getSyncId() == null || entry.getSyncId() == 0)
                ? null
                : entry.getSyncId().toString();
        final String title          = entry.getTitle();
        final String description    = entry.getDescription();
        final int color             = entry.getColor() == null
                ? getDefaultColor(context)
                : entry.getColor();
        final String imageUrl       = entry.getImageUrl();

        final ContentValues cv = new ContentValues();
        cv.put(COMMON_COLUMN_SYNC_ID,           syncId);
        cv.put(LIST_ITEMS_COLUMN_TITLE,         title);
        cv.put(LIST_ITEMS_COLUMN_DESCRIPTION,   description);
        cv.put(LIST_ITEMS_COLUMN_COLOR,         color);
        cv.put(LIST_ITEMS_COLUMN_IMAGE_URL,     imageUrl);

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

        if (updateBySyncId) {
            final int updateResult = db.update(
                    LIST_ITEMS_TABLE_NAME,
                    cv,
                    COMMON_COLUMN_SYNC_ID + " = ?",
                    new String[]{syncId});

            if (updateResult == 0) {
                throw new SQLiteException("[ERROR] The number of rows affected is 0");
            }

        } else {
            if (db.insert(LIST_ITEMS_TABLE_NAME, null, cv) == -1) {
                final String error = String.format(Locale.ENGLISH,
                        "[ERROR] Insert entry {%s: %s, %s: %s, %s: %d, %s: %s}",
                        LIST_ITEMS_COLUMN_TITLE,        title,
                        LIST_ITEMS_COLUMN_DESCRIPTION,  description,
                        LIST_ITEMS_COLUMN_COLOR,        color,
                        LIST_ITEMS_COLUMN_IMAGE_URL,    imageUrl);
                throw new SQLiteException(error);
            }
        }
    }

    /**
     * Update entry in database.
     * @param entry                 Entry, must contains non-null id.
     * @param editedViewedColumn    Which column should updated with current date and time.
     *
     * {@link DbHelper#LIST_ITEMS_COLUMN_EDITED} or
     * {@link DbHelper#LIST_ITEMS_COLUMN_VIEWED}.
     * If null, then default is
     * {@link DbHelper#LIST_ITEMS_COLUMN_EDITED}.
     */
    public static boolean updateEntry(
            @NonNull final Context context,
            @NonNull final ListEntry entry,
            @Nullable String editedViewedColumn) {

        if (editedViewedColumn == null) {
            editedViewedColumn = LIST_ITEMS_COLUMN_EDITED;
        }

        try {
            final SQLiteDatabase db     = getWritableDb(context);
            final long id               = entry.getId();
            final String syncId         = (entry.getSyncId() == null || entry.getSyncId() == 0)
                    ? null
                    : entry.getSyncId().toString();
            final String title          = entry.getTitle();
            final String description    = entry.getDescription();
            final int color             = entry.getColor() == null
                    ? getDefaultColor(context)
                    : entry.getColor();
            final String imageUrl       = entry.getImageUrl();

            final ContentValues cv = new ContentValues();
            cv.put(COMMON_COLUMN_SYNC_ID,           syncId);
            cv.put(LIST_ITEMS_COLUMN_TITLE,         title);
            cv.put(LIST_ITEMS_COLUMN_DESCRIPTION,   description);
            cv.put(LIST_ITEMS_COLUMN_COLOR,         color);
            cv.put(LIST_ITEMS_COLUMN_IMAGE_URL,     imageUrl);
            cv.put(editedViewedColumn,              getStringDateFormatSqlite(context, new Date(), true));

            final int updateResult = db.update(
                    LIST_ITEMS_TABLE_NAME,
                    cv,
                    BASE_COLUMN_ID + " = ?",
                    new String[]{Long.toString(id)});

            if (updateResult == 0) {
                throw new SQLiteException("[ERROR] The number of rows affected is 0");
            }

            return true;

        } catch (SQLiteException e) {
            Log.e(TAG, e.toString());
            showToast(context, getDbFailMessage(), Toast.LENGTH_SHORT);
            return false;
        }
    }

    public static boolean updateSyncId(
            @NonNull final Context context,
            @NonNull final String id,
            @NonNull final String syncId) {

        try {
            final SQLiteDatabase db = getWritableDb(context);

            final ContentValues cv = new ContentValues();
            cv.put(COMMON_COLUMN_SYNC_ID, syncId);

            final int updateResult = db.update(
                    LIST_ITEMS_TABLE_NAME,
                    cv,
                    BASE_COLUMN_ID + " = ?",
                    new String[]{id});

            if (updateResult == 0) {
                throw new SQLiteException("[ERROR] The number of rows affected is 0");
            }

            return true;

        } catch (SQLiteException e) {
            Log.e(TAG, e.toString());
            showToast(context, getDbFailMessage(), Toast.LENGTH_SHORT);
            return false;
        }
    }

    /**
     * Get entries from database, where sync id is null.
     * @return Result cursor.
     */
    @Nullable
    public static Cursor getNewEntries(
            @NonNull final Context context) {

        try {
            final SQLiteDatabase db = getReadableDb(context);
            return db.query(
                    LIST_ITEMS_TABLE_NAME,
                    null,
                    COMMON_COLUMN_SYNC_ID + " is NULL",
                    null,
                    null,
                    null,
                    null);

        } catch (SQLiteException e) {
            Log.e(TAG, e.toString());
            showToast(context, getDbFailMessage(), Toast.LENGTH_SHORT);
            return null;
        }
    }

    /**
     * Get filled object from database, with given id.
     * @param id    Id of entry.
     * @return      Filled object. See {@link ListEntry}
     */
    @Nullable
    public static ListEntry getEntry(
            @NonNull final Context context,
            @NonNull final Long id) {

        final ListEntry entry = new ListEntry();
        final SQLiteDatabase db = getReadableDb(context);

        try (Cursor cursor = db.query(
                LIST_ITEMS_TABLE_NAME,
                null,
                BASE_COLUMN_ID + " = ?",
                new String[] {id.toString()},
                null,
                null,
                null)) {

            if (cursor.moveToFirst()) {
                final int indexId           = cursor.getColumnIndex(BASE_COLUMN_ID);
                final int indexSyncId       = cursor.getColumnIndex(COMMON_COLUMN_SYNC_ID);
                final int indexTitle        = cursor.getColumnIndex(LIST_ITEMS_COLUMN_TITLE);
                final int indexDescription  = cursor.getColumnIndex(LIST_ITEMS_COLUMN_DESCRIPTION);
                final int indexColor        = cursor.getColumnIndex(LIST_ITEMS_COLUMN_COLOR);
                final int indexImageUrl     = cursor.getColumnIndex(LIST_ITEMS_COLUMN_IMAGE_URL);
                final int indexCreated      = cursor.getColumnIndex(LIST_ITEMS_COLUMN_CREATED);
                final int indexEdited       = cursor.getColumnIndex(LIST_ITEMS_COLUMN_EDITED);
                final int indexViewed       = cursor.getColumnIndex(LIST_ITEMS_COLUMN_VIEWED);

                entry.setId(            cursor.getLong(     indexId));
                entry.setSyncId(        cursor.getLong(     indexSyncId));
                entry.setTitle(         cursor.getString(   indexTitle));
                entry.setDescription(   cursor.getString(   indexDescription));
                entry.setColor(         cursor.getInt(      indexColor));
                entry.setImageUrl(      cursor.getString(   indexImageUrl));

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
            showToast(context, getDbFailMessage(), Toast.LENGTH_SHORT);
            return null;
        }
    }

    /**
     * Delete entry from database, with given id.
     * @param id    Id of entry to be deleted.
     * @return      True if success, otherwise false.
     */
    public static boolean deleteEntry(
            @NonNull final Context context,
            @NonNull final Long id,
            final boolean addToDeletedTable) {

        final ListEntry entry = getEntry(context, id);
        Long syncId = null;
        if (entry != null) {
            syncId = entry.getSyncId();
        }

        try {
            final SQLiteDatabase db = getWritableDb(context);
            int deleteResult;

            db.beginTransaction();
            try {
                deleteResult = db.delete(
                        LIST_ITEMS_TABLE_NAME,
                        BASE_COLUMN_ID + " = ?",
                        new String[]{id.toString()});

                if (addToDeletedTable) {
                    if (syncId != null && syncId > 0) {
                        insertEntryWithSingleValue(
                                context,
                                db,
                                SYNC_DELETED_TABLE_NAME,
                                COMMON_COLUMN_SYNC_ID,
                                syncId.toString());
                    }
                }

                if (syncId != null && syncId > 0) {
                    deleteEntryWithSingle(
                            context,
                            db,
                            SYNC_CONFLICT_TABLE_NAME,
                            COMMON_COLUMN_SYNC_ID,
                            syncId.toString(),
                            false);
                }

                if (deleteResult == 0) {
                    throw new SQLiteException("[ERROR] The number of rows affected is 0");
                }

                db.setTransactionSuccessful();

            } finally {
                db.endTransaction();
            }

            return true;

        } catch (SQLiteException e) {
            Log.e(TAG, e.toString());
            showToast(context, getDbFailMessage(), Toast.LENGTH_SHORT);
            return false;
        }
    }

    /**
     * Delete all rows from list table.
     * @return true if ok, otherwise false.
     */
    public static boolean removeAllEntries(
            @NonNull final Context context) {

        try {
            final SQLiteDatabase db = getWritableDb(context);
            db.beginTransaction();
            try {
                db.execSQL(SQL_LIST_ITEMS_DROP_TABLE);
                db.execSQL(SQL_LIST_ITEMS_CREATE_TABLE);
                db.setTransactionSuccessful();
                return true;

            } finally {
                db.endTransaction();
            }

        } catch (SQLiteException e) {
            Log.e(TAG, e.toString());
            showToast(context, getDbFailMessage(), Toast.LENGTH_SHORT);
        }

        return false;
    }

    /**
     * Add mock entries.
     * @return Number of added entries. If error, then return "-1".
     */
    public static int addMockEntries(
            @NonNull final Context context,
            @Nullable final ProgressNotificationHelper notification,
            final int numberOfEntries) {

        try {
            final SQLiteDatabase db = getWritableDb(context);
            db.beginTransaction();

            try {
                ListDbMockHelper.addMockEntries(
                        context,
                        numberOfEntries,
                        db,
                        notification,
                        true);
                db.setTransactionSuccessful();
                return numberOfEntries;

            } finally {
                db.endTransaction();
            }

        } catch (SQLiteException e) {
            Log.e(TAG, e.toString());
            showToast(context, getDbFailMessage(), Toast.LENGTH_SHORT);
        }

        return -1;
    }

    /**
     * @param context       Context.
     * @param constraint    Search text.
     * @param profileMap    Profile parameters.
     * @return              Cursor, with given params.
     */
    @Nullable
    public static Cursor getCursorWithParams(
            @NonNull final Context context,
            @Nullable final CharSequence constraint,
            @NonNull final Map<String, String> profileMap) {

        final DbQueryBuilder resultQueryBuilder =
                convertToQueryBuilder(
                        context, constraint, profileMap);
        return getEntries(context, LIST_ITEMS_TABLE_NAME, resultQueryBuilder);
    }

    /**
     * @param context       Context.
     * @param constraint    Search text.
     * @param profileMap    Profile parameters.
     * @return              Filled database query builder.
     */
    @NonNull
    public static DbQueryBuilder convertToQueryBuilder(
            @NonNull final Context context,
            @Nullable final CharSequence constraint,
            @NonNull final Map<String, String> profileMap) {

        final DbQueryBuilder searchTextQueryBuilder = new DbQueryBuilder();
        if (!TextUtils.isEmpty(constraint)) {
            searchTextQueryBuilder
                    .addOr( SEARCH_COLUMNS[0],
                            OPERATOR_LIKE,
                            new String[] {constraint.toString()})

                    .addOr( SEARCH_COLUMNS[1],
                            OPERATOR_LIKE,
                            new String[] {constraint.toString()});
        }

        final DbQueryBuilder resultQueryBuilder = new DbQueryBuilder();
        if (!TextUtils.isEmpty(profileMap.get(FAVORITE_COLUMN_COLOR))) {
            resultQueryBuilder.addAnd(
                    FAVORITE_COLUMN_COLOR,
                    OPERATOR_EQUALS,
                    new String[]{profileMap.get(FAVORITE_COLUMN_COLOR)});
        }

        for (String DATES_COLUMN : DATES_COLUMNS) {
            final String dates = getDateFromProfileMap(
                    context,
                    profileMap,
                    DATES_COLUMN,
                    EXTRA_DATES_TO_DATETIME);

            if (TextUtils.isEmpty(dates)) {
                continue;
            }

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
                    DATES_COLUMN,
                    EXTRA_DATES_FROM_DATE);
            datesArray[1] = getStringDateFormatSqlite(
                    context,
                    newDateTo.getTime(),
                    false);

            // Add viewed filter, if not empty or null.
            if (!TextUtils.isEmpty(profileMap.get(DATES_COLUMN))) {
                resultQueryBuilder.addAnd(
                        DATES_COLUMN,
                        OPERATOR_BETWEEN,
                        datesArray);
            }
        }

        // Add search text inner filter, if not empty or null.
        if (!TextUtils.isEmpty(constraint)) {
            resultQueryBuilder.addAndInner(searchTextQueryBuilder);
        }

        // Set sort order.
        resultQueryBuilder.setOrder(profileMap.get(SpFilterProfiles.SP_FILTER_ORDER));
        resultQueryBuilder.setAscDesc(profileMap.get(SpFilterProfiles.SP_FILTER_ORDER_ASC));

        return resultQueryBuilder;
    }

    static boolean swapManuallyColumnValue(
            @NonNull final Context context,
            @NonNull final String entryFirstId,
            @NonNull final String entrySecondId) {

        final SQLiteDatabase db = getWritableDb(context.getApplicationContext());
        db.beginTransaction();
        try {
            final String entryFirstValue = getManuallyColumnValue(context, db, entryFirstId);
            if (entryFirstValue == null) {
                throw new SQLiteException(
                        String.format(
                                Locale.ENGLISH,
                                "Getting value, from 'manually column', with entry id: %s",
                                entryFirstId));
            }

            final String entrySecondValue = getManuallyColumnValue(context, db, entrySecondId);
            if (entrySecondValue == null) {
                throw new SQLiteException(
                        String.format(
                                Locale.ENGLISH,
                                "Getting value, from 'manually column', with entry id: %s",
                                entrySecondId));
            }

            updateManuallyColumnValue(context, db, entryFirstId, entrySecondValue);
            updateManuallyColumnValue(context, db, entrySecondId, entryFirstValue);

            db.setTransactionSuccessful();

        } catch (SQLiteException e) {
            Log.e(TAG, e.toString());
            showToast(context, getDbFailMessage(), Toast.LENGTH_SHORT);
            return false;

        } finally {
            db.endTransaction();
        }

        return true;
    }

    @Nullable
    private static String getManuallyColumnValue(
            @NonNull final Context context,
            @NonNull final SQLiteDatabase db,
            @NonNull final String entryId) {

        String value = null;
        try (Cursor cursor = db.query(
                    LIST_ITEMS_TABLE_NAME,
                    null,
                    BASE_COLUMN_ID + " = ?",
                    new String[] {entryId},
                    null,
                    null,
                    null)) {

            if (cursor.moveToFirst()) {
                value = cursor.getString(
                        cursor.getColumnIndex(LIST_ITEMS_COLUMN_MANUALLY));
            } else {
                throw new SQLiteException(
                        String.format(
                                Locale.ENGLISH,
                                "Entry, with id '%s', is not exists", entryId));
            }

        } catch (SQLiteException e) {
            Log.e(TAG, e.toString());
            showToast(context, getDbFailMessage(), Toast.LENGTH_SHORT);
        }

        return value;
    }

    private static boolean updateManuallyColumnValue(
            @NonNull final Context context,
            @NonNull final SQLiteDatabase db,
            @NonNull final String entryId,
            @NonNull final String value) {

        final ContentValues cv = new ContentValues();
        cv.put(LIST_ITEMS_COLUMN_MANUALLY, value);

        final int updateResult = db.update(
                LIST_ITEMS_TABLE_NAME,
                cv,
                BASE_COLUMN_ID + " = ?",
                new String[] {entryId});

        if (updateResult == 0) {
            Log.e(TAG, "[ERROR] The number of rows affected is 0");
            showToast(context, getDbFailMessage(), Toast.LENGTH_SHORT);
            return false;
        }

        return true;
    }
}
