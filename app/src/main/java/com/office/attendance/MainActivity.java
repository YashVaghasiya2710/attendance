package com.office.attendance;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.office.attendance.fragment.MonthlyFragment;
import com.office.attendance.fragment.TodayFragment;

public class MainActivity extends AppCompatActivity {

    private TodayFragment todayFragment;
    private MonthlyFragment monthlyFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        todayFragment   = new TodayFragment();
        monthlyFragment = new MonthlyFragment();

        // Load default fragment
        loadFragment(todayFragment);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_today) {
                loadFragment(todayFragment);
            } else if (id == R.id.nav_monthly) {
                monthlyFragment = new MonthlyFragment(); 
                loadFragment(monthlyFragment);
            }
            return true;
        });

        checkUserSetup();
    }

    private void checkUserSetup() {
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        if (!prefs.contains("user_name")) {
            showUserSetupDialog();
        }
    }

    private void showUserSetupDialog() {
        // Simple name entry dialog
        EditText etName = new EditText(this);
        etName.setHint("Enter Your Name (e.g. Yash)");
        etName.setPadding(60, 40, 60, 40);

        new AlertDialog.Builder(this, R.style.DarkAlertDialog)
                .setTitle("INITIAL SETUP")
                .setMessage("Please enter your name for cloud syncing:")
                .setView(etName)
                .setCancelable(false)
                .setPositiveButton("SAVE", (d, w) -> {
                    String name = etName.getText().toString().trim().toLowerCase();
                    if (!name.isEmpty()) {
                        getSharedPreferences("prefs", MODE_PRIVATE)
                                .edit()
                                .putString("user_name", name)
                                .apply();
                    } else {
                        showUserSetupDialog(); // Require name
                    }
                })
                .show();
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, fragment);
        ft.commit();
    }
}