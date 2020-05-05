package net.harimurti.tv.extra;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import net.harimurti.tv.App;

public class Network {
    public static boolean IsConnected() {
        ConnectivityManager manager = (ConnectivityManager)App.getContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (manager == null) return false;

        NetworkInfo network = manager.getActiveNetworkInfo();
        if (network == null) return false;

        return network.isConnected();
    }
}
