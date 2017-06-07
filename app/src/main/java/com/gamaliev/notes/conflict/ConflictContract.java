package com.gamaliev.notes.conflict;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;

import com.gamaliev.notes.BasePresenter;
import com.gamaliev.notes.BaseView;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public interface ConflictContract {

    interface View extends BaseView<Presenter> {

        @NonNull
        RecyclerView getRecyclerView();

        @NonNull
        FragmentManager getFragmentManager();
    }

    interface Presenter extends BasePresenter {

        /**
         * Updating cursor, notifying adapter of recycler view, animation of deleted entry.
         * @param deletedPosition Deleted position of adapter of recycler view.
         */
        void updateRecyclerView(int deletedPosition);

        @Nullable
        String getSyncId(int position);

        int getItemCount();

        @NonNull
        FragmentManager getFragmentManager();

        void onDestroyView();

        void onDetachedFromRecyclerView();
    }
}
