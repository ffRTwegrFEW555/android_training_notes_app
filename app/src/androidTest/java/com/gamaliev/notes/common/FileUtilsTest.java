package com.gamaliev.notes.common;

import android.content.Context;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;

import com.gamaliev.notes.common.db.DbHelper;
import com.gamaliev.notes.common.shared_prefs.SpUsers;
import com.gamaliev.notes.list.db.ListDbHelper;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static com.gamaliev.notes.UtilsTest.clearUserPrefs;
import static com.gamaliev.notes.UtilsTest.initDefaultPrefs;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_TABLE_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
public class FileUtilsTest {

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
    public void exportImportEntriesAsync() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "123";
        final int number = 5;

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);

        final File outputDir = context.getCacheDir();
        final File outputFile = File.createTempFile("prefix", "extension", outputDir);

        final int n1 = DbHelper.getEntriesCount(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertEquals(n1, 0);

        final int n2 = ListDbHelper.addMockEntries(context, null, number);

        // #
        assertEquals(n2, number);

        final int n3 = DbHelper.getEntriesCount(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertEquals(n3, number);

        final Uri uri = Uri.fromFile(outputFile);
        FileUtils.exportEntriesAsync(context, uri);
        FileUtils.importEntriesAsync(context, uri);
        Thread.sleep(1000);

        final int n4 = DbHelper.getEntriesCount(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertEquals(n4, number * 2);
    }

    @Test
    public void getImportExportHandlerLooperThread() throws Exception {
        final CommonUtils.LooperHandlerThread looper =
                FileUtils.getImportExportHandlerLooperThread();

        assertFalse(looper.isInterrupted());
        assertTrue(looper.isAlive());
    }
}