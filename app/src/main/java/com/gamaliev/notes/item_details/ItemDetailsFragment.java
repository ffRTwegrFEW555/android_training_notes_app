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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.shared_prefs.SpFilterProfiles;
import com.gamaliev.notes.list.db.ListDbHelper;

import java.util.Map;

import static com.gamaliev.notes.common.db.DbHelper.BASE_COLUMN_ID;
import static com.gamaliev.notes.common.shared_prefs.SpCommon.convertJsonToMap;

public final class ItemDetailsFragment extends Fragment {

    /* Extra */
    private static final String EXTRA_ID = "ItemDetailsFragment.EXTRA_ID";

    /* ... */
    private static final int OFFSCREEN_PAGE_LIMIT = 5;

    @NonNull private View mParentView;
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

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mId = getArguments().getLong(EXTRA_ID);
    }

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
        return mParentView;
    }

    @Override
    public void onViewCreated(
            final View view,
            @Nullable final Bundle savedInstanceState) {

        init();
    }


    /*
        ...
     */

    private void init() {
        initViewPager();
        initActionBat();
    }

    private void initViewPager() {

        final Map<String, String> currentFilter = convertJsonToMap(
                SpFilterProfiles.getSelectedForCurrentUser(getContext()));
        final Cursor cursor = ListDbHelper.getCursorWithParams(
                getContext(),
                "",
                currentFilter);

        int startPosition = 0;
        if (cursor != null) {
            cursor.moveToFirst();
            startPosition = cursor.getPosition();
            do {
                final long id = cursor.getLong(cursor.getColumnIndex(BASE_COLUMN_ID));
                if (mId == id) {
                    startPosition = cursor.getPosition();
                    break;
                }
            } while (cursor.moveToNext());
        }

        final FragmentStatePagerAdapter adapter =
                new ItemDetailsPagerAdapter(
                        getChildFragmentManager(),
                        this,
                        cursor);

        final ViewPager viewPager =
                (ViewPager) mParentView.findViewById(R.id.fragment_item_details_vp);
        viewPager.setOffscreenPageLimit(OFFSCREEN_PAGE_LIMIT);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(startPosition);
    }

    private void initActionBat() {
        final ActionBar actionBar =
                ((AppCompatActivity) getActivity()).getSupportActionBar();

        if (actionBar != null) {
            actionBar.setElevation(0);
        }
    }
}