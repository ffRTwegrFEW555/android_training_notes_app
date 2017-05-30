package com.gamaliev.notes.user;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.transition.Fade;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.shared_prefs.SpUsers;

import static android.content.Context.MODE_PRIVATE;
import static com.gamaliev.notes.common.CommonUtils.showToast;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_USER_CHANGE_PREFERENCES;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_USER_DELETED;
import static com.gamaliev.notes.common.observers.ObserverHelper.USERS;
import static com.gamaliev.notes.common.observers.ObserverHelper.notifyObservers;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class UserPreferenceFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    /* ... */
    private static final String EXTRA_USER_ID = "UserPreferenceFragment.EXTRA_USER_ID";
    @NonNull private String mUserId;


    /*
        Init
    */

    @NonNull
    public static UserPreferenceFragment newInstance(@NonNull final String userId) {
        final Bundle args = new Bundle();
        args.putString(EXTRA_USER_ID, userId);

        final UserPreferenceFragment fragment = new UserPreferenceFragment();
        fragment.setArguments(args);
        return fragment;
    }


    /*
        Lifecycle
    */

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        mUserId = getArguments().getString(EXTRA_USER_ID);

        // Change preference name to current user.
        final PreferenceManager manager = getPreferenceManager();
        manager.setSharedPreferencesName(SpUsers.getPreferencesName(mUserId));
        manager.setSharedPreferencesMode(MODE_PRIVATE);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference_user);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initTransition();
        initActionBar();
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager()
                .getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onPause() {
        getPreferenceManager()
                .getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }


    /*
        Options menu
     */

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_user_preference, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_user_preference_delete:
                showConfirmDeleteDialog();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    /*
        ...
     */

    private void initTransition() {
        setExitTransition(new Fade());
        setEnterTransition(new Fade());
    }

    private void initActionBar() {
        final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.fragment_user_preference));
        }
        setHasOptionsMenu(true);
    }

    private void showConfirmDeleteDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder .setTitle(getString(R.string.fragment_user_preference_delete_dialog_title))
                .setMessage(getString(R.string.fragment_user_preference_delete_dialog_message))
                .setPositiveButton(getString(R.string.fragment_user_preference_delete_dialog_button_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                if (SpUsers.SP_USERS_DEFAULT_USER_ID.equals(mUserId)) {
                                    showToast(
                                            getContext(),
                                            getString(R.string.fragment_user_preference_delete_default_error),
                                            Toast.LENGTH_LONG);

                                } else {
                                    SpUsers.delete(getContext(), mUserId);
                                    finishDeleted();
                                }
                            }
                        })
                .setNegativeButton(
                        getString(R.string.fragment_user_preference_delete_dialog_button_cancel),
                        null)
                .create()
                .show();
    }


    /*
        SharedPreferences.OnSharedPreferenceChangeListener
     */

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        setPreferenceScreen(null);
        addPreferencesFromResource(R.xml.preference_user);
        notifyObservers(USERS, RESULT_CODE_USER_CHANGE_PREFERENCES, null);
    }


    /*
        Finish
     */

    private void finishDeleted() {
        getActivity().onBackPressed();
        notifyObservers(USERS, RESULT_CODE_USER_DELETED, null);
    }
}
