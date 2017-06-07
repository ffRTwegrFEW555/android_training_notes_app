package com.gamaliev.notes.list;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;

import com.gamaliev.notes.common.recycler_view_item_touch_helper.ItemTouchHelperCallback;
import com.gamaliev.notes.common.recycler_view_item_touch_helper.OnStartDragListener;
import com.gamaliev.notes.common.shared_prefs.SpFilterProfiles;

import java.util.HashMap;
import java.util.Map;

import static com.gamaliev.notes.app.NotesApp.getAppContext;
import static com.gamaliev.notes.common.db.DbHelper.BASE_COLUMN_ID;
import static com.gamaliev.notes.common.db.DbHelper.findColumnValueByCursorPosition;
import static com.gamaliev.notes.common.shared_prefs.SpCommon.convertJsonToMap;
import static com.gamaliev.notes.list.db.ListDbHelper.getCursorWithParams;
import static com.gamaliev.notes.list.db.ListDbHelper.swapManuallyColumnValue;


/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

@SuppressWarnings("NullableProblems")
class ListPresenter implements ListContract.Presenter, OnStartDragListener {

    /* Logger */
    @NonNull private static final String TAG = ListPresenter.class.getSimpleName();

    /* ... */
    @NonNull private FragmentActivity mActivity;
    @NonNull private final ListContract.View mListView;
    @NonNull private Map<String, String> mFilterProfileMap;
    @NonNull private ListRecyclerViewAdapter mAdapter;
    @NonNull private ItemTouchHelper mItemTouchHelper;
    @Nullable private Cursor mCursor;


    /*
        Init
     */

    ListPresenter(
            @NonNull final FragmentActivity activity,
            @NonNull final ListContract.View listView) {

        mActivity = activity;
        mListView = listView;
        mFilterProfileMap = new HashMap<>();

        mListView.setPresenter(this);
    }


    /*
        ListContract.Presenter
     */

    @Override
    public void start() {}

    @Override
    public void loadFilterProfile() {
        final String filterProfile = SpFilterProfiles.getSelectedForCurrentUser(mActivity);

        if (filterProfile == null) {
            final Map<String, String> map = convertJsonToMap(SpFilterProfiles.getDefaultProfile());
            if (map != null) {
                mFilterProfileMap = map;
            } else {
                Log.e(TAG, "Cannot get default filter profile.");
            }

        } else {
            final Map<String, String> map = convertJsonToMap(filterProfile);
            if (map != null) {
                mFilterProfileMap = map;
            } else {
                Log.e(TAG, "Cannot get filter profile for current user.");
            }
        }
    }

    @Override
    public void updateAdapter(@NonNull final String newText) {
        updateCursor(newText);
        mAdapter.notifyDataSetChanged();
        mListView.showFoundNotification(mAdapter.getItemCount());
    }

    @Override
    public void initRecyclerView(@NonNull final RecyclerView rv) {
        initAdapter();
        rv.setAdapter(mAdapter);

        // Drag & Drop.
        final ItemTouchHelper.Callback callback =
                new ItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(rv);
    }

    @Nullable
    @Override
    public Cursor getCursor() {
        return mCursor;
    }

    @Override
    public int getItemCount() {
        return mCursor == null || mCursor.isClosed() ? 0 : mCursor.getCount();
    }

    @Override
    public boolean swapItems(int from, int to) {
        if (mCursor == null || mCursor.isClosed()) {
            return false;
        }

        final String entryIdFrom = findColumnValueByCursorPosition(
                mCursor,
                BASE_COLUMN_ID,
                from);
        if (entryIdFrom == null) {
            return false;
        }

        final String entryIdTo = findColumnValueByCursorPosition(
                mCursor,
                BASE_COLUMN_ID,
                to);
        if (entryIdTo == null) {
            return false;
        }

        swapManuallyColumnValue(
                getAppContext(),
                entryIdFrom,
                entryIdTo);

        updateCursor("");
        return true;
    }

    @NonNull
    @Override
    public FragmentManager getSupportFragmentManager() {
        return mListView.getSupportFragmentManager();
    }

    @Override
    public void onDestroyView() {
        closeCursor();
    }


    /*
        OnStartDragListener
     */

    @Override
    public void onStartDrag(final RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }


    /*
        ...
     */

    private void initAdapter() {
        mAdapter = new ListRecyclerViewAdapter(mActivity, this, this);
    }

    /**
     * Update cursor of the recycler view adapter.
     * @param text              Search text.
     */
    private void updateCursor(@NonNull final String text) {
        closeCursor();
        mCursor = getCursorWithParams(
                getAppContext(),
                text,
                mFilterProfileMap);
    }

    private void closeCursor() {
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
    }
}


