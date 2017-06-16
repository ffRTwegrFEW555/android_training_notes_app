package com.gamaliev.notes.sync.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;

import com.gamaliev.notes.common.db.DbHelper;
import com.gamaliev.notes.common.rest.NoteApi;
import com.gamaliev.notes.common.rest.NoteApiUtils;
import com.gamaliev.notes.common.rest.NotesHttpServerTest;
import com.gamaliev.notes.common.shared_prefs.SpUsers;
import com.gamaliev.notes.list.db.ListDbHelper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutorService;

import retrofit2.Response;

import static android.content.Context.MODE_PRIVATE;
import static com.gamaliev.notes.UtilsTest.clearUserPrefs;
import static com.gamaliev.notes.UtilsTest.initDefaultPrefs;
import static com.gamaliev.notes.common.db.DbHelper.BASE_COLUMN_ID;
import static com.gamaliev.notes.common.db.DbHelper.COMMON_COLUMN_SYNC_ID;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_TABLE_NAME;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_COLUMN_ACTION;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_COLUMN_AMOUNT;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_COLUMN_FINISHED;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_COLUMN_STATUS;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_TABLE_NAME;
import static com.gamaliev.notes.common.rest.NoteApiUtils.API_KEY_DATA;
import static com.gamaliev.notes.common.rest.NoteApiUtils.LOCALHOST;
import static com.gamaliev.notes.common.rest.NoteApiUtils.TEST_PORT;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.SP_USER_SYNC_API_URL;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.SP_USER_SYNC_ID;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.SP_USER_SYNC_PENDING_TRUE;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.SP_USER_SYNC_WIFI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
@SuppressWarnings("NullableProblems")
public class SyncUtilsTest {

    /* ... */
    @NonNull private static NoteApi sNoteApi;
    @NonNull private NotesHttpServerTest mNotesHttpServerTest;


    /*
        Init
     */

    @BeforeClass
    public static void beforeClass() throws Exception {
        final NoteApi noteApi = NoteApiUtils.newInstance(null);
        if (noteApi == null) {
            throw new IllegalArgumentException("NoteApi is null.");
        }
        sNoteApi = noteApi;
    }

    @Before
    public void before() throws Exception {
        mNotesHttpServerTest = NotesHttpServerTest.newInstance(Integer.parseInt(TEST_PORT));
        mNotesHttpServerTest.startServer();

        initDefaultPrefs();
    }

    @After
    public void after() throws Exception {
        mNotesHttpServerTest.stopServer();
    }


    /*
        Tests
     */

    @Test
    public void isSyncRunning() throws Exception {
        SyncUtils.setSyncRunning(true);
        assertTrue(SyncUtils.isSyncRunning());
        SyncUtils.setSyncRunning(false);
        assertFalse(SyncUtils.isSyncRunning());
    }

    @Test
    public void setSyncRunning() throws Exception {
        SyncUtils.setSyncRunning(true);
        assertTrue(SyncUtils.isSyncRunning());
        SyncUtils.setSyncRunning(false);
        assertFalse(SyncUtils.isSyncRunning());
    }

