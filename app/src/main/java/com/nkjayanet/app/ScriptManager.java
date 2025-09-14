package com.nkjayanet.app;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.*;

public class ScriptManager {
    private static final String TAG = "ScriptManager";
    private final Context context;
    private final File scriptDir;

    public ScriptManager(Context ctx) {
        this.context = ctx;
        this.scriptDir = new File(ctx.getFilesDir(), "scripts");
        if (!scriptDir.exists()) scriptDir.mkdirs();
    }

    public void extractScripts() {
        try {
            AssetManager assetManager = context.getAssets();
            String[] files = assetManager.list("scripts");
            if (files == null) return;

            for (String file : files) {
                File outFile = new File(scriptDir, file);
                if (outFile.exists()) continue;

                try (InputStream in = assetManager.open("scripts/" + file);
                     OutputStream out = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
                if (file.endsWith(".sh")) outFile.setExecutable(true);
                Log.d(TAG, "Extracted: " + file);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to extract scripts", e);
        }
    }

    public void runServerScript() {
        File serverScript = new File(scriptDir, "server.sh");
        if (!serverScript.exists()) {
            Log.e(TAG, "server.sh not found");
            return;
        }

        try {
            new ProcessBuilder(serverScript.getAbsolutePath())
                .redirectErrorStream(true)
                .start();
            Log.i(TAG, "server.sh executed");
        } catch (IOException e) {
            Log.e(TAG, "Failed to run server.sh", e);
        }
    }

    public void runShutdownScript() {
        File shutdownScript = new File(scriptDir, "shutdown.sh");
        if (!shutdownScript.exists()) {
            Log.e(TAG, "shutdown.sh not found");
            return;
        }

        try {
            new ProcessBuilder(shutdownScript.getAbsolutePath())
                .redirectErrorStream(true)
                .start();
            Log.i(TAG, "shutdown.sh executed");
        } catch (IOException e) {
            Log.e(TAG, "Failed to run shutdown.sh", e);
        }
    }
}
