package com.gamaliev.notes.list.filter_sort_dialog;

import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayout;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.CommonUtils;

import java.util.Map;

import static com.gamaliev.notes.common.CommonUtils.EXTRA_REVEAL_ANIM_CENTER_TOP_END;
import static com.gamaliev.notes.common.CommonUtils.getResourceColorApi;
import static com.gamaliev.notes.common.CommonUtils.showToast;
import static com.gamaliev.notes.common.DialogFragmentUtils.initCircularRevealAnimation;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_LIST_FILTERED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_CREATED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_EDITED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_MANUALLY;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_TITLE;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_VIEWED;
import static com.gamaliev.notes.common.db.DbHelper.ORDER_ASCENDING;
import static com.gamaliev.notes.common.db.DbHelper.ORDER_DESCENDING;
import static com.gamaliev.notes.common.observers.ObserverHelper.LIST_FILTER;
import static com.gamaliev.notes.common.observers.ObserverHelper.notifyObservers;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_CREATED;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_EDITED;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_ID;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_TITLE;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_VIEWED;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

@SuppressWarnings("NullableProblems")
public final class FilterSortDialogFragment extends DialogFragment
        implements FilterSortDialogContract.View {

    /* Tags */
    @NonNull private static final String TAG_TITLE_TEXT_VIEW = "titleTextView";
    @NonNull private static final String TAG_FOUNDED_TEXT_VIEW = "foundedTextView";
    @NonNull private static final String TAG_PROGRESS_BAR = "progressBar";

    /* ... */
    @NonNull private FilterSortDialogContract.Presenter mPresenter;
    @NonNull private View mDialog;
    @NonNull private ViewGroup mProfilesViewGroup;
    @NonNull private GridLayout mColorsMatrixVg;
    @NonNull private CheckBox mColorCb;
    @NonNull private TextView mFoundTextView;
    @NonNull private View mProgressBar;


    /*
        Init
     */

    @NonNull
    public static FilterSortDialogFragment newInstance() {
        return new FilterSortDialogFragment();
    }


    /*
        Lifecycle
     */

    @Nullable
    @Override
    public View onCreateView(
            final LayoutInflater inflater,
            @Nullable final ViewGroup container,
            final Bundle savedInstanceState) {

        mDialog = inflater.inflate(R.layout.fragment_list_filter_dialog, container);
        disableTitle();
        return mDialog;
    }

    @Override
    public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
        initPresenter();
        mPresenter.onViewCreated(savedInstanceState);

        initComponents();
        setListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        initDialogSize();
        initCircularAnimation();
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        mPresenter.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }


    /*
        ...
     */

    private void disableTitle() {
        // Disable title for more space.
        final Window window = getDialog().getWindow();
        if (window != null) {
            window.requestFeature(Window.FEATURE_NO_TITLE);
        }
    }

    private void initPresenter() {
        new FilterSortDialogPresenter(this);
        mPresenter.start();
    }

    private void initDialogSize() {
        // Set max size of dialog. ( XML is not work :/ )
        final Window window = getDialog().getWindow();
        if (window != null) {
            final DisplayMetrics displayMetrics = getActivity().getResources().getDisplayMetrics();
            final ViewGroup.LayoutParams params = window.getAttributes();
            params.width = Math.min(
                    displayMetrics.widthPixels,
                    getActivity().getResources().getDimensionPixelSize(
                            R.dimen.fragment_list_filter_dialog_max_width));
            params.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
            window.setAttributes((android.view.WindowManager.LayoutParams) params);
        }
    }

    private void initCircularAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            initCircularRevealAnimation(
                    mDialog,
                    true,
                    EXTRA_REVEAL_ANIM_CENTER_TOP_END);
        }
    }

    @Override
    public void initComponents() {
        initProfilesComponents();
        mPresenter.initSortComponents();
        initDateComponents();
        initColorComponents();
        initFoundText();
    }

    private void setListeners() {
        setResetProfileListener();
        setAddCurrentProfileListener();
        setSortingListeners();
        setFilterDatesListeners();
        setActionButtonsListeners();
        setOnAttachStateChangeListener();
    }

    @Override
    public void initProfilesComponents() {
        mProfilesViewGroup = (ViewGroup) mDialog.findViewById(
                R.id.fragment_list_filter_dialog_profiles);
        mProfilesViewGroup.removeAllViews();
        mPresenter.initProfilesComponents();
    }

    private void initDateComponents() {
        mPresenter.setDatesStatus(
                SP_FILTER_CREATED,
                R.id.fragment_list_filter_dialog_filter_by_created_checkbox,
                R.id.fragment_list_filter_dialog_filter_by_created_button_from,
                R.id.fragment_list_filter_dialog_filter_by_created_button_to);

        mPresenter.setDatesStatus(
                SP_FILTER_EDITED,
                R.id.fragment_list_filter_dialog_filter_by_edited_checkbox,
                R.id.fragment_list_filter_dialog_filter_by_edited_button_from,
                R.id.fragment_list_filter_dialog_filter_by_edited_button_to);

        mPresenter.setDatesStatus(
                SP_FILTER_VIEWED,
                R.id.fragment_list_filter_dialog_filter_by_viewed_checkbox,
                R.id.fragment_list_filter_dialog_filter_by_viewed_button_from,
                R.id.fragment_list_filter_dialog_filter_by_viewed_button_to);
    }

    private void initColorComponents() {
        mColorsMatrixVg = (GridLayout) mDialog.findViewById(
                R.id.fragment_list_filter_dialog_color_by_body);
        mColorsMatrixVg.removeAllViews();

        mColorCb = (CheckBox) mDialog.findViewById(
                R.id.fragment_list_filter_dialog_color_by_default);
        mColorCb.setChecked(false);
        mColorCb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (mColorCb.isChecked()) {
                    mPresenter.performColorCbClick();
                }
                mColorCb.setChecked(true);
            }
        });

        mPresenter.initColorComponents();
    }

    private void initFoundText() {
        mFoundTextView = (TextView) mDialog
                .findViewById(R.id.fragment_list_filter_dialog_found_text_view);

        mProgressBar = mDialog.findViewById(
                R.id.fragment_list_filter_dialog_found_progress_bar);

        refreshFoundText();
    }

    private void setDateButtonOn(
            @NonNull final Button button,
            @Nullable final String text) {

        final float alpha = getResources()
                .getFraction(R.fraction.fragment_list_filter_dialog_date_alpha_on, 1, 1);

        // Change text, clickable, alpha.
        button.setText(text);
        button.setClickable(true);
        button.setAlpha(alpha);
    }

    private void setDateButtonOff(@NonNull final Button button) {
        final String defaultText = getString(R.string.fragment_list_filter_dialog_date_text_default);
        final float alpha = getResources()
                .getFraction(R.fraction.fragment_list_filter_dialog_date_alpha_off, 1, 1);

        // Change text, clickable, alpha.
        button.setText(defaultText);
        button.setClickable(false);
        button.setAlpha(alpha);
    }


    /*
        Listeners
     */

    private void setResetProfileListener() {
        mDialog.findViewById(R.id.fragment_list_filter_dialog_profile_reset)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        mPresenter.resetProfile();
                    }
                });
    }

    private void setAddCurrentProfileListener() {
        mDialog.findViewById(R.id.fragment_list_filter_dialog_profiles_save_current_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        showInputDialogAddProfile();
                    }
                });
    }

    @Override
    public void setSortingListeners() {
        ((RadioGroup) mDialog.findViewById(R.id.fragment_list_filter_dialog_sorting_by_radio_group_order))
                .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(final RadioGroup group, @IdRes final int checkedId) {
                        switch (checkedId) {
                            case R.id.fragment_list_filter_dialog_order_manually:
                                mPresenter.updateOrder(LIST_ITEMS_COLUMN_MANUALLY);
                                break;
                            case R.id.fragment_list_filter_dialog_order_title:
                                mPresenter.updateOrder(LIST_ITEMS_COLUMN_TITLE);
                                break;
                            case R.id.fragment_list_filter_dialog_order_created:
                                mPresenter.updateOrder(LIST_ITEMS_COLUMN_CREATED);
                                break;
                            case R.id.fragment_list_filter_dialog_order_edited:
                                mPresenter.updateOrder(LIST_ITEMS_COLUMN_EDITED);
                                break;
                            case R.id.fragment_list_filter_dialog_order_viewed:
                                mPresenter.updateOrder(LIST_ITEMS_COLUMN_VIEWED);
                                break;
                            default:
                                break;
                        }
                    }
                });

        ((RadioGroup) mDialog.findViewById(R.id.fragment_list_filter_dialog_sorting_by_radio_group_asc_desc))
                .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(final RadioGroup group, @IdRes final int checkedId) {
                        switch (checkedId) {
                            case R.id.fragment_list_filter_dialog_order_asc:
                                mPresenter.updateOrderAsc(ORDER_ASCENDING);
                                break;
                            case R.id.fragment_list_filter_dialog_order_desc:
                                mPresenter.updateOrderAsc(ORDER_DESCENDING);
                                break;
                            default:
                                break;
                        }
                    }
                });
    }

    private void setFilterDatesListeners() {
        setFilterDateListeners(
                SP_FILTER_CREATED,
                R.id.fragment_list_filter_dialog_filter_by_created_checkbox,
                R.id.fragment_list_filter_dialog_filter_by_created_button_from,
                R.id.fragment_list_filter_dialog_filter_by_created_button_to);
        
        setFilterDateListeners(
                SP_FILTER_EDITED,
                R.id.fragment_list_filter_dialog_filter_by_edited_checkbox,
                R.id.fragment_list_filter_dialog_filter_by_edited_button_from,
                R.id.fragment_list_filter_dialog_filter_by_edited_button_to);
        
        setFilterDateListeners(
                SP_FILTER_VIEWED,
                R.id.fragment_list_filter_dialog_filter_by_viewed_checkbox,
                R.id.fragment_list_filter_dialog_filter_by_viewed_button_from,
                R.id.fragment_list_filter_dialog_filter_by_viewed_button_to);
        
    }
    
    private void setFilterDateListeners(
            @NonNull final String filterCategory,
            final int checkBoxId, 
            final int dateFromId, 
            final int dateToId) {

        final CheckBox checkBox = (CheckBox) mDialog.findViewById(checkBoxId);
        final Button dateFrom   = (Button) mDialog.findViewById(dateFromId);
        final Button dateTo     = (Button) mDialog.findViewById(dateToId);

        checkBox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        if (checkBox.isChecked()) {
                            setDateButtonOn(
                                    dateFrom,
                                    getString(R.string.fragment_list_filter_dialog_date_text_from));
                            setDateButtonOn(
                                    dateTo,
                                    getString(R.string.fragment_list_filter_dialog_date_text_to));

                        } else {
                            setDateButtonOff(dateFrom);
                            setDateButtonOff(dateTo);
                            mPresenter.updateDate(filterCategory, "");
                        }
                        mPresenter.onChange();
                    }
                });

        dateFrom.setOnClickListener(getFilterDateListener(filterCategory, true));
        dateTo.setOnClickListener(getFilterDateListener(filterCategory, false));
    }

    @NonNull
    private View.OnClickListener getFilterDateListener(
            @NonNull final String filterCategory,
            final boolean isDateFrom) {

        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.initDatePicker(getContext(), filterCategory, isDateFrom);
            }
        };
    }

    private void setActionButtonsListeners() {
        mDialog.findViewById(R.id.fragment_list_filter_dialog_action_button_cancel)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mPresenter.performCancelBtnClick();
                    }
                });

        mDialog.findViewById(R.id.fragment_list_filter_dialog_action_button_filter)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mPresenter.performFilterBtnClick();
                    }
                });
    }

    private void setOnAttachStateChangeListener() {
        mDialog.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {

            @Override
            public void onViewAttachedToWindow(View v) {
                mPresenter.onViewAttachedToWindow();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                mPresenter.onViewDetachedFromWindow();
            }
        });
    }


    /*
        Add profile dialog.
     */

    private void showInputDialogAddProfile() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        final String title = getString(R.string.fragment_list_filter_dialog_profile_input_text_title);
        final String save = getString(R.string.fragment_list_filter_dialog_profile_input_text_save_title);
        final String cancel = getString(R.string.fragment_list_filter_dialog_profile_input_text_cancel_title);
        alert.setTitle(title);

        // Container with margin.
        final EditText editText = new EditText(getActivity());
        final FrameLayout container = new FrameLayout(getActivity());
        final FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        int m = getResources().getDimensionPixelSize(R.dimen.fragment_list_filter_dialog_input_title);
        params.setMargins(m, 0, m, 0);
        editText.setLayoutParams(params);
        container.addView(editText);

        alert   .setView(container)
                .setPositiveButton(save, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        final String enteredText = editText.getText().toString();
                        mPresenter.addProfile(enteredText);
                    }
                })
                .setNegativeButton(cancel, null)
                .show();
    }


    /*
        FilterSortDialogContract.View
     */

    @Override
    public void setPresenter(@NonNull final FilterSortDialogContract.Presenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public boolean isActive() {
        return isAdded() && mDialog.isAttachedToWindow();
    }

    @Override
    public void initSortComponents(
            @NonNull final String order,
            @NonNull final String orderAsc) {

        switch (order) {
            case LIST_ITEMS_COLUMN_MANUALLY:
                ((RadioButton) mDialog.findViewById(R.id.fragment_list_filter_dialog_order_manually))
                        .setChecked(true);
                break;
            case LIST_ITEMS_COLUMN_TITLE:
                ((RadioButton) mDialog.findViewById(R.id.fragment_list_filter_dialog_order_title))
                        .setChecked(true);
                break;
            case LIST_ITEMS_COLUMN_CREATED:
                ((RadioButton) mDialog.findViewById(R.id.fragment_list_filter_dialog_order_created))
                        .setChecked(true);
                break;
            case LIST_ITEMS_COLUMN_EDITED:
                ((RadioButton) mDialog.findViewById(R.id.fragment_list_filter_dialog_order_edited))
                        .setChecked(true);
                break;
            case LIST_ITEMS_COLUMN_VIEWED:
                ((RadioButton) mDialog.findViewById(R.id.fragment_list_filter_dialog_order_viewed))
                        .setChecked(true);
                break;
            default:
                break;
        }

        switch (orderAsc) {
            case ORDER_ASCENDING:
                ((RadioButton) mDialog.findViewById(R.id.fragment_list_filter_dialog_order_asc))
                        .setChecked(true);
                break;
            case ORDER_DESCENDING:
                ((RadioButton) mDialog.findViewById(R.id.fragment_list_filter_dialog_order_desc))
                        .setChecked(true);
                break;
            default:
                break;
        }
    }

    @Override
    public void addNewProfileView(@NonNull final Map<String, String> profileMap) {
        final View newView = View.inflate(
                getActivity(),
                R.layout.fragment_list_filter_dialog_profile,
                null);

        final String filterId = profileMap.get(SP_FILTER_ID);

        // Title.
        final String title = profileMap.get(SP_FILTER_TITLE);
        final TextView titleView = (TextView) newView.findViewById(
                R.id.fragment_list_filter_dialog_profile_title);
        titleView.setText(title);
        titleView.setTag(TAG_TITLE_TEXT_VIEW + filterId);

        // Founded.
        final TextView foundedTextView = (TextView) newView.findViewById(
                R.id.fragment_list_filter_dialog_profile_found);
        foundedTextView.setTag(TAG_FOUNDED_TEXT_VIEW + filterId);

        // Progress bar.
        final View progressBar = newView.findViewById(
                R.id.fragment_list_filter_dialog_profile_found_progress_bar);
        progressBar.setTag(TAG_PROGRESS_BAR + filterId);

        // On click changing local profile id, and updating views.
        newView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                mPresenter.performProfileClick(filterId);
            }
        });

        // Delete.
        final ImageButton deleteButton = (ImageButton) newView.findViewById(
                R.id.fragment_list_filter_dialog_profile_delete);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                mPresenter.performProfileDeleteClick(filterId);
            }
        });

        mProfilesViewGroup.addView(newView);
    }

    @Override
    public void addNewColorView(final int color, final boolean selected) {
        final int s = (int) getResources().getDimension(R.dimen.fragment_list_filter_dialog_color_button_size);
        final int m = (int) getResources().getDimension(R.dimen.fragment_list_filter_dialog_color_button_margin);
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(s, s);
        params.setMargins(m, m, m, m);

        final ImageButton button = new ImageButton(getActivity());
        button.setLayoutParams(params);

        // Set oval drawable.
        Drawable oval;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            oval = getResources().getDrawable(R.drawable.btn_oval, null);
        } else {
            //noinspection deprecation
            oval = getResources().getDrawable(R.drawable.btn_oval);
        }
        button.setBackground(oval);

        CommonUtils.setBackgroundColor(button, color);

        if (selected) {
            button.setImageResource(R.drawable.ic_done_white_24dp);
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                mColorCb.setChecked(false);
                mPresenter.performColorClick(color);
            }
        });

        mColorsMatrixVg.addView(button);
    }

    @Override
    public void setCheckedColorCb(boolean checked) {
        mColorCb.setChecked(true);
    }

    @Override
    public void showProfileFoundedEntriesText(
            @NonNull final String filterId,
            @NonNull final String foundedEntries) {

        final TextView foundedTextView =
                (TextView) mDialog.findViewWithTag(TAG_FOUNDED_TEXT_VIEW + filterId);
        final View progressBar = mDialog.findViewWithTag(TAG_PROGRESS_BAR + filterId);

        //noinspection SetTextI18n
        foundedTextView.setText("(" + foundedEntries + ")");
        progressBar.setVisibility(View.GONE);
        foundedTextView.setVisibility(View.VISIBLE);
    }

    @Override
    public void highlightProfileTitle(@NonNull String filterId) {
        final TextView titleView =
                (TextView) mDialog.findViewWithTag(TAG_TITLE_TEXT_VIEW + filterId);
        final String titleText = titleView.getText().toString();

        final int color = getResourceColorApi(getContext(), R.color.color_primary_contrast);
        titleView.setTextColor(color);

        final SpannableString titleUnderline = new SpannableString(titleText);
        titleUnderline.setSpan(new UnderlineSpan(), 0, titleUnderline.length(), 0);
        titleView.setText(titleUnderline);
    }

    @Override
    public void unsetSortingListeners() {
        ((RadioGroup) mDialog.findViewById(
                R.id.fragment_list_filter_dialog_sorting_by_radio_group_order))
                .setOnCheckedChangeListener(null);
        ((RadioGroup) mDialog.findViewById(
                R.id.fragment_list_filter_dialog_sorting_by_radio_group_asc_desc))
                .setOnCheckedChangeListener(null);
    }

    @Override
    public void setDatesStatus(
            final int checkboxId,
            final int dateFromId,
            final int dateToId,
            @NonNull final String localtimeDateFrom,
            @NonNull final String localtimeDateTo) {

        ((CheckBox) mDialog.findViewById(checkboxId)).setChecked(true);

        final Button buttonFrom = (Button) mDialog.findViewById(dateFromId);
        setDateButtonOn(buttonFrom, localtimeDateFrom);

        final Button buttonTo = (Button) mDialog.findViewById(dateToId);
        setDateButtonOn(buttonTo, localtimeDateTo);
    }

    @Override
    public void setDatesStatusNull(
            final int checkboxId,
            final int dateFromId,
            final int dateToId) {

        ((CheckBox) mDialog.findViewById(checkboxId)).setChecked(false);

        final Button buttonFrom = (Button) mDialog.findViewById(dateFromId);
        setDateButtonOff(buttonFrom);

        final Button buttonTo = (Button) mDialog.findViewById(dateToId);
        setDateButtonOff(buttonTo);
    }

    @Override
    public void refreshFoundText() {
        mFoundTextView.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);

        mPresenter.refreshFoundText();
    }

    @Override
    public void showFoundText(@NonNull final String text) {
        mFoundTextView.setText(text);
        mProgressBar.setVisibility(View.GONE);
        mFoundTextView.setVisibility(View.VISIBLE);
    }

    @Override
    public void finish() {
        showToast(getString(R.string.fragment_list_notification_filtered),
                Toast.LENGTH_SHORT);

        notifyObservers(
                LIST_FILTER,
                RESULT_CODE_LIST_FILTERED,
                null);
    }
}
