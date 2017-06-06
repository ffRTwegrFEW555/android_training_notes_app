package com.gamaliev.notes.color_picker;

import com.gamaliev.notes.BasePresenter;
import com.gamaliev.notes.BaseView;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public interface ColorPickerContract {

    interface View extends BaseView<Presenter> {

        /**
         * Add favorite colors to parent view, and set listeners.
         * @param favoriteColors Array, where index of array is index of color, value is color.
         */
        void addFavoriteColorBoxesAndSetListenersUiThread(int[] favoriteColors);

        /**
         * Update favorite view by given color.
         * @param view  ColorBox, that will be updated.
         * @param color Color to update.
         */
        void updateFavoriteColor(android.view.View view, int color);

        /**
         * Update result view by given color.
         * @param view  Favorite ColorBox, that will be refreshed.
         * @param color Color to update.
         */
        void updateResultColor(android.view.View view, int color);

        /**
         * @return Selected color.
         */
        int getResultColor();

        /**
         * @return True, if view is active (attached or added), otherwise false.
         */
        boolean isActive();
    }

    interface Presenter extends BasePresenter {

        /**
         * Update color in database.
         * @param view  ColorBox, that will be updated.
         * @param index Index of color.
         */
        void updateFavoriteColor(android.view.View view, int index);

        /**
         * Load color from database, and set to result box view.
         * @param view  ColorBox, that was clicked.
         * @param index Index of color.
         */
        void loadFavoriteColor(android.view.View view, int index);
    }
}
