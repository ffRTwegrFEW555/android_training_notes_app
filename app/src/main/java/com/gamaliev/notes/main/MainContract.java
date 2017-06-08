package com.gamaliev.notes.main;

import android.app.Activity;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.gamaliev.notes.BasePresenter;
import com.gamaliev.notes.BaseView;

import java.util.Map;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

interface MainContract {

    interface View extends BaseView<Presenter> {

        void updateUserInfo(@NonNull Map<String, String> userProfile);
    }

    interface Presenter extends BasePresenter {

        void initUserInfo();

        void importEntriesAsync(@NonNull Activity activity, @NonNull Uri selectedFile);

        void exportEntriesAsync(@NonNull Activity activity, @NonNull Uri selectedFile);

        void addMockEntries(int entriesCount);
    }
}
