package com.gamaliev.notes.color_picker.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.db.DbHelper;
import com.gamaliev.notes.common.shared_prefs.SpUsers;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static com.gamaliev.notes.UtilsTest.clearUserPrefs;
import static com.gamaliev.notes.UtilsTest.initDefaultPrefs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
public class ColorPickerDbHelperTest {

    /*
        Init
     */

    @Before
    public void before() throws Exception {
        initDefaultPrefs();
    }


    /*
        Tests
     */

    @Test
    public void updateFavoriteColor() throws Exception {
        final Context context   = InstrumentationRegistry.getTargetContext();
        final int colorIndex    = 777;
        final int color         = 777123;
        final int colorNew      = 123777;

        final String selectedUserId = SpUsers.getSelected(context);

        // #
        assertNotNull(selectedUserId);

        final SQLiteDatabase writableDb = DbHelper.getWritableDb(context);

        // #
        assertNotNull(writableDb);
        assertTrue(writableDb.isOpen());
        assertFalse(writableDb.isReadOnly());

        ColorPickerDbHelper.insertFavoriteColor(
                writableDb,
                colorIndex,
                color);

        final int colorFromDb = ColorPickerDbHelper.getFavoriteColor(context, colorIndex);

        // #
        assertEquals(color, colorFromDb);

        boolean updateResult = ColorPickerDbHelper.updateFavoriteColor(context, colorIndex, colorNew);

        // #
        assertTrue(updateResult);

        final int colorFromDbNew = ColorPickerDbHelper.getFavoriteColor(context, colorIndex);

        // #
        assertEquals(colorNew, colorFromDbNew);
    }

    @Test
    public void getAllFavoriteColors() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "123";
        final int[] colors = {777123, 123777};
        int boxesNumber = context
                .getResources()
                .getInteger(R.integer.fragment_color_picker_favorite_boxes_number);

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);

        final int[] colorsA = ColorPickerDbHelper.getAllFavoriteColors(context);

        // #
        assertTrue(colorsA.length == boxesNumber);

        final SQLiteDatabase writableDb = DbHelper.getWritableDb(context);

        // #
        assertNotNull(writableDb);
        assertTrue(writableDb.isOpen());
        assertFalse(writableDb.isReadOnly());

        ColorPickerDbHelper.insertFavoriteColor(writableDb, boxesNumber, colors[0]);
        ColorPickerDbHelper.insertFavoriteColor(writableDb, ++boxesNumber, colors[1]);
        final int[] colorsExpected = new int[colors.length + colorsA.length];
        System.arraycopy(colors, 0, colorsExpected, 0, colors.length);
        System.arraycopy(colorsA, 0, colorsExpected, colors.length, colorsA.length);
        Arrays.sort(colorsExpected);
        final int[] colorsActual = ColorPickerDbHelper.getAllFavoriteColors(context);
        Arrays.sort(colorsActual);

        // #
        assertTrue(Arrays.equals(colorsExpected, colorsActual));
    }

    @Test
    public void getFavoriteColor() throws Exception {
        final Context context   = InstrumentationRegistry.getTargetContext();
        final int colorIndex    = 777;
        final int color         = 777123;
        final int colorNew      = 123777;

        final String selectedUserId = SpUsers.getSelected(context);

        // #
        assertNotNull(selectedUserId);

        final SQLiteDatabase writableDb = DbHelper.getWritableDb(context);

        // #
        assertNotNull(writableDb);
        assertTrue(writableDb.isOpen());
        assertFalse(writableDb.isReadOnly());

        ColorPickerDbHelper.insertFavoriteColor(
                writableDb,
                colorIndex,
                color);

        final int colorFromDb = ColorPickerDbHelper.getFavoriteColor(context, colorIndex);

        // #
        assertEquals(color, colorFromDb);

        boolean updateResult = ColorPickerDbHelper.updateFavoriteColor(context, colorIndex, colorNew);

        // #
        assertTrue(updateResult);

        final int colorFromDbNew = ColorPickerDbHelper.getFavoriteColor(context, colorIndex);

        // #
        assertEquals(colorNew, colorFromDbNew);
    }

    @Test
    public void insertFavoriteColor() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "123";
        final int[] colors = {777123, 123777};
        int boxesNumber = context
                .getResources()
                .getInteger(R.integer.fragment_color_picker_favorite_boxes_number);

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);

        final int[] colorsA = ColorPickerDbHelper.getAllFavoriteColors(context);

        // #
        assertTrue(colorsA.length == boxesNumber);

        final SQLiteDatabase writableDb = DbHelper.getWritableDb(context);

        // #
        assertNotNull(writableDb);
        assertTrue(writableDb.isOpen());
        assertFalse(writableDb.isReadOnly());

        ColorPickerDbHelper.insertFavoriteColor(writableDb, boxesNumber, colors[0]);
        ColorPickerDbHelper.insertFavoriteColor(writableDb, ++boxesNumber, colors[1]);
        final int[] colorsExpected = new int[colors.length + colorsA.length];
        System.arraycopy(colors, 0, colorsExpected, 0, colors.length);
        System.arraycopy(colorsA, 0, colorsExpected, colors.length, colorsA.length);
        Arrays.sort(colorsExpected);
        final int[] colorsActual = ColorPickerDbHelper.getAllFavoriteColors(context);
        Arrays.sort(colorsActual);

        // #
        assertTrue(Arrays.equals(colorsExpected, colorsActual));
    }

    @Test
    public void getFavoriteColorsDefault() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        int boxesNumber = context
                .getResources()
                .getInteger(R.integer.fragment_color_picker_favorite_boxes_number);
        final int[] favoriteColorsDefault = ColorPickerDbHelper.getFavoriteColorsDefault();

        assertEquals(favoriteColorsDefault.length, boxesNumber);
    }
}