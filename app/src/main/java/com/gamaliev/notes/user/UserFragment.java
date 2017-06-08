package com.gamaliev.notes.user;

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
import com.gamaliev.notes.user.user_preference.UserPreferenceFragment;

import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_USER_ADDED;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_USER_SELECTED;
import static com.gamaliev.notes.common.observers.ObserverHelper.USERS;
import static com.gamaliev.notes.common.observers.ObserverHelper.notifyObservers;
import static com.gamaliev.notes.common.observers.ObserverHelper.registerObserver;
import static com.gamaliev.notes.common.observers.ObserverHelper.unregisterObserver;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

@SuppressWarnings("NullableProblems")
public class UserFragment extends Fragment
        implements Observer, UserContract.View {

    /* Observed */
    @NonNull private static final String[] OBSERVED = {USERS};

    /* ... */
    @NonNull private UserContract.Presenter mPresenter;
    @NonNull private View mParentView;
    @NonNull private RecyclerView mRecyclerView;


    /*
        Init
     */

    @NonNull
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
        initRecyclerView();
        initPresenter();
    }

    private void initTransition() {
        setExitTransition(new Fade());
        setEnterTransition(new Fade());
    }

    private void initActionBar() {
        final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.fragment_user));
        }
    }

    /**
     * Add new user.
     */
    private void initFabOnClickListener() {
        mParentView.findViewById(R.id.fragment_user_fab)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mPresenter.addNewUser();
                    }
                });
    }

    private void initRecyclerView() {
        mRecyclerView = (RecyclerView) mParentView.findViewById(R.id.fragment_user_rv);
        mRecyclerView.addItemDecoration(
                new DividerItemDecoration(
                        getActivity(),
                        DividerItemDecoration.VERTICAL));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

    private void initPresenter() {
        new UserPresenter(this);
        mPresenter.start();
    }


    /*
        Observer
     */

    @Override
    public void onNotify(final int resultCode, @Nullable final Bundle data) {
        switch (resultCode) {
            case RESULT_CODE_USER_SELECTED:
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                });
                break;
            default:
                break;
        }
    }


    /*
        UserContract.View
     */

    @Override
    public void setPresenter(@NonNull final UserContract.Presenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public boolean isActive() {
        return isAdded() && !isDetached();
    }

    @NonNull
    @Override
    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    @Override
    public void startUserPreferenceFragment(@NonNull final String newUserId) {
        final UserPreferenceFragment fragment
                = UserPreferenceFragment.newInstance(newUserId);
        getActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.activity_main_fragment_container, fragment)
                .addToBackStack(null)
                .commit();

        notifyObservers(USERS, RESULT_CODE_USER_ADDED, null);
    }


    /*
        Finish
     */

    private void finish() {
        getActivity().onBackPressed();
        notifyObservers(USERS, RESULT_CODE_USER_SELECTED, null);
    }
}
