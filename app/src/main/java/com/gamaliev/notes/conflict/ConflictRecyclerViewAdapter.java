package com.gamaliev.notes.conflict;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gamaliev.notes.R;
import com.gamaliev.notes.list.db.ListDbHelper;

import static com.gamaliev.notes.common.db.DbHelper.SYNC_CONFLICT_COLUMN_SYNC_ID;
import static com.gamaliev.notes.common.db.DbHelper.SYNC_CONFLICT_TABLE_NAME;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class ConflictRecyclerViewAdapter
        extends RecyclerView.Adapter<ConflictRecyclerViewAdapter.ViewHolder> {

    /* Logger */
    private static final String TAG = ConflictRecyclerViewAdapter.class.getSimpleName();

    /* ... */
    public static final int REQUEST_CODE_CONFLICT_SELECT = 101;

    @NonNull private Cursor mCursor;
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
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final CardView cardView = (CardView) LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.fragment_conflict_item, parent, false);

        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        mCursor.moveToPosition(position);
        final String syncId = mCursor.getString(mCursor.getColumnIndex(SYNC_CONFLICT_COLUMN_SYNC_ID));
        holder.mTextView.setText(syncId);

        holder.mCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch dialog.
                ConflictSelectDialogFragment df =
                        ConflictSelectDialogFragment.newInstance(syncId, position);
                df.setTargetFragment(mFragment, REQUEST_CODE_CONFLICT_SELECT);
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
        private final CardView mCardView;
        private final TextView mTextView;

        private ViewHolder(CardView itemView) {
            super(itemView);
            mCardView = itemView;
            mTextView = (TextView) mCardView.findViewById(R.id.fragment_conflict_item_text_view);
        }
    }


    /*
        ...
     */

    public void updateCursor(@NonNull final Context context) {
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
        mCursor = ListDbHelper.getEntriesWithSyncIdColumn(context, SYNC_CONFLICT_TABLE_NAME);
    }
}
