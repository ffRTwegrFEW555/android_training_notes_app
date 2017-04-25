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
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FilterQueryProvider;
import android.widget.ListView;

import com.gamaliev.list.R;
import com.gamaliev.list.common.DatabaseHelper;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.gamaliev.list.common.CommonUtils.circularRevealEffectOff;
import static com.gamaliev.list.common.CommonUtils.circularRevealEffectOn;

public class ListActivity extends AppCompatActivity {

    /* Log */
    private static final String TAG = ListActivity.class.getSimpleName();


    /* Intents */
    private static final int REQUEST_CODE_ADD = 1;
    private static final int REQUEST_CODE_EDIT = 2;


    /* SQLite */
    @NonNull private static final String[] searchColumns = {
            DatabaseHelper.LIST_ITEMS_COLUMN_TITLE,
            DatabaseHelper.LIST_ITEMS_COLUMN_DESCRIPTION};

    @NonNull private String selectionOrder = ListDatabaseHelper.ORDER_DEFAULT;
    @NonNull private ListDatabaseHelper dbHelper;
    @NonNull private ListCursorAdapter adapter;
    @NonNull private FilterQueryProvider fqp;

    /**
     * Ascending - true, Descending - false.
     */
    private boolean sortingType = true;


    /* Other */
    @NonNull private ListView listView;
    @NonNull private Button foundView;
    private long timerFound;


    /*
        Init
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        init();
    }

    private void init() {
        fqp = getFilterQueryProvider();
        foundView = (Button) findViewById(R.id.activity_list_button_found);

        setFabOnClickListener();
        refreshDatabaseAndView();
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
        setupSearchView(menu);
        return super.onCreateOptionsMenu(menu);
    }


    /**
     * Setting SearchView listener.<br>
     * Filtering and updating list on text change.<br>
     * Also there is a notification about the number of positions found.
     * @param menu menu of activity.
     */
    private void setupSearchView(Menu menu) {
        final SearchView searchView =
                (SearchView) menu.findItem(R.id.menu_list_search).getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                showFoundNotification();
                return true;
            }
        });
    }

    /**
     * Action bar menu item selection handler
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            // Fill example entries
            case R.id.menu_list_action_fill_mock:
                dbHelper.addMockEntries();
                refreshDatabaseAndView();
                break;

            // Remove all entries
            case R.id.menu_list_action_delete_entry:
                dbHelper.removeAllEntries();
                refreshDatabaseAndView();
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
    private void refreshDatabaseAndView() {
        if (dbHelper == null) {
            dbHelper = new ListDatabaseHelper(this);
        }
        Cursor cursor   = dbHelper.getAllEntries(null, null, null, true);
        adapter         = new ListCursorAdapter(this, cursor, 0);
        listView        = (ListView) findViewById(R.id.activity_list_listview);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // Shared transition color box
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    View iconView = view.findViewById(R.id.activity_list_item_color);
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

        // SearchView filter handler
        adapter.setFilterQueryProvider(fqp);
    }


    /*
        Intents
     */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_ADD) {
                // If new position added, then update list and scroll to new position (to end).
                // TODO: if sort by name?
                refreshDatabaseAndView();
                timerDelayRunForScroll(listView, adapter.getCount(), 200);
            } else if (requestCode == REQUEST_CODE_EDIT) {
                refreshDatabaseAndView();
            }
        }
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


    /*
        Methods
     */

    /**
     * Start {@link com.gamaliev.list.list.ItemDetailsActivity} activity for result,
     * with Add new entry action.
     */
    private void setFabOnClickListener() {
        findViewById(R.id.activity_list_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ItemDetailsActivity.startAdd(ListActivity.this, REQUEST_CODE_ADD);
            }
        });
    }

    // TODO: Handle;
    @NonNull
    private FilterQueryProvider getFilterQueryProvider() {
        return new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                return dbHelper.getAllEntries(
                        constraint.toString(),
                        searchColumns,
                        selectionOrder,
                        sortingType);
            }
        };
    }


    /*
        Found notification
     */

    // TODO: Handle
    private void showFoundNotification() {
        final Handler handler = new Handler();
        handler.postDelayed(getRunnableForFoundNotification(), 500); // TODO: constant
    }

    @NonNull
    private Runnable getRunnableForFoundNotification() {
        return new Runnable() {
            @Override
            public void run() {
                foundView.setText(String.format(
                        Locale.ENGLISH,
                        "FOUND:\n%d", listView.getCount())); // TODO: constant

                timerFound = System.currentTimeMillis();

                if (foundView.getVisibility() == View.INVISIBLE) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        circularRevealEffectOn(foundView);
                    } else {
                        foundView.setVisibility(View.VISIBLE);
                    }
                    final Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (System.currentTimeMillis() - timerFound > 3000) { // TODO: constant
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            circularRevealEffectOff(foundView);
                                        } else {
                                            foundView.setVisibility(View.INVISIBLE);
                                        }
                                    }
                                });
                                timer.cancel();
                            }
                        }
                    }, 1000, 1000); // TODO: constant
                }
            }
        };
    }
}
