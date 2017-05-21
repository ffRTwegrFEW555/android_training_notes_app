package com.gamaliev.notes.list;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayout;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.UnderlineSpan;
import android.util.DisplayMetrics;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gamaliev.notes.R;
import com.gamaliev.notes.colorpicker.db.ColorPickerDbHelper;
import com.gamaliev.notes.common.CommonUtils;
import com.gamaliev.notes.common.OnCompleteListener;
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

import static com.gamaliev.notes.common.CommonUtils.EXTRA_DATES_BOTH;
import static com.gamaliev.notes.common.CommonUtils.EXTRA_DATES_FROM_DATETIME;
import static com.gamaliev.notes.common.CommonUtils.EXTRA_DATES_FROM_DATE_UTC_TO_LOCALTIME;
import static com.gamaliev.notes.common.CommonUtils.EXTRA_DATES_TO_DATETIME;
import static com.gamaliev.notes.common.CommonUtils.EXTRA_DATES_TO_DATE_UTC_TO_LOCALTIME;
import static com.gamaliev.notes.common.CommonUtils.EXTRA_REVEAL_ANIM_CENTER_TOP_END;
import static com.gamaliev.notes.common.CommonUtils.convertLocalToUtc;
import static com.gamaliev.notes.common.CommonUtils.getDateFromProfileMap;
import static com.gamaliev.notes.common.CommonUtils.getResourceColorApi;
import static com.gamaliev.notes.common.CommonUtils.getStringDateFormatSqlite;
import static com.gamaliev.notes.common.DialogFragmentUtils.initCircularRevealAnimation;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_CREATED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_EDITED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_TITLE;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_VIEWED;
import static com.gamaliev.notes.common.db.DbHelper.ORDER_ASCENDING;
import static com.gamaliev.notes.common.db.DbHelper.ORDER_DESCENDING;
import static com.gamaliev.notes.common.shared_prefs.SpCommon.convertJsonToMap;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_COLOR;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_CREATED;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_EDITED;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_ID;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_ORDER;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_ORDER_ASC;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_PROFILE_CURRENT_ID;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_SYMBOL_DATE_SPLIT;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_TITLE;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_VIEWED;
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

public final class FilterSortDialogFragment extends DialogFragment {

    /* Logger */
    private static final String TAG = FilterSortDialogFragment.class.getSimpleName();

    /* Extra */
    private static final String EXTRA_PROFILE_MAP = "FilterSortDialogFragment.EXTRA_PROFILE_MAP";
    private static final String EXTRA_FOUNDED_MAP = "FilterSortDialogFragment.EXTRA_FOUNDED_MAP";
    private static final String EXTRA_SELECTED_ID = "FilterSortDialogFragment.EXTRA_SELECTED_ID";

    /* ... */
    @NonNull private View mDialog;
    @NonNull private Map<String, String> mFilterProfileMap;
    @NonNull private Map<String, String> mFoundedEntriesCache;
    @NonNull private String mSelectedFilterProfile;
    @NonNull private ExecutorService mSingleThreadExecutor;
    @NonNull private Set<Runnable> mRunnableTasks;
    @Nullable private OnCompleteListener mOnCompleteListener;


    /*
        Init
     */

    public FilterSortDialogFragment() {}


