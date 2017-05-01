package com.gamaliev.list.list;

import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.gamaliev.list.R;
import com.gamaliev.list.colorpicker.ColorPickerDatabaseHelper;
import com.gamaliev.list.common.CommonUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.gamaliev.list.common.CommonUtils.EXTRA_DATES_BOTH;
import static com.gamaliev.list.common.CommonUtils.EXTRA_DATES_FROM_DATETIME;
import static com.gamaliev.list.common.CommonUtils.EXTRA_DATES_FROM_DATE_UTC_TO_LOCALTIME;
import static com.gamaliev.list.common.CommonUtils.EXTRA_DATES_TO_DATETIME;
import static com.gamaliev.list.common.CommonUtils.EXTRA_DATES_TO_DATE_UTC_TO_LOCALTIME;
import static com.gamaliev.list.common.CommonUtils.convertLocalToUtc;
import static com.gamaliev.list.common.CommonUtils.getDateFromProfileMap;
import static com.gamaliev.list.common.CommonUtils.getResourceColorApi;
import static com.gamaliev.list.common.CommonUtils.getStringDateFormatSqlite;
import static com.gamaliev.list.common.DatabaseHelper.LIST_ITEMS_COLUMN_CREATED;
import static com.gamaliev.list.common.DatabaseHelper.LIST_ITEMS_COLUMN_EDITED;
import static com.gamaliev.list.common.DatabaseHelper.LIST_ITEMS_COLUMN_TITLE;
import static com.gamaliev.list.common.DatabaseHelper.LIST_ITEMS_COLUMN_VIEWED;
import static com.gamaliev.list.common.DatabaseHelper.ORDER_ASCENDING;
import static com.gamaliev.list.common.DatabaseHelper.ORDER_DESCENDING;
import static com.gamaliev.list.list.ListActivitySharedPreferencesUtils.SP_FILTER_COLOR;
import static com.gamaliev.list.list.ListActivitySharedPreferencesUtils.SP_FILTER_CREATED;
import static com.gamaliev.list.list.ListActivitySharedPreferencesUtils.SP_FILTER_EDITED;
import static com.gamaliev.list.list.ListActivitySharedPreferencesUtils.SP_FILTER_ID;
import static com.gamaliev.list.list.ListActivitySharedPreferencesUtils.SP_FILTER_ORDER;
import static com.gamaliev.list.list.ListActivitySharedPreferencesUtils.SP_FILTER_ORDER_ASC;
import static com.gamaliev.list.list.ListActivitySharedPreferencesUtils.SP_FILTER_PROFILE_CURRENT;
import static com.gamaliev.list.list.ListActivitySharedPreferencesUtils.SP_FILTER_PROFILE_CURRENT_ID;
import static com.gamaliev.list.list.ListActivitySharedPreferencesUtils.SP_FILTER_PROFILE_SELECTED_ID;
import static com.gamaliev.list.list.ListActivitySharedPreferencesUtils.SP_FILTER_SYMBOL_DATE_SPLIT;
import static com.gamaliev.list.list.ListActivitySharedPreferencesUtils.SP_FILTER_TITLE;
import static com.gamaliev.list.list.ListActivitySharedPreferencesUtils.SP_FILTER_VIEWED;
import static com.gamaliev.list.list.ListActivitySharedPreferencesUtils.convertProfileJsonToMap;
import static com.gamaliev.list.list.ListActivitySharedPreferencesUtils.convertProfileMapToJson;
import static com.gamaliev.list.list.ListActivitySharedPreferencesUtils.deleteProfile;
import static com.gamaliev.list.list.ListActivitySharedPreferencesUtils.getDefaultProfile;
import static com.gamaliev.list.list.ListActivitySharedPreferencesUtils.getProfilesSet;
import static com.gamaliev.list.list.ListActivitySharedPreferencesUtils.getSelectedProfileJson;
import static com.gamaliev.list.list.ListActivitySharedPreferencesUtils.getStringFromSp;
import static com.gamaliev.list.list.ListActivitySharedPreferencesUtils.setSelectedProfileId;
import static com.gamaliev.list.list.ListActivitySharedPreferencesUtils.setString;
import static com.gamaliev.list.list.ListActivitySharedPreferencesUtils.updateProfile;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class FilterSortDialogFragment extends DialogFragment {

    /* Logger */
    private static final String TAG = FilterSortDialogFragment.class.getSimpleName();

    /* Extra */
    private static final String EXTRA_PROFILE_MAP = "FilterSortDialogFragment.EXTRA_PROFILE_MAP";
    private static final String EXTRA_SELECTED_ID = "FilterSortDialogFragment.EXTRA_SELECTED_ID";

    @Nullable private View dialog;
    @Nullable private Map<String, String> profileMap;
    @Nullable private String selectedProfileId;
    @Nullable public OnCompleteListener onCompleteListener;


    /*
        Interface
     */

    public interface OnCompleteListener {
        void onComplete(final int code, @Nullable final Object object);
    }

    /*
        Init
     */

    public FilterSortDialogFragment() {}


    /*
        Methods
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (getActivity() instanceof OnCompleteListener) {
            onCompleteListener = (OnCompleteListener) getActivity();
        }
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            Bundle savedInstanceState) {

        // Inflate custom layout
        dialog = inflater.inflate(R.layout.activity_list_filter_dialog, null);

        // Disable title for more space.
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return dialog;
    }

    /*
        On save / restore instance save.
     */

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(EXTRA_PROFILE_MAP, (HashMap) profileMap);
        outState.putString(EXTRA_SELECTED_ID, selectedProfileId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        // Restore local variables, if exists, else init.
        if (savedInstanceState != null) {
            profileMap = (HashMap) savedInstanceState.getSerializable(EXTRA_PROFILE_MAP);
            selectedProfileId = savedInstanceState.getString(EXTRA_SELECTED_ID);
        } else {
            initLocalVariables();
        }

        // Init
        initComponents();

        // Set listeners
        setListeners();

        super.onViewStateRestored(savedInstanceState);
    }


    /*
        Init components
     */

    private void initLocalVariables() {
        setLocalProfileMap();
        setLocalSelectedProfileId();
    }

    private void setLocalProfileMap() {
        profileMap = convertProfileJsonToMap(getSelectedProfileJson(getActivity()));
    }

    private void setLocalSelectedProfileId() {
        this.selectedProfileId =  getStringFromSp(getActivity(), SP_FILTER_PROFILE_SELECTED_ID);
    }

    private void initComponents() {
        initProfilesComponents();
        initSortComponents();
        initDateComponents();
        initColorComponents();

        // Refresh found text.
        refreshFoundText();
    }

    private void setListeners() {
        setAddCurrentProfileListener();
        setSortingListeners();
        setFilterDatesListeners();
        setActionButtonsListeners();
    }

    /**
     * Init profiles components. Set listeners.
     */
    private void initProfilesComponents() {

        // Get profiles viewGroup
        final ViewGroup profiles = (ViewGroup) dialog.findViewById(
                R.id.activity_list_filter_dialog_profiles);

        // Remove all child.
        profiles.removeAllViews();

        // Open database.
        final ListDatabaseHelper dbHelper = new ListDatabaseHelper(getActivity());

        // Get profiles set from shared preferences.
        final Set<String> profilesSet = getProfilesSet(getActivity());

        // Seek.
        for (String profileJson : profilesSet) {

            // Create new child view.
            final View newView = View.inflate(
                    getActivity(),
                    R.layout.activity_list_filter_dialog_profile,
                    null);

            // Convert profile Json to Map.
            final Map<String, String> profileMap =
                    convertProfileJsonToMap(profileJson);

            // Get and set title.
            final String title = profileMap.get(SP_FILTER_TITLE);
            final TextView newTitleView = (TextView) newView.findViewById(
                    R.id.activity_list_filter_sort_dialog_profile_title);
            newTitleView.setText(title);

            // Get "found entries"-text dependent this profile and set.
            // Get entries from database, with given profile-params and count result.
            final Cursor cursor = dbHelper.getCursorWithParams(getActivity(), null, profileMap);
            final String count = String.valueOf(cursor.getCount());
            cursor.close();

            // Set count text.
            final TextView newFoundView = (TextView) newView.findViewById(
                    R.id.activity_list_filter_sort_dialog_profile_found);
            newFoundView.setText("(" + count + ")");

            // Get id and check selected. If id is selectedId, then set contrast color.
            final String id = profileMap.get(SP_FILTER_ID);
            if (selectedProfileId.equals(id)) {
                this.profileMap = profileMap;
                final int color = getResourceColorApi(getActivity(), R.color.color_primary_contrast);
                newTitleView.setTextColor(color);
            }

            // Set layout on click listener.
            // On click change local profile id, and update views.
            newView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedProfileId = profileMap.get(SP_FILTER_ID);
                    // BUG. TODO: ...
                    ((RadioGroup) dialog.findViewById(R.id.activity_list_filter_dialog_sorting_by_radio_group_order))
                            .setOnCheckedChangeListener(null);
                    ((RadioGroup) dialog.findViewById(R.id.activity_list_filter_dialog_sorting_by_radio_group_asc_desc))
                            .setOnCheckedChangeListener(null);

                    // Reinitialize.
                    initComponents();

                    // BUG. TODO: ...
                    setSortingListeners();
                }
            });

            // Set "delete button" on click listener.
            final ImageButton deleteButton = (ImageButton) newView.findViewById(
                    R.id.activity_list_filter_sort_dialog_profile_delete);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Delete profile.
                    deleteProfile(getActivity(), id);

                    // Set selected profile to current.
                    setSelectedProfileId(getActivity(), SP_FILTER_PROFILE_CURRENT_ID);

                    // Set default params to current profile.
                    setString(getActivity(), SP_FILTER_PROFILE_CURRENT, getDefaultProfile());

                    // Reinitialize.
                    initLocalVariables();
                    initComponents();
                }
            });

            // Add child view to parent.
            profiles.addView(newView);
        }

        // Close db.
        dbHelper.close();
    }

    private void initSortComponents() {

        // Sorting.
        switch (profileMap.get(SP_FILTER_ORDER)) {
            case LIST_ITEMS_COLUMN_TITLE:
                ((RadioButton) dialog.findViewById(R.id.activity_list_filter_dialog_order_title))
                        .setChecked(true);
                break;
            case LIST_ITEMS_COLUMN_CREATED:
                ((RadioButton) dialog.findViewById(R.id.activity_list_filter_dialog_order_created))
                        .setChecked(true);
                break;
            case LIST_ITEMS_COLUMN_EDITED:
                ((RadioButton) dialog.findViewById(R.id.activity_list_filter_dialog_order_edited))
                        .setChecked(true);
                break;
            case LIST_ITEMS_COLUMN_VIEWED:
                ((RadioButton) dialog.findViewById(R.id.activity_list_filter_dialog_order_viewed))
                        .setChecked(true);
                break;
            default:
                break;
        }

        // Sorting asc / desc.
        switch (profileMap.get(SP_FILTER_ORDER_ASC)) {
            case ORDER_ASCENDING:
                ((RadioButton) dialog.findViewById(R.id.activity_list_filter_dialog_order_asc))
                        .setChecked(true);
                break;
            case ORDER_DESCENDING:
                ((RadioButton) dialog.findViewById(R.id.activity_list_filter_dialog_order_desc))
                        .setChecked(true);
                break;
            default:
                break;
        }
    }

    private void initDateComponents() {

        // filter created.
        setDatesStatus(
                SP_FILTER_CREATED,
                R.id.activity_list_filter_dialog_filter_by_created_checkbox,
                R.id.activity_list_filter_dialog_filter_by_created_button_from,
                R.id.activity_list_filter_dialog_filter_by_created_button_to);

        // filter edited.
        setDatesStatus(
                SP_FILTER_EDITED,
                R.id.activity_list_filter_dialog_filter_by_edited_checkbox,
                R.id.activity_list_filter_dialog_filter_by_edited_button_from,
                R.id.activity_list_filter_dialog_filter_by_edited_button_to);

        // filter viewed.
        setDatesStatus(
                SP_FILTER_VIEWED,
                R.id.activity_list_filter_dialog_filter_by_viewed_checkbox,
                R.id.activity_list_filter_dialog_filter_by_viewed_button_from,
                R.id.activity_list_filter_dialog_filter_by_viewed_button_to);
    }

    private void initColorComponents() {

        // Open database for retrieving favorite colors.
        final ColorPickerDatabaseHelper dbHelper = new ColorPickerDatabaseHelper(getActivity());

        // Get colors matrix.
        final GridLayout colorsMatrix = (GridLayout) dialog.findViewById(
                R.id.activity_list_filter_dialog_color_by_body);

        // Remove all child.
        colorsMatrix.removeAllViews();

        // Init colorCheckbox default.
        final CheckBox colorCb = (CheckBox) dialog.findViewById(
                R.id.activity_list_filter_dialog_color_by_default);

        // Set off.
        colorCb.setChecked(false);

        // Set on click listener.
        colorCb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (colorCb.isChecked()) {

                    // Set empty value.
                    profileMap.put(SP_FILTER_COLOR, "");

                    // Reinitialize.
                    selectedProfileId = SP_FILTER_PROFILE_CURRENT_ID;
                    initProfilesComponents();
                    initColorComponents();
                    refreshFoundText();
                }
                // Set on.
                colorCb.setChecked(true);
            }});

        // If color is not selected
        if (TextUtils.isEmpty(profileMap.get(SP_FILTER_COLOR))) {
            colorCb.setChecked(true);
        }

        // Get dimension and create params.
        final int s = (int) getResources().getDimension(R.dimen.activity_list_filter_dialog_color_button_size);
        final int m = (int) getResources().getDimension(R.dimen.activity_list_filter_dialog_color_button_margin);
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(s, s);
        params.setMargins(m, m, m, m);

        // Seek all favorite colors.
        for (int i = 0; i < ColorPickerDatabaseHelper.FAVORITE_COLORS_DEFAULT.length; i++) {
            // Create new button
            final ImageButton button = new ImageButton(getActivity());

            // Set params.
            button.setLayoutParams(params);

            // Set oval drawable
            Drawable oval;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                oval = getResources().getDrawable(R.drawable.btn_oval, null);
            } else {
                oval = getResources().getDrawable(R.drawable.btn_oval);
            }
            button.setBackground(oval);

            // Get favorite color from database, and set.
            final int color = dbHelper.getFavoriteColor(i);
            CommonUtils.setBackgroundColor(button, color);

            // If color is selected, then set done icon.
            final String colorFromSettings = profileMap.get(SP_FILTER_COLOR);

            if (!TextUtils.isEmpty(colorFromSettings)
                    && Integer.parseInt(colorFromSettings) == color) {

                button.setImageResource(R.drawable.ic_done_white_24dp);
            }

            // Set on click listener
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Set off.
                    colorCb.setChecked(false);

                    // Set new value
                    profileMap.put(SP_FILTER_COLOR, String.valueOf(color));

                    // Reinitialize.
                    selectedProfileId = SP_FILTER_PROFILE_CURRENT_ID;
                    initProfilesComponents();
                    initColorComponents();
                    refreshFoundText();
                }
            });

            // Add button to colors matrix.
            colorsMatrix.addView(button);
        }

        // Database close.
        dbHelper.close();
    }

    private void setDatesStatus(
            @NonNull final String filterCategory,
            final int checkboxId,
            final int dateFromId,
            final int dateToId) {

        // Init.
        Button button;

        if (!TextUtils.isEmpty(getDateFromProfileMap(
                getActivity(),
                profileMap,
                filterCategory,
                EXTRA_DATES_BOTH))) {

            // Set checkbox status on.
            ((CheckBox) dialog.findViewById(checkboxId))
                    .setChecked(true);

            // Date_from button on, and set date.
            button = (Button) dialog.findViewById(dateFromId);
            final String localtimeDateFrom = getDateFromProfileMap(
                    getActivity(),
                    profileMap,
                    filterCategory,
                    EXTRA_DATES_FROM_DATE_UTC_TO_LOCALTIME);
            setDateButtonOn(button, localtimeDateFrom);

            // Date_to button on, and set date.
            button = (Button) dialog.findViewById(dateToId);
            final String localtimeDateTo = getDateFromProfileMap(
                    getActivity(),
                    profileMap,
                    filterCategory,
                    EXTRA_DATES_TO_DATE_UTC_TO_LOCALTIME);
            setDateButtonOn(button, localtimeDateTo);

        } else {

            // Set checkbox status on.
            ((CheckBox) dialog.findViewById(checkboxId))
                    .setChecked(false);

            // Date_from button off.
            button = (Button) dialog.findViewById(dateFromId);
            setDateButtonOff(button);

            // Date_to button off.
            button = (Button) dialog.findViewById(dateToId);
            setDateButtonOff(button);
        }
    }

    private void setDateButtonOn(
            @NonNull final Button button,
            @NonNull final String text) {

        // Get alpha on.
        final float alpha = getResources()
                .getFraction(R.fraction.activity_item_details_delete_dialog_date_alpha_on, 1, 1);

        // Change text, clickable, alpha.
        button.setText(text);
        button.setClickable(true);
        button.setAlpha(alpha);
    }

    private void setDateButtonOff(@NonNull final Button button) {
        // Get default string.
        final String defaultText = getString(R.string.activity_item_details_delete_dialog_date_text_default);

        // Get alpha off.
        final float alpha = getResources()
                .getFraction(R.fraction.activity_item_details_delete_dialog_date_alpha_off, 1, 1);

        // Change text, clickable, alpha.
        button.setText(defaultText);
        button.setClickable(false);
        button.setAlpha(alpha);
    }


    /*
        Listeners
     */

    /**
     * Show input text dialog, if confirm, then add new profile, and reinitialize.
     */
    private void setAddCurrentProfileListener() {
        dialog.findViewById(R.id.activity_list_filter_dialog_profiles_save_current_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Show input text dialog, if confirm, then add new profile, and reinitialize.
                        showInputDialog();
                    }
                });
    }

    private void setSortingListeners() {
        // Order listener. On click: change profileMap value.
        ((RadioGroup) dialog.findViewById(R.id.activity_list_filter_dialog_sorting_by_radio_group_order))
                .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                        switch (checkedId) {
                            // Title order
                            case R.id.activity_list_filter_dialog_order_title:
                                profileMap.put(SP_FILTER_ORDER, LIST_ITEMS_COLUMN_TITLE);
                                break;
                            // Created order
                            case R.id.activity_list_filter_dialog_order_created:
                                profileMap.put(SP_FILTER_ORDER, LIST_ITEMS_COLUMN_CREATED);
                                break;
                            // Edited order
                            case R.id.activity_list_filter_dialog_order_edited:
                                profileMap.put(SP_FILTER_ORDER, LIST_ITEMS_COLUMN_EDITED);
                                break;
                            // Viewed order
                            case R.id.activity_list_filter_dialog_order_viewed:
                                profileMap.put(SP_FILTER_ORDER, LIST_ITEMS_COLUMN_VIEWED);
                                break;
                            default:
                                break;
                        }

                        // Reinitialize.
                        // Refresh found text.
                        selectedProfileId = SP_FILTER_PROFILE_CURRENT_ID;
                        initProfilesComponents();
                        refreshFoundText();
                    }
                });

        // Order_asc_desc listener. On click: change profileMap value.
        ((RadioGroup) dialog.findViewById(R.id.activity_list_filter_dialog_sorting_by_radio_group_asc_desc))
                .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                        switch (checkedId) {
                            // ASC order
                            case R.id.activity_list_filter_dialog_order_asc:
                                profileMap.put(SP_FILTER_ORDER_ASC, ORDER_ASCENDING);
                                break;
                            // DESC order
                            case R.id.activity_list_filter_dialog_order_desc:
                                profileMap.put(SP_FILTER_ORDER_ASC, ORDER_DESCENDING);
                                break;
                            default:
                                break;
                        }

                        // Reinitialize.
                        // Refresh found text.
                        selectedProfileId = SP_FILTER_PROFILE_CURRENT_ID;
                        initProfilesComponents();
                        refreshFoundText();
                    }
                });
    }

    private void setFilterDatesListeners() {
        
        // Created
        setFilterDateListeners(
                SP_FILTER_CREATED,
                R.id.activity_list_filter_dialog_filter_by_created_checkbox,
                R.id.activity_list_filter_dialog_filter_by_created_button_from,
                R.id.activity_list_filter_dialog_filter_by_created_button_to);
        
        // Edited
        setFilterDateListeners(
                SP_FILTER_EDITED,
                R.id.activity_list_filter_dialog_filter_by_edited_checkbox,
                R.id.activity_list_filter_dialog_filter_by_edited_button_from,
                R.id.activity_list_filter_dialog_filter_by_edited_button_to);
        
        // Viewed
        setFilterDateListeners(
                SP_FILTER_VIEWED,
                R.id.activity_list_filter_dialog_filter_by_viewed_checkbox,
                R.id.activity_list_filter_dialog_filter_by_viewed_button_from,
                R.id.activity_list_filter_dialog_filter_by_viewed_button_to);
        
    }
    
    private void setFilterDateListeners(
            @NonNull final String filterCategory,
            final int checkBoxId, 
            final int dateFromId, 
            final int dateToId) {

        // Find views
        final CheckBox checkBox = (CheckBox) dialog.findViewById(checkBoxId);
        final Button dateFrom   = (Button) dialog.findViewById(dateFromId);
        final Button dateTo     = (Button) dialog.findViewById(dateToId);

        // Set checkbox listeners
        checkBox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (checkBox.isChecked()) {

                            // On buttons
                            setDateButtonOn(
                                    dateFrom,
                                    getString(R.string.activity_item_details_delete_dialog_date_text_from));
                            setDateButtonOn(
                                    dateTo,
                                    getString(R.string.activity_item_details_delete_dialog_date_text_to));

                        } else {
                            // Off buttons.
                            setDateButtonOff(dateFrom);
                            setDateButtonOff(dateTo);

                            // Update profileMap.
                            profileMap.put(filterCategory, "");
                        }

                        // Reinitialize.
                        // Refresh found text.
                        selectedProfileId = SP_FILTER_PROFILE_CURRENT_ID;
                        initProfilesComponents();
                        refreshFoundText();
                    }
                });

        // Set date from button listener
        dateFrom.setOnClickListener(getFilterDateListener(filterCategory, true));

        // Set date to button listener
        dateTo.setOnClickListener(getFilterDateListener(filterCategory, false));
    }

    @NonNull
    private View.OnClickListener getFilterDateListener(
            @NonNull final String filterCategory,
            final boolean isDateFrom) {

        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Init.
                int year;
                int month;
                int dayOfMonth;

                // Check on empty or null.
                if (!TextUtils.isEmpty(getDateFromProfileMap(
                        getActivity(),
                        profileMap,
                        filterCategory,
                        EXTRA_DATES_BOTH))) {

                    if (isDateFrom) {
                        // If "date_from".
                        final String[] fromDateArray = getDateFromProfileMap(
                                getActivity(),
                                profileMap,
                                filterCategory,
                                EXTRA_DATES_FROM_DATE_UTC_TO_LOCALTIME)
                                .split("-");

                        year        = Integer.parseInt(fromDateArray[0]);
                        month       = Integer.parseInt(fromDateArray[1]);
                        dayOfMonth  = Integer.parseInt(fromDateArray[2]);

                    } else {
                        // If "date_to".
                        final String[] toDateArray = getDateFromProfileMap(
                                getActivity(),
                                profileMap,
                                filterCategory,
                                EXTRA_DATES_TO_DATE_UTC_TO_LOCALTIME)
                                .split("-");

                        year        = Integer.parseInt(toDateArray[0]);
                        month       = Integer.parseInt(toDateArray[1]);
                        dayOfMonth  = Integer.parseInt(toDateArray[2]);
                    }

                    // Here months 0-11;
                    if (month == 0) {
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

                // Create date picker dialog
                final DatePickerDialog datePicker = new DatePickerDialog(
                        getActivity(),
                        getDatePickerListener(filterCategory, isDateFrom),
                        year,
                        month,
                        dayOfMonth);

                // Show.
                datePicker.show();
            }
        };
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

                    // Get date string format.
                    final Calendar date = Calendar.getInstance();
                    date.set(year, month, dayOfMonth, 0, 0, 0);
                    final String resultDate =
                            getStringDateFormatSqlite(getActivity(), date.getTime(), false);

                    // Update profileMap values.
                    updateDateSettings(filterCategory, resultDate, isDateFrom);

                    // Reinitialize.
                    initComponents();
                }
            }
        };
    }

    private void updateDateSettings(
            @NonNull final String filterCategory,
            @NonNull final String resultLocaltimeDateS,
            final boolean isDateFrom) {

        // New dates
        final String resultUtcDateS = convertLocalToUtc(getActivity(), resultLocaltimeDateS);
        String newUtcDateFromS = null;
        String newUtcDateToS = null;

        // Get date format
        final DateFormat dateFormatUtc = CommonUtils.getDateFormatSqlite(getActivity(), true);

        // Parse result date;
        Date resultUtcDateD = null;
        try {
            resultUtcDateD = dateFormatUtc.parse(resultLocaltimeDateS);
        } catch (ParseException e) {
            Log.e(TAG, e.toString());
        }

        // Check null or empty.
        if (!TextUtils.isEmpty(getDateFromProfileMap(
                getActivity(),
                profileMap,
                filterCategory,
                EXTRA_DATES_BOTH))) {

            try {

                // Parse "date_from".
                final Date utcDateFrom = dateFormatUtc.parse(
                        getDateFromProfileMap(
                                getActivity(),
                                profileMap,
                                filterCategory,
                                EXTRA_DATES_FROM_DATETIME));

                // Parse "date_to".
                final Date utcDateTo = dateFormatUtc.parse(
                        getDateFromProfileMap(
                                getActivity(),
                                profileMap,
                                filterCategory,
                                EXTRA_DATES_TO_DATETIME));

                // Check.
                if (isDateFrom) {

                    // Set new date;
                    newUtcDateFromS = resultUtcDateS;

                    // If new "from date" before "to date", then "to date" hold as is,
                    // else change to new.
                    newUtcDateToS = resultUtcDateD.before(utcDateTo)
                            ? getDateFromProfileMap(
                                    getActivity(),
                            profileMap,
                                    filterCategory,
                                    EXTRA_DATES_TO_DATETIME)
                            : resultUtcDateS;

                } else {
                    // If new "to date" after "from date", then "from date" hold as is,
                    // else change to new.
                    newUtcDateFromS = resultUtcDateD.after(utcDateFrom)
                            ? getDateFromProfileMap(
                                    getActivity(),
                            profileMap,
                                    filterCategory,
                                    EXTRA_DATES_FROM_DATETIME)
                            : resultUtcDateS;

                    // Set new date;
                    newUtcDateToS = resultUtcDateS;
                }

            } catch (ParseException e) {
                Log.e(TAG, e.toString());
            }

        } else {

            // If profileMap not contains dates - Create new pair.
            newUtcDateFromS = resultUtcDateS;
            newUtcDateToS = resultUtcDateS;
        }

        // Update dates.
        profileMap.put(filterCategory, newUtcDateFromS + SP_FILTER_SYMBOL_DATE_SPLIT + newUtcDateToS);
    }

    private void setActionButtonsListeners() {

        // Cancel button. Dismiss on click.
        dialog.findViewById(R.id.activity_list_filter_dialog_action_button_cancel)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();
                    }
                });

        // Filter button. Save changes
        dialog.findViewById(R.id.activity_list_filter_dialog_action_button_filter)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();
                        refreshListActivity(); // TODO: replace by Resume lifecycle?!
                    }
                });
    }

    private void refreshListActivity() {
        if (onCompleteListener != null) {

            // Refresh selected profile id.
            setSelectedProfileId(getActivity(), selectedProfileId);

            // If current id, then update.
            if (SP_FILTER_PROFILE_CURRENT_ID.equals(selectedProfileId)) {
                setString(
                        getActivity(),
                        SP_FILTER_PROFILE_CURRENT,
                        convertProfileMapToJson(profileMap));
            }

            // Return result.
            onCompleteListener.onComplete(
                    ListActivity.REQUEST_CODE_DIALOG_FRAGMENT_RETURN_PROFILE,
                    profileMap);
        }
    }

    /**
     * Get from database count of entries, with given profile params.
     */
    private void refreshFoundText() {
        final ListDatabaseHelper dbHelper = new ListDatabaseHelper(getActivity());
        final Cursor cursor = dbHelper.getCursorWithParams(getActivity(), null, profileMap);
        final String text = getString(R.string.activity_list_filter_dialog_profile_found_text);
        ((TextView) dialog
                .findViewById(R.id.activity_list_filter_dialog_found_text_view))
                .setText(text + " " + String.valueOf(cursor.getCount()));
        cursor.close();
        dbHelper.close();
    }

    /**
     * Show input text dialog, if confirm, then add new profile, and reinitialize.
     */
    private void showInputDialog() {
        // Create builder.
        final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        // Create edit text.
        final EditText editText = new EditText(getActivity());

        // Get string.
        final String title = getString(R.string.activity_list_filter_dialog_profile_input_text_title);
        final String save = getString(R.string.activity_list_filter_dialog_profile_input_text_save_title);
        final String cancel = getString(R.string.activity_list_filter_dialog_profile_input_text_cancel_title);

        // Set title.
        alert.setTitle(title);

        // Create container, set margin, add exitText.
        final FrameLayout container = new FrameLayout(getActivity());
        final FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        int m = getResources().getDimensionPixelSize(R.dimen.activity_list_filter_dialog_input_title);
        params.setMargins(m, 0, m, 0);
        editText.setLayoutParams(params);
        container.addView(editText);

        // Set view.
        alert.setView(container);

        // Set save button listener.
        alert.setPositiveButton(save, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String enteredText = editText.getText().toString();

                // Add to profiles, get newId, set selected newId.
                profileMap.put(SP_FILTER_ID, "");
                profileMap.put(SP_FILTER_TITLE, enteredText);
                String newId = updateProfile(getActivity(), profileMap);
                setSelectedProfileId(getActivity(), newId);

                // Reinitialize
                initLocalVariables();
                initComponents();
            }
        });

        // Set cancel button listener.
        alert.setNegativeButton(cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {}
        });

        // Show.
        alert.show();
    }
}
