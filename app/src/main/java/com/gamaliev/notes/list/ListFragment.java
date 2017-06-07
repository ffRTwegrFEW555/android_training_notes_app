package com.gamaliev.notes.list;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.transition.AutoTransition;
import android.transition.Fade;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.observers.Observer;
import com.gamaliev.notes.item_details.pager_item.ItemDetailsPagerItemFragment;
import com.gamaliev.notes.list.filter_sort_dialog.FilterSortDialogFragment;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.gamaliev.notes.common.CommonUtils.EXTRA_REVEAL_ANIM_CENTER_CENTER;
import static com.gamaliev.notes.common.CommonUtils.circularRevealAnimationOff;
import static com.gamaliev.notes.common.CommonUtils.circularRevealAnimationOn;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_ENTRY_ADDED;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_ENTRY_DELETED;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_ENTRY_EDITED;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_LIST_FILTERED;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_MOCK_ENTRIES_ADDED;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_NOTES_IMPORTED;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_SYNC_SUCCESS;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_USER_DELETED;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_USER_SELECTED;
import static com.gamaliev.notes.common.observers.ObserverHelper.ENTRIES_MOCK;
import static com.gamaliev.notes.common.observers.ObserverHelper.ENTRY;
import static com.gamaliev.notes.common.observers.ObserverHelper.FILE_IMPORT;
import static com.gamaliev.notes.common.observers.ObserverHelper.LIST_FILTER;
import static com.gamaliev.notes.common.observers.ObserverHelper.SYNC;
import static com.gamaliev.notes.common.observers.ObserverHelper.registerObserver;
import static com.gamaliev.notes.common.observers.ObserverHelper.unregisterObserver;
import static com.gamaliev.notes.item_details.pager_item.ItemDetailsPagerItemFragment.ACTION_ADD;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

