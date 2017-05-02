package com.gamaliev.list.list;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
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

import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.gamaliev.list.common.CommonUtils.circularRevealAnimationOff;
import static com.gamaliev.list.common.CommonUtils.circularRevealAnimationOn;
import static com.gamaliev.list.common.CommonUtils.showToast;
import static com.gamaliev.list.common.FileUtils.exportEntries;
import static com.gamaliev.list.common.FileUtils.importEntries;
import static com.gamaliev.list.list.ListActivitySharedPreferencesUtils.convertProfileJsonToMap;
import static com.gamaliev.list.list.ListActivitySharedPreferencesUtils.getSelectedProfileJson;
import static com.gamaliev.list.list.ListActivitySharedPreferencesUtils.initSharedPreferences;

public class ListActivity extends AppCompatActivity implements FilterSortDialogFragment.OnCompleteListener {

    /* Logger */
    private static final String TAG = ListActivity.class.getSimpleName();

    /* Intents */
    private static final int REQUEST_CODE_ADD           = 1;
    private static final int REQUEST_CODE_EDIT          = 2;
    private static final int REQUEST_CODE_IMPORT        = 3;
    public static final int REQUEST_CODE_DIALOG_FRAGMENT_RETURN_PROFILE = 4;

    private static final String RESULT_CODE_EXTRA       = "resultCodeExtra";
    public static final int RESULT_CODE_EXTRA_ADDED     = 1;
    public static final int RESULT_CODE_EXTRA_EDITED    = 2;
    public static final int RESULT_CODE_EXTRA_DELETED   = 3;

    /* SQLite */
    @NonNull public static final String[] SEARCH_COLUMNS = {
            DatabaseHelper.LIST_ITEMS_COLUMN_TITLE,
            DatabaseHelper.LIST_ITEMS_COLUMN_DESCRIPTION};

    @NonNull private ListDatabaseHelper mDbHelper;
    @NonNull private ListCursorAdapter mAdapter;
    @NonNull private FilterQueryProvider mQueryProvider;

    /* */
    @NonNull private ListView mListView;
    @NonNull private Button mFoundView;
    @NonNull private Map<String, String> mProfileMap;
    private long mTimerFound;


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
        initSharedPreferences(this);

        mQueryProvider = getFilterQueryProvider();
        mFoundView = (Button) findViewById(R.id.activity_list_button_found);
        mProfileMap = convertProfileJsonToMap(getSelectedProfileJson(this));

