package com.gamaliev.list.colorpicker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.gamaliev.list.common.DatabaseHelper;

import java.util.Locale;

import static com.gamaliev.list.common.CommonUtils.showToast;

/**
 * @author Vadim Gamaliev
 * <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class ColorPickerDatabaseHelper extends DatabaseHelper {

    /* Logger */
    private static final String TAG = ColorPickerDatabaseHelper.class.getSimpleName();

    /* */
    public static final int[] FAVORITE_COLORS_DEFAULT = {
            -53248,     -28672,     -4096,      -5177600,
            -11469056,  -16711920,  -16711824,  -16711728,
            -16723713,  -16748289,  -16772865,  -11534081,
            -5242625,   -65296,     -65392,     -65488
    };


    /*
        Init
     */

    public ColorPickerDatabaseHelper(@NonNull final Context context) {
        super(context);
    }


    /*
        Methods
     */

    /**
     * Update color in database.
     * @param index Index of color.
     * @param color Color.
     * @return True if update is success, otherwise false.
     */
    boolean updateFavoriteColor(final int index, final int color) {

        try (SQLiteDatabase db = getWritableDatabase()) {
            final ContentValues cv = new ContentValues();
            cv.put(FAVORITE_COLUMN_COLOR, color);
            db.update(
                    FAVORITE_TABLE_NAME,
                    cv,
                    FAVORITE_COLUMN_INDEX + " = ?",
                    new String[] {Integer.toString(index)});
            return true;

        } catch (SQLiteException e) {
            Log.e(TAG, e.toString());
            showToast(mContext, mDbFailMessage, Toast.LENGTH_SHORT);
            return false;
        }
    }

    /**
     * Get color from database.
     * @param index Index of color
     * @return Color number if success, otherwise "-1".
     */
    public int getFavoriteColor(final int index) {
        int color = -1;

        try (   SQLiteDatabase db = getReadableDatabase();
                Cursor cursor = db.query(
                    FAVORITE_TABLE_NAME,
                    new String[]{FAVORITE_COLUMN_COLOR},
                    FAVORITE_COLUMN_INDEX + " = ?",
                    new String[]{Integer.toString(index)},
                    null, null, null)) {

            if (cursor.moveToFirst()) {
                final int indexColor = cursor.getColumnIndex(DatabaseHelper.LIST_ITEMS_COLUMN_COLOR);
                color = cursor.getInt(indexColor);
            }
            return color;

        } catch (SQLiteException e) {
            Log.e(TAG, e.toString());
            showToast(mContext, mDbFailMessage, Toast.LENGTH_SHORT);
            return color;
        }
    }

    /**
     * Insert row (color with index) in database.
     * @param db    Opened database.
     * @param index Index of color.
     * @param color Color.
     */
    public static void insertFavoriteColor(
            @NonNull final SQLiteDatabase db,
            final int index,
            final int color) {

        final ContentValues cv = new ContentValues();
        cv.put(FAVORITE_COLUMN_INDEX, index);
        cv.put(FAVORITE_COLUMN_COLOR, color);
        if (db.insert(FAVORITE_TABLE_NAME, null, cv) == -1) {
            throw new SQLiteException(String.format(Locale.ENGLISH,
                    "[ERROR] Insert favorite color {%s: %d, %s: %d}",
                    FAVORITE_COLUMN_INDEX, index, FAVORITE_COLUMN_COLOR, color));
        }
    }
}
