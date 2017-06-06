package com.gamaliev.notes.common.shared_prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static android.content.Context.MODE_PRIVATE;
import static com.gamaliev.notes.common.db.DbHelper.BASE_COLUMN_ID;
import static com.gamaliev.notes.common.db.DbHelper.FAVORITE_COLUMN_COLOR;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_CREATED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_EDITED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_MANUALLY;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_TITLE;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_VIEWED;
import static com.gamaliev.notes.common.db.DbHelper.ORDER_ASCENDING;
import static com.gamaliev.notes.common.db.DbHelper.ORDER_ASC_DESC_DEFAULT;
import static com.gamaliev.notes.common.shared_prefs.SpCommon.convertMapToJson;
import static com.gamaliev.notes.common.shared_prefs.SpCommon.setString;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.getPreferencesName;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

@SuppressWarnings("WeakerAccess")
public final class SpFilterProfiles {

    /* Logger */
    @NonNull private static final String TAG = SpFilterProfiles.class.getSimpleName();

    /* Filter, profiles */
    @NonNull public static final String SP_FILTER_PROFILE_SELECTED_ID   = "filterProfileSelectedId";
    @NonNull public static final String SP_FILTER_PROFILE_DEFAULT       = "filterProfileDefault";
    @NonNull public static final String SP_FILTER_PROFILE_DEFAULT_ID    = "-1";
    @NonNull public static final String SP_FILTER_PROFILE_MANUAL        = "filterProfileManual";
    @NonNull public static final String SP_FILTER_PROFILE_MANUAL_ID     = "-2";
    @NonNull public static final String SP_FILTER_PROFILE_CURRENT       = "filterProfileCurrent";
    @NonNull public static final String SP_FILTER_PROFILE_CURRENT_ID    = "-3";
    @NonNull public static final String SP_FILTER_PROFILES_SET          = "filterProfilesSet";

    /* Filter */
    @NonNull public static final String SP_FILTER_ID        = BASE_COLUMN_ID;
    @NonNull public static final String SP_FILTER_TITLE     = "title";
    @NonNull public static final String SP_FILTER_COLOR     = FAVORITE_COLUMN_COLOR;
    @NonNull public static final String SP_FILTER_CREATED   = LIST_ITEMS_COLUMN_CREATED;
    @NonNull public static final String SP_FILTER_EDITED    = LIST_ITEMS_COLUMN_EDITED;
    @NonNull public static final String SP_FILTER_VIEWED    = LIST_ITEMS_COLUMN_VIEWED;

    @NonNull public static final String SP_FILTER_SYMBOL_DATE_SPLIT = "#";
    @NonNull public static final String SP_FILTER_ORDER     = "order";
    @NonNull public static final String SP_FILTER_ORDER_ASC = "orderAscDesc";


    /*
        Init
     */

    private SpFilterProfiles() {}


    /*
        Getters
     */

    /**
     * Get hardcoded default filter profile.
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

    /**
     * Get hardcoded filter profile for manual sorting (Drag & Drop).
     * @return Profile in Json-format, for manual sorting (Drag & Drop).
     */
    @NonNull
    public static String getManualProfile() {
        final JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put(SP_FILTER_ID,        SP_FILTER_PROFILE_MANUAL_ID);
            jsonObject.put(SP_FILTER_TITLE,     "");
            jsonObject.put(SP_FILTER_COLOR,     "");
            jsonObject.put(SP_FILTER_CREATED,   "");
            jsonObject.put(SP_FILTER_EDITED,    "");
            jsonObject.put(SP_FILTER_VIEWED,    "");
            jsonObject.put(SP_FILTER_ORDER,     LIST_ITEMS_COLUMN_MANUALLY);
            jsonObject.put(SP_FILTER_ORDER_ASC, ORDER_ASCENDING);

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }

        return jsonObject.toString();
    }

    /**
     * Get filter profile.
     * @param context   Context.
     * @param userId    User id.
     * @param profileId Profile id.
     * @return          Filter profile in Json-format if found, otherwise null.
     */
    @Nullable
    public static String get(
            @NonNull final Context context,
            @NonNull final String userId,
            @NonNull final String profileId) {

        final Set<String> set = getProfiles(context, userId);
        for (String profile : set) {
            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(profile);
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
                return null;
            }

            if (profileId.equals(jsonObject.optString(SP_FILTER_ID))) {
                return profile;
            }
        }

        return null;
    }

    /**
     * Get id of selected filter profile, for current user.
     * @param context   Context.
     * @return          Id of selected filter profile.
     */
    @Nullable
    public static String getSelectedIdForCurrentUser(
            @NonNull final Context context) {

        final String selected = SpUsers.getSelected(context);
        return selected == null
                ? null
                : getSelectedId(context, selected);
    }

    /**
     * Get id of selected filter profile.
     * @param context   Context.
     * @param userId    User id.
     * @return          Id of selected filter profile.
     */
    @Nullable
    public static String getSelectedId(
            @NonNull final Context context,
            @NonNull final String userId) {

        final SharedPreferences sp = context.getSharedPreferences(
                getPreferencesName(userId),
                MODE_PRIVATE);
        return sp.getString(SP_FILTER_PROFILE_SELECTED_ID, null);
    }

    /**
     * Get selected filter profile from current user preferences.
     * @param context   Context.
     * @return          Filter profile in Json-format.
     */
    @Nullable
    public static String getSelectedForCurrentUser(
            @NonNull final Context context) {

        final String selected = SpUsers.getSelected(context);
        return selected == null
                ? null
                : getSelected(context, selected);
    }

    /**
     * Get selected filter profile from user preferences.
     * @param context   Context.
     * @param userId    User id.
     * @return          Filter profile in Json-format.
     */
    @Nullable
    public static String getSelected(
            @NonNull final Context context,
            @NonNull final String userId) {

        final SharedPreferences sp = context.getSharedPreferences(
                getPreferencesName(userId),
                MODE_PRIVATE);

        final String profileId = getSelectedId(context, userId);
        if (profileId == null) {
            return null;
        }

        if (SP_FILTER_PROFILE_CURRENT_ID.equals(profileId)) {
            return sp.getString(SP_FILTER_PROFILE_CURRENT, null);
        } else if (SP_FILTER_PROFILE_DEFAULT_ID.equals(profileId)) {
            return sp.getString(SP_FILTER_PROFILE_DEFAULT, null);
        } else if (SP_FILTER_PROFILE_MANUAL_ID.equals(profileId)) {
            return sp.getString(SP_FILTER_PROFILE_MANUAL, null);
        } else {
            return get(context, userId, profileId);
        }
    }

    /**
     * Get all filter profiles from current user preferences.
     * @param context   Context.
     * @return          All filter profiles.
     */
    @Nullable
    public static Set<String> getProfilesForCurrentUser(
            @NonNull final Context context) {

        final String selected = SpUsers.getSelected(context);
        return selected == null
                ? null
                : getProfiles(context, selected);
    }

    /**
     * Get all filter profiles from user preferences.
     * @param context   Context.
     * @param userId    User id.
     * @return          All filter profiles.
     */
    @NonNull
    public static Set<String> getProfiles(
            @NonNull final Context context,
            @NonNull final String userId) {

        final SharedPreferences sp = context.getSharedPreferences(
                getPreferencesName(userId),
                MODE_PRIVATE);
        return sp.getStringSet(SP_FILTER_PROFILES_SET, new HashSet<String>());
    }


    /*
        Setters
     */

    /**
     * Add filter profile to current user preferences.
     * @param context   Context.
     * @param profile   Filter profile.
     * @return          Id of added filter profile.
     */
    @Nullable
    public static String addForCurrentUser(
            @NonNull final Context context,
            @NonNull final Map<String, String> profile) {

        final String selected = SpUsers.getSelected(context);
        return selected == null
                ? null
                : add(context, selected, profile);
    }

    /**
     * Add filter profile to user preferences.
     * @param context   Context.
     * @param userId    User id.
     * @param profile   Filter profile.
     * @return          Id of added filter profile.
     */
    @NonNull
    public static String add(
            @NonNull final Context context,
            @NonNull final String userId,
            @NonNull final Map<String, String> profile) {

        final UUID newId = UUID.randomUUID();
        profile.put(SP_FILTER_ID, newId.toString());

        final Set<String> set = getProfiles(context, userId);
        set.add(convertMapToJson(profile));
        updateProfiles(context, userId, set);

        return newId.toString();
    }

    /**
     * Set id of selected filter profile to current user preferences.
     * @param context   Context.
     * @param profileId Profile id.
     * @return True if ok, otherwise false.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static boolean setSelectedForCurrentUser(
            @NonNull final Context context,
            @NonNull final String profileId) {

        final String selected = SpUsers.getSelected(context);
        if (selected == null) {
            return false;
        }
        setSelected(context, selected, profileId);
        return true;
    }

    /**
     * Set id of selected filter profile to user preferences.
     * @param context   Context.
     * @param userId    User id.
     * @param profileId Profile id.
     */
    public static void setSelected(
            @NonNull final Context context,
            @NonNull final String userId,
            @NonNull final String profileId) {

        final SharedPreferences sp = context.getSharedPreferences(
                getPreferencesName(userId),
                MODE_PRIVATE);

        sp      .edit()
                .putString(SP_FILTER_PROFILE_SELECTED_ID, profileId)
                .apply();
    }

    /**
     * Update current profile in current user preferences, by given filter profile.
     * @param context   Context.
     * @param profile   Filter profile.
     * @return True if ok, otherwise false.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static boolean updateCurrentForCurrentUser(
            @NonNull final Context context,
            @NonNull final Map<String, String> profile) {

        final String selected = SpUsers.getSelected(context);
        if (selected == null) {
            return false;
        }
        updateCurrent(context, selected, profile);
        return true;
    }

    /**
     * Update current profile in user preferences, by given filter profile.
     * @param context   Context.
     * @param userId    User id.
     * @param profile   Filter profile.
     */
    public static void updateCurrent(
            @NonNull final Context context,
            @NonNull final String userId,
            @NonNull final Map<String, String> profile) {

        final SharedPreferences sp = context.getSharedPreferences(
                getPreferencesName(userId),
                MODE_PRIVATE);

        sp      .edit()
                .putString(SP_FILTER_PROFILE_CURRENT, convertMapToJson(profile))
                .apply();
    }

    /**
     * Reset current profile filter to default, in user preferences.
     * @param context   Context.
     * @param userId    User id.
     */
    public static void resetCurrent(
            @NonNull final Context context,
            @NonNull final String userId) {

        setString(
                context,
                getPreferencesName(userId),
                SP_FILTER_PROFILE_CURRENT,
                getDefaultProfile());

        setSelected(
                context,
                userId,
                SP_FILTER_PROFILE_CURRENT_ID);
    }

    /**
     * Update filter profiles in user preferences.
     * @param context   Context.
     * @param userId    User id.
     * @param profiles  Filter profiles.
     */
    public static void updateProfiles(
            @NonNull final Context context,
            @NonNull final String userId,
            @NonNull final Set<String> profiles) {

        final SharedPreferences sp = context.getSharedPreferences(
                getPreferencesName(userId),
                MODE_PRIVATE);

        sp      .edit()
                .putStringSet(SP_FILTER_PROFILES_SET, profiles)
                .apply();
    }

    /**
     * Delete filter profile from current user preferences.
     * @param context       Context.
     * @param profileId     Profile id.
     * @return True if ok, otherwise false.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static boolean deleteForCurrentUser(
            @NonNull final Context context,
            @NonNull final String profileId) {

        final String selected = SpUsers.getSelected(context);
        return selected != null && delete(context, selected, profileId);
    }

    /**
     * Delete filter profile from user preferences.
     * @param context       Context.
     * @param userId        User id.
     * @param profileId     Profile id.
     * @return True if ok, otherwise false.
     */
    public static boolean delete(
            @NonNull final Context context,
            @NonNull final String userId,
            @NonNull final String profileId) {

        if (SP_FILTER_PROFILE_DEFAULT_ID.equals(profileId)
                || SP_FILTER_PROFILE_MANUAL_ID.equals(profileId)) {
            return true;
        }

        if (SP_FILTER_PROFILE_CURRENT_ID.equals(profileId)) {
            resetCurrent(context, userId);
            return true;
        }

        if (profileId.equals(getSelectedId(context, userId))) {
            resetCurrent(context, userId);
        }

        final Set<String> set = getProfiles(context, userId);
        final Iterator<String> iterator = set.iterator();
        while (iterator.hasNext()) {
            final String next = iterator.next();

            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(next);
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
            }
            if (jsonObject == null) {
                return false;
            }
            final String id = jsonObject.optString(SP_FILTER_ID);

            if (profileId.equals(id)) {
                iterator.remove();
                break;
            }
        }

        updateProfiles(context, userId, set);
        return true;
    }
}