    /*
        Lifecycle
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (getActivity() instanceof OnCompleteListener) {
            mOnCompleteListener = (OnCompleteListener) getActivity();
        }
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            Bundle savedInstanceState) {

        mDialog = inflater.inflate(R.layout.activity_list_filter_dialog, null);
        initCircularRevealAnimation(
                mDialog,
                true,
                EXTRA_REVEAL_ANIM_CENTER_TOP_END);

        // Disable title for more space.
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return mDialog;
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        // Restore local variables, if exists, else init.
        if (savedInstanceState != null) {
            mFilterProfileMap       = (HashMap) savedInstanceState.getSerializable(EXTRA_PROFILE_MAP);
            mFoundedEntriesCache    = (HashMap) savedInstanceState.getSerializable(EXTRA_FOUNDED_MAP);
            mSelectedFilterProfile  = savedInstanceState.getString(EXTRA_SELECTED_ID);
        } else {
            initLocalVariables();
        }

        //
        mSingleThreadExecutor = Executors.newSingleThreadExecutor();
        mRunnableTasks = new HashSet<>();

        // Init
        initComponents();

        // Set listeners
        setListeners();

        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onResume() {
        // Set max size of dialog. ( XML is not work :/ )
        final DisplayMetrics displayMetrics = getActivity().getResources().getDisplayMetrics();
        final ViewGroup.LayoutParams params = getDialog().getWindow().getAttributes();
        params.width = Math.min(
                displayMetrics.widthPixels,
                getActivity().getResources().getDimensionPixelSize(
                        R.dimen.activity_list_filter_dialog_max_width));
        params.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
        getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(EXTRA_PROFILE_MAP, (HashMap) mFilterProfileMap);
        outState.putSerializable(EXTRA_FOUNDED_MAP, (HashMap) mFoundedEntriesCache);
        outState.putString(EXTRA_SELECTED_ID, mSelectedFilterProfile);
        super.onSaveInstanceState(outState);
    }


    /*
        Init components
     */

    private void initLocalVariables() {
        initFilterProfile();
        initSelectedIdFilterProfile();
        mFoundedEntriesCache = new HashMap<>();
    }

    private void initFilterProfile() {
        mFilterProfileMap = convertJsonToMap(getSelectedForCurrentUser(getActivity()));
    }

    private void initSelectedIdFilterProfile() {
        mSelectedFilterProfile = getSelectedIdForCurrentUser(getActivity());
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
        setResetProfileListener();
        setAddCurrentProfileListener();
        setSortingListeners();
        setFilterDatesListeners();
        setActionButtonsListeners();
        setOnAttachStateChangeListener();
    }

