package com.office.attendance.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.office.attendance.R;
import com.office.attendance.model.AttendanceRecord;
import com.office.attendance.model.Employee;
import com.office.attendance.utils.TimeUtils;

import java.util.List;

public class TodayAdapter extends RecyclerView.Adapter<TodayAdapter.ViewHolder> {

    public interface OnActionListener {
        void onCheckIn(Employee employee, String time);
        void onCheckOut(Employee employee, String time);
        void onManualUpdate(Employee employee, String checkIn, String checkOut);
    }

    private final List<Employee> employees;
    private final List<AttendanceRecord> records;
    private final OnActionListener listener;

    public TodayAdapter(List<Employee> employees, List<AttendanceRecord> records, OnActionListener listener) {
        this.employees = employees;
        this.records = records;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_today_employee, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        Employee emp = employees.get(pos);
        AttendanceRecord rec = records.get(pos);

        h.tvName.setText(emp.getName());

        boolean checkedIn  = rec != null && rec.isCheckedIn();
        boolean checkedOut = rec != null && rec.isCheckedOut();

        String currentTime = TimeUtils.formatTimeShort(System.currentTimeMillis());

        if (checkedIn) {
            h.etCheckIn.setText(TimeUtils.formatTimeShort(rec.getCheckIn()));
            if (checkedOut) {
                h.etCheckOut.setText(TimeUtils.formatTimeShort(rec.getCheckOut()));
            } else {
                h.etCheckOut.setText(currentTime);
            }
        } else {
            h.etCheckIn.setText(currentTime);
            h.etCheckOut.setText("");
        }

        h.btnSaveTime.setOnClickListener(v -> {
            String inStr = h.etCheckIn.getText().toString().trim();
            String outStr = h.etCheckOut.getText().toString().trim();
            listener.onManualUpdate(emp, inStr, outStr);
        });

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                h.btnSaveTime.setVisibility(View.VISIBLE);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };

        h.etCheckIn.addTextChangedListener(watcher);
        h.etCheckOut.addTextChangedListener(watcher);
        h.btnSaveTime.setVisibility(View.GONE);

        if (checkedIn) {
            h.llHoursRow.setVisibility(View.VISIBLE);
            if (checkedOut) {
                h.tvHours.setText(rec.getFormattedHours());
                h.tvStatus.setText("COMPLETED");
                h.tvStatus.setTextColor(h.itemView.getContext().getColor(R.color.muted));
                h.btnAction.setVisibility(View.GONE);
            } else {
                h.tvHours.setText("WORKING...");
                h.tvStatus.setText("IN OFFICE");
                h.tvStatus.setTextColor(h.itemView.getContext().getColor(R.color.success));
                h.btnAction.setVisibility(View.VISIBLE);
                h.btnAction.setText("OUT");
                h.btnAction.setBackgroundTintList(ColorStateList.valueOf(h.itemView.getContext().getColor(R.color.danger)));
                h.btnAction.setOnClickListener(v -> {
                    String outTime = h.etCheckOut.getText().toString().trim();
                    listener.onCheckOut(emp, outTime);
                });
            }
        } else {
            h.llHoursRow.setVisibility(View.GONE);
            h.btnAction.setVisibility(View.VISIBLE);
            h.btnAction.setText("IN");
            h.btnAction.setBackgroundTintList(ColorStateList.valueOf(h.itemView.getContext().getColor(R.color.accent)));
            h.btnAction.setOnClickListener(v -> {
                String inTime = h.etCheckIn.getText().toString().trim();
                listener.onCheckIn(emp, inTime);
            });
        }
    }

    @Override
    public int getItemCount() { return employees.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvHours, tvStatus;
        EditText etCheckIn, etCheckOut;
        ImageButton btnSaveTime;
        MaterialButton btnAction;
        LinearLayout llHoursRow;

        ViewHolder(View v) {
            super(v);
            tvName       = v.findViewById(R.id.tv_name);
            etCheckIn    = v.findViewById(R.id.et_checkin);
            etCheckOut   = v.findViewById(R.id.et_checkout);
            btnSaveTime  = v.findViewById(R.id.btn_save_time);
            tvHours      = v.findViewById(R.id.tv_hours);
            tvStatus     = v.findViewById(R.id.tv_status);
            btnAction    = v.findViewById(R.id.btn_action);
            llHoursRow   = v.findViewById(R.id.ll_hours_row);
        }
    }
}