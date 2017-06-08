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

class ColorPickerPresenter implements ColorPickerContract.Presenter {

    /* ... */
    @NonNull private final ColorPickerContract.View mColorPickerView;
    @NonNull private final Context mContext;


    /*
        Init
     */

    ColorPickerPresenter(@NonNull final ColorPickerContract.View colorPickerView) {
        mContext = getAppContext();
        mColorPickerView = colorPickerView;

        mColorPickerView.setPresenter(this);
    }


    /*
        ColorPickerContract.Presenter
     */

    @Override
    public void start() {
        loadFavoriteColors();
    }

    @Override
    public void updateFavoriteColor(@NonNull final View view, final int index) {
        final int resultColor = mColorPickerView.getResultColor();
        if (ColorPickerDbHelper.updateFavoriteColor(mContext, index, resultColor)
                && mColorPickerView.isActive()) {
            mColorPickerView.updateFavoriteColor(view, resultColor);
        }
    }

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
        final int[] favoriteColors = getAllFavoriteColors(mContext);
        if (mColorPickerView.isActive()) {
            mColorPickerView.addFavoriteColorBoxesAndSetListenersUiThread(favoriteColors);
        }
    }
}
