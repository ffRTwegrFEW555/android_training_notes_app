package com.gamaliev.notes.conflict;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.db.DbQueryBuilder;
import com.gamaliev.notes.common.network.NetworkUtils;
import com.gamaliev.notes.common.shared_prefs.SpUsers;
import com.gamaliev.notes.model.ListEntry;
import com.gamaliev.notes.rest.NoteApi;
import com.gamaliev.notes.sync.SyncUtils;

import org.json.JSONObject;

import java.util.Map;

import retrofit2.Response;

import static com.gamaliev.notes.common.CommonUtils.EXTRA_REVEAL_ANIM_CENTER_CENTER;
import static com.gamaliev.notes.common.CommonUtils.showToastRunOnUiThread;
import static com.gamaliev.notes.common.DialogFragmentUtils.initCircularRevealAnimation;
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
import static com.gamaliev.notes.common.shared_prefs.SpCommon.convertEntryJsonToString;
import static com.gamaliev.notes.common.shared_prefs.SpCommon.convertJsonToMap;
import static com.gamaliev.notes.common.shared_prefs.SpCommon.convertMapToJson;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.getSyncIdForCurrentUser;
import static com.gamaliev.notes.conflict.ConflictFragment.EXTRA_CONFLICT_SELECT_POSITION;
import static com.gamaliev.notes.conflict.ConflictUtils.checkConflictExistsAndHideStatusBarNotification;
import static com.gamaliev.notes.list.db.ListDbHelper.insertUpdateEntry;
import static com.gamaliev.notes.rest.NoteApiUtils.API_KEY_DATA;
import static com.gamaliev.notes.rest.NoteApiUtils.API_KEY_EXTRA;
import static com.gamaliev.notes.rest.NoteApiUtils.API_KEY_ID;
import static com.gamaliev.notes.rest.NoteApiUtils.API_KEY_STATUS;
import static com.gamaliev.notes.rest.NoteApiUtils.API_STATUS_OK;
import static com.gamaliev.notes.rest.NoteApiUtils.getNoteApi;
import static com.gamaliev.notes.sync.SyncUtils.ACTION_UPDATED_ON_LOCAL;
import static com.gamaliev.notes.sync.SyncUtils.ACTION_UPDATED_ON_SERVER;
import static com.gamaliev.notes.sync.SyncUtils.STATUS_OK;
import static com.gamaliev.notes.sync.SyncUtils.addToSyncJournalAndLogAndNotify;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

@SuppressWarnings("NullableProblems")
public class ConflictSelectDialogFragment extends DialogFragment {

    /* Logger */
    @NonNull private static final String TAG = ConflictSelectDialogFragment.class.getSimpleName();

    /* ... */
    private static final String EXTRA_SYNC_ID = "syncId";
    private static final String EXTRA_POSITION = "position";

    @NonNull private View mDialog;
    @NonNull private Button mServerSaveBtn;
    @NonNull private Button mLocalSaveBtn;
    @NonNull private String mSyncId;
    private int mPosition;
    private boolean mServerEntryLoaded;
    private boolean mLocalEntryLoaded;


    /*
        Init
     */

    /**
     * Get new instance of conflict select dialog fragment.
     * @param syncId    Synchronization id of Entry.
     * @param position  Position of entry, in adapter of recycler view.
     * @return New instance of conflict select dialog fragment.
     */
    @NonNull
    public static ConflictSelectDialogFragment newInstance(
            @NonNull final String syncId,
            final int position) {

        final Bundle args = new Bundle();
        args.putString(EXTRA_SYNC_ID, syncId);
        args.putInt(EXTRA_POSITION, position);

        final ConflictSelectDialogFragment fragment = new ConflictSelectDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }


    /*
        Lifecycle
     */

    @Nullable
    @Override
    public View onCreateView(
            final LayoutInflater inflater,
            @Nullable final ViewGroup container,
            final Bundle savedInstanceState) {

        mDialog = inflater.inflate(R.layout.fragment_dialog_conflict_select, container);
        disableTitle();
        return mDialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
    }


    /*
        ...
     */

    private void init() {
        initDialogSize();
        initCircularAnimation();
        initArgs();
        initSaveButtons();
        initServerLayoutAsync();
        initLocalLayoutAsync();
    }

    private void disableTitle() {
        // Disable title for more space.
        final Window window = getDialog().getWindow();
        if (window != null) {
            window.requestFeature(Window.FEATURE_NO_TITLE);
        }
    }

    private void initDialogSize() {
        // Set max size of dialog. ( XML is not work :/ )
        final Window window = getDialog().getWindow();
        if (window != null) {
            final DisplayMetrics displayMetrics = getActivity().getResources().getDisplayMetrics();
            final ViewGroup.LayoutParams params = window.getAttributes();
            params.width = Math.min(
                    displayMetrics.widthPixels,
                    getActivity().getResources().getDimensionPixelSize(
                            R.dimen.fragment_dialog_conflict_select_max_width));
            params.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
            window.setAttributes((android.view.WindowManager.LayoutParams) params);
        }
    }

