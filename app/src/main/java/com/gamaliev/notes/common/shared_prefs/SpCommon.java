package com.gamaliev.notes.common.shared_prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class SpCommon {

    /* Logger */
    private static final String TAG = SpCommon.class.getSimpleName();

    /* ... */
    public static final String SP_INITIALIZED = "initialized";
    public static final String SP_MAIN = "Main";


    /*
        Init
     */

    private SpCommon() {}


    /*
        ...
     */

    /**
     * Initialize application preferences on first start.
     * @param context Context.
     */
    public static void initSharedPreferences(
            @NonNull final Context context) {

        // Get main preferences.
        final SharedPreferences sp = context.getSharedPreferences(SP_MAIN, MODE_PRIVATE);

        // If app start first time.
        if (!sp.getBoolean(SP_INITIALIZED, false)) {

            // Add default user.
            final Map<String, String> map = SpUsers.getDefaultProfile();
            SpUsers.add(context, map);

            // Add mock data.
            SpMock.addMockData(context);

            // Mark initialized.
            sp      .edit()
                    .putBoolean(SP_INITIALIZED, true)
                    .apply();
        }
    }


    /*
        COMMON
     */

    /**
     * Set key-value pair to specified preferences.
     *
     * @param context   Context.
     * @param name      Name of preferences.
     * @param key       Key.
     * @param value     Value.
     */
    public static void setString(
            @NonNull final Context context,
            @NonNull final String name,
            @NonNull final String key,
            @NonNull final String value) {

        final SharedPreferences sp = context.getSharedPreferences(name, MODE_PRIVATE);
        sp      .edit()
                .putString(key, value)
                .apply();
    }

    /**
     * Get value from specified preferences, with specified key.
     *
     * @param context   Context.
     * @param name      Name of preferences.
     * @param key       Key.
     *
     * @return          Value.
     */
    @Nullable
    public static String getString(
            @NonNull final Context context,
            @NonNull final String name,
            @NonNull final String key) {

        final SharedPreferences sp = context.getSharedPreferences(name, MODE_PRIVATE);
        return sp.getString(key, null);
    }


    /*
        Converters
     */

    /**
     * Convert Json-format to Map-format.
     * @param json  Json-format.
     * @return      Map-format.
     */
    @Nullable
    public static Map<String, String> convertJsonToMap(
            @NonNull final String json) {

        //
        final Map<String, String> map = new HashMap<>();

        //
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(json);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return null;
        }

        //
        final Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            final String key = keys.next();
            map.put(key, jsonObject.optString(key, ""));
        }

        return map;
    }

    /**
     * Convert Map-format to Json-format.
     * @param map   Map-format.
     * @return      Json-format.
     */
    @Nullable
    public static String convertMapToJson(
            @NonNull final Map<String, String> map) {

        final JSONObject jsonObject = new JSONObject(map);
        return jsonObject.toString();
    }
}