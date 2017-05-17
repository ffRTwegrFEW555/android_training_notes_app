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
import com.gamaliev.notes.common.NetworkUtils;
import com.gamaliev.notes.common.OnCompleteListener;
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
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_SYNC_ID;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_SYNC_ID_JSON;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.getSyncIdForCurrentUser;
import static com.gamaliev.notes.list.db.ListDbHelper.deleteEntry;
import static com.gamaliev.notes.list.db.ListDbHelper.deleteEntryFromDeleted;
import static com.gamaliev.notes.list.db.ListDbHelper.getDeleted;
import static com.gamaliev.notes.list.db.ListDbHelper.getEntries;
import static com.gamaliev.notes.list.db.ListDbHelper.getNewEntries;
import static com.gamaliev.notes.list.db.ListDbHelper.insertEntry;
import static com.gamaliev.notes.rest.NoteApiUtils.getNoteApi;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class SyncUtils {

    /* Logger */
    private static final String TAG = SyncUtils.class.getSimpleName();

    /* ... */
    public static final int RESULT_CODE_START       = 1;
    public static final int RESULT_CODE_SUCCESS     = 2;

    private static final String API_KEY_STATUS      = "status";
    private static final String API_KEY_DATA        = "data";
    private static final String API_STATUS_OK       = "ok";
    private static final String API_STATUS_ERROR    = "error";
    private static final String API_KEY_ID          = "id";
    private static final String API_KEY_EXTRA       = "extra";

    private static final Map<String, OnCompleteListener> OBSERVERS;


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
        ...
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
                    // TODO: +Pending
                    makeSynchronize(context);
                    break;
                }

            case NetworkUtils.NETWORK_WIFI:
                makeSynchronize(context);
                break;

            case NetworkUtils.NETWORK_NO:
            default:
                // TODO: +Pending
                makeSynchronize(context);
                break;
        }
    }

    public static boolean makeSynchronize(@NonNull final Context context) {

        //
        final SyncEntry entryStart = new SyncEntry();
        entryStart.setFinished(new Date());
        entryStart.setAction(SyncDbHelper.ACTION_START);
        entryStart.setStatus(SyncDbHelper.STATUS_OK);
        entryStart.setAmount(0);
        makeOperations(
                context,
                entryStart,
                true,
                context.getString(R.string.activity_sync_item_action_started),
                RESULT_CODE_START);

        //
        int added   = addNewToServer(context);
        int deleted = deleteFromServer(context);
        int updated = synchronizeFromServer(context);
        int sum = added + deleted + updated;

        //
        final SyncEntry entryFinish = new SyncEntry();
        entryFinish.setFinished(new Date());
        entryFinish.setAction(SyncDbHelper.ACTION_COMPLETE);
        entryFinish.setStatus(SyncDbHelper.STATUS_OK);
        entryFinish.setAmount(sum);
        makeOperations(
                context,
                entryFinish,
                true,
                context.getString(R.string.activity_sync_item_action_completed),
                RESULT_CODE_SUCCESS);

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
        entry.setAction(SyncDbHelper.ACTION_ADDED_TO_SERVER);
        entry.setStatus(SyncDbHelper.STATUS_OK);
        entry.setAmount(counter);
        makeOperations(
                context,
                entry,
                false,
                context.getString(R.string.activity_sync_item_action_add_to_server),
                RESULT_CODE_SUCCESS);

        return counter;
    }

    private static int deleteFromServer(@NonNull final Context context) {

        int counter = 0;

        final Cursor cursor = getDeleted(context);
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
                        deleteEntryFromDeleted(context, syncId);
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
        entry.setAction(SyncDbHelper.ACTION_DELETED_FROM_SERVER);
        entry.setStatus(SyncDbHelper.STATUS_OK);
        entry.setAmount(counter);
        makeOperations(
                context,
                entry,
                false,
                context.getString(R.string.activity_sync_item_action_delete_from_server),
                RESULT_CODE_SUCCESS);

        return counter;
    }

    private static int synchronizeFromServer(@NonNull final Context context) {

        //
        int counterAddedOnLocal     = 0;
        int counterUpdatedOnServer  = 0;
        int counterUpdatedOnLocal   = 0;
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
                        addToLocalFinish.setAction(SyncDbHelper.ACTION_ADDED_TO_LOCAL);
                        addToLocalFinish.setStatus(SyncDbHelper.STATUS_OK);
                        addToLocalFinish.setAmount(counterAddedOnLocal);
                        makeOperations(
                                context,
                                addToLocalFinish,
                                false,
                                context.getString(R.string.activity_sync_item_action_add_to_local),
                                RESULT_CODE_SUCCESS);

                        // Delete from local and seek changes
                        final Cursor cursor = getEntries(context);
                        while (cursor.moveToNext()) {
                            final String syncIdLocal = cursor
                                    .getString(cursor.getColumnIndex(LIST_ITEMS_COLUMN_SYNC_ID));

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
                                        boolean equals = mapLocal.equals(mapServer);
                                        // TODO: chutok ostalos
                                        break;
                                    }

                                    if (i == data.length() - 1) {
                                        deleteEntry(context, Long.parseLong(syncIdLocal), false);
                                        counterDeletedOnLocal++;
                                    }
                                }
                            }
                        }
                        cursor.close();

                        // Delete on local finish
                        final SyncEntry deleteFromLocalFinish = new SyncEntry();
                        deleteFromLocalFinish.setFinished(new Date());
                        deleteFromLocalFinish.setAction(SyncDbHelper.ACTION_DELETED_FROM_LOCAL);
                        deleteFromLocalFinish.setStatus(SyncDbHelper.STATUS_OK);
                        deleteFromLocalFinish.setAmount(counterDeletedOnLocal);
                        makeOperations(
                                context,
                                deleteFromLocalFinish,
                                false,
                                context.getString(R.string.activity_sync_item_action_delete_from_local),
                                RESULT_CODE_SUCCESS);

                        // Update on local finish
                        final SyncEntry localChangesFinish = new SyncEntry();
                        localChangesFinish.setFinished(new Date());
                        localChangesFinish.setAction(SyncDbHelper.ACTION_UPDATED_ON_LOCAL);
                        localChangesFinish.setStatus(SyncDbHelper.STATUS_OK);
                        localChangesFinish.setAmount(counterUpdatedOnLocal);
                        makeOperations(
                                context,
                                localChangesFinish,
                                false,
                                context.getString(R.string.activity_sync_item_action_updated_on_local),
                                RESULT_CODE_SUCCESS);

                        // Update on server finish
                        final SyncEntry serverChangesFinish = new SyncEntry();
                        serverChangesFinish.setFinished(new Date());
                        serverChangesFinish.setAction(SyncDbHelper.ACTION_UPDATED_ON_SERVER);
                        serverChangesFinish.setStatus(SyncDbHelper.STATUS_OK);
                        serverChangesFinish.setAmount(counterUpdatedOnServer);
                        makeOperations(
                                context,
                                serverChangesFinish,
                                false,
                                context.getString(R.string.activity_sync_item_action_updated_on_server),
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
                + counterDeletedOnLocal
                + counterUpdatedOnLocal
                + counterUpdatedOnServer;
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
                    break;
                }
            case NetworkUtils.NETWORK_WIFI:
                deleteAllFromServerPerform(context);
                break;

            case NetworkUtils.NETWORK_NO:
            default:
                break;
        }
    }

    private static void deleteAllFromServerPerform(@NonNull final Context context) {

        // Start
        final SyncEntry entryStart = new SyncEntry();
        entryStart.setFinished(new Date());
        entryStart.setAction(SyncDbHelper.ACTION_DELETE_ALL_FROM_SERVER_START);
        entryStart.setStatus(SyncDbHelper.STATUS_OK);
        entryStart.setAmount(0);
        makeOperations(
                context,
                entryStart,
                true,
                context.getString(R.string.activity_sync_item_action_delete_all_from_server_start),
                RESULT_CODE_SUCCESS);

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

        // Finish
        final SyncEntry entryFinish = new SyncEntry();
        entryFinish.setFinished(new Date());
        entryFinish.setAction(SyncDbHelper.ACTION_DELETED_FROM_SERVER);
        entryFinish.setStatus(SyncDbHelper.STATUS_OK);
        entryFinish.setAmount(counter);
        makeOperations(
                context,
                entryFinish,
                true,
                context.getString(R.string.activity_sync_item_action_delete_all_from_server_completed),
                RESULT_CODE_SUCCESS);
    }


    /*
        ...
     */

    private static boolean makeOperations(
            @NonNull final Context context,
            final SyncEntry entry,
            final boolean syncStartShowToast,
            @Nullable final String message,
            final int resultCode) {

        // to Console.
        Log.i(TAG, message);

        // to Database.
        SyncDbHelper.insertEntry(context, entry);

        // Toast
        if (syncStartShowToast) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    showToast(context, message, Toast.LENGTH_SHORT);
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
