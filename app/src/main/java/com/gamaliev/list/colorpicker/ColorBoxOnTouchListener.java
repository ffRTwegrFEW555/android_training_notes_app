package com.gamaliev.list.colorpicker;

import android.content.res.Resources;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.gamaliev.list.R;
import com.gamaliev.list.common.SwitchableHorizontalScrollView;

import static com.gamaliev.list.common.CommonUtils.animateElevation;
import static com.gamaliev.list.common.CommonUtils.makeVibrate;
import static com.gamaliev.list.common.CommonUtils.playSoundAndShowToast;
import static com.gamaliev.list.common.CommonUtils.setBackgroundColorAPI;
import static com.gamaliev.list.common.CommonUtils.showToast;

/**
 * @author Vadim Gamaliev
 * <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

final class ColorBoxOnTouchListener implements View.OnTouchListener {

    @NonNull private final ColorPickerActivity context;
    @NonNull private final Resources resources;
    @NonNull private final SwitchableHorizontalScrollView paletteHsv;
    @NonNull private final PopupWindow editPw;
    @NonNull private final GestureDetector gestureDetector;
    @NonNull private final View view;
    @NonNull private final int[] hsvColors;
    @NonNull private final int[] hsvColorsOverride;
    private final int index;
    private final float hsvDegree;
    private boolean editable;
    private boolean longPressed;
    private boolean pauseNotify;
    private float x1;
    private float y1;

    ColorBoxOnTouchListener(
            @NonNull final ColorPickerActivity context,
            @NonNull final View view,
            final int index) {

        this.context = context;
        this.view   = view;
        this.index  = index;

        resources   = context.getResources();
        gestureDetector = new GestureDetector(context, getSimpleOnGestureListener());
        paletteHsv  = context.getPaletteHsv();
        editPw      = context.getEditPw();
        hsvColors   = context.getHsvColors();
        hsvColorsOverride = context.getHsvColorsOverride();
        hsvDegree   = context.getHsvDegree();
    }

    /**
     * Handler of long press, double tap, single tap, move action<br>
     * Animates views.<br>
     * <br>
     * Long press, double tap, single tap: see {@link #getSimpleOnGestureListener()}<br>
     * Move action: change palette box color, when "edit mode" is turn on.
     */
    @Override
    public boolean onTouch(View v, MotionEvent e) {
        gestureDetector.onTouchEvent(e);

        switch (e.getAction()) {

            // Animation
            case MotionEvent.ACTION_DOWN:
                animateElevation(view,
                        resources.getInteger(R.integer.activity_color_picker_palette_box_anim_elevation_duration),
                        resources.getDimensionPixelSize(R.dimen.activity_color_picker_palette_box_anim_elevation_on));
                return true;

            // Enable scrolling and turn off "Edit mode", and animation
            case MotionEvent.ACTION_UP:
                if (!paletteHsv.isEnableScrolling()) {
                    editable = false;
                    paletteHsv.setEnableScrolling(true);
                    editPw.dismiss();
                    showToast(context,
                            resources.getString(R.string.activity_color_picker_toast_edit_mode_off),
                            Toast.LENGTH_SHORT);
                }

                animateElevation(view,
                        resources.getInteger(R.integer.activity_color_picker_palette_box_anim_elevation_duration),
                        resources.getDimensionPixelSize(R.dimen.activity_color_picker_palette_box_anim_elevation_off));
                return true;

            // Animation
            case MotionEvent.ACTION_CANCEL:
                editable = false;
                animateElevation(view,
                        resources.getInteger(R.integer.activity_color_picker_palette_box_anim_elevation_duration),
                        resources.getDimensionPixelSize(R.dimen.activity_color_picker_palette_box_anim_elevation_off));
                return true;

            // Edit mode: Change color
            case MotionEvent.ACTION_MOVE:
                if (editable) {
                    if (paletteHsv.isEnableScrolling()) {
                        editable = false;
                        break;
                    }

                    final float size  = v.getWidth();
                    final float start = (index - 1) * hsvDegree;
                    final float end   = (index + 1) * hsvDegree;
                    final float range = end - start;
                    final float x0    = e.getX();
                    final float y0    = e.getY();
                    final float x     = x0 > size ? size : x0 < 0 ? 0 : x0;
                    final float y     = y0 > size ? size : y0 < 0 ? 0 : y0;
                    final float hue   = ((x / size) * range) + start;
                    final float value = y / size;
                    final int newColor = Color.HSVToColor(new float[] {hue, 1f, value});
                    context.setColorOnMove(newColor, view, index);

                    // Notify when border reaches
                    if (!pauseNotify && (x0 > size || x0 < 0 || y0 > size || y0 < 0)) {
                        makeVibrate(
                                context,
                                resources.getInteger(R.integer.activity_color_picker_vibration_duration));
                        pauseNotify = true;
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                pauseNotify = false;
                            }
                        }, resources.getInteger(R.integer.activity_color_picker_notification_interval_border_reaches));
                    }

                } else {
                    // Detector, when change HSV color turn on
                    if (longPressed) {
                        int delta = (int) resources.getDimension(R.dimen.activity_color_picker_delta_to_enable_change_color);
                        if (Math.abs(e.getX() - x1) > delta
                                || Math.abs(e.getY() - y1) > delta) {
                            editable = true;
                            longPressed = false;
                        }
                    }
                }

            default:
                break;
        }

        return false;
    }


    /**
     * Handler of long press, double tap, single tap.<br>
     * <br>
     * Long press:  disable scrolling and turn on "Edit mode".<br>
     * Double tap:  set default color back to palette box.<br>
     * Single tap:  set palette color to result box.
     */
    @NonNull
    private GestureDetector.SimpleOnGestureListener getSimpleOnGestureListener() {
        return new GestureDetector.SimpleOnGestureListener() {

            // Disable scrolling and turn on "Edit mode"
            @Override
            public void onLongPress(MotionEvent e) {
                playSoundAndShowToast(
                        context,
                        RingtoneManager.TYPE_NOTIFICATION,
                        resources.getString(R.string.activity_color_picker_toast_edit_mode_on),
                        Toast.LENGTH_SHORT);

                paletteHsv.setEnableScrolling(false);
                longPressed = true;
                x1 = e.getX();
                y1 = e.getY();
                context.showPopupWindow();
                context.setColorOnMove(-1, view, index);
            }

            // Set default color back to palette box
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                setBackgroundColorAPI(context, view, hsvColors[index]);
                hsvColorsOverride[index] = -1;
                return true;
            }

            // Set palette color to result box
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                int result = hsvColorsOverride[index] != -1
                        ? hsvColorsOverride[index] : hsvColors[index];
                context.setResultBoxColor(result);
                return true;
            }
        };
    }
}
