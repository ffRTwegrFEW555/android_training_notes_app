package com.gamaliev.notes.sync;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.Fade;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.observers.Observer;
import com.gamaliev.notes.conflict.ConflictFragment;
import com.gamaliev.notes.sync.db.SyncDbHelper;
import com.gamaliev.notes.sync.db.SyncRecyclerViewAdapter;

import static com.gamaliev.notes.common.CommonUtils.showToast;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_SYNC_JOURNAL_CLEARED;
import static com.gamaliev.notes.common.observers.ObserverHelper.SYNC;
import static com.gamaliev.notes.common.observers.ObserverHelper.notifyObservers;
import static com.gamaliev.notes.common.observers.ObserverHelper.registerObserver;
import static com.gamaliev.notes.common.observers.ObserverHelper.unregisterObserver;
import static com.gamaliev.notes.conflict.ConflictUtils.checkConflictingExists;
import static com.gamaliev.notes.conflict.ConflictUtils.hideConflictStatusBarNotification;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

@SuppressWarnings("NullableProblems")
public class SyncFragment extends Fragment implements Observer {

    /* Observed */
    @NonNull
    public static final String[] OBSERVED = {SYNC};

    /* ... */
    @NonNull private View mParentView;
    @NonNull private RecyclerView mRecyclerView;
    @NonNull private SyncRecyclerViewAdapter mAdapter;


    /*
        Init
     */

    public static SyncFragment newInstance() {
        return new SyncFragment();
    }


    /*
        Lifecycle
     */

    @Nullable
    @Override
    public View onCreateView(
            final LayoutInflater inflater,
            @Nullable final ViewGroup container,
            @Nullable final Bundle savedInstanceState) {

        mParentView = inflater.inflate(
                R.layout.fragment_sync,
                container,
                false);
        init();
        return mParentView;
    }

    @Override
    public void onResume() {
        registerObserver(OBSERVED, toString(), this);
        super.onResume();
    }

    @Override
    public void onPause() {
        unregisterObserver(OBSERVED, toString());
        super.onPause();
    }


    /*
        ...
     */

    private void init() {
        initTransition();
        initActionBar();
        initAdapter();
        initRecyclerView();
        initConflictWarning();
    }

    private void initTransition() {
        setExitTransition(new Fade());
        setEnterTransition(new Fade());
    }

    private void initActionBar() {
        final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.fragment_sync));
        }
        setHasOptionsMenu(true);
    }
    
    private void initConflictWarning() {
        final boolean result = checkConflictingExists(getContext());
        final View view = mParentView
                .findViewById(R.id.fragment_sync_conflicting_exists_notification_fl);
        if (result) {
            view.setVisibility(View.VISIBLE);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startConflictFragment();
                }
            });
        } else {
            view.setVisibility(View.GONE);
            view.setOnClickListener(null);
            hideConflictStatusBarNotification(getContext());
        }
    }


    /*
        RecyclerView & Adapter
     */

    private void initAdapter() {
        mAdapter = new SyncRecyclerViewAdapter(this);
    }

    private void initRecyclerView() {
        mRecyclerView = (RecyclerView) mParentView.findViewById(R.id.fragment_sync_rv);
        mRecyclerView.addItemDecoration(
                new DividerItemDecoration(
                        getActivity(),
                        DividerItemDecoration.VERTICAL));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(mAdapter);

        scrollRecyclerViewToBottom();
    }

    private void updateAdapter() {
        initConflictWarning();
        mAdapter.updateCursor();
        mAdapter.notifyDataSetChanged();
        scrollRecyclerViewToBottom();
    }

    private void scrollRecyclerViewToBottom() {
        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
            }
        });
    }


    /*
        Options menu
     */

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_sync, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sync_synchronize:
                SyncUtils.synchronize(getContext());
                break;

            case R.id.menu_sync_show_conflicting:
                startConflictFragment();
                break;

            case R.id.menu_sync_delete_all_from_server:
                showConfirmDeleteAllFromServerDialog();
                break;

            case R.id.menu_sync_clear_journal:
                showConfirmClearJournalDialog();
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    /*
        Fragments
     */

    private void startConflictFragment() {
        final ConflictFragment fragment = ConflictFragment.newInstance();
        getActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.activity_main_fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }


    /*
        Confirm operations.
     */

    private void showConfirmDeleteAllFromServerDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder .setTitle(getString(R.string.fragment_sync_dialog_confirm_delete_all_from_server_title))
                .setMessage(getString(R.string.fragment_sync_dialog_confirm_delete_all_from_server_body))
                .setPositiveButton(getString(R.string.fragment_sync_dialog_confirm_delete_all_from_server_btn_delete),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                SyncUtils.deleteAllFromServerAsync(getContext());
                            }
                        })
                .setNegativeButton(
                        getString(R.string.fragment_sync_dialog_confirm_delete_all_from_server_btn_cancel),
                        null)
                .create()
                .show();
    }

    private void showConfirmClearJournalDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder .setTitle(getString(R.string.fragment_sync_dialog_confirm_clear_journal_title))
                .setMessage(getString(R.string.fragment_sync_dialog_confirm_clear_journal_body))
                .setPositiveButton(getString(R.string.fragment_sync_dialog_confirm_clear_journal_btn_clear),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                if (SyncDbHelper.clear(getContext())) {
                                    showToast(
                                            getContext(),
                                            getString(R.string.menu_sync_action_clear_journal_success),
                                            Toast.LENGTH_SHORT);
                                } else {
                                    showToast(
                                            getContext(),
                                            getString(R.string.menu_sync_action_clear_journal_failed),
                                            Toast.LENGTH_SHORT);
                                }
                                notifyObservers(SYNC, RESULT_CODE_SYNC_JOURNAL_CLEARED, null);
                            }
                        })
                .setNegativeButton(
                        getString(R.string.fragment_sync_dialog_confirm_clear_journal_btn_cancel),
                        null)
                .create()
                .show();
    }


    /*
        Callback
     */

/*    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_CANCELED) {
            if (requestCode == REQUEST_CODE_CONFLICTING) {
                notifyDataSetChangedAndScrollToEnd();
            }
        }
    }*/


    /*
        Observer
     */

    @Override
    public void onNotify(final int resultCode, @Nullable final Bundle data) {

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateAdapter();
            }
        });
    }
}