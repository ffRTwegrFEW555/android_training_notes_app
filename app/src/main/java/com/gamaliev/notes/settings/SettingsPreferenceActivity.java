package com.gamaliev.notes.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.shared_prefs.SpUsers;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class SettingsPreferenceActivity extends AppCompatActivity {

    /*
        Init
     */

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_preference);

        initToolbar();

        if (savedInstanceState == null) {
            initPreferenceFragment();
        }
    }

    private void initToolbar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.activity_settings_preference_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void initPreferenceFragment() {
        final SettingsPreferenceFragment fragment = SettingsPreferenceFragment.getInstance();
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.activity_settings_preference_fragment, fragment, null)
                .commit();
    }


    /*
        Preference fragment
     */

    public static class SettingsPreferenceFragment extends PreferenceFragment {

        /*
            Init
         */

        @NonNull
        public static SettingsPreferenceFragment getInstance() {
            final SettingsPreferenceFragment fragment = new SettingsPreferenceFragment();
            return fragment;
        }


        /*
            Lifecycle
         */

        @Override
        public void onCreate(@Nullable final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Change preference name to current user.
            final PreferenceManager manager = getPreferenceManager();
            manager.setSharedPreferencesName(
                    SpUsers.getPreferencesName(SpUsers.getSelected(getActivity())));
            manager.setSharedPreferencesMode(MODE_PRIVATE);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preference_settings);
        }
    }


    /*
        Intents
     */

    public static void startIntent(
            @NonNull final Context context,
            final int requestCode) {

        final Intent starter = new Intent(context, SettingsPreferenceActivity.class);
        ((Activity) context).startActivityForResult(starter, requestCode);
    }
}
