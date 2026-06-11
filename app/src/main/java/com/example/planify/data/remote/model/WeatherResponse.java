package com.example.planify.data.remote.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class WeatherResponse {
    @SerializedName("name") public String cityName;
    @SerializedName("main") public WeatherMain main;
    @SerializedName("weather") public List<WeatherDescription> weather;
}
