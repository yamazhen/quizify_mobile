package com.yamazhen.quizify_mobile;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.provider.OpenableColumns;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private TextView filePathTextView;
    private Uri selectedFileUri;
    private String selectedFileName;

    private final ActivityResultLauncher<Intent> pdfPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedFileUri = result.getData().getData();
                    if (selectedFileUri != null) {
                        selectedFileName = getFileNameFromUri(selectedFileUri);
                        filePathTextView.setText(selectedFileName != null ? selectedFileName : "Unknown File");
                    } else {
                        filePathTextView.setText("No PDF selected");
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button uploadButton = findViewById(R.id.upload_button);
        filePathTextView = findViewById(R.id.file_path);
        Button submitButton = findViewById(R.id.submit_button);

        uploadButton.setOnClickListener(view -> {
            // Launch file picker
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            pdfPickerLauncher.launch(intent);
        });

        submitButton.setOnClickListener(v -> {
            if (selectedFileUri != null && selectedFileName != null) {
                uploadPdfToServer(selectedFileUri, selectedFileName);
            } else {
                Toast.makeText(MainActivity.this, "No PDF selected for upload", Toast.LENGTH_SHORT).show();
            }
        });

        loadResources();
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = null;

        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1)
                        fileName = cursor.getString(nameIndex);
                }
            }
        }

        if (fileName == null) {
            fileName = uri.getLastPathSegment();
        }

        return fileName;
    }

    private void uploadPdfToServer(Uri fileUri, String fileName) {
        // Convert Uri to a File
        File tempFile = new File(getCacheDir(), fileName);
        try (InputStream inputStream = getContentResolver().openInputStream(fileUri);
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to prepare file for upload", Toast.LENGTH_SHORT).show();
            return;
        }

        // Now we have tempFile ready to upload
        String uploadUrl = "http://10.0.2.2:3000/upload-pdf"; // Update this if needed

        OkHttpClient client = new OkHttpClient();
        RequestBody fileBody = RequestBody.create(tempFile, MediaType.parse("application/pdf"));

        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("pdf", fileName, fileBody)
                .build();

        Request request = new Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .addHeader("file-name", fileName) // optional if your server expects it
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "Server error: " + response.code(), Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                String responseBody = response.body().string();
                try {
                    JSONObject json = new JSONObject(responseBody);
                    JSONArray questionsArray = json.getJSONArray("questions");
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "Received " + questionsArray.length() + " questions", Toast.LENGTH_SHORT).show()
                    );
                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "JSON parse error", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void loadResources() {
        String resourcesUrl = "http://10.0.2.2:3000/resources"; // Adjust if needed

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(resourcesUrl).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Failed to load resources: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "Server error: " + response.code(), Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                String responseBody = response.body().string();
                try {
                    JSONObject json = new JSONObject(responseBody);
                    JSONArray resourcesArray = json.getJSONArray("questions");
                    // Adjust key if needed. The endpoint returns { questions: [...] }

                    runOnUiThread(() -> {
                        populateTable(resourcesArray);
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "JSON parse error", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void populateTable(JSONArray resources) {
        TableLayout table = findViewById(R.id.files_table);

        // Remove old rows if any (except the header)
        // Assuming the first row is the header row:
        while (table.getChildCount() > 1) {
            table.removeViewAt(1);
        }

        // Loop through the resources and create rows
        for (int i = 0; i < resources.length(); i++) {
            try {
                JSONObject resource = resources.getJSONObject(i);
                String fileName = resource.getString("fileName");
                String questionCount = resource.getString("questionCount");

                TableRow row = new TableRow(this);
                TableRow.LayoutParams params = new TableRow.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        2f
                );
                TableRow.LayoutParams buttonHolderParam = new TableRow.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                );


                TextView fileNameText = new TextView(this);
                fileNameText.setText(fileName);
                fileNameText.setLayoutParams(params);
                fileNameText.setPadding(16,16,16,16);

                TextView questionCountText = new TextView(this);
                questionCountText.setText(questionCount);
                questionCountText.setLayoutParams(params);
                questionCountText.setPadding(16,16,16,16);

                LinearLayout buttonHoldLayout = new LinearLayout(this);
                buttonHoldLayout.setLayoutParams(buttonHolderParam);
                buttonHoldLayout.setPadding(16,16,16,16);

                ImageButton startButton = new ImageButton(this);
                Drawable playIcon = getResources().getDrawable(R.drawable.ic_play, null);
                startButton.setBackgroundResource(R.drawable.round_button);
                startButton.setImageDrawable(playIcon);
                startButton.setPadding(20,20,20,20);
                startButton.setOnClickListener(view -> {
                    Intent intent = new Intent(MainActivity.this, QuizActivity.class);
                    intent.putExtra("fileName", fileName);
                    startActivity(intent);
                });

                buttonHoldLayout.addView(startButton);

                row.addView(fileNameText);
                row.addView(questionCountText);
                row.addView(buttonHoldLayout);
                table.addView(row);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

}
