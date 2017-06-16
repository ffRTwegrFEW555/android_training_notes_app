package com.gamaliev.notes.list.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;

import com.gamaliev.notes.common.db.DbHelper;
import com.gamaliev.notes.common.db.DbQueryBuilder;
import com.gamaliev.notes.common.shared_prefs.SpUsers;
import com.gamaliev.notes.entity.ListEntry;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.gamaliev.notes.UtilsTest.clearUserPrefs;
import static com.gamaliev.notes.UtilsTest.initDefaultPrefs;
import static com.gamaliev.notes.common.db.DbHelper.BASE_COLUMN_ID;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_EDITED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_MANUALLY;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_TITLE;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_VIEWED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_TABLE_NAME;
import static com.gamaliev.notes.common.db.DbHelper.ORDER_ASC_DESC_DEFAULT;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_DELETED_TABLE_NAME;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_COLOR;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_CREATED;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_EDITED;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_ORDER;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_ORDER_ASC;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_VIEWED;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
public class ListDbHelperTest {

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
    public void insertUpdateEntry() throws Exception {
        final Context context       = InstrumentationRegistry.getTargetContext();
        final String userId         = "123";
        final ListEntry entry       = getNewListEntry();
        final String descriptionNew = "new description";

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);

        final int n1 = DbHelper.getEntriesCount(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertEquals(n1, 0);

        boolean insertResult = ListDbHelper.insertUpdateEntry(context, entry, false);

        // #
        assertTrue(insertResult);

        final Cursor cursor = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor);
        assertEquals(cursor.getCount(), 1);

        final String entryId = DbHelper.findColumnValueByCursorPosition(cursor, BASE_COLUMN_ID, 0);

        // #
        assertNotNull(entryId);

        cursor.close();
        final long entryIdLong = Long.parseLong(entryId);
        final ListEntry entryFromDb = ListDbHelper.getEntry(context, entryIdLong);
        entry.setId(entryIdLong);

        // #
        assertNotNull(entryFromDb);
        assertEquals(entry, entryFromDb);

        entry.setDescription(descriptionNew);
        boolean updateResult = ListDbHelper.insertUpdateEntry(context, entry, true);

        // #
        assertTrue(updateResult);

