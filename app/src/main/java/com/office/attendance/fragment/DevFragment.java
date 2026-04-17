package com.office.attendance.fragment;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.office.attendance.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DevFragment extends Fragment {

    private RecyclerView rvUsers;
    private DatabaseReference mDatabase;
    private static final String GITHUB_JSON_URL = "https://raw.githubusercontent.com/YashVaghasiya2710/attendance/main/users.json";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dev, container, false);
        rvUsers = view.findViewById(R.id.rv_users);
        rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        
        mDatabase = FirebaseDatabase.getInstance("https://attendance-b55ba-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();
        
        fetchUsersFromGitHub();
        return view;
    }

    private void fetchUsersFromGitHub() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(GITHUB_JSON_URL).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if(getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Network Error: Check Internet", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> 
                                Toast.makeText(getContext(), "GitHub Error: " + response.code() + " (File not found?)", Toast.LENGTH_LONG).show());
                        }
                        return;
                    }

                    if (responseBody != null) {
                        String json = responseBody.string();
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> parseAndShowUsers(json));
                        }
                    }
                }
            }
        });
    }

    private void parseAndShowUsers(String json) {
        try {
            JSONArray array = new JSONArray(json);
            List<String> usernames = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                usernames.add(array.getJSONObject(i).getString("username"));
            }
            rvUsers.setAdapter(new UserManagementAdapter(usernames));
        } catch (Exception e) {
            if (getContext() != null)
                Toast.makeText(getContext(), "Error parsing users", Toast.LENGTH_SHORT).show();
        }
    }

    private class UserManagementAdapter extends RecyclerView.Adapter<UserViewHolder> {
        List<String> users;
        UserManagementAdapter(List<String> users) { this.users = users; }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new UserViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            String username = users.get(position);
            TextView tv = holder.itemView.findViewById(android.R.id.text1);
            tv.setText(username.toUpperCase());
            tv.setTextColor(getResources().getColor(R.color.text));
            holder.itemView.setOnClickListener(v -> showPasswordDialog(username));
        }

        @Override
        public int getItemCount() { return users.size(); }
    }

    private class UserViewHolder extends RecyclerView.ViewHolder {
        public UserViewHolder(@NonNull View itemView) { super(itemView); }
    }

    private void showPasswordDialog(String username) {
        EditText etPass = new EditText(getContext());
        etPass.setHint("Enter New Password");
        etPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(requireContext(), R.style.DarkAlertDialog)
                .setTitle("SET PASSWORD: " + username.toUpperCase())
                .setView(etPass)
                .setPositiveButton("UPDATE", (d, w) -> {
                    String newPass = etPass.getText().toString().trim();
                    if (!newPass.isEmpty()) {
                        mDatabase.child("passwords").child(username).setValue(newPass)
                                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Password Updated", Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }
}