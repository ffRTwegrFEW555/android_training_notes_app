package com.gamaliev.notes.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.transition.Fade;
import android.view.View;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.shared_prefs.SpUsers;

import static android.content.Context.MODE_PRIVATE;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class SettingsPreferenceFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    /*
        Init
    */

    @NonNull
    public static SettingsPreferenceFragment newInstance() {
        return new SettingsPreferenceFragment();
    }


    /*
        Lifecycle
    */

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Change preference name to current user.
        final PreferenceManager manager = getPreferenceManager();
        manager.setSharedPreferencesName(
                SpUsers.getPreferencesName(SpUsers.getSelected(getActivity())));
        manager.setSharedPreferencesMode(MODE_PRIVATE);

        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preference_settings, rootKey);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initTransition();
        initActionBar();
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager()
                .getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onPause() {
        getPreferenceManager()
                .getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }


    /*
        ...
     */

    private void initTransition() {
        setExitTransition(new Fade());
        setEnterTransition(new Fade());
    }

    private void initActionBar() {
        final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.fragment_settings_preference));
        }
    }


    /*
        SharedPreferences.OnSharedPreferenceChangeListener
     */

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        setPreferenceScreen(null);
        addPreferencesFromResource(R.xml.preference_settings);
    }
}
