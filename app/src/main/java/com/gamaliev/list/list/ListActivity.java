package com.gamaliev.list.list;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.Toast;

import com.gamaliev.list.R;
import com.gamaliev.list.common.DatabaseHelper;
import com.gamaliev.list.common.DatabaseQueryBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static com.gamaliev.list.common.CommonUtils.circularRevealAnimationOff;
import static com.gamaliev.list.common.CommonUtils.circularRevealAnimationOn;
import static com.gamaliev.list.common.CommonUtils.showToast;
import static com.gamaliev.list.common.DatabaseHelper.BASE_COLUMN_ID;
import static com.gamaliev.list.common.DatabaseHelper.FAVORITE_COLUMN_COLOR;
import static com.gamaliev.list.common.DatabaseHelper.LIST_ITEMS_COLUMN_CREATED;
import static com.gamaliev.list.common.DatabaseHelper.LIST_ITEMS_COLUMN_EDITED;
import static com.gamaliev.list.common.DatabaseHelper.LIST_ITEMS_COLUMN_VIEWED;
import static com.gamaliev.list.common.DatabaseQueryBuilder.OPERATOR_BETWEEN;
import static com.gamaliev.list.common.DatabaseQueryBuilder.OPERATOR_EQUALS;
import static com.gamaliev.list.common.DatabaseQueryBuilder.OPERATOR_LIKE;

public class ListActivity extends AppCompatActivity {

    /* Logger */
    private static final String TAG = ListActivity.class.getSimpleName();

    /* Intents */
    private static final int REQUEST_CODE_ADD           = 1;
    private static final int REQUEST_CODE_EDIT          = 2;
    private static final String RESULT_CODE_EXTRA       = "resultCodeExtra";
    public static final int RESULT_CODE_EXTRA_ADDED     = 1;
    public static final int RESULT_CODE_EXTRA_EDITED    = 2;
    public static final int RESULT_CODE_EXTRA_DELETED   = 3;

    /* SQLite */
    @NonNull private static final String[] SEARCH_COLUMNS = {
            DatabaseHelper.LIST_ITEMS_COLUMN_TITLE,
            DatabaseHelper.LIST_ITEMS_COLUMN_DESCRIPTION};

    @NonNull private ListDatabaseHelper dbHelper;
    @NonNull private ListCursorAdapter adapter;
    @NonNull private FilterQueryProvider queryProvider;

    /* Shared Preferences */
    private static final String SP_HAS_VISITED                  = "hasVisited";
    private static final String SP_ACTION_LOAD                  = "load";
    private static final String SP_ACTION_SAVE                  = "save";
    private static final String SP_FILTER_SORT_PROFILE_SELECTED = "filterSortProfileSelected";
    private static final String SP_FILTER_SORT_PROFILES_SET     = "filterSortProfilesSet";
    private static final String SP_FILTER_SORT_ID_DEFAULT       = "0";

    public static final String SP_FILTER_SORT_ID               = BASE_COLUMN_ID;
    public static final String SP_FILTER_COLOR                 = FAVORITE_COLUMN_COLOR;
    public static final String SP_FILTER_CREATED               = LIST_ITEMS_COLUMN_CREATED;
    public static final String SP_FILTER_EDITED                = LIST_ITEMS_COLUMN_EDITED;
    public static final String SP_FILTER_VIEWED                = LIST_ITEMS_COLUMN_VIEWED;
    public static final String SP_FILTER_SYMBOL_DATE_SPLIT     = "#";

    public static final String SP_ORDER                        = "order";
    public static final String SP_ORDER_ASC_DESC               = "orderAscDesc";

    /* */
    @NonNull private ListView listView;
    @NonNull private Button foundView;
    @NonNull private Map<String, String> settings;
    private long timerFound;


    /*
        Init
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_drawer_layout);
        init();
    }

    private void init() {
        queryProvider   = getFilterQueryProvider();
        foundView       = (Button) findViewById(R.id.activity_list_button_found);
        settings        = new HashMap<>();

        initToolbarAndNavigationDrawer();
        initSharedPreferences();
        setFabOnClickListener();
        refreshDbConnectAndView();
    }

    /**
     * Init toolbar and navigation drawer.
     */
    private void initToolbarAndNavigationDrawer() {
        // Set toolbar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.activity_list_toolbar);
        setSupportActionBar(toolbar);

        // Init toggle.
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.activity_list_drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawer,
                toolbar,
                R.string.activity_list_nav_drawer_open,
                R.string.activity_list_nav_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // Set navigation view listener
        NavigationView navigationView = (NavigationView) findViewById(R.id.activity_list_nav_view);
        navigationView.setNavigationItemSelectedListener(getNavItemSelectedListener());
    }