    /**
     * Init profiles components. Set listeners.
     */
    private void initProfilesComponents() {

        // Get profiles viewGroup
        final ViewGroup profiles = (ViewGroup) mDialog.findViewById(
                R.id.activity_list_filter_dialog_profiles);

        // Remove all child.
        profiles.removeAllViews();

        // Get profiles set from shared preferences.
        final Set<String> profilesSet = getProfilesForCurrentUser(getActivity());

        // Seek.
        for (String profileJson : profilesSet) {

            // Create new child view.
            final View newView = View.inflate(
                    getActivity(),
                    R.layout.activity_list_filter_dialog_profile,
                    null);

            // Convert profile Json to Map.
            final Map<String, String> profileMap =
                    convertJsonToMap(profileJson);

            // Get id
            final String id = profileMap.get(SP_FILTER_ID);

            // Title. Get and set.
            final String title = profileMap.get(SP_FILTER_TITLE);
            final TextView newTitleView = (TextView) newView.findViewById(
                    R.id.activity_list_filter_dialog_profile_title);
            newTitleView.setText(title);

            // Found text view.
            final TextView foundTextView = (TextView) newView.findViewById(
                    R.id.activity_list_filter_dialog_profile_found);

            // Progress bar.
            final View progressBar = newView.findViewById(
                    R.id.activity_list_filter_dialog_profile_found_progress_bar);

            // Check cached
            final String foundedEntries = mFoundedEntriesCache.get(id);
            if (foundedEntries == null) {
                // If not exists.
                // Get "found entries"-text dependent this profile and set.
                // Get entries from database, with given profile-params and count result.

                // Hide found text view.
                foundTextView.setVisibility(View.GONE);

                // Show progress bar.
                progressBar.setVisibility(View.VISIBLE);

                // Background task.
                mRunnableTasks.add(new Runnable() {
                    @Override
                    public void run() {

                        // Update views, if attached.
                        // Double check. Bug.
                        if (getActivity() != null && mDialog.isAttachedToWindow()) {

                            // Get number of entries.
                            final long count = ListDbHelper.getEntriesCount(
                                    getActivity(),
                                    ListDbHelper.convertToQueryBuilder(
                                            getActivity(),
                                            null,
                                            profileMap));

                            // Caching
                            mFoundedEntriesCache.put(id, String.valueOf(count));

                            // Update view
                            if (getActivity() != null && mDialog.isAttachedToWindow()) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // Set text.
                                        foundTextView.setText("(" + count + ")");

                                        // Hide progress bar.
                                        progressBar.setVisibility(View.GONE);

                                        // Show found text view.
                                        foundTextView.setVisibility(View.VISIBLE);
                                    }
                                });
                            }
                        }
                    }
                });

            } else {
                // If exists.
                foundTextView.setText("(" + foundedEntries + ")");

                // Show found text view.
                foundTextView.setVisibility(View.VISIBLE);
            }

            // Color. Get id and check selected.
            // If id is selectedId, then set contrast color.
            if (mSelectedFilterProfile.equals(id)) {
                mFilterProfileMap = profileMap;
                final int color = getResourceColorApi(getActivity(), R.color.color_primary_contrast);
                newTitleView.setTextColor(color);

                // Also add underline to title.
                SpannableString titleUnderline = new SpannableString(title);
                titleUnderline.setSpan(new UnderlineSpan(), 0, titleUnderline.length(), 0);
                newTitleView.setText(titleUnderline);
            }

            // Set layout on click listener.
            // On click change local profile id, and update views.
            newView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    mSelectedFilterProfile = profileMap.get(SP_FILTER_ID);

                    // BUG.
                    ((RadioGroup) mDialog.findViewById(R.id.activity_list_filter_dialog_sorting_by_radio_group_order))
                            .setOnCheckedChangeListener(null);
                    ((RadioGroup) mDialog.findViewById(R.id.activity_list_filter_dialog_sorting_by_radio_group_asc_desc))
                            .setOnCheckedChangeListener(null);

                    // Reinitialize.
                    initComponents();

                    // BUG.
                    setSortingListeners();
                }
            });

            // Set "delete button" on click listener.
            final ImageButton deleteButton = (ImageButton) newView.findViewById(
                    R.id.activity_list_filter_dialog_profile_delete);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    // Delete profile.
                    deleteForCurrentUser(getActivity(), id);

                    // If deleted profile equals selected, then set default settings.
                    if (mSelectedFilterProfile.equals(id)) {
                        // Reinitialize.
                        initLocalVariables();
                        initComponents();

                    } else {
                        // Reinitialize profiles block.
                        initProfilesComponents();
                    }
                }
            });

            // Add child view to parent.
            profiles.addView(newView);
        }
    }

    private void initSortComponents() {

        // Sorting.
        switch (mFilterProfileMap.get(SP_FILTER_ORDER)) {
            case LIST_ITEMS_COLUMN_TITLE:
                ((RadioButton) mDialog.findViewById(R.id.activity_list_filter_dialog_order_title))
                        .setChecked(true);
                break;
            case LIST_ITEMS_COLUMN_CREATED:
                ((RadioButton) mDialog.findViewById(R.id.activity_list_filter_dialog_order_created))
                        .setChecked(true);
                break;
            case LIST_ITEMS_COLUMN_EDITED:
                ((RadioButton) mDialog.findViewById(R.id.activity_list_filter_dialog_order_edited))
                        .setChecked(true);
                break;
            case LIST_ITEMS_COLUMN_VIEWED:
                ((RadioButton) mDialog.findViewById(R.id.activity_list_filter_dialog_order_viewed))
                        .setChecked(true);
                break;
            default:
                break;
        }

        // Sorting asc / desc.
        switch (mFilterProfileMap.get(SP_FILTER_ORDER_ASC)) {
            case ORDER_ASCENDING:
                ((RadioButton) mDialog.findViewById(R.id.activity_list_filter_dialog_order_asc))
                        .setChecked(true);
                break;
            case ORDER_DESCENDING:
                ((RadioButton) mDialog.findViewById(R.id.activity_list_filter_dialog_order_desc))
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

        // Get colors matrix.
        final GridLayout colorsMatrix = (GridLayout) mDialog.findViewById(
                R.id.activity_list_filter_dialog_color_by_body);

        // Remove all child.
        colorsMatrix.removeAllViews();

        // Init colorCheckbox default.
        final CheckBox colorCb = (CheckBox) mDialog.findViewById(
                R.id.activity_list_filter_dialog_color_by_default);

        // Set off.
        colorCb.setChecked(false);

        // Set on click listener.
        colorCb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (colorCb.isChecked()) {

                    // Set empty value.
                    mFilterProfileMap.put(SP_FILTER_COLOR, "");

                    // Reinitialize.
                    mSelectedFilterProfile = SP_FILTER_PROFILE_CURRENT_ID;
                    initComponents();
                }
                // Set on.
                colorCb.setChecked(true);
            }});

        // If color is not selected
        if (TextUtils.isEmpty(mFilterProfileMap.get(SP_FILTER_COLOR))) {
            colorCb.setChecked(true);
        }

        // Get dimension and create params.
        final int s = (int) getResources().getDimension(R.dimen.activity_list_filter_dialog_color_button_size);
        final int m = (int) getResources().getDimension(R.dimen.activity_list_filter_dialog_color_button_margin);
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(s, s);
        params.setMargins(m, m, m, m);

        // Seek all favorite colors.
        for (int i = 0; i < ColorPickerDbHelper.FAVORITE_COLORS_DEFAULT.length; i++) {
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
            final int color = ColorPickerDbHelper.getFavoriteColor(getActivity(), i);
            CommonUtils.setBackgroundColor(button, color);

            // If color is selected, then set done icon.
            final String colorFromSettings = mFilterProfileMap.get(SP_FILTER_COLOR);

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
                    mFilterProfileMap.put(SP_FILTER_COLOR, String.valueOf(color));

                    // Reinitialize.
                    mSelectedFilterProfile = SP_FILTER_PROFILE_CURRENT_ID;
                    initComponents();
                }
            });

            // Add button to colors matrix.
            colorsMatrix.addView(button);
        }
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
                mFilterProfileMap,
                filterCategory,
                EXTRA_DATES_BOTH))) {

            // Set checkbox status on.
            ((CheckBox) mDialog.findViewById(checkboxId))
                    .setChecked(true);

            // Date_from button on, and set date.
            button = (Button) mDialog.findViewById(dateFromId);
            final String localtimeDateFrom = getDateFromProfileMap(
                    getActivity(),
                    mFilterProfileMap,
                    filterCategory,
                    EXTRA_DATES_FROM_DATE_UTC_TO_LOCALTIME);
            setDateButtonOn(button, localtimeDateFrom);

            // Date_to button on, and set date.
            button = (Button) mDialog.findViewById(dateToId);
            final String localtimeDateTo = getDateFromProfileMap(
                    getActivity(),
                    mFilterProfileMap,
                    filterCategory,
                    EXTRA_DATES_TO_DATE_UTC_TO_LOCALTIME);
            setDateButtonOn(button, localtimeDateTo);

        } else {

            // Set checkbox status on.
            ((CheckBox) mDialog.findViewById(checkboxId))
                    .setChecked(false);

            // Date_from button off.
            button = (Button) mDialog.findViewById(dateFromId);
            setDateButtonOff(button);

            // Date_to button off.
            button = (Button) mDialog.findViewById(dateToId);
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
     * Reset to default profile.
     */
    private void setResetProfileListener() {
        mDialog.findViewById(R.id.activity_list_filter_dialog_profile_reset)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        // Set default profile id.
                        mSelectedFilterProfile = SP_FILTER_PROFILE_CURRENT_ID;

                        // Set default profile map.
                        mFilterProfileMap = convertJsonToMap(getDefaultProfile());

                        // Reinitialize.
                        initComponents();
                    }
                });
    }

    /**
     * Show input text dialog, if confirm, then add new profile, and reinitialize.
     */
    private void setAddCurrentProfileListener() {
        mDialog.findViewById(R.id.activity_list_filter_dialog_profiles_save_current_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        // Show input text dialog, if confirm, then add new profile, and reinitialize.
                        showInputDialogAddProfile();
                    }
                });
    }

    private void setSortingListeners() {
        // Order listener. On click: change profileMap value.
        ((RadioGroup) mDialog.findViewById(R.id.activity_list_filter_dialog_sorting_by_radio_group_order))
                .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                        switch (checkedId) {
                            // Title order
                            case R.id.activity_list_filter_dialog_order_title:
                                mFilterProfileMap.put(SP_FILTER_ORDER, LIST_ITEMS_COLUMN_TITLE);
                                break;
                            // Created order
                            case R.id.activity_list_filter_dialog_order_created:
                                mFilterProfileMap.put(SP_FILTER_ORDER, LIST_ITEMS_COLUMN_CREATED);
                                break;
                            // Edited order
                            case R.id.activity_list_filter_dialog_order_edited:
                                mFilterProfileMap.put(SP_FILTER_ORDER, LIST_ITEMS_COLUMN_EDITED);
                                break;
                            // Viewed order
                            case R.id.activity_list_filter_dialog_order_viewed:
                                mFilterProfileMap.put(SP_FILTER_ORDER, LIST_ITEMS_COLUMN_VIEWED);
                                break;
                            default:
                                break;
                        }

                        // Reinitialize.
                        mSelectedFilterProfile = SP_FILTER_PROFILE_CURRENT_ID;
                        initProfilesComponents();
                    }
                });

        // Order_asc_desc listener. On click: change profileMap value.
        ((RadioGroup) mDialog.findViewById(R.id.activity_list_filter_dialog_sorting_by_radio_group_asc_desc))
                .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                        switch (checkedId) {
                            // ASC order
                            case R.id.activity_list_filter_dialog_order_asc:
                                mFilterProfileMap.put(SP_FILTER_ORDER_ASC, ORDER_ASCENDING);
                                break;
                            // DESC order
                            case R.id.activity_list_filter_dialog_order_desc:
                                mFilterProfileMap.put(SP_FILTER_ORDER_ASC, ORDER_DESCENDING);
                                break;
                            default:
                                break;
                        }

                        // Reinitialize.
                        mSelectedFilterProfile = SP_FILTER_PROFILE_CURRENT_ID;
                        initProfilesComponents();
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
        final CheckBox checkBox = (CheckBox) mDialog.findViewById(checkBoxId);
        final Button dateFrom   = (Button) mDialog.findViewById(dateFromId);
        final Button dateTo     = (Button) mDialog.findViewById(dateToId);

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
                            mFilterProfileMap.put(filterCategory, "");
                        }

                        // Reinitialize.
                        mSelectedFilterProfile = SP_FILTER_PROFILE_CURRENT_ID;
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
                        mFilterProfileMap,
                        filterCategory,
                        EXTRA_DATES_BOTH))) {

                    if (isDateFrom) {
                        // If "date_from".
                        final String[] fromDateArray = getDateFromProfileMap(
                                getActivity(),
                                mFilterProfileMap,
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
                                mFilterProfileMap,
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
                    mSelectedFilterProfile = SP_FILTER_PROFILE_CURRENT_ID;
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
                mFilterProfileMap,
                filterCategory,
                EXTRA_DATES_BOTH))) {

            try {

                // Parse "date_from".
                final Date utcDateFrom = dateFormatUtc.parse(
                        getDateFromProfileMap(
                                getActivity(),
                                mFilterProfileMap,
                                filterCategory,
                                EXTRA_DATES_FROM_DATETIME));

                // Parse "date_to".
                final Date utcDateTo = dateFormatUtc.parse(
                        getDateFromProfileMap(
                                getActivity(),
                                mFilterProfileMap,
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
                            mFilterProfileMap,
                                    filterCategory,
                                    EXTRA_DATES_TO_DATETIME)
                            : resultUtcDateS;

                } else {
                    // If new "to date" after "from date", then "from date" hold as is,
                    // else change to new.
                    newUtcDateFromS = resultUtcDateD.after(utcDateFrom)
                            ? getDateFromProfileMap(
                                    getActivity(),
                            mFilterProfileMap,
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
        mFilterProfileMap.put(filterCategory, newUtcDateFromS + SP_FILTER_SYMBOL_DATE_SPLIT + newUtcDateToS);
    }

    private void setActionButtonsListeners() {

        // Cancel button. Dismiss on click.
        mDialog.findViewById(R.id.activity_list_filter_dialog_action_button_cancel)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSingleThreadExecutor.shutdownNow();
                        dismiss();
                    }
                });

        // Filter button. Save changes
        mDialog.findViewById(R.id.activity_list_filter_dialog_action_button_filter)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSingleThreadExecutor.shutdownNow();
                        dismiss();
                        refreshListActivity();
                    }
                });
    }

    private void setOnAttachStateChangeListener() {
        mDialog.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                startRunnableTasks();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                mSingleThreadExecutor.shutdownNow();
            }
        });
    }

    private void refreshListActivity() {
        if (mOnCompleteListener != null) {

            // Refresh selected profile id.
            setSelectedForCurrentUser(getActivity(), mSelectedFilterProfile);

            // If current id, then update current profile.
            if (SP_FILTER_PROFILE_CURRENT_ID.equals(mSelectedFilterProfile)) {
                updateCurrentForCurrentUser(getActivity(), mFilterProfileMap);
            }

            // Notify activity.
            mOnCompleteListener.onComplete(
                    ListActivity.RESULT_CODE_FILTER_DIALOG);
        }
    }

    /**
     * Get from database count of entries, with given profile params.
     */
    private void refreshFoundText() {

        // Found text view.
        final TextView foundTextView = (TextView) mDialog
                .findViewById(R.id.activity_list_filter_dialog_found_text_view);

        // Progress bar.
        final View progressBar = mDialog.findViewById(
                R.id.activity_list_filter_dialog_found_progress_bar);

        // Check cache, if exist, then set, else get from db in async task.
        if (mSelectedFilterProfile.equals(mFilterProfileMap.get(SP_FILTER_ID))
                && mFoundedEntriesCache.get(mSelectedFilterProfile) != null) {

            // Hide progress bar.
            progressBar.setVisibility(View.GONE);

            // Set text.
            final String text = getString(
                    R.string.activity_list_filter_dialog_profile_found_text)
                    + " " + mFoundedEntriesCache.get(mSelectedFilterProfile);
            foundTextView.setText(text);

            // Show found text view.
            foundTextView.setVisibility(View.VISIBLE);

        } else {
            // Hide found text view.
            foundTextView.setVisibility(View.GONE);

            // Show progress bar.
            progressBar.setVisibility(View.VISIBLE);

            // Background task.
            mRunnableTasks.add(new Runnable() {
                @Override
                public void run() {

                    // Update views, if attached.
                    // Double check. Bug.
                    if (getActivity() != null && mDialog.isAttachedToWindow()) {

                        // Get number of entries.
                        final long count = ListDbHelper.getEntriesCount(
                                getActivity(),
                                ListDbHelper.convertToQueryBuilder(
                                        getActivity(),
                                        null,
                                        mFilterProfileMap));
                        // Update view.
                        if (getActivity() != null && mDialog.isAttachedToWindow()) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //
                                    final String text = getString(
                                            R.string.activity_list_filter_dialog_profile_found_text)
                                            + " " + count;

                                    // Set text.
                                    foundTextView.setText(text);

                                    // Hide progress bar.
                                    progressBar.setVisibility(View.GONE);

                                    // Show found text view.
                                    foundTextView.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    }
                }
            });
        }

        startRunnableTasks();
    }

    private void startRunnableTasks() {
        // Run all tasks, and clear.
        if (mDialog.isAttachedToWindow()) {
            for (Runnable task : mRunnableTasks) {
                mSingleThreadExecutor.submit(task);
            }
            mRunnableTasks.clear();
        }
    }

    /**
     * Show input text dialog for add profile, if confirm, then add new profile, and reinitialize.
     */
    private void showInputDialogAddProfile() {
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
                mFilterProfileMap.put(SP_FILTER_ID, "");
                mFilterProfileMap.put(SP_FILTER_TITLE, enteredText);
                String newId = addForCurrentUser(getActivity(), mFilterProfileMap);
                mFilterProfileMap.put(SP_FILTER_ID, newId);

                // Reinitialize
                initComponents();
            }
        });

        // Set cancel button listener.
        alert.setNegativeButton(cancel, null);

        // Show.
        alert.show();
    }
}
