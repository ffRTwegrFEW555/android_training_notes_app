package com.gamaliev.notes.item_details.pager_item;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.gamaliev.notes.BasePresenter;
import com.gamaliev.notes.BaseView;
import com.gamaliev.notes.entity.ListEntry;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

interface ItemDetailsPagerItemContract {

    interface View extends BaseView<Presenter> {

        void showProgressBarAndHideMenuItems();

        void refreshEntry();

        void performSuccessLoadingImage();

        void performErrorLoadingImage();

        void finish(@NonNull String action);
    }

    interface Presenter extends BasePresenter {

        @NonNull
        ListEntry initializeNewEntry();

        @Nullable
        ListEntry initializeEntryFromDb();

        @Nullable
        ListEntry getEntry();

        void setEntryId(long entryId);

        long getEntryId();

        void refreshEntry(
                @NonNull String title,
                @NonNull String description,
                @NonNull String imageUrl);

        void updateEntryViewed();

        void loadImage(@NonNull ImageView imageView,
                       @NonNull String pathToImage);

        void onSaveInstanceState(@NonNull Bundle outState);

        void setEntryFromSavedInstanceStateOrDefault(@Nullable Bundle savedInstanceState);

        void startActionAsyncTask(@NonNull String action);
    }
}
