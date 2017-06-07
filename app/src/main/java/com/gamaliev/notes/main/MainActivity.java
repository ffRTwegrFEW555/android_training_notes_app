package com.gamaliev.notes.main;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.FileUtils;
import com.gamaliev.notes.common.ProgressNotificationHelper;
import com.gamaliev.notes.common.observers.Observer;
import com.gamaliev.notes.common.shared_prefs.SpUsers;
import com.gamaliev.notes.list.ListFragment;
import com.gamaliev.notes.list.db.ListDbHelper;
import com.gamaliev.notes.settings.SettingsPreferenceFragment;
import com.gamaliev.notes.sync.SyncFragment;
import com.gamaliev.notes.user.UserFragment;

import java.util.Map;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static com.gamaliev.notes.common.CommonUtils.checkAndRequestPermissions;
import static com.gamaliev.notes.common.CommonUtils.showMessageDialog;
import static com.gamaliev.notes.common.CommonUtils.showToastRunOnUiThread;
import static com.gamaliev.notes.common.FileUtils.exportEntriesAsync;
import static com.gamaliev.notes.common.FileUtils.importEntriesAsync;
import static com.gamaliev.notes.common.codes.RequestCode.REQUEST_CODE_NOTES_EXPORT;
import static com.gamaliev.notes.common.codes.RequestCode.REQUEST_CODE_NOTES_IMPORT;
import static com.gamaliev.notes.common.codes.RequestCode.REQUEST_CODE_PERMISSIONS_READ_EXTERNAL_STORAGE;
import static com.gamaliev.notes.common.codes.RequestCode.REQUEST_CODE_PERMISSIONS_WRITE_EXTERNAL_STORAGE;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_MOCK_ENTRIES_ADDED;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_USER_ADDED;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_USER_CHANGE_PREFERENCES;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_USER_DELETED;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_USER_SELECTED;
import static com.gamaliev.notes.common.observers.ObserverHelper.ENTRIES_MOCK;
import static com.gamaliev.notes.common.observers.ObserverHelper.USERS;
import static com.gamaliev.notes.common.observers.ObserverHelper.notifyObservers;
import static com.gamaliev.notes.common.observers.ObserverHelper.registerObserver;
import static com.gamaliev.notes.common.observers.ObserverHelper.unregisterObserver;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

@SuppressWarnings("NullableProblems")
public class MainActivity extends AppCompatActivity implements Observer {

    /* Logger */
    @NonNull private static final String TAG = MainActivity.class.getSimpleName();

    /* Observed */
    @NonNull private static final String[] OBSERVED = {USERS};

    /* ... */
    @NonNull private NavigationView mNavView;
    @NonNull private DrawerLayout mDrawer;
    @NonNull private Toolbar mToolbar;


