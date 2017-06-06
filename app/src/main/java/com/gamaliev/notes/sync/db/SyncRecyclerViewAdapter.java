package com.gamaliev.notes.sync.db;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.CommonUtils;
import com.gamaliev.notes.common.db.DbHelper;

import static com.gamaliev.notes.sync.SyncUtils.getActionText;
import static com.gamaliev.notes.sync.SyncUtils.getStatusText;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class SyncRecyclerViewAdapter
        extends RecyclerView.Adapter<SyncRecyclerViewAdapter.ViewHolder> {

    /* ... */
    @NonNull private final Fragment mFragment;
    @Nullable private Cursor mCursor;


    /*
        Init
     */

    public SyncRecyclerViewAdapter(@NonNull final Fragment fragment) {
        mFragment = fragment;
        updateCursor();
    }


    /*
        Lifecycle
     */

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.fragment_sync_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        if (mCursor == null || !mCursor.moveToPosition(position)) {
            return;
        }
        
        final Context context = mFragment.getContext();

        final int indexFinished = mCursor.getColumnIndex(DbHelper.SYNC_COLUMN_FINISHED);
        final int indexAction   = mCursor.getColumnIndex(DbHelper.SYNC_COLUMN_ACTION);
        final int indexStatus   = mCursor.getColumnIndex(DbHelper.SYNC_COLUMN_STATUS);
        final int indexAmount   = mCursor.getColumnIndex(DbHelper.SYNC_COLUMN_AMOUNT);

        final String finished   = CommonUtils
                .convertUtcToLocal(context, mCursor.getString(indexFinished));
        final int actionIndex   = Integer.parseInt(mCursor.getString(indexAction));
        final String action     = context.getString(getActionText()[actionIndex]);
        final int statusIndex   = Integer.parseInt(mCursor.getString(indexStatus));
        final String status     = context.getString(getStatusText()[statusIndex]);
        final String amount     = mCursor.getString(indexAmount);
        final String description =
                context.getString(R.string.fragment_sync_item_status_prefix) + ": "
                        + action + ", "
                        + status + " ("
                        + amount + ")";

        holder.mFinishedView.setText(finished);
        holder.mDescriptionView.setText(description);
    }

    @Override
    public int getItemCount() {
        return mCursor == null || mCursor.isClosed() ? 0 : mCursor.getCount();
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        closeCursor();
    }


    /*
        ViewHolder
     */

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView  mFinishedView;
        private final TextView  mDescriptionView;

        private ViewHolder(@NonNull final View view) {
            super(view);
            
            mFinishedView       = (TextView) view.findViewById(R.id.fragment_sync_item_finished);
            mDescriptionView    = (TextView) view.findViewById(R.id.fragment_sync_item_description);
        }
    }


    /*
        ...
     */

    public final void updateCursor() {
        closeCursor();
        mCursor = SyncDbHelper.getAll(mFragment.getContext());
    }

    private void closeCursor() {
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
    }
}
