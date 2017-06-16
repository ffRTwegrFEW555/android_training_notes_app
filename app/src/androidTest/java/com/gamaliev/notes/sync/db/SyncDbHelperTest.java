package com.gamaliev.notes.sync.db;

import android.content.Context;
import android.database.Cursor;
import android.support.test.InstrumentationRegistry;

import com.gamaliev.notes.common.shared_prefs.SpUsers;
import com.gamaliev.notes.entity.SyncEntry;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static com.gamaliev.notes.TestUtils.initDefaultPrefs;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_COLUMN_ACTION;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_COLUMN_AMOUNT;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_COLUMN_FINISHED;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_COLUMN_STATUS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
public class SyncDbHelperTest {

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
    public void insertEntry() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final int action1   = 1;
        final int amount1   = 2;
        final int status1   = 3;
        final Date date1    = new Date(System.currentTimeMillis());
        final int action2   = 4;
        final int amount2   = 5;
        final int status2   = 6;
        final Date date2    = new Date(System.currentTimeMillis());
        final int dateLengthExpected = 19;

        final String selectedUserId = SpUsers.getSelected(context);

        // #
        assertNotNull(selectedUserId);

        final Cursor cursor1 = SyncDbHelper.getAll(context);

        // #
        assertNotNull(cursor1);
        assertEquals(cursor1.getCount(), 0);

        cursor1.close();

        final SyncEntry syncEntry1 = new SyncEntry();
        syncEntry1.setAction(action1);
        syncEntry1.setAmount(amount1);
        syncEntry1.setStatus(status1);
        syncEntry1.setFinished(date1);

        final SyncEntry syncEntry2 = new SyncEntry();
        syncEntry2.setAction(action2);
        syncEntry2.setAmount(amount2);
        syncEntry2.setStatus(status2);
        syncEntry2.setFinished(date2);

        boolean insertResult1 = SyncDbHelper.insertEntry(context, syncEntry1);
        boolean insertResult2 = SyncDbHelper.insertEntry(context, syncEntry1);

        // #
        assertTrue(insertResult1);
        assertTrue(insertResult2);

        final Cursor cursor2 = SyncDbHelper.getAll(context);

        // #
        assertNotNull(cursor2);
        assertEquals(cursor2.getCount(), 2);

        cursor2.moveToNext();
        final int actionFromCursor1 = cursor2.getInt(cursor2.getColumnIndex(SYNC_COLUMN_ACTION));
        final int amountFromCursor1 = cursor2.getInt(cursor2.getColumnIndex(SYNC_COLUMN_AMOUNT));
        final int statusFromCursor1 = cursor2.getInt(cursor2.getColumnIndex(SYNC_COLUMN_STATUS));
        final String finishedFromCursor1 = cursor2.getString(cursor2.getColumnIndex(SYNC_COLUMN_FINISHED));

        // #
        if (action1 == actionFromCursor1) {
            assertEquals(amount1, amountFromCursor1);
            assertEquals(status1, statusFromCursor1);

        } else if (action2 == actionFromCursor1) {
            assertEquals(amount2, amountFromCursor1);
            assertEquals(status2, statusFromCursor1);

        } else {
            throw new Exception("Action not equals.");
        }
        assertTrue(finishedFromCursor1.length() == dateLengthExpected);

        cursor2.moveToNext();
        final int actionFromCursor2 = cursor2.getInt(cursor2.getColumnIndex(SYNC_COLUMN_ACTION));
        final int amountFromCursor2 = cursor2.getInt(cursor2.getColumnIndex(SYNC_COLUMN_AMOUNT));
        final int statusFromCursor2 = cursor2.getInt(cursor2.getColumnIndex(SYNC_COLUMN_STATUS));
        final String finishedFromCursor2 = cursor2.getString(cursor2.getColumnIndex(SYNC_COLUMN_FINISHED));
        cursor2.close();

