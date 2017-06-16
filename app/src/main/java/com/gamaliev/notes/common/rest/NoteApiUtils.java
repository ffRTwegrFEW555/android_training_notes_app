package com.gamaliev.notes.common.rest;

import android.support.annotation.Nullable;

import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class NoteApiUtils {

    /* ... */
    public static final String API_KEY_STATUS   = "status";
    public static final String API_KEY_DATA     = "data";
    public static final String API_STATUS_OK    = "ok";
    @SuppressWarnings({"unused", "WeakerAccess"})
    public static final String API_STATUS_ERROR = "error";
    public static final String API_KEY_ID       = "id";
    public static final String API_KEY_EXTRA    = "extra";

    /* Tests */
    public static final String LOCALHOST        = "http://localhost";
    public static final String TEST_PORT        = "8080";


    /*
        Init
     */

    private NoteApiUtils() {}

    /**
     * @param baseUrl Base url for rest API. Example: "http://localhost:8080"
     * @return NoteApi.class, implemented by Retrofit-framework, representing REST API for base URL.
     */
    @Nullable
    public static NoteApi newInstance(@Nullable final String baseUrl) {
        NoteApi noteApi = null;

        if (isTestMode()) {
            noteApi = new Retrofit.Builder()
                    .baseUrl(LOCALHOST + ':' + TEST_PORT)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build()
                    .create(NoteApi.class);

        } else if (baseUrl != null) {
            noteApi = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build()
                    .create(NoteApi.class);
        }

        return noteApi;
    }


    /*
        ...
     */

    private static boolean isTestMode() {
        boolean isTestMode;

        try {
            Class.forName("com.gamaliev.notes.common.rest.NoteApiUtilsTest");
            isTestMode = true;
        } catch (ClassNotFoundException e) {
            isTestMode = false;
        }

        return isTestMode;
    }
}
