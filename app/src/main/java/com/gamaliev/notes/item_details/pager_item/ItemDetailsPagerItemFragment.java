package com.gamaliev.notes.item_details.pager_item;

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
import com.gamaliev.notes.color_picker.ColorPickerFragment;
import com.gamaliev.notes.common.observers.Observer;
import com.gamaliev.notes.entity.ListEntry;

import static com.gamaliev.notes.app.NotesApp.getAppContext;
import static com.gamaliev.notes.color_picker.ColorPickerFragment.EXTRA_RESULT_COLOR;
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
import static com.gamaliev.notes.common.observers.ObserverHelper.COLOR_PICKER;
import static com.gamaliev.notes.common.observers.ObserverHelper.ENTRY;
import static com.gamaliev.notes.common.observers.ObserverHelper.notifyObservers;
import static com.gamaliev.notes.common.observers.ObserverHelper.registerObserver;
import static com.gamaliev.notes.common.observers.ObserverHelper.unregisterObserver;

@SuppressWarnings("NullableProblems")
public final class ItemDetailsPagerItemFragment extends Fragment
        implements Observer, ItemDetailsPagerItemContract.View {

    /* Logger */
    @NonNull private static final String TAG = ItemDetailsPagerItemFragment.class.getSimpleName();

    /* Action */
    @SuppressWarnings("WeakerAccess")
    @NonNull public static final String ACTION      = "ItemDetailsPagerItemFragment.ACTION";
    @NonNull public static final String ACTION_ADD  = "ItemDetailsPagerItemFragment.ACTION_ADD";
    @NonNull public static final String ACTION_EDIT = "ItemDetailsPagerItemFragment.ACTION_EDIT";

    @NonNull public static final String ACTION_ENTRY_ADD    = "ItemDetailsPagerItemFragment.ACTION_ENTRY_ADD";
    @NonNull public static final String ACTION_ENTRY_EDIT   = "ItemDetailsPagerItemFragment.ACTION_ENTRY_EDIT";
    @NonNull public static final String ACTION_ENTRY_DELETE = "ItemDetailsPagerItemFragment.ACTION_ENTRY_DELETE";

    /* Extra */
    @NonNull private static final String EXTRA_ENTRY_ID = "ItemDetailsPagerItemFragment.EXTRA_ENTRY_ID";

    /* Observed */
    @NonNull private static final String[] OBSERVED = {COLOR_PICKER};

    /* ... */
    @NonNull private ItemDetailsPagerItemContract.Presenter mPresenter;
    @NonNull private View       mParentView;
    @NonNull private ActionBar  mActionBar;
    @NonNull private View       mColorView;
    @NonNull private EditText   mTitleEditText;
    @NonNull private EditText   mDescEditText;
    @NonNull private EditText   mImageUrlEditText;
    @NonNull private ImageView  mImageView;
    @NonNull private View       mLoadingProgressBar;
    @NonNull private View       mErrorImageView;
    @NonNull private TextInputLayout mImageUrlEditTextLayout;
    @NonNull private Menu       mMenu;
    @NonNull private String     mAction;
    @Nullable private Bundle    mSavedInstanceState;


    /*
        Init
     */

    /**
     * Get new instance of item details pager item fragment.
     * @param action Action. See: {@link #ACTION_ADD}, {@link #ACTION_EDIT}.
     * @param entryId Entry id.
     * @return New instance of item details pager item fragment.
     */
    @NonNull
    public static ItemDetailsPagerItemFragment newInstance(
            @NonNull final String action,
            final long entryId) {

        final Bundle bundle = new Bundle();
        bundle.putString(ACTION, action);
        bundle.putLong(EXTRA_ENTRY_ID, entryId);

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
        mPresenter.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }


    /*
        ...
     */

    private void init(@Nullable final Bundle savedInstanceState) {
        mColorView          = mParentView.findViewById(R.id.fragment_item_details_color);
        mTitleEditText      = (EditText) mParentView.findViewById(R.id.fragment_item_details_title_text_view);
        mDescEditText       = (EditText) mParentView.findViewById(R.id.fragment_item_details_description_text_view);
        mImageUrlEditText   = (EditText) mParentView.findViewById(R.id.fragment_item_details_image_url_text_view);
        mImageUrlEditTextLayout = (TextInputLayout) mParentView.findViewById(
                R.id.fragment_item_details_image_url_text_input_layout);
        mImageView          = (ImageView) mParentView.findViewById(R.id.fragment_item_details_image_view);
        mLoadingProgressBar = mParentView.findViewById(R.id.fragment_item_details_image_view_progress);
        mErrorImageView     = mParentView.findViewById(R.id.fragment_item_details_image_view_error);
        mSavedInstanceState = savedInstanceState;

        initPresenter();
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
        final String action = getArguments().getString(ACTION);
        if (action == null) {
            Log.e(TAG, "Action is null.");
            getActivity().onBackPressed();
        } else {
            mAction = action;
        }
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
        final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar == null) {
            Log.e(TAG, "Action bar is null");
            getActivity().onBackPressed();
            return;
        }
        mActionBar = actionBar;
        mActionBar.setElevation(0);
        setHasOptionsMenu(true);
    }

    /**
     * Process Action, when start fragment.<br>
     * Filling in the necessary views.<br>
     * See also: {@link #ACTION_ADD}, {@link #ACTION_EDIT}.
     */
    private void initAction() {
        ListEntry entry = mPresenter.getEntry();
        
        switch (mAction) {
            case ACTION_ADD:
                mActionBar.setTitle(getString(R.string.fragment_item_details_title_add));
                if (mSavedInstanceState == null) {
                    if (entry == null) {
                        mPresenter.initializeNewEntry();
                    }
                } else {
                    mPresenter.setEntryFromSavedInstanceStateOrDefault(mSavedInstanceState);
                }
                fillActivityViews();
                break;

            case ACTION_EDIT:
                mActionBar.setTitle(getString(R.string.fragment_item_details_title_edit));
                if (mSavedInstanceState == null) {
                    if (entry == null) {
                        entry = mPresenter.initializeEntryFromDb();
                        mPresenter.updateEntryViewed();
                    }

                    if (entry != null && entry.getId() != null) {
                        fillActivityViews();

                    } else {
                        finish(null);
                        final String error = getString(R.string.fragment_item_details_edit_mode_wrong_id);
                        Log.e(TAG, error);
                        showToast(error, Toast.LENGTH_LONG);
                    }

                } else {
                    mPresenter.setEntryFromSavedInstanceStateOrDefault(mSavedInstanceState);
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
        final String pathToImage = mImageUrlEditText.getText().toString();

        if (!TextUtils.isEmpty(pathToImage)) {
            // Show loading.
            mLoadingProgressBar.setVisibility(View.VISIBLE);
            mImageView.setVisibility(View.VISIBLE);
            mErrorImageView.setVisibility(View.GONE);

            mPresenter.loadImage(mImageView, pathToImage);

        } else {
            performErrorLoadingImage();
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
    @SuppressWarnings("UnusedReturnValue")
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

                final ListEntry entry = mPresenter.getEntry();
                if (entry == null) {
                    Log.e(TAG, "Entry is null.");
                    return;
                }

                final Long entryId = entry.getId();
                final Integer color = entry.getColor();
                if (color == null) {
                    Log.e(TAG, "Entry id or color is null.");
                    return;
                }

                final ColorPickerFragment fragment =
                        ColorPickerFragment.newInstance(
                                entryId == null ? -1 : entryId,
                                color);

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

    private void initPresenter() {
        new ItemDetailsPagerItemPresenter(this);
        mPresenter.setEntryId(getArguments().getLong(EXTRA_ENTRY_ID, -1));
        mPresenter.start();
    }


    /*
        Updating views
     */

    /**
     * Fill all activity views with values from the entry-object.
     */
    private void fillActivityViews() {
        final ListEntry entry = mPresenter.getEntry();
        if (entry == null) {
            Log.e(TAG, "Entry is null.");
            return;
        }

        mTitleEditText.setText(entry.getTitle());
        mDescEditText.setText(entry.getDescription());
        mImageUrlEditText.setText(entry.getImageUrl());

        Integer color = entry.getColor();
        if (color == null) {
            color = getDefaultColor(getContext());
            entry.setColor(color);
        }
        refreshColorBox(color);
    }

    /**
     * Refresh color box, with given color.
     * @param color New color.
     */
    private void refreshColorBox(final int color) {
        mColorView.setBackground(getGradientDrawableCircleWithBorder(color));
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
                        mPresenter.startActionAsyncTask(ACTION_ENTRY_ADD);
                        break;
                    case ACTION_EDIT:
                        mPresenter.startActionAsyncTask(ACTION_ENTRY_EDIT);
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
        final ListEntry entry = mPresenter.getEntry();
        if (entry == null
                || entry.getCreated() == null
                || entry.getEdited() == null
                || entry.getViewed() == null) {
            Log.e(TAG, "Get date from entry error.");
            return;
        }

        final StringBuilder infoMessage = new StringBuilder();
        infoMessage
                .append(getString(R.string.fragment_item_details_info_dialog_message_created))
                .append('\n')
                .append(getStringDateFormatSqlite(getContext(), entry.getCreated(), false))
                .append('\n').append('\n')
                .append(getString(R.string.fragment_item_details_info_dialog_message_edited))
                .append('\n')
                .append(getStringDateFormatSqlite(getContext(), entry.getEdited(), false))
                .append('\n').append('\n')
                .append(getString(R.string.fragment_item_details_info_dialog_message_viewed))
                .append('\n')
                .append(getStringDateFormatSqlite(getContext(), entry.getViewed(), false))
                .append('\n').append('\n')
                .append(getString(R.string.fragment_item_details_info_dialog_message_sync_id))
                .append('\n')
                .append(entry.getSyncId());

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
                                mPresenter.startActionAsyncTask(ACTION_ENTRY_DELETE);
                            }
                        })
                .setNegativeButton(
                        getString(R.string.fragment_item_details_delete_dialog_button_cancel),
                        null)
                .create()
                .show();
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
                                || data.getLong(ColorPickerFragment.EXTRA_ID, -1) != mPresenter.getEntryId()) {
                            return;
                        }
                        final int color = data.getInt(
                                EXTRA_RESULT_COLOR,
                                getDefaultColor(getAppContext()));
                        final ListEntry entry = mPresenter.getEntry();
                        if (entry == null) {
                            Log.e(TAG, "Entry is null.");
                            return;
                        }
                        mPresenter.getEntry().setColor(color);
                        fillActivityViews();
                    }
                });
            default:
                break;
        }
    }


    /*
        ItemDetailsPagerItemContract.View
     */

    @Override
    public void setPresenter(ItemDetailsPagerItemContract.Presenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public boolean isActive() {
        return isAdded();
    }

    @Override
    public void showProgressBarAndHideMenuItems() {
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

    @Override
    public void refreshEntry() {
        mPresenter.refreshEntry(
                mTitleEditText.getText().toString(),
                mDescEditText.getText().toString(),
                mImageUrlEditText.getText().toString());
    }

    @Override
    public void performSuccessLoadingImage() {
        mLoadingProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void performErrorLoadingImage() {
        mLoadingProgressBar.setVisibility(View.GONE);
        mImageView.setVisibility(View.GONE);
        mErrorImageView.setVisibility(View.VISIBLE);
    }

    @Override
    public void finish(@Nullable final String action) {
        if (action != null) {
            int resultCode = RESULT_CODE_ENTRY_CANCEL;
            switch (action) {
                case ACTION_ENTRY_ADD:
                    resultCode = RESULT_CODE_ENTRY_ADDED;
                    showToast(getString(R.string.fragment_list_notification_entry_added),
                            Toast.LENGTH_SHORT);
                    break;

                case ACTION_ENTRY_EDIT:
                    resultCode = RESULT_CODE_ENTRY_EDITED;
                    showToast(getString(R.string.fragment_list_notification_entry_updated),
                            Toast.LENGTH_SHORT);
                    break;

                case ACTION_ENTRY_DELETE:
                    resultCode = RESULT_CODE_ENTRY_DELETED;
                    showToast(getString(R.string.fragment_list_notification_entry_deleted),
                            Toast.LENGTH_SHORT);
                    break;

                default:
                    break;
            }

            final Bundle bundle = new Bundle();
            bundle.putLong(EXTRA_ENTRY_ID, mPresenter.getEntryId());

            notifyObservers(
                    ENTRY,
                    resultCode,
                    bundle);
        }

        getActivity().onBackPressed();
    }
}