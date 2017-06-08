package com.gamaliev.notes.item_details;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.gamaliev.notes.R;
import com.gamaliev.notes.list.db.ListDbHelper;

import java.util.Map;

import static com.gamaliev.notes.app.NotesApp.getAppContext;
import static com.gamaliev.notes.common.db.DbHelper.BASE_COLUMN_ID;
import static com.gamaliev.notes.common.db.DbHelper.findCursorPositionByColumnValue;
import static com.gamaliev.notes.common.db.DbHelper.getDbFailMessage;
import static com.gamaliev.notes.common.shared_prefs.SpCommon.convertJsonToMap;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.getSelectedForCurrentUser;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

class ItemDetailsPresenter implements ItemDetailsContract.Presenter {

    /* Logger */
    @NonNull private static final String TAG = ItemDetailsPresenter.class.getSimpleName();

    /* ... */
    private static final int OFFSCREEN_PAGE_LIMIT = 5;

    @NonNull private final Context mContext;
    @NonNull private final ItemDetailsContract.View mItemDetailsView;
    @Nullable private Cursor mCursor;
    private long mInitialEntryId;


    /*
        Init
     */

    ItemDetailsPresenter(
            @NonNull final ItemDetailsContract.View itemDetailsView,
            final long initialEntryId) {

        mContext = getAppContext();
        mItemDetailsView = itemDetailsView;
        mInitialEntryId = initialEntryId;

        mItemDetailsView.setPresenter(this);
    }


    /*
        ItemDetailsContract.Presenter
     */

    @Override
    public void start() {
        initViewPager();
    }

    @Override
    public long getIdByPosition(int position) {
        if (mCursor != null && mCursor.moveToPosition(position)) {
            return mCursor.getLong(mCursor.getColumnIndex(BASE_COLUMN_ID));
        }
        return -1;
    }

    @Override
    public int getCount() {
        return mCursor == null ? 0 : mCursor.getCount();
    }

    @Override
    public void onDestroyView() {
        closeCursor();
    }


    /*
        ...
     */

    private void initViewPager() {
        final String profileNotFoundError = mContext.getString(
                R.string.fragment_item_details_edit_mode_filter_profile_not_found);

        final String filterProfile = getSelectedForCurrentUser(mContext);
        if (filterProfile == null) {
            Log.e(TAG, profileNotFoundError);
            mItemDetailsView.performError(profileNotFoundError);
            return;
        }
        final Map<String, String> filterProfileMap = convertJsonToMap(filterProfile);
        if (filterProfileMap == null) {
            Log.e(TAG, profileNotFoundError);
            mItemDetailsView.performError(profileNotFoundError);
            return;
        }

        mCursor = ListDbHelper.getCursorWithParams(mContext, "", filterProfileMap);
        if (mCursor == null) {
            Log.e(TAG, "Cursor is null.");
            mItemDetailsView.performError(getDbFailMessage());
            return;
        }

        int startPosition = findCursorPositionByColumnValue(
                mCursor,
                BASE_COLUMN_ID,
                String.valueOf(mInitialEntryId));

        final FragmentStatePagerAdapter adapter =
                new ItemDetailsPagerAdapter(this, mItemDetailsView.getChildFragmentManager());

        final ViewPager viewPager = mItemDetailsView.getViewPager();
        viewPager.setOffscreenPageLimit(OFFSCREEN_PAGE_LIMIT);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(startPosition);
    }

    private void closeCursor() {
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
    }
}
