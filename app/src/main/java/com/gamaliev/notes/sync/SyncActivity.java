package com.gamaliev.notes.sync;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.OnCompleteListener;
import com.gamaliev.notes.sync.db.SyncCursorAdapter;
import com.gamaliev.notes.sync.db.SyncDbHelper;

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


    /*
        Init
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);
        init();
    }

    private void init() {
        initToolbar();
        initListView();
    }

    private void initToolbar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.activity_sync_toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
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

    private void notifyDataSetChangedAndScrollToEnd() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
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

            // Delete all from server button
            case R.id.menu_sync_delete_all_from_server:
                SyncUtils.deleteAllFromServerAsync(getApplicationContext());
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
        ...
     */

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

    @Override
    public void onComplete(int code) {
        notifyDataSetChangedAndScrollToEnd();
    }
}
