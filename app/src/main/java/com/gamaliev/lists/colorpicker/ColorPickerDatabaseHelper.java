package com.gamaliev.lists.colorpicker;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.gamaliev.lists.R;

import java.util.Locale;

import static com.gamaliev.lists.common.CommonUtils.showToast;

/**
 * @author Vadim Gamaliev
 * <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

final class ColorPickerDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = ColorPickerDatabaseHelper.class.getSimpleName();
    private static final String DB_NAME = "ya_school_app";  // TODO: extract to common databasehelper
    private static final int DB_VERSION = 1;                // TODO: extract to common databasehelper

    @NonNull private ColorPickerActivity context;
    @NonNull private Resources resources;
    @NonNull private String dbFailMessage;


    /*
        Init
     */

    public ColorPickerDatabaseHelper(@NonNull final Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        init(context);
    }

    public ColorPickerDatabaseHelper(@NonNull final Context context,
                                     @NonNull final String name,
                                     @NonNull final SQLiteDatabase.CursorFactory factory,
                                     final int version) {
        super(context, name, factory, version);
        init(context);
    }

    public ColorPickerDatabaseHelper(@NonNull final Context context,
                                     @NonNull final String name,
                                     @NonNull final SQLiteDatabase.CursorFactory factory,
                                     final int version,
                                     @NonNull final DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
        init(context);
    }

    private void init(@NonNull final Context context) {
        this.context    = (ColorPickerActivity) context;
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
                db.execSQL(resources.getString(R.string.sql_init_v1));
                for (int i = 1; i < context.getHsvColors().length; i += 2) {
                    insertFavoriteColor(db, i, context.getHsvColors()[i]);
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
        cv.put("tbl_index", index);
        cv.put("color", color);
        if (db.insert("favorite_colors", null, cv) == -1) {
            throw new SQLiteException(String.format(
                    Locale.ENGLISH,
                    "[ERROR] Insert favorite color {tbl_index: %d, color: %d}", index, color));
        }
    }

    /**
     * Update color in database.
     * @param index index of color.
     * @param color color.
     * @return true if update is success, otherwise false.
     */
    boolean updateFavoriteColor(final int index, final int color) {
        SQLiteDatabase db = null;

        try {
            db = getWritableDatabase();
            final ContentValues cv = new ContentValues();
            cv.put("color", color);
            db.update(
                    "favorite_colors",
                    cv,
                    "tbl_index = ?",
                    new String[] {Integer.toString(index)});
            db.close();
            return true;

        } catch (SQLiteException e) {
            final String error = String.format(
                    Locale.ENGLISH,
                    "[ERROR] Update favorite color {tbl_index: %d, color: %d}", index, color);
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
     * Get color from database.
     * @param index index of color
     * @return color number if success, otherwise "-1".
     */
    int getFavoriteColor(final int index) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        int color = -1;

        try {
            db      = getReadableDatabase();
            cursor  = db.query(
                    "favorite_colors",
                    new String[] {"color"},
                    "tbl_index = ?",
                    new String[] {Integer.toString(index)},
                    null, null, null);

            if (cursor.moveToFirst()) {
                color = cursor.getInt(0);
            }
            return color;

        } catch (SQLiteException e) {
            final String error = String.format(
                    Locale.ENGLISH,
                    "[ERROR] Get favorite color {tbl_index: %d}", index);
            Log.e(TAG, error + ": " + e.getMessage());
            showToast(context, dbFailMessage, Toast.LENGTH_SHORT);
            return color;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
    }


    /*
        Get default color
     */

    /**
     * @return default color. See color resource with name "colorPickerDefault".
     */
    int getDefaultColor() {
        int defaultColor;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            defaultColor = resources.getColor(R.color.colorPickerDefault, null);
        } else {
            defaultColor = resources.getColor(R.color.colorPickerDefault);
        }
        return defaultColor;
    }
}
