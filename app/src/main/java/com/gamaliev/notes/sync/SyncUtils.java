package com.gamaliev.notes.sync;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.ProgressNotificationHelper;
import com.gamaliev.notes.common.db.DbQueryBuilder;
import com.gamaliev.notes.common.network.NetworkUtils;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

import static com.gamaliev.notes.common.CommonUtils.showToastRunOnUiThread;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_SYNC_FAILED;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_SYNC_PENDING_START;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_SYNC_START;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_SYNC_SUCCESS;
import static com.gamaliev.notes.common.db.DbHelper.BASE_COLUMN_ID;
import static com.gamaliev.notes.common.db.DbHelper.COMMON_COLUMN_SYNC_ID;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_SYNC_ID_JSON;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_TABLE_NAME;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_CONFLICT_TABLE_NAME;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_DELETED_TABLE_NAME;
import static com.gamaliev.notes.common.db.DbHelper.deleteEntryWithSingle;
import static com.gamaliev.notes.common.db.DbHelper.getEntries;
import static com.gamaliev.notes.common.db.DbHelper.getEntriesCount;
import static com.gamaliev.notes.common.db.DbHelper.insertEntryWithSingleValue;
import static com.gamaliev.notes.common.observers.ObserverHelper.SYNC;
import static com.gamaliev.notes.common.observers.ObserverHelper.notifyObservers;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.SP_USER_SYNC_PENDING_FALSE;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.SP_USER_SYNC_PENDING_TRUE;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.getPendingSyncStatusForCurrentUser;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.getProgressNotificationTimerForCurrentUser;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.getSyncIdForCurrentUser;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.setPendingSyncStatusForCurrentUser;
import static com.gamaliev.notes.conflict.ConflictUtils.checkConflictExistsAndShowStatusBarNotification;
import static com.gamaliev.notes.list.db.ListDbHelper.deleteEntry;
import static com.gamaliev.notes.list.db.ListDbHelper.getNewEntries;
import static com.gamaliev.notes.list.db.ListDbHelper.insertUpdateEntry;
import static com.gamaliev.notes.rest.NoteApiUtils.API_KEY_DATA;
import static com.gamaliev.notes.rest.NoteApiUtils.API_KEY_EXTRA;
import static com.gamaliev.notes.rest.NoteApiUtils.API_KEY_ID;
import static com.gamaliev.notes.rest.NoteApiUtils.API_KEY_STATUS;
import static com.gamaliev.notes.rest.NoteApiUtils.API_STATUS_OK;
import static com.gamaliev.notes.rest.NoteApiUtils.getNoteApi;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

@SuppressWarnings({"WeakerAccess", "unused"})
public final class SyncUtils {

    /* Logger */
    private static final String TAG = SyncUtils.class.getSimpleName();

    /* ... */
    public static final int STATUS_ERROR    = 0;
    public static final int STATUS_OK       = 1;

    public static final int ACTION_NOTHING              = 0;
    public static final int ACTION_ADDED_TO_SERVER      = 1; // If syncId == null
    public static final int ACTION_ADDED_TO_LOCAL       = 2; // If syncId is not exists
    public static final int ACTION_DELETED_FROM_SERVER  = 3; // If local delete
    public static final int ACTION_DELETED_FROM_LOCAL   = 4; // If local have syncId, but server not.
    public static final int ACTION_UPDATED_ON_SERVER    = 5; // compare, select, set.
    public static final int ACTION_UPDATED_ON_LOCAL     = 6; // compare, select, set.
    public static final int ACTION_START                = 7;
    public static final int ACTION_COMPLETE             = 8;
    public static final int ACTION_DELETE_ALL_FROM_SERVER_START = 9;
    public static final int ACTION_CONFLICTING_ADDED    = 10;
    public static final int ACTION_PENDING_START_NO_WIFI = 11;
    public static final int ACTION_PENDING_START_NO_INET = 12;

    public static final int[] STATUS_TEXT = {
            R.string.fragment_sync_item_status_error,
            R.string.fragment_sync_item_status_success
    };

