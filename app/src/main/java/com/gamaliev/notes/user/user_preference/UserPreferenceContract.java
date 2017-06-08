package com.gamaliev.notes.user.user_preference;

import android.support.annotation.NonNull;

import com.gamaliev.notes.BasePresenter;
import com.gamaliev.notes.BaseView;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public interface UserPreferenceContract {

    interface View extends BaseView<Presenter> {

        void onDeleteUserFailed();

        void onDeleteUserSuccess();
    }

    interface Presenter extends BasePresenter {

        @NonNull
        String getPreferenceName(@NonNull String userId);

        void deleteUser(@NonNull String userId);
    }
}
