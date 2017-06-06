package com.gamaliev.notes.common.custom_view;

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
    public CharSequence getSummary() {
        return getText();
    }
}
