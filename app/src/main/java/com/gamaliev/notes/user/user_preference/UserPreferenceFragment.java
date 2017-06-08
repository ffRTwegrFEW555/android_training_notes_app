package com.gamaliev.notes.user.user_preference;

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
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.gamaliev.notes.R;

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

@SuppressWarnings("NullableProblems")
public class UserPreferenceFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener, UserPreferenceContract.View {

    /* Logger */
    @NonNull private static final String TAG = UserPreferenceFragment.class.getSimpleName();

    /* ... */
    @NonNull private static final String EXTRA_USER_ID = "UserPreferenceFragment.EXTRA_USER_ID";
    @NonNull private UserPreferenceContract.Presenter mPresenter;
    @SuppressWarnings("NullableProblems")
    @NonNull private String mUserId;


    /*
        Init
    */

    /**
     * Get new instance of user preference fragment.
     * @param userId User id.
     * @return New instance of user preference fragment.
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
        // User id.
        final String userId = getArguments().getString(EXTRA_USER_ID);
        if (userId == null) {
            Log.e(TAG, "User id is null.");
            getActivity().onBackPressed();
            return;
        }
        mUserId = userId;

        // Presenter.
        new UserPreferencePresenter(this);

        // Change preference name to current user.
        final PreferenceManager manager = getPreferenceManager();
        manager.setSharedPreferencesName(mPresenter.getPreferenceName(mUserId));
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
                                mPresenter.deleteUser(mUserId);
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
        UserPreferenceContract.View
     */

    @Override
    public void setPresenter(@NonNull UserPreferenceContract.Presenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public boolean isActive() {
        return isAdded() && !isDetached();
    }

    @Override
    public void onDeleteUserFailed() {
        showToast(getString(R.string.fragment_user_preference_delete_default_error),
                Toast.LENGTH_LONG);
    }

    @Override
    public void onDeleteUserSuccess() {
        finishDeleted();
    }

    /*
        Finish
     */

    private void finishDeleted() {
        getActivity().onBackPressed();
        notifyObservers(USERS, RESULT_CODE_USER_DELETED, null);
    }
}
