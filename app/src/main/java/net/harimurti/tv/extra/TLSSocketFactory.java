package net.harimurti.tv.extra;

import android.content.Context;
import android.os.Build;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

public class TLSSocketFactory extends SSLSocketFactory {

    private static TrustManager[] trustManagers;
    private final SSLSocketFactory factory;

    public TLSSocketFactory(Context context) throws KeyManagementException, NoSuchAlgorithmException {
        if (trustManagers == null) {
            trustManagers = new TrustManager[] {
                    new HttpsTrustManager()
            };
        }

        try {
            ProviderInstaller.installIfNeeded(context);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }

        SSLContext sslContext = getSSLContext();
        sslContext.init(null, trustManagers, new SecureRandom());
        factory = sslContext.getSocketFactory();
    }

    private SSLContext getSSLContext() throws NoSuchAlgorithmException {
        if (Build.VERSION.SDK_INT < 22) {
            try {
                return SSLContext.getInstance("TLSv1.2");
            } catch (NoSuchAlgorithmException e) {
                // fallback to TLS
            }
        }
        return SSLContext.getInstance("TLS");
    }

    public void trustAllHttps() {
        HttpsURLConnection.setDefaultHostnameVerifier((arg0, arg1) -> true);
        HttpsURLConnection.setDefaultSSLSocketFactory(factory);
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return factory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return factory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket() throws IOException {
        return enableTLSOnSocket(factory.createSocket());
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return enableTLSOnSocket(factory.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return enableTLSOnSocket(factory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return enableTLSOnSocket(factory.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return enableTLSOnSocket(factory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return enableTLSOnSocket(factory.createSocket(address, port, localAddress, localPort));
    }

    private Socket enableTLSOnSocket(Socket socket) {
        if(socket instanceof SSLSocket) {
            ((SSLSocket)socket).setEnabledProtocols(new String[] {"TLSv1.1", "TLSv1.2"});
        }
        return socket;
    }
}
