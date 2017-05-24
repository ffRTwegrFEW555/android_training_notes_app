package com.gamaliev.notes.item_details;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.db.DbQueryBuilder;

import static com.gamaliev.notes.common.db.DbHelper.BASE_COLUMN_ID;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_TABLE_NAME;
import static com.gamaliev.notes.common.db.DbHelper.getEntries;

public final class ItemDetailsFragment extends Fragment {

    /* Logger */
    private static final String TAG = ItemDetailsFragment.class.getSimpleName();

    /* Action */
    public static final String ACTION      = "ItemDetailsFragment.ACTION";
    public static final String ACTION_EDIT = "ItemDetailsFragment.ACTION_EDIT";

    /* Extra */
    private static final String EXTRA_ID    = "ItemDetailsFragment.EXTRA_ID";

    /* ... */
    private static final int OFFSCREEN_PAGE_LIMIT = 3;

    @NonNull private View mParentView;
    @NonNull private ActionBar mActionBar;
    @NonNull private ViewPager mViewPager;
    @NonNull private FragmentStatePagerAdapter mPagerAdapter;
    @NonNull private String mAction;
    private long mId;


    /*
        Init
     */

    public static ItemDetailsFragment newInstance(
            @NonNull final String action,
            final long id) {

        final Bundle bundle = new Bundle();
        bundle.putString(ACTION, action);
        bundle.putLong(EXTRA_ID, id);

        final ItemDetailsFragment fragment = new ItemDetailsFragment();
        fragment.setArguments(bundle);
        
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAction = getArguments().getString(ACTION);
        mId = getArguments().getLong(EXTRA_ID);
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        mParentView = inflater.inflate(
                R.layout.fragment_item_details,
                container,
                false);

        return mParentView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        init();
    }

    private void init() {
        initViewPager();
    }

    private void initViewPager() {
        final DbQueryBuilder queryBuilder = new DbQueryBuilder();
        queryBuilder.setOrder(BASE_COLUMN_ID);
        final Cursor cursor = getEntries(
                getContext(),
                LIST_ITEMS_TABLE_NAME,
                queryBuilder);
        cursor.moveToFirst();
        int startPosition = cursor.getPosition();
        do {
            final long id = cursor.getLong(cursor.getColumnIndex(BASE_COLUMN_ID));
            if (mId == id) {
                startPosition = cursor.getPosition();
                break;
            }
        } while (cursor.moveToNext());

        //
        mPagerAdapter = new ItemDetailsPagerAdapter(
                getActivity().getSupportFragmentManager(),
                this,
                cursor);

        //
        mViewPager = (ViewPager) mParentView.findViewById(R.id.fragment_item_details_vp);
        mViewPager.setOffscreenPageLimit(OFFSCREEN_PAGE_LIMIT);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setCurrentItem(startPosition);
    }


    /*
        Callbacks
     */

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {}
}