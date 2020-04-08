package net.harimurti.tv.extra;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import net.harimurti.tv.R;
import net.harimurti.tv.data.Playlist;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RestClient {
    private final String TAG = "RestClient";
    private final OkHttpClient client = new OkHttpClient();

    private Context context;
    private OnClientResult onClientResult;

    public interface OnClientResult {
        default void onFailure(String status){}
        default void onProgress(boolean status){}
        default void onSuccess(Playlist playlist){}
    }

    public RestClient setOnClientResult(OnClientResult onClientResult) {
        this.onClientResult = onClientResult;
        return this;
    }

    public RestClient(Context context){
        this.context = context;
    }

    @SuppressWarnings("all")
    public void GetChannels() {
        onClientResult.onProgress(true);
        Request request = new Request.Builder()
                .url(context.getString(R.string.api_channel))
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, e.getMessage());
                onClientResult.onFailure(e.getMessage());
                onClientResult.onProgress(false);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    // convert json to channel
                    Playlist playlist = new Gson().fromJson(response.body().string(), Playlist.class);

                    onClientResult.onSuccess(playlist);
                }
                catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                    onClientResult.onFailure(e.getMessage());
                }
                finally {
                    onClientResult.onProgress(false);
                }
            }
        });
    }
}
