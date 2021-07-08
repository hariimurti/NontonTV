package net.harimurti.tv.extra;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Network {
    public static boolean IsConnected(Context context) {
        ConnectivityManager manager = (ConnectivityManager)context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (manager == null) return false;

        NetworkInfo network = manager.getActiveNetworkInfo();
        if (network == null) return false;

        return network.isConnected();
    }
}
