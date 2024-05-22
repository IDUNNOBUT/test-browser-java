package com.example.mybrowser;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    ImageButton searchButton;
    Button htmlButton;
    TextInputEditText searchBar;
    WebView webView;

    String pageContent = "";

    Dialog popup;
    private static final int REQUEST_WRITE_STORAGE = 112;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        searchButton = findViewById(R.id.searchButton);
        searchBar = findViewById(R.id.searchBar);
        webView = findViewById(R.id.webView);
        htmlButton = findViewById(R.id.htmlButton);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
            }
        }

        popup = new Dialog(this);

        popup.setContentView(R.layout.popup);

        TextView textView = popup.findViewById(R.id.textView);

        searchButton.setOnClickListener(view -> {
            String url = searchBar.getText().toString();
            if (!url.isEmpty()) {
                fetchAndDisplayHtml(url);
            } else {
                Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show();
            }
        });

        htmlButton.setOnClickListener(view-> {
            if (!pageContent.isEmpty()) {
                textView.setText(pageContent);
                popup.show();
            }
        });
    }
    private void fetchAndDisplayHtml(String urlString) {
        Handler handler = new Handler(Looper.getMainLooper());

        new Thread(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                String contentType = connection.getContentType();
                if (contentType != null && contentType.startsWith("video"))
                {
                    downloadFile(urlString);
                }
                else {
                    try {
                        connection.connect();

                        InputStream inputStream = connection.getInputStream();
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                        StringBuilder stringBuilder = new StringBuilder();
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            stringBuilder.append(line).append("\n");
                        }

                        pageContent = stringBuilder.toString();

                        handler.post(() -> webView.loadDataWithBaseURL(urlString, pageContent, "text/html", "UTF-8", null));
                    }
                    finally {
                        connection.disconnect();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void downloadFile(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to download file", Toast.LENGTH_SHORT).show());
                return;
            }

            String fileName = urlString.substring(urlString.lastIndexOf('/') + 1);
            String storagePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();

            InputStream input = new BufferedInputStream(connection.getInputStream());
            FileOutputStream output = new FileOutputStream(storagePath + "/" + fileName);

            byte[] data = new byte[4096];
            int count;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }

            output.flush();
            output.close();
            input.close();

            runOnUiThread(() -> Toast.makeText(MainActivity.this, "File downloaded: " + fileName, Toast.LENGTH_LONG).show());
        } catch (IOException e) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error downloading file", Toast.LENGTH_SHORT).show());
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
