package com.gamaliev.list.list;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.gamaliev.list.R;
import com.gamaliev.list.colorpicker.ColorPickerActivity;
import com.gamaliev.list.common.DatabaseHelper;

import static com.gamaliev.list.common.CommonUtils.getResourcesColorAPI;

public class ItemDetailsActivity extends AppCompatActivity {

    public final static String ACTION_INTENT    = "action";
    public final static String ACTION_ADD       = "add";
    public final static String ACTION_EDIT      = "edit";
    public final static String EXTRA_ID         = "id";

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

    @NonNull
    public static Intent getAddStartIntent(@NonNull final Context context) {
        Intent intent = new Intent(context, ItemDetailsActivity.class);
        intent.putExtra(ACTION_INTENT, ACTION_ADD);
        return intent;
    }

    @NonNull
    public static Intent getEditStartIntent(
            @NonNull final Context context,
            final long id) {

        Intent intent = new Intent(context, ItemDetailsActivity.class);
        intent.putExtra(ACTION_INTENT, ACTION_EDIT);
        intent.putExtra(EXTRA_ID, id);
        return intent;
    }

    @NonNull
    public static Intent getResultColorIntent(final int color) {
        Intent intent = new Intent();
        intent.putExtra(ColorPickerActivity.EXTRA_COLOR, color);
        return intent;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_COLOR && resultCode == RESULT_OK) {
            if (data != null) {
                refreshColorBox(data.getIntExtra(
                        ColorPickerActivity.EXTRA_COLOR,
                        DatabaseHelper.getDefaultColor(ItemDetailsActivity.this)));
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
        colorBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = ColorPickerActivity.getStartIntent(ItemDetailsActivity.this, color);
                startActivityForResult(intent, REQUEST_CODE_COLOR);
            }
        });

        if (actionBar != null) {
            switch (action) {

                case ACTION_ADD:
                    actionBar.setTitle(getResources().getString(R.string.activity_item_details_title_add));
                    if (savedInstanceState != null) {
                        refreshColorBox(savedInstanceState.getInt("color", color));
                    } else {
                        refreshColorBox(DatabaseHelper.getDefaultColor(this));
                    }
                    break;

                case ACTION_EDIT:
                    actionBar.setTitle(getResources().getString(R.string.activity_item_details_title_edit));
                    if (savedInstanceState != null) {
                        name.setText(savedInstanceState.getString("name", name.getText().toString()));
                        description.setText(savedInstanceState.getString("description", description.getText().toString()));
                        refreshColorBox(savedInstanceState.getInt("color", color));
                        changeableEntry = (ListEntry) savedInstanceState.getSerializable("changeableEntry");

                    } else {
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

    // TODO: handle
    private void refreshColorBox(final int color) {
        this.color = color;
        GradientDrawable circleWithBorder;
        circleWithBorder = getGradientDrawableCircleWithBorder(color);
        colorBox.setBackground(circleWithBorder);
    }

    // TODO: handle
    @NonNull
    private GradientDrawable getGradientDrawableCircleWithBorder(final int color) {
        GradientDrawable g = new GradientDrawable();
        g.setStroke(
                (int) getResources().getDimension(R.dimen.activity_item_details_ff_color_stroke_width),
                getResourcesColorAPI(this, android.R.color.primary_text_dark));
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

            // Save
            case R.id.menu_list_item_details_done:
                dbHelper = new ListDatabaseHelper(this);

                switch (action) {

                    case ACTION_ADD:
                        ListEntry newEntry = new ListEntry();
                        newEntry.setName(name.getText().toString());
                        newEntry.setDescription(description.getText().toString());
                        newEntry.setColor(color);
                        dbHelper.insertEntry(newEntry);
                        finish();
                        // TODO: return result with notify;
                        break;

                    case ACTION_EDIT:
                        if (changeableEntry != null) {
                            changeableEntry.setName(name.getText().toString());
                            changeableEntry.setDescription(description.getText().toString());
                            changeableEntry.setColor(color);
                            dbHelper.updateEntry(changeableEntry);
                        }
                        finish();
                        // TODO: return result with notify;
                        break;

                    default:
                        break;
                }

                dbHelper.close();
                break;

            // Cancel
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

    // TODO: handle
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("name", name.getText().toString());
        outState.putString("description", description.getText().toString());
        outState.putInt("color", color);
        outState.putSerializable("changeableEntry", changeableEntry);
        super.onSaveInstanceState(outState);
    }
}
