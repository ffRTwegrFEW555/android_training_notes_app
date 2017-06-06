package com.gamaliev.notes.common.shared_prefs;

import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.db.DbHelper;
import com.gamaliev.notes.common.network.NetworkBroadcastReceiver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;
import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;
import static com.gamaliev.notes.common.shared_prefs.SpCommon.SP_INITIALIZED;
import static com.gamaliev.notes.common.shared_prefs.SpCommon.SP_MAIN;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_PROFILE_CURRENT;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_PROFILE_CURRENT_ID;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_PROFILE_DEFAULT;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_PROFILE_MANUAL;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_PROFILE_SELECTED_ID;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

@SuppressWarnings("WeakerAccess")
public final class SpUsers {

    /* Logger */
    @NonNull private static final String TAG = SpUsers.class.getSimpleName();

    /* Users, main */
    public static final String SP_USERS_FILENAME_PREFIX     = "Users";
    public static final String SP_USERS_ID_COUNTER          = "usersIdCounter";
    public static final String SP_USERS_SET                 = "usersSet";
    public static final String SP_USERS_SELECTED_ID         = "usersSelectedId";
    public static final String SP_USERS_DEFAULT_USER_ID     = "-1";

    /* User, details */
    public static final String SP_USER_ID                   = "id";
    public static final String SP_USER_SYNC_ID              = "ext_id";
    public static final String SP_USER_EMAIL                = "email";
    public static final String SP_USER_FIRST_NAME           = "f_name";
    public static final String SP_USER_LAST_NAME            = "l_name";
    public static final String SP_USER_MIDDLE_NAME          = "m_name";
    public static final String SP_USER_DESCRIPTION          = "desc";
    public static final String SP_USER_MOCK_ENTRIES_DEFAULT = "mock_entries";
    public static final String SP_USER_PROGRESS_NOTIFICATION_TIMER = "progress_notification_timer";
    public static final String SP_USER_SYNC                 = "sync";
    public static final String SP_USER_SYNC_WIFI            = "sync_wifi";
    public static final String SP_USER_SYNC_API_URL         = "sync_api_url";
    public static final String SP_USER_SYNC_PENDING         = "sync_pending";

    /* Pending Sync */
    public static final String SP_USER_SYNC_PENDING_TRUE    = "true";
    public static final String SP_USER_SYNC_PENDING_FALSE   = "false";


    /*
        Init
     */

    private SpUsers() {}


    /*
        Getters
     */

    /**
     * Get hardcoded default user profile in Map-format.
     * @return Default user profile.
     */
    @SuppressWarnings({"LineLength"})
    @NonNull
    static Map<String, String> getDefaultProfile(@NonNull final Context context) {
        final Map<String, String> map = new HashMap<>();
        map.put(SP_USER_ID,             SP_USERS_DEFAULT_USER_ID);
        map.put(SP_USER_SYNC_ID,        context.getString(R.string.fragment_settings_default_external_id));
        map.put(SP_USER_EMAIL,          context.getString(R.string.fragment_settings_default_email));
        map.put(SP_USER_FIRST_NAME,     context.getString(R.string.fragment_settings_default_first_name));
        map.put(SP_USER_LAST_NAME,      context.getString(R.string.fragment_settings_default_last_name));
        map.put(SP_USER_MIDDLE_NAME,    context.getString(R.string.fragment_settings_default_middle_name));
        map.put(SP_USER_MOCK_ENTRIES_DEFAULT, context.getString(R.string.fragment_settings_default_number_mock_entries));
        map.put(SP_USER_PROGRESS_NOTIFICATION_TIMER, context.getString(R.string.fragment_settings_default_progress_notification_timer));
        map.put(SP_USER_SYNC,           context.getString(R.string.fragment_settings_default_sync));
        map.put(SP_USER_SYNC_WIFI,      context.getString(R.string.fragment_settings_default_sync_wifi));
        map.put(SP_USER_SYNC_API_URL,   context.getString(R.string.fragment_settings_default_sync_api_url));
        map.put(SP_USER_SYNC_PENDING,   SP_USER_SYNC_PENDING_FALSE);

        return map;
    }

