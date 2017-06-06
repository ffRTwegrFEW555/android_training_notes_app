package com.gamaliev.notes.color_picker.listeners;

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
import com.gamaliev.notes.color_picker.ColorPickerContract;
import com.gamaliev.notes.color_picker.ColorPickerFragment;
import com.gamaliev.notes.color_picker.db.ColorPickerDbHelper;

import static com.gamaliev.notes.common.CommonUtils.animateElevation;
import static com.gamaliev.notes.common.CommonUtils.animateScaleXy;
import static com.gamaliev.notes.common.CommonUtils.playSoundAndShowToast;
import static com.gamaliev.notes.common.CommonUtils.setBackgroundColor;

/**
 * @author Vadim Gamaliev
 * <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class ColorPickerFavoriteColorBoxOnTouchListener implements View.OnTouchListener {

    /* ... */
    @NonNull private final Resources mRes;
    @NonNull private final GestureDetector mGestureDetector;
    @NonNull private final View mView;
    @NonNull private ColorPickerContract.Presenter mPresenter;
    private final int mIndex;


    /*
        Init
     */

    /**
     * @param context   Context.
     * @param view      ColorBox, to be listened.
     * @param index     Index of favorite color.
     */
    public ColorPickerFavoriteColorBoxOnTouchListener(
            @NonNull final Context context,
            @NonNull final ColorPickerContract.Presenter presenter,
            @NonNull final View view,
            final int index) {

        mPresenter  = presenter;
        mView       = view;
        mIndex      = index;

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
                mPresenter.updateFavoriteColor(mView, mIndex);
            }

            // Set favorite color to result box. Get Last value from database.
            @Override
            public boolean onSingleTapConfirmed(final MotionEvent e) {
                mPresenter.loadFavoriteColor(mView, mIndex);
                return true;
            }
        };
    }
}