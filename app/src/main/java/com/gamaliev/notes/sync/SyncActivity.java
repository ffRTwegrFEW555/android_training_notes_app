package com.gamaliev.notes.sync;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.observers.Observer;
import com.gamaliev.notes.conflict.ConflictActivity;
import com.gamaliev.notes.sync.db.SyncCursorAdapter;
import com.gamaliev.notes.sync.db.SyncDbHelper;

import static com.gamaliev.notes.common.CommonUtils.showToast;
import static com.gamaliev.notes.common.codes.RequestCode.REQUEST_CODE_CONFLICTING;
import static com.gamaliev.notes.common.observers.ObserverHelper.SYNC;
import static com.gamaliev.notes.common.observers.ObserverHelper.registerObserver;
import static com.gamaliev.notes.common.observers.ObserverHelper.unregisterObserver;
import static com.gamaliev.notes.conflict.ConflictActivity.hideConflictStatusBarNotification;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class SyncActivity extends AppCompatActivity implements Observer {

    /* Logger */
    private static final String TAG = SyncActivity.class.getSimpleName();

    /* Observed */
    @NonNull
    public static final String[] OBSERVED = {SYNC};

    /* ... */
    @NonNull private CursorAdapter mAdapter;
    @NonNull private ListView mListView;


    /*
        Lifecycle
     */

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);
        init();
    }

    @Override
    public void onResume() {
        notifyDataSetChangedAndScrollToEnd();
        registerObserver(
                OBSERVED,
                toString(),
                this);
        super.onResume();
    }

    @Override
    public void onPause() {
        unregisterObserver(
                OBSERVED,
                toString());
        super.onPause();
    }


    /*
        Init
     */

    private void init() {
        initToolbar();
        initConflictWarning();
        initListView();
    }

    private void initToolbar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.activity_sync_toolbar);
        setSupportActionBar(toolbar);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void initConflictWarning() {
        final boolean result = ConflictActivity.checkConflictingExists(getApplicationContext());
        final View view = findViewById(R.id.activity_sync_conflicting_exists_notification_fl);
        if (result) {
            view.setVisibility(View.VISIBLE);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ConflictActivity.startIntent(
                            SyncActivity.this,
                            REQUEST_CODE_CONFLICTING);
                }
            });
        } else {
            view.setVisibility(View.GONE);
            view.setOnClickListener(null);
            hideConflictStatusBarNotification(getApplicationContext());
        }
    }

    private void initListView() {
        mAdapter = new SyncCursorAdapter(
                getApplicationContext(),
                SyncDbHelper.getAll(getApplicationContext()),
                0);

        mListView = (ListView) findViewById(R.id.activity_sync_list_view);
        mListView.setAdapter(mAdapter);
        scrollListViewToBottom();
    }


    /*
        Options menu
     */

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sync, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sync_synchronize:
                SyncUtils.synchronize(getApplicationContext());
                break;

            case R.id.menu_sync_show_conflicting:
                ConflictActivity.startIntent(
                        this,
                        REQUEST_CODE_CONFLICTING);
                break;

            case R.id.menu_sync_delete_all_from_server:
                showConfirmDeleteAllFromServerDialog();
                break;

            case R.id.menu_sync_clear_journal:
                showConfirmClearJournalDialog();
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    /*
        Intents
     */

    public static void startIntent(
            @NonNull final Context context,
            final int requestCode) {

        Intent starter = new Intent(context, SyncActivity.class);
        ((Activity) context).startActivityForResult(starter, requestCode);
    }


    /*
        Callback
     */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_CANCELED) {
            if (requestCode == REQUEST_CODE_CONFLICTING) {
                notifyDataSetChangedAndScrollToEnd();
            }
        }
    }


    /*
        ...
     */

    private void notifyDataSetChangedAndScrollToEnd() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                initConflictWarning();
                mAdapter.changeCursor(SyncDbHelper.getAll(getApplicationContext()));
                mAdapter.notifyDataSetChanged();
                scrollListViewToBottom();
            }
        });
    }

    private void scrollListViewToBottom() {
        mListView.post(new Runnable() {
            @Override
            public void run() {
                mListView.setSelection(mAdapter.getCount() - 1);
            }
        });
    }


    /*
        Confirm operations.
     */

    private void showConfirmDeleteAllFromServerDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(SyncActivity.this);
        builder .setTitle(getString(R.string.activity_sync_dialog_confirm_delete_all_from_server_title))
                .setMessage(getString(R.string.activity_sync_dialog_confirm_delete_all_from_server_body))
                .setPositiveButton(getString(R.string.activity_sync_dialog_confirm_delete_all_from_server_btn_delete),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                SyncUtils.deleteAllFromServerAsync(getApplicationContext());
                            }
                        })
                .setNegativeButton(
                        getString(R.string.activity_sync_dialog_confirm_delete_all_from_server_btn_cancel),
                        null);

        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void showConfirmClearJournalDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(SyncActivity.this);
        builder .setTitle(getString(R.string.activity_sync_dialog_confirm_clear_journal_title))
                .setMessage(getString(R.string.activity_sync_dialog_confirm_clear_journal_body))
                .setPositiveButton(getString(R.string.activity_sync_dialog_confirm_clear_journal_btn_clear),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                if (SyncDbHelper.clear(getApplicationContext())) {
                                    showToast(
                                            getApplicationContext(),
                                            getString(R.string.menu_sync_action_clear_journal_success),
                                            Toast.LENGTH_SHORT);
                                } else {
                                    showToast(
                                            getApplicationContext(),
                                            getString(R.string.menu_sync_action_clear_journal_failed),
                                            Toast.LENGTH_SHORT);
                                }
                                notifyDataSetChangedAndScrollToEnd();
                            }
                        })
                .setNegativeButton(
                        getString(R.string.activity_sync_dialog_confirm_clear_journal_btn_cancel),
                        null);

        final AlertDialog alert = builder.create();
        alert.show();
    }


    /*
        Observer
     */

    @Override
    public void onNotify(
            final int resultCode,
            @Nullable final Bundle data) {

        notifyDataSetChangedAndScrollToEnd();
    }
}