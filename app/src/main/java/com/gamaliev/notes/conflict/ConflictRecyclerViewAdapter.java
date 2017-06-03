package com.gamaliev.notes.conflict;

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

import static com.gamaliev.notes.common.db.DbHelper.COMMON_COLUMN_SYNC_ID;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_CONFLICT_TABLE_NAME;
import static com.gamaliev.notes.common.db.DbHelper.getEntries;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

final class ConflictRecyclerViewAdapter
        extends RecyclerView.Adapter<ConflictRecyclerViewAdapter.ViewHolder> {

    /* ... */
    @NonNull private final Fragment mFragment;
    @Nullable private Cursor mCursor;


    /*
        Init
     */

    ConflictRecyclerViewAdapter(@NonNull final Fragment fragment) {
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
                .inflate(R.layout.fragment_conflict_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        if (mCursor == null || !mCursor.moveToPosition(position)) {
            return;
        }

        final String syncId = mCursor.getString(mCursor.getColumnIndex(COMMON_COLUMN_SYNC_ID));
        holder.mTextView.setText(
                holder.mTextView.getContext()
                        .getString(R.string.fragment_dialog_conflict_select_item_title_prefix)
                + ": "
                + syncId);

        holder.mParentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ConflictSelectDialogFragment df =
                        ConflictSelectDialogFragment.newInstance(syncId, holder.getAdapterPosition());
                df.show(mFragment.getFragmentManager() , null);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mCursor == null || mCursor.isClosed() ? 0 : mCursor.getCount();
    }

    @Override
    public void onDetachedFromRecyclerView(final RecyclerView recyclerView) {
        closeCursor();
    }


    /*
        ViewHolder
     */

    static final class ViewHolder extends RecyclerView.ViewHolder {
        private final View mParentView;
        private final TextView mTextView;

        private ViewHolder(@NonNull final View view) {
            super(view);

            mParentView = view;
            mTextView = (TextView) view.findViewById(R.id.fragment_conflict_item_text_view);
        }
    }


    /*
        ...
     */

    void updateCursor() {
        closeCursor();
        mCursor = getEntries(mFragment.getContext(), SYNC_CONFLICT_TABLE_NAME, null);
    }

    private void closeCursor() {
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
    }
}
