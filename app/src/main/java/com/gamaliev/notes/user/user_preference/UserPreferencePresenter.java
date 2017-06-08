package com.gamaliev.notes.user.user_preference;

import android.content.Context;
import android.support.annotation.NonNull;

import com.gamaliev.notes.common.shared_prefs.SpUsers;

import static com.gamaliev.notes.app.NotesApp.getAppContext;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

class UserPreferencePresenter implements UserPreferenceContract.Presenter {

    /* ... */
    @NonNull private final Context mContext;
    @NonNull private final UserPreferenceContract.View mUserPreferenceView;


    /*
        Init
     */

    UserPreferencePresenter(@NonNull UserPreferenceContract.View userPreferenceView) {
        mContext = getAppContext();
        mUserPreferenceView = userPreferenceView;

        mUserPreferenceView.setPresenter(this);
    }


    /*
        UserPreferenceContract.Presenter
     */

    @Override
    public void start() {}

    @NonNull
    @Override
    public String getPreferenceName(@NonNull final String userId) {
        return SpUsers.getPreferencesName(userId);
    }

    @Override
    public void deleteUser(@NonNull final String userId) {
        if (SpUsers.SP_USERS_DEFAULT_USER_ID.equals(userId)) {
            mUserPreferenceView.onDeleteUserFailed();
        } else {
            SpUsers.delete(mContext, userId);
            mUserPreferenceView.onDeleteUserSuccess();
        }
    }
}
