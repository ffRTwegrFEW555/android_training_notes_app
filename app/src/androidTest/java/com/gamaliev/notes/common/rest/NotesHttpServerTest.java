package com.gamaliev.notes.common.rest;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_SYNC_ID_JSON;
import static com.gamaliev.notes.common.rest.NoteApiUtils.API_KEY_DATA;
import static com.gamaliev.notes.common.rest.NoteApiUtils.API_KEY_STATUS;
import static com.gamaliev.notes.common.rest.NoteApiUtils.API_STATUS_OK;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

@SuppressWarnings("NullableProblems")
public final class NotesHttpServerTest {

    /* Logger */
    private static final String TAG = NotesHttpServerTest.class.getSimpleName();

    /* Request types */
    private static final String HTTP_REQUEST_GET    = "get";
    private static final String HTTP_REQUEST_POST   = "post";
    private static final String HTTP_REQUEST_DELETE = "delete";

    /* Supported patterns */
    private static final String GET_INFO            = "^/?info/?$";
    private static final String GET_ALL             = "^/?user/\\d+/notes/?$";
    private static final String GET_NOTE            = "^/?user/\\d+/note/\\d+/?$";
    private static final String POST_ADD_NOTE       = "^/?user/\\d+/notes/?$";
    private static final String POST_UPDATE_NOTE    = "^/?user/\\d+/note/\\d+/?$";
    private static final String DELETE_NOTE         = "^/?user/\\d+/note/\\d+/?$";

    /* ... */
    private static final String ERROR_INVALID_DATA  = "invalid_data";
    private static final String ERROR_USER_NOT_FOUND = "User is not found.";
    private static final String ERROR_NOT_FOUND     = "not_found";
    private static final String ERROR_SERVER_ERROR  = "server_error";
    private static final String ERROR_INVALID_PORT  = "Port must be >= 1024, <= 65535";
    private static final String HEADER_CONTENT_LENGTH = "Content-Length:";

    /* Server */
    private final int mPort;
    @NonNull private ServerSocket mServerSocket;
    private boolean mIsRunning = false;

    /* Db, <UserId, <NoteId, Note>> */
    private Map<String, Map<String, JSONObject>> mDb;
    private AtomicInteger mNoteIdCounter;


    /*
        Init
     */

    {
        mDb = new ConcurrentHashMap<>();
        mNoteIdCounter = new AtomicInteger();
    }

    private NotesHttpServerTest(@IntRange(from = 1024, to = 65535) final int port) {
        mPort = port;
    }

    /**
     * @param port Must be >= 1024, <= 65535.
     */
    public static NotesHttpServerTest newInstance(@IntRange(from = 1024, to = 65535) final int port) {
        if (port < 1024 || port > 65535) {
            throw new IllegalArgumentException(ERROR_INVALID_PORT);
        }
        return new NotesHttpServerTest(port);
    }


    /*
        Init server
     */

