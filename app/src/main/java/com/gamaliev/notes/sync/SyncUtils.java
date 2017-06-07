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
import com.gamaliev.notes.entity.ListEntry;
import com.gamaliev.notes.entity.SyncEntry;
import com.gamaliev.notes.common.rest.NoteApi;
import com.gamaliev.notes.sync.db.SyncDbHelper;

import org.json.JSONArray;
import org.json.JSONObject;

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
import static com.gamaliev.notes.conflict.utils.ConflictUtils.checkConflictExistsAndShowStatusBarNotification;
import static com.gamaliev.notes.list.db.ListDbHelper.deleteEntry;
import static com.gamaliev.notes.list.db.ListDbHelper.getNewEntries;
import static com.gamaliev.notes.list.db.ListDbHelper.insertUpdateEntry;
import static com.gamaliev.notes.common.rest.NoteApiUtils.API_KEY_DATA;
import static com.gamaliev.notes.common.rest.NoteApiUtils.API_KEY_EXTRA;
import static com.gamaliev.notes.common.rest.NoteApiUtils.API_KEY_ID;
import static com.gamaliev.notes.common.rest.NoteApiUtils.API_KEY_STATUS;
import static com.gamaliev.notes.common.rest.NoteApiUtils.API_STATUS_OK;
import static com.gamaliev.notes.common.rest.NoteApiUtils.getNoteApi;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

@SuppressWarnings("WeakerAccess")
public final class SyncUtils {

    /* Logger */
    @NonNull private static final String TAG = SyncUtils.class.getSimpleName();

    /* ... */
    @SuppressWarnings("unused")
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
    public static final int ACTION_DELETE_ALL_FROM_SERVER_COMPLETE = 10;
    public static final int ACTION_CONFLICTING_ADDED    = 11;
    public static final int ACTION_PENDING_START_NO_WIFI = 12;
    public static final int ACTION_PENDING_START_NO_INTERNET = 13;

    @NonNull private static final int[] STATUS_TEXT = {
            R.string.fragment_sync_item_status_error,
            R.string.fragment_sync_item_status_success
    };

