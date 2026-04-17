package com.office.attendance.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.google.android.material.button.MaterialButton;
import com.office.attendance.R;
import com.office.attendance.db.DatabaseHelper;
import com.office.attendance.db.FirebaseHelper;
import com.office.attendance.model.AttendanceRecord;
import com.office.attendance.model.Employee;
import com.office.attendance.utils.TimeUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TodayFragment extends Fragment {

    private DatabaseHelper db;
    private FirebaseHelper firebaseHelper;
    private Employee employee;
    private AttendanceRecord record;

    private EditText etDate, etIn, etOut, etTarget;
    private MaterialButton btnAction, btnImport;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_today, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        db = DatabaseHelper.getInstance(requireContext());
        firebaseHelper = new FirebaseHelper(requireContext());
        employee = db.getPrimaryEmployee();

        etDate   = view.findViewById(R.id.et_entry_date);
        etIn     = view.findViewById(R.id.et_entry_in);
        etOut    = view.findViewById(R.id.et_entry_out);
        etTarget = view.findViewById(R.id.et_target_input);
        btnAction = view.findViewById(R.id.btn_add_log);
        btnImport = view.findViewById(R.id.btn_import_history);

        btnImport.setOnClickListener(v -> showImportDialog());

        float dailyTarget = requireContext().getSharedPreferences("prefs", 0).getFloat("daily_target", 9.0f);
        etTarget.setText(String.valueOf((int)dailyTarget));
        etTarget.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                try {
                    if (!s.toString().isEmpty()) {
                        float val = Float.parseFloat(s.toString());
                        requireContext().getSharedPreferences("prefs", 0).edit().putFloat("daily_target", val).apply();
                    }
                } catch (Exception ignored) {}
            }
        });

        // Set current date by default
        etDate.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        
        // Load existing record for selected date if any
        loadRecordForDate(etDate.getText().toString());

        etDate.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                loadRecordForDate(s.toString());
            }
        });

        btnAction.setOnClickListener(v -> saveEntry());
    }

    private void showImportDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_import, null);
        EditText etData = dialogView.findViewById(R.id.et_import_data);
        TextView tvPrompt = dialogView.findViewById(R.id.tv_import_prompt);
        View btnCopy = dialogView.findViewById(R.id.btn_copy_prompt);

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String currentMonth = sdf.format(cal.getTime());
        cal.add(Calendar.MONTH, -1);
        String pastMonth = sdf.format(cal.getTime());

        String prompt = "Extract dates and times for " + pastMonth + " and " + currentMonth + " from the provided chat. " +
                "IGNORE any other months. " +
                "Return ONLY a JSON array like: [{\"date\": \"YYYY-MM-DD\", \"in\": \"hh:mm AM/PM\", \"out\": \"hh:mm AM/PM\"}]. " +
                "Use empty string if OUT is missing.";
        tvPrompt.setText(prompt);

        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Gemini Prompt", tvPrompt.getText());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), "Prompt copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        new AlertDialog.Builder(requireContext(), R.style.DarkAlertDialog)
                .setView(dialogView)
                .setPositiveButton("IMPORT", (dialog, which) -> {
                    String json = etData.getText().toString();
                    if (!json.isEmpty()) {
                        processImport(json);
                    }
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void processImport(String json) {
        try {
            JSONArray arr = new JSONArray(json);
            int count = 0;

            // Define allowed range: current and past month
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.add(Calendar.MONTH, -1);
            String minDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String date = obj.getString("date");
                
                // Only import if date is within the last 2 months
                if (date.compareTo(minDate) < 0) {
                    continue; 
                }

                String inT = obj.getString("in");
                String outT = obj.optString("out", "");

                long inMillis = TimeUtils.parseTimeShort(inT, date);
                long outMillis = outT.isEmpty() ? 0 : TimeUtils.parseTimeShort(outT, date);

                if (inMillis > 0) {
                    db.updateRecord(employee.getId(), date, inMillis, outMillis);
                    AttendanceRecord r = db.getRecord(employee.getId(), date);
                    if (r != null) firebaseHelper.syncRecord(r, db);
                    count++;
                }
            }
            Toast.makeText(getContext(), "Imported " + count + " records", Toast.LENGTH_SHORT).show();
            loadRecordForDate(etDate.getText().toString());
        } catch (Exception e) {
            Toast.makeText(getContext(), "Invalid JSON format", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadRecordForDate(String date) {
        record = db.getRecord(employee.getId(), date);
        String currentTime = TimeUtils.formatTimeShort(System.currentTimeMillis());

        if (record != null) {
            etIn.setText(TimeUtils.formatTimeShort(record.getCheckIn()));
            if (record.isCheckedOut()) {
                etOut.setText(TimeUtils.formatTimeShort(record.getCheckOut()));
                btnAction.setText("UPDATE LOG");
            } else {
                etOut.setText(currentTime);
                btnAction.setText("OUT");
            }
        } else {
            etIn.setText(currentTime);
            etOut.setText("");
            btnAction.setText("IN");
        }
    }

    private void saveEntry() {
        String date = etDate.getText().toString();
        String inT  = etIn.getText().toString();
        String outT = etOut.getText().toString();

        if (date.isEmpty() || inT.isEmpty()) {
            Toast.makeText(getContext(), "Fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        long inMillis = TimeUtils.parseTimeShort(inT, date);
        long outMillis = outT.isEmpty() ? 0 : TimeUtils.parseTimeShort(outT, date);

        if (outMillis != 0 && outMillis <= inMillis) {
            Toast.makeText(getContext(), "Out-time must be after In-time", Toast.LENGTH_SHORT).show();
            return;
        }

        db.updateRecord(employee.getId(), date, inMillis, outMillis);
        AttendanceRecord r = db.getRecord(employee.getId(), date);
        if (r != null) firebaseHelper.syncRecord(r, db);

        Toast.makeText(getContext(), "Log updated", Toast.LENGTH_SHORT).show();
        loadRecordForDate(date);
    }
}