package com.gamaliev.notes.common.shared_prefs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.gamaliev.notes.UtilsTest.clearUserPrefs;
import static com.gamaliev.notes.UtilsTest.initDefaultPrefs;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_ID;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_PROFILE_CURRENT_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
public class SpFilterProfilesTest {

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
        final String notExpected = "{}";
        final String defaultProfile = SpFilterProfiles.getDefaultProfile();

        assertNotEquals(notExpected, defaultProfile);
    }

    @Test
    public void getManualProfile() throws Exception {
        final String notExpected = "{}";
        final String manualProfile = SpFilterProfiles.getManualProfile();

        assertNotEquals(notExpected, manualProfile);
    }

    @Test
    public void get() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "777";
        final Map<String, String> profile = getNewProfileOne();

        clearUserPrefs(userId);
        final String profileId = SpFilterProfiles.add(context, userId, profile);
        final String profileJsonFromSp = SpFilterProfiles.get(context, userId, profileId);

        // #1
        assertNotNull(profileJsonFromSp);

        final Map<String, String> converted = SpCommon.convertJsonToMap(profileJsonFromSp);

        // #2
        assertEquals(profile, converted);
    }

    @Test
    public void getSelectedIdForCurrentUser() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String profileId = "777";

        SpFilterProfiles.setSelectedForCurrentUser(context, profileId);
        final String selectedProfileId = SpFilterProfiles.getSelectedIdForCurrentUser(context);

        assertEquals(profileId, selectedProfileId);
    }

    @Test
    public void getSelectedId() throws Exception {
        final Context context   = InstrumentationRegistry.getTargetContext();
        final String userId     = "111";
        final String profileId  = "777";

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);
        SpFilterProfiles.setSelectedForCurrentUser(context, profileId);
        final String selectedProfileId = SpFilterProfiles.getSelectedId(context, userId);

        assertEquals(profileId, selectedProfileId);
    }

    @Test
    public void getSelectedForCurrentUser() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final Map<String, String> profile = getNewProfileOne();

        final String profileId = SpFilterProfiles.addForCurrentUser(context, profile);

        // #1
        assertNotNull(profileId);

        SpFilterProfiles.setSelectedForCurrentUser(context, profileId);
        final String selectedProfileJson = SpFilterProfiles.getSelectedForCurrentUser(context);

        // #2
        assertNotNull(selectedProfileJson);

        final Map<String, String> converted = SpCommon.convertJsonToMap(selectedProfileJson);

        // #3
        assertEquals(profile, converted);
    }

    @Test
    public void getSelected() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "111";
        final Map<String, String> profile = getNewProfileOne();

        clearUserPrefs(userId);
        final String profileId = SpFilterProfiles.add(context, userId, profile);

        // #1
        assertNotNull(profileId);

        SpUsers.setSelected(context, userId);
        SpFilterProfiles.setSelectedForCurrentUser(context, profileId);
        final String selectedProfileJson = SpFilterProfiles.getSelected(context, userId);

        // #2
        assertNotNull(selectedProfileJson);

        final Map<String, String> converted = SpCommon.convertJsonToMap(selectedProfileJson);

        // #3
        assertEquals(profile, converted);
    }

    @Test
    public void getProfilesForCurrentUser() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final Map<String, String> profile = getNewProfileOne();

        final Set<String> profiles = SpFilterProfiles.getProfilesForCurrentUser(context);
        final String newProfileId = SpFilterProfiles.addForCurrentUser(context, profile);
        final String selectedUserId = SpUsers.getSelected(context);

        // #1
        assertNotNull(profiles);
        assertNotNull(newProfileId);
        assertNotNull(selectedUserId);

        final String addedProfile = SpFilterProfiles.get(context, selectedUserId, newProfileId);

        // #2
        assertNotNull(addedProfile);

        profiles.add(addedProfile);
        final Set<String> profilesUpdated = SpFilterProfiles.getProfilesForCurrentUser(context);

        // #3
        assertEquals(profiles, profilesUpdated);
    }

    @Test
    public void getProfiles() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final Map<String, String> profile = getNewProfileOne();

        final String selectedUserId = SpUsers.getSelected(context);

        // #1
        assertNotNull(selectedUserId);

        final Set<String> profiles = SpFilterProfiles.getProfiles(context, selectedUserId);
        final String newProfileId = SpFilterProfiles.addForCurrentUser(context, profile);

        // #2
        assertNotNull(profiles);
        assertNotNull(newProfileId);

        final String addedProfile = SpFilterProfiles.get(context, selectedUserId, newProfileId);

        // #3
        assertNotNull(addedProfile);

        profiles.add(addedProfile);
        final Set<String> profilesUpdated = SpFilterProfiles.getProfiles(context, selectedUserId);

        // #4
        assertEquals(profiles, profilesUpdated);
    }

    @Test
    public void addForCurrentUser() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final Map<String, String> profile = getNewProfileOne();

        final String selectedUserId = SpUsers.getSelected(context);

        // #1
        assertNotNull(selectedUserId);

        final Set<String> profiles = SpFilterProfiles.getProfiles(context, selectedUserId);
        final String newProfileId = SpFilterProfiles.addForCurrentUser(context, profile);

        // #2
        assertNotNull(profiles);
        assertNotNull(newProfileId);

        final String addedProfile = SpFilterProfiles.get(context, selectedUserId, newProfileId);

        // #3
        assertNotNull(addedProfile);

        profiles.add(addedProfile);
        final Set<String> profilesUpdated = SpFilterProfiles.getProfiles(context, selectedUserId);

        // #4
        assertEquals(profiles, profilesUpdated);
    }

    @Test
    public void add() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "111";
        final Map<String, String> profile = getNewProfileOne();

        clearUserPrefs(userId);
        final String profileId = SpFilterProfiles.add(context, userId, profile);

        // #1
        assertNotNull(profileId);

        SpUsers.setSelected(context, userId);
        SpFilterProfiles.setSelectedForCurrentUser(context, profileId);
        final String selectedProfileJson = SpFilterProfiles.getSelected(context, userId);

        // #2
        assertNotNull(selectedProfileJson);

        final Map<String, String> converted = SpCommon.convertJsonToMap(selectedProfileJson);

        // #3
        assertEquals(profile, converted);
    }

    @Test
    public void setSelectedForCurrentUser() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "111";
        final Map<String, String> profile = getNewProfileOne();

        clearUserPrefs(userId);
        final String profileId = SpFilterProfiles.add(context, userId, profile);

        // #1
        assertNotNull(profileId);

        SpUsers.setSelected(context, userId);
        SpFilterProfiles.setSelectedForCurrentUser(context, profileId);
        final String selectedProfileJson = SpFilterProfiles.getSelected(context, userId);

        // #2
        assertNotNull(selectedProfileJson);

        final Map<String, String> converted = SpCommon.convertJsonToMap(selectedProfileJson);

        // #3
        assertEquals(profile, converted);
    }

    @Test
    public void setSelected() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "111";
        final Map<String, String> profile = getNewProfileOne();

        clearUserPrefs(userId);
        final String profileId = SpFilterProfiles.add(context, userId, profile);

        // #1
        assertNotNull(profileId);

        SpFilterProfiles.setSelected(context, userId, profileId);
        final String selectedProfileJson = SpFilterProfiles.getSelected(context, userId);

        // #2
        assertNotNull(selectedProfileJson);

        final Map<String, String> converted = SpCommon.convertJsonToMap(selectedProfileJson);

        // #3
        assertEquals(profile, converted);
    }

    @Test
    public void updateCurrentForCurrentUser() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "111";
        final Map<String, String> profile = getNewProfileOne();
        final Map<String, String> profileNew = getNewProfileTwo();

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);
        SpFilterProfiles.setSelectedForCurrentUser(context, SP_FILTER_PROFILE_CURRENT_ID);
        boolean updateResult = SpFilterProfiles.updateCurrentForCurrentUser(context, profile);

        // #
        assertTrue(updateResult);

        final String selectedProfile = SpFilterProfiles.getSelected(context, userId);

        // #
        assertNotNull(selectedProfile);

        final Map<String, String> converted = SpCommon.convertJsonToMap(selectedProfile);

        // #
        assertEquals(profile, converted);

        boolean updateResult2 = SpFilterProfiles.updateCurrentForCurrentUser(context, profileNew);

        // #
        assertTrue(updateResult2);

        final String selectedProfileAgain = SpFilterProfiles.getSelected(context, userId);

        // #
        assertNotNull(selectedProfileAgain);

        final Map<String, String> convertedAgain = SpCommon.convertJsonToMap(selectedProfileAgain);

        // #
        assertEquals(profileNew, convertedAgain);
    }

    @Test
    public void updateCurrent() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "111";
        final Map<String, String> profile = getNewProfileOne();
        final Map<String, String> profileNew = getNewProfileTwo();

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);
        SpFilterProfiles.setSelectedForCurrentUser(context, SP_FILTER_PROFILE_CURRENT_ID);
        SpFilterProfiles.updateCurrent(context, userId, profile);

        final String selectedProfile = SpFilterProfiles.getSelected(context, userId);

        // #
        assertNotNull(selectedProfile);

        final Map<String, String> converted = SpCommon.convertJsonToMap(selectedProfile);

        // #
        assertEquals(profile, converted);

        SpFilterProfiles.updateCurrent(context, userId, profileNew);
        final String selectedProfileAgain = SpFilterProfiles.getSelected(context, userId);

        // #
        assertNotNull(selectedProfileAgain);

        final Map<String, String> convertedAgain = SpCommon.convertJsonToMap(selectedProfileAgain);

        // #
        assertEquals(profileNew, convertedAgain);
    }

    @Test
    public void resetCurrent() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "111";
        final Map<String, String> profile = getNewProfileOne();

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);
        SpFilterProfiles.setSelectedForCurrentUser(context, SP_FILTER_PROFILE_CURRENT_ID);
        SpFilterProfiles.updateCurrent(context, userId, profile);

        final String selectedProfile = SpFilterProfiles.getSelected(context, userId);

        // #
        assertNotNull(selectedProfile);

        final Map<String, String> converted = SpCommon.convertJsonToMap(selectedProfile);

        // #
        assertEquals(profile, converted);

        SpFilterProfiles.resetCurrent(context, userId);

        final String selectedProfileAgain = SpFilterProfiles.getSelected(context, userId);

        // #
        assertNotNull(selectedProfile);
        assertEquals(SpFilterProfiles.getDefaultProfile(), selectedProfileAgain);
    }

    @Test
    public void updateProfiles() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "111";
        final Set<String> profiles = SpMock.getMockFilterProfiles();

        // #
        assertNotNull(profiles);

        clearUserPrefs(userId);
        SpUsers.setSelected(context, userId);
        SpFilterProfiles.updateProfiles(context, userId, profiles);
        final Set<String> profilesFromSp = SpFilterProfiles.getProfiles(context, userId);

        // #
        assertEquals(profiles, profilesFromSp);

        profiles.add("New profile");
        SpFilterProfiles.updateProfiles(context, userId, profiles);
        final Set<String> profilesFromSpAgain = SpFilterProfiles.getProfiles(context, userId);

        // #
        assertEquals(profiles, profilesFromSpAgain);
        assertNotEquals(profiles, profilesFromSp);
    }

    @Test
    public void deleteForCurrentUser() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "111";
        final Set<String> profiles = SpMock.getMockFilterProfiles();

        // #
        assertNotNull(profiles);

        clearUserPrefs(userId);
        final Iterator<String> iterator = profiles.iterator();
        final String nextProfile = iterator.next();
        final Map<String, String> profileMap = SpCommon.convertJsonToMap(nextProfile);

        // #
        assertNotNull(profileMap);

        final String nextProfileId = profileMap.get(SP_FILTER_ID);
        SpUsers.setSelected(context, userId);
        SpFilterProfiles.updateProfiles(context, userId, profiles);
        SpFilterProfiles.deleteForCurrentUser(context, nextProfileId);
        final Set<String> profilesFromSp = SpFilterProfiles.getProfiles(context, userId);

        // #
        assertNotEquals(profiles, profilesFromSp);

        iterator.remove();

        // #
        assertEquals(profiles, profilesFromSp);
    }

    @Test
    public void delete() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String userId = "111";
        final Set<String> profiles = SpMock.getMockFilterProfiles();

        // #
        assertNotNull(profiles);

        clearUserPrefs(userId);
        final Iterator<String> iterator = profiles.iterator();
        final String nextProfile = iterator.next();
        final Map<String, String> profileMap = SpCommon.convertJsonToMap(nextProfile);

        // #
        assertNotNull(profileMap);

        final String nextProfileId = profileMap.get(SP_FILTER_ID);
        SpUsers.setSelected(context, userId);
        SpFilterProfiles.updateProfiles(context, userId, profiles);
        SpFilterProfiles.delete(context, userId, nextProfileId);
        final Set<String> profilesFromSp = SpFilterProfiles.getProfiles(context, userId);

        // #
        assertNotEquals(profiles, profilesFromSp);

        iterator.remove();

        // #
        assertEquals(profiles, profilesFromSp);
    }


    /*
        Utils
     */

    @NonNull
    private static Map<String, String> getNewProfileOne() {
        final Map<String, String> profile = new HashMap<>();
        profile.put("one", "1");
        profile.put("two", "2");
        return profile;
    }


    @NonNull
    private static Map<String, String> getNewProfileTwo() {
        final Map<String, String> profileNew = new HashMap<>();
        profileNew.put("one", "3");
        profileNew.put("two", "4");
        return profileNew;
    }
}