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
import android.util.Log;
import android.widget.Toast;

import com.gamaliev.notes.R;
import com.gamaliev.notes.app.NotesApp;
import com.gamaliev.notes.colorpicker.db.ColorPickerDbHelper;
import com.gamaliev.notes.common.shared_prefs.SpUsers;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.gamaliev.notes.common.CommonUtils.showToast;
import static com.gamaliev.notes.common.CommonUtils.showToastRunOnUiThread;

/**
 * @author Vadim Gamaliev
 * <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class DbHelper extends SQLiteOpenHelper {

    /* Logger */
    private static final String TAG = DbHelper.class.getSimpleName();

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
    public static final String LIST_ITEMS_COLUMN_TITLE      = "title";
    public static final String LIST_ITEMS_COLUMN_SYNC_ID_JSON = "id";
    public static final String LIST_ITEMS_COLUMN_DESCRIPTION = "description";
    public static final String LIST_ITEMS_COLUMN_COLOR      = "color";
    public static final String LIST_ITEMS_COLUMN_IMAGE_URL  = "imageUrl";
    public static final String LIST_ITEMS_COLUMN_CREATED    = "created";
    public static final String LIST_ITEMS_COLUMN_EDITED     = "edited";
    public static final String LIST_ITEMS_COLUMN_VIEWED     = "viewed";

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
            "CREATE TABLE " + FAVORITE_TABLE_NAME + " (" +
                    BASE_COLUMN_ID +                        " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    FAVORITE_COLUMN_INDEX +                 " INTEGER NOT NULL UNIQUE, " +
                    FAVORITE_COLUMN_COLOR +                 " INTEGER NOT NULL); ";

    /* Entries */
    public static final String SQL_LIST_ITEMS_CREATE_TABLE =
            "CREATE TABLE " + LIST_ITEMS_TABLE_NAME + " (" +
                    BASE_COLUMN_ID +                        " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    LIST_ITEMS_COLUMN_TITLE +               " TEXT, " +
                    COMMON_COLUMN_SYNC_ID +                 " INTEGER, " +
                    LIST_ITEMS_COLUMN_DESCRIPTION +         " TEXT, " +
                    LIST_ITEMS_COLUMN_COLOR +               " INTEGER, " +
                    LIST_ITEMS_COLUMN_IMAGE_URL +           " TEXT, " +
                    LIST_ITEMS_COLUMN_CREATED + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    LIST_ITEMS_COLUMN_EDITED +  " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    LIST_ITEMS_COLUMN_VIEWED +  " DATETIME DEFAULT CURRENT_TIMESTAMP); ";

    /* Entries. Drop */
    public static final String SQL_LIST_ITEMS_DROP_TABLE =
            "DROP TABLE " + LIST_ITEMS_TABLE_NAME + ";";

    /* Sync. Journal table */
    public static final String SQL_SYNC_CREATE_TABLE =
            "CREATE TABLE " + SYNC_TABLE_NAME + " (" +
                    BASE_COLUMN_ID +                        " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    SYNC_COLUMN_FINISHED +                  " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    SYNC_COLUMN_ACTION +                    " INTEGER NOT NULL, " +
                    SYNC_COLUMN_STATUS +                    " INTEGER NOT NULL, " +
                    SYNC_COLUMN_AMOUNT +                    " INTEGER NOT NULL); ";

    /* Sync. Journal table. Drop */
    public static final String SQL_SYNC_DROP_TABLE =
            "DROP TABLE " + SYNC_TABLE_NAME + ";";

    /* Sync. Conflict table */
    private static final String SQL_SYNC_CONFLICT_CREATE_TABLE =
            "CREATE TABLE " + SYNC_CONFLICT_TABLE_NAME + " (" +
                    BASE_COLUMN_ID +                        " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COMMON_COLUMN_SYNC_ID +                 " INTEGER NOT NULL UNIQUE); ";

    /* Sync. Deleted table */
    private static final String SQL_SYNC_DELETED_CREATE_TABLE =
            "CREATE TABLE " + SYNC_DELETED_TABLE_NAME + " (" +
                    BASE_COLUMN_ID +                        " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COMMON_COLUMN_SYNC_ID +                 " INTEGER NOT NULL UNIQUE); ";

    /* Instances */
    @NonNull private static Map<String, DbHelper> sInstances;

    /* ... */
    @NonNull private static String mDbFailMessage;


    /*
        Init
     */

    static {
        sInstances = new ConcurrentHashMap<>();
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
        if (userId == null) {
            return null;
        }

        //
        if (mDbFailMessage == null) {
            mDbFailMessage = context.getString(R.string.sql_toast_fail);
        }

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstances.get(userId) == null) {
            sInstances.put(userId, new DbHelper(context.getApplicationContext(), userId));
        }
        return sInstances.get(userId);
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
    public void onCreate(SQLiteDatabase db) {
        updateDatabase(db, 0, DB_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        updateDatabase(db, oldVersion, newVersion);
    }

    /**
     * Creating a new table and populating with default values, or update if database exist
     * @param db            Database.
     * @param oldVersion    Old version of database.
     * @param newVersion    New version of database.
     */
    private void updateDatabase(
            @NonNull final SQLiteDatabase db,
            final int oldVersion,
            final int newVersion) {

        //
        if (oldVersion == 0) {

            // Begin transaction.
            db.beginTransaction();

            try {
                // Create tables
                db.execSQL(SQL_FAVORITE_CREATE_TABLE);
                db.execSQL(SQL_LIST_ITEMS_CREATE_TABLE);
                db.execSQL(SQL_SYNC_CREATE_TABLE);
                db.execSQL(SQL_SYNC_CONFLICT_CREATE_TABLE);
                db.execSQL(SQL_SYNC_DELETED_CREATE_TABLE);

                // Populating.
                populateDatabase(db);

                // If ok.
                db.setTransactionSuccessful();

            } catch (SQLiteException e) {
                Log.e(TAG, e.toString());

            } finally {
                db.endTransaction();
            }
        }
    }

    /**
     * Populating database with default values.
     * @param db Database.
     */
    private void populateDatabase(@NonNull final SQLiteDatabase db) {

        // Adding default favorite colors;
        final int boxesNumber = NotesApp
                .getAppContext()
                .getResources()
                .getInteger(R.integer.activity_color_picker_favorite_boxes_number);

        for (int i = 0; i < boxesNumber; i++) {
            ColorPickerDbHelper.insertFavoriteColor(
                    db,
                    i,
                    ColorPickerDbHelper.FAVORITE_COLORS_DEFAULT[i]);
        }
    }


    /*
        Common
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

            // Make query and return cursor, if ok;
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
            showToast(context, getDbFailMessage(), Toast.LENGTH_SHORT);
            return null;
        }
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
            @NonNull final String tableName,
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
     * @return              True if ok, otherwise false.
     */
    public static boolean insertEntryWithSingleValue(
            @NonNull final Context context,
            @Nullable SQLiteDatabase db,
            @NonNull final String tableName,
            @NonNull final String columnName,
            @NonNull final String value) {

        if (db == null) {
            db = DbHelper.getWritableDb(context);
        }

        // Check exists.
        final boolean exists = checkExists(
                context,
                tableName,
                columnName,
                value);
        if (exists) {
            return true;
        }

        try {
            // Content values
            final ContentValues cv = new ContentValues();
            cv.put(columnName, value);

            // Insert
            if (db.insert(tableName, null, cv) == -1) {
                final String error = String.format(Locale.ENGLISH,
                        "[ERROR] Insert entry {%s: %d}",
                        columnName, value);
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
     * Check entry with single value, in database.
     * @param context       Context.
     * @param tableName     Table, where checks a entry.
     * @param columnName    Column name.
     * @param value         Value.
     * @return              True if ok, otherwise false.
     */
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
    public static boolean deleteEntryWithSingle(
            @NonNull final Context context,
            @Nullable SQLiteDatabase db,
            @NonNull final String tableName,
            @NonNull final String columnName,
            @NonNull final String value,
            final boolean handleException) {

        try {
            if (db == null) {
                db = getWritableDb(context);
            }

            // Delete query.
            int deleteResult = db.delete(
                    tableName,
                    columnName + " = ?",
                    new String[]{value});

            // If error.
            if (deleteResult == 0) {
                throw new SQLiteException("[ERROR] The number of rows affected is 0");
            }

            // If ok.
            return true;

        } catch (SQLiteException e) {
            if (handleException) {
                Log.e(TAG, e.toString());
                showToastRunOnUiThread(context, getDbFailMessage(), Toast.LENGTH_SHORT);
                return false;
            } else {
                return true;
            }
        }
    }


    /*
        Getters
     */

    @NonNull
    public static SQLiteDatabase getWritableDb(@NonNull final Context context) {
        return getInstance(context).getWritableDatabase();
    }

    @NonNull
    public static SQLiteDatabase getReadableDb(@NonNull final Context context) {
        return getInstance(context).getReadableDatabase();
    }

    @NonNull
    public static String getDbFailMessage() {
        return mDbFailMessage;
    }


    /*
        ...
     */

    public static void clearInstances() {
        sInstances.clear();
    }
}
