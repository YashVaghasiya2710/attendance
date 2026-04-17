package com.office.attendance.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.office.attendance.R;
import com.office.attendance.adapter.MonthlyAdapter;
import com.office.attendance.db.DatabaseHelper;
import com.office.attendance.db.FirebaseHelper;
import com.office.attendance.model.AttendanceRecord;
import com.office.attendance.model.Employee;
import com.office.attendance.utils.TimeUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MonthlyFragment extends Fragment implements MonthlyAdapter.OnActionListener {

    private DatabaseHelper db;
    private FirebaseHelper firebaseHelper;
    private RecyclerView recyclerView;
    private MonthlyAdapter adapter;
    private TextView tvMonthLabel, tvNoData;
    private TextView tvTotalHoursStat, tvTotalDaysStat, tvTargetHoursStat;
    private TextView tvDeficitStat, tvDeficitLabel, tvAvgHoursStat, tvBestDayStat;
    private ProgressBar progressDeficit;
    private EditText etTargetInput;

    private Employee employee;
    private int currentYear, currentMonth;
    private double dailyTarget = 9.0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_monthly, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        db = DatabaseHelper.getInstance(requireContext());
        firebaseHelper = new FirebaseHelper(requireContext());
        employee = db.getPrimaryEmployee();

        dailyTarget = requireContext().getSharedPreferences("prefs", 0).getFloat("daily_target", 9.0f);

        Calendar cal = Calendar.getInstance();
        currentYear  = cal.get(Calendar.YEAR);
        currentMonth = cal.get(Calendar.MONTH);

        tvMonthLabel = view.findViewById(R.id.tv_month_label);
        tvNoData     = view.findViewById(R.id.tv_no_data);
        recyclerView = view.findViewById(R.id.recycler_monthly);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        tvTotalHoursStat = view.findViewById(R.id.tv_total_hours_stat);
        tvTotalDaysStat = view.findViewById(R.id.tv_total_days_stat);
        tvTargetHoursStat = view.findViewById(R.id.tv_target_hours_stat);
        tvDeficitStat = view.findViewById(R.id.tv_deficit_stat);
        tvDeficitLabel = view.findViewById(R.id.tv_deficit_label);
        tvAvgHoursStat = view.findViewById(R.id.tv_avg_hours_stat);
        tvBestDayStat = view.findViewById(R.id.tv_best_day_stat);
        progressDeficit = view.findViewById(R.id.progress_deficit);
        
        etTargetInput = view.findViewById(R.id.et_target_input);
        if (etTargetInput != null) {
            etTargetInput.setText(String.valueOf((int)dailyTarget));
            etTargetInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    try {
                        if (!s.toString().isEmpty()) {
                            dailyTarget = Double.parseDouble(s.toString());
                            requireContext().getSharedPreferences("prefs", 0).edit().putFloat("daily_target", (float)dailyTarget).apply();
                            loadData();
                        }
                    } catch (Exception ignored) {}
                }
            });
        }

        view.findViewById(R.id.btn_prev_month).setOnClickListener(v -> {
            currentMonth--;
            if (currentMonth < 0) { currentMonth = 11; currentYear--; }
            loadData();
        });

        view.findViewById(R.id.btn_next_month).setOnClickListener(v -> {
            currentMonth++;
            if (currentMonth > 11) { currentMonth = 0; currentYear++; }
            loadData();
        });

        // Push only UNSYNCED local records to cloud
        List<AttendanceRecord> unsynced = db.getUnsyncedRecords(employee.getId());
        if (!unsynced.isEmpty()) {
            firebaseHelper.syncAllRecords(unsynced, db);
        }

        firebaseHelper.pullRecords(employee.getId(), db, this::loadData);

        loadData();
    }

    public void loadData() {
        if (employee == null) {
            employee = db.getPrimaryEmployee();
        }
        if (employee == null) return;

        tvMonthLabel.setText(TimeUtils.monthYearLabel(currentYear, currentMonth));

        List<AttendanceRecord> dbRecords = db.getMonthRecords(employee.getId(), currentYear, currentMonth + 1);
        List<AttendanceRecord> displayRecords = new ArrayList<>();

        double totalWorked = 0;
        int daysLogged = 0;
        double maxDayHours = 0;

        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, 1);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Always show all days of the month for the table structure
        for (int d = 1; d <= daysInMonth; d++) {
            String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", currentYear, currentMonth + 1, d);
            AttendanceRecord found = null;
            for (AttendanceRecord r : dbRecords) {
                if (r.getDate().equals(dateStr)) {
                    found = r;
                    break;
                }
            }

            if (found != null) {
                displayRecords.add(found);
                if (found.isCheckedIn() && found.isCheckedOut()) {
                    daysLogged++;
                    double h = found.getHoursWorked();
                    totalWorked += h;
                    if (h > maxDayHours) maxDayHours = h;
                }
            } else {
                AttendanceRecord empty = new AttendanceRecord();
                empty.setDate(dateStr);
                empty.setEmployeeId(employee.getId());
                displayRecords.add(empty);
            }
        }

        int totalDaysExpected = 0;
        int daysExpectedUntilToday = 0;
        Calendar today = Calendar.getInstance();
        int ty = today.get(Calendar.YEAR);
        int tm = today.get(Calendar.MONTH);
        int td = today.get(Calendar.DAY_OF_MONTH);

        for (int d = 1; d <= daysInMonth; d++) {
            String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", currentYear, currentMonth + 1, d);
            
            // For monthly total target, just exclude sundays
            if (!isSunday(dateStr)) {
                totalDaysExpected++;
            }

            // Check if this day is currently marked as "WORKING"
            boolean isWorking = false;
            for (AttendanceRecord r : dbRecords) {
                if (r.getDate().equals(dateStr) && r.isCheckedIn() && !r.isCheckedOut()) {
                    isWorking = true;
                    break;
                }
            }

            if (!isSunday(dateStr) && !isWorking) {
                if (currentYear < ty || (currentYear == ty && currentMonth < tm)) {
                    daysExpectedUntilToday++;
                } else if (currentYear == ty && currentMonth == tm && d <= td) {
                    daysExpectedUntilToday++;
                }
            }
        }

        updateStats(totalWorked, daysLogged, dailyTarget, maxDayHours, totalDaysExpected, daysExpectedUntilToday);

        if (adapter == null) {
            adapter = new MonthlyAdapter(displayRecords, this);
            adapter.setDailyTarget(dailyTarget);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateData(displayRecords);
            adapter.setDailyTarget(dailyTarget);
        }
        
        tvNoData.setVisibility(displayRecords.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean isSunday(String dateStr) {
        try {
            String[] parts = dateStr.split("-");
            Calendar c = Calendar.getInstance();
            c.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
            return c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
        } catch (Exception e) {
            return false;
        }
    }

    private void updateStats(double totalHours, int totalDays, double target, double maxDayHours, int totalDaysExpected, int daysExpectedUntilToday) {
        double currentTarget = daysExpectedUntilToday * target;
        double deficit = totalHours - currentTarget;

        tvTotalHoursStat.setText(TimeUtils.formatHours(totalHours));
        tvTotalDaysStat.setText(totalDays + " days logged");
        
        tvTargetHoursStat.setText(TimeUtils.formatHours(totalDaysExpected * target));

        tvDeficitStat.setText(TimeUtils.formatHoursDiff(deficit));
        if (deficit >= 0) {
            tvDeficitStat.setTextColor(requireContext().getColor(R.color.success));
            tvDeficitLabel.setText("✓ ahead of target");
            progressDeficit.setProgressDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.progress_custom));
        } else {
            tvDeficitStat.setTextColor(requireContext().getColor(R.color.danger));
            tvDeficitLabel.setText("✗ behind target");
            progressDeficit.setProgressDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.progress_custom_red));
        }

        double totalTarget = totalDaysExpected * target;
        int progress = totalTarget > 0 ? (int) Math.min((totalHours / totalTarget) * 100, 100) : 0;
        progressDeficit.setProgress(progress);

        double avg = totalDays > 0 ? totalHours / totalDays : 0;
        tvAvgHoursStat.setText(TimeUtils.formatHours(avg));
        tvBestDayStat.setText(maxDayHours > 0 ? TimeUtils.formatHours(maxDayHours) : "--");
    }

    @Override
    public void onDelete(AttendanceRecord record) {
        new AlertDialog.Builder(requireContext(), R.style.DarkAlertDialog)
                .setTitle("Delete Entry")
                .setMessage("Delete record for " + TimeUtils.formatDateShort(record.getDate()) + "?")
                .setPositiveButton("Delete", (d, w) -> {
                    db.deleteRecord(record.getEmployeeId(), record.getDate());
                    firebaseHelper.deleteRecord(record.getDate());
                    loadData();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}