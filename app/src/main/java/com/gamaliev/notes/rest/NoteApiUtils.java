package com.gamaliev.notes.rest;

import com.gamaliev.notes.app.NotesApp;
import com.gamaliev.notes.common.shared_prefs.SpUsers;

import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class NoteApiUtils {

    /* Logger */
    @SuppressWarnings("unused")
    private static final String TAG = NoteApiUtils.class.getSimpleName();

    /* ... */
    public static final String API_KEY_STATUS   = "status";
    public static final String API_KEY_DATA     = "data";
    public static final String API_STATUS_OK    = "ok";
    public static final String API_STATUS_ERROR = "error";
    public static final String API_KEY_ID       = "id";
    public static final String API_KEY_EXTRA    = "extra";

    private static final NoteApi sNoteApi;


    /*
        Init
     */

    static {
        sNoteApi = new Retrofit.Builder()
                .baseUrl(SpUsers.getApiUrlForCurrentUser(NotesApp.getAppContext()))
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
                .create(NoteApi.class);
    }


    /*
        Getters
     */

    public static NoteApi getNoteApi() {
        return sNoteApi;
    }
}
