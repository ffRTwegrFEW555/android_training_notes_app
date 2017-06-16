package com.gamaliev.notes.common.rest;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

import retrofit2.Response;

import static com.gamaliev.notes.common.rest.NoteApiUtils.API_KEY_DATA;
import static com.gamaliev.notes.common.rest.NoteApiUtils.API_KEY_ID;
import static com.gamaliev.notes.common.rest.NoteApiUtils.API_KEY_STATUS;
import static com.gamaliev.notes.common.rest.NoteApiUtils.API_STATUS_ERROR;
import static com.gamaliev.notes.common.rest.NoteApiUtils.API_STATUS_OK;
import static com.gamaliev.notes.common.shared_prefs.SpCommon.convertJsonToMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
@SuppressWarnings("NullableProblems")
public class NoteApiUtilsTest {

    /* ... */
    @NonNull private static NoteApi sNoteApi;
    @NonNull private NotesHttpServerTest mNotesHttpServerTest;


    /*
        Init
     */

    @BeforeClass
    public static void beforeClass() throws Exception {
        final NoteApi noteApi = NoteApiUtils.newInstance(null);
        if (noteApi == null) {
            throw new IllegalArgumentException("NoteApi is null.");
        }
        sNoteApi = noteApi;
    }

    @Before
    public void before() throws Exception {
        mNotesHttpServerTest = NotesHttpServerTest.newInstance(Integer.parseInt(NoteApiUtils.TEST_PORT));
        mNotesHttpServerTest.startServer();
    }

    @After
    public void after() throws Exception {
        mNotesHttpServerTest.stopServer();
    }


    /*
        Tests
     */

    @Test
    public void getAll() throws Exception {
        final String userId = "123";
        final String noteOne = getNoteOneInstance();
        final String noteTwo = getNoteTwoInstance();

        // Add to server.
        sNoteApi.add(userId, noteOne).execute();
        sNoteApi.add(userId, noteTwo).execute();

        // Response 200 OK.
        final Response<String> response = sNoteApi.getAll(userId).execute();
        if (!response.isSuccessful()) {
            throw new Exception("Response is not successful.");
        }

        final JSONObject body = new JSONObject(response.body());

        // Status OK.
        final String status = body.optString(API_KEY_STATUS);
        if (!status.equalsIgnoreCase(API_STATUS_OK)) {
            throw new Exception("Body status is not ok.");
        }

        // Data OK.
        final JSONArray data = body.getJSONArray(API_KEY_DATA);
        if (data == null || data.length() != 2) {
            throw new Exception("Data is invalid.");
        }

        // Local entries preparing.
        final Map<String, String> noteOneMap = convertJsonToMap(noteOne);
        final Map<String, String> noteTwoMap = convertJsonToMap(noteTwo);

        // Server entries preparing.
        final JSONObject noteOneSrv = data.getJSONObject(0);
        final JSONObject noteTwoSrv = data.getJSONObject(1);
        noteOneSrv.remove(API_KEY_ID);
        noteTwoSrv.remove(API_KEY_ID);
        final Map<String, String> noteOneSrvMap = convertJsonToMap(noteOneSrv.toString());
        final Map<String, String> noteTwoSrvMap = convertJsonToMap(noteTwoSrv.toString());

        // Equals entries OK.
        assertNotNull(noteOneSrvMap);
        if (noteOneSrvMap.get("title").equals("Yelena Yeremeyeva")) {
            assertEquals(noteOneMap, noteOneSrvMap);
            assertEquals(noteTwoMap, noteTwoSrvMap);
        } else {
            assertEquals(noteOneMap, noteTwoSrvMap);
            assertEquals(noteTwoMap, noteOneSrvMap);
        }
    }

