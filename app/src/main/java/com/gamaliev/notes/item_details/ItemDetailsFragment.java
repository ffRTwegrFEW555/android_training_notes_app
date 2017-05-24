package com.gamaliev.notes.item_details;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import com.gamaliev.notes.list.ListFragment;
import com.gamaliev.notes.list.db.ListDbHelper;
import com.gamaliev.notes.model.ListEntry;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import static android.app.Activity.RESULT_OK;
import static com.gamaliev.notes.common.CommonUtils.getDefaultColor;
import static com.gamaliev.notes.common.CommonUtils.getResourceColorApi;
import static com.gamaliev.notes.common.CommonUtils.getStringDateFormatSqlite;
import static com.gamaliev.notes.common.CommonUtils.showToast;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_EDITED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_VIEWED;
import static com.gamaliev.notes.list.ListFragment.RESULT_CODE_EXTRA_ADDED;
import static com.gamaliev.notes.list.ListFragment.RESULT_CODE_EXTRA_DELETED;
import static com.gamaliev.notes.list.ListFragment.RESULT_CODE_EXTRA_EDITED;

public final class ItemDetailsFragment extends Fragment {

    /* Logger */
    private static final String TAG = ItemDetailsFragment.class.getSimpleName();

    /* Action */
    public static final String ACTION      = "ItemDetailsFragment.ACTION";
    public static final String ACTION_ADD  = "ItemDetailsFragment.ACTION_ADD";
    public static final String ACTION_EDIT = "ItemDetailsFragment.ACTION_EDIT";

    /* Extra */
    private static final String EXTRA_ID    = "ItemDetailsFragment.EXTRA_ID";
    private static final String EXTRA_ENTRY = "ItemDetailsFragment.EXTRA_ENTRY";

    /* Request code */
    private static final int REQUEST_CODE_COLOR = 1;

    /* ... */
    @NonNull private View mParentView;
    @NonNull private ActionBar mActionBar;
    @NonNull private View mColorView;
    @NonNull private EditText mTitleEditText;
    @NonNull private EditText mDescEditText;
    @NonNull private EditText mImageUrlEditText;
    @NonNull private ImageView mImageView;
    @NonNull private TextInputLayout mImageUrlEditTextLayout;
    @NonNull private ListEntry mEntry;
    @NonNull private Menu mMenu;
    @NonNull private String mAction;
    @Nullable private Bundle mSavedInstanceState;
    private int mColor;
    private long mId;


    /*
        Init
     */

    public static ItemDetailsFragment newInstance(
            @NonNull final String action,
            final long id) {

        final Bundle bundle = new Bundle();
        bundle.putString(ACTION, action);
        bundle.putLong(EXTRA_ID, id);

        final ItemDetailsFragment fragment = new ItemDetailsFragment();
        fragment.setArguments(bundle);
        
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAction = getArguments().getString(ACTION);
        mId = getArguments().getLong(EXTRA_ID);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        mParentView = inflater.inflate(
                R.layout.fragment_item_details,
                container,
                false);

        init(savedInstanceState);
        return mParentView;
    }

    private void init(@Nullable final Bundle savedInstanceState) {
        mActionBar      = ((AppCompatActivity) getActivity()).getSupportActionBar();
        mColorView      = mParentView.findViewById(R.id.fragment_item_details_color);
        mTitleEditText  = (EditText) mParentView.findViewById(R.id.fragment_item_details_title_text_view);
        mDescEditText   = (EditText) mParentView.findViewById(R.id.fragment_item_details_description_text_view);
        mImageUrlEditText = (EditText) mParentView.findViewById(R.id.fragment_item_details_image_url_text_view);
        mImageUrlEditTextLayout = (TextInputLayout) mParentView.findViewById(
                R.id.fragment_item_details_image_url_text_input_layout);
        mImageView      = (ImageView) mParentView.findViewById(R.id.fragment_item_details_image_view);
        mSavedInstanceState = savedInstanceState;

        setColorBoxListener();
        processAction();
        initImageUrlValidation();
        initImageView();
        initRefreshImageButton();
        initTransition();
    }

