package com.office.attendance.db;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.office.attendance.model.AttendanceRecord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private static final String REF_ATTENDANCE = "attendance";
    
    private final DatabaseReference dbRef;
    private final Context context;

    public FirebaseHelper(Context context) {
        this.dbRef = FirebaseDatabase.getInstance("https://attendance-b55ba-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference(REF_ATTENDANCE);
        this.context = context;
    }

    public String getCurrentUserId() {
        return context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                .getString("user_name", "default_user");
    }

    public void syncRecord(AttendanceRecord record, DatabaseHelper dbHelper) {
        String userId = getCurrentUserId();
        DatabaseReference recordRef = dbRef.child(userId).child(record.getDate());

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("date", record.getDate());
        data.put("checkIn", record.getCheckIn());
        data.put("checkOut", record.getCheckOut());
        data.put("updatedAt", System.currentTimeMillis());

        recordRef.setValue(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Record synced to RTDB");
                    dbHelper.markAsSynced(record.getEmployeeId(), record.getDate());
                })
                .addOnFailureListener(e -> Log.e(TAG, "RTDB Sync failed", e));
    }

    public void syncAllRecords(List<AttendanceRecord> records, DatabaseHelper dbHelper) {
        if (records.isEmpty()) return;
        
        String userId = getCurrentUserId();
        Map<String, Object> updates = new HashMap<>();
        long now = System.currentTimeMillis();
        
        for (AttendanceRecord record : records) {
            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("date", record.getDate());
            data.put("checkIn", record.getCheckIn());
            data.put("checkOut", record.getCheckOut());
            data.put("updatedAt", now);
            updates.put(userId + "/" + record.getDate(), data);
        }

        dbRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Bulk sync successful");
                    for (AttendanceRecord record : records) {
                        dbHelper.markAsSynced(record.getEmployeeId(), record.getDate());
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Bulk sync failed", e));
    }

    public void deleteRecord(String date) {
        String userId = getCurrentUserId();
        dbRef.child(userId).child(date).removeValue()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Record deleted from RTDB"))
                .addOnFailureListener(e -> Log.e(TAG, "RTDB Delete failed", e));
    }

    public interface SyncCallback {
        void onSyncComplete();
    }

    public void pullRecords(int employeeId, DatabaseHelper localDb, SyncCallback callback) {
        String userId = getCurrentUserId();
        dbRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                    String date = dateSnapshot.child("date").getValue(String.class);
                    Long inT = dateSnapshot.child("checkIn").getValue(Long.class);
                    Long outT = dateSnapshot.child("checkOut").getValue(Long.class);
                    
                    if (date != null && inT != null) {
                        // Mark as synced immediately when pulling from cloud
                        localDb.updateRecord(employeeId, date, inT, outT != null ? outT : 0);
                        localDb.markAsSynced(employeeId, date);
                    }
                }
                if (callback != null) callback.onSyncComplete();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "RTDB Pull failed", error.toException());
                if (callback != null) callback.onSyncComplete();
            }
        });
    }
}