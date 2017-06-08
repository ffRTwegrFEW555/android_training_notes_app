package com.gamaliev.notes.conflict.conflict_select_dialog;

import android.support.annotation.NonNull;

import com.gamaliev.notes.BasePresenter;
import com.gamaliev.notes.BaseView;

/**
 * @author Vadim Gamaliev <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
interface ConflictSelectDialogContract {

    interface View extends BaseView<Presenter> {

        void setServerHeader(@NonNull String text);

        void setServerBody(@NonNull String text);

        void setLocalHeader(@NonNull String text);

        void setLocalBody(@NonNull String text);

        void setSrvSaveBtnOnClickListener(android.view.View.OnClickListener listener);

        void setLocalSaveBtnOnClickListener(android.view.View.OnClickListener listener);

        void performError(@NonNull String text);

        void enableSaveButtons();

        void dismiss();
    }

    interface Presenter extends BasePresenter {
    }
}
