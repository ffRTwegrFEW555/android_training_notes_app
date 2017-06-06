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

        boolean isActive();
    }

    interface Presenter extends BasePresenter {
    }
}
