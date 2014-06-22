package com.robot;

import android.os.Handler;
import android.os.Looper;

import com.authy.authy.util.Log;
import com.squareup.otto.Bus;

public class EventBus extends Bus {

    private final static String TAG = "EventBus";
    private static EventBus instance;

    private final Handler mainThread = new Handler(Looper.getMainLooper());


    @Override
    public void post(final Object event) {
        Log.d(TAG, "Posting " + event);
        if (Looper.myLooper() == Looper.getMainLooper()) {
            super.post(event);
        } else {
            mainThread.post(new Runnable() {
                @Override
                public void run() {
                    post(event);
                }
            });
        }
    }

    public static EventBus get() {
        if (instance == null) {
            instance = new EventBus();
        }
        return instance;
    }
}
