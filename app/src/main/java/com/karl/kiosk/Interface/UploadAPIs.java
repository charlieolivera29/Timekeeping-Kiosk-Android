package com.karl.kiosk.Interface;

import com.google.gson.JsonElement;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.Map;

public interface UploadAPIs {

    @Multipart
    @POST("{fullUrl}")
    Call<JsonElement> checKPinWithImage(
            @Path(value = "fullUrl", encoded = true) String fullUrl,
            @HeaderMap Map<String, String> headers,
            @Part("user_id") RequestBody user_id,
            @Part("pin") RequestBody pin,
            @Part("date") RequestBody date,
            @Part("time") RequestBody time,
            @Part("location") RequestBody location,
            @Part("reference") RequestBody reference,
            @Part("api_token") RequestBody api_token,
            @Part("link") RequestBody link,
            @Part MultipartBody.Part image,


            @Query("link") String queryLink


            );
}