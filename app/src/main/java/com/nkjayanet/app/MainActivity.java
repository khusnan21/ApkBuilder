package com.nkjayanet.app;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.WebChromeClient;
import android.webkit.ConsoleMessage;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import java.io.*;

public class MainActivity extends AppCompatActivity {

    WebView webView;
    ProgressBar loadingBar;
    TextView logView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        loadingBar = findViewById(R.id.loadingBar);
        logView = findViewById(R.id.logView);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                Toast.makeText(MainActivity.this, "WebView error: " + err.getDescription(), Toast.LENGTH_LONG).show();
                appendLog("WebView error: " + err.getDescription());
                loadingBar.setVisibility(View.GONE);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            public boolean onConsoleMessage(ConsoleMessage msg) {
                appendLog("WebView console: " + msg.message());
                return true;
            }
        });

        // Copy all assets
        copyAssets("www", new File(getFilesDir(), "htdocs"));
        copyAssets("bin", new File(getFilesDir(), "bin"));
        copyAssets("conf", new File(getFilesDir(), "conf"));
        copyAssets("scripts", new File(getFilesDir(), "scripts"));

        // Start server
        startServerFromConfig();

        // Delay before loading WebView
        loadingBar.setVisibility(View.VISIBLE);
        new Handler().postDelayed(() -> {
            webView.loadUrl("http://127.0.0.1:8080/");
            appendLog("WebView loading: http://127.0.0.1:8080/");
            loadingBar.setVisibility(View.GONE);
        }, 4000); // delay 4 detik
    }

    private void startServerFromConfig() {
        File modeFile = new File(getFilesDir(), "conf/server_mode.txt");
        String mode = "php"; // default

        try (BufferedReader reader = new BufferedReader(new FileReader(modeFile))) {
            String line = reader.readLine();
            if (line != null && (line.equalsIgnoreCase("lighttpd") || line.equalsIgnoreCase("php"))) {
                mode = line.trim();
            }
        } catch (IOException e) {
            appendLog("Config error: server_mode.txt not found, defaulting to PHP");
        }

        if (mode.equals("lighttpd")) {
            startLighttpd();
        } else {
            startPhpServer();
        }
    }

    private void startPhpServer() {
        try {
            File php = new File(getFilesDir(), "bin/php");
            if (!php.setExecutable(true)) {
                Toast.makeText(this, "PHP binary not executable", Toast.LENGTH_LONG).show();
                appendLog("PHP binary not executable");
                return;
            }

            File htdocs = new File(getFilesDir(), "htdocs");
            appendLog("Starting PHP server...");

            Process p = Runtime.getRuntime().exec(new String[]{
                php.getAbsolutePath(),
                "-S", "127.0.0.1:8080",
                "-t", htdocs.getAbsolutePath()
            });

            logProcess(p);

        } catch (IOException e) {
            Toast.makeText(this, "PHP server failed to start", Toast.LENGTH_LONG).show();
            appendLog("PHP server failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startLighttpd() {
        try {
            File lighttpd = new File(getFilesDir(), "bin/lighttpd");
            File conf = new File(getFilesDir(), "conf/lighttpd.conf");
            if (!lighttpd.setExecutable(true)) {
                Toast.makeText(this, "Lighttpd binary not executable", Toast.LENGTH_LONG).show();
                appendLog("Lighttpd binary not executable");
                return;
            }

            appendLog("Starting Lighttpd server...");

            Process p = Runtime.getRuntime().exec(new String[]{
                lighttpd.getAbsolutePath(),
                "-f", conf.getAbsolutePath()
            });

            logProcess(p);

        } catch (IOException e) {
            Toast.makeText(this, "Lighttpd failed to start", Toast.LENGTH_LONG).show();
            appendLog("Lighttpd failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void logProcess(Process p) {
        BufferedReader stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));

        new Thread(() -> {
            try {
                String line;
                while ((line = stdout.readLine()) != null) {
                    appendLog("SERVER-OUT: " + line);
                }
            } catch (IOException e) {
                appendLog("Log stdout error: " + e.getMessage());
            }
        }).start();

        new Thread(() -> {
            try {
                String line;
                while ((line = stderr.readLine()) != null) {
                    appendLog("SERVER-ERR: " + line);
                }
            } catch (IOException e) {
                appendLog("Log stderr error: " + e.getMessage());
            }
        }).start();
    }

    private void appendLog(String msg) {
        runOnUiThread(() -> {
            logView.append(msg + "\n");
            logView.post(() -> logView.scrollTo(0, logView.getBottom()));
        });
    }

    private void copyAssets(String srcDir, File dstDir) {
        try {
            String[] assets = getAssets().list(srcDir);
            if (assets == null || assets.length == 0) return;

            if (!dstDir.exists()) dstDir.mkdirs();

            for (String name : assets) {
                String fullPath = srcDir + "/" + name;
                String[] subAssets = getAssets().list(fullPath);
                File outFile = new File(dstDir, name);

                if (subAssets != null && subAssets.length > 0) {
                    copyAssets(fullPath, outFile); // recursive
                } else {
                    InputStream in = getAssets().open(fullPath);
                    OutputStream out = new FileOutputStream(outFile);
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    in.close();
                    out.flush();
                    out.close();
                }
            }
        } catch (IOException e) {
            appendLog("Asset copy error: " + e.getMessage());
        }
    }
}