    public static final int[] ACTION_TEXT = {
            R.string.fragment_sync_item_action_nothing,
            R.string.fragment_sync_item_action_add_to_server,
            R.string.fragment_sync_item_action_add_to_local,
            R.string.fragment_sync_item_action_delete_from_server,
            R.string.fragment_sync_item_action_delete_from_local,
            R.string.fragment_sync_item_action_updated_on_server,
            R.string.fragment_sync_item_action_updated_on_local,
            R.string.fragment_sync_item_action_started,
            R.string.fragment_sync_item_action_completed,
            R.string.fragment_sync_item_action_delete_all_from_server_start,
            R.string.fragment_sync_item_action_conflict,
            R.string.fragment_sync_item_action_pending_start_no_wifi,
            R.string.fragment_sync_item_action_pending_start_no_internet
    };
    
    private static final ExecutorService SINGLE_THREAD_EXECUTOR;
    private static boolean sSyncRunning = false;


    /*
        Init
     */

    static {
        SINGLE_THREAD_EXECUTOR = Executors.newSingleThreadExecutor();
    }

    private SyncUtils() {}


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
        Network
     */

    private static void checkNetworkAndUserSettings(@NonNull final Context context) {
        switch (NetworkUtils.checkNetwork(context)) {
            case NetworkUtils.NETWORK_MOBILE:
                if (SpUsers.getSyncWifiOnlyForCurrentUser(context)) {
                    if (!getPendingSyncStatusForCurrentUser(context)
                            .equals(SpUsers.SP_USER_SYNC_PENDING_TRUE)) {

                        addToSyncJournalAndLogAndNotify(
                                context,
                                ACTION_PENDING_START_NO_WIFI,
                                STATUS_OK,
                                0,
                                RESULT_CODE_SYNC_PENDING_START,
                                true);
                    }
                    setPendingSyncStatusForCurrentUser(context, SP_USER_SYNC_PENDING_TRUE);
                    break;
                }

            case NetworkUtils.NETWORK_WIFI:
                makeSynchronize(context);
                break;

            case NetworkUtils.NETWORK_NO:
            default:
                if (!getPendingSyncStatusForCurrentUser(context)
                        .equals(SpUsers.SP_USER_SYNC_PENDING_TRUE)) {

                    addToSyncJournalAndLogAndNotify(
                            context,
                            ACTION_PENDING_START_NO_INET,
                            STATUS_OK,
                            0,
                            RESULT_CODE_SYNC_PENDING_START,
                            true);
                }
                setPendingSyncStatusForCurrentUser(context, SP_USER_SYNC_PENDING_TRUE);
                break;
        }
    }


    /*
        Main
     */

    public static void synchronize(@NonNull final Context context) {
        if (isSyncRunning()) {
            return;
        }

        SINGLE_THREAD_EXECUTOR.submit(new Runnable() {
                @Override
                public void run() {
                checkNetworkAndUserSettings(context);
            }
        });
    }

    public static boolean makeSynchronize(@NonNull final Context context) {
        setSyncRunning(true);

        addToSyncJournalAndLogAndNotify(
                context,
                ACTION_START,
                STATUS_OK,
                0,
                RESULT_CODE_SYNC_START,
                true);

        final ProgressNotificationHelper notification =
                new ProgressNotificationHelper(
                        context,
                        context.getString(R.string.fragment_sync_notification_panel_title),
                        context.getString(R.string.fragment_sync_notification_panel_text),
                        context.getString(R.string.fragment_sync_notification_panel_complete));
        notification.startTimerToEnableNotification(
                getProgressNotificationTimerForCurrentUser(context.getApplicationContext()),
                true);

        int added   = addNewToServer(context);
        int deleted = deleteFromServer(context);
        int updated = synchronizeFromServer(context);
        int sum     = added + deleted + updated;

        checkConflictExistsAndShowStatusBarNotification(context);
        notification.endProgress();

        addToSyncJournalAndLogAndNotify(
                context,
                ACTION_COMPLETE,
                STATUS_OK,
                sum,
                RESULT_CODE_SYNC_SUCCESS,
                true);

        setPendingSyncStatusForCurrentUser(context, SP_USER_SYNC_PENDING_FALSE);
        setSyncRunning(false);
        return true;
    }

