package com.nkjayanet.app;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.*;

public class ServerService extends Service {

    private static final String TAG = "ServerService";
    private ScriptManager scriptManager;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        scriptManager = new ScriptManager(this);

        new Thread(() -> {
            try {
                scriptManager.extractScripts();
                scriptManager.runServerScript();
            } catch (Exception e) {
                Log.e(TAG, "Server failed to start", e);
            }
        }).start();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (scriptManager != null) {
            scriptManager.runShutdownScript();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
