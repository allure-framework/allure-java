package io.qameta.allure.client;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Created by vicdev on 14.05.17.
 */
public interface ApiInterface {

    @GET("/simple")
    Call<ResponseBody> simple();
}