package com.gamaliev.notes.item_details;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;

import com.gamaliev.notes.BasePresenter;
import com.gamaliev.notes.BaseView;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public interface ItemDetailsContract {

    interface View extends BaseView<Presenter> {

        @NonNull
        FragmentManager getChildFragmentManager();

        @NonNull
        ViewPager getViewPager();

        void performError(@NonNull String text);
    }

    interface Presenter extends BasePresenter {

        long getIdByPosition(int position);

        int getCount();

        void onDestroyView();
    }
}
