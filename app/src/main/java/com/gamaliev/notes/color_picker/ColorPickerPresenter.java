package com.gamaliev.notes.color_picker;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;

import com.gamaliev.notes.color_picker.db.ColorPickerDbHelper;

import static com.gamaliev.notes.app.NotesApp.getAppContext;
import static com.gamaliev.notes.color_picker.db.ColorPickerDbHelper.getAllFavoriteColors;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class ColorPickerPresenter implements ColorPickerContract.Presenter {

    /* ... */
    @NonNull private final ColorPickerContract.View mColorPickerView;
    @NonNull private final Context mContext;


    /*
        Init
     */

    public ColorPickerPresenter(@NonNull final ColorPickerContract.View colorPickerView) {
        mColorPickerView = colorPickerView;
        mColorPickerView.setPresenter(this);
        mContext = getAppContext();
    }


    /*
        ColorPickerContract.Presenter
     */

    @Override
    public void start() {
        loadFavoriteColors();
    }

    /**
     * Update color in database.
     * @param view  ColorBox, that will be updated.
     * @param index Index of color.
     */
    @Override
    public void updateFavoriteColor(@NonNull final View view, final int index) {
        final int resultColor = mColorPickerView.getResultColor();
        if (ColorPickerDbHelper.updateFavoriteColor(mContext, index, resultColor)
                && mColorPickerView.isActive()) {
            mColorPickerView.updateFavoriteColor(view, resultColor);
        }
    }

    /**
     * Load color from database, and set to result box view.
     * @param view  ColorBox, that was clicked.
     * @param index Index of color.
     */
    @Override
    public void loadFavoriteColor(@NonNull final View view, final int index) {
        final int color = ColorPickerDbHelper.getFavoriteColor(mContext, index);
        if (color != -1 && mColorPickerView.isActive()) {
            mColorPickerView.updateResultColor(view, color);
        }
    }


    /*
        ...
     */

    private void loadFavoriteColors() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final int[] favoriteColors = getAllFavoriteColors(mContext);
                if (mColorPickerView.isActive()) {
                    mColorPickerView.addFavoriteColorBoxesAndSetListenersUiThread(favoriteColors);
                }
            }
        }).start();
    }
}
