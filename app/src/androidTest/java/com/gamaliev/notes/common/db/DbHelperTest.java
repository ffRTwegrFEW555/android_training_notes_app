package com.gamaliev.notes.common.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;

import com.gamaliev.notes.common.shared_prefs.SpUsers;

import org.junit.Before;
import org.junit.Test;

import static com.gamaliev.notes.TestUtils.clearUserPrefs;
import static com.gamaliev.notes.TestUtils.initDefaultPrefs;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_TITLE;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_TABLE_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
public class DbHelperTest {

    @Before
    public void before() throws Exception {
        initDefaultPrefs();
    }

    @Test
    public void getInstance() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "123";
        final String userId2 = "1234";

        clearUserPrefs(userId);
        clearUserPrefs(userId2);
        SpUsers.setSelected(context, userId);
        final Cursor cursor1 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor1);
        assertEquals(cursor1.getCount(), 0);

        DbHelper.insertEntryWithSingleValue(
                context,
                DbHelper.getWritableDb(context),
                LIST_ITEMS_TABLE_NAME,
                LIST_ITEMS_COLUMN_TITLE,
                "title_title");
        final Cursor cursor2 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor2);
        assertEquals(cursor2.getCount(), 1);

        SpUsers.setSelected(context, userId2);
        final Cursor cursor3 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor3);
        assertEquals(cursor3.getCount(), 0);
    }

    @Test
    public void onCreate() throws Exception {
    }

    @Test
    public void onUpgrade() throws Exception {
    }

    @Test
    public void getEntries() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "123";
        final String value = "title_title";

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);
        final Cursor cursor1 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor1);
        assertEquals(cursor1.getCount(), 0);

        cursor1.close();
        DbHelper.insertEntryWithSingleValue(
                context,
                DbHelper.getWritableDb(context),
                LIST_ITEMS_TABLE_NAME,
                LIST_ITEMS_COLUMN_TITLE,
                value);
        final Cursor cursor2 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor2);
        assertEquals(cursor2.getCount(), 1);

        cursor2.moveToNext();
        final String valueFromCursor = cursor2.getString(cursor2.getColumnIndex(LIST_ITEMS_COLUMN_TITLE));
        cursor2.close();

        // #
        assertEquals(value, valueFromCursor);
    }

    @Test
    public void getEntriesCount() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "123";
        final String userId2 = "1234";

        clearUserPrefs(userId);
        clearUserPrefs(userId2);
        SpUsers.setSelected(context, userId);
        int n = DbHelper.getEntriesCount(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertEquals(n, 0);

        DbHelper.insertEntryWithSingleValue(
                context,
                DbHelper.getWritableDb(context),
                LIST_ITEMS_TABLE_NAME,
                LIST_ITEMS_COLUMN_TITLE,
                "title_title");
        int n2 = DbHelper.getEntriesCount(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertEquals(n2, 1);

        SpUsers.setSelected(context, userId2);
        int n3 = DbHelper.getEntriesCount(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertEquals(n3, 0);
    }

    @Test
    public void insertEntryWithSingleValue() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "123";
        final String value = "title_title";

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);
        final Cursor cursor1 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor1);
        assertEquals(cursor1.getCount(), 0);

        cursor1.close();
        DbHelper.insertEntryWithSingleValue(
                context,
                DbHelper.getWritableDb(context),
                LIST_ITEMS_TABLE_NAME,
                LIST_ITEMS_COLUMN_TITLE,
                value);
        final Cursor cursor2 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor2);
        assertEquals(cursor2.getCount(), 1);

        cursor2.moveToNext();
        final String valueFromCursor = cursor2.getString(cursor2.getColumnIndex(LIST_ITEMS_COLUMN_TITLE));
        cursor2.close();

        // #
        assertEquals(value, valueFromCursor);
    }

    @Test
    public void checkExists() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "123";
        final String value = "title_title";

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);
        boolean result = DbHelper.checkExists(context, LIST_ITEMS_TABLE_NAME, LIST_ITEMS_COLUMN_TITLE, value);

        // #
        assertFalse(result);

        DbHelper.insertEntryWithSingleValue(
                context,
                DbHelper.getWritableDb(context),
                LIST_ITEMS_TABLE_NAME,
                LIST_ITEMS_COLUMN_TITLE,
                value);
        boolean result2 = DbHelper.checkExists(context, LIST_ITEMS_TABLE_NAME, LIST_ITEMS_COLUMN_TITLE, value);

        // #
        assertTrue(result2);
    }

    @Test
    public void deleteEntryWithSingle() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "123";
        final String value = "title_title";

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);
        final Cursor cursor1 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor1);
        assertEquals(cursor1.getCount(), 0);

        cursor1.close();
        DbHelper.insertEntryWithSingleValue(
                context,
                DbHelper.getWritableDb(context),
                LIST_ITEMS_TABLE_NAME,
                LIST_ITEMS_COLUMN_TITLE,
                value);
        final Cursor cursor2 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor2);
        assertEquals(cursor2.getCount(), 1);

        cursor2.close();
        boolean result = DbHelper.deleteEntryWithSingle(
                context,
                DbHelper.getWritableDb(context),
                LIST_ITEMS_TABLE_NAME,
                LIST_ITEMS_COLUMN_TITLE,
                value,
                false);

        // #
        assertTrue(result);

        final Cursor cursor3 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor3);
        assertEquals(cursor3.getCount(), 0);
    }

    @Test
    public void findCursorPositionByColumnValue() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "123";
        final String value = "title_title";

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);
        final Cursor cursor1 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor1);
        assertEquals(cursor1.getCount(), 0);

        cursor1.close();
        DbHelper.insertEntryWithSingleValue(
                context,
                DbHelper.getWritableDb(context),
                LIST_ITEMS_TABLE_NAME,
                LIST_ITEMS_COLUMN_TITLE,
                value);
        final Cursor cursor2 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor2);
        assertEquals(cursor2.getCount(), 1);

        final int position =
                DbHelper.findCursorPositionByColumnValue(cursor2, LIST_ITEMS_COLUMN_TITLE, value);
        cursor2.close();

        // #
        assertEquals(position, 0);
    }

    @Test
    public void findColumnValueByCursorPosition() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "123";
        final String value = "title_title";

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);
        final Cursor cursor1 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor1);
        assertEquals(cursor1.getCount(), 0);

        cursor1.close();
        DbHelper.insertEntryWithSingleValue(
                context,
                DbHelper.getWritableDb(context),
                LIST_ITEMS_TABLE_NAME,
                LIST_ITEMS_COLUMN_TITLE,
                value);
        final Cursor cursor2 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor2);
        assertEquals(cursor2.getCount(), 1);

        final String columnValue =
                DbHelper.findColumnValueByCursorPosition(cursor2, LIST_ITEMS_COLUMN_TITLE, 0);
        cursor2.close();

        // #
        assertEquals(value, columnValue);
    }

    @Test
    public void clearInstances() throws Exception {
        DbHelper.clearInstances();
    }

    @Test
    public void getWritableDb() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "123";
        final String userId2 = "1234";

        clearUserPrefs(userId);
        clearUserPrefs(userId2);
        SpUsers.setSelected(context, userId);
        final SQLiteDatabase writableDb = DbHelper.getWritableDb(context);

        // #
        assertNotNull(writableDb);
        assertTrue(writableDb.isOpen());
        assertFalse(writableDb.isReadOnly());

        SpUsers.setSelected(context, userId2);
        final SQLiteDatabase writableDb2 = DbHelper.getWritableDb(context);

        // #
        assertNotNull(writableDb2);
        assertTrue(writableDb2.isOpen());
        assertFalse(writableDb2.isReadOnly());
    }

    @Test
    public void getReadableDb() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "123";
        final String userId2 = "1234";

        clearUserPrefs(userId);
        clearUserPrefs(userId2);
        SpUsers.setSelected(context, userId);
        final SQLiteDatabase readableDb = DbHelper.getReadableDb(context);

        // #
        assertNotNull(readableDb);
        assertTrue(readableDb.isOpen());

        SpUsers.setSelected(context, userId2);
        final SQLiteDatabase readableDb2 = DbHelper.getReadableDb(context);

        // #
        assertNotNull(readableDb2);
        assertTrue(readableDb2.isOpen());
    }

    @Test
    public void getDbFailMessage() throws Exception {
        final String dbFailMessage = DbHelper.getDbFailMessage();

        assertNotNull(dbFailMessage);
        assertTrue(dbFailMessage.length() > 0);
    }
}