package com.gamaliev.list.list;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.gamaliev.list.R;
import com.gamaliev.list.colorpicker.ColorPickerActivity;

import static com.gamaliev.list.common.CommonUtils.getDefaultColor;
import static com.gamaliev.list.common.CommonUtils.getResourceColorApi;
import static com.gamaliev.list.common.CommonUtils.getStringDateFormatSqlite;
import static com.gamaliev.list.common.CommonUtils.showToast;
import static com.gamaliev.list.common.DatabaseHelper.LIST_ITEMS_COLUMN_VIEWED;
import static com.gamaliev.list.list.ListActivity.RESULT_CODE_EXTRA_ADDED;
import static com.gamaliev.list.list.ListActivity.RESULT_CODE_EXTRA_DELETED;
import static com.gamaliev.list.list.ListActivity.RESULT_CODE_EXTRA_EDITED;

public class ItemDetailsActivity extends AppCompatActivity {

    /* Logger */
    private static final String TAG = ItemDetailsActivity.class.getSimpleName();

    /* Action */
    private static final String ACTION_ADD  = "ItemDetailsActivity.ACTION_ADD";
    private static final String ACTION_EDIT = "ItemDetailsActivity.ACTION_EDIT";

    /* Extra */
    private static final String EXTRA_ID    = "ItemDetailsActivity.EXTRA_ID";
    private static final String EXTRA_ENTRY = "ItemDetailsActivity.EXTRA_ENTRY";

    /* Request code */
    private static final int REQUEST_CODE_COLOR = 1;

    /* */
    @NonNull private ListDatabaseHelper mDbHelper;
    @NonNull private ActionBar mActionBar;
    @NonNull private View mColorView;
    @NonNull private EditText mTitleEditText;
    @NonNull private EditText mDescEditText;
    @NonNull private ListEntry mEntry;
    @NonNull private Menu mMenu;
    @Nullable private Bundle mSavedInstanceState;
    private int mColor;


    /*
        Init
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_details);
        init(savedInstanceState);
    }

    private void init(@Nullable final Bundle savedInstanceState) {
        initToolbar();

        mActionBar      = getSupportActionBar();
        mColorView      = findViewById(R.id.activity_item_details_color);
        mTitleEditText  = (EditText) findViewById(R.id.activity_item_details_text_view_title);
        mDescEditText   = (EditText) findViewById(R.id.activity_item_details_text_view_description);
        mSavedInstanceState = savedInstanceState;

        enableEnterSharedTransition();
        setColorBoxListener();
        processAction();
    }

    /**
     * Init toolbar.
     */
    private void initToolbar() {
        // Set toolbar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.activity_item_details_toolbar);
        setSupportActionBar(toolbar);
    }

