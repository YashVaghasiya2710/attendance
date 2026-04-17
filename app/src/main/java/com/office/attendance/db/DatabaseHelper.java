package com.office.attendance.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.office.attendance.model.AttendanceRecord;
import com.office.attendance.model.Employee;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "attendance.db";
    private static final int DB_VERSION = 2; // Incremented version to add SYNCED column
    private static DatabaseHelper instance;

    // Employees table
    public static final String TABLE_EMPLOYEES = "employees";
    public static final String COL_EMP_ID = "id";
    public static final String COL_EMP_NAME = "name";
    public static final String COL_EMP_COLOR = "color";
    public static final String COL_EMP_CREATED = "created_at";

    // Attendance table
    public static final String TABLE_ATTENDANCE = "attendance";
    public static final String COL_ATT_ID = "id";
    public static final String COL_ATT_EMP_ID = "employee_id";
    public static final String COL_ATT_DATE = "date";
    public static final String COL_ATT_CHECKIN = "check_in";
    public static final String COL_ATT_CHECKOUT = "check_out";
    public static final String COL_ATT_SYNCED = "synced";

    private static final String CREATE_EMPLOYEES =
            "CREATE TABLE " + TABLE_EMPLOYEES + " (" +
                    COL_EMP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_EMP_NAME + " TEXT NOT NULL, " +
                    COL_EMP_COLOR + " TEXT NOT NULL, " +
                    COL_EMP_CREATED + " INTEGER NOT NULL)";

    private static final String CREATE_ATTENDANCE =
            "CREATE TABLE " + TABLE_ATTENDANCE + " (" +
                    COL_ATT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_ATT_EMP_ID + " INTEGER NOT NULL, " +
                    COL_ATT_DATE + " TEXT NOT NULL, " +
                    COL_ATT_CHECKIN + " INTEGER DEFAULT 0, " +
                    COL_ATT_CHECKOUT + " INTEGER DEFAULT 0, " +
                    COL_ATT_SYNCED + " INTEGER DEFAULT 0, " +
                    "UNIQUE(" + COL_ATT_EMP_ID + ", " + COL_ATT_DATE + "))";

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_EMPLOYEES);
        db.execSQL(CREATE_ATTENDANCE);
        
        // Add a default employee
        ContentValues values = new ContentValues();
        values.put(COL_EMP_NAME, "Admin");
        values.put(COL_EMP_COLOR, "#C8F060");
        values.put(COL_EMP_CREATED, System.currentTimeMillis());
        db.insert(TABLE_EMPLOYEES, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_ATTENDANCE + " ADD COLUMN " + COL_ATT_SYNCED + " INTEGER DEFAULT 0");
        }
    }

    public long addEmployee(String name, String color) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_EMP_NAME, name);
        values.put(COL_EMP_COLOR, color);
        values.put(COL_EMP_CREATED, System.currentTimeMillis());
        return db.insert(TABLE_EMPLOYEES, null, values);
    }

    public List<Employee> getAllEmployees() {
        List<Employee> employees = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_EMPLOYEES, null, null, null, null, null, COL_EMP_NAME + " ASC");

        if (cursor.moveToFirst()) {
            do {
                Employee e = new Employee();
                e.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_EMP_ID)));
                e.setName(cursor.getString(cursor.getColumnIndexOrThrow(COL_EMP_NAME)));
                e.setColor(cursor.getString(cursor.getColumnIndexOrThrow(COL_EMP_COLOR)));
                employees.add(e);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return employees;
    }

    public void deleteEmployee(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_EMPLOYEES, COL_EMP_ID + "=?", new String[]{String.valueOf(id)});
        db.delete(TABLE_ATTENDANCE, COL_ATT_EMP_ID + "=?", new String[]{String.valueOf(id)});
    }

    public AttendanceRecord getRecord(int employeeId, String date) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_ATTENDANCE, null,
                COL_ATT_EMP_ID + "=? AND " + COL_ATT_DATE + "=?",
                new String[]{String.valueOf(employeeId), date}, null, null, null);

        AttendanceRecord record = null;
        if (cursor.moveToFirst()) {
            record = new AttendanceRecord();
            record.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ATT_ID)));
            record.setEmployeeId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ATT_EMP_ID)));
            record.setDate(cursor.getString(cursor.getColumnIndexOrThrow(COL_ATT_DATE)));
            record.setCheckIn(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ATT_CHECKIN)));
            record.setCheckOut(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ATT_CHECKOUT)));
        }
        cursor.close();
        return record;
    }

    public void updateRecord(int employeeId, String date, long checkIn, long checkOut) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_ATT_EMP_ID, employeeId);
        cv.put(COL_ATT_DATE, date);
        cv.put(COL_ATT_CHECKIN, checkIn);
        cv.put(COL_ATT_CHECKOUT, checkOut);
        cv.put(COL_ATT_SYNCED, 0); // Mark for re-sync
        db.insertWithOnConflict(TABLE_ATTENDANCE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void markAsSynced(int employeeId, String date) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_ATT_SYNCED, 1);
        db.update(TABLE_ATTENDANCE, values, COL_ATT_EMP_ID + "=? AND " + COL_ATT_DATE + "=?",
                new String[]{String.valueOf(employeeId), date});
    }

    public List<AttendanceRecord> getUnsyncedRecords(int employeeId) {
        List<AttendanceRecord> records = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_ATTENDANCE, null, 
                COL_ATT_EMP_ID + "=? AND " + COL_ATT_SYNCED + "=0",
                new String[]{String.valueOf(employeeId)}, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                AttendanceRecord r = new AttendanceRecord();
                r.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ATT_ID)));
                r.setEmployeeId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ATT_EMP_ID)));
                r.setDate(cursor.getString(cursor.getColumnIndexOrThrow(COL_ATT_DATE)));
                r.setCheckIn(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ATT_CHECKIN)));
                r.setCheckOut(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ATT_CHECKOUT)));
                records.add(r);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return records;
    }

    public void deleteRecord(int employeeId, String date) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_ATTENDANCE, COL_ATT_EMP_ID + "=? AND " + COL_ATT_DATE + "=?",
                new String[]{String.valueOf(employeeId), date});
    }

    public Employee getPrimaryEmployee() {
        List<Employee> list = getAllEmployees();
        return list.isEmpty() ? null : list.get(0);
    }

    public List<AttendanceRecord> getMonthRecords(int employeeId, int year, int month) {
        List<AttendanceRecord> records = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String monthStr = String.format("%04d-%02d", year, month);
        Cursor cursor = db.query(TABLE_ATTENDANCE, null,
                COL_ATT_EMP_ID + "=? AND " + COL_ATT_DATE + " LIKE ?",
                new String[]{String.valueOf(employeeId), monthStr + "%"}, null, null, COL_ATT_DATE + " ASC");

        if (cursor.moveToFirst()) {
            do {
                AttendanceRecord r = new AttendanceRecord();
                r.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ATT_ID)));
                r.setEmployeeId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ATT_EMP_ID)));
                r.setDate(cursor.getString(cursor.getColumnIndexOrThrow(COL_ATT_DATE)));
                r.setCheckIn(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ATT_CHECKIN)));
                r.setCheckOut(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ATT_CHECKOUT)));
                records.add(r);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return records;
    }

    public double getMonthTotalHours(int employeeId, int year, int month) {
        List<AttendanceRecord> records = getMonthRecords(employeeId, year, month);
        double total = 0;
        for (AttendanceRecord r : records) {
            double h = r.getHoursWorked();
            if (h > 0) total += h;
        }
        return total;
    }

    public int getMonthPresentDays(int employeeId, int year, int month) {
        List<AttendanceRecord> records = getMonthRecords(employeeId, year, month);
        int count = 0;
        for (AttendanceRecord r : records) {
            if (r.isCheckedIn()) count++;
        }
        return count;
    }

    public List<AttendanceRecord> getAllRecords(int employeeId) {
        List<AttendanceRecord> records = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ATTENDANCE, null, COL_ATT_EMP_ID + "=?",
                new String[]{String.valueOf(employeeId)}, null, null, COL_ATT_DATE + " ASC");

        if (cursor.moveToFirst()) {
            do {
                AttendanceRecord r = new AttendanceRecord();
                r.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ATT_ID)));
                r.setEmployeeId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ATT_EMP_ID)));
                r.setDate(cursor.getString(cursor.getColumnIndexOrThrow(COL_ATT_DATE)));
                r.setCheckIn(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ATT_CHECKIN)));
                r.setCheckOut(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ATT_CHECKOUT)));
                records.add(r);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return records;
    }
}