    public void startServer() {
        mIsRunning = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mServerSocket = new ServerSocket(mPort);
                    while (true) {
                        handleRequest(mServerSocket.accept());
                        if (!mIsRunning) {
                            break;
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                }
            }
        }).start();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Log.e(TAG, e.toString());
        }
    }

    public void stopServer() {
        mIsRunning = false;
        try {
            mServerSocket.close();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }


    /*
        Request
     */

    private void handleRequest(@NonNull final Socket socket) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try (final BufferedReader br =
                             new BufferedReader(
                                     new InputStreamReader(
                                             socket.getInputStream(), StandardCharsets.UTF_8))) {

                    String header = br.readLine();
                    if (header == null) {
                        sendResponse(socket, getErrorResponse(ERROR_INVALID_DATA));
                        throw new RuntimeException("Header: first line is null.");
                    }
                    header = header.toLowerCase(Locale.ENGLISH);
                    final String[] split = header.split(" ");
                    if (split.length != 3
                            || split[0].isEmpty()
                            || split[1].isEmpty()
                            || split[2].isEmpty()) {
                        sendResponse(socket, get404Response());

                    } else if (HTTP_REQUEST_GET.equals(split[0])) {
                        if (split[1].matches(GET_INFO)) {
                            sendResponse(socket, getInfoResponse());

                        } else if (split[1].matches(GET_ALL)) {
                            final String userId = split[1].split("/")[2];
                            sendResponse(socket, getAllNotesResponse(userId));

                        } else if (split[1].matches(GET_NOTE)) {
                            final String[] path = split[1].split("/");
                            final String userId = path[2];
                            final String noteId = path[4];
                            sendResponse(socket, getNoteResponse(userId, noteId));

                        } else {
                            sendResponse(socket, get404Response());
                        }

                    } else if (HTTP_REQUEST_POST.equals(split[0])) {
                        if (split[1].matches(POST_ADD_NOTE)) {
                            final String userId = split[1].split("/")[2];
                            final String newNoteId = addNote(userId, getContent(br));
                            if (newNoteId != null) {
                                sendResponse(socket, getAddedNoteResponse(newNoteId));
                            } else {
                                sendResponse(socket, getErrorResponse(ERROR_INVALID_DATA));
                            }

                        } else if (split[1].matches(POST_UPDATE_NOTE)) {
                            final String[] path = split[1].split("/");
                            final String userId = path[2];
                            final String noteId = path[4];
                            if (updateNote(userId, noteId, getContent(br))) {
                                sendResponse(socket, getUpdatedNoteResponse());
                            } else {
                                sendResponse(socket, getErrorResponse(ERROR_INVALID_DATA));
                            }

                        } else {
                            sendResponse(socket, get404Response());
                        }

                    } else if (HTTP_REQUEST_DELETE.equals(split[0])) {
                        if (split[1].matches(DELETE_NOTE)) {
                            final String[] path = split[1].split("/");
                            final String userId = path[2];
                            final String noteId = path[4];
                            if (deleteNote(userId, noteId)) {
                                sendResponse(socket, getDeletedNoteResponse());
                            } else {
                                sendResponse(socket, getErrorResponse(ERROR_INVALID_DATA));
                            }

                        } else {
                            sendResponse(socket, get404Response());
                        }
                    }

                } catch (IOException | RuntimeException e) {
                    Log.e(TAG, e.toString());
                }

                try {
                    socket.close();
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                }
            }
        }).start();
    }

    @NonNull
    private String getContent(@NonNull final BufferedReader br) {
        final StringBuilder content = new StringBuilder();
        String header;
        int contentLength = 0;
        boolean contentExists = false;

        try {
            while (true) {
                // Read headers.
                header = br.readLine();
                if (header == null || header.isEmpty()) {

                    // Read content.
                    if (contentExists) {
                        final char[] buffer = new char[1024];
                        while (contentLength > 0) {
                            final int length = br.read(buffer);
                            if (length == -1) {
                                break;
                            }
                            contentLength -= length;
                            content.append(buffer, 0, length);
                        }
                    }
                    break;
                }

                // Find 'Content-length'.
                if (!contentExists && header.contains(HEADER_CONTENT_LENGTH)) {
                    final String[] split = header.split(HEADER_CONTENT_LENGTH);
                    if (split.length >= 2) {
                        contentLength = Integer.parseInt(split[1].trim());
                        if (contentLength > 0) {
                            contentExists = true;
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }

        return content.toString();
    }


    /*
        Response
     */

    private void sendResponse(
            @NonNull final Socket socket,
            @NonNull final String response) {

        try {
            final OutputStream os = socket.getOutputStream();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.flush();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    @NonNull
    private String get404Response() {
        return "HTTP/1.1 404 Not Found" + "\n"
                + HEADER_CONTENT_LENGTH + "78" + "\n"
                + "Content-Type: text/html" + "\n"
                + "\n"
                + "<h1>404 Not Found</h1><h3>The page you have requested could not be found.</h3>";
    }

    @NonNull
    private String getInfoResponse() {
        return getSuccessResponseHeader()
                + HEADER_CONTENT_LENGTH + "28" + "\n"
                + "\n"
                + "{\"status\": \"ok\", \"data\": {}}";
    }

    @NonNull
    private String getErrorResponse(@NonNull final String error) {
        final String errorResult = "{\"status\": \"error\", \"error\": \"" + error + "\"}";
        return getSuccessResponseHeader()
                + HEADER_CONTENT_LENGTH + errorResult.length() + "\n"
                + "\n"
                + errorResult;
    }

    @NonNull
    private String getAllNotesResponse(@NonNull final String userId) {
        final JSONObject responseBody = new JSONObject();
        try {
            responseBody.put(API_KEY_STATUS, API_STATUS_OK);

            final JSONArray notes = new JSONArray();
            final Map<String, JSONObject> map = mDb.get(userId);
            if (map != null) {
                for (JSONObject jsonObject : map.values()) {
                    notes.put(jsonObject);
                }
            }
            responseBody.put(API_KEY_DATA, notes);

            final String body = responseBody.toString();

            return getSuccessResponseHeader()
                    + HEADER_CONTENT_LENGTH + body.length() + '\n'
                    + '\n'
                    + body;

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }

        return getErrorResponse(ERROR_SERVER_ERROR);
    }

    @NonNull
    private String getNoteResponse(@NonNull final String userId, @NonNull final String noteId) {
        final JSONObject responseBody = new JSONObject();
        try {
            responseBody.put(API_KEY_STATUS, API_STATUS_OK);

            final Map<String, JSONObject> map = mDb.get(userId);
            if (map == null) {
                throw new IllegalArgumentException(ERROR_USER_NOT_FOUND);
            }

            final JSONObject jsonObject = map.get(noteId);
            if (jsonObject == null) {
                throw new IllegalArgumentException(ERROR_USER_NOT_FOUND);
            }
            responseBody.put(API_KEY_DATA, jsonObject);

            final String body = responseBody.toString();

            return getSuccessResponseHeader()
                    + HEADER_CONTENT_LENGTH + body.length() + '\n'
                    + '\n'
                    + body;

        } catch (JSONException | IllegalArgumentException e) {
            Log.e(TAG, e.toString());
        }

        return getErrorResponse(ERROR_NOT_FOUND);
    }

    @NonNull
    private String getAddedNoteResponse(@NonNull final String newNoteId) {
        final String body = "{\"status\": \"ok\", \"data\": " + newNoteId + "}";
        return getSuccessResponseHeader()
                + HEADER_CONTENT_LENGTH + body.length() + "\n"
                + "\n"
                + body;
    }

    @NonNull
    private String getUpdatedNoteResponse() {
        return getSuccessResponse();
    }

    @NonNull
    private String getDeletedNoteResponse() {
        return getSuccessResponse();
    }


    /*
        Database
     */

    @Nullable
    private String addNote(
            @NonNull final String userId,
            @NonNull final String noteJson) {

        String newNoteId = null;
        final Map<String, JSONObject> map = mDb.get(userId);
        try {
            final JSONObject noteJsonObject = new JSONObject(noteJson);
            newNoteId = String.valueOf(mNoteIdCounter.incrementAndGet());
            noteJsonObject.put(LIST_ITEMS_COLUMN_SYNC_ID_JSON, newNoteId);

            if (map == null) {
                final HashMap<String, JSONObject> newNotes = new HashMap<>();
                newNotes.put(newNoteId, noteJsonObject);
                mDb.put(userId, newNotes);
            } else {
                map.put(newNoteId, noteJsonObject);
            }

            return newNoteId;

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }

        return newNoteId;
    }

    private boolean updateNote(
            @NonNull final String userId,
            @NonNull final String noteId,
            @NonNull final String noteJson) {

        final Map<String, JSONObject> map = mDb.get(userId);
        if (map == null) {
            return false;
        }

        final JSONObject jsonObject = map.get(noteId);
        if (jsonObject == null) {
            return false;
        }

        try {
            final JSONObject noteJsonObject = new JSONObject(noteJson);
            noteJsonObject.put(LIST_ITEMS_COLUMN_SYNC_ID_JSON, noteId);
            map.put(noteId, noteJsonObject);
            return true;

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }

        return false;
    }

    private boolean deleteNote(
            @NonNull final String userId,
            @NonNull final String noteId) {

        final Map<String, JSONObject> map = mDb.get(userId);
        return map != null && map.remove(noteId) != null;
    }


    /*
        Utils
     */

    @NonNull
    private static String getSuccessResponseHeader() {
        return "HTTP/1.1 200 OK" + "\n"
                + "Content-Type: application/json; charset=utf-8" + "\n";
    }

    @NonNull
    private String getSuccessResponse() {
        return getSuccessResponseHeader()
                + "Content-Length: 16" + "\n"
                + "\n"
                + "{\"status\": \"ok\"}";
    }
}