    /**
     * Enable shared transition. Work if API >= 21.
     */
    private void enableEnterSharedTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setSharedElementEnterTransition(
                    TransitionInflater
                            .from(this)
                            .inflateTransition(R.transition.transition_activity_1));
            findViewById(android.R.id.content).invalidate();
        }
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
                // After start another activity - refreshed entry will be use in
                // onSaveInstanceState()-method.
                refreshEntry();

                // Start activity for result.
                // If API >= 21, then use shared transition animation.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                    // Prepare.
                    View colorBox = findViewById(R.id.activity_item_details_color);
                    colorBox.setTransitionName(
                            getString(R.string.shared_transition_name_color_box));
                    Pair<View, String> icon = new Pair<>(colorBox, colorBox.getTransitionName());
                    ActivityOptionsCompat aoc =
                            ActivityOptionsCompat.makeSceneTransitionAnimation(
                                    ItemDetailsActivity.this, icon);

                    // Start activity for result with shared transition animation.
                    ColorPickerActivity.startIntent(
                            ItemDetailsActivity.this,
                            mColor,
                            REQUEST_CODE_COLOR,
                            aoc.toBundle());

                } else {
                    // Start activity for result without shared transition animation.
                    ColorPickerActivity.startIntent(
                            ItemDetailsActivity.this,
                            mColor,
                            REQUEST_CODE_COLOR,
                            null);
                }
            }
        });
    }

    /**
     * Process Action, when start activity.<br>
     * Filling in the necessary views.<br>
     * See also: {@link #ACTION_ADD}, {@link #ACTION_EDIT}.
     */
    private void processAction() {
        switch (getIntent().getAction()) {

            // Start activity with Add action.
            case ACTION_ADD:
                mActionBar.setTitle(getString(R.string.activity_item_details_title_add));

                if (mSavedInstanceState == null) {

                    // On first start activity.
                    // Set color box with default color, and create new entry.
                    refreshColorBox(getDefaultColor(this));
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
                mActionBar.setTitle(getString(R.string.activity_item_details_title_edit));

                if (mSavedInstanceState == null) {

                    // On first start activity. Get entry from database, with given id.
                    final long id = getIntent().getLongExtra(EXTRA_ID, -1);
                    mDbHelper = new ListDatabaseHelper(this);
                    mEntry = mDbHelper.getEntry(id);
                    mDbHelper.close();

                    // If received object and id is not null.
                    // Else finish.
                    if (mEntry != null && mEntry.getId() != null) {
                        // Fill activity views values with received values.
                        fillActivityViews();

                        // Update viewed date.
                        mDbHelper.updateEntry(mEntry, LIST_ITEMS_COLUMN_VIEWED);

                    } else {
                        setResult(RESULT_CANCELED, null);
                        finish();
                        final String error = getString(R.string.activity_item_details_edit_mode_wrong_id);
                        Log.e(TAG, error);
                        showToast(
                                this,
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


    /*
        Methods
     */

    /**
     * Fill all activity views with values from the entry-object.
     */
    private void fillActivityViews() {
        mTitleEditText.setText(mEntry.getTitle());
        mDescEditText.setText(mEntry.getDescription());
        refreshColorBox(mEntry.getColor());
    }

    /**
     * Fill entry-fields with values from all activity views.
     */
    private void refreshEntry() {
        mEntry.setTitle(mTitleEditText.getText().toString());
        mEntry.setDescription(mDescEditText.getText().toString());
        mEntry.setColor(mColor);
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
                (int) getResources().getDimension(R.dimen.activity_item_details_ff_color_stroke_width),
                getResourceColorApi(this, android.R.color.primary_text_dark));
        g.setCornerRadius(getResources().getDimension(R.dimen.activity_item_details_ff_color_radius));
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

        Intent starter = new Intent(context, ItemDetailsActivity.class);
        starter.setAction(ACTION_ADD);
        ((Activity) context).startActivityForResult(starter, requestCode);
    }

    /**
     * Start intent with Edit action.
     * @param context       Context.
     * @param id            Id of entry, that link with intent, see: {@link #EXTRA_ID}.
     * @param requestCode   This code will be returned in onActivityResult() when the activity exits.
     * @param bundle        Additional options for how the Activity should be started.
     *                      If null, then start {@link android.app.Activity#startActivityForResult(Intent, int)},
     *                      otherwise start {@link android.app.Activity#startActivityForResult(Intent, int, Bundle)},
     * See also: {@link #ACTION_EDIT}.
     */
    public static void startEdit(
            @NonNull final Context context,
            final long id,
            final int requestCode,
            @Nullable final Bundle bundle) {

        Intent starter = new Intent(context, ItemDetailsActivity.class);
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
     * See {@link com.gamaliev.list.colorpicker.ColorPickerActivity#EXTRA_COLOR}
     */
    @NonNull
    public static Intent getResultColorIntent(final int color) {
        Intent intent = new Intent();
        intent.putExtra(ColorPickerActivity.EXTRA_COLOR, color);
        return intent;
    }

    /**
     * If color was selected, then refresh activity views.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_COLOR && resultCode == RESULT_OK) {
            if (data != null) {

                // Get selected color. If null, using default color.
                int color = data.getIntExtra(
                        ColorPickerActivity.EXTRA_COLOR,
                        getDefaultColor(ItemDetailsActivity.this));

                // Update entry, then activity views.
                mEntry.setColor(color);
                fillActivityViews();
            }
        }
    }


    /*
        Options menu
     */

    /**
     * Inflate action bar menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        getMenuInflater().inflate(R.menu.menu_list_item_details, menu);

        // If "edit action", then set info and delete buttons to visible.
        if (ACTION_EDIT.equals(getIntent().getAction())) {
            menu.findItem(R.id.menu_list_item_details_info).setVisible(true);
            menu.findItem(R.id.menu_list_item_details_delete).setVisible(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Action bar menu item selection handler
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            // On save action.
            // Create database -> process action -> close database.
            case R.id.menu_list_item_details_done:

                switch (getIntent().getAction()) {

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
                .append(getString(R.string.activity_item_details_info_dialog_message_created))
                .append("\n")
                .append(getStringDateFormatSqlite(this, mEntry.getCreated(), false))
                .append("\n\n")
                .append(getString(R.string.activity_item_details_info_dialog_message_edited))
                .append("\n")
                .append(getStringDateFormatSqlite(this, mEntry.getEdited(), false))
                .append("\n\n")
                .append(getString(R.string.activity_item_details_info_dialog_message_viewed))
                .append("\n")
                .append(getStringDateFormatSqlite(this, mEntry.getViewed(), false));

        // Create alert dialog.
        final AlertDialog.Builder builder = new AlertDialog.Builder(ItemDetailsActivity.this);
        builder .setTitle(getString(R.string.activity_item_details_info_dialog_title))
                .setMessage(infoMessage)
                .setNegativeButton(
                        getString(R.string.activity_item_details_info_dialog_button_ok),
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

        // Create alert dialog.
        final AlertDialog.Builder builder = new AlertDialog.Builder(ItemDetailsActivity.this);
        builder .setTitle(getString(R.string.activity_item_details_delete_dialog_title))
                .setMessage(getString(R.string.activity_item_details_delete_dialog_message))
                .setPositiveButton(getString(R.string.activity_item_details_delete_dialog_button_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //
                                dialog.cancel();
                                //
                                startActionAsyncTask(RESULT_CODE_EXTRA_DELETED);
                            }
                        })
                .setNegativeButton(getString(R.string.activity_item_details_delete_dialog_button_cancel),
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
     * Show progress bar, hide menu items, open database,
     * perform action in async mode, return result, finish activity.
     * @param resultCode    See:
     *                      {@link com.gamaliev.list.list.ListActivity#RESULT_CODE_EXTRA_ADDED},
     *                      {@link com.gamaliev.list.list.ListActivity#RESULT_CODE_EXTRA_EDITED},
     *                      {@link com.gamaliev.list.list.ListActivity#RESULT_CODE_EXTRA_DELETED},
     */
    private void startActionAsyncTask(final int resultCode) {
        //
        showProgressBarAndHideMenuItems();

        // Background task.
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                // Open database, and make action.
                mDbHelper = new ListDatabaseHelper(ItemDetailsActivity.this);

                //
                switch (resultCode) {

                    case RESULT_CODE_EXTRA_ADDED:
                        refreshEntry();
                        mDbHelper.insertEntry(mEntry);
                        break;

                    case RESULT_CODE_EXTRA_EDITED:
                        refreshEntry();
                        mDbHelper.updateEntry(mEntry, null);
                        break;

                    case RESULT_CODE_EXTRA_DELETED:
                        mDbHelper.deleteEntry(mEntry.getId());
                        break;

                    default:
                        break;
                }

                //
                mDbHelper.close();

                //
                makeDemonstrativePause();

                // Finish activity with result.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setResult(
                                RESULT_OK,
                                ListActivity.getResultIntent(resultCode));
                        finish();
                    }
                });
            }
        });
        thread.start();
    }

    private void showProgressBarAndHideMenuItems() {
        // Replace color box with progress bar.
        findViewById(R.id.activity_item_details_color)
                .setVisibility(View.GONE);
        findViewById(R.id.activity_item_details_progress_bar_replacer)
                .setVisibility(View.VISIBLE);

        // Hide menu items.
        mMenu.findItem(R.id.menu_list_item_details_done).setVisible(false);
        mMenu.findItem(R.id.menu_list_item_details_cancel).setVisible(false);
        mMenu.findItem(R.id.menu_list_item_details_delete).setVisible(false);
    }

    private void makeDemonstrativePause() {
        //
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showToast(
                        ItemDetailsActivity.this,
                        getString(R.string.activity_item_details_toast_demonstrative_pause),
                        Toast.LENGTH_SHORT);
            }
        });

        //
        try {
            Thread.sleep(getResources().getInteger(
                    R.integer.activity_item_detail_demonstrative_pause));
        } catch (InterruptedException e) {
            Log.e(TAG, e.toString());
        }

    }


    /*
        On Restore / Save instance state
     */

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(EXTRA_ENTRY, mEntry);
        super.onSaveInstanceState(outState);
    }
}