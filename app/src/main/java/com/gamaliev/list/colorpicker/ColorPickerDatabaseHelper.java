package com.gamaliev.list.colorpicker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
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

final class ColorPickerDatabaseHelper extends DatabaseHelper {

    private static final String TAG = ColorPickerDatabaseHelper.class.getSimpleName();


    /*
        Init
     */

    ColorPickerDatabaseHelper(@NonNull final Context context) {
        super(context);
    }

    ColorPickerDatabaseHelper(@NonNull final Context context,
                              @NonNull final String name,
                              @NonNull final SQLiteDatabase.CursorFactory factory,
                              final int version) {
        super(context, name, factory, version);
    }

    ColorPickerDatabaseHelper(@NonNull final Context context,
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
     * Update color in database.
     * @param index index of color.
     * @param color color.
     * @return true if update is success, otherwise false.
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
            db.close();
            return true;

        } catch (SQLiteException e) {
            final String error = String.format(
                    Locale.ENGLISH,
                    "[ERROR] Update favorite color {%s: %d, %s: %d}",
                    FAVORITE_COLUMN_INDEX, index, FAVORITE_COLUMN_COLOR, color);
            Log.e(TAG, error + ": " + e.getMessage());
            showToast(context, dbFailMessage, Toast.LENGTH_SHORT);
            return false;
        }
    }

    /**
     * Get color from database.
     * @param index index of color
     * @return color number if success, otherwise "-1".
     */
    int getFavoriteColor(final int index) {
        int color = -1;

        try (   SQLiteDatabase db = getReadableDatabase();
                Cursor cursor = db.query(
                    FAVORITE_TABLE_NAME,
                    new String[]{FAVORITE_COLUMN_COLOR},
                    FAVORITE_COLUMN_INDEX + " = ?",
                    new String[]{Integer.toString(index)},
                    null, null, null)) {

            if (cursor.moveToFirst()) {
                color = cursor.getInt(0);
            }
            return color;

        } catch (SQLiteException e) {
            final String error = String.format(
                    Locale.ENGLISH,
                    "[ERROR] Get favorite color {%s: %d}", FAVORITE_COLUMN_INDEX, index);
            Log.e(TAG, error + ": " + e.getMessage());
            showToast(context, dbFailMessage, Toast.LENGTH_SHORT);
            return color;
        }
    }
}
