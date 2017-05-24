package com.gamaliev.notes.colorpicker;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.gamaliev.notes.R;
import com.gamaliev.notes.colorpicker.db.ColorPickerDbHelper;
import com.gamaliev.notes.common.SwitchableHorizontalScrollView;

import java.util.Arrays;
import java.util.Locale;

import static com.gamaliev.notes.common.CommonUtils.getDefaultColor;
import static com.gamaliev.notes.common.CommonUtils.setBackgroundColorRectangleAPI;
import static com.gamaliev.notes.common.CommonUtils.shiftColor;

/**
 * @author Vadim Gamaliev
 * <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class ColorPickerFragment extends Fragment {

    /* Logger */
    private static final String TAG = ColorPickerFragment.class.getSimpleName();

    /* Extra */
    public  static final String EXTRA_COLOR                 = "ColorPickerFragment.EXTRA_COLOR";
    private static final String EXTRA_RESULT_COLOR          = "ColorPickerFragment.EXTRA_RESULT_COLOR";
    private static final String EXTRA_HSV_COLOR_OVERRIDDEN  = "ColorPickerFragment.EXTRA_HSV_COLOR_OVERRIDDEN";

    @NonNull private View mParentView;
    @NonNull private Resources mRes;
    @NonNull private SwitchableHorizontalScrollView mPaletteHsvSv;
    @NonNull private View mResultView;
    @NonNull private View mResultParentView;
    @NonNull private PopupWindow mEditPw;
    @NonNull private int[] mHsvColors;
    @NonNull private int[] mHsvColorsOverridden;
    private int mBoxesNumber;
    private int mResultColor;
    private float mHsvDegree;


    /*
        Init
     */

    public static ColorPickerFragment newInstance(final int color) {

        final Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_COLOR, color);

        final ColorPickerFragment fragment = new ColorPickerFragment();
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        mParentView = inflater.inflate(
                R.layout.fragment_color_picker_default,
                container,
                false);

        return mParentView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        init(savedInstanceState);
    }

    private void init(@Nullable final Bundle savedInstanceState) {
        mRes            = getResources();
        mPaletteHsvSv   = (SwitchableHorizontalScrollView)
                mParentView.findViewById(R.id.fragment_color_picker_scroll_palette_bar);

        mResultView     = mParentView.findViewById(R.id.fragment_color_picker_ff_result_box);
        mResultParentView = mParentView.findViewById(R.id.fragment_color_picker_ff_result_outer);
        mEditPw         = getPopupWindow();
        mBoxesNumber    = mRes.getInteger(R.integer.fragment_color_picker_palette_boxes_number);
        mHsvDegree      = 360f / (mBoxesNumber * 2);

        // Set result color on start.
        if (savedInstanceState == null) {
            final int color = getArguments().getInt(EXTRA_COLOR);
            mResultColor = color == -1
                    ? getDefaultColor(getContext())
                    : color;

            // Create and fill overridden colors array.
            mHsvColorsOverridden = new int[mBoxesNumber * 2 + 1];
            Arrays.fill(mHsvColorsOverridden, -1);

        } else {
            mResultColor = savedInstanceState.getInt(EXTRA_RESULT_COLOR);
            //noinspection ConstantConditions
            mHsvColorsOverridden = savedInstanceState.getIntArray(EXTRA_HSV_COLOR_OVERRIDDEN);
        }

        setGradient();
        addColorBoxesAndSetListeners();
        addFavoriteColorBoxesAndSetListeners();
        setResultBoxColor(mResultColor);
        setDoneCancelListeners();
        initTransition();
        initFullScreen();
    }


    /*
        Methods
     */

    /**
     * Set HSV gradient color (0-360) to background of palette bar.
     */
    private void setGradient() {
        final View view = mParentView.findViewById(R.id.fragment_color_picker_ll_palette_bar);

        // Create and fill default colors array.
        mHsvColors = new int[mBoxesNumber * 2 + 1];
        for (int i = 0; i < mHsvColors.length; i++) {
            mHsvColors[i] = Color.HSVToColor(new float[] {mHsvDegree * i, 1f, 1f});
        }

        // Set gradient.
        final GradientDrawable g =
                new GradientDrawable(GradientDrawable.Orientation.TL_BR, mHsvColors);
        g.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        view.setBackground(g);
    }

    /**
     * Add color boxes to palette bar and set listeners
     * (see: {@link ColorBoxOnTouchListener})
     */
    private void addColorBoxesAndSetListeners() {
        final ViewGroup paletteBarVg =
                (ViewGroup) mParentView.findViewById(R.id.fragment_color_picker_ll_palette_bar);

        // Params
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                (int) mRes.getDimension(R.dimen.fragment_color_picker_palette_box_width),
                (int) mRes.getDimension(R.dimen.fragment_color_picker_palette_box_height));
        final int m = (int) mRes.getDimension(R.dimen.fragment_color_picker_palette_box_margin);
        params.setMargins(m, m, m, m);

        for (int i = 1; i < mHsvColors.length; i += 2) {
            // Create color box with default or overridden color.
            final int color = mHsvColorsOverridden[i] != -1 ? mHsvColorsOverridden[i] : mHsvColors[i];
            final View colorBox = new FrameLayout(getContext());

            // Set start color and elevation (if API >= 21).
            colorBox.setBackground(new ColorDrawable(color));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                colorBox.setElevation(
                        mRes.getDimensionPixelSize(R.dimen.fragment_color_picker_palette_box_anim_elevation_off));
            }
            setBackgroundColorRectangleAPI(getContext(), colorBox, color);

            // Set on touch listener.
            // Handles single, double, long clicks; down, up, cancel, move actions.
            colorBox.setOnTouchListener(
                    new ColorBoxOnTouchListener(getContext(), this, colorBox, i));

            // Add color box to palette bar.
            paletteBarVg.addView(colorBox, params);
        }
    }

    /**
     * Add and fill, from database, color boxes to favorite colors bar and set listeners
     * (see: {@link com.gamaliev.notes.colorpicker.FavoriteColorBoxOnTouchListener})
     */
    private void addFavoriteColorBoxesAndSetListeners() {
        final ViewGroup favoriteBarVg =
                (ViewGroup) mParentView.findViewById(R.id.fragment_color_picker_ll_favorite_bar);
        final int boxesNumber = mRes.getInteger(R.integer.fragment_color_picker_favorite_boxes_number);

        // Params
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                (int) mRes.getDimension(R.dimen.fragment_color_picker_favorite_box_width),
                (int) mRes.getDimension(R.dimen.fragment_color_picker_favorite_box_height));
        final int m = (int) mRes.getDimension(R.dimen.fragment_color_picker_favorite_box_margin);
        params.setMargins(m, m, m, m);

        for (int i = 0; i < boxesNumber; i++) {
            // Create color box with color from database.
            final View button = new Button(getContext());

            // Set oval shape.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                button.setBackground(mRes.getDrawable(R.drawable.btn_oval, null));
            } else {
                //noinspection deprecation
                button.setBackground(mRes.getDrawable(R.drawable.btn_oval));
            }

            // Get and set color.
            button.getBackground()
                    .setColorFilter(ColorPickerDbHelper
                            .getFavoriteColor(getContext(), i), PorterDuff.Mode.SRC);

            // Set on touch listener.
            // Handles single, long clicks; down, up, cancel actions.
            button.setOnTouchListener(
                    new FavoriteColorBoxOnTouchListener(getContext(), this, button, i));

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
        shiftColor(mResultView, mResultColor, color,
                -1,
                mRes.getInteger(R.integer.fragment_color_picker_result_box_animation_change_color_duration));
        shiftColor(mResultParentView, mResultColor, color,
                mRes.getInteger(R.integer.fragment_color_picker_result_box_outer_alpha_percent) / 100.0f,
                mRes.getInteger(R.integer.fragment_color_picker_result_box_animation_change_color_duration));

        // Update value.
        mResultColor = color;

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
        final TextView tv = (TextView) mParentView
                .findViewById(R.id.fragment_color_picker_text_result_box);
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
        mParentView.findViewById(R.id.fragment_color_picker_ic_done)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Return RESULT_OK and selected color.
                        // setResult(RESULT_OK, ItemDetailsActivity.getResultColorIntent(mResultColor));
                        finish();
                    }
                });

        // Cancel button.
        mParentView.findViewById(R.id.fragment_color_picker_ic_cancel)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Finish the activity.
                        finish();
                    }
                });
    }

    private void initTransition() {
        ViewCompat.setTransitionName(
                mResultView,
                getString(R.string.shared_transition_name_color_box));
        ViewCompat.setTransitionName(
                mResultParentView,
                getString(R.string.shared_transition_name_layout));
    }

    private void initFullScreen() {
        final ActionBar actionBar =
                ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
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
            color = mHsvColorsOverridden[index] != -1 ? mHsvColorsOverridden[index] : mHsvColors[index];
        } else {
            color = newColor;
        }

        // Set color to edited view.
        setBackgroundColorRectangleAPI(getContext(), view, color);

        // Set color to popupWindow.
        mEditPw.getContentView().setBackgroundColor(color);

        // Update overridden array.
        mHsvColorsOverridden[index] = color;
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
        final FrameLayout fl = new FrameLayout(getContext());
        final PopupWindow popupWindow = new PopupWindow(
                fl,
                (int) mRes.getDimension(R.dimen.fragment_color_picker_popupwindow_width),
                (int) mRes.getDimension(R.dimen.fragment_color_picker_popupwindow_height),
                true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.setElevation(mRes.getDimension(R.dimen.fragment_color_picker_popupwindow_elevation));
        }
        return popupWindow;
    }

    /**
     * Shows the PopupWindow with animation.<br>
     * See also: {@link #getPopupWindow()}
     */
    void showPopupWindow() {
        mEditPw.setAnimationStyle(R.style.ColorPickerPopupWindowAnimation);
        mEditPw.showAtLocation(mParentView,
                mRes.getInteger(R.integer.fragment_color_picker_popupwindow_gravity),
                (int) mRes.getDimension(R.dimen.fragment_color_picker_popupwindow_offset_x),
                (int) mRes.getDimension(R.dimen.fragment_color_picker_popupwindow_offset_y));
    }


    /*
       On Save/Restore Instance State
     */

    /**
     * @param outState Save result color, and overridden colors array.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(EXTRA_RESULT_COLOR, mResultColor);
        outState.putIntArray(EXTRA_HSV_COLOR_OVERRIDDEN, mHsvColorsOverridden);
        super.onSaveInstanceState(outState);
    }


    /*
        Getters
     */

    int getResultColor() {
        return mResultColor;
    }

    @NonNull
    SwitchableHorizontalScrollView getPaletteHsvSv() {
        return mPaletteHsvSv;
    }

    @NonNull
    PopupWindow getEditPw() {
        return mEditPw;
    }

    @NonNull
    int[] getHsvColors() {
        return mHsvColors;
    }

    @NonNull
    int[] getHsvColorsOverridden() {
        return mHsvColorsOverridden;
    }

    float getHsvDegree() {
        return mHsvDegree;
    }


    /*
        ...
     */

    private void finish() {
        getActivity().onBackPressed();
    }
}