    @NonNull private static final int[] ACTION_TEXT = {
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
            R.string.fragment_sync_item_action_delete_all_from_server_completed,
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
        String status;
        switch (NetworkUtils.checkNetwork(context)) {
            case NetworkUtils.NETWORK_MOBILE:
                if (SpUsers.getSyncWifiOnlyForCurrentUser(context)) {
                    status = getPendingSyncStatusForCurrentUser(context);
                    if (status == null) {
                        status = SpUsers.SP_USER_SYNC_PENDING_FALSE;
                    }
                    if (!SpUsers.SP_USER_SYNC_PENDING_TRUE.equals(status)) {
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
                status = getPendingSyncStatusForCurrentUser(context);
                if (status == null) {
                    status = SpUsers.SP_USER_SYNC_PENDING_FALSE;
                }
                if (!SpUsers.SP_USER_SYNC_PENDING_TRUE.equals(status)) {
                    addToSyncJournalAndLogAndNotify(
                            context,
                            ACTION_PENDING_START_NO_INTERNET,
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

    /**
     * Starting synchronization of note entries with server in async mode.
     * {@link #isSyncRunning()} must be {@code false}.
     * @param context Context.
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

    /**
     * Starting synchronization of note entries with server in sync mode.
     * @param context Context.
     * @return {@code true} if ok, else {@code false}.
     */
    @SuppressWarnings({"UnusedReturnValue", "SameReturnValue"})
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

    private static int addNewToServer(@NonNull final Context context) {
        int counter = 0;
        final Cursor cursor = getNewEntries(context);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                final JSONObject jsonNewEntry = ListEntry.getJsonObjectFromCursor(context, cursor);
                if (jsonNewEntry == null) {
                    continue;
                }
                try {
                    final NoteApi noteApi = getNoteApi();
                    if (noteApi == null) {
                        throw new Exception("Cannot get note api.");
                    }
                    final Response<String> response = noteApi
                            .add(getSyncIdForCurrentUser(context), jsonNewEntry.toString())
                            .execute();

                    if (response.isSuccessful()) {
                        final String body = response.body();
                        final JSONObject jsonResponse = new JSONObject(body);
                        final String status = jsonResponse.optString(API_KEY_STATUS);

                        if (API_STATUS_OK.equals(status)) {
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

                } catch (Exception e) {
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
                    final NoteApi noteApi = getNoteApi();
                    if (noteApi == null) {
                        throw new Exception("Cannot get note api.");
                    }
                    final Response<String> response = noteApi
                            .delete(getSyncIdForCurrentUser(context),
                                    syncId)
                            .execute();

                    if (response.isSuccessful()) {
                        final String body = response.body();
                        final JSONObject jsonResponse = new JSONObject(body);
                        final String status = jsonResponse.optString(API_KEY_STATUS);

                        if (API_STATUS_OK.equals(status)) {
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

                } catch (Exception e) {
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

    private static int synchronizeFromServer(@NonNull final Context context) {
        int counterAddedOnLocal     = 0;
        int counterConflicting      = 0;
        int counterDeletedOnLocal   = 0;

        try {
            final NoteApi noteApi = getNoteApi();
            if (noteApi == null) {
                throw new Exception("Cannot get note api.");
            }
            final Response<String> response = noteApi
                    .getAll(getSyncIdForCurrentUser(context))
                    .execute();

            if (response.isSuccessful()) {
                final String body = response.body();
                final JSONObject jsonResponse = new JSONObject(body);
                final String status = jsonResponse.optString(API_KEY_STATUS);

                if (API_STATUS_OK.equals(status)) {
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
                                                    ListEntry.getJsonObjectFromCursor(context, cursor);
                                            if (jsonLocal == null) {
                                                continue;
                                            }
                                            final Map<String, String> mapLocal =
                                                    SpCommon.convertJsonToMap(jsonLocal.toString());
                                            final Map<String, String> mapServer =
                                                    SpCommon.convertJsonToMap(data.getString(i));
                                            if (mapLocal == null || mapServer == null) {
                                                continue;
                                            }
                                            mapServer.remove(API_KEY_ID);
                                            mapServer.remove(API_KEY_EXTRA);

                                            // Add to conflict table, if entries not equals.
                                            if (!mapLocal.equals(mapServer)) {
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

        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        return counterAddedOnLocal
                + counterConflicting
                + counterDeletedOnLocal;
    }


    /*
        Delete all from server (Optional. For tests)
     */

    /**
     * Deleting all note entries from server. Note, this action will not add deleted entries
     * in table of deleted entries (for synchronization).
     * @param context Context.
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

        final String currentUser = getSyncIdForCurrentUser(context);
        int counter = 0;

        try {
            final NoteApi noteApi = getNoteApi();
            if (noteApi == null) {
                throw new Exception("Cannot get note api.");
            }
            final Response<String> response = noteApi
                    .getAll(currentUser)
                    .execute();

            if (response.isSuccessful()) {
                final String body = response.body();
                final JSONObject jsonResponse = new JSONObject(body);
                final String status = jsonResponse.optString(API_KEY_STATUS);

                if (API_STATUS_OK.equals(status)) {
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

        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        notification.endProgress();

        addToSyncJournalAndLogAndNotify(
                context,
                ACTION_DELETE_ALL_FROM_SERVER_COMPLETE,
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
            @SuppressWarnings("SameParameterValue") final int status,
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
            showToastRunOnUiThread(message, Toast.LENGTH_LONG);
        }

        notifyObservers(
                SYNC,
                resultCode,
                null);
    }

    /**
     * Logging message with info level. Showing toast if needed.
     * Notifying registered observers with given result code.
     * @param message       Message to logging, and to showing toast.
     * @param showToast     {@code true} if show toast, else {@code false}.
     * @param resultCode    Result code for notifying registered observers.
     */
    @SuppressWarnings("SameParameterValue")
    public static void logAndNotify(
            @NonNull final String message,
            final boolean showToast,
            final int resultCode) {

        Log.i(TAG, message);

        if (showToast) {
            showToastRunOnUiThread(message, Toast.LENGTH_LONG);
        }

        notifyObservers(
                SYNC,
                resultCode,
                null);
    }


    /*
        ...
     */

    /**
     * Checking status of pending synchronization. If {@code true}, then start
     * {@link #synchronize(Context)}.
     *
     * @param context   Context.
     * @return          {@code true} if pending status true, and started synchronization,
     *                  otherwise {@code false}.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static boolean checkPendingSyncAndStart(@NonNull final Context context) {
        String status = getPendingSyncStatusForCurrentUser(context);
        if (status == null) {
            status = SpUsers.SP_USER_SYNC_PENDING_FALSE;
        }
        if (SpUsers.SP_USER_SYNC_PENDING_TRUE.equals(status)) {
            SyncUtils.synchronize(context);
            return true;
        }
        return false;
    }


    /*
        Getters
     */

    @NonNull
    public static ExecutorService getSingleThreadExecutor() {
        return SINGLE_THREAD_EXECUTOR;
    }

    /**
     * @return Clone of {@link #STATUS_TEXT} array.
     */
    @NonNull
    public static int[] getStatusText() {
        return STATUS_TEXT.clone();
    }

    /**
     * @return Clone of {@link #ACTION_TEXT} array.
     */
    @NonNull
    public static int[] getActionText() {
        return ACTION_TEXT.clone();
    }
}
