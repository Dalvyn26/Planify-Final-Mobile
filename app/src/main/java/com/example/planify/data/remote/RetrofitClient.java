package com.example.planify.data.remote;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class RetrofitClient {
    private static final String WEATHER_BASE_URL = "https://api.openweathermap.org/";
    private static Retrofit weatherInstance;

    public static Retrofit getWeatherInstance() {
        if (weatherInstance == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new HttpLoggingInterceptor().setLevel(
                    HttpLoggingInterceptor.Level.BODY))
                .build();
            weatherInstance = new Retrofit.Builder()
                .baseUrl(WEATHER_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        }
        return weatherInstance;
    }

    public static WeatherApiService getWeatherApiService() {
        return getWeatherInstance().create(WeatherApiService.class);
    }
}
