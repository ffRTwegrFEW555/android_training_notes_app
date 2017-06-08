package com.gamaliev.notes.item_details;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.gamaliev.notes.R;
import com.gamaliev.notes.item_details.pager_item.ItemDetailsPagerItemFragment;

import static com.gamaliev.notes.app.NotesApp.getAppContext;
import static com.gamaliev.notes.item_details.pager_item.ItemDetailsPagerItemFragment.ACTION_EDIT;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

class ItemDetailsPagerAdapter extends FragmentStatePagerAdapter {

    /* ... */
    @NonNull private final Context mContext;
    @NonNull private final ItemDetailsContract.Presenter mPresenter;


    /*
        Init
     */

    ItemDetailsPagerAdapter(
            @NonNull final ItemDetailsContract.Presenter presenter,
            @NonNull final FragmentManager fm) {

        super(fm);
        mContext = getAppContext();
        mPresenter = presenter;
    }


    /*
        Lifecycle
     */

    @Override
    public Fragment getItem(final int position) {
        final long id = mPresenter.getIdByPosition(position);
        if (id == -1) {
            return null;
        }
        return ItemDetailsPagerItemFragment.newInstance(ACTION_EDIT, id);
    }

    @Override
    public int getCount() {
        return mPresenter.getCount();
    }

    @Override
    public CharSequence getPageTitle(final int position) {
        final long id = mPresenter.getIdByPosition(position);
        return id == -1
                ? ""
                : mContext.getString(R.string.fragment_item_details_pager_title_strip_prefix)
                    + ": "
                    + String.valueOf(id);
    }
}
