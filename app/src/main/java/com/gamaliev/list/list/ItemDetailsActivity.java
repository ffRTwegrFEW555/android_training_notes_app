package com.gamaliev.list.list;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.gamaliev.list.R;
import com.gamaliev.list.colorpicker.ColorPickerActivity;
import com.gamaliev.list.common.CommonUtils;

import static com.gamaliev.list.common.CommonUtils.getDefaultColor;
import static com.gamaliev.list.common.CommonUtils.getResourceColorApi;

public class ItemDetailsActivity extends AppCompatActivity {

    private static final String ACTION_ADD      = "add";
    private static final String ACTION_EDIT     = "edit";
    private static final String EXTRA_ID        = "id";

    private static final int REQUEST_CODE_COLOR = 1;
    private static final String EXTRA_COLOR     = "color";
    private static final String EXTRA_ENTRY     = "entry";
    private static final String EXTRA_CHANGEABLE_ENTRY = "changeableEntry";

    @NonNull private ListDatabaseHelper dbHelper;
    @NonNull private ActionBar actionBar;
    @NonNull private View colorBox;
    @NonNull private EditText name;
    @NonNull private EditText description;
    @Nullable private ListEntry changeableEntry;
    @Nullable private Bundle savedInstanceState;
    private int color;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_details);
        init(savedInstanceState);
    }

    private void init(@Nullable final Bundle savedInstanceState) {
        actionBar = getSupportActionBar();
        colorBox = findViewById(R.id.activity_item_details_color);
        name = (EditText) findViewById(R.id.activity_item_details_text_view_name);
        description = (EditText) findViewById(R.id.activity_item_details_text_view_description);
        this.savedInstanceState = savedInstanceState;

        enableEnterSharedTransition();
        setColorBoxListener();
        processAction();
    }


    /*
        Methods
     */

    /**
     * Enable shared transition.
     */
    private void enableEnterSharedTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setSharedElementEnterTransition(CommonUtils.getChangeBounds(this));
            findViewById(android.R.id.content).invalidate();
        }
    }

    /**
     * Start choosing color on click.<br>
     * If API >= 19, then enable shared transition color box.
     */
    private void setColorBoxListener() {
        colorBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    View iconView = findViewById(R.id.activity_item_details_color);
                    iconView.setTransitionName(
                            getString(R.string.shared_transition_name_color_box));
                    Pair<View, String> icon = new Pair<>(iconView, iconView.getTransitionName());
                    ActivityOptionsCompat aoc =
                            ActivityOptionsCompat.makeSceneTransitionAnimation(
                                    ItemDetailsActivity.this, icon);
                    Intent intent = ColorPickerActivity.getStartIntent(ItemDetailsActivity.this, color);
                    startActivityForResult(intent, REQUEST_CODE_COLOR, aoc.toBundle());

                } else {
                    Intent intent = ColorPickerActivity.getStartIntent(ItemDetailsActivity.this, color);
                    startActivityForResult(intent, REQUEST_CODE_COLOR);
                }
            }
        });
    }

    /**
     * Process Action, when start activity.<br>
     * See also: {@link #ACTION_ADD}, {@link #ACTION_EDIT}.
     */
    private void processAction() {
        switch (getIntent().getAction()) {

            // Change action bar title, and set default color.
            case ACTION_ADD:
                actionBar.setTitle(getResources().getString(R.string.activity_item_details_title_add));
                if (savedInstanceState != null) {
                    refreshColorBox(savedInstanceState.getInt(EXTRA_COLOR, color));
                } else {
                    refreshColorBox(getDefaultColor(this));
                }
                break;

            // Change action bar title, and fill all fields
            case ACTION_EDIT:
                actionBar.setTitle(getResources().getString(R.string.activity_item_details_title_edit));
                if (savedInstanceState != null) {
                    // On restart activity
                    ListEntry entry = savedInstanceState.getParcelable(EXTRA_ENTRY);
                    name.setText(entry.getName());
                    description.setText(entry.getDescription());
                    refreshColorBox(savedInstanceState.getInt(EXTRA_COLOR, color));
                    changeableEntry = savedInstanceState
                            .getParcelable(EXTRA_CHANGEABLE_ENTRY);

                } else {
                    // Get entry from database, with given id.
                    long id = getIntent().getLongExtra(EXTRA_ID, -1);
                    dbHelper = new ListDatabaseHelper(this);
                    changeableEntry = dbHelper.getEntry(id);
                    dbHelper.close();
                    if (changeableEntry != null) {
                        name.setText(changeableEntry.getName());
                        description.setText(changeableEntry.getDescription());
                        refreshColorBox(changeableEntry.getColor());
                    }
                }
                break;

            default:
                break;
        }
    }

    /**
     * Refresh color box, with given color.
     * @param color new color.
     */
    private void refreshColorBox(final int color) {
        this.color = color;
        colorBox.setBackground(
                getGradientDrawableCircleWithBorder(color));
    }

    /**
     * @param color color of drawable.
     * @return new gradient drawable, circle, with border.
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

    /**
     * @return ListEntry, associated with current activity data.<br>
     * See also: {@link com.gamaliev.list.list.ListEntry}
     */
    @NonNull
    private ListEntry getFilledEntry() {
        final ListEntry entry = new ListEntry();
        entry.setName(name.getText().toString());
        entry.setDescription(description.getText().toString());
        entry.setColor(color);
        return entry;
    }


    /*
        Intents
     */

    /**
     * Start intent with Add action.
     * @param context       context.
     * @param requestCode   this code will be returned in onActivityResult() when the activity exits.
     * See {@link #ACTION_ADD}.
     */
    public static void startAdd(@NonNull final Context context, final int requestCode) {
        Intent starter = new Intent(context, ItemDetailsActivity.class);
        starter.setAction(ACTION_ADD);
        ((Activity) context).startActivityForResult(starter, requestCode);
    }

    /**
     * Start intent with Edit action.
     * @param context       context.
     * @param id            id of entry, that link with intent, see: {@link #EXTRA_ID}.
     * @param requestCode   this code will be returned in onActivityResult() when the activity exits.
     * @param bundle        Additional options for how the Activity should be started.
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
     * @param color color.
     * @return intent, with given color.
     * See {@link com.gamaliev.list.colorpicker.ColorPickerActivity#EXTRA_COLOR}
     */
    @NonNull
    public static Intent getResultColorIntent(final int color) {
        Intent intent = new Intent();
        intent.putExtra(ColorPickerActivity.EXTRA_COLOR, color);
        return intent;
    }

    /**
     * If color was selected, then refresh color box.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_COLOR && resultCode == RESULT_OK) {
            if (data != null) {
                int color = data.getIntExtra(
                        ColorPickerActivity.EXTRA_COLOR,
                        getDefaultColor(ItemDetailsActivity.this));
                refreshColorBox(color);
                if (savedInstanceState != null) {
                    savedInstanceState.putInt(EXTRA_COLOR, color);
                }
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
        getMenuInflater().inflate(R.menu.menu_list_item_details, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Action bar menu item selection handler
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            // On save action.
            case R.id.menu_list_item_details_done:
                dbHelper = new ListDatabaseHelper(this);

                switch (getIntent().getAction()) {

                    // If new, then add to database, and finish activity.
                    // TODO: return result with notify;
                    case ACTION_ADD:
                        ListEntry newEntry = new ListEntry();
                        newEntry.setName(name.getText().toString());
                        newEntry.setDescription(description.getText().toString());
                        newEntry.setColor(color);
                        dbHelper.insertEntry(newEntry);
                        setResult(RESULT_OK);
                        finish();
                        break;

                    // If edit, then update entry in database, and finish activity.
                    // TODO: return result with notify;
                    case ACTION_EDIT:
                        if (changeableEntry != null) {
                            changeableEntry.setName(name.getText().toString());
                            changeableEntry.setDescription(description.getText().toString());
                            changeableEntry.setColor(color);
                            dbHelper.updateEntry(changeableEntry);
                        }
                        setResult(RESULT_OK);
                        finish();
                        break;

                    default:
                        break;
                }

                dbHelper.close();
                break;

            // On cancel action.
            case R.id.menu_list_item_details_cancel:
                finish();
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    /*
        On Restore / Save instance state
     */

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(EXTRA_COLOR, color);
        outState.putParcelable(EXTRA_ENTRY, getFilledEntry());
        outState.putParcelable(EXTRA_CHANGEABLE_ENTRY, changeableEntry);
        super.onSaveInstanceState(outState);
    }
}
