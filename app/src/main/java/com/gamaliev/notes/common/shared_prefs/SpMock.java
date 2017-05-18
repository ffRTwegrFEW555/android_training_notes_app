package com.gamaliev.notes.common.shared_prefs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.db.DbHelper;
import com.gamaliev.notes.list.db.ListDbMockHelper;
import com.gamaliev.notes.sync.db.SyncDbMockHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_CREATED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_EDITED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_TITLE;
import static com.gamaliev.notes.common.db.DbHelper.ORDER_ASCENDING;
import static com.gamaliev.notes.common.db.DbHelper.ORDER_ASC_DESC_DEFAULT;
import static com.gamaliev.notes.common.db.DbHelper.ORDER_DESCENDING;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_COLOR;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_CREATED;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_EDITED;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_ID;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_ORDER;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_ORDER_ASC;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_TITLE;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_VIEWED;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.SP_USER_DESCRIPTION;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.SP_USER_EMAIL;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.SP_USER_SYNC_ID;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.SP_USER_FIRST_NAME;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.SP_USER_ID;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.SP_USER_LAST_NAME;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.SP_USER_MIDDLE_NAME;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.SP_USER_MOCK_ENTRIES_DEFAULT;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.SP_USER_PROGRESS_NOTIF_TIMER;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.SP_USER_SYNC;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.SP_USER_SYNC_API_URL;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.SP_USER_SYNC_PENDING;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.SP_USER_SYNC_PENDING_FALSE;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.SP_USER_SYNC_WIFI;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class SpMock {

    /* Logger */
    private static final String TAG = SpMock.class.getSimpleName();

    /* ... */
    private static final String[][] SP_MOCK_FILTER_PROFILES = new String[][] {
            {"0",
                    "Without filter, title asc",
                    "",
                    "",
                    "",
                    "",
                    LIST_ITEMS_COLUMN_TITLE,
                    ORDER_ASC_DESC_DEFAULT},

            {"1",
                    "Dates, color, title asc",
                    "-53248",
                    "2017-05-01 00:00:00#2017-05-10 00:00:00",
                    "2017-05-01 00:00:00#2017-05-07 00:00:00",
                    "2017-05-04 00:00:00#2017-05-10 00:00:00",
                    LIST_ITEMS_COLUMN_TITLE,
                    ORDER_ASCENDING},

            {"2",
                    "Created desc, color",
                    "-16711824",
                    "2017-05-05 00:00:00#2017-05-10 00:00:00",
                    "",
                    "2017-05-01 00:00:00#2017-05-10 00:00:00",
                    LIST_ITEMS_COLUMN_CREATED,
                    ORDER_DESCENDING},

            {"3",
                    "Color, edited filter, edit asc.",
                    "-4096",
                    "",
                    "2017-05-04 00:00:00#2017-05-10 00:00:00",
                    "",
                    LIST_ITEMS_COLUMN_EDITED,
                    ORDER_ASCENDING},
            {"4",
                    "No color, title desc, edit.",
                    "",
                    "",
                    "2017-05-04 00:00:00#2017-05-10 00:00:00",
                    "",
                    LIST_ITEMS_COLUMN_TITLE,
                    ORDER_DESCENDING}
    };

    private static final String[][] SP_MOCK_USER_PROFILES = new String[][] {
            {"0",
                    "777",
                    "gamaliev-vadim@yandex.com",
                    "Vadim",
                    "Gamaliev",
                    "Rafisovich",
                    "Description description",
                    "100000",
                    "3000",
                    "true",
                    "false",
                    "https://notesbackend-yufimtsev.rhcloud.com/",
                    SP_USER_SYNC_PENDING_FALSE},

            {"1",
                    "778",
                    "info@gamaliev.com",
                    "User",
                    "Userov",
                    "Userovich",
                    "Description... 123",
                    "500",
                    "3000",
                    "true",
                    "false",
                    "https://notesbackend-yufimtsev.rhcloud.com/",
                    SP_USER_SYNC_PENDING_FALSE},
    };


    /*
        Init
     */

    private SpMock() {}


    /*
        ...
     */

    public static void addMockData(@NonNull final Context context) {

        int entriesCount = context.getResources().getInteger(R.integer.mock_items_number_start);
        boolean allProfiles = false;

        // Add mock users, mock filter profiles, mock entries.
        final Set<Map<String, String>> mockUserProfiles = SpMock.getMockUserProfiles();
        final Set<String> mockFilterProfiles = SpMock.getMockFilterProfiles();

        if (mockFilterProfiles == null) {
            return;
        }

        for (Map<String, String> userProfile : mockUserProfiles) {
            // Get user id.
            final String userId = userProfile.get(SpUsers.SP_USER_ID);

            // Add user.
            SpUsers.add(context, userProfile);

            // Add filter profiles to user.
            SpFilterProfiles.updateProfiles(
                    context,
                    userId,
                    mockFilterProfiles);

            // Delete some profiles on next users after first mock user.
            if (!allProfiles) {
                SpFilterProfiles.delete(context, userId, "3");
                SpFilterProfiles.delete(context, userId, "4");
            }
            allProfiles = true;

            // Create personal database, fill default and mock values.
            ListDbMockHelper.addMockEntries(
                    context,
                    entriesCount,
                    DbHelper.getInstance(context).getWritableDatabase(),
                    null,
                    false);
            entriesCount += entriesCount;

            /*// Add mock synchronization
            SyncDbMockHelper.addMockSync(context);*/
        }
    }


    /*
        PROFILES
     */

    /**
     * @return Set of mock filter profiles, in JSON-format.
     */
    @Nullable
    public static Set<String> getMockFilterProfiles() {
        final Set<String> set = new HashSet<>();

        JSONObject jsonObject;
        for (String[] entry : SP_MOCK_FILTER_PROFILES) {
            try {
                jsonObject = new JSONObject();
                jsonObject.put(SP_FILTER_ID,        entry[0]);
                jsonObject.put(SP_FILTER_TITLE,     entry[1]);
                jsonObject.put(SP_FILTER_COLOR,     entry[2]);
                jsonObject.put(SP_FILTER_CREATED,   entry[3]);
                jsonObject.put(SP_FILTER_EDITED,    entry[4]);
                jsonObject.put(SP_FILTER_VIEWED,    entry[5]);
                jsonObject.put(SP_FILTER_ORDER,     entry[6]);
                jsonObject.put(SP_FILTER_ORDER_ASC, entry[7]);

                set.add(jsonObject.toString());

            } catch (JSONException e) {
                Log.e(TAG, e.toString());
                return null;
            }
        }

        return set;
    }


    /*
        USERS
     */

    /**
     * @return Set of mock user profiles, in Map-format.
     */
    @NonNull
    public static Set<Map<String, String>> getMockUserProfiles() {
        final Set<Map<String, String>> set = new HashSet<>();

        Map<String, String> map;

        for (String[] entry : SP_MOCK_USER_PROFILES) {

            map = new HashMap<>();
            map.put(SP_USER_ID,             entry[0]);
            map.put(SP_USER_SYNC_ID,        entry[1]);
            map.put(SP_USER_EMAIL,          entry[2]);
            map.put(SP_USER_FIRST_NAME,     entry[3]);
            map.put(SP_USER_LAST_NAME,      entry[4]);
            map.put(SP_USER_MIDDLE_NAME,    entry[5]);
            map.put(SP_USER_DESCRIPTION,    entry[6]);
            map.put(SP_USER_MOCK_ENTRIES_DEFAULT, entry[7]);
            map.put(SP_USER_PROGRESS_NOTIF_TIMER, entry[8]);
            map.put(SP_USER_SYNC,           entry[9]);
            map.put(SP_USER_SYNC_WIFI,      entry[10]);
            map.put(SP_USER_SYNC_API_URL,   entry[11]);
            map.put(SP_USER_SYNC_PENDING,   entry[12]);

            set.add(map);
        }

        return set;
    }
}
