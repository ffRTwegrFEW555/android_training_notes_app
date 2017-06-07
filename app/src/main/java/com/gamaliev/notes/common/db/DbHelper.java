package com.gamaliev.notes.common.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.gamaliev.notes.R;
import com.gamaliev.notes.color_picker.db.ColorPickerDbHelper;
import com.gamaliev.notes.common.shared_prefs.SpUsers;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.gamaliev.notes.app.NotesApp.getAppContext;
import static com.gamaliev.notes.common.CommonUtils.showToast;
import static com.gamaliev.notes.common.CommonUtils.showToastRunOnUiThread;

/**
 * @author Vadim Gamaliev
 * <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

@SuppressWarnings("WeakerAccess")
public final class DbHelper extends SQLiteOpenHelper {

    /* Logger */
    @NonNull private static final String TAG = DbHelper.class.getSimpleName();


    /*
        DB
     */

    /* Basic */
    private static final int DB_VERSION_A                   = 1;
    private static final int DB_VERSION                     = DB_VERSION_A;

    public static final String BASE_COLUMN_ID               = BaseColumns._ID;
    public static final String ORDER_ASCENDING              = "ASC";
    public static final String ORDER_DESCENDING             = "DESC";
    public static final String ORDER_ASC_DESC_DEFAULT       = ORDER_ASCENDING;
    public static final String ORDER_COLUMN_DEFAULT         = BASE_COLUMN_ID;

    /* Common */
    public static final String COMMON_COLUMN_SYNC_ID        = "sync_id";

    /* Favorite table */
    public static final String FAVORITE_TABLE_NAME          = "favorite_colors";
    public static final String FAVORITE_COLUMN_INDEX        = "tbl_index";
    public static final String FAVORITE_COLUMN_COLOR        = "color";

    /* List items table */
    public static final String LIST_ITEMS_TABLE_NAME        = "list_items";
    public static final String LIST_ITEMS_COLUMN_MANUALLY   = "manually";
    public static final String LIST_ITEMS_COLUMN_TITLE      = "title";
    public static final String LIST_ITEMS_COLUMN_SYNC_ID_JSON = "id";
    public static final String LIST_ITEMS_COLUMN_DESCRIPTION = "description";
    public static final String LIST_ITEMS_COLUMN_COLOR      = "color";
    public static final String LIST_ITEMS_COLUMN_IMAGE_URL  = "imageUrl";
    public static final String LIST_ITEMS_COLUMN_CREATED    = "created";
    public static final String LIST_ITEMS_COLUMN_EDITED     = "edited";
    public static final String LIST_ITEMS_COLUMN_VIEWED     = "viewed";

    private static final String LIST_ITEMS_TRIGGER_COLUMN_MANUALLY_AUTOINCREMENT
            = "manually_autoincrement";

    /* Sync. Journal table */
    public static final String SYNC_TABLE_NAME              = "sync_journal";
    public static final String SYNC_COLUMN_FINISHED         = "finished";
    public static final String SYNC_COLUMN_ACTION           = "action";
    public static final String SYNC_COLUMN_STATUS           = "status";
    public static final String SYNC_COLUMN_AMOUNT           = "amount";

    /* Sync. Conflict table */
    public static final String SYNC_CONFLICT_TABLE_NAME     = "sync_conflict";

    /* Sync. Deleted table */
    public static final String SYNC_DELETED_TABLE_NAME      = "deleted";


    /*
        Queries
    */

    /* Colors */
    private static final String SQL_FAVORITE_CREATE_TABLE =
            "CREATE TABLE " + FAVORITE_TABLE_NAME + " ("
                    + BASE_COLUMN_ID +          " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + FAVORITE_COLUMN_INDEX +   " INTEGER NOT NULL UNIQUE, "
                    + FAVORITE_COLUMN_COLOR +   " INTEGER NOT NULL); ";

    /* Entries */
    private static final String SQL_LIST_ITEMS_CREATE_TABLE =
            "CREATE TABLE " + LIST_ITEMS_TABLE_NAME + " ("
                    + BASE_COLUMN_ID +                  " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + LIST_ITEMS_COLUMN_MANUALLY +      " INTEGER DEFAULT 0, "
                    + LIST_ITEMS_COLUMN_TITLE +         " TEXT, "
                    + COMMON_COLUMN_SYNC_ID +           " INTEGER, "
                    + LIST_ITEMS_COLUMN_DESCRIPTION +   " TEXT, "
                    + LIST_ITEMS_COLUMN_COLOR +         " INTEGER, "
                    + LIST_ITEMS_COLUMN_IMAGE_URL +     " TEXT, "
                    + LIST_ITEMS_COLUMN_CREATED +       " DATETIME DEFAULT CURRENT_TIMESTAMP, "
                    + LIST_ITEMS_COLUMN_EDITED +        " DATETIME DEFAULT CURRENT_TIMESTAMP, "
                    + LIST_ITEMS_COLUMN_VIEWED +        " DATETIME DEFAULT CURRENT_TIMESTAMP); ";

    // --Commented out by Inspection START:
    //    /* Entries. Drop */
    //    public static final String SQL_LIST_ITEMS_DROP_TABLE =
    //            "DROP TABLE " + LIST_ITEMS_TABLE_NAME + ";";
    // --Commented out by Inspection STOP

    /* Sync. Journal table */
    public static final String SQL_SYNC_CREATE_TABLE =
            "CREATE TABLE " + SYNC_TABLE_NAME + " ("
                    + BASE_COLUMN_ID +          " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + SYNC_COLUMN_FINISHED +    " DATETIME DEFAULT CURRENT_TIMESTAMP, "
                    + SYNC_COLUMN_ACTION +      " INTEGER NOT NULL, "
                    + SYNC_COLUMN_STATUS +      " INTEGER NOT NULL, "
                    + SYNC_COLUMN_AMOUNT +      " INTEGER NOT NULL); ";

    /* Sync. Journal table. Drop */
    public static final String SQL_SYNC_DROP_TABLE =
            "DROP TABLE " + SYNC_TABLE_NAME + ";";

    /* Sync. Conflict table */
    private static final String SQL_SYNC_CONFLICT_CREATE_TABLE =
            "CREATE TABLE " + SYNC_CONFLICT_TABLE_NAME + " ("
                    + BASE_COLUMN_ID +          " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COMMON_COLUMN_SYNC_ID +   " INTEGER NOT NULL UNIQUE); ";

    /* Sync. Deleted table */
    private static final String SQL_SYNC_DELETED_CREATE_TABLE =
            "CREATE TABLE " + SYNC_DELETED_TABLE_NAME + " ("
                    + BASE_COLUMN_ID +          " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COMMON_COLUMN_SYNC_ID +   " INTEGER NOT NULL UNIQUE); ";


    /*
        Triggers
     */

    /* Entries. Autoincrement "manually" column */
    private static final String SQL_LIST_ITEMS_MANUALLY_AUTOINCREMENT =
            "CREATE TRIGGER " + LIST_ITEMS_TRIGGER_COLUMN_MANUALLY_AUTOINCREMENT + " "
                    + "AFTER INSERT ON " + LIST_ITEMS_TABLE_NAME + " "
                        + "BEGIN "
                            + "UPDATE " + LIST_ITEMS_TABLE_NAME + " SET "
                                + LIST_ITEMS_COLUMN_MANUALLY + "=(SELECT MAX("
                                + LIST_ITEMS_COLUMN_MANUALLY + ")+1 FROM "
                                + LIST_ITEMS_TABLE_NAME + ") "
                        + "WHERE rowid=NEW.rowid; "
                        + "END;";


    /*
        ...
     */

    @NonNull private static final Map<String, DbHelper> INSTANCES;
    @SuppressWarnings("NullableProblems")
    @NonNull private static final String DB_FAILED_MESSAGE;


    /*
        Init
     */

    static {
        INSTANCES = new ConcurrentHashMap<>();
        DB_FAILED_MESSAGE = getAppContext().getString(R.string.sql_toast_fail);
    }

    /**
     * Get instance of database for current user.
     * @param context   Context.
     * @return          Database helper instance. If current user not found, return null.
     */
    @Nullable
    public static synchronized DbHelper getInstance(
            @NonNull final Context context) {

        final String userId = SpUsers.getSelected(context);
        if (TextUtils.isEmpty(userId)) {
            return null;
        }

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (INSTANCES.get(userId) == null) {
            INSTANCES.put(userId, new DbHelper(context.getApplicationContext(), userId));
        }
        return INSTANCES.get(userId);
    }

    private DbHelper(
            @NonNull final Context context,
            @NonNull final String userId) {

        // userId as Database name.
        super(context, userId, null, DB_VERSION);
    }


    /*
        Init database
     */

    @Override
    public void onCreate(final SQLiteDatabase db) {
        updateDatabase(db, 0, DB_VERSION);
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        updateDatabase(db, oldVersion, newVersion);
    }

    // Creating a new table and populating with default values, or update if database exist.
    private void updateDatabase(
            @NonNull final SQLiteDatabase db,
            final int oldVersion,
            @SuppressWarnings("UnusedParameters") final int newVersion) {

        if (oldVersion == 0) {
            db.beginTransaction();
            try {
                db.execSQL(SQL_FAVORITE_CREATE_TABLE);
                db.execSQL(SQL_LIST_ITEMS_CREATE_TABLE);
                db.execSQL(SQL_SYNC_CREATE_TABLE);
                db.execSQL(SQL_SYNC_CONFLICT_CREATE_TABLE);
                db.execSQL(SQL_SYNC_DELETED_CREATE_TABLE);
                db.execSQL(SQL_LIST_ITEMS_MANUALLY_AUTOINCREMENT);

                populateDatabase(db);

                db.setTransactionSuccessful();

            } catch (SQLiteException e) {
                Log.e(TAG, e.toString());

            } finally {
                db.endTransaction();
            }
        }
    }

    private void populateDatabase(@NonNull final SQLiteDatabase db) {
        // Adding default favorite colors;
        final int boxesNumber =
                getAppContext()
                .getResources()
                .getInteger(R.integer.fragment_color_picker_favorite_boxes_number);
        for (int i = 0; i < boxesNumber; i++) {
            ColorPickerDbHelper.insertFavoriteColor(
                    db,
                    i,
                    ColorPickerDbHelper.getFavoriteColorsDefault()[i]);
        }
    }


    /*
        ...
     */

    /**
     * Get entries from database with specified parameters.
     * @param context       Context.
     * @param tableName     Table name.
     * @param queryBuilder  Query builder, contains specified params. If null, then get all entries.
     * @return Result cursor.
     */
    @Nullable
    public static Cursor getEntries(
            @NonNull final Context context,
            @NonNull final String tableName,
            @Nullable final DbQueryBuilder queryBuilder) {

        try {
            final SQLiteDatabase db = getReadableDb(context);
            if (db == null) {
                throw new SQLiteException(getDbFailMessage());
            }
            return db.query(
                    tableName,
                    null,
                    queryBuilder == null ? null : queryBuilder.getSelectionResult(),
                    queryBuilder == null ? null : queryBuilder.getSelectionArgs(),
                    null,
                    null,
                    queryBuilder == null ? null : queryBuilder.getSortOrder());

        } catch (SQLiteException e) {
            Log.e(TAG, e.toString());
            showToast(getDbFailMessage(), Toast.LENGTH_SHORT);
        }

        return null;
    }

    /**
     * Get count of entries from database with specified parameters.
     * @param context       Context.
     * @param tableName     Table name.
     * @param queryBuilder  Query builder, contains specified params. If null, then count all entries.
     * @return              If success, then return count of rows. If error, then return -1.
     */
    public static int getEntriesCount(
            @NonNull final Context context,
            @SuppressWarnings("SameParameterValue") @NonNull final String tableName,
            @Nullable final DbQueryBuilder queryBuilder) {

        final Cursor cursor = getEntries(context, tableName, queryBuilder);
        if (cursor != null) {
            try {
                return cursor.getCount();
            } finally {
                cursor.close();
            }
        }

        return -1;
    }

    /**
     * Insert entry with single value, in database.
     * @param context       Context.
     * @param db            Database. If null, then get new.
     * @param tableName     Table, where to insert a entry.
     * @param columnName    Column name.
     * @param value         Value.
     */
    @SuppressWarnings({"UnusedReturnValue", "PMD.AvoidReassigningParameters"})
    public static boolean insertEntryWithSingleValue(
            @NonNull final Context context,
            @Nullable SQLiteDatabase db,
            @NonNull final String tableName,
            @SuppressWarnings("SameParameterValue") @NonNull final String columnName,
            @NonNull final String value) {

        try {
            if (db == null) {
                db = DbHelper.getWritableDb(context);
                if (db == null) {
                    throw new SQLiteException(getDbFailMessage());
                }
            }

            // If exists, do nothing.
            final boolean exists = checkExists(
                    context,
                    tableName,
                    columnName,
                    value);
            if (exists) {
                return false;
            }

            final ContentValues cv = new ContentValues();
            cv.put(columnName, value);
            if (db.insert(tableName, null, cv) == -1) {
                final String error = String.format(Locale.ENGLISH,
                        "Insert entry is failed: {%s: %s}",
                        columnName, value);
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
     * Check entry with single value, in database.
     * @param context       Context.
     * @param tableName     Table, where checks a entry.
     * @param columnName    Column name.
     * @param value         Value.
     * @return              True if ok, otherwise false.
     */
    @SuppressWarnings("WeakerAccess")
    public static boolean checkExists(
            @NonNull final Context context,
            @NonNull final String tableName,
            @NonNull final String columnName,
            @NonNull final String value) {

        final DbQueryBuilder queryBuilder = new DbQueryBuilder();
        queryBuilder.addOr(
                columnName,
                DbQueryBuilder.OPERATOR_EQUALS,
                new String[]{value});

        final Cursor cursor = getEntries(
                context,
                tableName,
                queryBuilder);

        if (cursor != null) {
            try {
                if (cursor.getCount() > 0) {
                    return true;
                }
            } finally {
                cursor.close();
            }
        }
        return false;
    }

    /**
     * Delete entry with single value from database.
     * @param context       Context.
     * @param db            Database. If null, then get new.
     * @param tableName     Table, where to insert a entry.
     * @param columnName    Column name.
     * @param value         Value.
     * @param handleException Handle exception or not.
     * @return              True if success, otherwise false.
     */
    @SuppressWarnings({"PMD.AvoidReassigningParameters"})
    public static boolean deleteEntryWithSingle(
            @NonNull final Context context,
            @Nullable SQLiteDatabase db,
            @NonNull final String tableName,
            @SuppressWarnings("SameParameterValue") @NonNull final String columnName,
            @NonNull final String value,
            final boolean handleException) {

        try {
            if (db == null) {
                db = getWritableDb(context);
                if (db == null) {
                    throw new SQLiteException(getDbFailMessage());
                }
            }

            int deleteResult = db.delete(
                    tableName,
                    columnName + " = ?",
                    new String[]{value});

            if (deleteResult == 0) {
                throw new SQLiteException("[ERROR] The number of rows affected is 0");
            }

            return true;

        } catch (SQLiteException e) {
            if (handleException) {
                Log.e(TAG, e.toString());
                showToastRunOnUiThread(getDbFailMessage(), Toast.LENGTH_SHORT);
            } else {
                return true;
            }
        }

        return false;
    }

    /**
     * Finding cursor position by given parameters.
     * @param cursor Cursor.
     * @param column Column.
     * @param value  Column value.
     * @return Position. Positive number if found, otherwise '-1'.
     */
    public static int findCursorPositionByColumnValue(
            @NonNull final Cursor cursor,
            @SuppressWarnings("SameParameterValue") @NonNull final String column,
            @NonNull final String value) {

        while (cursor.moveToNext()) {
            final String valueSeek = cursor.getString(cursor.getColumnIndex(column));
            if (value.equals(valueSeek)) {
                return cursor.getPosition();
            }
        }
        return -1;
    }

    /**
     * Finding column value by cursor position.
     * @param cursor    Cursor.
     * @param column    Column.
     * @param position  Position.
     * @return Value if found, else null.
     */
    @Nullable
    public static String findColumnValueByCursorPosition(
            @NonNull final Cursor cursor,
            @SuppressWarnings("SameParameterValue") @NonNull final String column,
            final int position) {

        if (cursor.moveToPosition(position)) {
            return cursor.getString(cursor.getColumnIndex(column));
        }
        return null;
    }

    public static void clearInstances() {
        INSTANCES.clear();
    }


    /*
        Getters
     */

    @Nullable
    public static SQLiteDatabase getWritableDb(@NonNull final Context context) {
        final DbHelper dbHelper = getInstance(context);
        return dbHelper == null ? null : dbHelper.getWritableDatabase();
    }

    @Nullable
    public static SQLiteDatabase getReadableDb(@NonNull final Context context) {
        final DbHelper dbHelper = getInstance(context);
        return dbHelper == null ? null : dbHelper.getReadableDatabase();
    }

    @NonNull
    public static String getDbFailMessage() {
        return DB_FAILED_MESSAGE;
    }
}
