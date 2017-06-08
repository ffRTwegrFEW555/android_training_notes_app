package com.gamaliev.notes.sync;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.CommonUtils;
import com.gamaliev.notes.common.db.DbHelper;

import static com.gamaliev.notes.app.NotesApp.getAppContext;
import static com.gamaliev.notes.sync.utils.SyncUtils.getActionText;
import static com.gamaliev.notes.sync.utils.SyncUtils.getStatusText;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

class SyncRecyclerViewAdapter
        extends RecyclerView.Adapter<SyncRecyclerViewAdapter.ViewHolder> {

    /* ... */
    @NonNull private final Context mContext;
    @NonNull private final SyncContract.Presenter mPresenter;

    
    /*
        Init
     */

    SyncRecyclerViewAdapter(@NonNull final SyncContract.Presenter presenter) {
        mContext = getAppContext();
        mPresenter = presenter;
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
        final Cursor cursor = mPresenter.getCursor();
        if (cursor == null || !cursor.moveToPosition(position)) {
            return;
        }

        final int indexFinished = cursor.getColumnIndex(DbHelper.SYNC_COLUMN_FINISHED);
        final int indexAction   = cursor.getColumnIndex(DbHelper.SYNC_COLUMN_ACTION);
        final int indexStatus   = cursor.getColumnIndex(DbHelper.SYNC_COLUMN_STATUS);
        final int indexAmount   = cursor.getColumnIndex(DbHelper.SYNC_COLUMN_AMOUNT);

        final String finished   = CommonUtils
                .convertUtcToLocal(mContext, cursor.getString(indexFinished));
        final int actionIndex   = Integer.parseInt(cursor.getString(indexAction));
        final String action     = mContext.getString(getActionText()[actionIndex]);
        final int statusIndex   = Integer.parseInt(cursor.getString(indexStatus));
        final String status     = mContext.getString(getStatusText()[statusIndex]);
        final String amount     = cursor.getString(indexAmount);
        final String description =
                mContext.getString(R.string.fragment_sync_item_status_prefix) + ": "
                        + action + ", "
                        + status + " ("
                        + amount + ")";

        holder.mFinishedView.setText(finished);
        holder.mDescriptionView.setText(description);
    }

    @Override
    public int getItemCount() {
        return mPresenter.getItemCount();
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        mPresenter.onDetachedFromRecyclerView();
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
}
