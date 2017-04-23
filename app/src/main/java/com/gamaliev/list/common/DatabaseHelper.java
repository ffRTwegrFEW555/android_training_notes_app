package com.gamaliev.list.common;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.gamaliev.list.R;

import java.util.Locale;

import static com.gamaliev.list.common.CommonUtils.showToast;

/**
 * @author Vadim Gamaliev
 * <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = DatabaseHelper.class.getSimpleName();
    private static final String DB_NAME = "ya_school_app";
    private static final int DB_VERSION = 1;

    protected static final String FAVORITE_TABLE_NAME                   = "favorite_colors";
    protected static final String FAVORITE_COLUMN_INDEX                 = "tbl_index";
    protected static final String FAVORITE_COLUMN_COLOR                 = "color";

    protected static final String LIST_ITEMS_TABLE_NAME                 = "list_items";
    protected static final String LIST_ITEMS_COLUMN_NAME                = "name";
    protected static final String LIST_ITEMS_COLUMN_DESCRIPTION         = "description";
    protected static final String LIST_ITEMS_COLUMN_COLOR               = "color";
    protected static final String LIST_ITEMS_COLUMN_CREATED_TIMESTAMP   = "c_timestamp";
    protected static final String LIST_ITEMS_COLUMN_MODIFIED_TIMESTAMP  = "m_timestamp";

    protected static final String SQL_FAVORITE_CREATE_TABLE =
            "CREATE TABLE " + FAVORITE_TABLE_NAME + " (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    FAVORITE_COLUMN_INDEX +                 " INTEGER NOT NULL, " +
                    FAVORITE_COLUMN_COLOR +                 " INTEGER NOT NULL); ";

    protected static final String SQL_LIST_ITEMS_CREATE_TABLE =
            "CREATE TABLE " + LIST_ITEMS_TABLE_NAME + " (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    LIST_ITEMS_COLUMN_NAME +                " TEXT, " +
                    LIST_ITEMS_COLUMN_DESCRIPTION +         " TEXT, " +
                    LIST_ITEMS_COLUMN_COLOR +               " INTEGER NOT NULL, " +
                    LIST_ITEMS_COLUMN_CREATED_TIMESTAMP +   " DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    LIST_ITEMS_COLUMN_MODIFIED_TIMESTAMP +  " DATETIME NOT NULL);";

    protected static final String SQL_LIST_ITEMS_DROP_TABLE =
            "DROP TABLE " + LIST_ITEMS_TABLE_NAME + ";";

    @NonNull protected Context context;
    @NonNull protected Resources resources;
    @NonNull protected String dbFailMessage;


    /*
        Init
     */

    public DatabaseHelper(@NonNull final Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        init(context);
    }

    public DatabaseHelper(@NonNull final Context context,
                          @NonNull final String name,
                          @NonNull final SQLiteDatabase.CursorFactory factory,
                          final int version) {
        super(context, name, factory, version);
        init(context);
    }

    public DatabaseHelper(@NonNull final Context context,
                          @NonNull final String name,
                          @NonNull final SQLiteDatabase.CursorFactory factory,
                          final int version,
                          @NonNull final DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
        init(context);
    }

    private void init(@NonNull final Context context) {
        this.context    = context;
        resources       = context.getResources();
        dbFailMessage   = resources.getString(R.string.sql_toast_fail);
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
     * @param db database.
     * @param oldVersion old version of database.
     * @param newVersion new version of database.
     */
    private void updateDatabase(
            @NonNull final SQLiteDatabase db,
            final int oldVersion,
            final int newVersion) {

        // Creating a table and populating with default values
        if (oldVersion == 0) {
            db.beginTransaction();
            try {
                db.execSQL(SQL_FAVORITE_CREATE_TABLE);
                db.execSQL(SQL_LIST_ITEMS_CREATE_TABLE);

                // Adding default favorite colors;
                final int boxesNumber = resources.getInteger(R.integer.activity_color_picker_favorite_boxes_number);
                final int defaultColor = getDefaultColor(context);
                for (int i = 0; i < boxesNumber; i++) {
                    insertFavoriteColor(db, i, defaultColor);
                }

                db.setTransactionSuccessful();

            } catch (SQLiteException e) {
                Log.e(TAG, e.getMessage());
                showToast(context, dbFailMessage, Toast.LENGTH_SHORT);

            } finally {
                db.endTransaction();
            }
        }
    }

    /**
     * Insert row (color with index) in database.
     * @param db database.
     * @param index index of color.
     * @param color color.
     */
    private void insertFavoriteColor(
            @NonNull final SQLiteDatabase db,
            final int index,
            final int color) {

        final ContentValues cv = new ContentValues();
        cv.put(FAVORITE_COLUMN_INDEX, index);
        cv.put(FAVORITE_COLUMN_COLOR, color);
        if (db.insert(FAVORITE_TABLE_NAME, null, cv) == -1) {
            throw new SQLiteException(String.format(
                    Locale.ENGLISH,
                    "[ERROR] Insert favorite color {%s: %d, %s: %d}",
                    FAVORITE_COLUMN_INDEX, index, FAVORITE_COLUMN_COLOR, color));
        }
    }


    /*
        Get default color
     */

    /**
     * @return default color. See color resource with name "colorPickerDefault".
     */
    // TODO: handle
    public static int getDefaultColor(@NonNull final Context context) {
        int defaultColor;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            defaultColor = context.getResources().getColor(R.color.color_picker_default, null);
        } else {
            defaultColor = context.getResources().getColor(R.color.color_picker_default);
        }
        return defaultColor;
    }
}
