package com.office.attendance;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private ProgressBar progressBar;
    private static final String GITHUB_JSON_URL = "https://raw.githubusercontent.com/YashVaghasiya2710/attendance/main/users.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Check if already logged in via prefs
        if (getSharedPreferences("prefs", MODE_PRIVATE).contains("user_name")) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        etUsername = findViewById(R.id.et_email); // Reusing existing ID from layout
        etPassword = findViewById(R.id.et_password);
        progressBar = findViewById(R.id.progressBar); // Make sure to add this to XML if missing

        findViewById(R.id.btn_login).setOnClickListener(v -> handleGitHubAuth());
    }

    private void handleGitHubAuth() {
        String username = etUsername.getText().toString().trim().toLowerCase();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter credentials", Toast.LENGTH_SHORT).show();
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(GITHUB_JSON_URL)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(LoginActivity.this, "Network error. Check connection.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "Error fetching user list", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                String jsonData = response.body().string();
                runOnUiThread(() -> validateUser(jsonData, username, password));
            }
        });
    }

    private void validateUser(String json, String inputUser, String inputPass) {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        try {
            JSONArray users = new JSONArray(json);
            boolean found = false;

            for (int i = 0; i < users.length(); i++) {
                JSONObject user = users.getJSONObject(i);
                if (user.getString("username").equalsIgnoreCase(inputUser) &&
                    user.getString("password").equals(inputPass)) {
                    
                    // Success! Save username to prefs (this acts as Login)
                    getSharedPreferences("prefs", MODE_PRIVATE)
                            .edit()
                            .putString("user_name", inputUser)
                            .apply();
                    
                    found = true;
                    break;
                }
            }

            if (found) {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Invalid Username or Password", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e("Auth", "JSON Parse error", e);
            Toast.makeText(this, "Auth system error", Toast.LENGTH_SHORT).show();
        }
    }
}