package com.gamaliev.notes.item_details;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.transition.Fade;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gamaliev.notes.R;
import com.gamaliev.notes.list.db.ListDbHelper;

import java.util.Map;

import static com.gamaliev.notes.common.db.DbHelper.BASE_COLUMN_ID;
import static com.gamaliev.notes.common.db.DbHelper.findCursorPositionByColumnValue;
import static com.gamaliev.notes.common.shared_prefs.SpCommon.convertJsonToMap;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.getSelectedForCurrentUser;

public final class ItemDetailsFragment extends Fragment {

    /* Logger */
    private static final String TAG = ItemDetailsFragment.class.getSimpleName();

    /* Extra */
    private static final String EXTRA_ID = "ItemDetailsFragment.EXTRA_ID";

    /* ... */
    private static final int OFFSCREEN_PAGE_LIMIT = 5;

    @SuppressWarnings("NullableProblems")
    @NonNull private View mParentView;
    @Nullable private Cursor mCursor;
    private long mId;


    /*
        Init
     */

    public static ItemDetailsFragment newInstance(final long id) {
        final Bundle bundle = new Bundle();
        bundle.putLong(EXTRA_ID, id);

        final ItemDetailsFragment fragment = new ItemDetailsFragment();
        fragment.setArguments(bundle);
        return fragment;
    }


    /*
        Lifecycle
     */

    @Nullable
    @Override
    public View onCreateView(
            final LayoutInflater inflater,
            @Nullable final ViewGroup container,
            @Nullable final Bundle savedInstanceState) {

        mParentView = inflater.inflate(
                R.layout.fragment_item_details,
                container,
                false);
        init();
        return mParentView;
    }

    @Override
    public void onDestroyView() {
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
        super.onDestroyView();
    }

    /*
        ...
     */

    private void init() {
        initArgs();
        initTransition();
        initActionBar();
        initViewPager();
    }

    private void initArgs() {
        mId = getArguments().getLong(EXTRA_ID);
    }

    private void initTransition() {
        setExitTransition(new Fade());
        setEnterTransition(new Fade());
    }

    private void initActionBar() {
        final ActionBar actionBar =
                ((AppCompatActivity) getActivity()).getSupportActionBar();

        if (actionBar != null) {
            actionBar.setElevation(0);
        }
    }

    private void initViewPager() {
        final String filterProfile = getSelectedForCurrentUser(getContext());
        if (filterProfile == null) {
            Log.e(TAG, "Filter profile for current user is null.");
            getActivity().onBackPressed();
            return;
        }
        final Map<String, String> filterProfileMap = convertJsonToMap(filterProfile);
        if (filterProfileMap == null) {
            Log.e(TAG, "Filter profile for current user is null.");
            getActivity().onBackPressed();
            return;
        }

        mCursor = ListDbHelper.getCursorWithParams(
                getContext(),
                "",
                filterProfileMap);

        int startPosition = 0;
        if (mCursor != null) {
            startPosition = findCursorPositionByColumnValue(
                    mCursor,
                    BASE_COLUMN_ID,
                    String.valueOf(mId));
        }

        final FragmentStatePagerAdapter adapter =
                new ItemDetailsPagerAdapter(
                        getChildFragmentManager(),
                        this,
                        mCursor);

        final ViewPager viewPager =
                (ViewPager) mParentView.findViewById(R.id.fragment_item_details_vp);
        viewPager.setOffscreenPageLimit(OFFSCREEN_PAGE_LIMIT);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(startPosition);
    }
}