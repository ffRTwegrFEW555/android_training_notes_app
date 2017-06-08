package com.gamaliev.notes.user;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;

import com.gamaliev.notes.BasePresenter;
import com.gamaliev.notes.BaseView;

import java.util.Map;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

interface UserContract {

    interface View extends BaseView<Presenter> {

        @NonNull
        RecyclerView getRecyclerView();

        void startUserPreferenceFragment(@NonNull final String newUserId);
    }

    interface Presenter extends BasePresenter {

        void addNewUser();

        int getItemCount();

        @NonNull
        String getUserId(int position);

        @NonNull
        Map<String, String> getUserProfile(@NonNull String userId);

        @Nullable
        String getSelectedUserId();

        void selectUser(@NonNull String userId);

        void startUserPreferenceFragment(@NonNull String userId);
    }
}
