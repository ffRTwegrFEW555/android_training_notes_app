package com.gamaliev.notes.list;

import android.database.Cursor;
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

public interface ListContract {

    interface View extends BaseView<Presenter> {

        void showFoundNotification(int itemsCount);

        @NonNull
        FragmentManager getSupportFragmentManager();
    }

    interface Presenter extends BasePresenter {

        void loadFilterProfile();

        void updateAdapter(@NonNull String text);

        void initRecyclerView(@NonNull RecyclerView rv);

        @Nullable
        Cursor getCursor();

        int getItemCount();

        boolean swapItems(int from, int to);

        @NonNull
        FragmentManager getSupportFragmentManager();

        void onDestroyView();
    }
}
