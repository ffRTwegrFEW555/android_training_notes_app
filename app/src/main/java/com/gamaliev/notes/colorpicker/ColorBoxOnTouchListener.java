package com.gamaliev.notes.colorpicker;

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

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.SwitchableHorizontalScrollView;

import static com.gamaliev.notes.common.CommonUtils.animateElevation;
import static com.gamaliev.notes.common.CommonUtils.makeVibrate;
import static com.gamaliev.notes.common.CommonUtils.playSoundAndShowToast;
import static com.gamaliev.notes.common.CommonUtils.setBackgroundColorRectangleAPI;
import static com.gamaliev.notes.common.CommonUtils.showToast;

/**
 * @author Vadim Gamaliev
 * <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

final class ColorBoxOnTouchListener implements View.OnTouchListener {

    @NonNull private final ColorPickerActivity mContext;
    @NonNull private final Resources mResources;
    @NonNull private final SwitchableHorizontalScrollView mPaletteHsvSv;
    @NonNull private final PopupWindow mEditPw;
    @NonNull private final GestureDetector mGestureDetector;
    @NonNull private final View mView;
    @NonNull private final int[] mHsvColors;
    @NonNull private final int[] mHsvColorsOverridden;
    private final int mIndex;
    private final float mHsvDegree;
    private boolean mEditable;
    private boolean mLongPressed;
    private boolean mPauseNotify;
    private float mX1;
    private float mY1;

    ColorBoxOnTouchListener(
            @NonNull final ColorPickerActivity context,
            @NonNull final View view,
            final int index) {

        mContext    = context;
        mView       = view;
        mIndex      = index;

        mResources          = context.getResources();
        mGestureDetector    = new GestureDetector(context, getSimpleOnGestureListener());
        mPaletteHsvSv       = context.getPaletteHsvSv();
        mEditPw             = context.getEditPw();
        mHsvColors          = context.getHsvColors();
        mHsvColorsOverridden = context.getHsvColorsOverridden();
        mHsvDegree          = context.getHsvDegree();
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
        mGestureDetector.onTouchEvent(e);

        switch (e.getAction()) {

            // Elevation animation on.
            case MotionEvent.ACTION_DOWN:
                animateElevation(mView,
                        mResources.getInteger(R.integer.activity_color_picker_palette_box_anim_elevation_duration),
                        mResources.getDimensionPixelSize(R.dimen.activity_color_picker_palette_box_anim_elevation_on));
                return true;

            // Enable scrolling and turn off "Edit mode". Notify.
            // Elevation animation off.
            case MotionEvent.ACTION_UP:
                if (!mPaletteHsvSv.isEnableScrolling()) {
                    mEditable = false;
                    mPaletteHsvSv.setEnableScrolling(true);
                    mEditPw.dismiss();
                    showToast(mContext,
                            mResources.getString(R.string.activity_color_picker_toast_edit_mode_off),
                            Toast.LENGTH_SHORT);
                }

                animateElevation(mView,
                        mResources.getInteger(R.integer.activity_color_picker_palette_box_anim_elevation_duration),
                        mResources.getDimensionPixelSize(R.dimen.activity_color_picker_palette_box_anim_elevation_off));
                return true;

            // Elevation animation off.
            case MotionEvent.ACTION_CANCEL:
                mEditable = false;
                animateElevation(mView,
                        mResources.getInteger(R.integer.activity_color_picker_palette_box_anim_elevation_duration),
                        mResources.getDimensionPixelSize(R.dimen.activity_color_picker_palette_box_anim_elevation_off));
                return true;

            // Edit mode: Change color
            case MotionEvent.ACTION_MOVE:
                // Check.
                if (mEditable) {
                    if (mPaletteHsvSv.isEnableScrolling()) {
                        mEditable = false;
                        break;
                    }

                    // Change color.
                    final float size  = v.getWidth();
                    final float start = (mIndex - 1) * mHsvDegree;
                    final float end   = (mIndex + 1) * mHsvDegree;
                    final float range = end - start;
                    final float x0    = e.getX();
                    final float y0    = e.getY();
                    final float x     = x0 > size ? size : x0 < 0 ? 0 : x0;
                    final float y     = y0 > size ? size : y0 < 0 ? 0 : y0;
                    final float hue   = ((x / size) * range) + start;
                    final float value = y / size;
                    final int newColor = Color.HSVToColor(new float[] {hue, 1f, value});
                    mContext.setColorOnMove(mView, newColor, mIndex);

                    // Notify when border reaches
                    if (!mPauseNotify && (x0 > size || x0 < 0 || y0 > size || y0 < 0)) {
                        makeVibrate(
                                mContext,
                                mResources.getInteger(R.integer.activity_color_picker_vibration_duration));
                        mPauseNotify = true;
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mPauseNotify = false;
                            }
                        }, mResources.getInteger(R.integer.activity_color_picker_notification_interval_border_reaches));
                    }

                } else {
                    // Check condition to start change color.
                    if (mLongPressed) {
                        int delta = (int) mResources.getDimension(R.dimen.activity_color_picker_delta_to_enable_change_color);
                        if (Math.abs(e.getX() - mX1) > delta
                                || Math.abs(e.getY() - mY1) > delta) {
                            mEditable = true;
                            mLongPressed = false;
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
                // Notification
                playSoundAndShowToast(
                        mContext,
                        RingtoneManager.TYPE_NOTIFICATION,
                        mResources.getString(R.string.activity_color_picker_toast_edit_mode_on),
                        Toast.LENGTH_SHORT);

                // Enable scrolling
                mPaletteHsvSv.setEnableScrolling(false);

                //
                mLongPressed = true;
                mX1 = e.getX();
                mY1 = e.getY();

                // Show popup window, and set current color.
                mContext.showPopupWindow();
                mContext.setColorOnMove(mView, -1, mIndex);
            }

            // Set default color back to palette box
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                setBackgroundColorRectangleAPI(mContext, mView, mHsvColors[mIndex]);
                // Update overridden array.
                mHsvColorsOverridden[mIndex] = -1;
                return true;
            }

            // Set palette color to result box
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                int result = mHsvColorsOverridden[mIndex] != -1
                        ? mHsvColorsOverridden[mIndex] : mHsvColors[mIndex];
                mContext.setResultBoxColor(result);
                return true;
            }
        };
    }
}