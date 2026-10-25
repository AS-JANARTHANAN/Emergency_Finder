package com.emergencyroutefinder.network;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * Retrofit API client factory.
 * Singleton pattern — one instance for the entire app lifecycle.
 *
 * Demonstrates: Singleton, Network abstraction
 */
public class ApiClient {

    private static String BASE_URL;
    private static Retrofit retrofit;
    private static String authToken;

    private ApiClient() {}

    public static void init(String baseUrl) {
        BASE_URL = baseUrl;
        retrofit = null; // Reset on re-init
    }

    public static void setAuthToken(String token) {
        authToken = token;
        retrofit = null; // Rebuild client with new token
    }

    public static Retrofit getRetrofit() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .addInterceptor(chain -> {
                        okhttp3.Request request = chain.request();
                        try {
                            return chain.proceed(request);
                        } catch (java.io.IOException e) {
                            okhttp3.HttpUrl originalUrl = request.url();
                            if ("192.168.31.148".equals(originalUrl.host())) {
                                okhttp3.HttpUrl fallbackUrl = originalUrl.newBuilder()
                                        .host("127.0.0.1")
                                        .build();
                                okhttp3.Request fallbackRequest = request.newBuilder()
                                        .url(fallbackUrl)
                                        .build();
                                try {
                                    return chain.proceed(fallbackRequest);
                                } catch (java.io.IOException ignored) {
                                    throw e;
                                }
                            }
                            throw e;
                        }
                    })
                    .addInterceptor(logging);

            // Add auth token to every request if set
            if (authToken != null) {
                clientBuilder.addInterceptor(chain -> {
                    okhttp3.Request original = chain.request();
                    okhttp3.Request request = original.newBuilder()
                            .header("Authorization", "Bearer " + authToken)
                            .build();
                    return chain.proceed(request);
                });
            }

            String normalizedUrl = (BASE_URL != null && BASE_URL.endsWith("/")) ? BASE_URL : (BASE_URL + "/");
            retrofit = new Retrofit.Builder()
                    .baseUrl(normalizedUrl)
                    .client(clientBuilder.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static <T> T createService(Class<T> serviceClass) {
        return getRetrofit().create(serviceClass);
    }

    public static String getBaseUrl() { return BASE_URL; }
}
