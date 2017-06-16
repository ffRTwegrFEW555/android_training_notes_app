package com.gamaliev.notes.conflict.conflict_select_dialog;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.db.DbQueryBuilder;
import com.gamaliev.notes.common.network.NetworkUtils;
import com.gamaliev.notes.common.rest.NoteApi;
import com.gamaliev.notes.common.rest.NoteApiUtils;
import com.gamaliev.notes.common.shared_prefs.SpUsers;
import com.gamaliev.notes.entity.ListEntry;
import com.gamaliev.notes.sync.utils.SyncUtils;

import org.json.JSONObject;

import java.util.Map;

import retrofit2.Response;

import static com.gamaliev.notes.app.NotesApp.getAppContext;
import static com.gamaliev.notes.common.CommonUtils.getMainHandler;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_CONFLICTED_SUCCESS;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_SYNC_SUCCESS;
import static com.gamaliev.notes.common.db.DbHelper.COMMON_COLUMN_SYNC_ID;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_TABLE_NAME;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_CONFLICT_TABLE_NAME;
import static com.gamaliev.notes.common.db.DbHelper.deleteEntryWithSingle;
import static com.gamaliev.notes.common.db.DbHelper.getDbFailMessage;
import static com.gamaliev.notes.common.db.DbHelper.getEntries;
import static com.gamaliev.notes.common.db.DbHelper.getWritableDb;
import static com.gamaliev.notes.common.observers.ObserverHelper.CONFLICT;
import static com.gamaliev.notes.common.observers.ObserverHelper.notifyObservers;
import static com.gamaliev.notes.common.rest.NoteApiUtils.API_KEY_DATA;
import static com.gamaliev.notes.common.rest.NoteApiUtils.API_KEY_EXTRA;
import static com.gamaliev.notes.common.rest.NoteApiUtils.API_KEY_ID;
import static com.gamaliev.notes.common.rest.NoteApiUtils.API_KEY_STATUS;
import static com.gamaliev.notes.common.rest.NoteApiUtils.API_STATUS_OK;
import static com.gamaliev.notes.common.shared_prefs.SpCommon.convertEntryJsonToString;
import static com.gamaliev.notes.common.shared_prefs.SpCommon.convertJsonToMap;
import static com.gamaliev.notes.common.shared_prefs.SpCommon.convertMapToJson;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.getApiUrlForCurrentUser;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.getSyncIdForCurrentUser;
import static com.gamaliev.notes.conflict.ConflictFragment.EXTRA_CONFLICT_SELECT_POSITION;
import static com.gamaliev.notes.conflict.utils.ConflictUtils.checkConflictExistsAndHideStatusBarNotification;
import static com.gamaliev.notes.list.db.ListDbHelper.insertUpdateEntry;
import static com.gamaliev.notes.sync.utils.SyncUtils.ACTION_UPDATED_ON_LOCAL;
import static com.gamaliev.notes.sync.utils.SyncUtils.ACTION_UPDATED_ON_SERVER;
import static com.gamaliev.notes.sync.utils.SyncUtils.STATUS_OK;
import static com.gamaliev.notes.sync.utils.SyncUtils.addToSyncJournalAndLogAndNotify;

