package net.harimurti.tv.extra;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Network {
    private Context context;

    public Network(Context context) {
        this.context = context;
    }

    public boolean IsConnected() {
        ConnectivityManager manager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) return false;

        NetworkInfo network = manager.getActiveNetworkInfo();
        if (network == null) return false;

        return network.isConnected();
    }
}
