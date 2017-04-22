package com.gamaliev.list.colorpicker;

import android.content.res.Resources;
import android.media.RingtoneManager;
import android.support.annotation.NonNull;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.gamaliev.list.R;

import static com.gamaliev.list.common.CommonUtils.animateElevation;
import static com.gamaliev.list.common.CommonUtils.animateScaleXY;
import static com.gamaliev.list.common.CommonUtils.playSoundAndShowToast;
import static com.gamaliev.list.common.CommonUtils.setBackgroundColor;

/**
 * @author Vadim Gamaliev
 * <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

final class FavoriteColorBoxOnTouchListener implements View.OnTouchListener {

    @NonNull private final ColorPickerActivity context;
    @NonNull private final Resources resources;
    @NonNull private final GestureDetector gestureDetector;
    @NonNull private final View view;
    @NonNull private final ColorPickerDatabaseHelper dbHelper;
    private final int index;

    FavoriteColorBoxOnTouchListener(
            @NonNull final ColorPickerActivity context,
            @NonNull final View view,
            @NonNull final ColorPickerDatabaseHelper dbHelper,
            final int index) {

        this.index      = index;
        this.context    = context;
        this.view       = view;
        this.dbHelper   = dbHelper;

        resources       = context.getResources();
        gestureDetector = new GestureDetector(context, getSimpleOnGestureListener());
    }

    /**
     * Handler of long press, single tap.<br>
     * Animates views.<br>
     * <br>
     * Long press, single tap: see {@link #getSimpleOnGestureListener()}
     */
    @Override
    public boolean onTouch(View v, MotionEvent e) {
        gestureDetector.onTouchEvent(e);

        // Animation
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                animateElevation(v,
                        resources.getInteger(R.integer.activity_color_picker_favorite_box_anim_elevation_duration),
                        resources.getDimensionPixelOffset(R.dimen.activity_color_picker_favorite_box_anim_elevation_on));
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                animateElevation(v,
                        resources.getInteger(R.integer.activity_color_picker_favorite_box_anim_elevation_duration),
                        resources.getDimensionPixelOffset(R.dimen.activity_color_picker_favorite_box_anim_elevation_off));
                animateScaleXY(v,
                        resources.getInteger(R.integer.activity_color_picker_favorite_box_anim_scale_off) / 100.0f,
                        resources.getInteger(R.integer.activity_color_picker_favorite_box_anim_scale_duration));
                return true;

            default:
                break;
        }

        return false;
    }

    /**
     * Handler of long press, single tap.<br>
     * Animates views.<br>
     * <br>
     * Long press:  add result color to favorite box.<br>
     * Single tap:  set favorite color to result box.
     */
    @NonNull
    private GestureDetector.SimpleOnGestureListener getSimpleOnGestureListener() {
        return new GestureDetector.SimpleOnGestureListener() {

            // Add result color to favorite box
            @Override
            public void onLongPress(MotionEvent e) {
                final int resultColor = context.getResultColor();
                if (dbHelper.updateFavoriteColor(index, resultColor)) {
                    playSoundAndShowToast(
                            context,
                            RingtoneManager.TYPE_NOTIFICATION,
                            resources.getString(R.string.activity_color_picker_toast_favorite_color_added),
                            Toast.LENGTH_SHORT);

                    setBackgroundColor(view, resultColor);
                    animateScaleXY(view,
                            resources.getInteger(R.integer.activity_color_picker_favorite_box_anim_scale_on) / 100.0f,
                            resources.getInteger(R.integer.activity_color_picker_favorite_box_anim_scale_duration));
                }
            }

            // Set favorite color to result box
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                final int color = dbHelper.getFavoriteColor(index);
                if (color != -1) {
                    setBackgroundColor(view, color);
                    context.setResultBoxColor(color);
                }
                return true;
            }
        };
    }
}
