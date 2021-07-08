package net.harimurti.tv.extra;

import android.annotation.SuppressLint;

import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;

public class HttpsTrustManager implements X509TrustManager {

    private static final X509Certificate[] _AcceptedIssuers = new X509Certificate[] { };

    @SuppressLint("TrustAllX509TrustManager")
    @Override
    public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) { }

    @SuppressLint("TrustAllX509TrustManager")
    @Override
    public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) { }

    public boolean isClientTrusted(X509Certificate[] chain) {
        return true;
    }

    public boolean isServerTrusted(X509Certificate[] chain) {
        return true;
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return _AcceptedIssuers;
    }

}
