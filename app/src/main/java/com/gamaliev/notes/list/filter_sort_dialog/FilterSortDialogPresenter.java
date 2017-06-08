package com.gamaliev.notes.list.filter_sort_dialog;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.DatePicker;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.CommonUtils;
import com.gamaliev.notes.list.db.ListDbHelper;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.gamaliev.notes.app.NotesApp.getAppContext;
import static com.gamaliev.notes.color_picker.db.ColorPickerDbHelper.getFavoriteColor;
import static com.gamaliev.notes.color_picker.db.ColorPickerDbHelper.getFavoriteColorsDefault;
import static com.gamaliev.notes.common.CommonUtils.EXTRA_DATES_BOTH;
import static com.gamaliev.notes.common.CommonUtils.EXTRA_DATES_FROM_DATETIME;
import static com.gamaliev.notes.common.CommonUtils.EXTRA_DATES_FROM_DATE_UTC_TO_LOCALTIME;
import static com.gamaliev.notes.common.CommonUtils.EXTRA_DATES_TO_DATETIME;
import static com.gamaliev.notes.common.CommonUtils.EXTRA_DATES_TO_DATE_UTC_TO_LOCALTIME;
import static com.gamaliev.notes.common.CommonUtils.convertLocalToUtc;
import static com.gamaliev.notes.common.CommonUtils.getDateFromProfileMap;
import static com.gamaliev.notes.common.CommonUtils.getMainHandler;
import static com.gamaliev.notes.common.CommonUtils.getStringDateFormatSqlite;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_TABLE_NAME;
import static com.gamaliev.notes.common.db.DbHelper.getEntriesCount;
import static com.gamaliev.notes.common.shared_prefs.SpCommon.convertJsonToMap;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_COLOR;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_ID;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_ORDER;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_ORDER_ASC;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_PROFILE_CURRENT_ID;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_SYMBOL_DATE_SPLIT;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_TITLE;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.addForCurrentUser;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.deleteForCurrentUser;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.getDefaultProfile;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.getProfilesForCurrentUser;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.getSelectedForCurrentUser;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.getSelectedIdForCurrentUser;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.setSelectedForCurrentUser;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.updateCurrentForCurrentUser;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

@SuppressWarnings("NullableProblems")
class FilterSortDialogPresenter implements FilterSortDialogContract.Presenter {

    /* Logger */
    @NonNull private static final String TAG = FilterSortDialogPresenter.class.getSimpleName();

    /* Extra */
    @NonNull private static final String EXTRA_PROFILE_MAP = "FilterSortDialogFragment.EXTRA_PROFILE_MAP";
    @NonNull private static final String EXTRA_FOUNDED_MAP = "FilterSortDialogFragment.EXTRA_FOUNDED_MAP";
    @NonNull private static final String EXTRA_SELECTED_ID = "FilterSortDialogFragment.EXTRA_SELECTED_ID";

    /* ... */
    @NonNull private final Context mContext;
    @NonNull private final FilterSortDialogContract.View mFilterSortDialogView;

    @NonNull private Map<String, String> mFilterProfileMap;
    @NonNull private Map<String, String> mFoundedEntriesCache;
    @NonNull private String mSelectedFilterProfile;
    @NonNull private ExecutorService mSingleThreadExecutor;
    @NonNull private Set<Runnable> mRunnableTasks;


    /*
        Init
     */

    FilterSortDialogPresenter(
            @NonNull final FilterSortDialogContract.View filterSortDialogView) {

        mContext = getAppContext();
        mFilterSortDialogView = filterSortDialogView;

        mFilterSortDialogView.setPresenter(this);
    }


    /*
        FilterSortDialogContract.Presenter
     */

    @Override
    public void start() {}

    @Override
    public void onViewCreated(@Nullable final Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            //noinspection unchecked,ConstantConditions
            mFilterProfileMap       = (Map<String, String>) savedInstanceState.getSerializable(EXTRA_PROFILE_MAP);
            //noinspection unchecked,ConstantConditions
            mFoundedEntriesCache    = (Map<String, String>) savedInstanceState.getSerializable(EXTRA_FOUNDED_MAP);
            //noinspection ConstantConditions
            mSelectedFilterProfile  = savedInstanceState.getString(EXTRA_SELECTED_ID);
        } else {
            initLocalVariables();
        }

