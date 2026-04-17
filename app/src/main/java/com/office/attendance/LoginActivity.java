package com.office.attendance;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private ProgressBar progressBar;
    private static final String GITHUB_JSON_URL = "https://raw.githubusercontent.com/YashVaghasiya2710/attendance/main/users.json";
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mDatabase = FirebaseDatabase.getInstance("https://attendance-b55ba-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();

        // Check if already logged in via prefs
        if (getSharedPreferences("prefs", MODE_PRIVATE).contains("user_name")) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        etUsername = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btn_login).setOnClickListener(v -> handleLogin());
    }

    private void handleLogin() {
        String username = etUsername.getText().toString().trim().toLowerCase();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter credentials", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hardcoded Developer Check
        if (username.equals("yash") && password.equals("Vagha@2710")) {
            saveSession(username, true);
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        checkGitHubAndFirebase(username, password);
    }

    private void checkGitHubAndFirebase(String inputUser, String inputPass) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(GITHUB_JSON_URL).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(LoginActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                            Toast.makeText(LoginActivity.this, "Auth Source Error: " + response.code(), Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    if (responseBody != null) {
                        String json = responseBody.string();
                        runOnUiThread(() -> validateAgainstFirebase(json, inputUser, inputPass));
                    }
                }
            }
        });
    }

    private void validateAgainstFirebase(String json, String inputUser, String inputPass) {
        try {
            JSONArray users = new JSONArray(json);
            boolean isAuthorized = false;
            for (int i = 0; i < users.length(); i++) {
                if (users.getJSONObject(i).getString("username").equalsIgnoreCase(inputUser)) {
                    isAuthorized = true;
                    break;
                }
            }

            if (!isAuthorized) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "User not in authorized list", Toast.LENGTH_SHORT).show();
                return;
            }

            mDatabase.child("passwords").child(inputUser).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    String correctPass = snapshot.getValue(String.class);
                    
                    if (correctPass != null && correctPass.equals(inputPass)) {
                        saveSession(inputUser, false);
                    } else {
                        Toast.makeText(LoginActivity.this, "Invalid Password", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(LoginActivity.this, "Database error", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Auth logic error", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveSession(String username, boolean isDev) {
        getSharedPreferences("prefs", MODE_PRIVATE)
                .edit()
                .putString("user_name", username)
                .putBoolean("is_dev", isDev)
                .apply();
        
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}