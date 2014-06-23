package com.robot;

import android.os.Handler;
import android.os.Looper;

import com.squareup.otto.Bus;

/**
 * A subclass of {@link com.squareup.otto.Bus otto.Bus} that posts every event on the Android UI thread.
 */
public class EventBus extends Bus {

    private final Handler mainThread = new Handler(Looper.getMainLooper());

    @Override
    public void post(final Object event) {
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
}
