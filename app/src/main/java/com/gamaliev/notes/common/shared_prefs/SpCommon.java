package com.gamaliev.notes.common.shared_prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.gamaliev.notes.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_COLOR;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_CREATED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_DESCRIPTION;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_EDITED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_IMAGE_URL;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_TITLE;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_VIEWED;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class SpCommon {

    /* Logger */
    private static final String TAG = SpCommon.class.getSimpleName();

    /* ... */
    static final String SP_INITIALIZED = "initialized";
    static final String SP_MAIN = "Main";


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

        final SharedPreferences sp = context.getSharedPreferences(SP_MAIN, MODE_PRIVATE);
        if (!sp.getBoolean(SP_INITIALIZED, false)) {
            final Map<String, String> map = SpUsers.getDefaultProfile(context);
            SpUsers.add(context, map);

            SpMock.addMockData(context);

            sp      .edit()
                    .putString(SpUsers.SP_USERS_ID_COUNTER, "5")
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
    @NonNull
    public static Map<String, String> convertJsonToMap(
            @NonNull final String json) {

        final Map<String, String> map = new HashMap<>();

        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(json);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return map;
        }

        final Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            final String key = keys.next();
            map.put(key, jsonObject.optString(key, ""));
        }

        return map;
    }

    /**
     * Convert Json-format to Formatted string with line breaks.
     * @param json  Json-format.
     * @return      Formatted string, with line breaks.
     */
    @NonNull
    public static String convertEntryJsonToString(
            @NonNull final Context context,
            @NonNull final String json) {

        final StringBuilder sb = new StringBuilder();

        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(json);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return "";
        }

        final String title = jsonObject.optString(LIST_ITEMS_COLUMN_TITLE, "");
        sb      .append(context.getString(R.string.fragment_dialog_conflict_select_body_title))
                .append(":\n")
                .append(title)
                .append("\n\n");

        final String description = jsonObject.optString(LIST_ITEMS_COLUMN_DESCRIPTION, "");
        sb      .append(context.getString(R.string.fragment_dialog_conflict_select_body_description))
                .append(":\n")
                .append(description)
                .append("\n\n");

        final String color = jsonObject.optString(LIST_ITEMS_COLUMN_COLOR, "");
        sb      .append(context.getString(R.string.fragment_dialog_conflict_select_body_color))
                .append(":\n")
                .append(color)
                .append("\n\n");

        final String imageUrl = jsonObject.optString(LIST_ITEMS_COLUMN_IMAGE_URL, "");
        sb      .append(context.getString(R.string.fragment_dialog_conflict_select_body_image_url))
                .append(":\n")
                .append(imageUrl)
                .append("\n\n");

        final String created = jsonObject.optString(LIST_ITEMS_COLUMN_CREATED, "");
        sb      .append(context.getString(R.string.fragment_dialog_conflict_select_body_created))
                .append(":\n")
                .append(created)
                .append("\n\n");

        final String edited = jsonObject.optString(LIST_ITEMS_COLUMN_EDITED, "");
        sb      .append(context.getString(R.string.fragment_dialog_conflict_select_body_edited))
                .append(":\n")
                .append(edited)
                .append("\n\n");

        final String viewed = jsonObject.optString(LIST_ITEMS_COLUMN_VIEWED, "");
        sb      .append(context.getString(R.string.fragment_dialog_conflict_select_body_viewed))
                .append(":\n")
                .append(viewed);

        return sb.toString();
    }

    /**
     * Convert Map-format to Json-format.
     * @param map   Map-format.
     * @return      Json-format.
     */
    @NonNull
    public static String convertMapToJson(
            @NonNull final Map<String, String> map) {

        final JSONObject jsonObject = new JSONObject(map);
        return jsonObject.toString();
    }
}