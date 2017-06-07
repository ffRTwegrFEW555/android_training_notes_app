package com.gamaliev.notes.color_picker;

import com.gamaliev.notes.BasePresenter;
import com.gamaliev.notes.BaseView;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public interface ColorPickerContract {

    interface View extends BaseView<Presenter> {

        void addFavoriteColorBoxesAndSetListenersUiThread(int[] favoriteColors);

        void updateFavoriteColor(android.view.View view, int color);

        void updateResultColor(android.view.View view, int color);

        int getResultColor();
    }

    interface Presenter extends BasePresenter {

        void updateFavoriteColor(android.view.View view, int index);

        void loadFavoriteColor(android.view.View view, int index);
    }
}
