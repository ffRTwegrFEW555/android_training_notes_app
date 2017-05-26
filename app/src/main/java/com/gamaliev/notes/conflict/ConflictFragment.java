package com.gamaliev.notes.conflict;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gamaliev.notes.R;

import static com.gamaliev.notes.common.codes.RequestCode.REQUEST_CODE_CONFLICT_DIALOG_SELECT;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_CONFLICTED_SUCCESS;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class ConflictFragment extends Fragment {

    /* ... */
    public static final String EXTRA_CONFLICT_SELECT_POSITION = "position";

    @NonNull private RecyclerView mRecyclerView;
    @NonNull private ConflictRecyclerViewAdapter mAdapter;


    /*
        Lifecycle
     */

    @Nullable
    @Override
    public View onCreateView(
            final LayoutInflater inflater,
            @Nullable final ViewGroup container,
            @Nullable final Bundle savedInstanceState) {

        // Adapter.
        mAdapter = new ConflictRecyclerViewAdapter(getContext(), this);

        // RecyclerView
        mRecyclerView = (RecyclerView) inflater.inflate(
                R.layout.fragment_conflict,
                container,
                false);
        mRecyclerView.addItemDecoration(
                new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(mAdapter);

        return mRecyclerView;
    }


    /*
        ...
     */

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_CODE_CONFLICTED_SUCCESS) {
            if (requestCode == REQUEST_CODE_CONFLICT_DIALOG_SELECT) {
                final int pos = data.getIntExtra(EXTRA_CONFLICT_SELECT_POSITION, -1);
                if (pos > -1) {
                    mAdapter.updateCursor(getContext());
                    mAdapter.notifyItemRemoved(pos);
                    mAdapter.notifyItemRangeChanged(pos, mAdapter.getItemCount());
                }
            }
        }
    }


    /*
        Intents
     */

    @NonNull
    public static Intent getResultIntent(final int position) {
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_CONFLICT_SELECT_POSITION, position);
        return intent;
    }
}
