package com.gamaliev.notes.item_details;

import android.content.DialogInterface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.transition.AutoTransition;
import android.transition.Fade;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.gamaliev.notes.R;
import com.gamaliev.notes.colorpicker.ColorPickerFragment;
import com.gamaliev.notes.common.observers.Observer;
import com.gamaliev.notes.list.db.ListDbHelper;
import com.gamaliev.notes.model.ListEntry;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import static com.gamaliev.notes.app.NotesApp.getAppContext;
import static com.gamaliev.notes.colorpicker.ColorPickerFragment.EXTRA_RESULT_COLOR;
import static com.gamaliev.notes.common.CommonUtils.getDefaultColor;
import static com.gamaliev.notes.common.CommonUtils.getResourceColorApi;
import static com.gamaliev.notes.common.CommonUtils.getStringDateFormatSqlite;
import static com.gamaliev.notes.common.CommonUtils.hideKeyboard;
import static com.gamaliev.notes.common.CommonUtils.showToast;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_COLOR_PICKER_SELECTED;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_ENTRY_ADDED;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_ENTRY_CANCEL;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_ENTRY_DELETED;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_ENTRY_EDITED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_EDITED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_VIEWED;
import static com.gamaliev.notes.common.observers.ObserverHelper.COLOR_PICKER;
import static com.gamaliev.notes.common.observers.ObserverHelper.ENTRY;
import static com.gamaliev.notes.common.observers.ObserverHelper.notifyObservers;
import static com.gamaliev.notes.common.observers.ObserverHelper.registerObserver;
import static com.gamaliev.notes.common.observers.ObserverHelper.unregisterObserver;

public final class ItemDetailsPagerItemFragment extends Fragment implements Observer {

    /* Logger */
    private static final String TAG = ItemDetailsPagerItemFragment.class.getSimpleName();

    /* Action */
    public static final String ACTION      = "ItemDetailsPagerItemFragment.ACTION";
    public static final String ACTION_ADD  = "ItemDetailsPagerItemFragment.ACTION_ADD";
    public static final String ACTION_EDIT = "ItemDetailsPagerItemFragment.ACTION_EDIT";

    private static final String ACTION_ENTRY_ADD    = "ItemDetailsPagerItemFragment.ACTION_ENTRY_ADD";
    private static final String ACTION_ENTRY_EDIT   = "ItemDetailsPagerItemFragment.ACTION_ENTRY_EDIT";
    private static final String ACTION_ENTRY_DELETE = "ItemDetailsPagerItemFragment.ACTION_ENTRY_DELETE";

    /* Extra */
    private static final String EXTRA_ID    = "ItemDetailsPagerItemFragment.EXTRA_ID";
    private static final String EXTRA_ENTRY = "ItemDetailsPagerItemFragment.EXTRA_ENTRY";

    /* Observed */
    @NonNull
    public static final String[] OBSERVED = {COLOR_PICKER};

    /* ... */
    @NonNull private View       mParentView;
    @NonNull private ActionBar  mActionBar;
    @NonNull private View       mColorView;
    @NonNull private EditText   mTitleEditText;
    @NonNull private EditText   mDescEditText;
    @NonNull private EditText   mImageUrlEditText;
    @NonNull private ImageView  mImageView;
    @NonNull private TextInputLayout mImageUrlEditTextLayout;
    @NonNull private ListEntry  mEntry;
    @NonNull private Menu       mMenu;
    @NonNull private String     mAction;
    @Nullable private Bundle    mSavedInstanceState;
    private int mColor;
    private long mId;


    /*
        Init
     */

    public static ItemDetailsPagerItemFragment newInstance(
            @NonNull final String action,
            final long id) {

        final Bundle bundle = new Bundle();
        bundle.putString(ACTION, action);
        bundle.putLong(EXTRA_ID, id);

        final ItemDetailsPagerItemFragment fragment = new ItemDetailsPagerItemFragment();
        fragment.setArguments(bundle);
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
            @Nullable final Bundle savedInstanceState) {

        mParentView = inflater.inflate(
                R.layout.fragment_item_details_pager_item,
                container,
                false);
        init(savedInstanceState);
        return mParentView;
    }

    @Override
    public void onResume() {
        registerObserver(OBSERVED, toString(), this);
        super.onResume();
    }

    @Override
    public void onPause() {
        hideKeyboard(getContext(), mParentView); /* Bug */
        unregisterObserver(OBSERVED, toString());
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        outState.putParcelable(EXTRA_ENTRY, mEntry);
        super.onSaveInstanceState(outState);
    }


    /*
        ...
     */

