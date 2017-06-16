package com.gamaliev.notes.common.shared_prefs;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static com.gamaliev.notes.UtilsTest.clearUserPrefs;
import static com.gamaliev.notes.UtilsTest.initDefaultPrefs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
public class SpUsersTest {

    /*
        Init
     */

    @Before
    public void before() throws Exception {
        initDefaultPrefs();
    }


    /*
        Tests
     */

    @Test
    public void getDefaultProfile() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final Map<String, String> defaultProfile = SpUsers.getDefaultProfile(context);

        assertNotNull(defaultProfile);
        assertTrue(defaultProfile.size() > 0);
    }

    @Test
    public void get() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final Map<String, String> profile =
                SpUsers.get(context, SpUsers.SP_USERS_DEFAULT_USER_ID);

        assertNotNull(profile);
        assertTrue(profile.size() > 0);
    }

    @Test
    public void getSelected() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "123";

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);
        final String selectedUserId = SpUsers.getSelected(context);

        assertEquals(userId, selectedUserId);
    }

    @Test
    public void getProfiles() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final Set<String> profiles = SpUsers.getProfiles(context);

        assertNotNull(profiles);
        assertTrue(profiles.size() > 0);
    }

    @Test
    public void getPreferencesName() throws Exception {
        final String name = "123";
        final String nameFromSp = SpUsers.getPreferencesName(name);

        assertNotNull(nameFromSp);
        assertTrue(nameFromSp.length() >= name.length());
    }

    @Test
    public void getNumberMockEntriesForCurrentUser() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final int n = SpUsers.getNumberMockEntriesForCurrentUser(context);

        assertTrue(n > 0);
    }

    @Test
    public void getProgressNotificationTimerForCurrentUser() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final int n = SpUsers.getProgressNotificationTimerForCurrentUser(context);

        assertTrue(n > 0);
    }

    @Test
    public void getSyncWifiOnlyForCurrentUser() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        SpUsers.getSyncWifiOnlyForCurrentUser(context);
    }

    @Test
    public void getApiUrlForCurrentUser() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();

        final String apiUrl = SpUsers.getApiUrlForCurrentUser(context);

        assertNotNull(apiUrl);
    }

    @Test
    public void getSyncIdForCurrentUser() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();

        final String userSyncId = SpUsers.getSyncIdForCurrentUser(context);

        assertNotNull(userSyncId);
    }

    @Test
    public void getPendingSyncStatusForCurrentUser() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();

        final String status = SpUsers.getPendingSyncStatusForCurrentUser(context);

        assertNotNull(status);
    }

    @Test
    public void add() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String syncId = "777";
        final String userId = "123";

        clearUserPrefs(userId);
        final Map<String, String> profile = SpUsers.getDefaultProfile(context);
        profile.put(SpUsers.SP_USER_SYNC_ID, syncId);
        profile.put(SpUsers.SP_USER_ID, userId);
        SpUsers.add(context, profile);
        SpUsers.setSelected(context, userId);
        final String syncIdFromSp = SpUsers.getSyncIdForCurrentUser(context);

        assertEquals(syncId, syncIdFromSp);
    }

    @Test
    public void setSelected() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "123";

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);
        final String selectedUserId = SpUsers.getSelected(context);

        assertEquals(userId, selectedUserId);
    }

    @Test
    public void setPendingSyncStatusForCurrentUser() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String statusA = "statusA";
        final String statusB = "statusB";

        SpUsers.setPendingSyncStatusForCurrentUser(context, statusA);
        final String statusAFromSp = SpUsers.getPendingSyncStatusForCurrentUser(context);

        // #
        assertEquals(statusA, statusAFromSp);

        SpUsers.setPendingSyncStatusForCurrentUser(context, statusB);
        final String statusBFromSp = SpUsers.getPendingSyncStatusForCurrentUser(context);

        // #
        assertEquals(statusB, statusBFromSp);
    }

    @Test
    public void delete() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String syncId = "777";
        final String userId = "123";

        clearUserPrefs(userId);
        final Map<String, String> profile = SpUsers.getDefaultProfile(context);
        profile.put(SpUsers.SP_USER_SYNC_ID, syncId);
        profile.put(SpUsers.SP_USER_ID, userId);
        SpUsers.add(context, profile);
        SpUsers.setSelected(context, userId);
        final String syncIdFromSp = SpUsers.getSyncIdForCurrentUser(context);

        // #
        assertEquals(syncId, syncIdFromSp);

        final Set<String> profiles = SpUsers.getProfiles(context);
        int profilesSize = profiles.size();
        boolean deleteResult = SpUsers.delete(context, userId);

        // #
        assertTrue(deleteResult);

        final String selectedUserId = SpUsers.getSelected(context);

        // #
        assertNotEquals(userId, selectedUserId);

        final Set<String> profilesAgain = SpUsers.getProfiles(context);
        int profilesSizeAgain = profilesAgain.size();

        // #
        assertTrue(profilesSizeAgain == profilesSize - 1);

        final String syncIdFromSpAgain = SpUsers.getSyncIdForCurrentUser(context);

        // #
        assertNotEquals(syncId, syncIdFromSpAgain);
    }
}