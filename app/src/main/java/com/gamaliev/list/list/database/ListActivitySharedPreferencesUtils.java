package com.gamaliev.list.list.database;

import android.app.Activity;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static android.content.Context.MODE_PRIVATE;
import static com.gamaliev.list.common.database.DatabaseHelper.BASE_COLUMN_ID;
import static com.gamaliev.list.common.database.DatabaseHelper.FAVORITE_COLUMN_COLOR;
import static com.gamaliev.list.common.database.DatabaseHelper.LIST_ITEMS_COLUMN_CREATED;
import static com.gamaliev.list.common.database.DatabaseHelper.LIST_ITEMS_COLUMN_EDITED;
import static com.gamaliev.list.common.database.DatabaseHelper.LIST_ITEMS_COLUMN_TITLE;
import static com.gamaliev.list.common.database.DatabaseHelper.LIST_ITEMS_COLUMN_VIEWED;
import static com.gamaliev.list.common.database.DatabaseHelper.ORDER_ASC_DESC_DEFAULT;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class ListActivitySharedPreferencesUtils {

    /* Logger */
    private static final String TAG = ListActivitySharedPreferencesUtils.class.getSimpleName();

    /* Shared Preferences */
    private static final String SP_HAS_VISITED = "hasVisited";

    public static final String SP_FILTER_PROFILE_SELECTED_ID = "filterProfileSelected";
    public static final String SP_FILTER_PROFILE_DEFAULT    = "defaultProfile";
    public static final String SP_FILTER_PROFILE_DEFAULT_ID = "-1";
    public static final String SP_FILTER_PROFILE_CURRENT    = "currentProfile";
    public static final String SP_FILTER_PROFILE_CURRENT_ID = "-2";
    public static final String SP_FILTER_PROFILES_SET       = "filterProfilesSet";

    public static final String SP_FILTER_ID                 = BASE_COLUMN_ID;
    public static final String SP_FILTER_TITLE              = "title";
    public static final String SP_FILTER_COLOR              = FAVORITE_COLUMN_COLOR;
    public static final String SP_FILTER_CREATED            = LIST_ITEMS_COLUMN_CREATED;
    public static final String SP_FILTER_EDITED             = LIST_ITEMS_COLUMN_EDITED;
    public static final String SP_FILTER_VIEWED             = LIST_ITEMS_COLUMN_VIEWED;
    public static final String SP_FILTER_SYMBOL_DATE_SPLIT  = "#";

    public static final String SP_FILTER_ORDER              = "order";
    public static final String SP_FILTER_ORDER_ASC          = "orderAscDesc";


    /*
        Init
     */

    private ListActivitySharedPreferencesUtils() {
    }

    /**
     * Initialize shared preferences on first start.
     * @param activity Activity.
     */
    public static void initSharedPreferences(
            @NonNull final Activity activity) {

        // Get
        final SharedPreferences sp = activity.getPreferences(MODE_PRIVATE);

        // If app start first time.
        if (!sp.getBoolean(SP_HAS_VISITED, false)) {

            // Get editor.
            final SharedPreferences.Editor editor = sp.edit();

            // Add mock profiles.
            final Set<String> profiles = ListDatabaseMockHelper.getMockProfiles();
            editor.putStringSet(SP_FILTER_PROFILES_SET, profiles);

            // Set default profile as selected profile.
            editor.putString(SP_FILTER_PROFILE_SELECTED_ID, SP_FILTER_PROFILE_CURRENT_ID);

            // Add default profile
            editor.putString(SP_FILTER_PROFILE_DEFAULT, getDefaultProfile());

            // Add current profile
            editor.putString(SP_FILTER_PROFILE_CURRENT, getDefaultProfile());

            // Mark visited.
            editor.putBoolean(SP_HAS_VISITED, true);
            editor.apply();
        }
    }


    /*
        Converters
     */

    /**
     * Convert profile from Json-format to Map-format.
     * @param profileJson   Profile Json-format.
     * @return              Profile Map-format.
     */
    @Nullable
    public static Map<String, String> convertProfileJsonToMap(
            @NonNull final String profileJson) {

        // Create new profile map.
        final Map<String, String> profileMap = new HashMap<>();

        // Create and parse Json-object from string.
        JSONObject profileJsonObject = null;
        try {
            profileJsonObject = new JSONObject(profileJson);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }

        // Populate profile map.
        final Iterator<String> keys = profileJsonObject.keys();
        while (keys.hasNext()) {
            final String key = keys.next();
            profileMap.put(key, profileJsonObject.optString(key, ""));
        }

        return profileMap;
    }

    /**
     * Convert profile from Map-format to Json-format.
     * @param profileMap    Profile Map-format.
     * @return              Profile Json-format.
     */
    @Nullable
    public static String convertProfileMapToJson (
            @NonNull final Map<String, String> profileMap) {

        final JSONObject profileJsonObject = new JSONObject(profileMap);
        return profileJsonObject.toString();
    }


    /*
        Getters
     */

    /**
     * Get value from shared preferences, with given key.
     * @param activity  Activity.
     * @param key       Key.
     * @return          Value.
     */
    @Nullable
    public static String getStringFromSp(
            @NonNull final Activity activity,
            @NonNull final String key) {

        final SharedPreferences sp = activity.getPreferences(MODE_PRIVATE);
        return sp.getString(key, null);
    }

    /**
     * Get profiles in Json-format from shared preferences, with "selectedId"-from-shared-preferences.
     * @param activity  Activity.
     * @return          Profile in Json-format.
     */
    @Nullable
    public static String getSelectedProfileJson(
            @NonNull final Activity activity) {

        final SharedPreferences sp = activity.getPreferences(MODE_PRIVATE);

        // Get selected profile id.
        final String selectedProfileId = sp.getString(SP_FILTER_PROFILE_SELECTED_ID, null);

        // Default, current or not.
        if (SP_FILTER_PROFILE_DEFAULT_ID.equals(selectedProfileId)) {
            return sp.getString(SP_FILTER_PROFILE_DEFAULT, null);
        } else if (SP_FILTER_PROFILE_CURRENT_ID.equals(selectedProfileId)) {
            return sp.getString(SP_FILTER_PROFILE_CURRENT, null);
        } else {
            return getProfile(activity, selectedProfileId);
        }
    }

    /**
     * Get profiles in Json-format from shared preferences, with given id.
     * @param activity  Activity.
     * @param id        Profile id.
     * @return          Profile in Json-format.
     */
    @Nullable
    public static String getProfile(
            @NonNull final Activity activity,
            @NonNull final String id) {

        // Get profiles set.
        final Set<String> profilesSet = getProfilesSet(activity);

        // Find profile with given id.
        for (String profileJson : profilesSet) {

            // Create and parse Json-object from string.
            JSONObject profileJsonObject = null;
            try {
                profileJsonObject = new JSONObject(profileJson);
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
            }

            // If found.
            if (id.equals(profileJsonObject.optString(SP_FILTER_ID))) {
                return profileJson;
            }
        }

        return null;
    }

    /**
     * Get all profiles from shared preferences.
     * @param activity  Activity.
     * @return          All profiles.
     */
    @Nullable
    public static Set<String> getProfilesSet(
            @NonNull final Activity activity) {

        final SharedPreferences sp = activity.getPreferences(MODE_PRIVATE);
        return sp.getStringSet(SP_FILTER_PROFILES_SET, null);
    }

    /**
     * Get hardcoded default profile in Json-format.
     * @return Default profile in Json-format.
     */
    @NonNull
    public static String getDefaultProfile() {
        final JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put(SP_FILTER_ID,        SP_FILTER_PROFILE_CURRENT_ID);
            jsonObject.put(SP_FILTER_TITLE,     "");
            jsonObject.put(SP_FILTER_COLOR,     "");
            jsonObject.put(SP_FILTER_CREATED,   "");
            jsonObject.put(SP_FILTER_EDITED,    "");
            jsonObject.put(SP_FILTER_VIEWED,    "");
            jsonObject.put(SP_FILTER_ORDER,     LIST_ITEMS_COLUMN_TITLE);
            jsonObject.put(SP_FILTER_ORDER_ASC, ORDER_ASC_DESC_DEFAULT);

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }

        return jsonObject.toString();
    }


    /*
        Setters
     */

    /**
     * Set pair key-value to shared preferences.
     * @param activity  Activity.
     * @param key       Key.
     * @param value     Value.
     */
    public static void setString(
            @NonNull final Activity activity,
            @NonNull final String key,
            @NonNull final String value) {

        // Get sp.
        final SharedPreferences sp = activity.getPreferences(MODE_PRIVATE);

        // Get editor.
        final SharedPreferences.Editor editor = sp.edit();

        // Put, apply.
        editor.putString(key, value);
        editor.apply();
    }

    /**
     * Set selected profile id to shared preferences.
     * @param activity  Activity.
     * @param id        Id.
     */
    public static void setSelectedProfileId(
            @NonNull final Activity activity,
            @NonNull final String id) {

        // Get sp.
        final SharedPreferences sp = activity.getPreferences(MODE_PRIVATE);

        // Get editor.
        final SharedPreferences.Editor editor = sp.edit();

        // Put, apply.
        editor.putString(SP_FILTER_PROFILE_SELECTED_ID, id);
        editor.apply();
    }

    /**
     * Save profiles to shared preferences.
     * @param activity      Activity.
     * @param profilesSet   Profiles set.
     */
    public static void saveProfilesSet(
            @NonNull final Activity activity,
            @NonNull final Set<String> profilesSet) {

        // Get sp.
        final SharedPreferences sp = activity.getPreferences(MODE_PRIVATE);

        // Get editor.
        final SharedPreferences.Editor editor = sp.edit();

        // Put, apply.
        editor.putStringSet(SP_FILTER_PROFILES_SET, profilesSet);
        editor.apply();
    }

    /**
     * Save given profile in Map-format to "currentProfile", in shared preferences.
     * @param activity      Activity.
     * @param profileMap    Profile in Map-format.
     */
    public static void saveCurrentProfile(
            @NonNull final Activity activity,
            @NonNull final Map<String, String> profileMap) {

        // Get sp.
        final SharedPreferences sp = activity.getPreferences(MODE_PRIVATE);

        // Get editor.
        final SharedPreferences.Editor editor = sp.edit();

        // Put, apply.
        editor.putString(SP_FILTER_PROFILE_CURRENT, convertProfileMapToJson(profileMap));
        editor.apply();
    }

    /**
     * Update given profile in shared preferences.
     * @param activity      Activity.
     * @param profileMap    Profile in Map-format.
     * @return              New id.
     */
    @Nullable
    public static String updateProfile(
            @NonNull final Activity activity,
            @NonNull final Map<String, String> profileMap) {

        // Get given id
        final String idGiven = profileMap.get(SP_FILTER_ID);

        // If default profile, then access denied.
        if (SP_FILTER_PROFILE_DEFAULT_ID.equals(idGiven)) {
            return null;
        }

        // If current profile, then store to current pair.
        if (SP_FILTER_PROFILE_CURRENT_ID.equals(idGiven)) {
            saveCurrentProfile(activity, profileMap);
            return null;
        }

        // Else add new profile.

        // Add new id.
        final UUID newId = UUID.randomUUID();
        profileMap.put(SP_FILTER_ID, newId.toString());

        // Get profiles set.
        final Set<String> profilesSet = getProfilesSet(activity);

        // Add new profile.
        profilesSet.add(convertProfileMapToJson(profileMap));

        // Store updated profiles set.
        saveProfilesSet(activity, profilesSet);

        return newId.toString();
    }

    /**
     * Delete profile from shared preferences, with given id.
     * @param activity      Activity.
     * @param idToDelete    Id of profile. whose will be deleted.
     */
    public static void deleteProfile(
            @NonNull final Activity activity,
            @NonNull final String idToDelete) {

        // If default profile, then access denied.
        if (SP_FILTER_PROFILE_DEFAULT_ID.equals(idToDelete)) {
            return;
        }

        // If profile is current, then set default profile.
        if (SP_FILTER_PROFILE_CURRENT_ID.equals(idToDelete)) {
            setString(activity, SP_FILTER_PROFILE_CURRENT, getDefaultProfile());
            return;
        }

        // Reset profile if selected profile id equals removed id.
        if (idToDelete.equals(getStringFromSp(activity, SP_FILTER_PROFILE_CURRENT))) {
            setString(activity, SP_FILTER_PROFILE_CURRENT, getDefaultProfile());
            setSelectedProfileId(activity, SP_FILTER_PROFILE_CURRENT_ID);
        }

        // Get profiles set. Or given, or new.
        final Set<String> profilesSet = getProfilesSet(activity);

        // Get iterator
        final Iterator<String> iterator = profilesSet.iterator();

        // Seek
        while (iterator.hasNext()) {
            final String profileJson = iterator.next();

            // Parse to Json-object.
            JSONObject profileJsonObject = null;
            try {
                profileJsonObject = new JSONObject(profileJson);
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
            }

            // Get id from json
            final String id = profileJsonObject.optString(SP_FILTER_ID);

            // Compare. If equals, then remove old.
            if (idToDelete.equals(id)) {
                iterator.remove();
                break;
            }
        }

        // Store updated profiles set, if new.
        saveProfilesSet(activity, profilesSet);

        // Reset profile if selected profile id equals removed id.
        if (idToDelete.equals(getStringFromSp(activity, SP_FILTER_PROFILE_CURRENT))) {
            setString(activity, SP_FILTER_PROFILE_CURRENT, getDefaultProfile());
            setSelectedProfileId(activity, SP_FILTER_PROFILE_CURRENT_ID);
        }
    }
}