package com.gamaliev.notes.conflict.conflict_select_dialog;

import com.gamaliev.notes.BasePresenter;
import com.gamaliev.notes.BaseView;

/**
 * @author Vadim Gamaliev <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
public interface ConflictSelectDialogContract {

    interface View extends BaseView<Presenter> {

        void setServerHeader(String text);

        void setServerBody(String text);

        void setLocalHeader(String text);

        void setLocalBody(String text);

        void setSrvSaveBtnOnClickListener(android.view.View.OnClickListener listener);

        void setLocalSaveBtnOnClickListener(android.view.View.OnClickListener listener);

        void performNoWifiError();

        void performNoInternetError();

        void performSrvEntryNotFoundError();

        void performSrvConnectionRequestError();

        void performSrvConnectionServerError();

        void performLocalDbError();

        void performConflictResolutionFailedError();

        void enableSaveButtons();

        boolean isActive();

        void dismiss();
    }

    interface Presenter extends BasePresenter {
    }
}