    /**
     * Get user profile in Map-format.
     * @param context   Context.
     * @param userId    User id.
     * @return          User profile.
     */
    @SuppressWarnings({"LineLength"})
    @NonNull
    public static Map<String, String> get(
            @NonNull final Context context,
            @NonNull final String userId) {

        final SharedPreferences sp = context.getSharedPreferences(
                getPreferencesName(userId),
                MODE_PRIVATE);

        final Map<String, String> defaultProfile = getDefaultProfile(context);

        final Map<String, String> map = new HashMap<>();
        map.put(SP_USER_ID,         userId);
        map.put(SP_USER_SYNC_ID,    sp.getString(SP_USER_SYNC_ID,       defaultProfile.get(SP_USER_SYNC_ID)));
        map.put(SP_USER_EMAIL,      sp.getString(SP_USER_EMAIL,         defaultProfile.get(SP_USER_EMAIL)));
        map.put(SP_USER_FIRST_NAME, sp.getString(SP_USER_FIRST_NAME,    defaultProfile.get(SP_USER_FIRST_NAME)));
        map.put(SP_USER_LAST_NAME,  sp.getString(SP_USER_LAST_NAME,     defaultProfile.get(SP_USER_LAST_NAME)));
        map.put(SP_USER_MIDDLE_NAME, sp.getString(SP_USER_MIDDLE_NAME,  defaultProfile.get(SP_USER_MIDDLE_NAME)));
        map.put(SP_USER_DESCRIPTION, sp.getString(SP_USER_DESCRIPTION,  defaultProfile.get(SP_USER_DESCRIPTION)));
        map.put(SP_USER_MOCK_ENTRIES_DEFAULT, sp.getString(SP_USER_MOCK_ENTRIES_DEFAULT, defaultProfile.get(SP_USER_MOCK_ENTRIES_DEFAULT)));
        map.put(SP_USER_PROGRESS_NOTIFICATION_TIMER, sp.getString(SP_USER_PROGRESS_NOTIFICATION_TIMER, defaultProfile.get(SP_USER_PROGRESS_NOTIFICATION_TIMER)));
        map.put(SP_USER_SYNC,       String.valueOf(sp.getBoolean(SP_USER_SYNC,      Boolean.parseBoolean(defaultProfile.get(SP_USER_SYNC)))));
        map.put(SP_USER_SYNC_WIFI,  String.valueOf(sp.getBoolean(SP_USER_SYNC_WIFI, Boolean.parseBoolean(defaultProfile.get(SP_USER_SYNC)))));
        map.put(SP_USER_SYNC_API_URL, sp.getString(SP_USER_SYNC_API_URL, defaultProfile.get(SP_USER_SYNC_API_URL)));
        map.put(SP_USER_SYNC_PENDING, sp.getString(SP_USER_SYNC_PENDING, defaultProfile.get(SP_USER_SYNC_PENDING)));

        return map;
    }

    /**
     * Get id of selected user.
     * @param context   Context.
     * @return          Id of selected user.
     */
    @Nullable
    public static String getSelected(@NonNull final Context context) {
        final SharedPreferences sp = context.getSharedPreferences(SP_MAIN, MODE_PRIVATE);
        return sp.getString(SP_USERS_SELECTED_ID, null);
    }

    /**
     * Get all user profiles from main preferences.
     * @param context   Context.
     * @return          All user profiles.
     */
    @NonNull
    public static Set<String> getProfiles(
            @NonNull final Context context) {

        final SharedPreferences sp = context.getSharedPreferences(SP_MAIN, MODE_PRIVATE);
        return sp.getStringSet(SP_USERS_SET, new HashSet<String>());
    }

    @NonNull
    public static String getPreferencesName(@NonNull final String userId) {
        return SP_USERS_FILENAME_PREFIX + "." + userId;
    }

    @NonNull
    private static String getNextUserId(@NonNull final Context context) {
        int number = Integer.parseInt(getIdCounter(context));
        final String nextNumber = String.valueOf(++number);
        setIdCounter(context, String.valueOf(++number));
        return nextNumber;
    }

    /**
     * @param context   Context.
     * @return          Number of mock entries for current user. Return 0, if error.
     */
    public static int getNumberMockEntriesForCurrentUser(@NonNull final Context context) {
        final String selected = getSelected(context);
        if (selected == null) {
            Log.e(TAG, "Id of selected user is null");
            return 0;
        }

        final SharedPreferences sp = context.getSharedPreferences(
                getPreferencesName(selected),
                MODE_PRIVATE);
        final String string = sp.getString(
                SP_USER_MOCK_ENTRIES_DEFAULT,
                context.getString(R.string.fragment_settings_default_number_mock_entries));
        return Integer.parseInt(string);
    }

    /**
     * @param context   Context.
     * @return          Progress notification timer for current user, ms. Return 0, if error.
     */
    public static int getProgressNotificationTimerForCurrentUser(@NonNull final Context context) {
        final String selected = getSelected(context);
        if (selected == null) {
            Log.e(TAG, "Id of selected user is null");
            return 0;
        }

        final SharedPreferences sp = context.getSharedPreferences(
                getPreferencesName(selected),
                MODE_PRIVATE);
        final String string = sp.getString(
                SP_USER_PROGRESS_NOTIFICATION_TIMER,
                context.getString(R.string.fragment_settings_default_progress_notification_timer));
        return Integer.parseInt(string);

    }

