package com.gamaliev.notes.color_picker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.media.RingtoneManager;
import android.support.annotation.NonNull;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.gamaliev.notes.R;
import com.gamaliev.notes.color_picker.db.ColorPickerDbHelper;

import static com.gamaliev.notes.common.CommonUtils.animateElevation;
import static com.gamaliev.notes.common.CommonUtils.animateScaleXy;
import static com.gamaliev.notes.common.CommonUtils.playSoundAndShowToast;
import static com.gamaliev.notes.common.CommonUtils.setBackgroundColor;

/**
 * @author Vadim Gamaliev
 * <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

final class FavoriteColorBoxOnTouchListener implements View.OnTouchListener {

    /* ... */
    @NonNull private final Context mContext;
    @NonNull private final ColorPickerFragment mFragment;
    @NonNull private final Resources mRes;
    @NonNull private final GestureDetector mGestureDetector;
    @NonNull private final View mView;
    private final int mIndex;


    /*
        Init
     */

    FavoriteColorBoxOnTouchListener(
            @NonNull final Context context,
            @NonNull final ColorPickerFragment fragment,
            @NonNull final View view,
            final int index) {

        mIndex      = index;
        mContext    = context;
        mFragment   = fragment;
        mView       = view;
        mRes        = context.getResources();
        mGestureDetector = new GestureDetector(context, getSimpleOnGestureListener());
    }


    /*
        View.OnTouchListener
     */

    /**
     * Handler of long press, single tap.<br>
     * Animates views.<br>
     * <br>
     * Long press, single tap: see {@link #getSimpleOnGestureListener()}
     */
    @SuppressLint({"ClickableViewAccessibility"})
    @Override
    public boolean onTouch(final View v, final MotionEvent e) {
        mGestureDetector.onTouchEvent(e);

        // Elevation and scale animation.
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                animateElevation(v,
                        mRes.getInteger(R.integer.fragment_color_picker_favorite_box_anim_elevation_duration),
                        mRes.getDimensionPixelOffset(R.dimen.fragment_color_picker_favorite_box_anim_elevation_on));
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                animateElevation(v,
                        mRes.getInteger(R.integer.fragment_color_picker_favorite_box_anim_elevation_duration),
                        mRes.getDimensionPixelOffset(R.dimen.fragment_color_picker_favorite_box_anim_elevation_off));
                animateScaleXy(v,
                        mRes.getInteger(R.integer.fragment_color_picker_favorite_box_anim_scale_off) / 100.0f,
                        mRes.getInteger(R.integer.fragment_color_picker_favorite_box_anim_scale_duration));
                return true;
            default:
                break;
        }

        return false;
    }


    /*
        ...
     */

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

            // Add result color to favorite box.
            @Override
            public void onLongPress(final MotionEvent e) {
                final int resultColor = mFragment.getResultColor();
                if (ColorPickerDbHelper.updateFavoriteColor(mContext, mIndex, resultColor)) {
                    playSoundAndShowToast(
                            mContext,
                            RingtoneManager.TYPE_NOTIFICATION,
                            mRes.getString(R.string.fragment_color_picker_toast_favorite_color_added),
                            Toast.LENGTH_SHORT);
                    setBackgroundColor(mView, resultColor);
                    animateScaleXy(mView,
                            mRes.getInteger(R.integer.fragment_color_picker_favorite_box_anim_scale_on) / 100.0f,
                            mRes.getInteger(R.integer.fragment_color_picker_favorite_box_anim_scale_duration));
                }
            }

            // Set favorite color to result box.
            @Override
            public boolean onSingleTapConfirmed(final MotionEvent e) {
                final int color = ColorPickerDbHelper.getFavoriteColor(mContext, mIndex);
                if (color != -1) {
                    setBackgroundColor(mView, color);
                    mFragment.setResultBoxColor(color);
                }
                return true;
            }
        };
    }
}