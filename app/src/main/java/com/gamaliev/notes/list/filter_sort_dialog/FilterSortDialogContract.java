package com.gamaliev.notes.list.filter_sort_dialog;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gamaliev.notes.BasePresenter;
import com.gamaliev.notes.BaseView;

import java.util.Map;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

interface FilterSortDialogContract {

    interface View extends BaseView<Presenter> {

        void initComponents();

        void initProfilesComponents();

        void initSortComponents(@NonNull String order, @NonNull String orderAsc);

        void addNewProfileView(@NonNull Map<String, String> profileMap);

        void addNewColorView(int color, boolean selected);

        void setCheckedColorCb(boolean checked);

        void showProfileFoundedEntriesText(@NonNull String filterId, @NonNull String foundedEntries);

        void highlightProfileTitle(@NonNull String filterId);

        void setSortingListeners();

        void unsetSortingListeners();

        void setDatesStatus(
                int checkboxId,
                int dateFromId,
                int dateToId,
                @NonNull String localtimeDateFrom,
                @NonNull String localtimeDateTo);

        void setDatesStatusNull(
                final int checkboxId,
                final int dateFromId,
                final int dateToId);

        void refreshFoundText();

        void showFoundText(@NonNull String text);

        void finish();

        void dismiss();
    }

    interface Presenter extends BasePresenter {

        void onViewCreated(@Nullable Bundle savedInstanceState);

        void onSaveInstanceState(@NonNull Bundle outState);

        void onViewAttachedToWindow();

        void onViewDetachedFromWindow();

        void initProfilesComponents();

        void initSortComponents();

        void initColorComponents();

        void addProfile(@NonNull String profileName);

        void resetProfile();

        void performProfileClick(@NonNull String filterId);

        void performProfileDeleteClick(@NonNull String filterId);

        void performColorCbClick();

        void performColorClick(int color);

        void performCancelBtnClick();

        void performFilterBtnClick();

        void setDatesStatus(@NonNull String filterCategory,
                            int checkboxId,
                            int dateFromId,
                            int dateToId);

        void updateOrder(@NonNull String order);

        void updateOrderAsc(@NonNull String orderAsc);

        void updateDate(@NonNull String filterCategory, @NonNull String date);

        void onChange();

        void refreshFoundText();

        void initDatePicker(
                @NonNull Context context,
                @NonNull String filterCategory,
                boolean isDateFrom);
    }
}
