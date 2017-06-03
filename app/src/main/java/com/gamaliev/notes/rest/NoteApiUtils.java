package com.gamaliev.notes.rest;

import android.support.annotation.Nullable;

import com.gamaliev.notes.app.NotesApp;
import com.gamaliev.notes.common.shared_prefs.SpUsers;

import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class NoteApiUtils {

    /* ... */
    public static final String API_KEY_STATUS   = "status";
    public static final String API_KEY_DATA     = "data";
    public static final String API_STATUS_OK    = "ok";
    @SuppressWarnings("unused")
    public static final String API_STATUS_ERROR = "error";
    public static final String API_KEY_ID       = "id";
    public static final String API_KEY_EXTRA    = "extra";

    @Nullable private static final NoteApi sNoteApi;


    /*
        Init
     */

    static {
        final String url = SpUsers.getApiUrlForCurrentUser(NotesApp.getAppContext());
        if (url == null) {
            sNoteApi = null;
        } else {
            sNoteApi = new Retrofit.Builder()
                    .baseUrl(url)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build()
                    .create(NoteApi.class);
        }
    }


    /*
        Getters
     */

    @Nullable
    public static NoteApi getNoteApi() {
        return sNoteApi;
    }
}
