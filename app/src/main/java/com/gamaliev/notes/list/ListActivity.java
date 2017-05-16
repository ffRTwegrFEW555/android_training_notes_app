package com.gamaliev.notes.list;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.FileUtils;
import com.gamaliev.notes.common.shared_prefs.SpUsers;
import com.gamaliev.notes.user.UserActivity;
import com.gamaliev.notes.common.ProgressNotificationHelper;
import com.gamaliev.notes.common.db.DbHelper;
import com.gamaliev.notes.common.OnCompleteListener;
import com.gamaliev.notes.common.shared_prefs.SpFilterProfiles;
import com.gamaliev.notes.item_details.ItemDetailsActivity;
import com.gamaliev.notes.list.db.ListCursorAdapter;
import com.gamaliev.notes.list.db.ListDbHelper;

import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.gamaliev.notes.common.CommonUtils.checkAndRequestPermissions;
import static com.gamaliev.notes.common.CommonUtils.circularRevealAnimationOff;
import static com.gamaliev.notes.common.CommonUtils.circularRevealAnimationOn;
import static com.gamaliev.notes.common.CommonUtils.showMessageDialog;
import static com.gamaliev.notes.common.CommonUtils.showToast;
import static com.gamaliev.notes.common.CommonUtils.showToastRunOnUiThread;
import static com.gamaliev.notes.common.FileUtils.REQUEST_CODE_PERMISSIONS_READ_EXTERNAL_STORAGE;
import static com.gamaliev.notes.common.FileUtils.REQUEST_CODE_PERMISSIONS_WRITE_EXTERNAL_STORAGE;
import static com.gamaliev.notes.common.FileUtils.exportEntriesAsync;
import static com.gamaliev.notes.common.FileUtils.importEntriesAsync;
import static com.gamaliev.notes.common.shared_prefs.SpCommon.convertJsonToMap;

public final class ListActivity extends AppCompatActivity implements OnCompleteListener {

    /* Logger */
    private static final String TAG = ListActivity.class.getSimpleName();

    /* Intents */
    private static final int REQUEST_CODE_ADD           = 101;
    private static final int REQUEST_CODE_EDIT          = 102;
    private static final int REQUEST_CODE_IMPORT        = 103;
    private static final int REQUEST_CODE_EXPORT        = 104;
    private static final int REQUEST_CODE_CHANGE_USER   = 105;

    private static final String RESULT_CODE_EXTRA       = "resultCodeExtra";
    public static final int RESULT_CODE_FILTER_DIALOG   = 201;
    public static final int RESULT_CODE_EXTRA_ADDED     = 202;
    public static final int RESULT_CODE_EXTRA_EDITED    = 203;
    public static final int RESULT_CODE_EXTRA_DELETED   = 204;
    public static final int RESULT_CODE_EXTRA_IMPORTED  = 205;
    public static final int RESULT_CODE_EXTRA_EXPORTED  = 206;

    /* SQLite */
    @NonNull public static final String[] SEARCH_COLUMNS = {
            DbHelper.LIST_ITEMS_COLUMN_TITLE,
            DbHelper.LIST_ITEMS_COLUMN_DESCRIPTION};

    @NonNull private ListCursorAdapter mAdapter;
    @NonNull private FilterQueryProvider mQueryProvider;

    /* */
    @NonNull private ListView mListView;
    @NonNull private Button mFoundView;
    @NonNull private SearchView mSearchView;
    @NonNull private Map<String, String> mFilterProfileMap;
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
        mQueryProvider = getFilterQueryProvider();
        mFoundView = (Button) findViewById(R.id.activity_list_button_found);

