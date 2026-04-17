package com.office.attendance.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.office.attendance.R;
import com.office.attendance.model.AttendanceRecord;
import com.office.attendance.utils.TimeUtils;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MonthlyAdapter extends RecyclerView.Adapter<MonthlyAdapter.ViewHolder> {

    public interface OnActionListener {
        void onDelete(AttendanceRecord record);
    }

    private List<AttendanceRecord> records;
    private final OnActionListener listener;
    private double dailyTarget = 9.0;

    public MonthlyAdapter(List<AttendanceRecord> records, OnActionListener listener) {
        this.records = records;
        this.listener = listener;
    }

    public void setDailyTarget(double target) {
        this.dailyTarget = target;
        notifyDataSetChanged();
    }

    public void updateData(List<AttendanceRecord> newRecords) {
        this.records = newRecords;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_day_record, parent, false);
        return new ViewHolder(v);
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

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        AttendanceRecord rec = records.get(pos);
        boolean sunday = isSunday(rec.getDate());

        h.tvDate.setText(TimeUtils.formatDateShort(rec.getDate()));
        
        if (rec.isCheckedIn()) {
            h.tvIn.setText(TimeUtils.formatTimeShort(rec.getCheckIn()));
            if (rec.isCheckedOut()) {
                h.tvOut.setText(TimeUtils.formatTimeShort(rec.getCheckOut()));
                
                double worked = rec.getHoursWorked();
                h.tvHours.setText(TimeUtils.formatHours(worked));
                
                double diff = worked - dailyTarget;
                h.tvDiff.setText(TimeUtils.formatHoursDiff(diff));
                h.tvDiff.setTextColor(h.itemView.getContext().getColor(diff >= 0 ? R.color.success : R.color.danger));

                // Color coding like the HTML pills
                if (worked >= dailyTarget) {
                    h.tvHours.setTextColor(h.itemView.getContext().getColor(R.color.success));
                } else if (worked >= dailyTarget * 0.85) {
                    h.tvHours.setTextColor(h.itemView.getContext().getColor(R.color.accent));
                } else {
                    h.tvHours.setTextColor(h.itemView.getContext().getColor(R.color.danger));
                }
            } else {
                h.tvOut.setText("--:--");
                h.tvHours.setText("WORKING");
                h.tvDiff.setText("");
                h.tvHours.setTextColor(h.itemView.getContext().getColor(R.color.accent));
            }
        } else {
            h.tvIn.setText("--:--");
            h.tvOut.setText("--:--");
            h.tvHours.setText(sunday ? "HOLIDAY" : "ABSENT");
            h.tvDiff.setText("");
            h.tvHours.setTextColor(h.itemView.getContext().getColor(sunday ? R.color.accent : R.color.muted));
        }

        h.itemView.setOnLongClickListener(v -> {
            if (listener != null && rec.isCheckedIn()) {
                listener.onDelete(rec);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() { return records.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvIn, tvOut, tvHours, tvDiff;

        ViewHolder(View v) {
            super(v);
            tvDate  = v.findViewById(R.id.tv_day_date);
            tvIn    = v.findViewById(R.id.tv_day_in);
            tvOut   = v.findViewById(R.id.tv_day_out);
            tvHours = v.findViewById(R.id.tv_day_hours);
            tvDiff  = v.findViewById(R.id.tv_day_diff);
        }
    }
}