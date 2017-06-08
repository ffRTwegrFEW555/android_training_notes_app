package com.gamaliev.notes.item_details;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.transition.Fade;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.gamaliev.notes.R;

import static com.gamaliev.notes.common.CommonUtils.showToastRunOnUiThread;

@SuppressWarnings("NullableProblems")
public final class ItemDetailsFragment extends Fragment implements ItemDetailsContract.View {

    /* Extra */
    @NonNull private static final String EXTRA_ID = "ItemDetailsFragment.EXTRA_ID";

    /* ... */
    @NonNull private View mParentView;
    @NonNull private ItemDetailsContract.Presenter mPresenter;


    /*
        Init
     */

    /**
     * Get new instance of item details fragment.
     * @param initialEntryId Initial entry id.
     * @return New instance of item details fragment.
     */
    @NonNull
    public static ItemDetailsFragment newInstance(final long initialEntryId) {
        final Bundle bundle = new Bundle();
        bundle.putLong(EXTRA_ID, initialEntryId);

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
        mPresenter.onDestroyView();
        super.onDestroyView();
    }


    /*
        ...
     */

    private void init() {
        initTransition();
        initActionBar();
        initPresenter();
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

    private void initPresenter() {
        final long initialEntryId = getArguments().getLong(EXTRA_ID, -1);
        if (initialEntryId == -1) {
            performError(getContext().getString(R.string.fragment_item_details_edit_mode_wrong_id));
            return;
        }

        new ItemDetailsPresenter(this, initialEntryId);
        mPresenter.start();
    }


    /*
        ItemDetailsContract.View
     */

    @Override
    public void setPresenter(@NonNull final ItemDetailsContract.Presenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public boolean isActive() {
        return isAdded() && !isDetached();
    }

    @NonNull
    @Override
    public ViewPager getViewPager() {
        return (ViewPager) mParentView.findViewById(R.id.fragment_item_details_vp);
    }

    @Override
    public void performError(@NonNull final String text) {
        showToastRunOnUiThread(text, Toast.LENGTH_LONG);
        getActivity().onBackPressed();
    }
}