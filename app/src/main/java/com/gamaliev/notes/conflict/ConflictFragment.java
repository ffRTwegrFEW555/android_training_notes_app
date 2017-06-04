package com.gamaliev.notes.conflict;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.Fade;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.observers.Observer;

import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_CONFLICTED_SUCCESS;
import static com.gamaliev.notes.common.observers.ObserverHelper.CONFLICT;
import static com.gamaliev.notes.common.observers.ObserverHelper.registerObserver;
import static com.gamaliev.notes.common.observers.ObserverHelper.unregisterObserver;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

@SuppressWarnings("NullableProblems")
public class ConflictFragment extends Fragment implements Observer {

    /* Observed */
    @NonNull private static final String[] OBSERVED = {CONFLICT};

    /* ... */
    public static final String EXTRA_CONFLICT_SELECT_POSITION = "position";

    @NonNull private View mParentView;
    @NonNull private ConflictRecyclerViewAdapter mAdapter;


    /*
        Init
     */

    @NonNull
    public static ConflictFragment newInstance() {
        return new ConflictFragment();
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
                R.layout.fragment_conflict,
                container,
                false);
        init();
        return mParentView;
    }

    @Override
    public void onResume() {
        registerObserver(OBSERVED, toString(), this);
        super.onResume();
    }

    @Override
    public void onPause() {
        unregisterObserver(OBSERVED, toString());
        super.onPause();
    }


    /*
        ...
     */

    private void init() {
        initTransition();
        initActionBar();
        initAdapter();
        initRecyclerView();
    }

    private void initTransition() {
        setExitTransition(new Fade());
        setEnterTransition(new Fade());
    }

    private void initActionBar() {
        final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.fragment_conflict));
        }
    }


    /*
        RecyclerView & Adapter
     */

    private void initAdapter() {
        mAdapter = new ConflictRecyclerViewAdapter(this);
    }

    private void initRecyclerView() {
        final RecyclerView rv = (RecyclerView) mParentView.findViewById(R.id.fragment_conflict_rv);
        rv.addItemDecoration(
                new DividerItemDecoration(
                        getActivity(),
                        DividerItemDecoration.VERTICAL));
        rv.setLayoutManager(new LinearLayoutManager(getActivity()));
        rv.setAdapter(mAdapter);
    }

    private void updateAdapter(final int deletedPosition) {
        mAdapter.updateCursor();
        mAdapter.notifyItemRemoved(deletedPosition);
        mAdapter.notifyItemRangeChanged(deletedPosition, mAdapter.getItemCount());
    }


    /*
        Observer
     */

    @Override
    public void onNotify(final int resultCode, @Nullable final Bundle data) {
        switch (resultCode) {
            case RESULT_CODE_CONFLICTED_SUCCESS:
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (data != null) {
                            updateAdapter(data.getInt(EXTRA_CONFLICT_SELECT_POSITION));
                        }
                    }
                });
                break;
            default:
                break;
        }
    }
}