    @SuppressWarnings("ConstantConditions")
    private static int addNewToServer(@NonNull final Context context) {
        int counter = 0;
        final Cursor cursor = getNewEntries(context);
        if (cursor != null) {
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

            cursor.close();
        }

        addToSyncJournalAndLogAndNotify(
                context,
                ACTION_ADDED_TO_SERVER,
                STATUS_OK,
                counter,
                RESULT_CODE_SYNC_SUCCESS,
                false);

        return counter;
    }

    private static int deleteFromServer(@NonNull final Context context) {
        int counter = 0;
        final Cursor cursor = getEntries(
                context,
                SYNC_DELETED_TABLE_NAME,
                null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                final String syncId = cursor.getString(cursor.getColumnIndex(COMMON_COLUMN_SYNC_ID));
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
                            deleteEntryWithSingle(
                                    context,
                                    null,
                                    SYNC_DELETED_TABLE_NAME,
                                    COMMON_COLUMN_SYNC_ID,
                                    syncId,
                                    true);
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

            cursor.close();
        }

        addToSyncJournalAndLogAndNotify(
                context,
                ACTION_DELETED_FROM_SERVER,
                STATUS_OK,
                counter,
                RESULT_CODE_SYNC_SUCCESS,
                false);

        return counter;
    }

    @SuppressWarnings("ConstantConditions")
    private static int synchronizeFromServer(@NonNull final Context context) {
        int counterAddedOnLocal     = 0;
        int counterConflicting      = 0;
        int counterDeletedOnLocal   = 0;

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
                                        COMMON_COLUMN_SYNC_ID,
                                        DbQueryBuilder.OPERATOR_EQUALS,
                                        new String[]{syncId});
                                final long entriesCount = getEntriesCount(
                                        context,
                                        LIST_ITEMS_TABLE_NAME,
                                        queryBuilder);

