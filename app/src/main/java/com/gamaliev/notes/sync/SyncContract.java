package com.gamaliev.notes.sync;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;

import com.gamaliev.notes.BasePresenter;
import com.gamaliev.notes.BaseView;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

interface SyncContract {

    interface View extends BaseView<Presenter> {

        @NonNull
        RecyclerView getRecyclerView();

        void scrollRecyclerViewToBottom(int position);

        void showConflictWarning();

        void hideConflictWarning();

        void onSuccessClearJournal();

        void onFailedClearJournal();
    }

    interface Presenter extends BasePresenter {

        void synchronize();

        void deleteAllFromServerAsync();

        void clearJournal();

        void updateAdapter();

        @Nullable
        Cursor getCursor();

        int getItemCount();

        void onDestroyView();

        void onDetachedFromRecyclerView();
    }
}
