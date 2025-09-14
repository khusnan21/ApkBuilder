package com.nkjayanet.app;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Mulai service server
        startService(new Intent(this, ServerService.class));

        // Load layout WebView
        webView = new WebView(this);
        setContentView(webView);

        // Konfigurasi WebView
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());

        // Akses dashboard lokal
        webView.loadUrl("http://127.0.0.1:8080/index.php");
    }
}