@SuppressWarnings("NullableProblems")
public class ListFragment extends Fragment
        implements Observer, ListContract.View {

    /* Observed */
    @NonNull private static final String[] OBSERVED = {
            FILE_IMPORT,
            LIST_FILTER,
            ENTRY,
            ENTRIES_MOCK,
            SYNC};

    /* ... */
    @NonNull private ListContract.Presenter mPresenter;
    @NonNull private View mParentView;
    @NonNull private Button mFoundView;
    @NonNull private SearchView mSearchView;
    private long mTimerFound;


    /*
        Init
     */

    @NonNull
    public static ListFragment newInstance() {
        return new ListFragment();
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
                R.layout.fragment_list,
                container,
                false);
        init();
        return mParentView;
    }

    @Override
    public void onResume() {
        mPresenter.loadFilterProfile();
        updateAdapter();
        registerObserver(OBSERVED, toString(), this);
        super.onResume();
    }

    @Override
    public void onPause() {
        unregisterObserver(OBSERVED, toString());
        super.onPause();
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
        initPresenter();
        initTransition();
        initActionBar();
        initFabOnClickListener();
        initFoundView();
        initRecyclerView();
    }

    private void initTransition() {
        setExitTransition(new Fade());
        setEnterTransition(new Fade());
    }

    private void initActionBar() {
        final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.activity_main));
        }
        setHasOptionsMenu(true);
    }

    /**
     * Add new entry.
     */
    private void initFabOnClickListener() {
        final View fab = mParentView.findViewById(R.id.fragment_list_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ItemDetailsPagerItemFragment fragment =
                        ItemDetailsPagerItemFragment.newInstance(ACTION_ADD, -1);

                // Transitions.
                final String transName =
                        getContext().getString(R.string.shared_transition_name_layout);
                ViewCompat.setTransitionName(v, transName);

                fragment.setSharedElementEnterTransition(new AutoTransition());
                fragment.setSharedElementReturnTransition(new AutoTransition());

                getActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .addSharedElement(v, transName)
                        .replace(R.id.activity_main_fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    private void initRecyclerView() {
        final RecyclerView rv = (RecyclerView) mParentView.findViewById(R.id.fragment_list_rv);
        rv.addItemDecoration(
                new DividerItemDecoration(
                        getActivity(),
                        DividerItemDecoration.VERTICAL));
        rv.setLayoutManager(new LinearLayoutManager(getActivity()));

        mPresenter.initRecyclerView(rv);
    }

    private void initPresenter() {
        new ListPresenter(getActivity(), this);
        mPresenter.start();
    }

    /**
     * Getting text from search view, and use for filter. If text is empty, then using empty string.
     */
    private void updateAdapter() {
        //noinspection ConstantConditions
        if (mSearchView == null) {
            mPresenter.updateAdapter("");
        } else {
            final String searchText = mSearchView.getQuery().toString();
            mPresenter.updateAdapter(TextUtils.isEmpty(searchText) ? "" : searchText);
        }
    }


    /*
        Options menu
     */

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.menu_list, menu);
        initSearchView(menu);
        initFilterMenu(menu);
    }

    private void initSearchView(@NonNull final Menu menu) {
        mSearchView = (SearchView) menu.findItem(R.id.menu_list_search).getActionView();
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mPresenter.updateAdapter(newText);
                return true;
            }
        });
    }

    private void initFilterMenu(@NonNull final Menu menu) {
        menu.findItem(R.id.menu_list_filter_sort)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        FilterSortDialogFragment df = FilterSortDialogFragment.newInstance();
                        df.show(getChildFragmentManager(), null);
                        return true;
                    }
                });
    }


    /*
        Found notification
     */

    private void initFoundView() {
        mFoundView = (Button) mParentView.findViewById(R.id.fragment_list_button_found);
    }

    @NonNull
    private Runnable getRunnableForFoundNotification(final int itemsCount) {
        return new Runnable() {
            @Override
            public void run() {
                if (!ListFragment.this.isAdded()) {
                    return;
                }

                final int delay = getResources()
                        .getInteger(R.integer.fragment_list_notification_delay);
                final int delayClose = getResources()
                        .getInteger(R.integer.fragment_list_notification_delay_auto_close);

                if (mFoundView.isAttachedToWindow()) {
                    mFoundView.setText(String.format(
                            Locale.ENGLISH,
                            getString(R.string.fragment_list_notification_found_text) + "\n%d",
                            itemsCount));

                    mTimerFound = System.currentTimeMillis();

                    if (mFoundView.getVisibility() == View.INVISIBLE) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            circularRevealAnimationOn(
                                    mFoundView,
                                    EXTRA_REVEAL_ANIM_CENTER_CENTER,
                                    getResources().getInteger(R.integer.circular_reveal_animation_default_value));
                        } else {
                            mFoundView.setVisibility(View.VISIBLE);
                        }

                        // Start notification close timer.
                        // Timer is cyclical, while notification is showed.
                        final Timer timer = new Timer();
                        timer.schedule(getTimerTaskForFoundNotification(delayClose, timer), delay, delay);
                    }
                }
            }
        };
    }

    @NonNull
    private TimerTask getTimerTaskForFoundNotification(final int delayClose, final Timer timer) {
        return new TimerTask() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - mTimerFound > delayClose) {
                    final FragmentActivity activity = getActivity();
                    if (activity != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    circularRevealAnimationOff(
                                            mFoundView,
                                            EXTRA_REVEAL_ANIM_CENTER_CENTER,
                                            getResources().getInteger(
                                                    R.integer.circular_reveal_animation_default_value));
                                } else {
                                    mFoundView.setVisibility(View.INVISIBLE);
                                }
                            }
                        });
                    }

                    // If notification is closed, then stop timer.
                    timer.cancel();
                }
            }
        };
    }


    /*
        Observer
     */

    @Override
    public void onNotify(final int resultCode, @Nullable final Bundle data) {
        switch (resultCode) {
            case RESULT_CODE_NOTES_IMPORTED:
            case RESULT_CODE_LIST_FILTERED:
            case RESULT_CODE_SYNC_SUCCESS:
            case RESULT_CODE_MOCK_ENTRIES_ADDED:
            case RESULT_CODE_ENTRY_ADDED:
            case RESULT_CODE_ENTRY_EDITED:
            case RESULT_CODE_ENTRY_DELETED:
            case RESULT_CODE_USER_SELECTED:
            case RESULT_CODE_USER_DELETED:
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPresenter.loadFilterProfile();
                        updateAdapter();
                    }
                });
            default:
                break;
        }
    }


    /*
        ListContract.View
     */

    @Override
    public void setPresenter(@NonNull final ListContract.Presenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public void showFoundNotification(final int itemsCount) {
        final Handler handler = new Handler();
        handler.postDelayed(
                getRunnableForFoundNotification(itemsCount),
                getResources().getInteger(R.integer.fragment_list_notification_delay));
    }

    @NonNull
    @Override
    public FragmentManager getSupportFragmentManager() {
        return getActivity().getSupportFragmentManager();
    }
}
