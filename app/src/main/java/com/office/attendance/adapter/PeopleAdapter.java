package com.office.attendance.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.office.attendance.R;
import com.office.attendance.model.Employee;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PeopleAdapter extends RecyclerView.Adapter<PeopleAdapter.ViewHolder> {

    public interface OnDeleteListener {
        void onDelete(Employee employee, int position);
    }

    private final List<Employee> employees;
    private final OnDeleteListener listener;

    public PeopleAdapter(List<Employee> employees, OnDeleteListener listener) {
        this.employees = employees;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_people_employee, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        Employee emp = employees.get(pos);
        int color = Color.parseColor(emp.getColor());

        h.tvAvatar.setText(emp.getInitials());
        h.tvAvatar.setBackgroundTintList(ColorStateList.valueOf(adjustAlpha(color, 0.15f)));
        h.tvAvatar.setTextColor(color);
        h.tvAvatar.setStrokeColor(ColorStateList.valueOf(color));

        h.tvName.setText(emp.getName());
        h.tvSerial.setText("Employee #" + (pos + 1));

        String added = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(new Date(emp.getCreatedAt()));
        h.tvAdded.setText("Added " + added);

        h.btnDelete.setOnClickListener(v -> listener.onDelete(emp, h.getAdapterPosition()));
    }

    @Override
    public int getItemCount() { return employees.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialButton tvAvatar;
        TextView tvName, tvSerial, tvAdded;
        MaterialButton btnDelete;

        ViewHolder(View v) {
            super(v);
            tvAvatar  = v.findViewById(R.id.tv_avatar);
            tvName    = v.findViewById(R.id.tv_name);
            tvSerial  = v.findViewById(R.id.tv_serial);
            tvAdded   = v.findViewById(R.id.tv_added);
            btnDelete = v.findViewById(R.id.btn_delete);
        }
    }

    private static int adjustAlpha(int color, float factor) {
        int a = Math.round(Color.alpha(color) * factor);
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color));
    }
}