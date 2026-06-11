package com.example.planify.data.remote;

import com.example.planify.data.remote.model.WeatherResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApiService {
    // Fetch by city name (fallback)
    @GET("data/2.5/weather")
    Call<WeatherResponse> getCurrentWeather(
        @Query("q") String city,
        @Query("appid") String apiKey,
        @Query("units") String units
    );

    // Fetch by GPS coordinates (primary — more accurate)
    @GET("data/2.5/weather")
    Call<WeatherResponse> getWeatherByCoords(
        @Query("lat") double lat,
        @Query("lon") double lon,
        @Query("appid") String apiKey,
        @Query("units") String units
    );
}
