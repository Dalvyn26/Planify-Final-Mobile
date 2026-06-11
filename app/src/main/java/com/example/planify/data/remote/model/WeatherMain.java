package com.example.planify.data.remote.model;

import com.google.gson.annotations.SerializedName;

public class WeatherMain {
    @SerializedName("temp") public double temp;
    @SerializedName("feels_like") public double feelsLike;
    @SerializedName("humidity") public int humidity;
}
