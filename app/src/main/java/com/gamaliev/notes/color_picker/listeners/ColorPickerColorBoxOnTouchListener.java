package com.gamaliev.notes.color_picker.listeners;

import android.annotation.SuppressLint;
import android.content.Context;
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
import com.gamaliev.notes.color_picker.ColorPickerFragment;
import com.gamaliev.notes.common.custom_view.SwitchableHorizontalScrollView;

import static com.gamaliev.notes.common.CommonUtils.animateElevation;
import static com.gamaliev.notes.common.CommonUtils.makeVibrate;
import static com.gamaliev.notes.common.CommonUtils.playSoundAndShowToast;
import static com.gamaliev.notes.common.CommonUtils.setBackgroundColorRectangleApi;
import static com.gamaliev.notes.common.CommonUtils.showToast;

/**
 * @author Vadim Gamaliev
 * <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class ColorPickerColorBoxOnTouchListener implements View.OnTouchListener {

    /* ... */
    @NonNull private final Context mContext;
    @NonNull private final ColorPickerFragment mFragment;
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


    /*
        Init
     */

    /**
     * @param context   Context.
     * @param fragment  Fragment.
     * @param view      ColorBox, to be listened.
     * @param index     Index of color.
     */
    public ColorPickerColorBoxOnTouchListener(
            @NonNull final Context context,
            @NonNull final ColorPickerFragment fragment,
            @NonNull final View view,
            final int index) {

        mContext    = context;
        mFragment   = fragment;
        mView       = view;
        mIndex      = index;

        mResources          = context.getResources();
        mGestureDetector    = new GestureDetector(context, getSimpleOnGestureListener());
        mPaletteHsvSv       = fragment.getPaletteHsvSv();
        mEditPw             = fragment.getEditPw();
        mHsvColors          = fragment.getHsvColors();
        mHsvColorsOverridden = fragment.getHsvColorsOverridden();
        mHsvDegree          = fragment.getHsvDegree();
    }


    /*
        View.OnTouchListener
     */

    /**
     * Handler of long press, double tap, single tap, move action<br>
     * Animates views.<br>
     * <br>
     * Long press, double tap, single tap: see {@link #getSimpleOnGestureListener()}<br>
     * Move action: change palette box color, when "edit mode" is turn on.
     */
    @SuppressLint({"ClickableViewAccessibility"})
    @Override
    public boolean onTouch(final View v, final MotionEvent e) {
        mGestureDetector.onTouchEvent(e);

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                animateElevation(mView,
                        mResources.getInteger(R.integer.fragment_color_picker_palette_box_anim_elevation_duration),
                        mResources.getDimensionPixelSize(R.dimen.fragment_color_picker_palette_box_anim_elevation_on));
                return true;

            // Enable scrolling and turn off "Edit mode".
            case MotionEvent.ACTION_UP:
                if (!mPaletteHsvSv.isEnableScrolling()) {
                    mEditable = false;
                    mPaletteHsvSv.setEnableScrolling(true);
                    mEditPw.dismiss();
                    showToast(mResources.getString(R.string.fragment_color_picker_toast_edit_mode_off),
                            Toast.LENGTH_SHORT);
                }

                animateElevation(mView,
                        mResources.getInteger(R.integer.fragment_color_picker_palette_box_anim_elevation_duration),
                        mResources.getDimensionPixelSize(R.dimen.fragment_color_picker_palette_box_anim_elevation_off));
                return true;

            case MotionEvent.ACTION_CANCEL:
                mEditable = false;
                animateElevation(mView,
                        mResources.getInteger(R.integer.fragment_color_picker_palette_box_anim_elevation_duration),
                        mResources.getDimensionPixelSize(R.dimen.fragment_color_picker_palette_box_anim_elevation_off));
                return true;

            // Edit mode: Change color.
            case MotionEvent.ACTION_MOVE:
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
                    mFragment.setColorOnMove(mView, newColor, mIndex);

                    // Notify when border reaches.
                    if (!mPauseNotify && (x0 > size || x0 < 0 || y0 > size || y0 < 0)) {
                        makeVibrate(
                                mContext,
                                mResources.getInteger(R.integer.fragment_color_picker_vibration_duration));
                        mPauseNotify = true;
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mPauseNotify = false;
                            }
                        }, mResources.getInteger(R.integer.fragment_color_picker_notification_interval_border_reaches));
                    }

                } else {
                    // Check condition to start change color.
                    if (mLongPressed) {
                        int delta = (int) mResources.getDimension(
                                R.dimen.fragment_color_picker_delta_to_enable_change_color);
                        if (Math.abs(e.getX() - mX1) > delta
                                || Math.abs(e.getY() - mY1) > delta) {
                            mEditable = true;
                            mLongPressed = false;
                        }
                    }
                }
                break;

            default:
                break;
        }

        return false;
    }


    /*
        ...
     */

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
            // Disable scrolling and turn on "Edit mode".
            @Override
            public void onLongPress(final MotionEvent e) {
                playSoundAndShowToast(
                        RingtoneManager.TYPE_NOTIFICATION,
                        mResources.getString(R.string.fragment_color_picker_toast_edit_mode_on),
                        Toast.LENGTH_SHORT);

                mPaletteHsvSv.setEnableScrolling(false);
                mLongPressed = true;
                mX1 = e.getX();
                mY1 = e.getY();

                mFragment.showPopupWindow();
                mFragment.setColorOnMove(mView, -1, mIndex);
            }

            // Set default color back to palette box.
            @Override
            public boolean onDoubleTap(final MotionEvent e) {
                setBackgroundColorRectangleApi(mContext, mView, mHsvColors[mIndex]);
                mHsvColorsOverridden[mIndex] = -1;
                return true;
            }

            // Set palette color to result box.
            @Override
            public boolean onSingleTapConfirmed(final MotionEvent e) {
                int result = mHsvColorsOverridden[mIndex] != -1
                        ? mHsvColorsOverridden[mIndex] : mHsvColors[mIndex];
                mFragment.setResultBoxColor(result);
                return true;
            }
        };
    }
}