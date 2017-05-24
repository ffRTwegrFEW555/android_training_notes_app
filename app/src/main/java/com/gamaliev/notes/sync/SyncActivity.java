package com.gamaliev.notes.sync;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import com.gamaliev.notes.common.OnCompleteListener;
import com.gamaliev.notes.conflict.ConflictActivity;
import com.gamaliev.notes.sync.db.SyncCursorAdapter;
import com.gamaliev.notes.sync.db.SyncDbHelper;

import static com.gamaliev.notes.common.CommonUtils.showToast;
import static com.gamaliev.notes.conflict.ConflictActivity.hideConflictStatusBarNotification;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class SyncActivity extends AppCompatActivity implements OnCompleteListener {

    /* Logger */
    private static final String TAG = SyncActivity.class.getSimpleName();

    /* ... */
    @NonNull private CursorAdapter mAdapter;
    @NonNull private ListView mListView;

    public static final int REQUEST_CODE_START_CONFLICTING = 0;


    /*
        Lifecycle
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);
        init();
    }

    @Override
    protected void onResume() {
        SyncUtils.addObserver(TAG, this);
        super.onResume();
    }

    @Override
    protected void onPause() {
        SyncUtils.removeObserver(TAG);
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
                            REQUEST_CODE_START_CONFLICTING);
                }
            });
        } else {
            view.setVisibility(View.GONE);
            view.setOnClickListener(null);
            hideConflictStatusBarNotification(getApplicationContext());
        }
    }

    private void initListView() {
        // Create adapter.
        mAdapter = new SyncCursorAdapter(
                getApplicationContext(),
                SyncDbHelper.getAll(getApplicationContext()),
                0);

        // Init list view
        mListView = (ListView) findViewById(R.id.activity_sync_list_view);
        mListView.setAdapter(mAdapter);
        scrollListViewToBottom();
    }


    /*
        Options menu
     */

    /**
     * Inflate action bar menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sync, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Action bar menu item selection handler
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            // Synchronize button
            case R.id.menu_sync_synchronize:
                SyncUtils.synchronize(getApplicationContext());
                break;

            // Show conflicting
            case R.id.menu_sync_show_conflicting:
                ConflictActivity.startIntent(
                        this,
                        REQUEST_CODE_START_CONFLICTING);
                break;

            // Delete all from server button
            case R.id.menu_sync_delete_all_from_server:
                showConfirmDeleteAllFromServerDialog();
                break;

            // Clear journal
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

    /**
     * Start intent.
     * @param context       Context.
     * @param requestCode   This code will be returned in onActivityResult() when the activity exits.
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
    public void onComplete(int code) {
        notifyDataSetChangedAndScrollToEnd();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_CANCELED) {
            if (requestCode == REQUEST_CODE_START_CONFLICTING) {
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
                                //
                                dialog.cancel();
                                //
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
                                //
                                dialog.cancel();
                                //
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
}