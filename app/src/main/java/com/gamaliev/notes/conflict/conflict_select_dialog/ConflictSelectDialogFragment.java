package com.gamaliev.notes.conflict.conflict_select_dialog;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gamaliev.notes.R;

import static com.gamaliev.notes.common.CommonUtils.EXTRA_REVEAL_ANIM_CENTER_CENTER;
import static com.gamaliev.notes.common.CommonUtils.showToastRunOnUiThread;
import static com.gamaliev.notes.common.DialogFragmentUtils.initCircularRevealAnimation;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

@SuppressWarnings("NullableProblems")
public class ConflictSelectDialogFragment extends DialogFragment
        implements ConflictSelectDialogContract.View {

    /* Logger */
    @NonNull private static final String TAG = ConflictSelectDialogFragment.class.getSimpleName();

    /* ... */
    private static final String EXTRA_SYNC_ID = "syncId";
    private static final String EXTRA_POSITION = "position";

    @NonNull private View mDialog;
    @NonNull private ConflictSelectDialogContract.Presenter mPresenter;
    @NonNull private Button mSrvSaveBtn;
    @NonNull private Button mLocalSaveBtn;


    /*
        Init
     */

    /**
     * Get new instance of conflict select dialog fragment.
     * @param syncId    Synchronization id of Entry.
     * @param position  Position of entry, in adapter of recycler view.
     * @return New instance of conflict select dialog fragment.
     */
    @NonNull
    public static ConflictSelectDialogFragment newInstance(
            @NonNull final String syncId,
            final int position) {

        final Bundle args = new Bundle();
        args.putString(EXTRA_SYNC_ID, syncId);
        args.putInt(EXTRA_POSITION, position);

        final ConflictSelectDialogFragment fragment = new ConflictSelectDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }


    /*
        Lifecycle
     */

    @Nullable
    @Override
    public View onCreateView(
            final LayoutInflater inflater,
            @Nullable final ViewGroup container,
            final Bundle savedInstanceState) {

        mDialog = inflater.inflate(R.layout.fragment_dialog_conflict_select, container);
        disableTitle();
        return mDialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
    }


    /*
        ...
     */

    private void init() {
        initDialogSize();
        initCircularAnimation();
        initSaveButtons();
        initPresenter();
    }

    private void disableTitle() {
        // Disable title for more space.
        final Window window = getDialog().getWindow();
        if (window != null) {
            window.requestFeature(Window.FEATURE_NO_TITLE);
        }
    }

    private void initDialogSize() {
        // Set max size of dialog. ( XML is not work :/ )
        final Window window = getDialog().getWindow();
        if (window != null) {
            final DisplayMetrics displayMetrics = getActivity().getResources().getDisplayMetrics();
            final ViewGroup.LayoutParams params = window.getAttributes();
            params.width = Math.min(
                    displayMetrics.widthPixels,
                    getActivity().getResources().getDimensionPixelSize(
                            R.dimen.fragment_dialog_conflict_select_max_width));
            params.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
            window.setAttributes((android.view.WindowManager.LayoutParams) params);
        }
    }

    private void initCircularAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            initCircularRevealAnimation(
                    mDialog,
                    true,
                    EXTRA_REVEAL_ANIM_CENTER_CENTER);
        }
    }

    private void initSaveButtons() {
        mSrvSaveBtn = (Button) mDialog
                .findViewById(R.id.fragment_dialog_conflict_select_server_save_btn);
        mLocalSaveBtn = (Button) mDialog
                .findViewById(R.id.fragment_dialog_conflict_select_local_save_btn);
    }

    private void initPresenter() {
        final String syncId = getArguments().getString(EXTRA_SYNC_ID);
        if (syncId == null) {
            final String error = getString(R.string.fragment_dialog_conflict_sync_id_is_null);
            Log.e(TAG, error);
            showToastRunOnUiThread(error, Toast.LENGTH_SHORT);
            dismiss();
            return;
        }

        new ConflictSelectDialogPresenter(this, syncId, getArguments().getInt(EXTRA_POSITION));
        mPresenter.start();
    }


    /*
        ConflictSelectDialogContract.View
     */

    @Override
    public void setPresenter(@NonNull final ConflictSelectDialogContract.Presenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public void setServerHeader(@NonNull final String text) {
        final TextView serverHeaderTv = (TextView) mDialog
                .findViewById(R.id.fragment_dialog_conflict_select_server_header_tv);
        if (serverHeaderTv != null) {
            //noinspection SetTextI18n
            serverHeaderTv.setText(
                    getString(R.string.fragment_dialog_conflict_select_server_header_prefix)
                            + ": "
                            + text);
        }
    }

    @Override
    public void setServerBody(@NonNull final String text) {
        final TextView serverBodyTv = (TextView) mDialog
                .findViewById(R.id.fragment_dialog_conflict_select_server_body_tv);
        if (serverBodyTv != null) {
            serverBodyTv.setText(text);
        }
    }

    @Override
    public void setLocalHeader(@NonNull final String text) {
        final TextView localHeaderTv = (TextView) mDialog
                .findViewById(R.id.fragment_dialog_conflict_select_local_header_tv);
        if (localHeaderTv != null) {
            //noinspection SetTextI18n
            localHeaderTv.setText(
                    getString(R.string.fragment_dialog_conflict_select_local_header_prefix)
                            + ": "
                            + text);
        }
    }

    @Override
    public void setLocalBody(@NonNull final String text) {
        final TextView localBodyTv = (TextView) mDialog
                .findViewById(R.id.fragment_dialog_conflict_select_local_body_tv);
        if (localBodyTv != null) {
            localBodyTv.setText(text);
        }
    }

    @Override
    public void setSrvSaveBtnOnClickListener(@NonNull final View.OnClickListener listener) {
        mSrvSaveBtn.setOnClickListener(listener);
    }

    @Override
    public void setLocalSaveBtnOnClickListener(@NonNull final View.OnClickListener listener) {
        mLocalSaveBtn.setOnClickListener(listener);
    }

    @Override
    public void performError(@NonNull final String text) {
        showToastRunOnUiThread(text, Toast.LENGTH_LONG);
        dismiss();
    }

    @Override
    public void enableSaveButtons() {
        mSrvSaveBtn.setEnabled(true);
        mLocalSaveBtn.setEnabled(true);
    }

    /**
     * @return True, if view is active (attached or added), otherwise false.
     */
    @Override
    public boolean isActive() {
        return isAdded() && mDialog.isAttachedToWindow();
    }
}
