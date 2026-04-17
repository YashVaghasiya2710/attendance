package com.office.attendance.model;

public class AttendanceRecord {
    private int id;
    private int employeeId;
    private String date; // YYYY-MM-DD
    private long checkIn;  // millis, 0 = not checked in
    private long checkOut; // millis, 0 = not checked out

    public AttendanceRecord() {}

    public AttendanceRecord(int id, int employeeId, String date, long checkIn, long checkOut) {
        this.id = id;
        this.employeeId = employeeId;
        this.date = date;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public long getCheckIn() { return checkIn; }
    public void setCheckIn(long checkIn) { this.checkIn = checkIn; }

    public long getCheckOut() { return checkOut; }
    public void setCheckOut(long checkOut) { this.checkOut = checkOut; }

    public boolean isCheckedIn() { return checkIn > 0; }
    public boolean isCheckedOut() { return checkOut > 0; }

    /** Returns worked hours, or -1 if incomplete */
    public double getHoursWorked() {
        if (checkIn <= 0 || checkOut <= 0) return -1;
        return (checkOut - checkIn) / 3600000.0;
    }

    /** Format as "Xh YYm" */
    public String getFormattedHours() {
        double h = getHoursWorked();
        if (h < 0) return "--";
        int hours = (int) h;
        int mins = (int) Math.round((h - hours) * 60);
        return String.format("%dh %02dm", hours, mins);
    }
}