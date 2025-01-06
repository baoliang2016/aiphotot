package com.example.aiphoto;

import android.content.Context;
import okhttp3.*;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;

public class ApiClient {
    private static final String API_URL = "https://api.deepseek.com/v1/analyze";
    private static final String API_KEY = "sk-7f9c6c17b1ac4c648d7fd3c6910add02";

    private final OkHttpClient client;
    private final Context context;

    public interface ApiCallback {
        void onSuccess(String analysis, String description);
        void onError(String errorMessage);
    }

    public ApiClient(Context context) {
        this.context = context;
        this.client = new OkHttpClient();
    }

    public void analyzeImage(File imageFile, ApiCallback callback) {
        // Create request body
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", imageFile.getName(),
                        RequestBody.create(imageFile, MediaType.parse("image/jpeg")))
                .build();

        // Create request
        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + API_KEY)
                .post(requestBody)
                .build();

        // Execute request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONObject json = new JSONObject(responseData);

                        String analysis = json.optString("analysis", "");
                        String description = json.optString("description", "");

                        callback.onSuccess(analysis, description);
                    } catch (Exception e) {
                        callback.onError("Failed to parse response: " + e.getMessage());
                    }
                } else {
                    callback.onError("API request failed with code: " + response.code());
                }
            }
        });
    }
}
