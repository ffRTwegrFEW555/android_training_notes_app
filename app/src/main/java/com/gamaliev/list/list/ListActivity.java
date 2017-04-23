package com.gamaliev.list.list;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.gamaliev.list.R;

public class ListActivity extends AppCompatActivity {

    private static final String TAG = ListActivity.class.getSimpleName();

    @NonNull private ListDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
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
                Intent intent = ItemDetailsActivity.getAddStartIntent(this);
                startActivity(intent);
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


    /*
        On Pause/Resume
     */

    /**
     * Open database helper.. see {@link #refreshDatabase()}
     */
    @Override
    protected void onResume() {
        refreshDatabase();
        super.onResume();
    }

    /**
     * Open a new database helper, get cursor, create and set adapter, set on click listener.<br>
     * See also: {@link com.gamaliev.list.list.ListDatabaseHelper}
     */
    private void refreshDatabase() {
        if (dbHelper == null) {
            dbHelper = new ListDatabaseHelper(this);
        }
        Cursor cursor               = dbHelper.getAllEntries(null, true);
        ListCursorAdapter adapter   = new ListCursorAdapter(this, cursor, 0);
        ListView listView           = (ListView) findViewById(R.id.activity_list_listview);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = ItemDetailsActivity.getEditStartIntent(ListActivity.this, id);
                startActivity(intent);
            }
        });
        timerDelayRunForScroll(listView, adapter.getCount() - 1, 100);
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

    /**
     * Close database helper.<br>
     * See also: {@link com.gamaliev.list.list.ListDatabaseHelper}
     */
    @Override
    protected void onPause() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onPause();
    }
}
