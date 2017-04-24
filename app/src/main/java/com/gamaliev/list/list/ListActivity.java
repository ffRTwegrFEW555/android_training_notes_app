package com.gamaliev.list.list;

import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.gamaliev.list.R;

public class ListActivity extends AppCompatActivity {

    private static final String TAG = ListActivity.class.getSimpleName();
    private static final int REQUEST_CODE_ADD = 1;
    private static final int REQUEST_CODE_EDIT = 2;

    @NonNull private ListDatabaseHelper dbHelper;
    @NonNull private ListCursorAdapter adapter;
    @NonNull private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        init();
    }

    private void init() {
        refreshDatabase();
    }


    /*
        Options menu
     */

    /**
     * Inflate action bar menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_list, menu);
        return super.onCreateOptionsMenu(menu);
    }


    /**
     * Action bar menu item selection handler
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            // Add new entry
            case R.id.menu_list_action_add_entry:
                ItemDetailsActivity.startAdd(this, REQUEST_CODE_ADD);
                break;

            // Fill example entries
            case R.id.menu_list_action_fill_mock:
                dbHelper.addMockEntries();
                refreshDatabase();
                break;

            // Remove all entries
            case R.id.menu_list_action_delete_entry:
                dbHelper.removeAllEntries();
                refreshDatabase();
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Open a new database helper, get cursor, create and set adapter, set on click listener.<br>
     * See also: {@link com.gamaliev.list.list.ListDatabaseHelper}
     */
    private void refreshDatabase() {
        if (dbHelper == null) {
            dbHelper = new ListDatabaseHelper(this);
        }
        Cursor cursor   = dbHelper.getAllEntries(null, true);
        adapter         = new ListCursorAdapter(this, cursor, 0);
        listView        = (ListView) findViewById(R.id.activity_list_listview);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // Shared transition color box
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    View iconView = view.findViewById(R.id.activity_list_item_icon);
                    iconView.setTransitionName(
                            getString(R.string.shared_transition_name_color_box));
                    Pair<View, String> icon = new Pair<>(iconView, iconView.getTransitionName());
                    ActivityOptionsCompat aoc =
                            ActivityOptionsCompat.makeSceneTransitionAnimation(
                                    ListActivity.this, icon);
                    ItemDetailsActivity.startEdit(
                            ListActivity.this, id, REQUEST_CODE_EDIT, aoc.toBundle());

                } else {
                    ItemDetailsActivity.startEdit(
                            ListActivity.this, id, REQUEST_CODE_EDIT, null);
                }
            }
        });
    }

    /**
     * Smooth scroll ListView object to given position, and delay time.
     * @param listView  ListView object.
     * @param position  position to scroll.
     * @param time      delay before start scrolling.
     */
    private void timerDelayRunForScroll(
            @NonNull final ListView listView,
            final int position,
            final long time) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                listView.smoothScrollToPosition(position);
            }
        }, time);
    }


    /*
        Intents
     */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_ADD) {
                refreshDatabase();
                timerDelayRunForScroll(listView, adapter.getCount(), 200);
            } else if (requestCode == REQUEST_CODE_EDIT) {
                refreshDatabase();
            }
        }
    }


    /*
        On Pause/Resume
     */

    /**
     * Close database helper.<br>
     * See also: {@link com.gamaliev.list.list.ListDatabaseHelper}
     */
    @Override
    protected void onPause() {
        dbHelper.close();
        super.onPause();
    }
}
