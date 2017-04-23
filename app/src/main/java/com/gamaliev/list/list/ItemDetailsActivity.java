package com.gamaliev.list.list;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.gamaliev.list.R;
import com.gamaliev.list.colorpicker.ColorPickerActivity;

import static com.gamaliev.list.common.CommonUtils.getDefaultColor;
import static com.gamaliev.list.common.CommonUtils.getResourceColorApi;

public class ItemDetailsActivity extends AppCompatActivity {

    private final static String ACTION_INTENT   = "action";
    private final static String ACTION_ADD      = "add";
    private final static String ACTION_EDIT     = "edit";
    private final static String EXTRA_ID        = "id";

    private final static int REQUEST_CODE_COLOR = 1;

    @NonNull private ListDatabaseHelper dbHelper;
    @NonNull private Intent intent;
    @NonNull private String action;
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
        intent = getIntent();
        action = intent.getStringExtra(ACTION_INTENT);
        actionBar = getSupportActionBar();
        colorBox = findViewById(R.id.activity_item_details_color);
        name = (EditText) findViewById(R.id.activity_item_details_text_view_name);
        description = (EditText) findViewById(R.id.activity_item_details_text_view_description);
        this.savedInstanceState = savedInstanceState;
    }


    /*
        Intents
     */

    /**
     * @param context context.
     * @return started intent of ItemDetailsActivity, with Add action.
     * See {@link #ACTION_ADD}.
     */
    @NonNull
    public static Intent getAddStartIntent(@NonNull final Context context) {
        Intent intent = new Intent(context, ItemDetailsActivity.class);
        intent.putExtra(ACTION_INTENT, ACTION_ADD);
        return intent;
    }

    /**
     * @param context   context.
     * @param id        id of entry, that link with intent, see: {@link #EXTRA_ID}.
     * @return started intent of ItemDetailsActivity, with Edit action.
     * See {@link #ACTION_EDIT}.
     */
    @NonNull
    public static Intent getEditStartIntent(
            @NonNull final Context context,
            final long id) {

        Intent intent = new Intent(context, ItemDetailsActivity.class);
        intent.putExtra(ACTION_INTENT, ACTION_EDIT);
        intent.putExtra(EXTRA_ID, id);
        return intent;
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
                refreshColorBox(data.getIntExtra(
                        ColorPickerActivity.EXTRA_COLOR,
                        getDefaultColor(ItemDetailsActivity.this)));
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

        // Start choosing color on click.
        colorBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = ColorPickerActivity.getStartIntent(ItemDetailsActivity.this, color);
                startActivityForResult(intent, REQUEST_CODE_COLOR);
            }
        });

        if (actionBar != null) {
            switch (action) {

                // Change action bar title, and set default color.
                case ACTION_ADD:
                    actionBar.setTitle(getResources().getString(R.string.activity_item_details_title_add));
                    if (savedInstanceState != null) {
                        refreshColorBox(savedInstanceState.getInt("color", color));
                    } else {
                        refreshColorBox(getDefaultColor(this));
                    }
                    break;

                // Change action bar title, and fill all fields
                case ACTION_EDIT:
                    actionBar.setTitle(getResources().getString(R.string.activity_item_details_title_edit));
                    if (savedInstanceState != null) {
                        // On restart activity
                        name.setText(savedInstanceState.getString("name", name.getText().toString()));
                        description.setText(savedInstanceState.getString("description", description.getText().toString()));
                        refreshColorBox(savedInstanceState.getInt("color", color));
                        changeableEntry = (ListEntry) savedInstanceState.getSerializable("changeableEntry");

                    } else {
                        // Get entry from database, with given id.
                        long id = intent.getLongExtra(EXTRA_ID, -1);
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

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Refresh color box, with given color.
     * @param color new color.
     */
    private void refreshColorBox(final int color) {
        this.color = color;
        GradientDrawable circleWithBorder;
        circleWithBorder = getGradientDrawableCircleWithBorder(color);
        colorBox.setBackground(circleWithBorder);
    }

    /**
     * @param color color of drawable.
     * @return new gradient drawable, circle, with border.
     */
    @NonNull
    private GradientDrawable getGradientDrawableCircleWithBorder(final int color) {
        GradientDrawable g = new GradientDrawable();
        g.setStroke(
                (int) getResources().getDimension(R.dimen.activity_item_details_ff_color_stroke_width),
                getResourceColorApi(this, android.R.color.primary_text_dark));
        g.setCornerRadius(getResources().getDimension(R.dimen.activity_item_details_ff_color_radius));
        g.setColor(color);
        return g;
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

                switch (action) {

                    // If new, then add to database, and finish activity.
                    // TODO: return result with notify;
                    case ACTION_ADD:
                        ListEntry newEntry = new ListEntry();
                        newEntry.setName(name.getText().toString());
                        newEntry.setDescription(description.getText().toString());
                        newEntry.setColor(color);
                        dbHelper.insertEntry(newEntry);
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
        outState.putString("name", name.getText().toString());
        outState.putString("description", description.getText().toString());
        outState.putInt("color", color);
        outState.putSerializable("changeableEntry", changeableEntry);
        super.onSaveInstanceState(outState);
    }
}