    /**
     * @param context   Context.
     * @return          Number of id counter
     */
    @Nullable
    private static String getIdCounter(@NonNull final Context context) {
        final SharedPreferences sp = context.getSharedPreferences(SP_MAIN, MODE_PRIVATE);
        return sp.getString(SP_USERS_ID_COUNTER, null);
    }

    /**
     * @param context   Context.
     * @return          Sync Wi-fi only status. Return true, if error.
     */
    public static boolean getSyncWifiOnlyForCurrentUser(@NonNull final Context context) {
        final String selected = getSelected(context);
        if (selected == null) {
            Log.e(TAG, "Id of selected user is null");
            return true;
        }

        final SharedPreferences sp = context.getSharedPreferences(
                getPreferencesName(selected),
                MODE_PRIVATE);
        return sp.getBoolean(SP_USER_SYNC_WIFI, true);
    }

    /**
     * @param context   Context.
     * @return          Api url for current user. Return null, if error.
     */
    @Nullable
    public static String getApiUrlForCurrentUser(@NonNull final Context context) {
        final String selected = getSelected(context);
        if (selected == null) {
            Log.e(TAG, "Id of selected user is null");
            return null;
        }

        final SharedPreferences sp = context.getSharedPreferences(
                getPreferencesName(selected),
                MODE_PRIVATE);
        return sp.getString(SP_USER_SYNC_API_URL, null);
    }

    /**
     * @param context   Context.
     * @return          Sync id for current user. Return null, if error.
     */
    @Nullable
    public static String getSyncIdForCurrentUser(@NonNull final Context context) {
        final String selected = getSelected(context);
        if (selected == null) {
            Log.e(TAG, "Id of selected user is null");
            return null;
        }

        final SharedPreferences sp = context.getSharedPreferences(
                getPreferencesName(selected),
                MODE_PRIVATE);
        return sp.getString(SP_USER_SYNC_ID, null);
    }

    /**
     * @param context   Context.
     * @return          Pending sync status for current user. Return null, if error.
     */
    @Nullable
    public static String getPendingSyncStatusForCurrentUser(@NonNull final Context context) {
        final String selected = getSelected(context);
        if (selected == null) {
            Log.e(TAG, "Id of selected user is null");
            return null;
        }

        final SharedPreferences sp = context.getSharedPreferences(
                getPreferencesName(selected),
                MODE_PRIVATE);
        return sp.getString(SP_USER_SYNC_PENDING, null);
    }


    /*
        Setters
     */

    /**
     * Add new user, from given profile. If profile is null, then create default profile.
     * Creating new preferences file, with default settings.
     * Add user info.
     * Set new user as selected.
     * Update main preferences settings.
     * @param context   Context.
     * @param profile   User profile.
     * @return Id of added user.
     */
    @SuppressWarnings({"PMD.AvoidReassigningParameters"})
    public static String add(
            @NonNull final Context context,
            @Nullable Map<String, String> profile) {

        if (profile == null) {
            profile = getDefaultProfile(context);
            profile.put(SP_USER_ID, getNextUserId(context));
        }

        // Create. Filename example: "Users.123"
        final SharedPreferences sp = context.getSharedPreferences(
                getPreferencesName(profile.get(SP_USER_ID)),
                MODE_PRIVATE);

        final SharedPreferences.Editor editor = sp.edit();

        /* User info */
        editor  .putString(SP_USER_ID,          profile.get(SP_USER_ID))
                .putString(SP_USER_SYNC_ID,     profile.get(SP_USER_SYNC_ID))
                .putString(SP_USER_EMAIL,       profile.get(SP_USER_EMAIL))
                .putString(SP_USER_FIRST_NAME,  profile.get(SP_USER_FIRST_NAME))
                .putString(SP_USER_LAST_NAME,   profile.get(SP_USER_LAST_NAME))
                .putString(SP_USER_MIDDLE_NAME, profile.get(SP_USER_MIDDLE_NAME))
                .putString(SP_USER_DESCRIPTION, profile.get(SP_USER_DESCRIPTION))
                .putString(SP_USER_MOCK_ENTRIES_DEFAULT, profile.get(SP_USER_MOCK_ENTRIES_DEFAULT))
                .putString(SP_USER_PROGRESS_NOTIFICATION_TIMER, profile.get(SP_USER_PROGRESS_NOTIFICATION_TIMER))
                .putBoolean(SP_USER_SYNC,       Boolean.parseBoolean(profile.get(SP_USER_SYNC)))
                .putBoolean(SP_USER_SYNC_WIFI,  Boolean.parseBoolean(profile.get(SP_USER_SYNC_WIFI)))
                .putString(SP_USER_SYNC_API_URL, profile.get(SP_USER_SYNC_API_URL))
                .putString(SP_USER_SYNC_PENDING, profile.get(SP_USER_SYNC_PENDING));

        /* Filter profiles */
        editor  .putString(SP_FILTER_PROFILE_DEFAULT, SpFilterProfiles.getDefaultProfile())
                .putString(SP_FILTER_PROFILE_MANUAL, SpFilterProfiles.getManualProfile())
                .putString(SP_FILTER_PROFILE_CURRENT, SpFilterProfiles.getDefaultProfile())
                .putString(SP_FILTER_PROFILE_SELECTED_ID, SP_FILTER_PROFILE_CURRENT_ID);

        /* Finish */
        editor  .putBoolean(SP_INITIALIZED, true)
                .apply();
        setSelected(context, profile.get(SP_USER_ID));
        addToProfiles(context, profile.get(SP_USER_ID));
        return profile.get(SP_USER_ID);
    }

