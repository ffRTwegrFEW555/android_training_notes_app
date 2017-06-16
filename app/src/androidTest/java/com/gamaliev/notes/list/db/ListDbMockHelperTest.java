package com.gamaliev.notes.list.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;

import com.gamaliev.notes.common.db.DbHelper;
import com.gamaliev.notes.common.shared_prefs.SpUsers;

import org.junit.Before;
import org.junit.Test;

import static com.gamaliev.notes.TestUtils.clearUserPrefs;
import static com.gamaliev.notes.TestUtils.initDefaultPrefs;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_TABLE_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
public class ListDbMockHelperTest {

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
    public void addMockEntries() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "123";
        final int number = 5;

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);

        final int n1 = DbHelper.getEntriesCount(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertEquals(n1, 0);

        final SQLiteDatabase writableDb = DbHelper.getWritableDb(context);

        // #
        assertNotNull(writableDb);
        assertTrue(writableDb.isOpen());
        assertFalse(writableDb.isReadOnly());

        writableDb.beginTransaction();
        try {
            ListDbMockHelper.addMockEntries(context, number, writableDb, null, true);
            writableDb.setTransactionSuccessful();
        } finally {
            writableDb.endTransaction();
        }

        final int n3 = DbHelper.getEntriesCount(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertEquals(n3, number);
    }

}