package com.example.planify.data.repository;

import android.content.Context;

import com.example.planify.data.remote.RetrofitClient;
import com.example.planify.data.remote.WeatherApiService;
import com.example.planify.data.remote.model.WeatherResponse;
import com.example.planify.utils.Constants;
import com.example.planify.utils.PreferenceManager;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherRepository {
    private final WeatherApiService weatherApiService;
    private final PreferenceManager prefs;
    private final Gson gson;

    // Cache TTL: 15 minutes — weather doesn't change that fast
    private static final long CACHE_TTL_MS = 15 * 60 * 1000L;

    public WeatherRepository(Context context) {
        weatherApiService = RetrofitClient.getWeatherApiService();
        prefs = PreferenceManager.getInstance(context);
        gson = new Gson();
    }

    /**
     * Fetch weather by GPS coordinates (primary method).
     */
    public void fetchWeatherByCoords(double lat, double lon, WeatherCallback callback) {
        weatherApiService.getWeatherByCoords(lat, lon, Constants.WEATHER_API_KEY, "metric")
            .enqueue(new Callback<WeatherResponse>() {
                @Override
                public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        WeatherResponse data = response.body();
                        cacheWeatherData(data);
                        callback.onSuccess(data);
                    } else {
                        callback.onError(new Exception("API error: " + response.code()));
                    }
                }

                @Override
                public void onFailure(Call<WeatherResponse> call, Throwable t) {
                    callback.onError(new Exception(t));
                }
            });
    }

    /**
     * Fetch weather by city name (fallback when GPS is unavailable).
     */
    public void fetchWeatherByCity(String city, WeatherCallback callback) {
        weatherApiService.getCurrentWeather(city, Constants.WEATHER_API_KEY, "metric")
            .enqueue(new Callback<WeatherResponse>() {
                @Override
                public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        WeatherResponse data = response.body();
                        cacheWeatherData(data);
                        callback.onSuccess(data);
                    } else {
                        callback.onError(new Exception("API error: " + response.code()));
                    }
                }

                @Override
                public void onFailure(Call<WeatherResponse> call, Throwable t) {
                    callback.onError(new Exception(t));
                }
            });
    }

    private void cacheWeatherData(WeatherResponse data) {
        prefs.cacheWeather(gson.toJson(data));
        prefs.setWeatherCacheTimestamp(System.currentTimeMillis());
    }

    /**
     * Check if the cached weather data is still fresh (within TTL).
     */
    public boolean isCacheFresh() {
        long cachedAt = prefs.getWeatherCacheTimestamp();
        return cachedAt > 0 && (System.currentTimeMillis() - cachedAt) < CACHE_TTL_MS;
    }

    public WeatherResponse getCachedWeather() {
        String json = prefs.getCachedWeather();
        if (json == null || json.isEmpty()) return null;
        try {
            return gson.fromJson(json, WeatherResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

    public interface WeatherCallback {
        void onSuccess(WeatherResponse data);
        void onError(Exception e);
    }
}
