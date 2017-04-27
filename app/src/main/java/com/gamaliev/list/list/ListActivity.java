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
import com.gamaliev.list.common.DatabaseQueryBuilder;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.gamaliev.list.common.CommonUtils.circularRevealAnimationOff;
import static com.gamaliev.list.common.CommonUtils.circularRevealAnimationOn;
import static com.gamaliev.list.common.DatabaseHelper.BASE_COLUMN_ID;
import static com.gamaliev.list.common.DatabaseHelper.ORDER_ASCENDING;
import static com.gamaliev.list.common.DatabaseQueryBuilder.OPERATOR_LIKE;

public class ListActivity extends AppCompatActivity {

    /* Logger */
    private static final String TAG = ListActivity.class.getSimpleName();

    /* Intents */
    private static final int REQUEST_CODE_ADD = 1;
    private static final int REQUEST_CODE_EDIT = 2;

    /* SQLite */
    @NonNull private static final String[] SEARCH_COLUMNS = {
            DatabaseHelper.LIST_ITEMS_COLUMN_TITLE,
            DatabaseHelper.LIST_ITEMS_COLUMN_DESCRIPTION};

    @NonNull private ListDatabaseHelper dbHelper;
    @NonNull private ListCursorAdapter adapter;
    @NonNull private FilterQueryProvider queryProvider;

    /* */
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
        queryProvider = getFilterQueryProvider();
        foundView = (Button) findViewById(R.id.activity_list_button_found);

        setFabOnClickListener();
        refreshDbConnectAndView();
    }


    /*
        Options menu
     */

    /**
     * Inflate action bar menu and setup search function.
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
     * @param menu Action bar menu of activity.
     */
    private void setupSearchView(@NonNull final Menu menu) {
        final SearchView searchView =
                (SearchView) menu.findItem(R.id.menu_list_search).getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Set filter text and show "Found" notification.
                adapter.getFilter().filter(newText);
                showFoundNotification();
                return true;
            }
        });
    }

    /**
     * Action bar menu item selection handler.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            // Fill example entries
            case R.id.menu_list_action_fill_mock:
                dbHelper.addMockEntries();
                refreshDbConnectAndView();
                break;

            // Remove all entries
            case R.id.menu_list_action_delete_entry:
                dbHelper.removeAllEntries();
                refreshDbConnectAndView();
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Open a new database helper, get cursor, create and set adapter,
     * set on click listener, set filter query provider.<br>
     * See also: {@link com.gamaliev.list.list.ListDatabaseHelper}
     */
    private void refreshDbConnectAndView() {
        if (dbHelper == null) {
            dbHelper = new ListDatabaseHelper(this);
        }
        Cursor cursor   = dbHelper.getEntries(new DatabaseQueryBuilder(this));
        adapter         = new ListCursorAdapter(this, cursor, 0);
        listView        = (ListView) findViewById(R.id.activity_list_listview);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // On click - start item details activity, with edit action.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // If API >= 21, then set shared transition animation.
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

        // SearchView filter query provider.
        adapter.setFilterQueryProvider(queryProvider);
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
                refreshDbConnectAndView();
                timerDelayRunForScroll(
                        listView,
                        adapter.getCount(),
                        getResources().getInteger(R.integer.activity_list_smooth_scroll_delay));

            } else if (requestCode == REQUEST_CODE_EDIT) {
                refreshDbConnectAndView();
            }
        }
    }

    /**
     * Smooth scroll ListView object to given position, and delay time.
     * @param listView  ListView object.
     * @param position  Position to scroll.
     * @param time      Delay before start scrolling.
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
        Found notification
     */


    /**
     * Show found notification.<br>
     * After stopping the input of text, after some times, the notification closes.
     */
    private void showFoundNotification() {
        final Handler handler = new Handler();
        handler.postDelayed(
                getRunnableForFoundNotification(),
                getResources().getInteger(R.integer.activity_list_notification_delay));
    }

    /**
     * @return Runnable task for found notification, contains all logic.
     */
    @NonNull
    private Runnable getRunnableForFoundNotification() {
        return new Runnable() {
            @Override
            public void run() {
                final int delay = getResources()
                        .getInteger(R.integer.activity_list_notification_delay);
                final int delayClose = getResources()
                        .getInteger(R.integer.activity_list_notification_delay_auto_close);

                // Set text.
                foundView.setText(String.format(Locale.ENGLISH,
                        getResources().getString(R.string.activity_list_notification_found_text) + "\n%d",
                        listView.getCount()));

                // Set start time of the notification display.
                timerFound = System.currentTimeMillis();

                if (foundView.getVisibility() == View.INVISIBLE) {
                    // Show notification. If API >= 21, then with circular reveal animation.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        circularRevealAnimationOn(foundView);
                    } else {
                        foundView.setVisibility(View.VISIBLE);
                    }

                    // Start notification close timer.
                    // Timer is cyclical, while notification is showed.
                    final Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (System.currentTimeMillis() - timerFound >
                                    delayClose) {

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            circularRevealAnimationOff(foundView);
                                        } else {
                                            foundView.setVisibility(View.INVISIBLE);
                                        }
                                    }
                                });

                                // If notification is closed, then stop timer.
                                timer.cancel();
                            }
                        }
                    }, delay, delay);
                }
            }
        };
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


    /**
     * @return Query provider, with logic: Create query builder, setting user values, make query.
     */
    @NonNull
    private FilterQueryProvider getFilterQueryProvider() {
        return new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {

                // Create and fill query builder.
                DatabaseQueryBuilder queryBuilder = new DatabaseQueryBuilder(ListActivity.this);

                // Add text for search in 'Name' and 'Description' columns.
                queryBuilder
                        .addOr(
                                SEARCH_COLUMNS[0],
                                OPERATOR_LIKE,
                                new String[] {constraint.toString()})

                        .addOr(
                                SEARCH_COLUMNS[1],
                                OPERATOR_LIKE,
                                new String[] {constraint.toString()});

                // Set sort order.
                queryBuilder.setOrder(BASE_COLUMN_ID);
                queryBuilder.setAscDesc(ORDER_ASCENDING);

                // Go-go-go.
                return dbHelper.getEntries(queryBuilder);
            }
        };
    }
}