        initToolbarAndNavigationDrawer();
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
     * See also: {@link com.gamaliev.list.list.ListDatabaseHelper}
     */
    private void refreshDbConnectAndView() {
        if (mDbHelper == null) {
            mDbHelper = new ListDatabaseHelper(this);
        }
        Cursor cursor   = mDbHelper.getEntries(new DatabaseQueryBuilder());
        mAdapter = new ListCursorAdapter(this, cursor, 0);
        mListView = (ListView) findViewById(R.id.activity_list_listview);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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
        mAdapter.setFilterQueryProvider(mQueryProvider);

        // Apply filter.
        mAdapter.getFilter().filter("");
        showFoundNotification();
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
        initSearchView(menu);
        initFilterMenu(menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Setting SearchView listener.<br>
     * Filtering and updating list on text change.<br>
     * Also there is a notification about the number of positions found.
     * @param menu Action bar menu of activity.
     */
    private void initSearchView(@NonNull final Menu menu) {
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
                mAdapter.getFilter().filter(newText);
                showFoundNotification();
                return true;
            }
        });
    }

    private void initFilterMenu(@NonNull final Menu menu) {
        // Filter / Sort list.
        menu.findItem(R.id.menu_list_filter_sort)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // Launch dialog.
                FilterSortDialogFragment df = new FilterSortDialogFragment();
                df.show(getFragmentManager(), null);
                return true;
            }
        });
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

                // Update
                mAdapter.getFilter().filter("");
                showFoundNotification();

                // Notification if added.
                showToast(
                        this,
                        getString(R.string.activity_list_notification_entry_added),
                        Toast.LENGTH_SHORT);

            } else if (requestCode == REQUEST_CODE_EDIT) {
                refreshDbConnectAndView();

                if (data.getIntExtra(RESULT_CODE_EXTRA, -1) == RESULT_CODE_EXTRA_EDITED) {
                    // Notification if edited.
                    showToast(
                            this,
                            getString(R.string.activity_list_notification_entry_updated),
                            Toast.LENGTH_SHORT);

                } else if (data.getIntExtra(RESULT_CODE_EXTRA, -1) == RESULT_CODE_EXTRA_DELETED) {
                    // Notification if deleted.
                    showToast(
                            this,
                            getString(R.string.activity_list_notification_entry_deleted),
                            Toast.LENGTH_SHORT);
                }
            } else if (requestCode == REQUEST_CODE_IMPORT) {

                // If file selected, then start import.
                Uri selectedFile = data.getData();
                importEntries(ListActivity.this, selectedFile);

                // Update
                mAdapter.getFilter().filter("");
                showFoundNotification();
            }
        }
    }

    @Override
    public void onComplete(final int code, @Nullable final Object object) {
        if (code == REQUEST_CODE_DIALOG_FRAGMENT_RETURN_PROFILE && object != null) {
            mProfileMap = convertProfileJsonToMap(getSelectedProfileJson(this));
            mAdapter.getFilter().filter("");
            showFoundNotification();
            showToast(
                    this,
                    getString(R.string.activity_list_notification_filtered),
                    Toast.LENGTH_SHORT);
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
        mDbHelper.close();
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
                mFoundView.setText(String.format(Locale.ENGLISH,
                        getString(R.string.activity_list_notification_found_text) + "\n%d",
                        mListView.getCount()));

                // Set start time of the notification display.
                mTimerFound = System.currentTimeMillis();

                if (mFoundView.getVisibility() == View.INVISIBLE) {
                    // Show notification. If API >= 21, then with circular reveal animation.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        circularRevealAnimationOn(mFoundView);
                    } else {
                        mFoundView.setVisibility(View.VISIBLE);
                    }

                    // Start notification close timer.
                    // Timer is cyclical, while notification is showed.
                    final Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (System.currentTimeMillis() - mTimerFound >
                                    delayClose) {

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            circularRevealAnimationOff(mFoundView);
                                        } else {
                                            mFoundView.setVisibility(View.INVISIBLE);
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
     * @return Query provider, with logic: Create query builder, setting user values, make query.
     */
    @NonNull
    private FilterQueryProvider getFilterQueryProvider() {
        return new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {

                return mDbHelper.getCursorWithParams(
                        ListActivity.this,
                        constraint,
                        mProfileMap);
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

                    // Import entries.
                    case R.id.activity_list_nav_drawer_item_import_entries:

                        // Start file chooser.
                        Intent intent = new Intent();
                        intent.setType("*/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(
                                Intent.createChooser(intent, "Choose import-file, *.ili"),
                                REQUEST_CODE_IMPORT);
                        break;

                    // Export entries.
                    case R.id.activity_list_nav_drawer_item_export_entries:
                        exportEntries(ListActivity.this);
                        break;

                    // Add mock entries.
                    case R.id.activity_list_nav_drawer_item_add_mock_entries:
                        mDbHelper.addMockEntries();
                        refreshDbConnectAndView();
                        // Notification.
                        showToast(
                                ListActivity.this,
                                getString(R.string.activity_list_notification_add_mock_entries),
                                Toast.LENGTH_SHORT);
                        break;

                    // Remove all entries.
                    case R.id.activity_list_nav_drawer_item_delete_all_entries:
                        mDbHelper.removeAllEntries();
                        refreshDbConnectAndView();
                        // Notification.
                        showToast(
                                ListActivity.this,
                                getString(R.string.activity_list_notification_delete_all_entries),
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
