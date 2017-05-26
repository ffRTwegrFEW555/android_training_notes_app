package com.gamaliev.notes.conflict;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.gamaliev.notes.R;

import static com.gamaliev.notes.common.codes.RequestCode.REQUEST_CODE_CONFLICT_DIALOG_SELECT;
import static com.gamaliev.notes.common.db.DbHelper.COMMON_COLUMN_SYNC_ID;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_CONFLICT_TABLE_NAME;
import static com.gamaliev.notes.common.db.DbHelper.getEntries;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class ConflictRecyclerViewAdapter
        extends RecyclerView.Adapter<ConflictRecyclerViewAdapter.ViewHolder> {

    /* ... */
    @Nullable private Cursor mCursor;
    @NonNull private Fragment mFragment;


    /*
        Init
     */

    public ConflictRecyclerViewAdapter(
            @NonNull final Context context,
            @NonNull final Fragment fragment) {

        updateCursor(context);
        mFragment = fragment;
    }


    /*
        Lifecycle
     */

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final FrameLayout fl = (FrameLayout) LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.fragment_conflict_item, parent, false);

        return new ViewHolder(fl);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        mCursor.moveToPosition(position);
        final String syncId = mCursor.getString(mCursor.getColumnIndex(COMMON_COLUMN_SYNC_ID));
        holder.mTextView.setText(
                holder.mTextView.getContext()
                        .getString(R.string.fragment_dialog_conflict_select_item_title_prefix)
                + ": "
                + syncId);

        holder.mFrameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch dialog.
                ConflictSelectDialogFragment df =
                        ConflictSelectDialogFragment.newInstance(syncId, position);
                df.setTargetFragment(mFragment, REQUEST_CODE_CONFLICT_DIALOG_SELECT);
                df.show(mFragment.getFragmentManager() , null);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }


    /*
        ViewHolder
     */

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout mFrameLayout;
        private final TextView mTextView;

        private ViewHolder(@NonNull final FrameLayout itemView) {
            super(itemView);
            mFrameLayout = itemView;
            mTextView = (TextView) mFrameLayout.findViewById(R.id.fragment_conflict_item_text_view);
        }
    }


    /*
        ...
     */

    public void updateCursor(@NonNull final Context context) {
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
        mCursor = getEntries(context, SYNC_CONFLICT_TABLE_NAME, null);
    }
}
