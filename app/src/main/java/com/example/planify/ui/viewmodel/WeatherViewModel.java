package com.example.planify.ui.viewmodel;

import android.Manifest;
import android.app.Application;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.planify.data.remote.model.WeatherResponse;
import com.example.planify.data.repository.WeatherRepository;
import com.example.planify.utils.Constants;
import com.example.planify.utils.NetworkUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

public class WeatherViewModel extends AndroidViewModel {
    private static final String TAG = "WeatherViewModel";

    private final WeatherRepository repository;
    private final FusedLocationProviderClient fusedLocationClient;
    public final MutableLiveData<WeatherResponse> weatherData = new MutableLiveData<>();
    public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    public final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public WeatherViewModel(@NonNull Application application) {
        super(application);
        repository = new WeatherRepository(application);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(application);
    }

    /**
     * Main entry point — tries GPS first, falls back to city name.
     * Respects 15-min cache TTL to avoid redundant API calls.
     */
    public void fetchWeather() {
        // If cache is still fresh, use cached data (no API call)
        if (repository.isCacheFresh()) {
            WeatherResponse cached = repository.getCachedWeather();
            if (cached != null) {
                Log.d(TAG, "Using cached weather (still fresh)");
                weatherData.setValue(cached);
                errorMessage.setValue(null);
                return;
            }
        }

        isLoading.setValue(true);
        errorMessage.setValue(null);

        if (!NetworkUtils.isConnected(getApplication())) {
            handleOffline();
            return;
        }

        // Try GPS location first
        if (hasLocationPermission()) {
            fetchLocationThenWeather();
        } else {
            // No location permission — fall back to default city
            Log.d(TAG, "No location permission, using default city: " + Constants.DEFAULT_CITY);
            fetchByCityFallback();
        }
    }

    /**
     * Force-refresh: bypasses cache TTL.
     */
    public void forceRefresh() {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        if (!NetworkUtils.isConnected(getApplication())) {
            handleOffline();
            return;
        }

        if (hasLocationPermission()) {
            fetchLocationThenWeather();
        } else {
            fetchByCityFallback();
        }
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(getApplication(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(getApplication(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressWarnings("MissingPermission") // Already checked in hasLocationPermission()
    private void fetchLocationThenWeather() {
        CancellationTokenSource cts = new CancellationTokenSource();

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.getToken())
            .addOnSuccessListener(location -> {
                if (location != null) {
                    Log.d(TAG, "GPS location: " + location.getLatitude() + ", " + location.getLongitude());
                    fetchByCoords(location.getLatitude(), location.getLongitude());
                } else {
                    // getCurrentLocation returned null — try getLastLocation
                    fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(lastLocation -> {
                            if (lastLocation != null) {
                                Log.d(TAG, "Using last known location: " + lastLocation.getLatitude() + ", " + lastLocation.getLongitude());
                                fetchByCoords(lastLocation.getLatitude(), lastLocation.getLongitude());
                            } else {
                                Log.w(TAG, "No location available, falling back to city");
                                fetchByCityFallback();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "getLastLocation failed", e);
                            fetchByCityFallback();
                        });
                }
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "getCurrentLocation failed", e);
                fetchByCityFallback();
            });
    }

    private void fetchByCoords(double lat, double lon) {
        repository.fetchWeatherByCoords(lat, lon, new WeatherRepository.WeatherCallback() {
            @Override
            public void onSuccess(WeatherResponse data) {
                isLoading.postValue(false);
                weatherData.postValue(data);
                errorMessage.postValue(null);
            }

            @Override
            public void onError(Exception e) {
                Log.w(TAG, "Coord fetch failed, trying city fallback", e);
                fetchByCityFallback();
            }
        });
    }

    private void fetchByCityFallback() {
        repository.fetchWeatherByCity(Constants.DEFAULT_CITY, new WeatherRepository.WeatherCallback() {
            @Override
            public void onSuccess(WeatherResponse data) {
                isLoading.postValue(false);
                weatherData.postValue(data);
                errorMessage.postValue(null);
            }

            @Override
            public void onError(Exception e) {
                isLoading.postValue(false);
                WeatherResponse cached = repository.getCachedWeather();
                if (cached != null) {
                    weatherData.postValue(cached);
                    errorMessage.postValue("Offline - showing cached data");
                } else {
                    errorMessage.postValue("Failed to load weather");
                }
            }
        });
    }

    private void handleOffline() {
        WeatherResponse cached = repository.getCachedWeather();
        isLoading.setValue(false);
        if (cached != null) {
            weatherData.setValue(cached);
            errorMessage.setValue("Offline - showing cached data");
        } else {
            errorMessage.setValue("No internet connection");
        }
    }
}
