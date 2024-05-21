package com.example.mybrowser;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.WebView;
import android.widget.ImageButton;
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
    TextInputEditText searchBar;
    WebView webView;
    ExecutorService executorService;
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

        // Initialize the ExecutorService
        executorService = Executors.newSingleThreadExecutor();

        // Check for storage permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
            }
        }

        searchButton.setOnClickListener(view -> {
            String url = searchBar.getText().toString();
            if (!url.isEmpty()) {
                fetchAndDisplayHtml(url);
            } else {
                Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchAndDisplayHtml(String urlString) {
        executorService.execute(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("HEAD");

                // Check the content type
                String contentType = connection.getContentType();
                if (contentType != null && contentType.startsWith("video")) {
                    // Prompt user to download the video file
                    runOnUiThread(() -> offerDownload(urlString));
                } else {
                    // If not a video file, fetch and display the HTML content
                    connection = (HttpURLConnection) url.openConnection();

                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    String cleanedHtml = cleanHtmlContent(response.toString());

                    runOnUiThread(() -> webView.loadData(cleanedHtml, "text/html", "UTF-8"));
                }
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error fetching page", Toast.LENGTH_SHORT).show());
                e.printStackTrace();
            }
        });
    }

    private void offerDownload(String urlString) {
        Toast.makeText(this, "Video file detected. Starting download...", Toast.LENGTH_SHORT).show();
        executorService.execute(() -> downloadFile(urlString));
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

    private String cleanHtmlContent(String html) {
        // Basic cleaning using regex to remove problematic attributes and empty attributes
        html = html.replaceAll("(<[^>]+)\\s+\\w*=\"\"", "$1"); // Remove empty attributes
        html = html.replaceAll("(<[^>]+)\\s+\\w*=''", "$1"); // Remove empty attributes
        // Optionally, you can add more cleaning rules here

        return html;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
