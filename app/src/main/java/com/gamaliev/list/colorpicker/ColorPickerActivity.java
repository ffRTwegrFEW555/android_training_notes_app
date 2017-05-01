package com.gamaliev.list.colorpicker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.gamaliev.list.R;
import com.gamaliev.list.common.SwitchableHorizontalScrollView;
import com.gamaliev.list.list.ItemDetailsActivity;

import java.util.Arrays;
import java.util.Locale;

import static com.gamaliev.list.common.CommonUtils.getDefaultColor;
import static com.gamaliev.list.common.CommonUtils.setBackgroundColorRectangleAPI;
import static com.gamaliev.list.common.CommonUtils.shiftColor;

/**
 * @author Vadim Gamaliev
 * <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class ColorPickerActivity extends AppCompatActivity {
    /* Logger */
    private static final String TAG = ColorPickerActivity.class.getSimpleName();

    /* Extra */
    public  static final String EXTRA_COLOR                 = "ColorPickerActivity.EXTRA_COLOR";
    private static final String EXTRA_RESULT_COLOR          = "ColorPickerActivity.EXTRA_RESULT_COLOR";
    private static final String EXTRA_HSV_COLOR_OVERRIDDEN  = "ColorPickerActivity.EXTRA_HSV_COLOR_OVERRIDDEN";

    @NonNull private Resources resources;
    @NonNull private SwitchableHorizontalScrollView paletteHsvSv;
    @NonNull private ColorPickerDatabaseHelper dbHelper;
    @NonNull private View resultView;
    @NonNull private View resultParentView;
    @NonNull private PopupWindow editPw;
    @NonNull private int[] hsvColors;
    @NonNull private int[] hsvColorsOverridden;
    private int boxesNumber;
    private int resultColor;
    private float hsvDegree;


    /*
        Init
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_picker);
        init(savedInstanceState);
    }

    private void init(@Nullable final Bundle savedInstanceState) {
        resources   = getResources();
        paletteHsvSv = (SwitchableHorizontalScrollView)
                findViewById(R.id.activity_color_picker_scroll_palette_bar);

        dbHelper    = new ColorPickerDatabaseHelper(this);
        resultView  = findViewById(R.id.activity_color_picker_ff_result_box);
        resultParentView = findViewById(R.id.activity_color_picker_ff_result_outer);
        editPw      = getPopupWindow();
        boxesNumber = resources.getInteger(R.integer.activity_color_picker_palette_boxes_number);
        hsvDegree   = 360f / (boxesNumber * 2);

        // Set result color on start activity. Either by default, or from the Intent.
        if (savedInstanceState == null) {
            Intent intent = getIntent();
            resultColor = intent == null
                    ? getDefaultColor(this)
                    : intent.getIntExtra(EXTRA_COLOR, getDefaultColor(this));

            // Create and fill overridden colors array.
            hsvColorsOverridden = new int[boxesNumber * 2 + 1];
            Arrays.fill(hsvColorsOverridden, -1);

        } else {
            resultColor = savedInstanceState.getInt(EXTRA_RESULT_COLOR);
            hsvColorsOverridden = savedInstanceState.getIntArray(EXTRA_HSV_COLOR_OVERRIDDEN);
        }

        setGradient();
        addColorBoxesAndSetListeners();
        addFavoriteColorBoxesAndSetListeners();
        setResultBoxColor(resultColor);
        setDoneCancelListeners();
        enableEnterSharedTransition();
    }


    /*
        Methods
     */

    /**
     * Set HSV gradient color (0-360) to background of palette bar.
     */
    private void setGradient() {
        final View view = findViewById(R.id.activity_color_picker_ll_palette_bar);

        // Create and fill default colors array.
        hsvColors = new int[boxesNumber * 2 + 1];
        for (int i = 0; i < hsvColors.length; i++) {
            hsvColors[i] = Color.HSVToColor(new float[] {hsvDegree * i, 1f, 1f});
        }

        // Set gradient.
        final GradientDrawable g =
                new GradientDrawable(GradientDrawable.Orientation.TL_BR, hsvColors);
        g.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        view.setBackground(g);
    }

    /**
     * Add color boxes to palette bar and set listeners
     * (see: {@link ColorBoxOnTouchListener})
     */
    private void addColorBoxesAndSetListeners() {
        final ViewGroup paletteBarVg =
                (ViewGroup) findViewById(R.id.activity_color_picker_ll_palette_bar);

        // Params
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                (int) resources.getDimension(R.dimen.activity_color_picker_palette_box_width),
                (int) resources.getDimension(R.dimen.activity_color_picker_palette_box_height));
        final int m = (int) resources.getDimension(R.dimen.activity_color_picker_palette_box_margin);
        params.setMargins(m, m, m, m);

        for (int i = 1; i < hsvColors.length; i += 2) {
            // Create color box with default or overridden color.
            final int color = hsvColorsOverridden[i] != -1 ? hsvColorsOverridden[i] : hsvColors[i];
            final View colorBox = new FrameLayout(this);

            // Set start color and elevation (if API >= 21).
            colorBox.setBackground(new ColorDrawable(color));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                colorBox.setElevation(
                        resources.getDimensionPixelSize(R.dimen.activity_color_picker_palette_box_anim_elevation_off));
            }
            setBackgroundColorRectangleAPI(this, colorBox, color);

            // Set on touch listener.
            // Handles single, double, long clicks; down, up, cancel, move actions.
            colorBox.setOnTouchListener(new ColorBoxOnTouchListener(this, colorBox, i));

            // Add color box to palette bar.
            paletteBarVg.addView(colorBox, params);
        }
    }

    /**
     * Add and fill, from database, color boxes to favorite colors bar and set listeners
     * (see: {@link com.gamaliev.list.colorpicker.FavoriteColorBoxOnTouchListener})
     */
    private void addFavoriteColorBoxesAndSetListeners() {
        final ViewGroup favoriteBarVg =
                (ViewGroup) findViewById(R.id.activity_color_picker_ll_favorite_bar);
        final int boxesNumber = resources.getInteger(R.integer.activity_color_picker_favorite_boxes_number);

        // Params
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                (int) resources.getDimension(R.dimen.activity_color_picker_favorite_box_width),
                (int) resources.getDimension(R.dimen.activity_color_picker_favorite_box_height));
        final int m = (int) resources.getDimension(R.dimen.activity_color_picker_favorite_box_margin);
        params.setMargins(m, m, m, m);

        for (int i = 0; i < boxesNumber; i++) {
            // Create color box with color from database.
            final View button = new Button(this);

            // Set oval shape.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                button.setBackground(resources.getDrawable(R.drawable.btn_oval, null));
            } else {
                button.setBackground(resources.getDrawable(R.drawable.btn_oval));
            }

            // Get and set color.
            button.getBackground()
                    .setColorFilter(dbHelper.getFavoriteColor(i), PorterDuff.Mode.SRC);

            // Set on touch listener.
            // Handles single, long clicks; down, up, cancel actions.
            button.setOnTouchListener(
                    new FavoriteColorBoxOnTouchListener(this, button, dbHelper, i));

            // Add color box to favorite bar.
            favoriteBarVg.addView(button, params);
        }
    }

    /**
     * Set color to result box.
     * @param color Color to set.
     */
    void setResultBoxColor(final int color) {
        // Change color of result-box, and result-box-parent,
        // with shift color animation.
        shiftColor(resultView, resultColor, color,
                -1,
                resources.getInteger(R.integer.activity_color_picker_result_box_animation_change_color_duration));
        shiftColor(resultParentView, resultColor, color,
                resources.getInteger(R.integer.activity_color_picker_result_box_outer_alpha_percent) / 100.0f,
                resources.getInteger(R.integer.activity_color_picker_result_box_animation_change_color_duration));

        // Update value.
        resultColor = color;

        // Get rgb string.
        final int r = color >> 16 & 0xFF;
        final int g = color >> 8  & 0xFF;
        final int b = color       & 0xFF;
        final String rgb = r + ", " + g + ", " + b;

        // Get hsv string.
        final float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        final String hsvString = String.format(Locale.ENGLISH,
                "%d, %d%%, %d%%",
                (int) (hsv[0]),
                (int) (hsv[1] * 100),
                (int) (hsv[2] * 100));

        // Set contrast text of result box.
        final TextView tv = (TextView) findViewById(R.id.activity_color_picker_text_result_box);
        tv.setText(rgb + "\n" + hsvString);
        tv.setTextColor(Color.rgb(
                255 - Color.red(color),
                255 - Color.green(color),
                255 - Color.blue(color)));
    }

    /**
     * Setting listeners on 'Done' and 'Cancel' buttons.
     */
    private void setDoneCancelListeners() {
        // Done button.
        findViewById(R.id.activity_color_picker_ic_done)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Return RESULT_OK and selected color.
                        setResult(RESULT_OK, ItemDetailsActivity.getResultColorIntent(resultColor));
                        finish();
                    }
                });

        // Cancel button.
        findViewById(R.id.activity_color_picker_ic_cancel)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Finish the activity.
                        finish();
                    }
                });
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
            getWindow().setSharedElementReturnTransition(null);
            findViewById(android.R.id.content).invalidate();
        }
    }

    /**
     * Set color to palette box and popupWindow, when "edit mode" is turn on.
     * @param newColor  Color to set.
     * @param view      View, whose "edit mode" is turn on, that changes color.
     * @param index     Color index.
     */
    void setColorOnMove(
            @NonNull final View view,
            final int newColor,
            final int index) {

        int color;

        // If color is not specified, then get color from hsv-colors-arrays.
        // If override-color is not specified, then get default color.
        if (newColor == -1) {
            color = hsvColorsOverridden[index] != -1 ? hsvColorsOverridden[index] : hsvColors[index];
        } else {
            color = newColor;
        }

        // Set color to edited view.
        setBackgroundColorRectangleAPI(this, view, color);

        // Set color to popupWindow.
        editPw.getContentView().setBackgroundColor(color);

        // Update overridden array.
        hsvColorsOverridden[index] = color;
    }


    /*
        PopupWindow
     */

    /**
     * @return a new {@link PopupWindow} object, with specific size and elevation.
     * See integer-resources, section "ColorPicker PopupWindow".
     */
    @NonNull
    private PopupWindow getPopupWindow() {
        final FrameLayout fl = new FrameLayout(this);
        final PopupWindow popupWindow = new PopupWindow(
                fl,
                (int) resources.getDimension(R.dimen.activity_color_picker_popupwindow_width),
                (int) resources.getDimension(R.dimen.activity_color_picker_popupwindow_height),
                true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.setElevation(resources.getDimension(R.dimen.activity_color_picker_popupwindow_elevation));
        }
        return popupWindow;
    }

    /**
     * Shows the PopupWindow with animation.<br>
     * See also: {@link #getPopupWindow()}
     */
    void showPopupWindow() {
        editPw.setAnimationStyle(R.style.ColorPickerPopupWindowAnimation);
        editPw.showAtLocation(findViewById(android.R.id.content),
                resources.getInteger(R.integer.activity_color_picker_popupwindow_gravity),
                (int) resources.getDimension(R.dimen.activity_color_picker_popupwindow_offset_x),
                (int) resources.getDimension(R.dimen.activity_color_picker_popupwindow_offset_y));
    }


    /*
       On Save/Restore Instance State
     */

    /**
     * @param outState Save result color, and overridden colors array.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(EXTRA_RESULT_COLOR, resultColor);
        outState.putIntArray(EXTRA_HSV_COLOR_OVERRIDDEN, hsvColorsOverridden);
        super.onSaveInstanceState(outState);
    }


    /*
        On Pause/Resume
     */

    /**
     * Open a new database helper.<br>
     * See also: {@link com.gamaliev.list.colorpicker.ColorPickerDatabaseHelper}
     */
    @Override
    protected void onResume() {
        if (dbHelper == null) {
            dbHelper = new ColorPickerDatabaseHelper(this);
        }
        super.onResume();
    }

    /**
     * Close database helper.<br>
     * See also: {@link com.gamaliev.list.colorpicker.ColorPickerDatabaseHelper}
     */
    @Override
    protected void onPause() {
        try {
            dbHelper.close();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        super.onPause();
    }


    /*
        Getters
     */

    int getResultColor() {
        return resultColor;
    }

    @NonNull
    SwitchableHorizontalScrollView getPaletteHsvSv() {
        return paletteHsvSv;
    }

    @NonNull
    PopupWindow getEditPw() {
        return editPw;
    }

    @NonNull
    int[] getHsvColors() {
        return hsvColors;
    }

    @NonNull
    int[] getHsvColorsOverridden() {
        return hsvColorsOverridden;
    }

    float getHsvDegree() {
        return hsvDegree;
    }


    /*
        Intents
     */

    /**
     * Start intent for result for editing color.
     * @param context       Context.
     * @param color         Color for editing.
     * @param requestCode   This code will be returned in onActivityResult() when the activity exits.
     * @param bundle        Additional options for how the Activity should be started.
     *                      If null, then start {@link android.app.Activity#startActivityForResult(Intent, int)},
     *                      otherwise start {@link android.app.Activity#startActivityForResult(Intent, int, Bundle)},
     */
    public static void startIntent(
            @NonNull final Context context,
            final int color,
            final int requestCode,
            @Nullable final Bundle bundle) {

        Intent starter = new Intent(context, ColorPickerActivity.class);
        starter.putExtra(EXTRA_COLOR, color);
        if (bundle == null) {
            ((Activity) context).startActivityForResult(starter, requestCode);
        } else {
            ((Activity) context).startActivityForResult(starter, requestCode, bundle);
        }
    }
}
