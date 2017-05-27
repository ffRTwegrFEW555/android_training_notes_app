package com.gamaliev.notes.common.shared_prefs;

import android.content.Context;
import android.preference.EditTextPreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class EditTextPreferenceWithValueSummary extends EditTextPreference {

    /*
        Init
     */

    public EditTextPreferenceWithValueSummary(@NonNull final Context context) {
        super(context);
    }

    public EditTextPreferenceWithValueSummary(
            @NonNull final Context context,
            @NonNull final AttributeSet attrs) {

        super(context, attrs);
    }


    /*
        Lifecycle
     */

    @Override
    protected View onCreateView(final ViewGroup parent) {
        this.setSummary(this.getText());
        return super.onCreateView(parent);
    }

    @Override
    protected void onDialogClosed(final boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            this.setSummary(getText());
        }
    }
}
