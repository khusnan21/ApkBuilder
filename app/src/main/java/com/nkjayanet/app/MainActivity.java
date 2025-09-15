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

        // Validate all key files
        validateFile("htdocs/admin.php");
        validateFile("htdocs/index.php");
        validateFile("htdocs/router.php");
        validateFile("htdocs/config.json");
        validateFile("bin/php-cgi");
        validateFile("bin/lighttpd");
        validateFile("conf/lighttpd.conf");

        // Start server
        startServerFromConfig();

        // Load WebView with fallback
        File adminPhp = new File(getFilesDir(), "htdocs/admin.php");
        String targetUrl = adminPhp.exists()
            ? "http://127.0.0.1:8080/admin.php"
            : "http://127.0.0.1:8080/index.php";

        appendLog("WebView loading: " + targetUrl);
        loadingBar.setVisibility(View.VISIBLE);
        new Handler().postDelayed(() -> {
            webView.loadUrl(targetUrl);
            loadingBar.setVisibility(View.GONE);
        }, 4000);
    }

    private void startServerFromConfig() {
        File modeFile = new File(getFilesDir(), "conf/server_mode.txt");
        String mode = "php";

        try (BufferedReader reader = new BufferedReader(new FileReader(modeFile))) {
            String line = reader.readLine();
            if (line != null && (line.equalsIgnoreCase("lighttpd") || line.equalsIgnoreCase("php"))) {
                mode = line.trim();
            }
        } catch (IOException e) {
            appendLog("server_mode.txt not found, defaulting to PHP");
        }

        if (mode.equals("lighttpd")) {
            startPhpCgi();
            startLighttpd();
        } else {
            startPhpServer();
        }
    }

    private void startPhpCgi() {
        try {
            File phpCgi = new File(getFilesDir(), "bin/php-cgi");
            File sock = new File(getFilesDir(), "php-fcgi.sock");
            if (sock.exists()) sock.delete();

            if (!phpCgi.setExecutable(true)) {
                appendLog("php-cgi not executable");
                return;
            }

            Process p = Runtime.getRuntime().exec(new String[]{
                phpCgi.getAbsolutePath(),
                "-b", sock.getAbsolutePath()
            });

            logProcess(p);
            appendLog("php-cgi started on socket: " + sock.getAbsolutePath());

        } catch (IOException e) {
            appendLog("php-cgi failed: " + e.getMessage());
        }
    }

    private void startLighttpd() {
        try {
            File lighttpd = new File(getFilesDir(), "bin/lighttpd");
            File conf = new File(getFilesDir(), "conf/lighttpd.conf");

            if (!lighttpd.setExecutable(true)) {
                appendLog("lighttpd not executable");
                return;
            }

            Process p = Runtime.getRuntime().exec(new String[]{
                lighttpd.getAbsolutePath(),
                "-f", conf.getAbsolutePath()
            });

            logProcess(p);
            appendLog("lighttpd started with config: " + conf.getAbsolutePath());

        } catch (IOException e) {
            appendLog("lighttpd failed: " + e.getMessage());
        }
    }

    private void startPhpServer() {
        try {
            File php = new File(getFilesDir(), "bin/php");
            if (!php.exists()) {
                appendLog("PHP binary not found");
                return;
            }
            if (!php.setExecutable(true)) {
                appendLog("PHP binary not executable");
                return;
            }

            File htdocs = new File(getFilesDir(), "htdocs");
            Process p = Runtime.getRuntime().exec(new String[]{
                php.getAbsolutePath(),
                "-S", "127.0.0.1:8080",
                "-t", htdocs.getAbsolutePath()
            });

            logProcess(p);
            appendLog("php -S server started");

        } catch (IOException e) {
            appendLog("php server failed: " + e.getMessage());
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
                appendLog("stdout error: " + e.getMessage());
            }
        }).start();

        new Thread(() -> {
            try {
                String line;
                while ((line = stderr.readLine()) != null) {
                    appendLog("SERVER-ERR: " + line);
                }
            } catch (IOException e) {
                appendLog("stderr error: " + e.getMessage());
            }
        }).start();
    }

    private void appendLog(String msg) {
        runOnUiThread(() -> {
            logView.append(msg + "\n");
            logView.post(() -> logView.scrollTo(0, logView.getBottom()));
        });
    }

    private void validateFile(String relativePath) {
        File f = new File(getFilesDir(), relativePath);
        if (f.exists()) {
            appendLog("✔ " + relativePath + " found (" + f.length() + " bytes)");
        } else {
            appendLog("✘ " + relativePath + " missing");
        }
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
