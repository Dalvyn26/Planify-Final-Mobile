package com.example.planify.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

public class NetworkUtils {
    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities cap = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return cap != null && (
            cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            cap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        );
    }
}
