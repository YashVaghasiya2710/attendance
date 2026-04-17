package com.office.attendance;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.office.attendance.fragment.DevFragment;
import com.office.attendance.fragment.MonthlyFragment;
import com.office.attendance.fragment.TodayFragment;

public class MainActivity extends AppCompatActivity {

    private TodayFragment todayFragment;
    private MonthlyFragment monthlyFragment;
    private DevFragment devFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        todayFragment   = new TodayFragment();
        monthlyFragment = new MonthlyFragment();
        devFragment     = new DevFragment();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        boolean isDev = prefs.getBoolean("is_dev", false);
        
        MenuItem devItem = bottomNav.getMenu().findItem(R.id.nav_dev);
        if (devItem != null) {
            devItem.setVisible(isDev);
        }

        loadFragment(todayFragment);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_today) {
                loadFragment(todayFragment);
            } else if (id == R.id.nav_monthly) {
                loadFragment(new MonthlyFragment());
            } else if (id == R.id.nav_dev) {
                loadFragment(devFragment);
            }
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, fragment);
        ft.commit();
    }
}