    /**
     * Add user profile to main preferences profiles.
     * @param context   Context.
     * @param userId    User id.
     */
    private static void addToProfiles(
            @NonNull final Context context,
            @NonNull final String userId) {

        final Set<String> set = getProfiles(context);
        set.add(userId);
        updateProfiles(context, set);
    }

    /**
     * Update user profiles in main preferences.
     * @param context   Context.
     * @param profiles  User profiles.
     */
    private static void updateProfiles(
            @NonNull final Context context,
            @NonNull final Set<String> profiles) {

        final SharedPreferences sp = context.getSharedPreferences(SP_MAIN, MODE_PRIVATE);
        sp      .edit()
                .putStringSet(SP_USERS_SET, profiles)
                .apply();
    }

    /**
     * Set id of selected user to main preferences.
     * @param context   Context.
     * @param userId    User id.
     */
    public static void setSelected(
            @NonNull final Context context,
            @NonNull final String userId) {

        final SharedPreferences sp = context.getSharedPreferences(SP_MAIN, MODE_PRIVATE);
        sp      .edit()
                .putString(SP_USERS_SELECTED_ID, userId)
                .apply();
    }

    /**
     * Set number of id counter.
     * @param context   Context.
     * @param number    Number of id counter.
     */
    private static void setIdCounter(
            @NonNull final Context context,
            @NonNull final String number) {

        final SharedPreferences sp = context.getSharedPreferences(SP_MAIN, MODE_PRIVATE);
        sp      .edit()
                .putString(SP_USERS_ID_COUNTER, number)
                .apply();
    }

    /**
     * Set pending sync status for current user.
     * @param context   Context.
     * @param status    See:    {@link #SP_USER_SYNC_PENDING_TRUE},
     *                          {@link #SP_USER_SYNC_PENDING_FALSE},
     * @return True, if ok, otherwise false.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static boolean setPendingSyncStatusForCurrentUser(
            @NonNull final Context context,
            @NonNull final String status) {

        final String selected = getSelected(context);
        if (selected == null) {
            Log.e(TAG, "Id of selected user is null");
            return false;
        }

        final SharedPreferences sp = context.getSharedPreferences(
                getPreferencesName(selected),
                MODE_PRIVATE);

        sp      .edit()
                .putString(SP_USER_SYNC_PENDING, status)
                .apply();

        // Register / Unregister broadcast receiver manually
        if (SP_USER_SYNC_PENDING_TRUE.equals(status)) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(CONNECTIVITY_ACTION);
            context.registerReceiver(
                    NetworkBroadcastReceiver.getInstance(),
                    intentFilter);

        } else if (SP_USER_SYNC_PENDING_FALSE.equals(status)) {
            try {
                context.unregisterReceiver(NetworkBroadcastReceiver.getInstance());
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }

        return true;
    }

    /**
     * Delete user.
     * If deleted user is selected, then set default as selected.
     * If deleted user is default user, then denied.
     * @param context   Context.
     * @param userId    User id.
     * @return True, if ok, otherwise false.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static boolean delete(
            @NonNull final Context context,
            @NonNull final String userId) {

        final String selected = getSelected(context);
        if (selected == null) {
            Log.e(TAG, "Id of selected user is null");
            return false;
        }

        if (selected.equals(userId)) {
            setSelected(context, SP_USERS_DEFAULT_USER_ID);
        }

        if (SP_USERS_DEFAULT_USER_ID.equals(userId)) {
            return true;
        }

        // Remove from profiles
        final Set<String> profiles = getProfiles(context);
        final Iterator<String> iterator = profiles.iterator();
        while (iterator.hasNext()) {
            final String next = iterator.next();
            if (next.equals(userId)) {
                iterator.remove();
                break;
            }
        }
        updateProfiles(context, profiles);

        // Remove preference file.
        final SharedPreferences sp = context.getSharedPreferences(
                getPreferencesName(userId),
                MODE_PRIVATE);
        sp      .edit()
                .clear()
                .apply();

        // Remove database and update..
        context.deleteDatabase(userId);
        DbHelper.clearInstances();

        return true;
    }
}
