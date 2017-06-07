package com.gamaliev.notes.conflict;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;

import static com.gamaliev.notes.app.NotesApp.getAppContext;
import static com.gamaliev.notes.common.db.DbHelper.COMMON_COLUMN_SYNC_ID;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_CONFLICT_TABLE_NAME;
import static com.gamaliev.notes.common.db.DbHelper.getEntries;

/**
 * @author Vadim Gamaliev <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
public class ConflictPresenter implements ConflictContract.Presenter {

    /* ... */
    @NonNull private final Context mContext;
    @NonNull private final ConflictContract.View mConflictView;
    @NonNull private ConflictRecyclerViewAdapter mAdapter;
    @Nullable private Cursor mCursor;



    /*
        Init
     */

    public ConflictPresenter(@NonNull final ConflictContract.View conflictView) {
        mContext = getAppContext();
        mConflictView = conflictView;
        mConflictView.setPresenter(this);
    }


    /*
        ConflictContract.Presenter
     */

    @Override
    public void start() {
        initAdapter();
        initRecyclerView();
    }

    /**
     * Updating cursor, notifying adapter of recycler view, animation of deleted entry.
     * @param deletedPosition Deleted position of adapter of recycler view.
     */
    @Override
    public void updateRecyclerView(final int deletedPosition) {
        updateCursor();
        mAdapter.notifyItemRemoved(deletedPosition);
        mAdapter.notifyItemRangeChanged(deletedPosition, mAdapter.getItemCount());
    }

    @Nullable
    public String getSyncId(final int position) {
        if (mCursor == null || !mCursor.moveToPosition(position)) {
            return null;
        }

        return mCursor.getString(mCursor.getColumnIndex(COMMON_COLUMN_SYNC_ID));
    }

    @Override
    public int getItemCount() {
        return mCursor == null || mCursor.isClosed() ? 0 : mCursor.getCount();
    }

    @Override
    public FragmentManager getFragmentManager() {
        return mConflictView.getFragmentManager();
    }

    private void updateCursor() {
        closeCursor();
        mCursor = getEntries(mContext, SYNC_CONFLICT_TABLE_NAME, null);
    }

    @Override
    public void closeCursor() {
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
    }


    /*
        RecyclerView & Adapter
     */

    private void initAdapter() {
        updateCursor();
        mAdapter = new ConflictRecyclerViewAdapter(this);
    }

    private void initRecyclerView() {
        final RecyclerView rv = mConflictView.getRecyclerView();
        rv.setAdapter(mAdapter);
    }
}