        final Cursor cursor2 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor2);
        assertEquals(cursor2.getCount(), 1);

        cursor2.close();
        final ListEntry entryFromDbUpdated = ListDbHelper.getEntry(context, entryIdLong);

        // #
        assertNotNull(entryFromDbUpdated);
        assertEquals(entry, entryFromDbUpdated);
    }

    @Test
    public void insertUpdateEntryWithGivenDb() throws Exception {
        final Context context       = InstrumentationRegistry.getTargetContext();
        final String userId         = "123";
        final ListEntry entry       = getNewListEntry();
        final String descriptionNew = "new description";

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);

        final SQLiteDatabase writableDb = DbHelper.getWritableDb(context);

        // #
        assertNotNull(writableDb);
        assertTrue(writableDb.isOpen());
        assertFalse(writableDb.isReadOnly());

        final int n1 = DbHelper.getEntriesCount(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertEquals(n1, 0);

        ListDbHelper.insertUpdateEntry(context, entry, writableDb, false);
        final Cursor cursor = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor);
        assertEquals(cursor.getCount(), 1);

        final String entryId = DbHelper.findColumnValueByCursorPosition(cursor, BASE_COLUMN_ID, 0);

        // #
        assertNotNull(entryId);

        cursor.close();
        final long entryIdLong = Long.parseLong(entryId);
        final ListEntry entryFromDb = ListDbHelper.getEntry(context, entryIdLong);
        entry.setId(entryIdLong);

        // #
        assertNotNull(entryFromDb);
        assertEquals(entry, entryFromDb);

        entry.setDescription(descriptionNew);
        ListDbHelper.insertUpdateEntry(context, entry, writableDb, true);
        final Cursor cursor2 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor2);
        assertEquals(cursor2.getCount(), 1);

        cursor2.close();
        final ListEntry entryFromDbUpdated = ListDbHelper.getEntry(context, entryIdLong);

        // #
        assertNotNull(entryFromDbUpdated);
        assertEquals(entry, entryFromDbUpdated);
    }

    @Test
    public void updateEntryEdited() throws Exception {
        final Context context       = InstrumentationRegistry.getTargetContext();
        final String userId         = "123";
        final ListEntry entry       = getNewListEntry();
        final String descriptionNew = "new description";

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);

        final SQLiteDatabase writableDb = DbHelper.getWritableDb(context);

        // #
        assertNotNull(writableDb);
        assertTrue(writableDb.isOpen());
        assertFalse(writableDb.isReadOnly());

        final int n1 = DbHelper.getEntriesCount(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertEquals(n1, 0);

        ListDbHelper.insertUpdateEntry(context, entry, writableDb, false);
        final Cursor cursor = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor);
        assertEquals(cursor.getCount(), 1);

        final String entryId = DbHelper.findColumnValueByCursorPosition(cursor, BASE_COLUMN_ID, 0);

        // #
        assertNotNull(entryId);

        cursor.close();
        final long entryIdLong = Long.parseLong(entryId);
        final ListEntry entryFromDb = ListDbHelper.getEntry(context, entryIdLong);
        entry.setId(entryIdLong);

        // #
        assertNotNull(entryFromDb);
        assertEquals(entry, entryFromDb);

        entry.setDescription(descriptionNew);
        Thread.sleep(1001);
        boolean updateResult = ListDbHelper.updateEntry(context, entry, LIST_ITEMS_COLUMN_EDITED);

        // #
        assertTrue(updateResult);

        final Cursor cursor2 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor2);
        assertEquals(cursor2.getCount(), 1);

        cursor2.close();
        final ListEntry entryFromDbUpdated = ListDbHelper.getEntry(context, entryIdLong);

        // #
        assertNotNull(entryFromDbUpdated);
        assertNotEquals(entry.getEdited(), entryFromDbUpdated.getEdited());

        final Date newDate = new Date(System.currentTimeMillis());
        entry.setEdited(newDate);
        entryFromDbUpdated.setEdited(newDate);

        // #
        assertEquals(entry, entryFromDbUpdated);
    }

    @Test
    public void updateEntryViewed() throws Exception {
        final Context context       = InstrumentationRegistry.getTargetContext();
        final String userId         = "123";
        final ListEntry entry       = getNewListEntry();
        final String descriptionNew = "new description";

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);

        final SQLiteDatabase writableDb = DbHelper.getWritableDb(context);

        // #
        assertNotNull(writableDb);
        assertTrue(writableDb.isOpen());
        assertFalse(writableDb.isReadOnly());

        final int n1 = DbHelper.getEntriesCount(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertEquals(n1, 0);

        ListDbHelper.insertUpdateEntry(context, entry, writableDb, false);
        final Cursor cursor = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor);
        assertEquals(cursor.getCount(), 1);

        final String entryId = DbHelper.findColumnValueByCursorPosition(cursor, BASE_COLUMN_ID, 0);

        // #
        assertNotNull(entryId);

        cursor.close();
        final long entryIdLong = Long.parseLong(entryId);
        final ListEntry entryFromDb = ListDbHelper.getEntry(context, entryIdLong);
        entry.setId(entryIdLong);

        // #
        assertNotNull(entryFromDb);
        assertEquals(entry, entryFromDb);

        entry.setDescription(descriptionNew);
        Thread.sleep(1001);
        boolean updateResult = ListDbHelper.updateEntry(context, entry, LIST_ITEMS_COLUMN_VIEWED);

        // #
        assertTrue(updateResult);
        final Cursor cursor2 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor2);
        assertEquals(cursor2.getCount(), 1);

        cursor2.close();
        final ListEntry entryFromDbUpdated = ListDbHelper.getEntry(context, entryIdLong);

        // #
        assertNotNull(entryFromDbUpdated);
        assertNotEquals(entry.getViewed(), entryFromDbUpdated.getViewed());

        final Date newDate = new Date(System.currentTimeMillis());
        entry.setViewed(newDate);
        entryFromDbUpdated.setViewed(newDate);

        // #
        assertEquals(entry, entryFromDbUpdated);
    }

    @Test
    public void updateSyncId() throws Exception {
        final Context context   = InstrumentationRegistry.getTargetContext();
        final String userId     = "123";
        final ListEntry entry   = getNewListEntry();
        final Long syncIdNew    = 3L;

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);

        final SQLiteDatabase writableDb = DbHelper.getWritableDb(context);

        // #
        assertNotNull(writableDb);
        assertTrue(writableDb.isOpen());
        assertFalse(writableDb.isReadOnly());

        final int n1 = DbHelper.getEntriesCount(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertEquals(n1, 0);

        ListDbHelper.insertUpdateEntry(context, entry, writableDb, false);
        final Cursor cursor = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor);
        assertEquals(cursor.getCount(), 1);

        final String entryId = DbHelper.findColumnValueByCursorPosition(cursor, BASE_COLUMN_ID, 0);

        // #
        assertNotNull(entryId);

        cursor.close();
        final long entryIdLong = Long.parseLong(entryId);
        final ListEntry entryFromDb = ListDbHelper.getEntry(context, entryIdLong);
        entry.setId(entryIdLong);

        // #
        assertNotNull(entryFromDb);
        assertEquals(entry, entryFromDb);

        boolean updateResult = ListDbHelper.updateSyncId(context, entryId, syncIdNew.toString());

        // #
        assertTrue(updateResult);

        final Cursor cursor2 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor2);
        assertEquals(cursor2.getCount(), 1);

        cursor2.close();
        final ListEntry entryFromDbUpdated = ListDbHelper.getEntry(context, entryIdLong);

        // #
        assertNotNull(entryFromDbUpdated);

        final Long syncIdFromDb = entryFromDbUpdated.getSyncId();

        // #
        assertNotNull(syncIdFromDb);
        assertEquals(syncIdNew, syncIdFromDb);

        entry.setSyncId(syncIdFromDb);

        // #
        assertEquals(entry, entryFromDbUpdated);
    }

    @Test
    public void getNewEntries() throws Exception {
        final Context context   = InstrumentationRegistry.getTargetContext();
        final String userId     = "123";
        final long millis       = System.currentTimeMillis();
        final long droppedMillis = 1000 * (millis / 1000);
        final Date date         = new Date(droppedMillis);

        final Long syncIdNew    = 3L;
        final String title      = "title";
        final String description = "description";
        final Integer color     = 123456;
        final String imageUrl   = "http://jpg.jpg.jpg";
        final Date created      = (Date) date.clone();
        final Date edited       = (Date) date.clone();
        final Date viewed       = (Date) date.clone();

        final ListEntry entry = new ListEntry();
        entry.setTitle(title);
        entry.setDescription(description);
        entry.setColor(color);
        entry.setImageUrl(imageUrl);
        entry.setCreated(created);
        entry.setEdited(edited);
        entry.setViewed(viewed);

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);

        final SQLiteDatabase writableDb = DbHelper.getWritableDb(context);

        // #
        assertNotNull(writableDb);
        assertTrue(writableDb.isOpen());
        assertFalse(writableDb.isReadOnly());

        final int n1 = DbHelper.getEntriesCount(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertEquals(n1, 0);

        ListDbHelper.insertUpdateEntry(context, entry, writableDb, false);
        ListDbHelper.insertUpdateEntry(context, entry, writableDb, false);
        final Cursor cursor = ListDbHelper.getNewEntries(context);

        // #
        assertNotNull(cursor);
        assertEquals(cursor.getCount(), 2);

        final String entryId = DbHelper.findColumnValueByCursorPosition(cursor, BASE_COLUMN_ID, 0);

        // #
        assertNotNull(entryId);

        cursor.close();
        final long entryIdLong = Long.parseLong(entryId);
        final ListEntry entryFromDb = ListDbHelper.getEntry(context, entryIdLong);
        entry.setId(entryIdLong);

        // #
        assertNotNull(entryFromDb);

        final Long entrySyncId = entryFromDb.getSyncId();

        // #
        assertNotNull(entrySyncId);

        entry.setSyncId(entrySyncId);

        // #
        assertEquals(entry, entryFromDb);

        boolean updateResult = ListDbHelper.updateSyncId(context, entryId, syncIdNew.toString());

        // #
        assertTrue(updateResult);

        final Cursor cursor2 = ListDbHelper.getNewEntries(context);

        // #
        assertNotNull(cursor2);
        assertEquals(cursor2.getCount(), 1);

        cursor2.close();
        final ListEntry entryFromDbUpdated = ListDbHelper.getEntry(context, entryIdLong);

        // #
        assertNotNull(entryFromDbUpdated);

        final Long syncIdFromDb = entryFromDbUpdated.getSyncId();

        // #
        assertNotNull(syncIdFromDb);
        assertEquals(syncIdNew, syncIdFromDb);

        entry.setSyncId(syncIdFromDb);

        // #
        assertEquals(entry, entryFromDbUpdated);
    }

    @Test
    public void getEntry() throws Exception {
        final Context context       = InstrumentationRegistry.getTargetContext();
        final String userId         = "123";
        final ListEntry entry       = getNewListEntry();
        final String descriptionNew = "new description";

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);

        final int n1 = DbHelper.getEntriesCount(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertEquals(n1, 0);

        boolean insertResult = ListDbHelper.insertUpdateEntry(context, entry, false);

        // #
        assertTrue(insertResult);

        final Cursor cursor = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor);
        assertEquals(cursor.getCount(), 1);

        final String entryId = DbHelper.findColumnValueByCursorPosition(cursor, BASE_COLUMN_ID, 0);

        // #
        assertNotNull(entryId);

        cursor.close();
        final long entryIdLong = Long.parseLong(entryId);
        final ListEntry entryFromDb = ListDbHelper.getEntry(context, entryIdLong);
        entry.setId(entryIdLong);

        // #
        assertNotNull(entryFromDb);
        assertEquals(entry, entryFromDb);

        entry.setDescription(descriptionNew);
        boolean updateResult = ListDbHelper.insertUpdateEntry(context, entry, true);

        // #
        assertTrue(updateResult);

        final Cursor cursor2 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor2);
        assertEquals(cursor2.getCount(), 1);

        cursor2.close();
        final ListEntry entryFromDbUpdated = ListDbHelper.getEntry(context, entryIdLong);

        // #
        assertNotNull(entryFromDbUpdated);
        assertEquals(entry, entryFromDbUpdated);
    }

    @Test
    public void deleteEntry() throws Exception {
        final Context context   = InstrumentationRegistry.getTargetContext();
        final String userId     = "123";
        final ListEntry entry   = getNewListEntry();

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);

        final SQLiteDatabase writableDb = DbHelper.getWritableDb(context);

        // #
        assertNotNull(writableDb);
        assertTrue(writableDb.isOpen());
        assertFalse(writableDb.isReadOnly());

        final int n1 = DbHelper.getEntriesCount(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertEquals(n1, 0);

        ListDbHelper.insertUpdateEntry(context, entry, writableDb, false);
        ListDbHelper.insertUpdateEntry(context, entry, writableDb, false);
        final Cursor cursor = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor);
        assertEquals(cursor.getCount(), 2);

        final String entryId = DbHelper.findColumnValueByCursorPosition(cursor, BASE_COLUMN_ID, 0);

        // #
        assertNotNull(entryId);

        cursor.close();
        final long entryIdLong = Long.parseLong(entryId);
        final ListEntry entryFromDb = ListDbHelper.getEntry(context, entryIdLong);
        entry.setId(entryIdLong);

        // #
        assertNotNull(entryFromDb);
        assertEquals(entry, entryFromDb);

        boolean deleteResult = ListDbHelper.deleteEntry(context, entryIdLong, false);

        // #
        assertTrue(deleteResult);

        final Cursor cursor2 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor2);
        assertEquals(cursor2.getCount(), 1);

        cursor2.close();
    }

    @Test
    public void deleteEntryWithAddToDeleteTable() throws Exception {
        final Context context   = InstrumentationRegistry.getTargetContext();
        final String userId     = "123";
        final ListEntry entry   = getNewListEntry();

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);

        final SQLiteDatabase writableDb = DbHelper.getWritableDb(context);

        // #
        assertNotNull(writableDb);
        assertTrue(writableDb.isOpen());
        assertFalse(writableDb.isReadOnly());

        final int n1 = DbHelper.getEntriesCount(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertEquals(n1, 0);

        ListDbHelper.insertUpdateEntry(context, entry, writableDb, false);
        ListDbHelper.insertUpdateEntry(context, entry, writableDb, false);
        final Cursor cursor = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor);
        assertEquals(cursor.getCount(), 2);

        final String entryId = DbHelper.findColumnValueByCursorPosition(cursor, BASE_COLUMN_ID, 0);

        // #
        assertNotNull(entryId);

        cursor.close();
        final long entryIdLong = Long.parseLong(entryId);
        final ListEntry entryFromDb = ListDbHelper.getEntry(context, entryIdLong);
        entry.setId(entryIdLong);

        // #
        assertNotNull(entryFromDb);
        assertEquals(entry, entryFromDb);

        final int deletedCount = DbHelper.getEntriesCount(context, SYNC_DELETED_TABLE_NAME, null);

        // #
        assertEquals(deletedCount, 0);

        boolean deleteResult = ListDbHelper.deleteEntry(context, entryIdLong, true);

        // #
        assertTrue(deleteResult);

        final Cursor cursor2 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor2);
        assertEquals(cursor2.getCount(), 1);

        cursor2.close();
        final int deletedCount2 = DbHelper.getEntriesCount(context, SYNC_DELETED_TABLE_NAME, null);

        // #
        assertEquals(deletedCount2, 1);
    }

    @Test
    public void addMockEntries() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "123";
        final int number = 5;

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);

        final int n1 = DbHelper.getEntriesCount(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertEquals(n1, 0);

        final int n2 = ListDbHelper.addMockEntries(context, null, number);

        // #
        assertEquals(n2, number);

        final int n3 = DbHelper.getEntriesCount(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertEquals(n3, number);
    }

    @Test
    public void getCursorWithParams() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "123";
        final String color = "123456";
        final String text = "good";

        final Map<String, String> filterProfile = new HashMap<>();
        filterProfile.put(SP_FILTER_COLOR, color);
        filterProfile.put(SP_FILTER_ORDER, LIST_ITEMS_COLUMN_TITLE);
        filterProfile.put(SP_FILTER_ORDER_ASC, ORDER_ASC_DESC_DEFAULT);

        final ListEntry entry1 = new ListEntry();
        entry1.setTitle("title1");
        entry1.setDescription("description1");

        final ListEntry entry2 = new ListEntry();
        entry2.setTitle("title2");
        entry2.setDescription("description2");

        final ListEntry entry3 = new ListEntry();
        entry3.setTitle("title3");
        entry3.setDescription("description3");
        entry3.setColor(Integer.parseInt(color));

        final ListEntry entry4 = new ListEntry();
        entry4.setTitle("title" + text + "4");
        entry4.setDescription("description4");
        entry4.setColor(Integer.parseInt(color));

        final ListEntry entry5 = new ListEntry();
        entry5.setTitle("title5");
        entry5.setDescription("description" + text + "5");
        entry5.setColor(Integer.parseInt(color));

        final ListEntry entry6 = new ListEntry();
        entry6.setTitle("title123" + text + "4567");
        entry6.setDescription("description123" + text + "4567");

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);

        final int n1 = DbHelper.getEntriesCount(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertEquals(n1, 0);

        ListDbHelper.insertUpdateEntry(context, entry1, false);
        ListDbHelper.insertUpdateEntry(context, entry2, false);
        ListDbHelper.insertUpdateEntry(context, entry3, false);
        ListDbHelper.insertUpdateEntry(context, entry4, false);
        ListDbHelper.insertUpdateEntry(context, entry5, false);
        ListDbHelper.insertUpdateEntry(context, entry6, false);
        final Cursor cursor = ListDbHelper.getCursorWithParams(context, text, filterProfile);

        // #
        assertNotNull(cursor);
        assertEquals(cursor.getCount(), 2);
    }

    @Test
    public void convertToQueryBuilder() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String color = "123456";
        final String text = "good";

        final Map<String, String> filterProfile = new HashMap<>();

        filterProfile.put(SP_FILTER_COLOR,      color);
        filterProfile.put(SP_FILTER_CREATED,    "2017-05-04 00:00:00#2017-05-10 00:00:00");
        filterProfile.put(SP_FILTER_EDITED,     "2017-05-04 00:00:00#2017-05-10 00:00:00");
        filterProfile.put(SP_FILTER_VIEWED,     "2017-05-04 00:00:00#2017-05-10 00:00:00");
        filterProfile.put(SP_FILTER_ORDER,      LIST_ITEMS_COLUMN_TITLE);
        filterProfile.put(SP_FILTER_ORDER_ASC,  ORDER_ASC_DESC_DEFAULT);

        final String[] selectionArgsExpected = {
                "2017-05-04",
                "2017-05-11 00:00:00",
                "2017-05-04",
                "2017-05-11 00:00:00",
                "2017-05-04",
                "2017-05-11 00:00:00",
                color,
                "%" + text + "%",
                "%" + text + "%"};
        Arrays.sort(selectionArgsExpected);
        final String sortOrderExpected = LIST_ITEMS_COLUMN_TITLE + " " + ORDER_ASC_DESC_DEFAULT;

        final DbQueryBuilder builder =
                ListDbHelper.convertToQueryBuilder(context, text, filterProfile);
        final String[] selectionArgsActual = builder.getSelectionArgs();
        final String sortOrderActual = builder.getSortOrder();

        // #
        assertNotNull(selectionArgsActual);

        Arrays.sort(selectionArgsActual);

        // #
        assertArrayEquals(selectionArgsExpected, selectionArgsActual);
        assertEquals(sortOrderExpected, sortOrderActual);
    }

    @Test
    public void swapManuallyColumnValue() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "123";

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);

        final ListEntry entry1 = getNewListEntry();
        final ListEntry entry2 = getNewListEntry();

        final int n1 = DbHelper.getEntriesCount(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertEquals(n1, 0);

        ListDbHelper.insertUpdateEntry(context, entry1, false);
        ListDbHelper.insertUpdateEntry(context, entry2, false);

        final Cursor cursor = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor);
        assertEquals(cursor.getCount(), 2);

        cursor.moveToNext();
        final String entryIdOne1 = cursor.getString(cursor.getColumnIndex(BASE_COLUMN_ID));
        final String manualIdOne1 =
                cursor.getString(cursor.getColumnIndex(LIST_ITEMS_COLUMN_MANUALLY));
        cursor.moveToNext();
        final String entryIdTwo1 = cursor.getString(cursor.getColumnIndex(BASE_COLUMN_ID));
        final String manualIdTwo1 =
                cursor.getString(cursor.getColumnIndex(LIST_ITEMS_COLUMN_MANUALLY));
        cursor.close();

        boolean result = ListDbHelper.swapManuallyColumnValue(context, entryIdOne1, entryIdTwo1);

        // #
        assertTrue(result);

        final Cursor cursor2 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor2);
        assertEquals(cursor2.getCount(), 2);

        cursor2.moveToNext();
        final String entryIdOne2 = cursor2.getString(cursor2.getColumnIndex(BASE_COLUMN_ID));
        final String manualIdOne2 = cursor2.getString(cursor2.getColumnIndex(LIST_ITEMS_COLUMN_MANUALLY));
        cursor2.moveToNext();
        final String entryIdTwo2 = cursor2.getString(cursor2.getColumnIndex(BASE_COLUMN_ID));
        final String manualIdTwo2 = cursor2.getString(cursor2.getColumnIndex(LIST_ITEMS_COLUMN_MANUALLY));
        cursor2.close();

        if (entryIdOne1.equals(entryIdOne2)) {
            assertEquals(manualIdOne1, manualIdTwo2);
            assertEquals(manualIdTwo1, manualIdOne2);
        } else if (entryIdOne1.equals(entryIdTwo2)) {
            assertEquals(manualIdOne1, manualIdOne2);
            assertEquals(manualIdTwo1, manualIdTwo2);
        } else {
            throw new Exception("Trouble.");
        }
    }


    /*
        Utils
     */

    @NonNull
    private static ListEntry getNewListEntry() {
        final long millis       = System.currentTimeMillis();
        final long droppedMillis = 1000 * (millis / 1000);
        final Date date         = new Date(droppedMillis);

        final Long syncId       = 2L;
        final String title      = "title";
        final String description = "description";
        final Integer color     = 123456;
        final String imageUrl   = "http://jpg.jpg.jpg";
        final Date created      = (Date) date.clone();
        final Date edited       = (Date) date.clone();
        final Date viewed       = (Date) date.clone();

        final ListEntry entry = new ListEntry();
        entry.setSyncId(syncId);
        entry.setTitle(title);
        entry.setDescription(description);
        entry.setColor(color);
        entry.setImageUrl(imageUrl);
        entry.setCreated(created);
        entry.setEdited(edited);
        entry.setViewed(viewed);

        return entry;
    }
}