    @Test
    public void get() throws Exception {
        final String userId = "123";
        final String note = getNoteOneInstance();

        /*
            Add to server.
         */

        final Response<String> addResponse = sNoteApi.add(userId, note).execute();

        // Response 200 OK.
        if (!addResponse.isSuccessful()) {
            throw new Exception("Response is not successful.");
        }

        final JSONObject addBody = new JSONObject(addResponse.body());

        // Status OK.
        final String addStatus = addBody.optString(API_KEY_STATUS);
        if (!addStatus.equalsIgnoreCase(API_STATUS_OK)) {
            throw new Exception("Body status is not ok.");
        }

        // Data OK.
        final String newId = addBody.optString(API_KEY_DATA);
        if (Integer.parseInt(newId) <= 0) {
            throw new Exception("Body status is not ok.");
        }


        /*
            Get from server.
         */

        final Response<String> getResponse = sNoteApi.get(userId, newId).execute();

        // Response 200 OK.
        if (!getResponse.isSuccessful()) {
            throw new Exception("Response is not successful.");
        }

        final JSONObject getBody = new JSONObject(getResponse.body());

        // Status OK.
        final String getStatus = getBody.optString(API_KEY_STATUS);
        if (!getStatus.equalsIgnoreCase(API_STATUS_OK)) {
            throw new Exception("Body status is not ok.");
        }

        // Data OK.
        final JSONObject noteSrv = getBody.getJSONObject(API_KEY_DATA);
        if (noteSrv == null || noteSrv.toString().isEmpty()) {
            throw new Exception("Data is invalid.");
        }

        // Local entries preparing.
        final Map<String, String> noteMap = convertJsonToMap(note);

        // Server entries preparing.
        noteSrv.remove(API_KEY_ID);
        final Map<String, String> noteSrvMap = convertJsonToMap(noteSrv.toString());

        // Equals entries OK.
        assertEquals(noteMap, noteSrvMap);


        /*
            Get from server. Wrong id.
         */

        final Response<String> getWrongResponse = sNoteApi.get(userId, "777123").execute();

        // Response 200 OK.
        if (!getWrongResponse.isSuccessful()) {
            throw new Exception("Response is not successful.");
        }

        final JSONObject getWrongBody = new JSONObject(getWrongResponse.body());

        // Status ERROR.
        final String getWrongStatus = getWrongBody.optString(API_KEY_STATUS);
        assertEquals(API_STATUS_ERROR, getWrongStatus);
    }

    @Test
    public void add() throws Exception {
        final String userId = "123";
        final String noteOne = getNoteOneInstance();
        final String noteTwo = getNoteTwoInstance();

        // Add to server.
        sNoteApi.add(userId, noteOne).execute();
        sNoteApi.add(userId, noteTwo).execute();

        // Response 200 OK.
        final Response<String> response = sNoteApi.getAll(userId).execute();
        if (!response.isSuccessful()) {
            throw new Exception("Response is not successful.");
        }

        final JSONObject body = new JSONObject(response.body());

        // Status OK.
        final String status = body.optString(API_KEY_STATUS);
        if (!status.equalsIgnoreCase(API_STATUS_OK)) {
            throw new Exception("Body status is not ok.");
        }

        // Data OK.
        final JSONArray data = body.getJSONArray(API_KEY_DATA);
        if (data == null || data.length() != 2) {
            throw new Exception("Data is invalid.");
        }

        // Local entries preparing.
        final Map<String, String> noteOneMap = convertJsonToMap(noteOne);
        final Map<String, String> noteTwoMap = convertJsonToMap(noteTwo);

        // Server entries preparing.
        final JSONObject noteOneSrv = data.getJSONObject(0);
        final JSONObject noteTwoSrv = data.getJSONObject(1);
        noteOneSrv.remove(API_KEY_ID);
        noteTwoSrv.remove(API_KEY_ID);
        final Map<String, String> noteOneSrvMap = convertJsonToMap(noteOneSrv.toString());
        final Map<String, String> noteTwoSrvMap = convertJsonToMap(noteTwoSrv.toString());

        // Equals entries OK.
        assertNotNull(noteOneSrvMap);
        if (noteOneSrvMap.get("title").equals("Yelena Yeremeyeva")) {
            assertEquals(noteOneMap, noteOneSrvMap);
            assertEquals(noteTwoMap, noteTwoSrvMap);
        } else {
            assertEquals(noteOneMap, noteTwoSrvMap);
            assertEquals(noteTwoMap, noteOneSrvMap);
        }
    }

