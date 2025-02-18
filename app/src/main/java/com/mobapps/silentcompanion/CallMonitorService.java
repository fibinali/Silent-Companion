package com.mobapps.silentcompanion;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class CallMonitorService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start the incoming call receiver to listen for calls in the background
        return START_STICKY; // Keep the service running until explicitly stopped
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}