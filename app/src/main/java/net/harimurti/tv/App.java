package net.harimurti.tv;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDexApplication;

public class App extends MultiDexApplication {
    @SuppressLint("StaticFieldLeak")
    protected static Context context = null;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
    }

    public static Context getContext() {
        return context;
    }
}