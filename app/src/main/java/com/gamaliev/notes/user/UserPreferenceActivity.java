package com.gamaliev.notes.user;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.shared_prefs.SpUsers;

import static com.gamaliev.notes.common.CommonUtils.showToast;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class UserPreferenceActivity extends AppCompatActivity {

    /* Logger */
    private static final String TAG = UserPreferenceActivity.class.getSimpleName();

    /* ... */
    private static final String EXTRA_USER_ID = "UserPreferenceActivity.EXTRA_USER_ID";
    @NonNull private String mUserId;


    /*
        Init
     */

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_preference);

        initUserId();
        initToolbar();

        if (savedInstanceState == null) {
            initPreferenceFragment();
        }
    }

    private void initUserId() {
        mUserId = getIntent().getStringExtra(EXTRA_USER_ID);
    }

    private void initToolbar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.activity_user_preference_toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void initPreferenceFragment() {
        final UserPreferenceFragment fragment = UserPreferenceFragment.getInstance(mUserId);

        getFragmentManager()
                .beginTransaction()
                .replace(R.id.activity_user_preference_fragment, fragment, null)
                .commit();
    }


    /*
        Options menu
     */

    /**
     * Inflate action bar menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_user_preference, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Action bar menu item selection handler
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            // Delete button
            case R.id.menu_user_preference_delete:
                showConfirmDeleteDialog();
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    /*
        Preference fragment
     */

    public static class UserPreferenceFragment extends PreferenceFragment {

        private static final String ARG_USER_ID = "userId";

        @NonNull private String mUserId;

        @NonNull
        public static UserPreferenceFragment getInstance(@NonNull final String userId) {
            final Bundle args = new Bundle();
            args.putString(ARG_USER_ID, userId);
            final UserPreferenceFragment fragment = new UserPreferenceFragment();
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mUserId = getArguments().getString(ARG_USER_ID);

            // Change preference name to current user.
            final PreferenceManager manager = getPreferenceManager();
            manager.setSharedPreferencesName(
                    SpUsers.getPreferencesName(mUserId));
            manager.setSharedPreferencesMode(MODE_PRIVATE);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preference_user);
        }
    }


    /*
        Intents
     */

    /**
     * Start intent.
     * @param context       Context.
     * @param requestCode   This code will be returned in onActivityResult() when the activity exits.
     * @param userId        User id.
     */
    public static void startIntent(
            @NonNull final Context context,
            final int requestCode,
            @NonNull final String userId) {

        Intent starter = new Intent(context, UserPreferenceActivity.class);
        starter.putExtra(EXTRA_USER_ID, userId);
        ((Activity) context).startActivityForResult(starter, requestCode);
    }


    /*
        ...
     */

    /**
     * Show confirm delete dialog with Ok, Cancel buttons.
     * If current user is default, then denied.
     */
    private void showConfirmDeleteDialog() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(UserPreferenceActivity.this);
        builder .setTitle(getString(R.string.activity_user_preference_delete_dialog_title))
                .setMessage(getString(R.string.activity_user_preference_delete_dialog_message))
                .setPositiveButton(getString(R.string.activity_user_preference_delete_dialog_button_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //
                                dialog.cancel();
                                //
                                if (SpUsers.SP_USERS_DEFAULT_USER_ID.equals(mUserId)) {
                                    showToast(
                                            getApplicationContext(),
                                            getString(R.string.activity_user_preference_delete_default_error),
                                            Toast.LENGTH_LONG);

                                } else {
                                    SpUsers.delete(getApplicationContext(), mUserId);
                                    finish();
                                }
                            }
                        })
                .setNegativeButton(
                        getString(R.string.activity_user_preference_delete_dialog_button_cancel),
                        null);

        final AlertDialog alert = builder.create();
        alert.show();
    }
}
