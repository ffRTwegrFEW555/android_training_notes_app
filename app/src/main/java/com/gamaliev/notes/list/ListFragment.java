package com.gamaliev.notes.list;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.transition.Fade;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.db.DbHelper;
import com.gamaliev.notes.common.observers.Observer;
import com.gamaliev.notes.common.recycler_view_item_touch_helper.ItemTouchHelperCallback;
import com.gamaliev.notes.common.recycler_view_item_touch_helper.OnStartDragListener;
import com.gamaliev.notes.common.shared_prefs.SpFilterProfiles;
import com.gamaliev.notes.item_details.ItemDetailsPagerItemFragment;
import com.gamaliev.notes.list.db.ListRecyclerViewAdapter;

import java.util.Locale;
import java.util.Map;
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
import static com.gamaliev.notes.common.observers.ObserverHelper.ENTRIES_MOCK;
import static com.gamaliev.notes.common.observers.ObserverHelper.ENTRY;
import static com.gamaliev.notes.common.observers.ObserverHelper.FILE_IMPORT;
import static com.gamaliev.notes.common.observers.ObserverHelper.LIST_FILTER;
import static com.gamaliev.notes.common.observers.ObserverHelper.SYNC;
import static com.gamaliev.notes.common.observers.ObserverHelper.registerObserver;
import static com.gamaliev.notes.common.observers.ObserverHelper.unregisterObserver;
import static com.gamaliev.notes.common.shared_prefs.SpCommon.convertJsonToMap;
import static com.gamaliev.notes.item_details.ItemDetailsPagerItemFragment.ACTION_ADD;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class ListFragment extends Fragment
        implements OnStartDragListener, Observer {

    /* Logger */
    @SuppressWarnings("unused")
    private static final String TAG = ListFragment.class.getSimpleName();

    /* SQLite */
    @NonNull
    public static final String[] SEARCH_COLUMNS = {
            DbHelper.LIST_ITEMS_COLUMN_TITLE,
            DbHelper.LIST_ITEMS_COLUMN_DESCRIPTION};

    /* Observed */
    @NonNull
    public static final String[] OBSERVED = {
            FILE_IMPORT,
            LIST_FILTER,
            ENTRY,
            ENTRIES_MOCK,
            SYNC};

    /* ... */
    @NonNull private View mParentView;
    @NonNull private ListRecyclerViewAdapter mAdapter;
    @NonNull private RecyclerView mRecyclerView;
    @NonNull private Button mFoundView;
    @NonNull private SearchView mSearchView;
    @NonNull private Map<String, String> mFilterProfileMap;
    @NonNull private ItemTouchHelper mItemTouchHelper;
    private long mTimerFound;


    /*
        Init
     */

    public static ListFragment newInstance() {
        return new ListFragment();
    }


    /*
        Lifecycle
     */

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

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

        return mParentView;
    }

    @Override
    public void onViewCreated(
            final View view,
            @Nullable final Bundle savedInstanceState) {

        init();
    }

    @Override
    public void onResume() {
        initFilterProfile();
        updateFilterAdapter();

        registerObserver(
                OBSERVED,
                toString(),
                this);
        super.onResume();
    }

    @Override
    public void onPause() {
        unregisterObserver(
                OBSERVED,
                toString());
        super.onPause();
    }

    /*
        ...
     */

    private void init() {
        mFoundView = (Button) mParentView.findViewById(R.id.fragment_list_button_found);

        initActionBarTitle();
        initFilterProfile();
        initFabOnClickListener();
        initAdapterAndList();
    }

    private void initActionBarTitle() {
        ((AppCompatActivity) getActivity())
                .getSupportActionBar()
                .setTitle(getString(R.string.activity_main_name));
    }

    private void initFilterProfile() {
        mFilterProfileMap = convertJsonToMap(
                SpFilterProfiles.getSelectedForCurrentUser(getContext()));
    }

    /**
     * Start fragment, with Add new entry action.
     */
    private void initFabOnClickListener() {
        mParentView.findViewById(R.id.fragment_list_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ItemDetailsPagerItemFragment fragment =
                        ItemDetailsPagerItemFragment.newInstance(ACTION_ADD, -1);

                setExitTransition(new Fade());
                fragment.setEnterTransition(new Fade());

                getActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.activity_main_fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    /**
     * Get cursor from database, create and set adapter,
     * set on click listener, set filter query provider.<br>
     */
    private void initAdapterAndList() {

        // Create adapter.
        mAdapter = new ListRecyclerViewAdapter(this, this);
        mAdapter.updateCursor(getContext(), "", mFilterProfileMap);

        // Init
        mRecyclerView = (RecyclerView) mParentView.findViewById(R.id.fragment_list_rv);
        mRecyclerView.addItemDecoration(
                new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(mAdapter);

        //
        ItemTouchHelper.Callback callback = new ItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);
    }


    /*
        Options menu
     */

    /**
     * Inflate action bar menu and setup search function.
     */
    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.menu_list, menu);
        initSearchView(menu);
        initFilterMenu(menu);
    }

    /**
     * Setting SearchView listener.<br>
     * Filtering and updating list on text change.<br>
     * Also there is a notification about the number of positions found.
     * @param menu Action bar menu of activity.
     */
    private void initSearchView(@NonNull final Menu menu) {
        mSearchView = (SearchView) menu.findItem(R.id.menu_list_search).getActionView();

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                updateAdapter(newText);
                return true;
            }
        });
    }

    private void initFilterMenu(@NonNull final Menu menu) {
        // Filter / Sort list.
        menu.findItem(R.id.menu_list_filter_sort)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        // Launch dialog.
                        FilterSortDialogFragment df = new FilterSortDialogFragment();
                        df.show(getChildFragmentManager(), null);
                        return true;
                    }
                });
    }


    /*
        Found notification
     */

    /**
     * Show found notification.<br>
     * After stopping the input of text, after some times, the notification closes.
     */
    private void showFoundNotification() {
        final Handler handler = new Handler();
        handler.postDelayed(
                getRunnableForFoundNotification(),
                getResources().getInteger(R.integer.fragment_list_notification_delay));
    }

    /**
     * @return Runnable task for found notification, contains all logic.
     */
    @NonNull
    private Runnable getRunnableForFoundNotification() {
        return new Runnable() {
            @Override
            public void run() {
                final int delay = getResources()
                        .getInteger(R.integer.fragment_list_notification_delay);
                final int delayClose = getResources()
                        .getInteger(R.integer.fragment_list_notification_delay_auto_close);

                // Check..
                if (mFoundView.isAttachedToWindow()) {

                    // Set text.
                    mFoundView.setText(String.format(Locale.ENGLISH,
                            getString(R.string.fragment_list_notification_found_text) + "\n%d",
                            mAdapter.getItemCount()));

                    // Set start time of the notification display.
                    mTimerFound = System.currentTimeMillis();

                    if (mFoundView.getVisibility() == View.INVISIBLE) {
                        // Show notification. If API >= 21, then with circular reveal animation.
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
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                if (System.currentTimeMillis() - mTimerFound >
                                        delayClose) {

                                    final FragmentActivity activity = getActivity();
                                    if (activity != null) {
                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                                    circularRevealAnimationOff(
                                                            mFoundView,
                                                            EXTRA_REVEAL_ANIM_CENTER_CENTER,
                                                            getResources().getInteger(R.integer.circular_reveal_animation_default_value));
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
                        }, delay, delay);
                    }
                }
            }
        };
    }


    /*
        ...
     */

    /**
     * Getting text from search view, and use for filter. If text is empty, then using empty string.
     */
    @SuppressWarnings("ConstantConditions")
    private void updateFilterAdapter() {
        if (mSearchView != null) {
            final String searchText = mSearchView.getQuery().toString();
            updateAdapter(TextUtils.isEmpty(searchText) ? "" : searchText);

        } else {
            updateAdapter("");
        }
    }

    private void updateAdapter(String newText) {
        // Refresh view.
        mAdapter.updateCursor(getContext(), newText, mFilterProfileMap);
        mAdapter.notifyDataSetChanged();

        //
        showFoundNotification();
    }


    /*
        OnStartDragListener
     */

    @Override
    public void onStartDrag(final RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
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
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initFilterProfile();
                        updateFilterAdapter();
                    }
                });
                break;

            default:
                break;
        }
    }
}
