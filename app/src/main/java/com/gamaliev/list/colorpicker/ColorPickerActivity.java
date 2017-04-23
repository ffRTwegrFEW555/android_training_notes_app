package com.gamaliev.list.colorpicker;

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

    private static final String TAG = ColorPickerActivity.class.getSimpleName();
    public static final String EXTRA_COLOR = "color";

    @NonNull private Resources resources;
    @NonNull private SwitchableHorizontalScrollView paletteHsv;
    @NonNull private ColorPickerDatabaseHelper dbHelper;
    @NonNull private View resultView;
    @NonNull private View resultParentView;
    @NonNull private PopupWindow editPw;
    @NonNull private int[] hsvColors;
    @NonNull private int[] hsvColorsOverride;
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
        paletteHsv  = (SwitchableHorizontalScrollView)
                findViewById(R.id.activity_color_picker_scroll_palette_bar);

        dbHelper    = new ColorPickerDatabaseHelper(this);
        resultView  = findViewById(R.id.activity_color_picker_ff_result_box);
        resultParentView = findViewById(R.id.activity_color_picker_ff_result_outer);
        editPw      = getPopupWindow();
        boxesNumber = resources.getInteger(R.integer.activity_color_picker_palette_boxes_number);
        hsvDegree   = 360f / (boxesNumber * 2);

        if (savedInstanceState == null) {
            // Set result color on start activity. Either by default, or from the Intent.
            Intent intent = getIntent();
            resultColor = intent == null ?
                            getDefaultColor(this) :
                            intent.getIntExtra(EXTRA_COLOR, getDefaultColor(this));
            hsvColorsOverride = new int[boxesNumber * 2 + 1];
            Arrays.fill(hsvColorsOverride, -1);

        } else {
            resultColor = savedInstanceState.getInt("resultColor");
            hsvColorsOverride = savedInstanceState.getIntArray("hsvColorsOverride");
        }

        setGradient();
        addColorBoxesAndSetListeners();
        addFavoriteColorBoxesAndSetListeners();
        setResultBoxColor(resultColor);
        setDoneCancelListeners();
    }


    /*
        Methods
     */

    /**
     * Set HSV gradient color (0-360) to background of palette bar.
     */
    private void setGradient() {
        final View view = findViewById(R.id.activity_color_picker_ll_palette_bar);

        hsvColors = new int[boxesNumber * 2 + 1];
        for (int i = 0; i < hsvColors.length; i++) {
            hsvColors[i] = Color.HSVToColor(new float[] {hsvDegree * i, 1f, 1f});
        }

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
        final ViewGroup viewGroup =
                (ViewGroup) findViewById(R.id.activity_color_picker_ll_palette_bar);

        for (int i = 1; i < hsvColors.length; i += 2) {
            // Params
            final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) resources.getDimension(R.dimen.activity_color_picker_palette_box_width),
                    (int) resources.getDimension(R.dimen.activity_color_picker_palette_box_height));
            final int m = (int) resources.getDimension(R.dimen.activity_color_picker_palette_box_margin);
            params.setMargins(m, m, m, m);

            // Create view
            final int color = hsvColorsOverride[i] != -1 ? hsvColorsOverride[i] : hsvColors[i];
            final View box = new FrameLayout(this);
            box.setBackground(new ColorDrawable(color));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                box.setElevation(
                        resources.getDimensionPixelSize(R.dimen.activity_color_picker_palette_box_anim_elevation_off));
            }
            setBackgroundColorRectangleAPI(this, box, color);
            box.setOnTouchListener(new ColorBoxOnTouchListener(this, box, i));

            // Add
            viewGroup.addView(box, params);
        }
    }

    /**
     * Add and fill, from database, color boxes to favorite colors bar and set listeners
     * (see: {@link com.gamaliev.list.colorpicker.FavoriteColorBoxOnTouchListener})
     */
    private void addFavoriteColorBoxesAndSetListeners() {
        final ViewGroup viewGroup =
                (ViewGroup) findViewById(R.id.activity_color_picker_ll_favorite_bar);
        final int boxesNumber = resources.getInteger(R.integer.activity_color_picker_favorite_boxes_number);

        for (int i = 0; i < boxesNumber; i++) {
            // Params
            final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) resources.getDimension(R.dimen.activity_color_picker_favorite_box_width),
                    (int) resources.getDimension(R.dimen.activity_color_picker_favorite_box_height));
            final int m = (int) resources.getDimension(R.dimen.activity_color_picker_favorite_box_margin);
            params.setMargins(m, m, m, m);

            // Create view
            final View button = new Button(this);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                button.setBackground(resources.getDrawable(R.drawable.btn_oval, null));
            } else {
                button.setBackground(resources.getDrawable(R.drawable.btn_oval));
            }
            button.getBackground()
                    .setColorFilter(dbHelper.getFavoriteColor(i), PorterDuff.Mode.SRC);
            button.setOnTouchListener(
                    new FavoriteColorBoxOnTouchListener(this, button, dbHelper, i));

            // Add
            viewGroup.addView(button, params);
        }
    }

    /**
     * Set color to result box.
     * @param color color to set.
     */
    void setResultBoxColor(final int color) {
        // Color change animation
        shiftColor(resultView, resultColor, color,
                -1,
                resources.getInteger(R.integer.activity_color_picker_result_box_animation_change_color_duration));
        shiftColor(resultParentView, resultColor, color,
                resources.getInteger(R.integer.activity_color_picker_result_box_outer_alpha_percent) / 100.0f,
                resources.getInteger(R.integer.activity_color_picker_result_box_animation_change_color_duration));

        resultColor = color;

        // Get rgb string
        final int r = color >> 16 & 0xFF;
        final int g = color >> 8  & 0xFF;
        final int b = color       & 0xFF;
        final String rgb = r + ", " + g + ", " + b;

        // Get hsv string
        final float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        final String hsvString = String.format(Locale.ENGLISH,
                "%d, %d%%, %d%%",
                (int) (hsv[0]),
                (int) (hsv[1] * 100),
                (int) (hsv[2] * 100));

        // Set contrast text
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
        // Return RESULT_OK and selected color.
        findViewById(R.id.activity_color_picker_ic_done)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setResult(RESULT_OK, ItemDetailsActivity.getResultColorIntent(resultColor));
                        finish();
                    }
                });

        // Finish the activity
        findViewById(R.id.activity_color_picker_ic_cancel)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });
    }

    /**
     * Set color to palette box and popupWindow, when "edit mode" is turn on.
     * @param newColor color to set.
     * @param view view, whose "edit mode" is turn on, that changes color.
     * @param index color index.
     */
    void setColorOnMove(final int newColor, @NonNull final View view, final int index) {
        int color;
        if (newColor == -1) {
            color = hsvColorsOverride[index] != -1 ? hsvColorsOverride[index] : hsvColors[index];
        } else {
            color = newColor;
        }
        setBackgroundColorRectangleAPI(this, view, color);
        editPw.getContentView().setBackgroundColor(color);
        hsvColorsOverride[index] = color;
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
     * Shows the PopupWindow<br>
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt ("resultColor", resultColor);
        outState.putIntArray ("hsvColorsOverride", hsvColorsOverride);
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
            Log.e(TAG, e.getMessage());
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
    SwitchableHorizontalScrollView getPaletteHsv() {
        return paletteHsv;
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
    int[] getHsvColorsOverride() {
        return hsvColorsOverride;
    }

    float getHsvDegree() {
        return hsvDegree;
    }


    /*
        Intents
     */

    /**
     * @param context   context.
     * @param color     color, that link with intent, see: {@link #EXTRA_COLOR}.
     * @return started intent of ColorPickerActivity, with given color.
     */
    @NonNull
    public static Intent getStartIntent(
            @NonNull final Context context,
            final int color) {
        Intent intent = new Intent(context, ColorPickerActivity.class);
        intent.putExtra(EXTRA_COLOR, color);
        return intent;
    }
}
