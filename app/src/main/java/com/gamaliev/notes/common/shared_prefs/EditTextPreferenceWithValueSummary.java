package com.gamaliev.notes.common.shared_prefs;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class EditTextPreferenceWithValueSummary extends EditTextPreference {

    public EditTextPreferenceWithValueSummary(Context context) {
        super(context);
    }

    public EditTextPreferenceWithValueSummary(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        this.setSummary(this.getText());
        return super.onCreateView(parent);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            this.setSummary(getText());
        }
    }
}