/**
 * @author Vadim Gamaliev <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
class ConflictSelectDialogPresenter implements ConflictSelectDialogContract.Presenter {

    /* Logger */
    @NonNull private static final String TAG = ConflictSelectDialogPresenter.class.getSimpleName();

    /* ... */
    @NonNull private final Context mContext;
    @NonNull private final ConflictSelectDialogContract.View mConflictSelectDialogView;
    @NonNull private final String mSyncId;
    private final int mPosition;
    private boolean mServerEntryLoaded;
    private boolean mLocalEntryLoaded;


    /*
        Init
     */

    ConflictSelectDialogPresenter(
            @NonNull final ConflictSelectDialogContract.View conflictSelectDialogView,
            @NonNull final String syncId,
            final int position) {

        mContext = getAppContext();
        mConflictSelectDialogView = conflictSelectDialogView;
        mSyncId = syncId;
        mPosition = position;

        mConflictSelectDialogView.setPresenter(this);
    }


    /*
        ConflictSelectDialogContract.Presenter
     */

    @Override
    public void start() {
        initServerLayoutAsync();
        initLocalLayoutAsync();
    }


    /*
        ...
     */

    private void initServerLayoutAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                initServerLayout();
            }
        }).start();
    }

    private void initServerLayout() {
        final int networkResult = NetworkUtils.checkNetwork(mContext);
        switch (networkResult) {
            case NetworkUtils.NETWORK_MOBILE:
                if (SpUsers.getSyncWifiOnlyForCurrentUser(mContext)) {
                    mConflictSelectDialogView.performError(
                            mContext.getString(R.string.fragment_dialog_conflict_select_connection_only_wifi));
                    return;
                }
                break;
            case NetworkUtils.NETWORK_WIFI:
                break;
            case NetworkUtils.NETWORK_NO:
            default:
                mConflictSelectDialogView.performError(
                        mContext.getString(R.string.fragment_dialog_conflict_select_connection_no_internet));
                return;
        }

        // Get entry from server.
        try {
            final NoteApi noteApi = NoteApiUtils.newInstance(getApiUrlForCurrentUser(mContext));
            if (noteApi == null) {
                throw new Exception("Cannot get note api.");
            }
            final Response<String> response = noteApi
                    .get(getSyncIdForCurrentUser(mContext), mSyncId)
                    .execute();

            if (response.isSuccessful()) {
                final JSONObject jsonResponse = new JSONObject(response.body());
                final String status = jsonResponse.optString(API_KEY_STATUS);

                if (API_STATUS_OK.equals(status)) {
                    final String data = jsonResponse.getString(API_KEY_DATA);

                    if (!TextUtils.isEmpty(data)) {
                        final Map<String, String> mapServer = convertJsonToMap(data);
                        if (mapServer == null) {
                            throw new Exception("Cannot convert json to map");
                        }
                        mapServer.remove(API_KEY_ID);
                        mapServer.remove(API_KEY_EXTRA);

                        // Srv header and body text.
                        final String srvBodyText =
                                convertEntryJsonToString(mContext, convertMapToJson(mapServer));
                        if (srvBodyText == null) {
                            throw new Exception("Cannot convert map to result string.");
                        }
                        getMainHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                if (mConflictSelectDialogView.isActive()) {
                                    mConflictSelectDialogView.setServerHeader(mSyncId);
                                    mConflictSelectDialogView.setServerBody(srvBodyText);
                                }
                            }
                        });

                        // Save btn.
                        mConflictSelectDialogView.setSrvSaveBtnOnClickListener(
                                getSrvBtnSaveOnClickListener(data));
                        mServerEntryLoaded = true;
                        tryEnableButtons();

                    } else {
                        // If entry not exists.
                        Log.e(TAG, response.toString());
                        mConflictSelectDialogView.performError(
                                mContext.getString(
                                        R.string.fragment_dialog_conflict_select_connection_server_entry_not_found));
                    }
                } else {
                    // If status not ok.
                    Log.e(TAG, response.toString());
                    mConflictSelectDialogView.performError(
                            mContext.getString(
                                    R.string.fragment_dialog_conflict_select_connection_server_request_error));
                }
            } else {
                // If response not ok.
                Log.e(TAG, response.toString());
                mConflictSelectDialogView.performError(
                        mContext.getString(R.string.fragment_dialog_conflict_select_connection_server_error));
            }

        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    private void initLocalLayoutAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                initLocalLayout();
            }
        }).start();
    }

    private void initLocalLayout() {
        final String localDbError =
                mContext.getString(R.string.fragment_dialog_conflict_select_local_database_error);

        // Get entry from database.
        final DbQueryBuilder queryBuilder = new DbQueryBuilder();
        queryBuilder.addOr(
                COMMON_COLUMN_SYNC_ID,
                DbQueryBuilder.OPERATOR_EQUALS,
                new String[]{mSyncId});

        try (Cursor entryCursor = getEntries(
                mContext,
                LIST_ITEMS_TABLE_NAME,
                queryBuilder)) {

            if (entryCursor == null || !entryCursor.moveToFirst()) {
                Log.e(TAG, localDbError);
                mConflictSelectDialogView.performError(
                        mContext.getString(R.string.fragment_dialog_conflict_select_local_database_error));
                return;
            }

            final JSONObject jsonObject = ListEntry.getJsonObjectFromCursor(mContext, entryCursor);
            if (jsonObject == null) {
                Log.e(TAG, localDbError);
                mConflictSelectDialogView.performError(
                        mContext.getString(R.string.fragment_dialog_conflict_select_local_database_error));
                return;
            }

            // Local header and body text.
            final String localBodyText = convertEntryJsonToString(mContext, jsonObject.toString());
            if (localBodyText == null) {
                mConflictSelectDialogView.performError(
                        mContext.getString(R.string.fragment_dialog_conflict_select_local_database_error));
                return;
            }
            getMainHandler().post(new Runnable() {
                @Override
                public void run() {
                    if (mConflictSelectDialogView.isActive()) {
                        mConflictSelectDialogView.setLocalHeader(mSyncId);
                        mConflictSelectDialogView.setLocalBody(localBodyText);
                    }
                }
            });

            // Save btn.
            mConflictSelectDialogView.setLocalSaveBtnOnClickListener(
                    getLocalBtnSaveOnClickListener(jsonObject.toString()));
            mLocalEntryLoaded = true;
            tryEnableButtons();
        }
    }


    /*
        Buttons listeners.
     */

    @NonNull
    private View.OnClickListener getSrvBtnSaveOnClickListener(
            @NonNull final String data) {

        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SyncUtils.getSingleThreadExecutor().submit(new Runnable() {
                    @Override
                    public void run() {
                        saveServerEntryToLocal(data);
                    }
                });
                mConflictSelectDialogView.dismiss();
            }
        };
    }

    @NonNull
    private View.OnClickListener getLocalBtnSaveOnClickListener(
            @NonNull final String jsonEntry) {

        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SyncUtils.getSingleThreadExecutor().submit(new Runnable() {
                    @Override
                    public void run() {
                        saveLocalEntryToServer(jsonEntry);
                    }
                });
                mConflictSelectDialogView.dismiss();
            }
        };
    }


    /*
        Sync operations.
     */

    private void saveServerEntryToLocal(@NonNull final String data) {
        try {
            final ListEntry entry = ListEntry.convertJsonToListEntry(mContext, data);
            if (entry == null) {
                throw new Exception("Entry is null.");
            }
            entry.setSyncId(Long.valueOf(mSyncId));

            final SQLiteDatabase db = getWritableDb(mContext);
            if (db == null) {
                throw new SQLiteException(getDbFailMessage());
            }

            db.beginTransaction();
            try {
                insertUpdateEntry(
                        mContext,
                        entry,
                        db,
                        true);

                final boolean result = deleteEntryWithSingle(
                        mContext,
                        db,
                        SYNC_CONFLICT_TABLE_NAME,
                        COMMON_COLUMN_SYNC_ID,
                        mSyncId,
                        true);

                if (!result) {
                    throw new SQLiteException(
                            "Delete entry from conflict table is failed.");
                }

                db.setTransactionSuccessful();

                addToSyncJournalAndLogAndNotify(
                        mContext,
                        ACTION_UPDATED_ON_LOCAL,
                        STATUS_OK,
                        1,
                        RESULT_CODE_SYNC_SUCCESS,
                        true);

                makeFinishOperations();

            } finally {
                db.endTransaction();
            }

        } catch (Exception e) {
            Log.e(TAG, e.toString());
            mConflictSelectDialogView.performError(
                    mContext.getString(R.string.fragment_dialog_conflict_resolution_failed));
        }
    }

    private void saveLocalEntryToServer(@NonNull final String jsonEntry) {
        try {
            final NoteApi noteApi = NoteApiUtils.newInstance(getApiUrlForCurrentUser(mContext));
            if (noteApi == null) {
                throw new Exception("Cannot get note api.");
            }
            final Response<String> response = noteApi
                    .update(getSyncIdForCurrentUser(mContext),
                            mSyncId,
                            jsonEntry)
                    .execute();

            if (response.isSuccessful()) {
                final JSONObject jsonResponse = new JSONObject(response.body());
                final String status = jsonResponse.optString(API_KEY_STATUS);

                if (API_STATUS_OK.equals(status)) {
                    final boolean result = deleteEntryWithSingle(
                            mContext,
                            null,
                            SYNC_CONFLICT_TABLE_NAME,
                            COMMON_COLUMN_SYNC_ID,
                            mSyncId,
                            true);

                    if (!result) {
                        throw new SQLiteException(
                                "[ERROR] Delete entry from conflict table is failed.");
                    }

                    addToSyncJournalAndLogAndNotify(
                            mContext,
                            ACTION_UPDATED_ON_SERVER,
                            STATUS_OK,
                            1,
                            RESULT_CODE_SYNC_SUCCESS,
                            true);

                    makeFinishOperations();
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.toString());
            mConflictSelectDialogView.performError(
                    mContext.getString(R.string.fragment_dialog_conflict_resolution_failed));
        }
    }

    private void tryEnableButtons() {
        if (mServerEntryLoaded && mLocalEntryLoaded) {
            getMainHandler().post(new Runnable() {
                @Override
                public void run() {
                    if (mConflictSelectDialogView.isActive()) {
                        mConflictSelectDialogView.enableSaveButtons();
                    }
                }
            });
        }
    }


    /*
        Finish
     */

    private void makeFinishOperations() {
        final Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_CONFLICT_SELECT_POSITION, mPosition);
        notifyObservers(CONFLICT, RESULT_CODE_CONFLICTED_SUCCESS, bundle);

        checkConflictExistsAndHideStatusBarNotification(mContext);
    }
}
