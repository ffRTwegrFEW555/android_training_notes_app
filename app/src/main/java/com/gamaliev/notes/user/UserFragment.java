package com.gamaliev.notes.user;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import com.gamaliev.notes.common.shared_prefs.SpUsers;

import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_USER_DELETED;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_USER_SELECTED;
import static com.gamaliev.notes.common.observers.ObserverHelper.USERS;
import static com.gamaliev.notes.common.observers.ObserverHelper.notifyObservers;
import static com.gamaliev.notes.common.observers.ObserverHelper.registerObserver;
import static com.gamaliev.notes.common.observers.ObserverHelper.unregisterObserver;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class UserFragment extends Fragment implements Observer {

    /* Observed */
    @NonNull public static final String[] OBSERVED = {USERS};

    /* ... */
    @NonNull private View mParentView;
    @NonNull private RecyclerView mRecyclerView;
    @NonNull private UserRecyclerViewAdapter mAdapter;


    /*
        Init
     */

    public static UserFragment newInstance() {
        return new UserFragment();
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
                R.layout.fragment_user,
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
        initFabOnClickListener();
        initAdapter();
        initRecyclerView();
    }

    private void initTransition() {
        setExitTransition(new Fade());
        setEnterTransition(new Fade());
    }

    private void initActionBar() {
        ((AppCompatActivity) getActivity())
                .getSupportActionBar()
                .setTitle(getString(R.string.fragment_user));
    }

    /**
     * Add new user.
     */
    private void initFabOnClickListener() {
        mParentView.findViewById(R.id.fragment_user_fab)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String newUserId = SpUsers.add(getContext(), null);
                        final UserPreferenceFragment fragment
                                = UserPreferenceFragment.newInstance(newUserId);
                        getActivity()
                                .getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.activity_main_fragment_container, fragment)
                                .addToBackStack(null)
                                .commit();
                    }
                });
    }


    /*
        RecyclerView & Adapter
     */

    private void initAdapter() {
        mAdapter = new UserRecyclerViewAdapter(this);
    }

    private void initRecyclerView() {
        mRecyclerView = (RecyclerView) mParentView.findViewById(R.id.fragment_user_rv);
        mRecyclerView.addItemDecoration(
                new DividerItemDecoration(
                        getActivity(),
                        DividerItemDecoration.VERTICAL));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(mAdapter);
    }

    private void updateAdapter() {
        initAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
    }


    /*
        Observer
     */

    @Override
    public void onNotify(int resultCode, @Nullable Bundle data) {
        switch (resultCode) {
            case RESULT_CODE_USER_SELECTED:
                finish();
                break;
            case RESULT_CODE_USER_DELETED:
                updateAdapter();
                break;
            default:
                break;
        }
    }


    /*
        Finish
     */

    private void finish() {
        getActivity().onBackPressed();
        notifyObservers(USERS, RESULT_CODE_USER_SELECTED, null);
    }
}