    private void init(@Nullable final Bundle savedInstanceState) {
        mColorView      = mParentView.findViewById(R.id.fragment_item_details_color);
        mTitleEditText  = (EditText) mParentView.findViewById(R.id.fragment_item_details_title_text_view);
        mDescEditText   = (EditText) mParentView.findViewById(R.id.fragment_item_details_description_text_view);
        mImageUrlEditText = (EditText) mParentView.findViewById(R.id.fragment_item_details_image_url_text_view);
        mImageUrlEditTextLayout = (TextInputLayout) mParentView.findViewById(
                R.id.fragment_item_details_image_url_text_input_layout);
        mImageView      = (ImageView) mParentView.findViewById(R.id.fragment_item_details_image_view);
        mSavedInstanceState = savedInstanceState;

        initArgs();
        initTransition();
        initActionBar();
        initAction();
        initImageUrlValidation();
        initImageView();
        initRefreshImageButton();
        setColorViewListener();
    }

    private void initArgs() {
        mAction = getArguments().getString(ACTION);
        mId = getArguments().getLong(EXTRA_ID);
    }

    private void initTransition() {
        setExitTransition(new Fade());
        setEnterTransition(new Fade());
        ViewCompat.setTransitionName(
                mColorView,
                getString(R.string.shared_transition_name_color_box));
        ViewCompat.setTransitionName(
                mParentView.findViewById(R.id.fragment_item_details_ff_header),
                getString(R.string.shared_transition_name_layout));
    }

    private void initActionBar() {
        mActionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        mActionBar.setElevation(0);
        setHasOptionsMenu(true);
    }

    /**
     * Process Action, when start activity.<br>
     * Filling in the necessary views.<br>
     * See also: {@link #ACTION_ADD}, {@link #ACTION_EDIT}.
     */
    private void initAction() {
        switch (mAction) {
            case ACTION_ADD:
                mActionBar.setTitle(getString(R.string.fragment_item_details_title_add));
                if (mSavedInstanceState == null) {
                    if (mEntry == null) {
                        mEntry = new ListEntry();
                        mEntry.setColor(getDefaultColor(getContext()));
                    }

                } else {
                    mEntry = mSavedInstanceState.getParcelable(EXTRA_ENTRY);
                }
                fillActivityViews();
                break;

            case ACTION_EDIT:
                mActionBar.setTitle(getString(R.string.fragment_item_details_title_edit));
                if (mSavedInstanceState == null) {
                    if (mEntry == null) {
                        mEntry = ListDbHelper.getEntry(getContext(), mId);
                        ListDbHelper.updateEntry(getContext(), mEntry, LIST_ITEMS_COLUMN_VIEWED);
                    }

                    if (mEntry != null && mEntry.getId() != null) {
                        fillActivityViews();

                    } else {
                        finish(null);
                        final String error = getString(R.string.fragment_item_details_edit_mode_wrong_id);
                        Log.e(TAG, error);
                        showToast(
                                getContext(),
                                error,
                                Toast.LENGTH_LONG);
                    }

                } else {
                    mEntry = mSavedInstanceState.getParcelable(EXTRA_ENTRY);
                    fillActivityViews();
                }
                break;

            default:
                break;
        }
    }

    private void initImageUrlValidation() {
        checkUrlAndSetError(mImageUrlEditText.getText().toString());

        mImageUrlEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkUrlAndSetError(mImageUrlEditText.getText().toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void initImageView() {
        final View loadingProgressBar = mParentView.findViewById(R.id.fragment_item_details_image_view_progress);
        final View errorImageView = mParentView.findViewById(R.id.fragment_item_details_image_view_error);
        final String pathToImage = mImageUrlEditText.getText().toString();

        if (!TextUtils.isEmpty(pathToImage)) {
            loadingProgressBar.setVisibility(View.VISIBLE);
            mImageView.setVisibility(View.VISIBLE);
            errorImageView.setVisibility(View.GONE);

            Picasso.with(getContext())
                    .load(pathToImage)
                    .fit()
                    .centerInside()
                    .into(mImageView, new Callback() {
                        @Override
                        public void onSuccess() {
                            loadingProgressBar.setVisibility(View.GONE);
                        }
                        @Override
                        public void onError() {
                            loadingProgressBar.setVisibility(View.GONE);
                            mImageView.setVisibility(View.GONE);
                            errorImageView.setVisibility(View.VISIBLE);
                        }
                    });

        } else {
            loadingProgressBar.setVisibility(View.GONE);
            mImageView.setVisibility(View.GONE);
            errorImageView.setVisibility(View.VISIBLE);
        }
    }

    private void initRefreshImageButton() {
        mParentView.findViewById(R.id.fragment_item_details_image_url_refresh_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        initImageView();
                    }
                });
    }

