package com.gamaliev.notes.user;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;

import com.gamaliev.notes.common.shared_prefs.SpUsers;

import java.util.Map;
import java.util.Set;

import static com.gamaliev.notes.app.NotesApp.getAppContext;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_USER_SELECTED;
import static com.gamaliev.notes.common.observers.ObserverHelper.USERS;
import static com.gamaliev.notes.common.observers.ObserverHelper.notifyObservers;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

@SuppressWarnings("NullableProblems")
class UserPresenter implements UserContract.Presenter {

    /* ... */
    @NonNull private final Context mContext;
    @NonNull private final UserContract.View mUserView;
    @NonNull private UserRecyclerViewAdapter mAdapter;
    @NonNull private String[] mProfiles;


    /*
        Init
     */

    UserPresenter(@NonNull UserContract.View userView) {
        mContext = getAppContext();
        mUserView = userView;

        mUserView.setPresenter(this);
    }


    /*
        UserContract.Presenter
     */

    @Override
    public void start() {
        initAdapter();
        initRecyclerView();
    }

    @Override
    public void addNewUser() {
        final String newUserId = SpUsers.add(mContext, null);
        mUserView.startUserPreferenceFragment(newUserId);
    }

    @Override
    public int getItemCount() {
        return mProfiles.length;
    }

    @NonNull
    @Override
    public String getUserId(final int position) {
        return mProfiles[position];
    }

    @NonNull
    @Override
    public Map<String, String> getUserProfile(@NonNull final String userId) {
        return SpUsers.get(mContext, userId);
    }

    @Nullable
    @Override
    public String getSelectedUserId() {
        return SpUsers.getSelected(mContext);
    }

    @Override
    public void selectUser(@NonNull final String userId) {
        SpUsers.setSelected(mContext, userId);
        notifyObservers(USERS, RESULT_CODE_USER_SELECTED, null);
    }

    @Override
    public void startUserPreferenceFragment(@NonNull final String userId) {
        mUserView.startUserPreferenceFragment(userId);
    }


    /*
        ...
     */

    private void initAdapter() {
        mAdapter = new UserRecyclerViewAdapter(this);
    }

    private void initRecyclerView() {
        updateProfiles();
        final RecyclerView rv = mUserView.getRecyclerView();
        rv.setAdapter(mAdapter);
    }

    private void updateProfiles() {
        final Set<String> profiles = SpUsers.getProfiles(mContext);
        mProfiles = new String[profiles.size()];
        profiles.toArray(mProfiles);
    }
}
