package com.gamaliev.notes.sync;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.network.NetworkUtils;
import com.gamaliev.notes.common.OnCompleteListener;
import com.gamaliev.notes.common.ProgressNotificationHelper;
import com.gamaliev.notes.common.db.DbQueryBuilder;
import com.gamaliev.notes.common.shared_prefs.SpCommon;
import com.gamaliev.notes.common.shared_prefs.SpUsers;
import com.gamaliev.notes.list.db.ListDbHelper;
import com.gamaliev.notes.model.ListEntry;
import com.gamaliev.notes.model.SyncEntry;
import com.gamaliev.notes.rest.NoteApi;
import com.gamaliev.notes.sync.db.SyncDbHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.WeakHashMap;

import retrofit2.Response;

import static com.gamaliev.notes.common.CommonUtils.showToast;
import static com.gamaliev.notes.common.db.DbHelper.BASE_COLUMN_ID;
import static com.gamaliev.notes.common.db.DbHelper.DELETED_COLUMN_SYNC_ID;
import static com.gamaliev.notes.common.db.DbHelper.DELETED_TABLE_NAME;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_SYNC_ID;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_SYNC_ID_JSON;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_CONFLICT_COLUMN_SYNC_ID;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_CONFLICT_TABLE_NAME;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.SP_USER_SYNC_PENDING_FALSE;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.SP_USER_SYNC_PENDING_TRUE;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.getPendingSyncStatusForCurrentUser;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.getProgressNotificationTimerForCurrentUser;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.getSyncIdForCurrentUser;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.setPendingSyncStatusForCurrentUser;
import static com.gamaliev.notes.list.db.ListDbHelper.deleteEntry;
import static com.gamaliev.notes.list.db.ListDbHelper.deleteEntryWithSingleSyncId;
import static com.gamaliev.notes.list.db.ListDbHelper.getEntriesWithSyncIdField;
import static com.gamaliev.notes.list.db.ListDbHelper.getEntries;
import static com.gamaliev.notes.list.db.ListDbHelper.getNewEntries;
import static com.gamaliev.notes.list.db.ListDbHelper.insertEntry;
import static com.gamaliev.notes.list.db.ListDbHelper.insertSyncIdEntry;
import static com.gamaliev.notes.rest.NoteApiUtils.getNoteApi;
import static com.gamaliev.notes.sync.db.SyncDbHelper.ACTION_ADDED_TO_LOCAL;
import static com.gamaliev.notes.sync.db.SyncDbHelper.ACTION_ADDED_TO_SERVER;
import static com.gamaliev.notes.sync.db.SyncDbHelper.ACTION_COMPLETE;
import static com.gamaliev.notes.sync.db.SyncDbHelper.ACTION_CONFLICTED_ADDED;
import static com.gamaliev.notes.sync.db.SyncDbHelper.ACTION_DELETED_FROM_LOCAL;
import static com.gamaliev.notes.sync.db.SyncDbHelper.ACTION_DELETED_FROM_SERVER;
import static com.gamaliev.notes.sync.db.SyncDbHelper.ACTION_DELETE_ALL_FROM_SERVER_START;
import static com.gamaliev.notes.sync.db.SyncDbHelper.ACTION_PENDING_START_NO_INET;
import static com.gamaliev.notes.sync.db.SyncDbHelper.ACTION_PENDING_START_NO_WIFI;
import static com.gamaliev.notes.sync.db.SyncDbHelper.ACTION_START;
import static com.gamaliev.notes.sync.db.SyncDbHelper.ACTION_TEXT;
import static com.gamaliev.notes.sync.db.SyncDbHelper.STATUS_OK;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class SyncUtils {

    /* Logger */
    private static final String TAG = SyncUtils.class.getSimpleName();

    /* ... */
    public static final int RESULT_CODE_PENDING_START = 0;
    public static final int RESULT_CODE_START       = 1;
    public static final int RESULT_CODE_SUCCESS     = 2;
    public static final int RESULT_CODE_FAILED      = 3;

    private static final String API_KEY_STATUS      = "status";
    private static final String API_KEY_DATA        = "data";
    private static final String API_STATUS_OK       = "ok";
    private static final String API_STATUS_ERROR    = "error";
    private static final String API_KEY_ID          = "id";
    private static final String API_KEY_EXTRA       = "extra";

    private static final Map<String, OnCompleteListener> OBSERVERS;

    private static boolean sSyncRunning = false;


    /*
        Init
     */

    static {
        OBSERVERS = new WeakHashMap<>();
    }

    private SyncUtils() {}


    /*
        Weak observers
     */

    public static void addObserver(
            @NonNull final String key,
            @NonNull final OnCompleteListener observer) {
        OBSERVERS.put(key, observer);
    }

    public static void removeObserver(@NonNull final String key) {
        OBSERVERS.remove(key);
    }


    /*
        Sync is Running
     */

    public static synchronized boolean isSyncRunning() {
        return sSyncRunning;
    }

    public static synchronized void setSyncRunning(final boolean syncRunning) {
        sSyncRunning = syncRunning;
    }

    /*
        Main
     */

    public static void synchronize(@NonNull final Context context) {
        new Thread(new Runnable() {
                @Override
                public void run() {
                checkNetworkAndUserSettings(context);
            }
        }).start();
    }

    private static void checkNetworkAndUserSettings(@NonNull final Context context) {

        switch (NetworkUtils.checkNetwork(context)) {
            case NetworkUtils.NETWORK_MOBILE:
                if (SpUsers.getSyncWifiOnlyForCurrentUser(context)) {
                    if (!getPendingSyncStatusForCurrentUser(context)
                            .equals(SpUsers.SP_USER_SYNC_PENDING_TRUE)) {
                        //
                        setPendingSyncStatusForCurrentUser(context, SP_USER_SYNC_PENDING_TRUE);
                        //
                        final SyncEntry pendingStart = new SyncEntry();
                        pendingStart.setFinished(new Date());
                        pendingStart.setAction(ACTION_PENDING_START_NO_WIFI);
                        pendingStart.setStatus(STATUS_OK);
                        pendingStart.setAmount(0);
                        makeOperations(
                                context,
                                pendingStart,
                                true,
                                context.getString(ACTION_TEXT[ACTION_PENDING_START_NO_WIFI]),
                                RESULT_CODE_PENDING_START);
                    }
                    break;
                }

            case NetworkUtils.NETWORK_WIFI:
                makeSynchronize(context);
                break;

            case NetworkUtils.NETWORK_NO:
            default:
                if (!getPendingSyncStatusForCurrentUser(context)
                        .equals(SpUsers.SP_USER_SYNC_PENDING_TRUE)) {
                    //
                    setPendingSyncStatusForCurrentUser(context, SP_USER_SYNC_PENDING_TRUE);
                    //
                    final SyncEntry pendingStart = new SyncEntry();
                    pendingStart.setFinished(new Date());
                    pendingStart.setAction(ACTION_PENDING_START_NO_INET);
                    pendingStart.setStatus(STATUS_OK);
                    pendingStart.setAmount(0);
                    makeOperations(
                            context,
                            pendingStart,
                            true,
                            context.getString(ACTION_TEXT[ACTION_PENDING_START_NO_INET]),
                            RESULT_CODE_PENDING_START);
                }
                break;
        }
    }

    public static boolean makeSynchronize(@NonNull final Context context) {

        //
        if (isSyncRunning()) {
            return false;
        }

        //
        setSyncRunning(true);

        //
        final SyncEntry entryStart = new SyncEntry();
        entryStart.setFinished(new Date());
        entryStart.setAction(ACTION_START);
        entryStart.setStatus(STATUS_OK);
        entryStart.setAmount(0);
        makeOperations(
                context,
                entryStart,
                true,
                context.getString(ACTION_TEXT[ACTION_START]),
                RESULT_CODE_START);

        // Continuing Progress notifications start.
        final ProgressNotificationHelper notification =
                new ProgressNotificationHelper(
                        context,
                        context.getString(R.string.activity_sync_notification_panel_title),
                        context.getString(R.string.activity_sync_notification_panel_text),
                        context.getString(R.string.activity_sync_notification_panel_complete));

        // Timer for notification enable
        notification.startTimerToEnableNotification(
                getProgressNotificationTimerForCurrentUser(context.getApplicationContext()),
                true);

        //
        int added   = addNewToServer(context);
        int deleted = deleteFromServer(context);
        int updated = synchronizeFromServer(context);
        int sum     = added + deleted + updated;

        // Notification complete
        notification.endProgress();

        //
        final SyncEntry entryFinish = new SyncEntry();
        entryFinish.setFinished(new Date());
        entryFinish.setAction(ACTION_COMPLETE);
        entryFinish.setStatus(STATUS_OK);
        entryFinish.setAmount(sum);
        makeOperations(
                context,
                entryFinish,
                true,
                context.getString(ACTION_TEXT[ACTION_COMPLETE]),
                RESULT_CODE_SUCCESS);

        //
        setPendingSyncStatusForCurrentUser(context, SP_USER_SYNC_PENDING_FALSE);

        //
        setSyncRunning(false);

        return false;
    }

    private static int addNewToServer(@NonNull final Context context) {

        int counter = 0;

        final Cursor cursor = getNewEntries(context);
        while (cursor.moveToNext()) {
            final JSONObject jsonNewEntry = ListEntry.getJsonObject(context, cursor);

            try {
                final Response<String> response = getNoteApi()
                        .add(   getSyncIdForCurrentUser(context),
                                jsonNewEntry.toString())
                        .execute();

                if (response.isSuccessful()) {
                    final String body = response.body();
                    final JSONObject jsonResponse = new JSONObject(body);
                    final String status = jsonResponse.optString(API_KEY_STATUS);

                    if (status.equals(API_STATUS_OK)) {
                        final String newSyncId = jsonResponse.optString(API_KEY_DATA);
                        final String id = cursor.getString(cursor.getColumnIndex(BASE_COLUMN_ID));
                        ListDbHelper.updateSyncId(context, id, newSyncId);
                        counter++;

                    } else {
                        Log.e(TAG, response.toString());
                    }

                } else {
                    Log.e(TAG, response.toString());
                }

            } catch (IOException | JSONException e) {
                Log.e(TAG, e.toString());
            }
        }

        //
        cursor.close();

        // Add result to journal
        final SyncEntry entry = new SyncEntry();
        entry.setFinished(new Date());
        entry.setAction(ACTION_ADDED_TO_SERVER);
        entry.setStatus(STATUS_OK);
        entry.setAmount(counter);
        makeOperations(
                context,
                entry,
                false,
                context.getString(ACTION_TEXT[ACTION_ADDED_TO_SERVER]),
                RESULT_CODE_SUCCESS);

        return counter;
    }

    private static int deleteFromServer(@NonNull final Context context) {

        int counter = 0;

        final Cursor cursor = getEntriesWithSyncIdField(context, DELETED_TABLE_NAME);
        while (cursor.moveToNext()) {
            final String syncId = cursor.getString(cursor.getColumnIndex(DELETED_COLUMN_SYNC_ID));

            try {
                final Response<String> response = getNoteApi()
                        .delete(getSyncIdForCurrentUser(context),
                                syncId)
                        .execute();

                if (response.isSuccessful()) {
                    final String body = response.body();
                    final JSONObject jsonResponse = new JSONObject(body);
                    final String status = jsonResponse.optString(API_KEY_STATUS);

                    if (status.equals(API_STATUS_OK)) {
                        deleteEntryWithSingleSyncId(
                                context,
                                syncId,
                                DELETED_TABLE_NAME,
                                DELETED_COLUMN_SYNC_ID);
                        counter++;

                    } else {
                        Log.e(TAG, response.toString());
                    }

                } else {
                    Log.e(TAG, response.toString());
                }

            } catch (IOException | JSONException e) {
                Log.e(TAG, e.toString());
            }
        }

        //
        cursor.close();

        // Add result to journal
        final SyncEntry entry = new SyncEntry();
        entry.setFinished(new Date());
        entry.setAction(ACTION_DELETED_FROM_SERVER);
        entry.setStatus(STATUS_OK);
        entry.setAmount(counter);
        makeOperations(
                context,
                entry,
                false,
                context.getString(ACTION_TEXT[ACTION_DELETED_FROM_SERVER]),
                RESULT_CODE_SUCCESS);

        return counter;
    }

    private static int synchronizeFromServer(@NonNull final Context context) {

        //
        int counterAddedOnLocal     = 0;
        int counterConflicted       = 0;
        int counterDeletedOnLocal   = 0;

        //
        try {
            final Response<String> response = getNoteApi()
                    .getAll(getSyncIdForCurrentUser(context))
                    .execute();

            if (response.isSuccessful()) {
                final String body = response.body();
                final JSONObject jsonResponse = new JSONObject(body);
                final String status = jsonResponse.optString(API_KEY_STATUS);

                if (status.equals(API_STATUS_OK)) {

                    final JSONArray data = jsonResponse.getJSONArray(API_KEY_DATA);
                    if (data != null) {

                        // Add new to local.
                        for (int i = 0; i < data.length(); i++) {
                            final JSONObject jsonObject = data.getJSONObject(i);
                            final String syncId = jsonObject.optString(LIST_ITEMS_COLUMN_SYNC_ID_JSON, null);

                            if (syncId != null) {
                                final DbQueryBuilder queryBuilder = new DbQueryBuilder();
                                queryBuilder.addOr(
                                        LIST_ITEMS_COLUMN_SYNC_ID,
                                        DbQueryBuilder.OPERATOR_EQUALS,
                                        new String[]{syncId});
                                final long entriesCount = ListDbHelper.getEntriesCount(context, queryBuilder);

                                if (entriesCount == 0) {
                                    insertEntry(context, ListEntry.convertJsonToListEntry(
                                            context,
                                            jsonObject));
                                    counterAddedOnLocal++;
                                }
                            }
                        }

                        // Add to local finish.
                        final SyncEntry addToLocalFinish = new SyncEntry();
                        addToLocalFinish.setFinished(new Date());
                        addToLocalFinish.setAction(ACTION_ADDED_TO_LOCAL);
                        addToLocalFinish.setStatus(STATUS_OK);
                        addToLocalFinish.setAmount(counterAddedOnLocal);
                        makeOperations(
                                context,
                                addToLocalFinish,
                                false,
                                context.getString(ACTION_TEXT[ACTION_ADDED_TO_LOCAL]),
                                RESULT_CODE_SUCCESS);

                        // Delete from local and seek changes
                        final Cursor cursor = getEntries(context);
                        start:
                        while (cursor.moveToNext()) {
                            final String syncIdLocal = cursor
                                    .getString(cursor.getColumnIndex(LIST_ITEMS_COLUMN_SYNC_ID));

                            if (syncIdLocal != null) {
                                for (int i = 0; i < data.length(); i++) {
                                    final JSONObject jsonServer = data.getJSONObject(i);
                                    final String syncIdServer = jsonServer
                                            .optString(LIST_ITEMS_COLUMN_SYNC_ID_JSON, null);

                                    // If sync id equals.
                                    if (syncIdLocal.equals(syncIdServer)) {
                                        final JSONObject jsonLocal =
                                                ListEntry.getJsonObject(context, cursor);
                                        final Map<String, String> mapLocal =
                                                SpCommon.convertJsonToMap(jsonLocal.toString());
                                        final Map<String, String> mapServer =
                                                SpCommon.convertJsonToMap(data.getString(i));
                                        mapServer.remove(API_KEY_ID);
                                        mapServer.remove(API_KEY_EXTRA);

                                        // If entries not equals, then add to conflict table.
                                        if(!mapLocal.equals(mapServer)) {
                                            insertSyncIdEntry(
                                                    context,
                                                    Long.parseLong(syncIdLocal),
                                                    null,
                                                    SYNC_CONFLICT_TABLE_NAME,
                                                    SYNC_CONFLICT_COLUMN_SYNC_ID);
                                            counterConflicted++;
                                        }
                                        continue start;
                                    }
                                }

                                // Delete from local, if syncId not exists on server
                                deleteEntry(
                                        context,
                                        cursor.getLong(cursor.getColumnIndex(BASE_COLUMN_ID)),
                                        false);
                                counterDeletedOnLocal++;
                            }
                        }
                        cursor.close();

                        // Delete on local finish
                        final SyncEntry deleteFromLocalFinish = new SyncEntry();
                        deleteFromLocalFinish.setFinished(new Date());
                        deleteFromLocalFinish.setAction(ACTION_DELETED_FROM_LOCAL);
                        deleteFromLocalFinish.setStatus(STATUS_OK);
                        deleteFromLocalFinish.setAmount(counterDeletedOnLocal);
                        makeOperations(
                                context,
                                deleteFromLocalFinish,
                                false,
                                context.getString(ACTION_TEXT[ACTION_DELETED_FROM_LOCAL]),
                                RESULT_CODE_SUCCESS);

                        // Conflicted finish
                        final SyncEntry conflictedFinish = new SyncEntry();
                        conflictedFinish.setFinished(new Date());
                        conflictedFinish.setAction(ACTION_CONFLICTED_ADDED);
                        conflictedFinish.setStatus(STATUS_OK);
                        conflictedFinish.setAmount(counterConflicted);
                        makeOperations(
                                context,
                                conflictedFinish,
                                false,
                                context.getString(ACTION_TEXT[ACTION_CONFLICTED_ADDED]),
                                RESULT_CODE_SUCCESS);
                    }

                } else {
                    Log.e(TAG, response.toString());
                }

            } else {
                Log.e(TAG, response.toString());
            }

        } catch (IOException | JSONException e) {
            Log.e(TAG, e.toString());
        }

        //
        return counterAddedOnLocal
                + counterConflicted
                + counterDeletedOnLocal;
    }


    /*
        Delete all from server (Optional. For tests)
     */

    public static void deleteAllFromServerAsync(@NonNull final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                deleteAllFromServer(context);
            }
        }).start();
    }

    private static void deleteAllFromServer(@NonNull final Context context) {

        switch (NetworkUtils.checkNetwork(context)) {
            case NetworkUtils.NETWORK_MOBILE:
                if (SpUsers.getSyncWifiOnlyForCurrentUser(context)) {
                    makeOperations(
                            context,
                            null,
                            true,
                            context.getString(R.string.activity_sync_item_action_delete_all_from_server_no_wifi),
                            RESULT_CODE_FAILED);
                    break;
                }
            case NetworkUtils.NETWORK_WIFI:
                deleteAllFromServerPerform(context);
                break;

            case NetworkUtils.NETWORK_NO:
                makeOperations(
                        context,
                        null,
                        true,
                        context.getString(R.string.activity_sync_item_action_delete_all_from_server_no_internet),
                        RESULT_CODE_FAILED);
            default:
                break;
        }
    }

    private static void deleteAllFromServerPerform(@NonNull final Context context) {

        // Start
        final SyncEntry entryStart = new SyncEntry();
        entryStart.setFinished(new Date());
        entryStart.setAction(ACTION_DELETE_ALL_FROM_SERVER_START);
        entryStart.setStatus(STATUS_OK);
        entryStart.setAmount(0);
        makeOperations(
                context,
                entryStart,
                true,
                context.getString(ACTION_TEXT[ACTION_DELETE_ALL_FROM_SERVER_START]),
                RESULT_CODE_SUCCESS);

        // Continuing Progress notifications start.
        final ProgressNotificationHelper notification =
                new ProgressNotificationHelper(
                        context,
                        context.getString(R.string.activity_sync_notification_panel_del_all_title),
                        context.getString(R.string.activity_sync_notification_panel_del_all_text),
                        context.getString(R.string.activity_sync_notification_panel_del_all_complete));

        // Timer for notification enable
        notification.startTimerToEnableNotification(
                getProgressNotificationTimerForCurrentUser(context.getApplicationContext()),
                true);

        // Main
        final NoteApi noteApi = getNoteApi();
        final String currentUser = getSyncIdForCurrentUser(context);
        int counter = 0;

        try {
            final Response<String> response = noteApi
                    .getAll(currentUser)
                    .execute();

            if (response.isSuccessful()) {
                final String body = response.body();
                final JSONObject jsonResponse = new JSONObject(body);
                final String status = jsonResponse.optString(API_KEY_STATUS);

                if (status.equals(API_STATUS_OK)) {
                    final JSONArray entries = jsonResponse.getJSONArray(API_KEY_DATA);
                    for (int i = 0; i < entries.length(); i++) {
                        final JSONObject jsonObject = entries.getJSONObject(i);
                        final String syncId = jsonObject.optString(LIST_ITEMS_COLUMN_SYNC_ID_JSON, null);

                        if (syncId != null) {
                            noteApi .delete(currentUser, syncId)
                                    .execute();
                            counter++;
                        }
                    }

                } else {
                    Log.e(TAG, response.toString());
                }

            } else {
                Log.e(TAG, response.toString());
            }

        } catch (IOException | JSONException e) {
            Log.e(TAG, e.toString());
        }

        // Notification complete
        notification.endProgress();

        // Finish
        final SyncEntry entryFinish = new SyncEntry();
        entryFinish.setFinished(new Date());
        entryFinish.setAction(ACTION_DELETED_FROM_SERVER);
        entryFinish.setStatus(STATUS_OK);
        entryFinish.setAmount(counter);
        makeOperations(
                context,
                entryFinish,
                true,
                context.getString(ACTION_TEXT[ACTION_DELETED_FROM_SERVER]),
                RESULT_CODE_SUCCESS);
    }


    /*
        ...
     */

    private static boolean makeOperations(
            @NonNull final Context context,
            @Nullable final SyncEntry entry,
            final boolean syncStartShowToast,
            @Nullable final String message,
            final int resultCode) {

        // to Console.
        Log.i(TAG, message);

        // to Database.
        if (entry != null) {
            SyncDbHelper.insertEntry(context, entry);
        }

        // Toast
        if (syncStartShowToast) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    showToast(context, message, Toast.LENGTH_LONG);
                }
            });
        }

        // Notify observers
        for (OnCompleteListener value : OBSERVERS.values()) {
            value.onComplete(resultCode);
        }

        return true;
    }
}