        mSingleThreadExecutor = Executors.newSingleThreadExecutor();
        mRunnableTasks = new HashSet<>();
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        outState.putSerializable(EXTRA_PROFILE_MAP, (HashMap) mFilterProfileMap);
        outState.putSerializable(EXTRA_FOUNDED_MAP, (HashMap) mFoundedEntriesCache);
        outState.putString(EXTRA_SELECTED_ID, mSelectedFilterProfile);
    }

    @Override
    public void onViewAttachedToWindow() {
        startRunnableTasks();
    }

    @Override
    public void onViewDetachedFromWindow() {
        mSingleThreadExecutor.shutdownNow();
    }

    @Override
    public void initProfilesComponents() {
        final Set<String> profilesSet = getProfilesForCurrentUser(mContext);
        if (profilesSet == null) {
            Log.e(TAG, "getProfilesForCurrentUser() is null.");
            mFilterSortDialogView.dismiss();
            return;
        }

        for (String profileJson : profilesSet) {
            final Map<String, String> profileMap = convertJsonToMap(profileJson);
            if (profileMap == null) {
                Log.e(TAG, "profileMap is null.");
                mFilterSortDialogView.dismiss();
                return;
            }
            final String filterId = profileMap.get(SP_FILTER_ID);

            mFilterSortDialogView.addNewProfileView(profileMap);

            // Cache.
            final String foundedEntriesCache = mFoundedEntriesCache.get(filterId);
            if (foundedEntriesCache == null) {
                mRunnableTasks.add(new Runnable() {
                    @Override
                    public void run() {
                        if (mFilterSortDialogView.isActive()) {
                            final int foundedEntries = getEntriesCount(
                                    mContext,
                                    LIST_ITEMS_TABLE_NAME,
                                    ListDbHelper.convertToQueryBuilder(
                                            mContext,
                                            null,
                                            profileMap));

                            mFoundedEntriesCache.put(filterId, String.valueOf(foundedEntries));

                            if (mFilterSortDialogView.isActive()) {
                                getMainHandler().post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mFilterSortDialogView.showProfileFoundedEntriesText(
                                                filterId,
                                                String.valueOf(foundedEntries));
                                    }
                                });
                            }
                        }
                    }
                });

            } else {
                mFilterSortDialogView.showProfileFoundedEntriesText(filterId, foundedEntriesCache);
            }

            // Highlight title. Colorize and set underline.
            if (mSelectedFilterProfile.equals(filterId)) {
                mFilterProfileMap = profileMap;
                mFilterSortDialogView.highlightProfileTitle(filterId);
            }
        }
    }

    @Override
    public void initSortComponents() {
        final String order = mFilterProfileMap.get(SP_FILTER_ORDER);
        if (order == null) {
            Log.e(TAG, "Order is null.");
            mFilterSortDialogView.dismiss();
            return;
        }

        final String orderAsc = mFilterProfileMap.get(SP_FILTER_ORDER_ASC);
        if (orderAsc == null) {
            Log.e(TAG, "OrderAsc is null.");
            mFilterSortDialogView.dismiss();
            return;
        }

        mFilterSortDialogView.initSortComponents(order, orderAsc);
    }

    @Override
    public void initColorComponents() {
        if (TextUtils.isEmpty(mFilterProfileMap.get(SP_FILTER_COLOR))) {
            mFilterSortDialogView.setCheckedColorCb(true);
        }

        // Seek all favorite colors.
        final int n = getFavoriteColorsDefault().length;
        final String colorFromSettings = mFilterProfileMap.get(SP_FILTER_COLOR);
        for (int i = 0; i < n; i++) {
            final int color = getFavoriteColor(mContext, i);
            boolean selected = false;
            if (!TextUtils.isEmpty(colorFromSettings)
                    && Integer.parseInt(colorFromSettings) == color) {
                selected = true;
            }
            mFilterSortDialogView.addNewColorView(color, selected);
        }
    }

    @Override
    public void addProfile(@NonNull final String profileName) {
        mFilterProfileMap.put(SP_FILTER_ID, "");
        mFilterProfileMap.put(SP_FILTER_TITLE, profileName);
        final String newId = addForCurrentUser(mContext, mFilterProfileMap);
        mFilterProfileMap.put(SP_FILTER_ID, newId);

        mFilterSortDialogView.initComponents();
    }

    @Override
    public void resetProfile() {
        mSelectedFilterProfile = SP_FILTER_PROFILE_CURRENT_ID;
        final Map<String, String> map = convertJsonToMap(getDefaultProfile());
        if (map == null) {
            Log.e(TAG, "convertJsonToMap(getDefaultProfile()) is null.");
            mFilterSortDialogView.dismiss();
            return;
        } else {
            mFilterProfileMap = map;
        }
        mFilterSortDialogView.initComponents();
    }

    @Override
    public void performProfileClick(@NonNull final String filterId) {
        mSelectedFilterProfile = filterId;
        mFilterSortDialogView.unsetSortingListeners(); /* Bug */
        mFilterSortDialogView.initComponents();
        mFilterSortDialogView.setSortingListeners(); /* Bug */
    }

    @Override
    public void performProfileDeleteClick(@NonNull final String filterId) {
        deleteForCurrentUser(mContext, filterId);

        if (mSelectedFilterProfile.equals(filterId)) {
            initLocalVariables();
            mFilterSortDialogView.initComponents();
        } else {
            mFilterSortDialogView.initProfilesComponents();
        }
    }

    @Override
    public void performColorCbClick() {
        mFilterProfileMap.put(SP_FILTER_COLOR, "");
        mSelectedFilterProfile = SP_FILTER_PROFILE_CURRENT_ID;
        mFilterSortDialogView.initComponents();
    }

    @Override
    public void performColorClick(final int color) {
        mFilterProfileMap.put(SP_FILTER_COLOR, String.valueOf(color));
        mSelectedFilterProfile = SP_FILTER_PROFILE_CURRENT_ID;
        mFilterSortDialogView.initComponents();
    }

    @Override
    public void performCancelBtnClick() {
        mSingleThreadExecutor.shutdownNow();
        mFilterSortDialogView.dismiss();
    }

    @Override
    public void performFilterBtnClick() {
        mSingleThreadExecutor.shutdownNow();

        setSelectedForCurrentUser(mContext, mSelectedFilterProfile);
        if (SP_FILTER_PROFILE_CURRENT_ID.equals(mSelectedFilterProfile)) {
            updateCurrentForCurrentUser(mContext, mFilterProfileMap);
        }
        mFilterSortDialogView.finish();
        mFilterSortDialogView.dismiss();
    }

    @Override
    public void setDatesStatus(
            @NonNull final String filterCategory,
            final int checkboxId,
            final int dateFromId,
            final int dateToId) {

        if (!TextUtils.isEmpty(getDateFromProfileMap(
                mContext,
                mFilterProfileMap,
                filterCategory,
                EXTRA_DATES_BOTH))) {

            final String localtimeDateFrom = getDateFromProfileMap(
                    mContext,
                    mFilterProfileMap,
                    filterCategory,
                    EXTRA_DATES_FROM_DATE_UTC_TO_LOCALTIME);
            if (localtimeDateFrom == null) {
                Log.e(TAG, "localtimeDateFrom is null");
                mFilterSortDialogView.dismiss();
                return;
            }

            final String localtimeDateTo = getDateFromProfileMap(
                    mContext,
                    mFilterProfileMap,
                    filterCategory,
                    EXTRA_DATES_TO_DATE_UTC_TO_LOCALTIME);
            if (localtimeDateTo == null) {
                Log.e(TAG, "localtimeDateTo is null");
                mFilterSortDialogView.dismiss();
                return;
            }

            mFilterSortDialogView.setDatesStatus(
                    checkboxId,
                    dateFromId,
                    dateToId,
                    localtimeDateFrom,
                    localtimeDateTo);

        } else {
            mFilterSortDialogView.setDatesStatusNull(
                    checkboxId,
                    dateFromId,
                    dateToId);
        }
    }

    @Override
    public void updateOrder(@NonNull final String order) {
        mFilterProfileMap.put(SP_FILTER_ORDER, order);
        mSelectedFilterProfile = SP_FILTER_PROFILE_CURRENT_ID;
        mFilterSortDialogView.initProfilesComponents();
    }

    @Override
    public void updateOrderAsc(@NonNull final String orderAsc) {
        mFilterProfileMap.put(SP_FILTER_ORDER_ASC, orderAsc);
        mSelectedFilterProfile = SP_FILTER_PROFILE_CURRENT_ID;
        mFilterSortDialogView.initProfilesComponents();
    }

    @Override
    public void updateDate(
            @NonNull final String filterCategory,
            @NonNull final String date) {
        mFilterProfileMap.put(filterCategory, date);
    }

    @Override
    public void onChange() {
        mSelectedFilterProfile = SP_FILTER_PROFILE_CURRENT_ID;
        mFilterSortDialogView.initProfilesComponents();
        mFilterSortDialogView.refreshFoundText();
    }

    @Override
    public void refreshFoundText() {
        // Cache.
        final String entriesCount = mFoundedEntriesCache.get(mSelectedFilterProfile);
        if (mSelectedFilterProfile.equals(mFilterProfileMap.get(SP_FILTER_ID))
                && entriesCount != null) {

            final String text = mContext.getString(R.string.fragment_list_filter_dialog_profile_found_text)
                    + " " + entriesCount;
            mFilterSortDialogView.showFoundText(text);

        } else {
            mRunnableTasks.add(new Runnable() {
                @Override
                public void run() {
                    if (mFilterSortDialogView.isActive()) {
                        final long count = getEntriesCount(
                                mContext,
                                LIST_ITEMS_TABLE_NAME,
                                ListDbHelper.convertToQueryBuilder(
                                        mContext,
                                        null,
                                        mFilterProfileMap));

                        if (mFilterSortDialogView.isActive()) {
                            getMainHandler().post(new Runnable() {
                                @Override
                                public void run() {
                                    final String text = mContext.getString(
                                            R.string.fragment_list_filter_dialog_profile_found_text)
                                            + " " + count;
                                    mFilterSortDialogView.showFoundText(text);
                                }
                            });
                        }
                    }
                }
            });
        }

        startRunnableTasks();
    }

    @Override
    public void initDatePicker(
            @NonNull final Context context,
            @NonNull final String filterCategory,
            final boolean isDateFrom) {

        int year;
        int month;
        int dayOfMonth;

        if (!TextUtils.isEmpty(getDateFromProfileMap(
                mContext,
                mFilterProfileMap,
                filterCategory,
                EXTRA_DATES_BOTH))) {

            if (isDateFrom) {
                // If "date_from".
                final String dateFrom = getDateFromProfileMap(
                        mContext,
                        mFilterProfileMap,
                        filterCategory,
                        EXTRA_DATES_FROM_DATE_UTC_TO_LOCALTIME);
                if (dateFrom == null) {
                    Log.e(TAG, "Cannot get dateFrom.");
                    mFilterSortDialogView.dismiss();
                    return;
                }
                final String[] fromDateArray = dateFrom.split("-");

                year        = Integer.parseInt(fromDateArray[0]);
                month       = Integer.parseInt(fromDateArray[1]);
                dayOfMonth  = Integer.parseInt(fromDateArray[2]);

            } else {
                // If "date_to".
                final String dateTo = getDateFromProfileMap(
                        mContext,
                        mFilterProfileMap,
                        filterCategory,
                        EXTRA_DATES_TO_DATE_UTC_TO_LOCALTIME);
                if (dateTo == null) {
                    Log.e(TAG, "Cannot get dateTo.");
                    mFilterSortDialogView.dismiss();
                    return;
                }
                final String[] toDateArray = dateTo.split("-");

                year        = Integer.parseInt(toDateArray[0]);
                month       = Integer.parseInt(toDateArray[1]);
                dayOfMonth  = Integer.parseInt(toDateArray[2]);
            }

            // Here months 0-11;
            if (month == 0) {
                //noinspection CheckStyle
                month = 11;
            } else {
                month -= 1;
            }

        } else {
            // If text not exist, then set current.
            final Calendar instance
                    = Calendar.getInstance();

            year        = instance.get(Calendar.YEAR);
            month       = instance.get(Calendar.MONTH);
            dayOfMonth  = instance.get(Calendar.DATE);
        }

        final DatePickerDialog datePicker = new DatePickerDialog(
                context,
                getDatePickerListener(filterCategory, isDateFrom),
                year,
                month,
                dayOfMonth);
        datePicker.show();
    }


    /*
        ...
     */

    private void initLocalVariables() {
        initFilterProfile();
        initSelectedIdFilterProfile();
        mFoundedEntriesCache = new HashMap<>();
    }

    private void initFilterProfile() {
        final String selectedFilter = getSelectedForCurrentUser(mContext);
        if (selectedFilter == null) {
            Log.e(TAG, "getSelectedForCurrentUser() is null.");
            mFilterSortDialogView.dismiss();
            return;
        }

        final Map<String, String> map = convertJsonToMap(selectedFilter);
        if (map == null) {
            Log.e(TAG, "convertJsonToMap(selectedFilter) is null.");
            mFilterSortDialogView.dismiss();
        } else {
            mFilterProfileMap = map;
        }
    }

    private void initSelectedIdFilterProfile() {
        final String selected = getSelectedIdForCurrentUser(mContext);
        if (selected == null) {
            Log.e(TAG, "getSelectedIdForCurrentUser() is null.");
            mFilterSortDialogView.dismiss();
        } else {
            mSelectedFilterProfile = selected;
        }
    }

    @NonNull
    private DatePickerDialog.OnDateSetListener getDatePickerListener(
            @NonNull final String filterCategory,
            final boolean isDateFrom) {

        return new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                // if action done -- called twice. Checking.
                if (view.isShown()) {
                    final Calendar date = Calendar.getInstance();
                    date.set(year, month, dayOfMonth, 0, 0, 0);
                    final String resultDate =
                            getStringDateFormatSqlite(mContext, date.getTime(), false);

                    updateDateSettings(filterCategory, resultDate, isDateFrom);
                    mSelectedFilterProfile = SP_FILTER_PROFILE_CURRENT_ID;
                    mFilterSortDialogView.initComponents();
                }
            }
        };
    }

    private void updateDateSettings(
            @NonNull final String filterCategory,
            @NonNull final String resultLocaltimeDateS,
            final boolean isDateFrom) {

        final String resultUtcDateS = convertLocalToUtc(mContext, resultLocaltimeDateS);
        String newUtcDateFromS;
        String newUtcDateToS;

        final DateFormat dateFormatUtc = CommonUtils.getDateFormatSqlite(mContext, true);

        Date resultUtcDateD = null;
        try {
            resultUtcDateD = dateFormatUtc.parse(resultLocaltimeDateS);
        } catch (ParseException e) {
            Log.e(TAG, e.toString());
        }
        if (resultUtcDateD == null) {
            return;
        }

        if (!TextUtils.isEmpty(getDateFromProfileMap(
                mContext,
                mFilterProfileMap,
                filterCategory,
                EXTRA_DATES_BOTH))) {

            try {
                // Parse "date_from".
                final Date utcDateFrom = dateFormatUtc.parse(
                        getDateFromProfileMap(
                                mContext,
                                mFilterProfileMap,
                                filterCategory,
                                EXTRA_DATES_FROM_DATETIME));

                // Parse "date_to".
                final Date utcDateTo = dateFormatUtc.parse(
                        getDateFromProfileMap(
                                mContext,
                                mFilterProfileMap,
                                filterCategory,
                                EXTRA_DATES_TO_DATETIME));

                if (isDateFrom) {
                    // Set new date;
                    newUtcDateFromS = resultUtcDateS;

                    // If new "from date" before "to date", then "to date" hold as is,
                    // else change to new.
                    newUtcDateToS = resultUtcDateD.before(utcDateTo)
                            ? getDateFromProfileMap(
                            mContext,
                            mFilterProfileMap,
                            filterCategory,
                            EXTRA_DATES_TO_DATETIME)
                            : resultUtcDateS;

                } else {
                    // If new "to date" after "from date", then "from date" hold as is,
                    // else change to new.
                    newUtcDateFromS = resultUtcDateD.after(utcDateFrom)
                            ? getDateFromProfileMap(
                            mContext,
                            mFilterProfileMap,
                            filterCategory,
                            EXTRA_DATES_FROM_DATETIME)
                            : resultUtcDateS;

                    // Set new date;
                    newUtcDateToS = resultUtcDateS;
                }

            } catch (ParseException e) {
                Log.e(TAG, e.toString());
                newUtcDateFromS = resultUtcDateS;
                newUtcDateToS = resultUtcDateS;
            }

        } else {
            // If profileMap not contains dates - Create new pair.
            newUtcDateFromS = resultUtcDateS;
            newUtcDateToS = resultUtcDateS;
        }

        // Update dates.
        mFilterProfileMap.put(
                filterCategory,
                newUtcDateFromS + SP_FILTER_SYMBOL_DATE_SPLIT + newUtcDateToS);
    }


    /*
        Async tasks.
     */

    private void startRunnableTasks() {
        if (mFilterSortDialogView.isActive()) {
            for (Runnable task : mRunnableTasks) {
                mSingleThreadExecutor.submit(task);
            }
            mRunnableTasks.clear();
        }
    }
}