        // #
        if (action1 == actionFromCursor2) {
            assertEquals(amount1, amountFromCursor2);
            assertEquals(status1, statusFromCursor2);

        } else if (action2 == actionFromCursor2) {
            assertEquals(amount2, amountFromCursor2);
            assertEquals(status2, statusFromCursor2);

        } else {
            throw new Exception("Action not equals.");
        }
        assertTrue(finishedFromCursor2.length() == dateLengthExpected);
    }

    @Test
    public void getAll() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final int action1   = 1;
        final int amount1   = 2;
        final int status1   = 3;
        final Date date1    = new Date(System.currentTimeMillis());
        final int action2   = 4;
        final int amount2   = 5;
        final int status2   = 6;
        final Date date2    = new Date(System.currentTimeMillis());
        final int dateLengthExpected = 19;

        final String selectedUserId = SpUsers.getSelected(context);

        // #
        assertNotNull(selectedUserId);

        final Cursor cursor1 = SyncDbHelper.getAll(context);

        // #
        assertNotNull(cursor1);
        assertEquals(cursor1.getCount(), 0);

        cursor1.close();

        final SyncEntry syncEntry1 = new SyncEntry();
        syncEntry1.setAction(action1);
        syncEntry1.setAmount(amount1);
        syncEntry1.setStatus(status1);
        syncEntry1.setFinished(date1);

        final SyncEntry syncEntry2 = new SyncEntry();
        syncEntry2.setAction(action2);
        syncEntry2.setAmount(amount2);
        syncEntry2.setStatus(status2);
        syncEntry2.setFinished(date2);

        boolean insertResult1 = SyncDbHelper.insertEntry(context, syncEntry1);
        boolean insertResult2 = SyncDbHelper.insertEntry(context, syncEntry1);

        // #
        assertTrue(insertResult1);
        assertTrue(insertResult2);

        final Cursor cursor2 = SyncDbHelper.getAll(context);

        // #
        assertNotNull(cursor2);
        assertEquals(cursor2.getCount(), 2);

        cursor2.moveToNext();
        final int actionFromCursor1 = cursor2.getInt(cursor2.getColumnIndex(SYNC_COLUMN_ACTION));
        final int amountFromCursor1 = cursor2.getInt(cursor2.getColumnIndex(SYNC_COLUMN_AMOUNT));
        final int statusFromCursor1 = cursor2.getInt(cursor2.getColumnIndex(SYNC_COLUMN_STATUS));
        final String finishedFromCursor1 = cursor2.getString(cursor2.getColumnIndex(SYNC_COLUMN_FINISHED));

        // #
        if (action1 == actionFromCursor1) {
            assertEquals(amount1, amountFromCursor1);
            assertEquals(status1, statusFromCursor1);

        } else if (action2 == actionFromCursor1) {
            assertEquals(amount2, amountFromCursor1);
            assertEquals(status2, statusFromCursor1);

        } else {
            throw new Exception("Action not equals.");
        }
        assertTrue(finishedFromCursor1.length() == dateLengthExpected);

        cursor2.moveToNext();
        final int actionFromCursor2 = cursor2.getInt(cursor2.getColumnIndex(SYNC_COLUMN_ACTION));
        final int amountFromCursor2 = cursor2.getInt(cursor2.getColumnIndex(SYNC_COLUMN_AMOUNT));
        final int statusFromCursor2 = cursor2.getInt(cursor2.getColumnIndex(SYNC_COLUMN_STATUS));
        final String finishedFromCursor2 = cursor2.getString(cursor2.getColumnIndex(SYNC_COLUMN_FINISHED));
        cursor2.close();

        // #
        if (action1 == actionFromCursor2) {
            assertEquals(amount1, amountFromCursor2);
            assertEquals(status1, statusFromCursor2);

        } else if (action2 == actionFromCursor2) {
            assertEquals(amount2, amountFromCursor2);
            assertEquals(status2, statusFromCursor2);

        } else {
            throw new Exception("Action not equals.");
        }
        assertTrue(finishedFromCursor2.length() == dateLengthExpected);
    }

    @Test
    public void clear() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final int action1   = 1;
        final int amount1   = 2;
        final int status1   = 3;
        final Date date1    = new Date(System.currentTimeMillis());
        final int action2   = 4;
        final int amount2   = 5;
        final int status2   = 6;
        final Date date2    = new Date(System.currentTimeMillis());

        final String selectedUserId = SpUsers.getSelected(context);

        // #
        assertNotNull(selectedUserId);

        final Cursor cursor1 = SyncDbHelper.getAll(context);

        // #
        assertNotNull(cursor1);
        assertEquals(cursor1.getCount(), 0);

        cursor1.close();

        final SyncEntry syncEntry1 = new SyncEntry();
        syncEntry1.setAction(action1);
        syncEntry1.setAmount(amount1);
        syncEntry1.setStatus(status1);
        syncEntry1.setFinished(date1);

        final SyncEntry syncEntry2 = new SyncEntry();
        syncEntry2.setAction(action2);
        syncEntry2.setAmount(amount2);
        syncEntry2.setStatus(status2);
        syncEntry2.setFinished(date2);

        boolean insertResult1 = SyncDbHelper.insertEntry(context, syncEntry1);
        boolean insertResult2 = SyncDbHelper.insertEntry(context, syncEntry1);

        // #
        assertTrue(insertResult1);
        assertTrue(insertResult2);

        final Cursor cursor2 = SyncDbHelper.getAll(context);

        // #
        assertNotNull(cursor2);
        assertEquals(cursor2.getCount(), 2);

        cursor2.close();
        boolean clearResult = SyncDbHelper.clear(context);

        // #
        assertTrue(clearResult);

        final Cursor cursor3 = SyncDbHelper.getAll(context);

        // #
        assertNotNull(cursor3);
        assertEquals(cursor3.getCount(), 0);
    }
}