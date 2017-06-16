package com.gamaliev.notes.common.shared_prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.test.InstrumentationRegistry;

import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;
import static com.gamaliev.notes.common.shared_prefs.SpCommon.SP_INITIALIZED;
import static com.gamaliev.notes.common.shared_prefs.SpCommon.SP_MAIN;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.SP_USERS_ID_COUNTER;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.SP_USERS_SELECTED_ID;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.SP_USERS_SET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
public class SpCommonTest {

    @Test
    public void initSharedPreferences() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        SpCommon.initSharedPreferences(context);

        final SharedPreferences sp      = context.getSharedPreferences(SP_MAIN, MODE_PRIVATE);
        final Map<String, ?> all        = sp.getAll();
        final Boolean initialized       = (Boolean) all.get(SP_INITIALIZED);
        final Set<String> usersSet      = (HashSet<String>) all.get(SP_USERS_SET);
        final String usersSelectedId    = (String) all.get(SP_USERS_SELECTED_ID);
        final String usersIdCounter     = (String) all.get(SP_USERS_ID_COUNTER);

        assertTrue(initialized);
        assertTrue(usersSet.size() > 1);
        assertTrue(!usersSelectedId.isEmpty());
        assertTrue(!usersIdCounter.isEmpty());
    }

    @Test
    public void setString() throws Exception {
        final Context context   = InstrumentationRegistry.getTargetContext();
        final String prefsName  = "prefs_name";
        final String value      = "value";
        final String key        = "key";

        SpCommon.setString(context, prefsName, value, key);

        final SharedPreferences sp = context.getSharedPreferences(prefsName, MODE_PRIVATE);
        final String keyFromSp = sp.getString(value, null);

        assertEquals(key, keyFromSp);
    }

    @Test
    public void convertJsonToMap() throws Exception {
        final String json = "{one:\"1\", two: \"\", three: null}";
        final Map<String, String> mapExpected = new HashMap<>();
        mapExpected.put("one", "1");
        mapExpected.put("two", "");
        mapExpected.put("three", "null");

        final Map<String, String> mapActual = SpCommon.convertJsonToMap(json);

        // #1
        assertEquals(mapExpected, mapActual);

        final String wrongJson = "{one:\"1\", two: \"\", three: }";
        final Map<String, String> convertedWrongJson = SpCommon.convertJsonToMap(wrongJson);

        // #2
        assertNull(convertedWrongJson);
    }

    @Test
    public void convertMapToJson() throws Exception {
        final Map<String, String> map = new HashMap<>();
        map.put("one", "1");
        map.put("two", "");
        map.put("three", "null");
        final JSONObject jsonObject = new JSONObject(map);
        final String jsonExpected = jsonObject.toString();

        final String jsonActual = SpCommon.convertMapToJson(map);

        assertEquals(jsonExpected, jsonActual);
    }

    @Test
    public void convertEntryJsonToString() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String note = "{\"title\": \"Yelena Yeremeyeva\", "
                + "\"color\": \"#0010FF\", "
                + "\"imageUrl\": \"https:\\/\\/developer.android.com\\/images\\/brand\\/Android_Robot_100.png\", "
                + "\"description\": \"butterfly galaxy Peace Smile bubble cosmopolitan cosy Love \", "
                + "\"created\": \"2017-05-05T02:25:35+05:00\", "
                + "\"edited\": \"2017-05-05T02:25:35+05:00\", "
                + "\"viewed\": \"2017-05-08T02:25:35+05:00\"}";

        final String formattedNote = SpCommon.convertEntryJsonToString(context, note);

        // #1
        assertNotNull(formattedNote);
        assertTrue(!formattedNote.isEmpty());

        final String wrongJson = "123abc";
        final String wrongResult = SpCommon.convertEntryJsonToString(context, wrongJson);

        // #2
        assertNull(wrongResult);
    }
}