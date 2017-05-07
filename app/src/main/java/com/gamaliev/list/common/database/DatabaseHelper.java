package com.gamaliev.list.common.database;

import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.gamaliev.list.R;
import com.gamaliev.list.colorpicker.database.ColorPickerDatabaseHelper;
import com.gamaliev.list.list.database.ListDatabaseMockHelper;

import static com.gamaliev.list.common.CommonUtils.showToast;

/**
 * @author Vadim Gamaliev
 * <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class DatabaseHelper extends SQLiteOpenHelper implements AutoCloseable {

    /* Logger */
    private static final String TAG = DatabaseHelper.class.getSimpleName();

    /* Basic */
    private static final String DB_NAME                     = "ya_school_app";
    private static final int DB_VERSION_A                   = 1;
    private static final int DB_VERSION                     = DB_VERSION_A;

    public static final String BASE_COLUMN_ID               = BaseColumns._ID;
    public static final String ORDER_ASCENDING              = "ASC";
    public static final String ORDER_DESCENDING             = "DESC";
    public static final String ORDER_ASC_DESC_DEFAULT       = ORDER_ASCENDING;
    public static final String ORDER_COLUMN_DEFAULT         = BASE_COLUMN_ID;

    /* Favorite table */
    protected static final String FAVORITE_TABLE_NAME       = "favorite_colors";
    protected static final String FAVORITE_COLUMN_INDEX     = "tbl_index";
    public static final String FAVORITE_COLUMN_COLOR        = "color";

    /* List items table */
    protected static final String LIST_ITEMS_TABLE_NAME     = "list_items";
    public static final String LIST_ITEMS_COLUMN_TITLE      = "title";
    public static final String LIST_ITEMS_COLUMN_DESCRIPTION = "description";
    public static final String LIST_ITEMS_COLUMN_COLOR      = "color";
    public static final String LIST_ITEMS_COLUMN_CREATED    = "created";
    public static final String LIST_ITEMS_COLUMN_EDITED     = "edited";
    public static final String LIST_ITEMS_COLUMN_VIEWED     = "viewed";

    /* Queries */
    protected static final String SQL_FAVORITE_CREATE_TABLE =
            "CREATE TABLE " + FAVORITE_TABLE_NAME + " (" +
                    BASE_COLUMN_ID +                        " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    FAVORITE_COLUMN_INDEX +                 " INTEGER NOT NULL, " +
                    FAVORITE_COLUMN_COLOR +                 " INTEGER NOT NULL); ";

    protected static final String SQL_LIST_ITEMS_CREATE_TABLE =
            "CREATE TABLE " + LIST_ITEMS_TABLE_NAME + " (" +
                    BASE_COLUMN_ID +                        " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    LIST_ITEMS_COLUMN_TITLE +               " TEXT, " +
                    LIST_ITEMS_COLUMN_DESCRIPTION +         " TEXT, " +
                    LIST_ITEMS_COLUMN_COLOR +               " INTEGER, " +
                    LIST_ITEMS_COLUMN_CREATED + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    LIST_ITEMS_COLUMN_EDITED +  " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    LIST_ITEMS_COLUMN_VIEWED +  " DATETIME DEFAULT CURRENT_TIMESTAMP); ";

    protected static final String SQL_LIST_ITEMS_DROP_TABLE =
            "DROP TABLE " + LIST_ITEMS_TABLE_NAME + ";";

    /* Local */
    @NonNull protected Context mContext;
    @NonNull protected Resources mRes;
    @NonNull protected String mDbFailMessage;


    /*
        Init
     */

    public DatabaseHelper(@NonNull final Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        init(context);
    }

    private void init(@NonNull final Context context) {
        mContext = context;
        mRes = context.getResources();
        mDbFailMessage = mRes.getString(R.string.sql_toast_fail);
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

        // Creating a table and populating with default values
        if (oldVersion == 0) {

            // Begin transaction.
            db.beginTransaction();

            try {
                // Create tables
                db.execSQL(SQL_FAVORITE_CREATE_TABLE);
                db.execSQL(SQL_LIST_ITEMS_CREATE_TABLE);

                // Populating.
                populateDatabase(db);

                // If ok.
                db.setTransactionSuccessful();

            } catch (SQLiteException e) {
                Log.e(TAG, e.toString());
                showToast(mContext, mDbFailMessage, Toast.LENGTH_SHORT);

            } finally {
                db.endTransaction();
            }
        }
    }

    /**
     * Populating database with default and mock values.
     * @param db Opened database.
     */
    private void populateDatabase(@NonNull final SQLiteDatabase db) {
        // Adding default favorite colors;
        final int boxesNumber = mRes.getInteger(R.integer.activity_color_picker_favorite_boxes_number);
        for (int i = 0; i < boxesNumber; i++) {
            ColorPickerDatabaseHelper.insertFavoriteColor(
                    db,
                    i,
                    ColorPickerDatabaseHelper.FAVORITE_COLORS_DEFAULT[i]);
        }

        // Adding mock entries in list activity.
        ListDatabaseMockHelper.addMockEntries(
                mRes.getInteger(R.integer.mock_items_number_start),
                db,
                null);
    }
}
