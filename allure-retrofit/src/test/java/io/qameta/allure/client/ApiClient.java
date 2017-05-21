package io.qameta.allure.client;

import io.qameta.allure.retrofit.AllureLoggingInterceptor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

/**
 * Created by vicdev on 14.05.17.
 */
public class ApiClient {

    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(new AllureLoggingInterceptor()).build();
        retrofit = new Retrofit.Builder()
                .baseUrl("http://localhost:1080")
                .client(client)
                .build();
        return retrofit;
    }

}
