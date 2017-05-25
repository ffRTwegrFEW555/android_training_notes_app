package com.gamaliev.notes.item_details;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.gamaliev.notes.R;

import static com.gamaliev.notes.common.db.DbHelper.BASE_COLUMN_ID;
import static com.gamaliev.notes.item_details.ItemDetailsPagerItemFragment.ACTION_EDIT;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class ItemDetailsPagerAdapter extends FragmentStatePagerAdapter {

    /* Logger */
    @SuppressWarnings("unused")
    private static final String TAG = ItemDetailsPagerAdapter.class.getSimpleName();

    /* ... */
    @NonNull private final Fragment mFragment;
    @Nullable private Cursor mCursor;


    /*
        Init
     */

    public ItemDetailsPagerAdapter(
            @NonNull final FragmentManager fm,
            @NonNull final Fragment fragment,
            @Nullable final Cursor cursor) {

        super(fm);
        mFragment = fragment;
        mCursor = cursor;
    }


    /*
        Lifecycle
     */

    @Override
    public Fragment getItem(final int position) {
        if (mCursor != null && mCursor.moveToPosition(position)) {
            final long id = getId(position);
            return ItemDetailsPagerItemFragment.newInstance(ACTION_EDIT, id);
        }
        return null;
    }

    @Override
    public int getCount() {
        return mCursor == null ? 0 : mCursor.getCount();
    }

    @Override
    public CharSequence getPageTitle(final int position) {
        final long id = getId(position);
        return id == -1
                ? ""
                :   mFragment.getString(R.string.fragment_item_details_pager_title_strip_prefix)
                    + ": "
                    + String.valueOf(id);
    }


    /*
        ...
     */

    private long getId(final int position) {
        if (mCursor != null && mCursor.moveToPosition(position)) {
            return mCursor.getLong(
                    mCursor.getColumnIndex(BASE_COLUMN_ID));
        }
        return -1;
    }

    public void setCursor(@NonNull final Cursor cursor) {
        mCursor = cursor;
    }
}