                                if (entriesCount == 0) {
                                    insertUpdateEntry(
                                            context,
                                            ListEntry.convertJsonToListEntry(
                                                    context,
                                                    jsonObject),
                                            false);
                                    counterAddedOnLocal++;
                                }
                            }
                        }

                        addToSyncJournalAndLogAndNotify(
                                context,
                                ACTION_ADDED_TO_LOCAL,
                                STATUS_OK,
                                counterAddedOnLocal,
                                RESULT_CODE_SYNC_SUCCESS,
                                false);

                        // Delete from local and seek changes
                        final Cursor cursor = getEntries(
                                context,
                                LIST_ITEMS_TABLE_NAME,
                                null);
                        if (cursor != null) {
                            start:
                            while (cursor.moveToNext()) {
                                final String syncIdLocal = cursor
                                        .getString(cursor.getColumnIndex(COMMON_COLUMN_SYNC_ID));

                                if (syncIdLocal != null) {
                                    for (int i = 0; i < data.length(); i++) {
                                        final JSONObject jsonServer = data.getJSONObject(i);
                                        final String syncIdServer = jsonServer
                                                .optString(LIST_ITEMS_COLUMN_SYNC_ID_JSON, null);

                                        if (syncIdLocal.equals(syncIdServer)) {
                                            final JSONObject jsonLocal =
                                                    ListEntry.getJsonObject(context, cursor);
                                            final Map<String, String> mapLocal =
                                                    SpCommon.convertJsonToMap(jsonLocal.toString());
                                            final Map<String, String> mapServer =
                                                    SpCommon.convertJsonToMap(data.getString(i));
                                            mapServer.remove(API_KEY_ID);
                                            mapServer.remove(API_KEY_EXTRA);

                                            // Add to conflict table, if entries not equals.
                                            if(!mapLocal.equals(mapServer)) {
                                                insertEntryWithSingleValue(
                                                        context,
                                                        null,
                                                        SYNC_CONFLICT_TABLE_NAME,
                                                        COMMON_COLUMN_SYNC_ID,
                                                        syncIdLocal);
                                                counterConflicting++;
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
                        }

                        addToSyncJournalAndLogAndNotify(
                                context,
                                ACTION_DELETED_FROM_LOCAL,
                                STATUS_OK,
                                counterDeletedOnLocal,
                                RESULT_CODE_SYNC_SUCCESS,
                                false);

                        addToSyncJournalAndLogAndNotify(
                                context,
                                ACTION_CONFLICTING_ADDED,
                                STATUS_OK,
                                counterConflicting,
                                RESULT_CODE_SYNC_SUCCESS,
                                false);
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

        return counterAddedOnLocal
                + counterConflicting
                + counterDeletedOnLocal;
    }


    /*
        Delete all from server (Optional. For tests)
     */

    public static void deleteAllFromServerAsync(@NonNull final Context context) {
        SINGLE_THREAD_EXECUTOR.submit(new Runnable() {
            @Override
            public void run() {
                deleteAllFromServer(context);
            }
        });
    }

    private static void deleteAllFromServer(@NonNull final Context context) {
        switch (NetworkUtils.checkNetwork(context)) {
            case NetworkUtils.NETWORK_MOBILE:
                if (SpUsers.getSyncWifiOnlyForCurrentUser(context)) {
                    logAndNotify(
                            context,
                            context.getString(R.string.fragment_sync_item_action_delete_all_from_server_no_wifi),
                            true,
                            RESULT_CODE_SYNC_FAILED);
                    break;
                }

            case NetworkUtils.NETWORK_WIFI:
                deleteAllFromServerPerform(context);
                break;

            case NetworkUtils.NETWORK_NO:
                logAndNotify(
                        context,
                        context.getString(R.string.fragment_sync_item_action_delete_all_from_server_no_internet),
                        true,
                        RESULT_CODE_SYNC_FAILED);

            default:
                break;
        }
    }

    private static void deleteAllFromServerPerform(@NonNull final Context context) {
        addToSyncJournalAndLogAndNotify(
                context,
                ACTION_DELETE_ALL_FROM_SERVER_START,
                STATUS_OK,
                0,
                RESULT_CODE_SYNC_SUCCESS,
                true);

        final ProgressNotificationHelper notification =
                new ProgressNotificationHelper(
                        context,
                        context.getString(R.string.fragment_sync_notification_panel_del_all_title),
                        context.getString(R.string.fragment_sync_notification_panel_del_all_text),
                        context.getString(R.string.fragment_sync_notification_panel_del_all_complete));
        notification.startTimerToEnableNotification(
                getProgressNotificationTimerForCurrentUser(context.getApplicationContext()),
                true);

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

        notification.endProgress();

        addToSyncJournalAndLogAndNotify(
                context,
                ACTION_DELETED_FROM_SERVER,
                STATUS_OK,
                counter,
                RESULT_CODE_SYNC_SUCCESS,
                true);
    }


    /*
        Logging
     */

    /**
     * @param context       Context.
     * @param action        See: {@link SyncDbHelper}, ACTION*.
     * @param status        See: {@link SyncDbHelper}, STATUS*.
     * @param count         Count of processed entries.
     * @param resultCode    See: {@link SyncUtils}, RESULT*.
     * @param showToast     True if show, false if not.
     */
    public static void addToSyncJournalAndLogAndNotify(
            @NonNull final Context context,
            final int action,
            final int status,
            final int count,
            final int resultCode,
            final boolean showToast) {

        final String message = context.getString(ACTION_TEXT[action]);

        final SyncEntry entry = new SyncEntry();
        entry.setFinished(new Date());
        entry.setAction(action);
        entry.setStatus(status);
        entry.setAmount(count);
        SyncDbHelper.insertEntry(context, entry);

        Log.i(TAG, message);

        if (showToast) {
            showToastRunOnUiThread(context, message, Toast.LENGTH_LONG);
        }

        notifyObservers(
                SYNC,
                resultCode,
                null);
    }

    public static void logAndNotify(
            @NonNull final Context context,
            @NonNull final String message,
            final boolean showToast,
            final int resultCode) {

        Log.i(TAG, message);

        if (showToast) {
            showToastRunOnUiThread(context, message, Toast.LENGTH_LONG);
        }

        notifyObservers(
                SYNC,
                resultCode,
                null);
    }


    /*
        ...
     */

    public static boolean checkPendingSyncAndStart(@NonNull final Context context) {
        if (getPendingSyncStatusForCurrentUser(context)
                .equals(SpUsers.SP_USER_SYNC_PENDING_TRUE)) {
            SyncUtils.synchronize(context);
            return true;
        }
        return false;
    }


    /*
        Getters
     */

    public static ExecutorService getSingleThreadExecutor() {
        return SINGLE_THREAD_EXECUTOR;
    }
}
