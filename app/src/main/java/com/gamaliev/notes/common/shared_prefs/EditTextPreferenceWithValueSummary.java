package com.gamaliev.notes.common.shared_prefs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.preference.EditTextPreference;
import android.util.AttributeSet;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class EditTextPreferenceWithValueSummary extends EditTextPreference {

    /*
        Init
     */

    @SuppressWarnings("unused")
    public EditTextPreferenceWithValueSummary(@NonNull final Context context) {
        super(context);
    }

    @SuppressWarnings("unused")
    public EditTextPreferenceWithValueSummary(
            @NonNull final Context context,
            @NonNull final AttributeSet attrs) {

        super(context, attrs);
    }


    /*
        Lifecycle
     */

    @Override
    public CharSequence getSummary() {
        return getText();
    }
}
