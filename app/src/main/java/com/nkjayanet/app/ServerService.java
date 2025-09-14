package com.nkjayanet.app;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.IBinder;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;

public class ServerService extends Service {
    private static final String CHANNEL_ID = "NKJayaNetChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NK Jaya Net")
            .setContentText("Server aktif di 127.0.0.1:8080")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build();
        startForeground(1, notification);

        // Jalankan start.sh
        try {
            File scriptsDir = new File(getFilesDir(), "scripts");
            File startScript = new File(scriptsDir, "start.sh");

            // Pastikan bisa dieksekusi
            startScript.setExecutable(true);

            // Jalankan proses
            new ProcessBuilder(startScript.getAbsolutePath())
                .directory(scriptsDir)
                .start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "NK Jaya Net Channel",
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}