    /**
     * Load setting from shared preferences.<br>
     * If activity start first time,
     * then save default settings in shared preferences of this activity.
     */
    private void initSharedPreferences() {
        final SharedPreferences sp = getPreferences(MODE_PRIVATE);

        if(!sp.getBoolean(SP_HAS_VISITED, false)) {
            // Get editor.
            final SharedPreferences.Editor editor = sp.edit();

            // Put mock and default values.
            final Set<String> profiles = ListDatabaseMockHelper.getMockProfiles();
            editor.putStringSet(SP_FILTER_SORT_PROFILES_SET, profiles);
            editor.putString(SP_FILTER_SORT_PROFILE_SELECTED, SP_FILTER_SORT_ID_DEFAULT);

            // Mark visited.
            editor.putBoolean(SP_HAS_VISITED, true);
            editor.apply();
        }

        loadSharedPreferences();
    }

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
     * Open a new database helper, get cursor, create and set adapter,
     * set on click listener, set filter query provider.<br>
     * See also: {@link com.gamaliev.list.list.ListDatabaseHelper}
     */
    private void refreshDbConnectAndView() {
        if (dbHelper == null) {
            dbHelper = new ListDatabaseHelper(this);
        }
        // TODO: FILTERED CURSOR
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
        return super.onOptionsItemSelected(item);
    }


    /*
        On back pressed.
     */

    @Override
    public void onBackPressed() {
        // Check navigation drawer. If open, then close.
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.activity_list_drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    /*
        Intents
     */

    /**
     * @param resultCodeExtra   Result code extra.
     *                          {@link #RESULT_CODE_EXTRA_ADDED},
     *                          {@link #RESULT_CODE_EXTRA_EDITED},
     *                          {@link #RESULT_CODE_EXTRA_DELETED}.
     * @return Intent, with given result code extra.
     * See {@link com.gamaliev.list.colorpicker.ColorPickerActivity#EXTRA_COLOR}
     */
    @NonNull
    public static Intent getResultIntent(final int resultCodeExtra) {
        Intent intent = new Intent();
        intent.putExtra(RESULT_CODE_EXTRA, resultCodeExtra);
        return intent;
    }

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

                // Notification if added.
                showToast(
                        this,
                        getResources().getString(R.string.activity_list_notification_entry_added),
                        Toast.LENGTH_SHORT);

            } else if (requestCode == REQUEST_CODE_EDIT) {
                refreshDbConnectAndView();

                if (data.getIntExtra(RESULT_CODE_EXTRA, -1) == RESULT_CODE_EXTRA_EDITED) {
                    // Notification if edited.
                    showToast(
                            this,
                            getResources().getString(R.string.activity_list_notification_entry_updated),
                            Toast.LENGTH_SHORT);

                } else if (data.getIntExtra(RESULT_CODE_EXTRA, -1) == RESULT_CODE_EXTRA_DELETED) {
                    // Notification if deleted.
                    showToast(
                            this,
                            getResources().getString(R.string.activity_list_notification_entry_deleted),
                            Toast.LENGTH_SHORT);
                }
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
        Shared preferences
     */

    /**
     * Load settings from shared preferences of this activity
     */
    private void loadSharedPreferences() {
        makeActionSharedPreferences(SP_ACTION_LOAD);
    }

    /**
     * Save settings to shared preferences of this activity
     */
    private void saveSharedPreferences() {
        makeActionSharedPreferences(SP_ACTION_SAVE);
    }

    /**
     * Main logic for load/save settings from/to shared preferences of this activity.
     * @param action {@link #SP_ACTION_LOAD}, {@link #SP_ACTION_SAVE}
     */
    private void makeActionSharedPreferences(@NonNull final String action) {
        final SharedPreferences sp = getPreferences(MODE_PRIVATE);

        // Get selected profile.
        final String selectedProfile = sp.getString(
                SP_FILTER_SORT_PROFILE_SELECTED,
                SP_FILTER_SORT_ID_DEFAULT);

        // Get all profiles.
        final Set<String> profiles = sp.getStringSet(
                SP_FILTER_SORT_PROFILES_SET,
                new HashSet<String>());

        // Seek profiles.
        for (String profile : profiles) {

            try {
                JSONObject profileJson = new JSONObject(profile);

                // Get id.
                String id = profileJson.optString(SP_FILTER_SORT_ID, "-1");

                // If found, process and break, else seek next;
                if (selectedProfile.equals(id)) {

                    if (SP_ACTION_SAVE.equals(action)) {
                        // Update profile from map to shared preferences.
                        JSONObject profileJsonFromMap = new JSONObject(settings);
                        profile = profileJsonFromMap.toString();

                        // Save profiles to shared preferences.
                        final SharedPreferences.Editor editor = sp.edit();
                        editor.putStringSet(SP_FILTER_SORT_PROFILES_SET, profiles);
                        editor.apply();

                    } else if (SP_ACTION_LOAD.equals(action)) {
                        // Put key-value pairs to settings map.
                        settings.put(SP_FILTER_SORT_ID, profileJson.optString(SP_FILTER_SORT_ID));
                        settings.put(SP_FILTER_COLOR,   profileJson.optString(SP_FILTER_COLOR,  ""));
                        settings.put(SP_FILTER_CREATED, profileJson.optString(SP_FILTER_CREATED,""));
                        settings.put(SP_FILTER_EDITED,  profileJson.optString(SP_FILTER_EDITED, ""));
                        settings.put(SP_FILTER_VIEWED,  profileJson.optString(SP_FILTER_VIEWED, ""));
                        settings.put(SP_ORDER,          profileJson.optString(SP_ORDER,         ""));
                        settings.put(SP_ORDER_ASC_DESC, profileJson.optString(SP_ORDER_ASC_DESC,""));
                    }

                    break;
                }

            } catch (JSONException e) {
                Log.e(TAG, e.toString());
            }
        }
    }


    /*
        Methods
     */

    /**
     * @return Query provider, with logic: Create query builder, setting user values, make query.
     */
    @NonNull
    private FilterQueryProvider getFilterQueryProvider() {
        return new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {

                // Create and fill query builder for text search.
                final DatabaseQueryBuilder searchTextQueryBuilder = new DatabaseQueryBuilder(ListActivity.this);

                // Add text for search in 'Name' and 'Description' columns.
                searchTextQueryBuilder
                        .addOr( SEARCH_COLUMNS[0],
                                OPERATOR_LIKE,
                                new String[] {constraint.toString()})

                        .addOr( SEARCH_COLUMNS[1],
                                OPERATOR_LIKE,
                                new String[] {constraint.toString()});

                // Create and fill query result builder.
                final DatabaseQueryBuilder resultQueryBuilder = new DatabaseQueryBuilder(ListActivity.this);

                // Add color filter, if not empty or null.
                if (!TextUtils.isEmpty(settings.get(FAVORITE_COLUMN_COLOR))) {
                    resultQueryBuilder.addAnd(
                            FAVORITE_COLUMN_COLOR,
                            OPERATOR_EQUALS,
                            new String[]{settings.get(FAVORITE_COLUMN_COLOR)});
                }

                // Add created filter, if not empty or null.
                if (!TextUtils.isEmpty(settings.get(LIST_ITEMS_COLUMN_CREATED))) {
                    resultQueryBuilder.addAnd(
                            LIST_ITEMS_COLUMN_CREATED,
                            OPERATOR_BETWEEN,
                            settings.get(LIST_ITEMS_COLUMN_CREATED)
                                    .split(SP_FILTER_SYMBOL_DATE_SPLIT));
                }

                // Add edited filter, if not empty or null.
                if (!TextUtils.isEmpty(settings.get(LIST_ITEMS_COLUMN_EDITED))) {
                    resultQueryBuilder.addAnd(
                            LIST_ITEMS_COLUMN_EDITED,
                            OPERATOR_BETWEEN,
                            settings.get(LIST_ITEMS_COLUMN_EDITED)
                                    .split(SP_FILTER_SYMBOL_DATE_SPLIT));
                }

                // Add viewed filter, if not empty or null.
                if (!TextUtils.isEmpty(settings.get(LIST_ITEMS_COLUMN_VIEWED))) {
                    resultQueryBuilder.addAnd(
                            LIST_ITEMS_COLUMN_VIEWED,
                            OPERATOR_BETWEEN,
                            settings.get(LIST_ITEMS_COLUMN_VIEWED)
                                    .split(SP_FILTER_SYMBOL_DATE_SPLIT));
                }

                // Add search text inner filter
                resultQueryBuilder.addAndInner(searchTextQueryBuilder);

                // Set sort order.
                resultQueryBuilder.setOrder(settings.get(SP_ORDER));
                resultQueryBuilder.setAscDesc(settings.get(SP_ORDER_ASC_DESC));

                // Go-go-go.
                return dbHelper.getEntries(resultQueryBuilder);
            }
        };
    }