    /**
     * Check URL, and set error if needed.
     * @param url Checked URL.
     * @return True if invalid, otherwise false.
     */
    private boolean checkUrlAndSetError(
            @NonNull final String url) {

        if (!Patterns.WEB_URL.matcher(url).matches()) {
            mImageUrlEditTextLayout.setError(
                    getString(R.string.fragment_item_details_image_url_error));
            return true;

        } else {
            mImageUrlEditTextLayout.setError(null);
            return false;
        }
    }

    /**
     * Starting choosing color.
     */
    private void setColorViewListener() {
        mColorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshEntry();

                final ColorPickerFragment fragment =
                        ColorPickerFragment.newInstance(mId, mColor);

                // Transitions.
                final View headerBox = mParentView.findViewById(R.id.fragment_item_details_ff_header);
                final String colorTransName = getString(R.string.shared_transition_name_color_box);
                final String headerBoxTransName = getString(R.string.shared_transition_name_layout);
                ViewCompat.setTransitionName(mColorView, colorTransName);
                ViewCompat.setTransitionName(headerBox, headerBoxTransName);

                fragment.setSharedElementEnterTransition(new AutoTransition());
                fragment.setSharedElementReturnTransition(new AutoTransition());

                getActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .addSharedElement(headerBox, headerBoxTransName)
                        .addSharedElement(mColorView, colorTransName)
                        .replace(R.id.activity_main_fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
    }


    /*
        Updating views
     */

    /**
     * Fill all activity views with values from the entry-object.
     */
    private void fillActivityViews() {
        mTitleEditText.setText(mEntry.getTitle());
        mDescEditText.setText(mEntry.getDescription());
        mImageUrlEditText.setText(mEntry.getImageUrl());
        refreshColorBox(mEntry.getColor());
    }

    /**
     * Fill entry-fields with values from all activity views.
     */
    private void refreshEntry() {
        mEntry.setTitle(mTitleEditText.getText().toString());
        mEntry.setDescription(mDescEditText.getText().toString());
        mEntry.setColor(mColor);
        mEntry.setImageUrl(mImageUrlEditText.getText().toString());
    }

    /**
     * Refresh color box, with given color.
     * @param color New color.
     */
    private void refreshColorBox(final int color) {
        mColor = color;
        mColorView.setBackground(
                getGradientDrawableCircleWithBorder(color));
    }

    /**
     * @param color Color of drawable.
     * @return New gradient drawable, circle, with border.
     */
    @NonNull
    private GradientDrawable getGradientDrawableCircleWithBorder(final int color) {
        final GradientDrawable g = new GradientDrawable();
        g.setStroke(
                (int) getResources().getDimension(R.dimen.fragment_item_details_ff_color_stroke_width),
                getResourceColorApi(getContext(), android.R.color.primary_text_dark));
        g.setCornerRadius(getResources().getDimension(R.dimen.fragment_item_details_ff_color_radius));
        g.setColor(color);
        return g;
    }


    /*
        Options menu
     */

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        menu.clear();
        mMenu = menu;
        inflater.inflate(R.menu.menu_list_item_details, menu);

        if (ACTION_EDIT.equals(mAction)) {
            menu.findItem(R.id.menu_list_item_details_info).setVisible(true);
            menu.findItem(R.id.menu_list_item_details_delete).setVisible(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_list_item_details_done:
                switch (mAction) {
                    case ACTION_ADD:
                        startActionAsyncTask(ACTION_ENTRY_ADD);
                        break;
                    case ACTION_EDIT:
                        startActionAsyncTask(ACTION_ENTRY_EDIT);
                        break;
                    default:
                        break;
                }
                break;

            case R.id.menu_list_item_details_cancel:
                finish(null);
                break;

            case R.id.menu_list_item_details_info:
                showInfoDialog();
                break;

            case R.id.menu_list_item_details_delete:
                showConfirmDeleteDialog();
                break;

            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    /**
     * Show info dialog with next info: created, edited, viewed dates.
     */
    private void showInfoDialog() {
        final StringBuilder infoMessage = new StringBuilder();
        infoMessage
                .append(getString(R.string.fragment_item_details_info_dialog_message_created))
                .append("\n")
                .append(getStringDateFormatSqlite(getContext(), mEntry.getCreated(), false))
                .append("\n\n")
                .append(getString(R.string.fragment_item_details_info_dialog_message_edited))
                .append("\n")
                .append(getStringDateFormatSqlite(getContext(), mEntry.getEdited(), false))
                .append("\n\n")
                .append(getString(R.string.fragment_item_details_info_dialog_message_viewed))
                .append("\n")
                .append(getStringDateFormatSqlite(getContext(), mEntry.getViewed(), false))
                .append("\n\n")
                .append(getString(R.string.fragment_item_details_info_dialog_message_sync_id))
                .append("\n")
                .append(mEntry.getSyncId());

        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder .setTitle(getString(R.string.fragment_item_details_info_dialog_title))
                .setMessage(infoMessage)
                .setNegativeButton(
                        getString(R.string.fragment_item_details_info_dialog_button_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                .create()
                .show();
    }

    private void showConfirmDeleteDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder .setTitle(getString(R.string.fragment_item_details_delete_dialog_title))
                .setMessage(getString(R.string.fragment_item_details_delete_dialog_message))
                .setPositiveButton(getString(R.string.fragment_item_details_delete_dialog_button_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                startActionAsyncTask(ACTION_ENTRY_DELETE);
                            }
                        })
                .setNegativeButton(
                        getString(R.string.fragment_item_details_delete_dialog_button_cancel),
                        null)
                .create()
                .show();
    }

    private void startActionAsyncTask(@NonNull final String action) {
        showProgressBarAndHideMenuItems();

        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                switch (action) {
                    case ACTION_ENTRY_ADD:
                        refreshEntry();
                        ListDbHelper.insertUpdateEntry(
                                getContext(),
                                mEntry,
                                false);
                        break;

                    case ACTION_ENTRY_EDIT:
                        refreshEntry();
                        ListDbHelper.updateEntry(
                                getContext(),
                                mEntry,
                                LIST_ITEMS_COLUMN_EDITED);
                        break;

                    case ACTION_ENTRY_DELETE:
                        ListDbHelper.deleteEntry(getContext(), mEntry.getId(), true);
                        break;

                    default:
                        break;
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        finish(action);
                    }
                });
            }
        });
        thread.start();
    }

    private void showProgressBarAndHideMenuItems() {
        // Replace color box with progress bar.
        mParentView.findViewById(R.id.fragment_item_details_color)
                .setVisibility(View.GONE);
        mParentView.findViewById(R.id.fragment_item_details_progress_bar_replacer)
                .setVisibility(View.VISIBLE);

        // Hide menu items.
        mMenu.findItem(R.id.menu_list_item_details_done).setVisible(false);
        mMenu.findItem(R.id.menu_list_item_details_cancel).setVisible(false);
        mMenu.findItem(R.id.menu_list_item_details_delete).setVisible(false);
    }


    /*
        Observer
     */

    @Override
    public void onNotify(final int resultCode, @Nullable final Bundle data) {
        switch (resultCode) {
            case RESULT_CODE_COLOR_PICKER_SELECTED:
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (data == null
                                || data.getLong(ColorPickerFragment.EXTRA_ID, -1) != mId) {
                            return;
                        }
                        final int color = data.getInt(
                                EXTRA_RESULT_COLOR,
                                getDefaultColor(getAppContext()));
                        mEntry.setColor(color);
                        fillActivityViews();
                    }
                });
            default:
                break;
        }
    }


    /*
        Finish
     */

    private void finish(@Nullable final String action) {
        if (action != null) {
            int resultCode = RESULT_CODE_ENTRY_CANCEL;
            switch (action) {
                case ACTION_ENTRY_ADD:
                    resultCode = RESULT_CODE_ENTRY_ADDED;
                    showToast(
                            getContext(),
                            getString(R.string.fragment_list_notification_entry_added),
                            Toast.LENGTH_SHORT);
                    break;

                case ACTION_ENTRY_EDIT:
                    resultCode = RESULT_CODE_ENTRY_EDITED;
                    showToast(
                            getContext(),
                            getString(R.string.fragment_list_notification_entry_updated),
                            Toast.LENGTH_SHORT);
                    break;

                case ACTION_ENTRY_DELETE:
                    resultCode = RESULT_CODE_ENTRY_DELETED;
                    showToast(
                            getContext(),
                            getString(R.string.fragment_list_notification_entry_deleted),
                            Toast.LENGTH_SHORT);
                    break;

                default:
                    break;
            }

            final Bundle bundle = new Bundle();
            bundle.putLong(EXTRA_ID, mId);

            notifyObservers(
                    ENTRY,
                    resultCode,
                    bundle);
        }

        getActivity().onBackPressed();
    }
}