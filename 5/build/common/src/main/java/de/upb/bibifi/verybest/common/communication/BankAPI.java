package de.upb.bibifi.verybest.common.communication;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface BankAPI {
    @GET("/init")
    Call<ResponseBody> init();

    @POST("/")
    Call<ResponseBody> postMessage(@Body RequestBody array);
}
