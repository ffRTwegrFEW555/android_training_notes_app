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
    private static final String TAG = NoteApiUtils.class.getSimpleName();

    /* ... */
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