        initFilterProfile();
        initToolbarAndNavigationDrawer();
        initFabOnClickListener();
        initAdapterAndListView();
    }

    private void initFilterProfile() {
        mFilterProfileMap = convertJsonToMap(
                SpFilterProfiles.getSelectedForCurrentUser(getApplicationContext()));
    }

    /**
     * Init toolbar and navigation drawer.
     */
    private void initToolbarAndNavigationDrawer() {
        // Set toolbar.
        final Toolbar toolbar = (Toolbar) findViewById(R.id.activity_list_toolbar);
        setSupportActionBar(toolbar);

        // Init toggle.
        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.activity_list_drawer_layout);
        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawer,
                toolbar,
                R.string.activity_list_nav_drawer_open,
                R.string.activity_list_nav_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // Set navigation view listener
        final NavigationView navigationView = (NavigationView) findViewById(R.id.activity_list_nav_view);
        navigationView.setNavigationItemSelectedListener(getNavItemSelectedListener());

        // Change user title
        final Map<String, String> userProfile
                = SpUsers.get(
                getApplicationContext(),
                SpUsers.getSelected(getApplicationContext()));

        ((TextView) navigationView
                .getHeaderView(0)
                .findViewById(R.id.activity_list_nav_drawable_header_title_text_view))

                .setText(   userProfile.get(SpUsers.SP_USER_FIRST_NAME) + " " +
                            userProfile.get(SpUsers.SP_USER_LAST_NAME) + " " +
                            userProfile.get(SpUsers.SP_USER_MIDDLE_NAME));

        ((TextView) navigationView
                .getHeaderView(0)
                .findViewById(R.id.activity_list_nav_drawable_header_mail_text_view))

                .setText(userProfile.get(SpUsers.SP_USER_EMAIL));
    }

    /**
     * Start {@link ItemDetailsActivity} activity for result,
     * with Add new entry action.
     */
    private void initFabOnClickListener() {
        findViewById(R.id.activity_list_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ItemDetailsActivity.startAdd(ListActivity.this, REQUEST_CODE_ADD);
            }
        });
    }

    /**
     * Get cursor from database, create and set adapter,
     * set on click listener, set filter query provider.<br>
     */
    private void initAdapterAndListView() {

        // Create adapter.
        mAdapter = new ListCursorAdapter(this, null, 0);

        // SearchView filter query provider.
        mAdapter.setFilterQueryProvider(mQueryProvider);

        // Register observer. Showing found notification.
        mAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                showFoundNotification();
            }
        });

        // Init list view
        mListView = (ListView) findViewById(R.id.activity_list_listview);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // On click - start item details activity, with edit action.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // If API >= 21, then set shared transition animation.
                    final View iconView = view.findViewById(R.id.activity_list_item_color);
                    iconView.setTransitionName(
                            getString(R.string.shared_transition_name_color_box));
                    final Pair<View, String> icon = new Pair<>(iconView, iconView.getTransitionName());
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

        // Refresh view.
        filterAdapter("");
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
        mSearchView = (SearchView) menu.findItem(R.id.menu_list_search).getActionView();

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Refresh view.
                filterAdapter(newText);
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
                df.show(getSupportFragmentManager(), null);
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
        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.activity_list_drawer_layout);
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
     * See {@link com.gamaliev.notes.colorpicker.ColorPickerActivity#EXTRA_COLOR}
     */
    @NonNull
    public static Intent getResultIntent(final int resultCodeExtra) {
        final Intent intent = new Intent();
        intent.putExtra(RESULT_CODE_EXTRA, resultCodeExtra);
        return intent;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_ADD) {
                // Notification if added.
                showToast(
                        this,
                        getString(R.string.activity_list_notification_entry_added),
                        Toast.LENGTH_SHORT);

            } else if (requestCode == REQUEST_CODE_EDIT) {
                if (data.getIntExtra(RESULT_CODE_EXTRA, -1) == RESULT_CODE_EXTRA_EDITED) {
                    // Show notification if edited.
                    showToast(
                            this,
                            getString(R.string.activity_list_notification_entry_updated),
                            Toast.LENGTH_SHORT);

                } else if (data.getIntExtra(RESULT_CODE_EXTRA, -1) == RESULT_CODE_EXTRA_DELETED) {
                    // Show notification if deleted.
                    showToast(
                            this,
                            getString(R.string.activity_list_notification_entry_deleted),
                            Toast.LENGTH_SHORT);
                }

            } else if (requestCode == REQUEST_CODE_IMPORT) {

                // If file selected, then start import.
                final Uri selectedFile = data.getData();
                importEntriesAsync(this, selectedFile, this);

            } else if (requestCode == REQUEST_CODE_EXPORT) {

                // If file selected, then start export.
                final Uri selectedFile = data.getData();
                exportEntriesAsync(this, selectedFile, this);
            }

            //
            initFilterProfile();
            updateFilterAdapter();

        } else if (resultCode == RESULT_CANCELED) {
            if (requestCode == REQUEST_CODE_CHANGE_USER) {
                initFilterProfile();
                updateFilterAdapter();
            }
        }
    }

    /**
     * Request permission result handler.
     * @param requestCode   Request code.
     * @param permissions   Checked permissions. See: {@link android.Manifest.permission}
     * @param grantResults  Result.
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        switch (requestCode) {
            case REQUEST_CODE_PERMISSIONS_WRITE_EXTERNAL_STORAGE:

                // If access to write is granted, then export entries.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    //
                    startExportFileChooser();
                } else {

                    // If denied, then make explanation notification.
                    final String deniedMessage = getString(R.string.file_utils_export_message_write_external_storage_denied);
                    Log.i(TAG, deniedMessage);
                    showMessageDialog(
                            this,
                            null,
                            deniedMessage);
                }

                break;

            case REQUEST_CODE_PERMISSIONS_READ_EXTERNAL_STORAGE:

                // If access to read is granted, then import entries.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    //
                    startImportFileChooser();
                } else {

                    // If denied, then make explanation notification.
                    final String deniedMessage = getString(R.string.file_utils_import_message_read_external_storage_denied);
                    Log.i(TAG, deniedMessage);
                    showMessageDialog(
                            this,
                            null,
                            deniedMessage);
                }

                break;

            default:
                break;
        }
    }

    @Override
    public void onComplete(final int code) {
        if (code == RESULT_CODE_FILTER_DIALOG) {
            // Show notification
            showToast(
                    this,
                    getString(R.string.activity_list_notification_filtered),
                    Toast.LENGTH_SHORT);
        }
        //
        initFilterProfile();
        updateFilterAdapter();
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

                // Check..
                if (mFoundView.isAttachedToWindow()) {

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

                return ListDbHelper.getCursorWithParams(
                        ListActivity.this,
                        constraint,
                        mFilterProfileMap);
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

                        // Check readable. If denied, make request, then break.
                        if (checkAndRequestPermissions(
                                ListActivity.this,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                REQUEST_CODE_PERMISSIONS_READ_EXTERNAL_STORAGE)) {

                            //
                            startImportFileChooser();
                        }
                        break;

                    // Export entries.
                    case R.id.activity_list_nav_drawer_item_export_entries:

                        // Check writable. If denied, make request, then break.
                        if (checkAndRequestPermissions(
                                ListActivity.this,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                REQUEST_CODE_PERMISSIONS_WRITE_EXTERNAL_STORAGE)) {

                            //
                            startExportFileChooser();
                        }
                        break;

                    // Add mock entries.
                    case R.id.activity_list_nav_drawer_item_add_mock_entries:
                        showInputDialogAddMockEntries();
                        break;

                    // Remove all entries.
                    case R.id.activity_list_nav_drawer_item_delete_all_entries:
                        deleteAllEntries();
                        break;

                    // Change user.
                    case R.id.activity_list_nav_drawer_item_change_user:
                        UserActivity.startIntent(
                                ListActivity.this,
                                REQUEST_CODE_CHANGE_USER);
                        break;

                    //
                    default:
                        break;

                }

                // Close nav drawer after click.
                final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.activity_list_drawer_layout);
                drawer.closeDrawer(GravityCompat.START);
                return true;
            }
        };
    }

    /**
     * Show a dialog box to enter the number of entries.
     */
    private void showInputDialogAddMockEntries() {

        // Create builder.
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);

        // Create edit text.
        final EditText editText = new EditText(this);
        editText.setText(
                String.valueOf(
                        getResources().getInteger(R.integer.mock_items_number_click)));

        // Get string.
        final String title = getString(R.string.activity_list_notification_add_mock_title);
        final String save = getString(R.string.activity_list_notification_add_mock_ok);
        final String cancel = getString(R.string.activity_list_notification_add_mock_cancel);

        // Set title.
        alert.setTitle(title);

        // Create container, set margin, add exitText.
        final FrameLayout container = new FrameLayout(this);
        final FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        int m = getResources().getDimensionPixelSize(R.dimen.activity_list_filter_dialog_input_title);
        params.setMargins(m, 0, m, 0);
        editText.setLayoutParams(params);
        container.addView(editText);

        // Set view.
        alert.setView(container);

        // Set save button listener.
        alert.setPositiveButton(save, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                final String enteredText = editText.getText().toString();
                addMockEntries(Integer.parseInt(enteredText));
            }
        });

        // Set cancel button listener.
        alert.setNegativeButton(cancel, null);

        // Show.
        alert.show();
    }

    private void addMockEntries(
            final int numberOfEntries) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                //
                showToastRunOnUiThread(
                        ListActivity.this,
                        getString(R.string.activity_list_notification_add_mock_entries_start),
                        Toast.LENGTH_SHORT);

                // Create progress notification.
                final ProgressNotificationHelper notification =
                        new ProgressNotificationHelper(
                                ListActivity.this,
                                getString(R.string.activity_list_notification_add_mock_title),
                                getString(R.string.activity_list_notification_add_mock_text),
                                getString(R.string.activity_list_notification_add_mock_finish));

                // Timer for notification enable
                notification.startTimerToEnableNotification(
                        getResources().getInteger(
                                R.integer.activity_list_notification_panel_add_mock_timer_enable));

                // Add.
                final int added = ListDbHelper.addMockEntries(
                        ListActivity.this,
                        notification,
                        numberOfEntries);

                //
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //
                        initFilterProfile();
                        updateFilterAdapter();
                    }
                });

                // Show result notification.
                showToastRunOnUiThread(
                        ListActivity.this,
                        added > -1
                                ? getString(R.string.activity_list_notification_add_mock_entries_success)
                                + " (" + added + ")"
                                : getString(R.string.activity_list_notification_add_mock_entries_failed),
                        Toast.LENGTH_SHORT);
            }
        }).start();
    }

    private void deleteAllEntries() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                // Remove.
                final boolean success = ListDbHelper.removeAllEntries(ListActivity.this);
                //
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //
                        initFilterProfile();
                        updateFilterAdapter();
                    }
                });
                // Show result notification.
                showToastRunOnUiThread(
                        ListActivity.this,
                        success
                                ? getString(R.string.activity_list_notification_delete_all_entries_success)
                                : getString(R.string.activity_list_notification_delete_all_entries_failed),
                        Toast.LENGTH_SHORT);
            }
        }).start();
    }

    /**
     * @param text Text to filter.
     */
    private void filterAdapter(@NonNull final String text) {
        mAdapter.getFilter().filter(text);
    }

    /**
     * Getting text from search view, and use for filter. If text is empty, then using empty string.
     */
    private void updateFilterAdapter() {
        if (mSearchView != null) {
            final String searchText = mSearchView.getQuery().toString();
            filterAdapter(TextUtils.isEmpty(searchText) ? "" : searchText);

        } else {
            filterAdapter("");
        }
    }

    /**
     * Creating intent, and starting file chooser for import-file.<br>
     * Then result handle by {@link #onActivityResult(int, int, Intent)},
     * with {@link #REQUEST_CODE_IMPORT}.
     */
    private void startImportFileChooser() {
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_CODE_IMPORT);
    }

    /**
     * Creating intent, and starting file chooser for export-file.<br>
     * Then result handle by {@link #onActivityResult(int, int, Intent)},
     * with {@link #REQUEST_CODE_EXPORT}.
     */
    private void startExportFileChooser() {
        final Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, FileUtils.FILE_NAME_EXPORT_DEFAULT);
        startActivityForResult(intent, REQUEST_CODE_EXPORT);
    }
}
