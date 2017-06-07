package com.gamaliev.notes.conflict;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gamaliev.notes.R;
import com.gamaliev.notes.conflict.conflict_select_dialog.ConflictSelectDialogFragment;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

final class ConflictRecyclerViewAdapter
        extends RecyclerView.Adapter<ConflictRecyclerViewAdapter.ViewHolder> {

    /* ... */
    @NonNull private final ConflictContract.Presenter mPresenter;


    /*
        Init
     */

    ConflictRecyclerViewAdapter(@NonNull final ConflictContract.Presenter presenter) {
        mPresenter = presenter;
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
        final String syncId = mPresenter.getSyncId(position);
        if (syncId == null) {
            holder.mTextView.setText("");
            holder.mParentView.setOnClickListener(null);
            return;
        }

        //noinspection SetTextI18n
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
                df.show(mPresenter.getSupportFragmentManager(), null);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mPresenter.getItemCount();
    }

    @Override
    public void onDetachedFromRecyclerView(final RecyclerView recyclerView) {
        mPresenter.onDetachedFromRecyclerView();
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
}
