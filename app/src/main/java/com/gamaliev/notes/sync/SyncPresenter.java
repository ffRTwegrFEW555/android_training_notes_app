package com.gamaliev.notes.sync;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;

import com.gamaliev.notes.sync.utils.SyncUtils;

import static com.gamaliev.notes.app.NotesApp.getAppContext;
import static com.gamaliev.notes.conflict.utils.ConflictUtils.checkConflictingExists;
import static com.gamaliev.notes.conflict.utils.ConflictUtils.hideConflictStatusBarNotification;
import static com.gamaliev.notes.sync.db.SyncDbHelper.clear;
import static com.gamaliev.notes.sync.db.SyncDbHelper.getAll;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

@SuppressWarnings("NullableProblems")
class SyncPresenter implements SyncContract.Presenter {

    /* ... */
    @NonNull private final Context mContext;
    @NonNull private final SyncContract.View mSyncView;
    @NonNull private SyncRecyclerViewAdapter mAdapter;
    @Nullable private Cursor mCursor;


    /*
        Init
     */

    SyncPresenter(@NonNull SyncContract.View syncView) {
        mContext = getAppContext();
        mSyncView = syncView;

        mSyncView.setPresenter(this);
    }


    /*
        SyncContract.Presenter
     */

    @Override
    public void start() {
        initAdapter();
        initRecyclerView();
    }

    @Override
    public void synchronize() {
        SyncUtils.synchronize(mContext);
    }

    @Override
    public void deleteAllFromServerAsync() {
        SyncUtils.deleteAllFromServerAsync(mContext);
    }

    @Override
    public void clearJournal() {
        if (clear(mContext)) {
            mSyncView.onSuccessClearJournal();
        } else {
            mSyncView.onFailedClearJournal();
        }
    }

    @Override
    public void updateAdapter() {
        updateCursor();
        mAdapter.notifyDataSetChanged();
        mSyncView.scrollRecyclerViewToBottom(mAdapter.getItemCount() - 1);
        initConflictWarning();
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
    public void onDestroyView() {
        closeCursor();
    }

    @Override
    public void onDetachedFromRecyclerView() {
        closeCursor();
    }


    /*
        ...
     */

    private void initAdapter() {
        mAdapter = new SyncRecyclerViewAdapter(this);
    }

    private void initRecyclerView() {
        final RecyclerView rv = mSyncView.getRecyclerView();
        rv.setAdapter(mAdapter);
        updateAdapter();
    }

    private void updateCursor() {
        closeCursor();
        mCursor = getAll(mContext);
    }

    private void closeCursor() {
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
    }

    private void initConflictWarning() {
        final boolean result = checkConflictingExists(mContext);
        if (result) {
            mSyncView.showConflictWarning();
        } else {
            mSyncView.hideConflictWarning();
            hideConflictStatusBarNotification(mContext);
        }
    }
}
