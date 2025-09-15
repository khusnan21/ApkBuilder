package com.nkjayanet.app;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private TextView logView;
    private ProgressBar loadingBar;
    private WebView webView;
    private final String targetUrl = "http://127.0.0.1:8080/index.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        logView = findViewById(R.id.logView);
        loadingBar = findViewById(R.id.loadingBar);
        webView = findViewById(R.id.webView);

        appendLog("Copying assets...");
        try {
            // Copy all needed asset folders
            copyAssets("www", new File(getFilesDir(), "htdocs"));
            copyAssets("bin", new File(getFilesDir(), "bin"));
            copyAssets("conf", new File(getFilesDir(), "conf"));
            copyAssets("scripts", new File(getFilesDir(), "scripts"));
        } catch (IOException e) {
            appendLog("Failed to copy assets: " + e.getMessage());
            Toast.makeText(this, "Copy asset gagal: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        // Set executable permission for all files in bin/ and scripts/
        setExecutableRecursive(new File(getFilesDir(), "bin"));
        setExecutableRecursive(new File(getFilesDir(), "scripts"));

        appendLog("Starting php-cgi...");
        startPhpCgi();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            appendLog("Starting lighttpd...");
            startLighttpd();
            waitServerAndLoadWebView();
        }, 1000);
    }

    private void appendLog(String msg) {
        runOnUiThread(() -> {
            logView.append(msg + "\n");
            android.util.Log.d("ApkBuilder", msg);
        });
    }

    // Recursively copy all assets
    private void copyAssets(String assetPath, File outDir) throws IOException {
        String[] files = getAssets().list(assetPath);
        if (files == null) return;
        if (!outDir.exists()) outDir.mkdirs();

        for (String filename : files) {
            String inPath = assetPath + "/" + filename;
            File outFile = new File(outDir, filename);
            String[] subfiles = getAssets().list(inPath);
            if (subfiles != null && subfiles.length > 0) {
                copyAssets(inPath, outFile);
            } else {
                try (InputStream in = getAssets().open(inPath);
                     OutputStream out = new FileOutputStream(outFile)) {
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }
            }
        }
    }

    // Set all files in dir (recursively) to executable
    private void setExecutableRecursive(File dir) {
        if (!dir.exists()) return;
        if (dir.isDirectory()) {
            for (File f : dir.listFiles()) setExecutableRecursive(f);
        } else {
            dir.setExecutable(true, false);
            dir.setReadable(true, false);
            dir.setWritable(true, false);
        }
    }

    private void startPhpCgi() {
        try {
            File php = new File(getFilesDir(), "bin/php-cgi");
            if (!php.exists()) {
                appendLog("php-cgi not found!");
                return;
            }
            Process phpProc = Runtime.getRuntime().exec(new String[]{
                php.getAbsolutePath(),
                "-b", "127.0.0.1:9000"
            }, null, getFilesDir());
            logProcess(phpProc, "php-cgi");
            appendLog("php-cgi started.");
        } catch (IOException e) {
            appendLog("php-cgi failed: " + e.getMessage());
        }
    }

    private void startLighttpd() {
        try {
            File lighttpd = new File(getFilesDir(), "bin/lighttpd");
            File conf = new File(getFilesDir(), "conf/lighttpd.conf");
            if (!lighttpd.exists() || !conf.exists()) {
                appendLog("lighttpd/conf file not found!");
                return;
            }
            Process p = Runtime.getRuntime().exec(new String[]{
                lighttpd.getAbsolutePath(),
                "-f", conf.getAbsolutePath()
            }, null, getFilesDir());
            logProcess(p, "lighttpd");
            appendLog("lighttpd started.");
        } catch (IOException e) {
            appendLog("lighttpd failed: " + e.getMessage());
        }
    }

    private void logProcess(Process p, String name) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    appendLog(name + ": " + line);
                }
            } catch (IOException ignored) {}
        }).start();
    }

    // Wait for lighttpd to listen on 8080 before loading WebView
    private void waitServerAndLoadWebView() {
        new Thread(() -> {
            boolean ready = false;
            for (int i = 0; i < 12; i++) { // max 12 detik
                try (Socket s = new Socket()) {
                    s.connect(new InetSocketAddress("127.0.0.1", 8080), 700);
                    ready = true;
                    break;
                } catch (IOException e) {
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                }
            }
            boolean serverReady = ready;
            runOnUiThread(() -> {
                loadingBar.setVisibility(android.view.View.GONE);
                if (serverReady) {
                    setupWebView();
                    webView.loadUrl(targetUrl);
                    appendLog("WebView loading: " + targetUrl);
                } else {
                    appendLog("Server not ready after timeout");
                    Toast.makeText(this, "Server gagal running.", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                appendLog("WebView error: " + description);
                Toast.makeText(MainActivity.this, "WebView error: " + description, Toast.LENGTH_SHORT).show();
            }
        });
    }
                 }