    private void initCircularAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            initCircularRevealAnimation(
                    mDialog,
                    true,
                    EXTRA_REVEAL_ANIM_CENTER_CENTER);
        }
    }

    private void initArgs() {
        final String syncId = getArguments().getString(EXTRA_SYNC_ID);
        if (syncId == null) {
            final String error = getString(R.string.fragment_dialog_conflict_sync_id_is_null);
            Log.e(TAG, error);
            showToastRunOnUiThread(getContext(), error, Toast.LENGTH_SHORT);
            dismiss();
            return;
        }
        mSyncId = syncId;
        mPosition = getArguments().getInt(EXTRA_POSITION);
    }

    private void initSaveButtons() {
        mServerSaveBtn = (Button) mDialog
                .findViewById(R.id.fragment_dialog_conflict_select_server_save_btn);
        mLocalSaveBtn = (Button) mDialog
                .findViewById(R.id.fragment_dialog_conflict_select_local_save_btn);
    }

    private void initServerLayoutAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                initServerLayout();
            }
        }).start();
    }

    private void initServerLayout() {
        final int networkResult = NetworkUtils.checkNetwork(getContext());
        switch (networkResult) {
            case NetworkUtils.NETWORK_MOBILE:
                if (SpUsers.getSyncWifiOnlyForCurrentUser(getContext())) {
                    showToastRunOnUiThread(
                            getContext(),
                            getString(R.string.fragment_dialog_conflict_select_connection_only_wifi),
                            Toast.LENGTH_LONG);
                    dismiss();
                    return;
                }
                break;
            case NetworkUtils.NETWORK_WIFI:
                break;
            case NetworkUtils.NETWORK_NO:
            default:
                showToastRunOnUiThread(
                        getContext(),
                        getString(R.string.fragment_dialog_conflict_select_connection_no_internet),
                        Toast.LENGTH_LONG);
                dismiss();
                return;
        }

        // Get entry from server.
        try {
            final NoteApi noteApi = getNoteApi();
            if (noteApi == null) {
                throw new Exception("Cannot get note api.");
            }
            final Response<String> response = noteApi
                    .get(getSyncIdForCurrentUser(getContext()), mSyncId)
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

                        // Header and body text.
                        final TextView serverHeaderTv = (TextView) mDialog
                                .findViewById(R.id.fragment_dialog_conflict_select_server_header_tv);
                        final TextView serverBodyTv = (TextView) mDialog
                                .findViewById(R.id.fragment_dialog_conflict_select_server_body_tv);

                        final String textServer =
                                convertEntryJsonToString(
                                        getContext(),
                                        convertMapToJson(mapServer));

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //noinspection SetTextI18n
                                serverHeaderTv.setText(
                                        getString(R.string.fragment_dialog_conflict_select_server_header_prefix)
                                                + ": "
                                                + mSyncId);
                                serverBodyTv.setText(textServer);
                            }
                        });

                        // Save button.
                        mServerSaveBtn.setOnClickListener(
                                getSrvBtnSaveOnClickListener(getActivity(), data));
                        mServerEntryLoaded = true;
                        tryEnableButtons();
                        return;

                    } else {
                        // If entry not exists.
                        Log.e(TAG, response.toString());
                        showToastRunOnUiThread(
                                getContext(),
                                getString(R.string.fragment_dialog_conflict_select_connection_server_entry_not_found),
                                Toast.LENGTH_LONG);
                    }
                } else {
                    // If status not ok.
                    Log.e(TAG, response.toString());
                    showToastRunOnUiThread(
                            getContext(),
                            getString(R.string.fragment_dialog_conflict_select_connection_server_error_request),
                            Toast.LENGTH_LONG);
                }
            } else {
                // If response not ok.
                Log.e(TAG, response.toString());
                showToastRunOnUiThread(
                        getContext(),
                        getString(R.string.fragment_dialog_conflict_select_connection_server_error),
                        Toast.LENGTH_LONG);
            }

        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        dismiss();
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
        // Get entry from database.
        final DbQueryBuilder queryBuilder = new DbQueryBuilder();
        queryBuilder.addOr(
                COMMON_COLUMN_SYNC_ID,
                DbQueryBuilder.OPERATOR_EQUALS,
                new String[]{mSyncId});

        try (Cursor entryCursor = getEntries(
                getContext(),
                LIST_ITEMS_TABLE_NAME,
                queryBuilder)) {

            if (entryCursor == null || !entryCursor.moveToFirst()) {
                showToastRunOnUiThread(
                        getContext(),
                        getString(R.string.fragment_dialog_conflict_select_local_database_error),
                        Toast.LENGTH_LONG);
                dismiss();
                return;
            }

            final JSONObject jsonObject = ListEntry.getJsonObjectFromCursor(getContext(), entryCursor);
            if (jsonObject == null) {
                showToastRunOnUiThread(
                        getContext(),
                        getString(R.string.fragment_dialog_conflict_select_local_database_error),
                        Toast.LENGTH_LONG);
                dismiss();
                return;
            }
            final String textLocal = convertEntryJsonToString(getContext(), jsonObject.toString());

            // Header and body text.
            final TextView localHeaderTv = (TextView) mDialog
                    .findViewById(R.id.fragment_dialog_conflict_select_local_header_tv);
            final TextView localBodyTv = (TextView) mDialog
                    .findViewById(R.id.fragment_dialog_conflict_select_local_body_tv);

            //noinspection SetTextI18n
            localHeaderTv.setText(
                    getString(R.string.fragment_dialog_conflict_select_local_header_prefix)
                            + ": "
                            + mSyncId);
            localBodyTv.setText(textLocal);

            // Save button.
            mLocalSaveBtn.setOnClickListener(
                    getLocalBtnSaveOnClickListener(getActivity(), jsonObject.toString()));
            mLocalEntryLoaded = true;
            tryEnableButtons();
        }
    }


    /*
        Buttons listeners.
     */

    @NonNull
    private View.OnClickListener getSrvBtnSaveOnClickListener(
            @NonNull final Activity activity,
            @NonNull final String data) {

        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SyncUtils.getSingleThreadExecutor().submit(new Runnable() {
                    @Override
                    public void run() {
                        saveServerEntryToLocal(activity, data);
                    }
                });
                dismiss();
            }
        };
    }

    @NonNull
    private View.OnClickListener getLocalBtnSaveOnClickListener(
            @NonNull final Activity activity,
            @NonNull final String jsonEntry) {

        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SyncUtils.getSingleThreadExecutor().submit(new Runnable() {
                    @Override
                    public void run() {
                        saveLocalEntryToServer(activity, jsonEntry);
                    }
                });
                dismiss();
            }
        };
    }


    /*
        Sync operations.
     */

    private void saveServerEntryToLocal(
            @NonNull final Activity activity,
            @NonNull final String data) {

        final Context context = activity.getApplicationContext();

        try {
            final ListEntry entry = ListEntry
                    .convertJsonToListEntry(context, data);
            if (entry == null) {
                throw new Exception("Entry is null.");
            }
            entry.setSyncId(Long.valueOf(mSyncId));

            final SQLiteDatabase db = getWritableDb(context);
            if (db == null) {
                throw new SQLiteException(getDbFailMessage());
            }

            db.beginTransaction();
            try {
                insertUpdateEntry(
                        context,
                        entry,
                        db,
                        true);

                final boolean result = deleteEntryWithSingle(
                        context,
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
                        context,
                        ACTION_UPDATED_ON_LOCAL,
                        STATUS_OK,
                        1,
                        RESULT_CODE_SYNC_SUCCESS,
                        true);

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        makeFinishOperations(activity);
                    }
                });

            } finally {
                db.endTransaction();
            }

        } catch (Exception e) {
            Log.e(TAG, e.toString());
            showToastRunOnUiThread(
                    context,
                    getString(R.string.fragment_dialog_conflict_resolution_failed),
                    Toast.LENGTH_LONG);
        }
    }

    private void saveLocalEntryToServer(
            @NonNull final Activity activity,
            @NonNull final String jsonEntry) {

        final Context context = activity.getApplicationContext();

        try {
            final NoteApi noteApi = getNoteApi();
            if (noteApi == null) {
                throw new Exception("Cannot get note api.");
            }
            final Response<String> response = noteApi
                    .update(getSyncIdForCurrentUser(context),
                            mSyncId,
                            jsonEntry)
                    .execute();

            if (response.isSuccessful()) {
                final JSONObject jsonResponse = new JSONObject(response.body());
                final String status = jsonResponse.optString(API_KEY_STATUS);

                if (API_STATUS_OK.equals(status)) {
                    final boolean result = deleteEntryWithSingle(
                            context,
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
                            context,
                            ACTION_UPDATED_ON_SERVER,
                            STATUS_OK,
                            1,
                            RESULT_CODE_SYNC_SUCCESS,
                            true);

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            makeFinishOperations(activity);
                        }
                    });
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    private void tryEnableButtons() {
        if (mServerEntryLoaded && mLocalEntryLoaded) {
            if (mDialog.isAttachedToWindow()) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mServerSaveBtn.setEnabled(true);
                        mLocalSaveBtn.setEnabled(true);
                    }
                });
            }
        }
    }


    /*
        Finish
     */

    private void makeFinishOperations(@NonNull final Context context) {
        final Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_CONFLICT_SELECT_POSITION, mPosition);
        notifyObservers(CONFLICT, RESULT_CODE_CONFLICTED_SUCCESS, bundle);

        checkConflictExistsAndHideStatusBarNotification(context);
    }
}