    @Test
    public void synchronize() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "123";
        final int number = 5;

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);
        SyncUtils.setSyncRunning(false);
        setSyncWifiOnly(context, userId, false);
        setApiUrl(context, userId, LOCALHOST + ':' + TEST_PORT);
        setSyncId(context, userId, userId);


        /*
            Add to local mock entries.
         */

        final int n1 = DbHelper.getEntriesCount(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertEquals(n1, 0);

        final Response<String> response = sNoteApi.getAll(userId).execute();
        final JSONObject body = new JSONObject(response.body());
        final JSONArray data = body.getJSONArray(API_KEY_DATA);

        // #
        assertNotNull(data);
        assertTrue(data.length() == 0);

        ListDbHelper.addMockEntries(context, null, number);
        SyncUtils.synchronize(context);

        Thread.sleep(1000);

        final Response<String> response2 = sNoteApi.getAll(userId).execute();
        final JSONObject body2 = new JSONObject(response2.body());
        final JSONArray data2 = body2.getJSONArray(API_KEY_DATA);

        // #
        assertNotNull(data2);
        assertTrue(data2.length() == number);


        /*
            +1 to local.
         */

        ListDbHelper.addMockEntries(context, null, 1);
        SyncUtils.synchronize(context);

        Thread.sleep(1000);

        final Response<String> response3 = sNoteApi.getAll(userId).execute();
        final JSONObject body3 = new JSONObject(response3.body());
        final JSONArray data3 = body3.getJSONArray(API_KEY_DATA);

        // #
        assertNotNull(data3);
        assertTrue(data3.length() == number + 1);


        /*
            -1 from local.
         */

        final Cursor cursor1 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor1);
        assertEquals(cursor1.getCount(), number + 1);

        cursor1.moveToNext();
        final String entryId = cursor1.getString(cursor1.getColumnIndex(BASE_COLUMN_ID));
        cursor1.moveToNext();
        final String syncId = cursor1.getString(cursor1.getColumnIndex(COMMON_COLUMN_SYNC_ID));
        cursor1.close();

        ListDbHelper.deleteEntry(context, Long.valueOf(entryId), true);
        SyncUtils.synchronize(context);

        Thread.sleep(1000);

        final Response<String> response4 = sNoteApi.getAll(userId).execute();
        final JSONObject body4 = new JSONObject(response4.body());
        final JSONArray data4 = body4.getJSONArray(API_KEY_DATA);

        // #
        assertNotNull(data4);
        assertTrue(data4.length() == number);


        /*
            -1 from server.
         */

        sNoteApi.delete(userId, syncId).execute();
        SyncUtils.synchronize(context);

        Thread.sleep(1000);

        final Cursor cursor2 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor2);
        assertEquals(cursor2.getCount(), number - 1);

        cursor2.close();


        /*
            +1 to server.
         */

        final String note = "{\"title\": \"Yelena Yeremeyeva\", "
                + "\"color\": \"#0010FF\", "
                + "\"imageUrl\": \"https:\\/\\/developer.android.com\\/images\\/brand\\/Android_Robot_100.png\", "
                + "\"description\": \"butterfly galaxy Peace Smile bubble cosmopolitan cosy Love \", "
                + "\"created\": \"2017-05-05T02:25:35+05:00\", "
                + "\"edited\": \"2017-05-05T02:25:35+05:00\", "
                + "\"viewed\": \"2017-05-08T02:25:35+05:00\"}";

        final Response<String> response5 = sNoteApi.add(userId, note).execute();
        final JSONObject body5 = new JSONObject(response5.body());
        final String newSyncId = body5.optString(API_KEY_DATA);
        SyncUtils.synchronize(context);

        Thread.sleep(1000);

        final Cursor cursor3 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor3);
        assertEquals(cursor3.getCount(), number);

        final int pos = DbHelper.findCursorPositionByColumnValue(
                cursor3,
                COMMON_COLUMN_SYNC_ID,
                newSyncId);
        cursor3.close();

        // #
        assertTrue(pos > -1);
    }

    @Test
    public void makeSynchronize() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "123";
        final int number = 5;

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);
        SyncUtils.setSyncRunning(false);
        setSyncWifiOnly(context, userId, false);
        setApiUrl(context, userId, LOCALHOST + ':' + TEST_PORT);
        setSyncId(context, userId, userId);


        /*
            Add to local mock entries.
         */

        final int n1 = DbHelper.getEntriesCount(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertEquals(n1, 0);

        final Response<String> response = sNoteApi.getAll(userId).execute();
        final JSONObject body = new JSONObject(response.body());
        final JSONArray data = body.getJSONArray(API_KEY_DATA);

        // #
        assertNotNull(data);
        assertTrue(data.length() == 0);

        ListDbHelper.addMockEntries(context, null, number);
        SyncUtils.makeSynchronize(context);

        final Response<String> response2 = sNoteApi.getAll(userId).execute();
        final JSONObject body2 = new JSONObject(response2.body());
        final JSONArray data2 = body2.getJSONArray(API_KEY_DATA);

        // #
        assertNotNull(data2);
        assertTrue(data2.length() == number);


        /*
            +1 to local.
         */

        ListDbHelper.addMockEntries(context, null, 1);
        SyncUtils.makeSynchronize(context);

        final Response<String> response3 = sNoteApi.getAll(userId).execute();
        final JSONObject body3 = new JSONObject(response3.body());
        final JSONArray data3 = body3.getJSONArray(API_KEY_DATA);

        // #
        assertNotNull(data3);
        assertTrue(data3.length() == number + 1);


        /*
            -1 from local.
         */

        final Cursor cursor1 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor1);
        assertEquals(cursor1.getCount(), number + 1);

        cursor1.moveToNext();
        final String entryId = cursor1.getString(cursor1.getColumnIndex(BASE_COLUMN_ID));
        cursor1.moveToNext();
        final String syncId = cursor1.getString(cursor1.getColumnIndex(COMMON_COLUMN_SYNC_ID));
        cursor1.close();

        ListDbHelper.deleteEntry(context, Long.valueOf(entryId), true);
        SyncUtils.makeSynchronize(context);

        final Response<String> response4 = sNoteApi.getAll(userId).execute();
        final JSONObject body4 = new JSONObject(response4.body());
        final JSONArray data4 = body4.getJSONArray(API_KEY_DATA);

        // #
        assertNotNull(data4);
        assertTrue(data4.length() == number);


        /*
            -1 from server.
         */

        sNoteApi.delete(userId, syncId).execute();
        SyncUtils.makeSynchronize(context);

        final Cursor cursor2 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor2);
        assertEquals(cursor2.getCount(), number - 1);

        cursor2.close();


        /*
            +1 to server.
         */

        final String note = "{\"title\": \"Yelena Yeremeyeva\", "
                + "\"color\": \"#0010FF\", "
                + "\"imageUrl\": \"https:\\/\\/developer.android.com\\/images\\/brand\\/Android_Robot_100.png\", "
                + "\"description\": \"butterfly galaxy Peace Smile bubble cosmopolitan cosy Love \", "
                + "\"created\": \"2017-05-05T02:25:35+05:00\", "
                + "\"edited\": \"2017-05-05T02:25:35+05:00\", "
                + "\"viewed\": \"2017-05-08T02:25:35+05:00\"}";

        final Response<String> response5 = sNoteApi.add(userId, note).execute();
        final JSONObject body5 = new JSONObject(response5.body());
        final String newSyncId = body5.optString(API_KEY_DATA);
        SyncUtils.makeSynchronize(context);

        final Cursor cursor3 = DbHelper.getEntries(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertNotNull(cursor3);
        assertEquals(cursor3.getCount(), number);

        final int pos = DbHelper.findCursorPositionByColumnValue(
                cursor3,
                COMMON_COLUMN_SYNC_ID,
                newSyncId);
        cursor3.close();

        // #
        assertTrue(pos > -1);
    }

    @Test
    public void deleteAllFromServerAsync() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "123";
        final int number = 5;

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);
        SyncUtils.setSyncRunning(false);
        setSyncWifiOnly(context, userId, false);
        setApiUrl(context, userId, LOCALHOST + ':' + TEST_PORT);
        setSyncId(context, userId, userId);


        /*
            Add to local mock entries.
         */

        final int n1 = DbHelper.getEntriesCount(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertEquals(n1, 0);

        final Response<String> response = sNoteApi.getAll(userId).execute();
        final JSONObject body = new JSONObject(response.body());
        final JSONArray data = body.getJSONArray(API_KEY_DATA);

        // #
        assertNotNull(data);
        assertTrue(data.length() == 0);

        ListDbHelper.addMockEntries(context, null, number);
        SyncUtils.makeSynchronize(context);

        final Response<String> response2 = sNoteApi.getAll(userId).execute();
        final JSONObject body2 = new JSONObject(response2.body());
        final JSONArray data2 = body2.getJSONArray(API_KEY_DATA);

        // #
        assertNotNull(data2);
        assertTrue(data2.length() == number);

        final int n2 = DbHelper.getEntriesCount(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertEquals(n2, number);


        /*
            Delete all from server.
         */

        SyncUtils.deleteAllFromServerAsync(context);

        Thread.sleep(1000);

        final Response<String> response3 = sNoteApi.getAll(userId).execute();
        final JSONObject body3 = new JSONObject(response3.body());
        final JSONArray data3 = body3.getJSONArray(API_KEY_DATA);

        // #
        assertNotNull(data3);
        assertTrue(data3.length() == 0);

        SyncUtils.makeSynchronize(context);

        final Response<String> response4 = sNoteApi.getAll(userId).execute();
        final JSONObject body4 = new JSONObject(response4.body());
        final JSONArray data4 = body4.getJSONArray(API_KEY_DATA);

        // #
        assertNotNull(data4);
        assertTrue(data4.length() == 0);

        final int n3 = DbHelper.getEntriesCount(context, LIST_ITEMS_TABLE_NAME, null);

        // #
        assertEquals(n3, 0);
    }

    @Test
    public void addToSyncJournalAndLogAndNotify() throws Exception {
        final Context context   = InstrumentationRegistry.getTargetContext();
        final String userId     = "123";
        final int action        = 1;
        final int status        = 2;
        final int amount        = 3;
        final int resultCode    = 4;
        final int dateCharsCount = 19;

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);

        SyncUtils.addToSyncJournalAndLogAndNotify(context, action, status, amount, resultCode, true);
        final Cursor cursor = DbHelper.getEntries(context, SYNC_TABLE_NAME, null);

        // #
        assertNotNull(cursor);
        assertEquals(cursor.getCount(), 1);

        cursor.moveToNext();
        final int actionActual = cursor.getInt(cursor.getColumnIndex(SYNC_COLUMN_ACTION));
        final int statusActual = cursor.getInt(cursor.getColumnIndex(SYNC_COLUMN_STATUS));
        final int amountActual = cursor.getInt(cursor.getColumnIndex(SYNC_COLUMN_AMOUNT));
        final String date = cursor.getString(cursor.getColumnIndex(SYNC_COLUMN_FINISHED));
        cursor.close();

        // $
        assertEquals(action, actionActual);
        assertEquals(status, statusActual);
        assertEquals(amount, amountActual);
        assertEquals(dateCharsCount, date.length());
    }

    @Test
    public void logAndNotify() throws Exception {
        SyncUtils.logAndNotify("test", true, -1);
    }

    @Test
    public void checkPendingSyncAndStart() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "123";

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);
        SyncUtils.setSyncRunning(false);
        setSyncWifiOnly(context, userId, false);
        setApiUrl(context, userId, LOCALHOST + ':' + TEST_PORT);
        setSyncId(context, userId, userId);

        SyncUtils.checkPendingSyncAndStart(context);

        SpUsers.setPendingSyncStatusForCurrentUser(context, SP_USER_SYNC_PENDING_TRUE);

        SyncUtils.checkPendingSyncAndStart(context);
    }

    @Test
    public void getSingleThreadExecutor() throws Exception {
        final ExecutorService singleThreadExecutor = SyncUtils.getSingleThreadExecutor();
        assertFalse(singleThreadExecutor.isShutdown());
    }

    @Test
    public void getStatusText() throws Exception {
        final int[] statuses = SyncUtils.getStatusText();
        assertTrue(statuses.length > 0);
    }

    @Test
    public void getActionText() throws Exception {
        final int[] actions = SyncUtils.getActionText();
        assertTrue(actions.length > 0);
    }


    /*
        Utils
     */

    private static void setSyncWifiOnly(
            @NonNull final Context context,
            @NonNull final String userId,
            boolean syncWifiOnly) {

        final SharedPreferences sp = context.getSharedPreferences(
                SpUsers.getPreferencesName(userId),
                MODE_PRIVATE);

        sp  .edit()
            .putBoolean(SP_USER_SYNC_WIFI, syncWifiOnly)
            .apply();
    }

    private static void setApiUrl(
            @NonNull final Context context,
            @NonNull final String userId,
            @NonNull final String apiUrl) {

        final SharedPreferences sp = context.getSharedPreferences(
                SpUsers.getPreferencesName(userId),
                MODE_PRIVATE);

        sp  .edit()
            .putString(SP_USER_SYNC_API_URL, apiUrl)
            .apply();
    }

    private static void setSyncId(
            @NonNull final Context context,
            @NonNull final String userId,
            @NonNull final String syncId) {

        final SharedPreferences sp = context.getSharedPreferences(
                SpUsers.getPreferencesName(userId),
                MODE_PRIVATE);

        sp  .edit()
            .putString(SP_USER_SYNC_ID, syncId)
            .apply();
    }
}