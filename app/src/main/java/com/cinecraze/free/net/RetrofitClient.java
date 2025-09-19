package com.cinecraze.free.net;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class RetrofitClient {

    private static Retrofit retrofit = null;
    private static final String BASE_URL = "https://raw.githubusercontent.com/MovieAddict88/Movie-Source/main/";

    public static Retrofit getClient() {
        if (retrofit == null) {
            // Create logging interceptor
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Create custom interceptor to handle redirects and add headers
            Interceptor customInterceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request original = chain.request();
                    
                    // Add headers to mimic a browser request
                    Request request = original.newBuilder()
                            .header("User-Agent", "Mozilla/5.0 (Android) CineCraze/1.0")
                            .header("Accept", "application/json")
                            .method(original.method(), original.body())
                            .build();
                    
                    return chain.proceed(request);
                }
            };

            // Create OkHttpClient with timeouts, logging, and custom interceptor
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(customInterceptor)
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