    /**
     * Set color box listener.<br>
     * Start choosing color activity on click.<br>
     * If API >= 21, then enable shared transition color box.
     */
    private void setColorBoxListener() {
        mColorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // After start - refreshed entry will be use in
                // onSaveInstanceState()-method.
                refreshEntry();

                // Start color picker fragment.
                final ColorPickerFragment fragment =
                        ColorPickerFragment.newInstance(mColor);

                // Init transitions.
                final View headerBox = mParentView.findViewById(R.id.fragment_item_details_ff_header);
                final String colorTransName = getString(R.string.shared_transition_name_color_box);
                final String headerBoxTransName = getString(R.string.shared_transition_name_layout);
                ViewCompat.setTransitionName(mColorView, colorTransName);
                ViewCompat.setTransitionName(headerBox, headerBoxTransName);

                setExitTransition(new Fade());
                fragment.setEnterTransition(new Fade());
                fragment.setSharedElementEnterTransition(new AutoTransition());
                fragment.setSharedElementReturnTransition(new AutoTransition());

                // Start fragment.
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

    /**
     * Process Action, when start activity.<br>
     * Filling in the necessary views.<br>
     * See also: {@link #ACTION_ADD}, {@link #ACTION_EDIT}.
     */
    private void processAction() {
        //
        mActionBar.setElevation(0);

        switch (mAction) {

            // Start activity with Add action.
            case ACTION_ADD:
                mActionBar.setTitle(getString(R.string.fragment_item_details_title_add));

                if (mSavedInstanceState == null) {

                    // On first start activity.
                    // Set color box with default color, and create new entry.
                    refreshColorBox(getDefaultColor(getContext()));
                    mEntry = new ListEntry();
                    refreshEntry();
                } else {

                    // On restart activity.
                    // Fill activity views values with restored entry-values.
                    mEntry = mSavedInstanceState.getParcelable(EXTRA_ENTRY);
                    fillActivityViews();
                }
                break;

            // Start activity with Edit action.
            case ACTION_EDIT:
                mActionBar.setTitle(getString(R.string.fragment_item_details_title_edit));

                if (mSavedInstanceState == null) {

                    // On first start activity. Get entry from database, with given id.
                    mEntry = ListDbHelper.getEntry(getContext(), mId);

                    // If received object and id is not null.
                    // Else finish.
                    if (mEntry != null && mEntry.getId() != null) {
                        // Fill activity views values with received values.
                        fillActivityViews();

                        // Update viewed date.
                        ListDbHelper.updateEntry(getContext(), mEntry, LIST_ITEMS_COLUMN_VIEWED);

                    } else {
                        finish();
                        final String error = getString(R.string.fragment_item_details_edit_mode_wrong_id);
                        Log.e(TAG, error);
                        showToast(
                                getContext(),
                                error,
                                Toast.LENGTH_LONG);
                    }

                } else {

                    // On restart activity.
                    // Fill activity views values with restored entry-values.
                    mEntry = mSavedInstanceState.getParcelable(EXTRA_ENTRY);
                    fillActivityViews();
                }
                break;

            default:
                break;
        }
    }

    private void initTransition() {
        ViewCompat.setTransitionName(
                mColorView,
                getString(R.string.shared_transition_name_color_box));
        ViewCompat.setTransitionName( // TODO: bug. Try remove, when ListView -> RecyclerView
                mParentView.findViewById(R.id.fragment_item_details_ff_header),
                getString(R.string.shared_transition_name_layout));
    }

    private void initImageUrlValidation() {
        //
        checkUrlAndSetError(mImageUrlEditText.getText().toString());

        // URL validation on change.
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


    /*
        Methods
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
     * Refresh color box, with given color, and color-variable of object.
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
        Intents
     */

    /**
     * Start intent with Add action.
     * @param context       Context.
     * @param requestCode   This code will be returned in onActivityResult() when the activity exits.
     * See {@link #ACTION_ADD}.
     */
    public static void startAdd(
            @NonNull final Context context,
            final int requestCode) {

        Intent starter = new Intent(context, ItemDetailsFragment.class);
        starter.setAction(ACTION_ADD);
        ((Activity) context).startActivityForResult(starter, requestCode);
    }

    /**
     * Start intent with Edit action.
     * @param context       Context.
     * @param id            Id of entry, that link with intent, see: {@link #EXTRA_ID}.
     * @param requestCode   This code will be returned in onActivityResult() when the activity exits.
     * @param bundle        Additional options for how the Activity should be started.
     *                      If null, then start {@link Activity#startActivityForResult(Intent, int)},
     *                      otherwise start {@link Activity#startActivityForResult(Intent, int, Bundle)},
     * See also: {@link #ACTION_EDIT}.
     */
    public static void startEdit(
            @NonNull final Context context,
            final long id,
            final int requestCode,
            @Nullable final Bundle bundle) {

        Intent starter = new Intent(context, ItemDetailsFragment.class);
        starter.setAction(ACTION_EDIT);
        starter.putExtra(EXTRA_ID, id);
        if (bundle == null) {
            ((Activity) context).startActivityForResult(starter, requestCode);
        } else {
            ((Activity) context).startActivityForResult(starter, requestCode, bundle);
        }
    }

    /**
     * @param color Color.
     * @return Intent, with given color.
     * See {@link ColorPickerFragment#EXTRA_COLOR}
     */
    @NonNull
    public static Intent getResultColorIntent(final int color) {
        Intent intent = new Intent();
        intent.putExtra(ColorPickerFragment.EXTRA_COLOR, color);
        return intent;
    }

    /**
     * If color was selected, then refresh activity views.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_COLOR && resultCode == RESULT_OK) {
            if (data != null) {

                // Get selected color. If null, using default color.
                int color = data.getIntExtra(
                        ColorPickerFragment.EXTRA_COLOR,
                        getDefaultColor(getContext()));

                // Update entry, then activity views.
                mEntry.setColor(color);
                fillActivityViews();
            }
        }
    }


    /*
        Options menu
     */

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mMenu = menu;
        inflater.inflate(R.menu.menu_list_item_details, menu);

        // If "edit action", then set info and delete buttons to visible.
        if (ACTION_EDIT.equals(mAction)) {
            menu.findItem(R.id.menu_list_item_details_info).setVisible(true);
            menu.findItem(R.id.menu_list_item_details_delete).setVisible(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            // On save action.
            // Create database -> process action -> close database.
            case R.id.menu_list_item_details_done:

                switch (mAction) {

                    // If new, then add to database, and finish activity with RESULT_OK.
                    case ACTION_ADD:
                        //
                        startActionAsyncTask(RESULT_CODE_EXTRA_ADDED);
                        break;

                    // If edit, then update entry in database, and finish activity with RESULT_OK.
                    case ACTION_EDIT:
                        //
                        startActionAsyncTask(RESULT_CODE_EXTRA_EDITED);
                        break;

                    default:
                        break;
                }

                break;

            // On cancel action. Finish activity.
            case R.id.menu_list_item_details_cancel:
                finish();
                break;

            // Info button
            case R.id.menu_list_item_details_info:
                showInfoDialog();
                break;

            // Delete button
            case R.id.menu_list_item_details_delete:
                showConfirmDeleteDialog();
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Show info dialog with next info: created, edited, viewed dates.
     */
    private void showInfoDialog() {

        // Create message.
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

        // Create alert dialog.
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder .setTitle(getString(R.string.fragment_item_details_info_dialog_title))
                .setMessage(infoMessage)
                .setNegativeButton(
                        getString(R.string.fragment_item_details_info_dialog_button_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        final AlertDialog alert = builder.create();

        // Show
        alert.show();
    }

    /**
     * Show confirm delete dialog with Ok, Cancel buttons.
     */
    private void showConfirmDeleteDialog() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder .setTitle(getString(R.string.fragment_item_details_delete_dialog_title))
                .setMessage(getString(R.string.fragment_item_details_delete_dialog_message))
                .setPositiveButton(getString(R.string.fragment_item_details_delete_dialog_button_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //
                                dialog.cancel();
                                //
                                startActionAsyncTask(RESULT_CODE_EXTRA_DELETED);
                            }
                        })
                .setNegativeButton(
                        getString(R.string.fragment_item_details_delete_dialog_button_cancel),
                        null);

        final AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Show progress bar, hide menu items, open database,
     * perform action in async mode, return result, finish activity.
     * @param resultCode    See:
     *                      {@link ListFragment#RESULT_CODE_EXTRA_ADDED},
     *                      {@link ListFragment#RESULT_CODE_EXTRA_EDITED},
     *                      {@link ListFragment#RESULT_CODE_EXTRA_DELETED},
     */
    private void startActionAsyncTask(final int resultCode) {
        //
        showProgressBarAndHideMenuItems();

        // Background task.
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                //
                switch (resultCode) {

                    case RESULT_CODE_EXTRA_ADDED:
                        refreshEntry();
                        ListDbHelper.insertUpdateEntry(
                                getContext(),
                                mEntry,
                                false);
                        break;

                    case RESULT_CODE_EXTRA_EDITED:
                        refreshEntry();
                        ListDbHelper.updateEntry(
                                getContext(),
                                mEntry,
                                LIST_ITEMS_COLUMN_EDITED);
                        break;

                    case RESULT_CODE_EXTRA_DELETED:
                        ListDbHelper.deleteEntry(getContext(), mEntry.getId(), true);
                        break;

                    default:
                        break;
                }

                // Finish.
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        finish();
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
        On Restore / Save instance state
     */

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(EXTRA_ENTRY, mEntry);
        super.onSaveInstanceState(outState);
    }


    /*
        ...
     */

    private void finish() {
        getActivity().onBackPressed();
    }
}