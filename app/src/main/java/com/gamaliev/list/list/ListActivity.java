package com.gamaliev.list.list;

import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
        init();
    }

    private void init() {}


    /*
        Options menu
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return super.onOptionsItemSelected(item);
    }

    /*
        On Pause/Resume
     */

    /**
     * Open a new database helper, get cursor, create and set adapter, set on click listener.<br>
     * See also: {@link com.gamaliev.list.list.ListDatabaseHelper}
     */
    @Override
    protected void onResume() {
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
        super.onResume();
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
