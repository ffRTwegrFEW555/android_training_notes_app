package com.gamaliev.notes.conflict;

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

        RecyclerView getRecyclerView();

        FragmentManager getFragmentManager();
    }

    interface Presenter extends BasePresenter {

        void updateRecyclerView(int deletedPosition);

        String getSyncId(int position);

        int getItemCount();

        FragmentManager getFragmentManager();

        void onDestroyView();

        void onDetachedFromRecyclerView();
    }
}
