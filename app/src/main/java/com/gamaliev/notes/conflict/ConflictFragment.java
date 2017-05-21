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

import static android.app.Activity.RESULT_OK;
import static com.gamaliev.notes.conflict.ConflictRecyclerViewAdapter.REQUEST_CODE_CONFLICT_SELECT;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class ConflictFragment extends Fragment {

    /* Logger */
    private static final String TAG = ConflictFragment.class.getSimpleName();

    /* ... */
    public static final String EXTRA_CONFLICT_SELECT_POSITION = "position";

    @NonNull private RecyclerView mRecyclerView;
    @NonNull private ConflictRecyclerViewAdapter mAdapter;

    /*
        Init
     */

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        // Adapter.
        mAdapter = new ConflictRecyclerViewAdapter(getContext(), this);

        // RecyclerView
        mRecyclerView = (RecyclerView) inflater.inflate(
                R.layout.fragment_conflict,
                container,
                false);
        mRecyclerView.addItemDecoration(
                new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(mAdapter);

        return mRecyclerView;
    }


    /*
        ...
     */

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_CONFLICT_SELECT) {
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