    /**
     * @return Navigation bar listener object.
     */
    @NonNull
    private NavigationView.OnNavigationItemSelectedListener getNavItemSelectedListener() {
        return new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // Handle navigation view item clicks here.
                final int id = item.getItemId();

                switch (id) {
                    // Filter / Sort list.
                    case R.id.activity_list_nav_drawer_item_filter_sort:
                        break;

                    // Import entries.
                    case R.id.activity_list_nav_drawer_item_import_entries:
                        break;

                    // Export entries.
                    case R.id.activity_list_nav_drawer_item_export_entries:
                        break;

                    // Add mock entries.
                    case R.id.activity_list_nav_drawer_item_add_mock_entries:
                        dbHelper.addMockEntries();
                        refreshDbConnectAndView();
                        // Notification.
                        showToast(
                                ListActivity.this,
                                getResources().getString(R.string.activity_list_notification_add_mock_entries),
                                Toast.LENGTH_SHORT);
                        break;

                    // Remove all entries.
                    case R.id.activity_list_nav_drawer_item_delete_all_entries:
                        dbHelper.removeAllEntries();
                        refreshDbConnectAndView();
                        // Notification.
                        showToast(
                                ListActivity.this,
                                getResources().getString(R.string.activity_list_notification_delete_all_entries),
                                Toast.LENGTH_SHORT);
                        break;

                    // Default.
                    default:
                        break;

                }

                // Close nav drawer after click.
                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.activity_list_drawer_layout);
                drawer.closeDrawer(GravityCompat.START);
                return true;
            }
        };
    }
}