    /*
        Lifecycle
     */

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_drawer_layout);
        init();

        if (savedInstanceState == null) {
            ListFragment fragment = ListFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.activity_main_fragment_container, fragment)
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        initDrawerLockMode();
        initUserInfo(mNavView);
        registerObserver(OBSERVED, toString(), this);
        super.onResume();
    }

    @Override
    public void onPause() {
        unregisterObserver(OBSERVED, toString());
        super.onPause();
    }


    /*
        ...
     */

    private void init() {
        initToolbarAndNavigationDrawer();
    }

    private void initToolbarAndNavigationDrawer() {
        mToolbar = (Toolbar) findViewById(R.id.activity_main_toolbar);
        setSupportActionBar(mToolbar);

        mDrawer = (DrawerLayout) findViewById(R.id.activity_main_drawer_layout);

        mNavView = (NavigationView) findViewById(R.id.activity_main_nav_view);
        mNavView.setNavigationItemSelectedListener(getNavItemSelectedListener());

        initUserInfo(mNavView);
    }

    private void initDrawerLockMode() {
        if (getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE) {
            mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
            mDrawer.setScrimColor(Color.TRANSPARENT);

        } else {
            final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this,
                    mDrawer,
                    mToolbar,
                    R.string.activity_main_nav_drawer_open,
                    R.string.activity_main_nav_drawer_close);
            mDrawer.addDrawerListener(toggle);
            mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            mDrawer.closeDrawer(GravityCompat.START);
            toggle.syncState();
        }
    }

    private void initUserInfo(@NonNull final NavigationView navView) {
        final String userId = SpUsers.getSelected(getApplicationContext());
        if (userId == null) {
            Log.e(TAG, "User ID is null.");
            return;
        }

        final Map<String, String> userProfile = SpUsers.get(getApplicationContext(), userId);

        ((TextView) navView
                .getHeaderView(0)
                .findViewById(R.id.activity_main_nav_drawable_header_title_text_view))
                //noinspection SetTextI18n
                .setText(userProfile.get(SpUsers.SP_USER_FIRST_NAME) + " "
                                + userProfile.get(SpUsers.SP_USER_LAST_NAME) + " "
                                + userProfile.get(SpUsers.SP_USER_MIDDLE_NAME));

        ((TextView) navView
                .getHeaderView(0)
                .findViewById(R.id.activity_main_nav_drawable_header_mail_text_view))
                .setText(userProfile.get(SpUsers.SP_USER_EMAIL));
    }


    /*
        On back pressed.
     */

    @Override
    public void onBackPressed() {
        // Check navigation drawer. If open, then close.
        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.activity_main_drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)
                && getResources().getConfiguration().orientation != ORIENTATION_LANDSCAPE) {
            drawer.closeDrawer(GravityCompat.START);

        } else {
            showActionBarAndFullscreenOff();
            super.onBackPressed();
        }
    }


    /*
        Callbacks
     */

    @Override
    protected void onActivityResult(
            final int requestCode,
            final int resultCode,
            final Intent data) {

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_NOTES_IMPORT) {
                final Uri selectedFile = data.getData();
                importEntriesAsync(this, selectedFile);

            } else if (requestCode == REQUEST_CODE_NOTES_EXPORT) {
                final Uri selectedFile = data.getData();
                exportEntriesAsync(this, selectedFile);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode,
            @NonNull final String[] permissions,
            @NonNull final int[] grantResults) {

        switch (requestCode) {
            case REQUEST_CODE_PERMISSIONS_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startExportFileChooser();

                } else {
                    final String deniedMessage =
                            getString(R.string.file_utils_export_message_write_external_storage_denied);
                    Log.i(TAG, deniedMessage);
                    showMessageDialog(
                            this,
                            null,
                            deniedMessage);
                }
                break;

            case REQUEST_CODE_PERMISSIONS_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startImportFileChooser();

                } else {
                    final String deniedMessage =
                            getString(R.string.file_utils_import_message_read_external_storage_denied);
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


    /*
        Mock entries.
     */

    private void showInputDialogAddMockEntries() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);

        final String title = getString(R.string.activity_main_notification_add_mock_title);
        final String save = getString(R.string.activity_main_notification_add_mock_ok);
        final String cancel = getString(R.string.activity_main_notification_add_mock_cancel);
        alert.setTitle(title);

        // Container with margin.
        final EditText editText = new EditText(this);
        editText.setText(
                String.valueOf(SpUsers.getNumberMockEntriesForCurrentUser(getApplicationContext())));
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        final FrameLayout container = new FrameLayout(this);
        final FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        int m = getResources().getDimensionPixelSize(R.dimen.activity_main_dialog_input_number_et_margin);
        params.setMargins(m, 0, m, 0);
        editText.setLayoutParams(params);
        container.addView(editText);

        alert   .setView(container)
                .setPositiveButton(save, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        final String enteredText = editText.getText().toString();
                        addMockEntries(Integer.parseInt(enteredText));
                    }
                })
                .setNegativeButton(cancel, null)
                .show();
    }

    private void addMockEntries(final int numberOfEntries) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                showToastRunOnUiThread(
                        getString(R.string.activity_main_notification_add_mock_entries_start),
                        Toast.LENGTH_SHORT);

                final ProgressNotificationHelper notification =
                        new ProgressNotificationHelper(
                                MainActivity.this,
                                getString(R.string.activity_main_notification_add_mock_title),
                                getString(R.string.activity_main_notification_add_mock_text),
                                getString(R.string.activity_main_notification_add_mock_finish));
                notification.startTimerToEnableNotification(
                        SpUsers.getProgressNotificationTimerForCurrentUser(getApplicationContext()),
                        false);

                final int added = ListDbHelper.addMockEntries(
                        MainActivity.this,
                        notification,
                        numberOfEntries);

                showToastRunOnUiThread(
                        added > -1
                                ? getString(R.string.activity_main_notification_add_mock_entries_success)
                                + " (" + added + ")"
                                : getString(R.string.activity_main_notification_add_mock_entries_failed),
                        Toast.LENGTH_SHORT);

                notifyObservers(
                        ENTRIES_MOCK,
                        RESULT_CODE_MOCK_ENTRIES_ADDED,
                        null);
            }
        }).start();
    }


    /*
        Intents
     */

    private void startImportFileChooser() {
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_CODE_NOTES_IMPORT);
    }

    private void startExportFileChooser() {
        final Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, FileUtils.FILE_NAME_EXPORT_DEFAULT);
        startActivityForResult(intent, REQUEST_CODE_NOTES_EXPORT);
    }


    /*
        ...
     */

    @NonNull
    private NavigationView.OnNavigationItemSelectedListener getNavItemSelectedListener() {
        return new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.activity_main_nav_drawer_item_import_entries:
                        if (checkAndRequestPermissions(
                                MainActivity.this,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                REQUEST_CODE_PERMISSIONS_READ_EXTERNAL_STORAGE)) {
                            startImportFileChooser();
                        }
                        break;

                    case R.id.activity_main_nav_drawer_item_export_entries:
                        if (checkAndRequestPermissions(
                                MainActivity.this,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                REQUEST_CODE_PERMISSIONS_WRITE_EXTERNAL_STORAGE)) {
                            startExportFileChooser();
                        }
                        break;

                    case R.id.activity_main_nav_drawer_item_notes_list:
                        showActionBarAndFullscreenOff();
                        final ListFragment listFragment = ListFragment.newInstance();
                        getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.activity_main_fragment_container, listFragment)
                                .addToBackStack(null)
                                .commit();
                        break;

                    case R.id.activity_main_nav_drawer_item_add_mock_entries:
                        showInputDialogAddMockEntries();
                        break;

                    /*// Remove all entries.
                    case R.id.activity_main_nav_drawer_item_delete_all_entries:
                        deleteAllEntries();
                        break;*/

                    case R.id.activity_main_nav_drawer_item_sync_notes:
                        showActionBarAndFullscreenOff();
                        final SyncFragment syncFragment = SyncFragment.newInstance();
                        getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.activity_main_fragment_container, syncFragment)
                                .addToBackStack(null)
                                .commit();
                    break;

                    case R.id.activity_main_nav_drawer_item_change_user:
                        showActionBarAndFullscreenOff();
                        final UserFragment userFragment = UserFragment.newInstance();
                        getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.activity_main_fragment_container, userFragment)
                                .addToBackStack(null)
                                .commit();
                        break;

                    case R.id.activity_main_nav_drawer_item_settings:
                        showActionBarAndFullscreenOff();
                        final SettingsPreferenceFragment settingsFragment
                                = SettingsPreferenceFragment.newInstance();
                        getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.activity_main_fragment_container, settingsFragment)
                                .addToBackStack(null)
                                .commit();
                        break;

                    default:
                        break;
                }

                // Close nav drawer after click.
                if (getResources().getConfiguration().orientation != ORIENTATION_LANDSCAPE) {
                    final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.activity_main_drawer_layout);
                    drawer.closeDrawer(GravityCompat.START);
                }
                return true;
            }
        };
    }

    // --Commented out by Inspection START:
    //    private void deleteAllEntries() {
    //        new Thread(new Runnable() {
    //            @Override
    //            public void run() {
    //                final boolean success = ListDbHelper.removeAllEntries(MainActivity.this);
    //                showToastRunOnUiThread(
    //                        success
    //                                ? getString(R.string.activity_main_notification_delete_all_entries_success)
    //                                : getString(R.string.activity_main_notification_delete_all_entries_failed),
    //                        Toast.LENGTH_SHORT);
    //            }
    //        }).start();
    //    }
    // --Commented out by Inspection STOP

    private void showActionBarAndFullscreenOff() {
        // Show Action bar.
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setElevation(
                    getResources().getDimension(R.dimen.activity_main_toolbar_elevation));
            actionBar.show();
        }

        // Fullscreen off.
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
    }


    /*
        Observer
     */

    @Override
    public void onNotify(final int resultCode, @Nullable final Bundle data) {
        switch (resultCode) {
            case RESULT_CODE_USER_SELECTED:
            case RESULT_CODE_USER_ADDED:
            case RESULT_CODE_USER_DELETED:
            case RESULT_CODE_USER_CHANGE_PREFERENCES:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initUserInfo(mNavView);
                    }
                });
            default:
                break;
        }
    }
}