    @Test
    public void update() throws Exception {
        final String userId = "123";
        final String note = getNoteOneInstance();
        final String updatedNote = getNoteTwoInstance();

        /*
            Add to server.
         */

        final Response<String> addResponse = sNoteApi.add(userId, note).execute();

        // Response 200 OK.
        if (!addResponse.isSuccessful()) {
            throw new Exception("Response is not successful.");
        }

        final JSONObject addBody = new JSONObject(addResponse.body());

        // Status OK.
        final String addStatus = addBody.optString(API_KEY_STATUS);
        if (!addStatus.equalsIgnoreCase(API_STATUS_OK)) {
            throw new Exception("Body status is not ok.");
        }

        // Data OK.
        final String newId = addBody.optString(API_KEY_DATA);
        if (Integer.parseInt(newId) <= 0) {
            throw new Exception("Body status is not ok.");
        }


        /*
            Get from server.
         */

        final Response<String> getResponse = sNoteApi.get(userId, newId).execute();

        // Response 200 OK.
        if (!getResponse.isSuccessful()) {
            throw new Exception("Response is not successful.");
        }

        final JSONObject getBody = new JSONObject(getResponse.body());

        // Status OK.
        final String getStatus = getBody.optString(API_KEY_STATUS);
        if (!getStatus.equalsIgnoreCase(API_STATUS_OK)) {
            throw new Exception("Body status is not ok.");
        }

        // Data OK.
        final JSONObject noteSrv = getBody.getJSONObject(API_KEY_DATA);
        if (noteSrv == null || noteSrv.toString().isEmpty()) {
            throw new Exception("Data is invalid.");
        }

        // Local entries preparing.
        final Map<String, String> noteMap = convertJsonToMap(note);

        // Server entries preparing.
        noteSrv.remove(API_KEY_ID);
        final Map<String, String> noteSrvMap = convertJsonToMap(noteSrv.toString());

        // Equals entries OK.
        assertEquals(noteMap, noteSrvMap);


        /*
            Update on server.
         */

        final Response<String> updateResponse =
                sNoteApi.update(userId, newId, updatedNote).execute();

        // Response 200 OK.
        if (!updateResponse.isSuccessful()) {
            throw new Exception("Response is not successful.");
        }

        final JSONObject updateBody = new JSONObject(updateResponse.body());

        // Status OK.
        final String updateStatus = updateBody.optString(API_KEY_STATUS);
        assertEquals(API_STATUS_OK, updateStatus);


        /*
            Get updated
         */

        final Response<String> getUpdatedResponse = sNoteApi.get(userId, newId).execute();

        // Response 200 OK.
        if (!getUpdatedResponse.isSuccessful()) {
            throw new Exception("Response is not successful.");
        }

        final JSONObject getUpdatedBody = new JSONObject(getUpdatedResponse.body());

        // Status OK.
        final String getUpdatedStatus = getUpdatedBody.optString(API_KEY_STATUS);
        if (!getUpdatedStatus.equalsIgnoreCase(API_STATUS_OK)) {
            throw new Exception("Body status is not ok.");
        }

        // Data OK.
        final JSONObject updatedNoteSrv = getUpdatedBody.getJSONObject(API_KEY_DATA);
        if (updatedNoteSrv == null || updatedNoteSrv.toString().isEmpty()) {
            throw new Exception("Data is invalid.");
        }

        // Local entries preparing.
        final Map<String, String> updatedNoteMap = convertJsonToMap(updatedNote);

        // Server entries preparing.
        updatedNoteSrv.remove(API_KEY_ID);
        final Map<String, String> updatedNoteSrvMap = convertJsonToMap(updatedNoteSrv.toString());

        // Equals entries OK.
        assertEquals(updatedNoteMap, updatedNoteSrvMap);
    }

