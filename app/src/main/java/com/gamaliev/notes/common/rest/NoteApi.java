package com.gamaliev.notes.common.rest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public interface NoteApi {

    // --Commented out by Inspection START:
    //    @GET("info")
    //    Call<String> getInfo();
    // --Commented out by Inspection STOP

    @GET("user/{user_id}/notes")
    Call<String> getAll(@Path("user_id") final String userId);

    @GET("user/{user_id}/note/{note_id}")
    Call<String> get(
            @Path("user_id") final String userId,
            @Path("note_id") final String noteId);

    @Headers("Content-Type: application/json")
    @POST("user/{user_id}/notes")
    Call<String> add(
            @Path("user_id") final String userId,
            @Body final String noteJson);

    @Headers("Content-Type: application/json")
    @POST("user/{user_id}/note/{note_id}")
    Call<String> update(
            @Path("user_id") final String userId,
            @Path("note_id") final String noteId,
            @Body final String noteJson);

    @DELETE("user/{user_id}/note/{note_id}")
    Call<String> delete(
            @Path("user_id") final String userId,
            @Path("note_id") final String noteId);
}
