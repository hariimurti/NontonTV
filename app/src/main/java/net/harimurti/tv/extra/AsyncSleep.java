package net.harimurti.tv.extra;

import android.content.Context;
import android.os.Handler;

public class AsyncSleep {
    private Task task = null;
    private Context context;

    public interface Task {
        default void onCountDown(int left){}
        default void onFinish(){}
    }

    public AsyncSleep task(Task task) {
        this.task = task;
        return this;
    }

    public AsyncSleep(Context context) {
        this.context = context;
    }

    public void start(int second) {
        for (int i = 1; i <= second; i++) {
            int left = second - i;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(()-> {
                        task.onCountDown(left);
                        if (left == 0) {
                            task.onFinish();
                        }
                    });
                }
            }, i * 1000);
        }
    }

    private void runOnUiThread(Runnable task) {
        new Handler(context.getMainLooper()).post(task);
    }
}