    @Test
    public void delete() throws Exception {
        final String userId = "123";
        final String note = getNoteOneInstance();

        /*
            Add to server.
         */

        final Response<String> addResponse = sNoteApi.add(userId, note).execute();

        // Response 200 OK.
        if (!addResponse.isSuccessful()) {
            throw new Exception("Response is not successful.");
        }

        final JSONObject addBody = new JSONObject(addResponse.body());

        // Status OK.
        final String addStatus = addBody.optString(API_KEY_STATUS);
        if (!addStatus.equalsIgnoreCase(API_STATUS_OK)) {
            throw new Exception("Body status is not ok.");
        }

        // Data OK.
        final String newId = addBody.optString(API_KEY_DATA);
        if (Integer.parseInt(newId) <= 0) {
            throw new Exception("Body status is not ok.");
        }


        /*
            Get from server.
         */

        final Response<String> getResponse = sNoteApi.get(userId, newId).execute();

        // Response 200 OK.
        if (!getResponse.isSuccessful()) {
            throw new Exception("Response is not successful.");
        }

        final JSONObject getBody = new JSONObject(getResponse.body());

        // Status OK.
        final String getStatus = getBody.optString(API_KEY_STATUS);
        if (!getStatus.equalsIgnoreCase(API_STATUS_OK)) {
            throw new Exception("Body status is not ok.");
        }

        // Data OK.
        final JSONObject noteSrv = getBody.getJSONObject(API_KEY_DATA);
        if (noteSrv == null || noteSrv.toString().isEmpty()) {
            throw new Exception("Data is invalid.");
        }

        // Local entries preparing.
        final Map<String, String> noteMap = convertJsonToMap(note);

        // Server entries preparing.
        noteSrv.remove(API_KEY_ID);
        final Map<String, String> noteSrvMap = convertJsonToMap(noteSrv.toString());

        // Equals entries OK.
        assertEquals(noteMap, noteSrvMap);


        /*
            Delete from server.
         */

        final Response<String> deleteResponse = sNoteApi.delete(userId, newId).execute();

        // Response 200 OK.
        if (!deleteResponse.isSuccessful()) {
            throw new Exception("Response is not successful.");
        }

        final JSONObject deleteBody = new JSONObject(deleteResponse.body());

        // Status OK.
        final String deleteStatus = deleteBody.optString(API_KEY_STATUS);
        assertEquals(API_STATUS_OK, deleteStatus);


        /*
            Get again.
         */

        final Response<String> getAgainResponse = sNoteApi.get(userId, newId).execute();

        // Response 200 OK.
        if (!getAgainResponse.isSuccessful()) {
            throw new Exception("Response is not successful.");
        }

        final JSONObject getAgainBody = new JSONObject(getAgainResponse.body());

        // Status ERROR.
        final String getAgainStatus = getAgainBody.optString(API_KEY_STATUS);
        assertEquals(API_STATUS_ERROR, getAgainStatus);
    }


    /*
        Utils
     */

    @NonNull
    private static String getNoteOneInstance() {
        return "{\"title\": \"Yelena Yeremeyeva\", "
                + "\"color\": \"#0010FF\", "
                + "\"imageUrl\": \"https:\\/\\/developer.android.com\\/images\\/brand\\/Android_Robot_100.png\", "
                + "\"description\": \"butterfly galaxy Peace Smile bubble cosmopolitan cosy Love \", "
                + "\"created\": \"2017-05-05T02:25:35+05:00\", "
                + "\"edited\": \"2017-05-05T02:25:35+05:00\", "
                + "\"viewed\": \"2017-05-08T02:25:35+05:00\"}";
    }

    @NonNull
    private static String getNoteTwoInstance() {
        return "{\"title\": \"Tatyana Vasilieva\", "
                + "\"color\": \"#0020FF\", "
                + "\"imageUrl\": \"https:\\/\\/developer.android.com\\/images\\/brand\\/Android_Robot_101.png\", "
                + "\"description\": \"butterfly galaxy Peace Smile bubble cosmopolitan cosy \", "
                + "\"created\": \"2017-05-05T02:25:35+05:01\", "
                + "\"edited\": \"2017-05-05T02:25:35+05:01\", "
                + "\"viewed\": \"2017-05-08T02:25:35+05:01\"}";
    }
}