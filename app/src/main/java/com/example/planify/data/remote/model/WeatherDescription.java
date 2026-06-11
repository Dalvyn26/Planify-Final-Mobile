package com.example.planify.data.remote.model;

import com.google.gson.annotations.SerializedName;

public class WeatherDescription {
    @SerializedName("id") public int id;
    @SerializedName("main") public String main;
    @SerializedName("description") public String description;
    @SerializedName("icon") public String icon;
}
