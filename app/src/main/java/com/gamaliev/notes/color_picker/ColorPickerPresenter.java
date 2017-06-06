package com.gamaliev.notes.color_picker;

import android.support.annotation.NonNull;

import static com.gamaliev.notes.app.NotesApp.getAppContext;
import static com.gamaliev.notes.color_picker.db.ColorPickerDbHelper.getAllFavoriteColors;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class ColorPickerPresenter implements ColorPickerContract.Presenter {

    /* ... */
    @NonNull private final ColorPickerContract.View mColorPickerView;


    /*
        Init
     */

    public ColorPickerPresenter(@NonNull final ColorPickerContract.View colorPickerView) {
        mColorPickerView = colorPickerView;
        mColorPickerView.setPresenter(this);
    }


    /*
        ColorPickerContract.Presenter
     */

    public void start() {
        loadFavoriteColors();
    }


    /*
        ...
     */

    private void loadFavoriteColors() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final int[] favoriteColors = getAllFavoriteColors(getAppContext());
                if (mColorPickerView.isActive()) {
                    mColorPickerView.addFavoriteColorBoxesAndSetListenersUiThread(favoriteColors);
                }
            }
        }).start();
    }
}
