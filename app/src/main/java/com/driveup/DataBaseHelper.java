package com.driveup;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.driveup.ui.ride.Ride;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class DataBaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "driveup.db";
    private static final int DB_VERSION = 1;
    private static DataBaseHelper instance;

    public static synchronized DataBaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DataBaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DataBaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS ride (\n" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "    date TEXT NOT NULL,\n" +
                "    start_hour TEXT NOT NULL,\n" +
                "    end_hour TEXT NOT NULL,\n" +
                "    price REAL NOT NULL\n" +
                ");\n");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    // CRUD des courses
    public long insertRide(Ride ride) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("date", ride.getDate().toString());
        values.put("start_hour", ride.getStartHour().toString());
        values.put("end_hour", ride.getEndHour().toString());
        values.put("price", ride.getPrice());
        
        long id = db.insert("ride", null, values);
        db.close();
        return id;
    }

    public List<Ride> getAllRides() {
        List<Ride> rides = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM ride ORDER BY date DESC, start_hour DESC", null);
        
        android.util.Log.d("DataBaseHelper", "Found " + cursor.getCount() + " rides in database");
        
        if (cursor.moveToFirst()) {
            do {
                Ride ride = new Ride();
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                ride.setId(id);
                ride.setDate(LocalDate.parse(cursor.getString(cursor.getColumnIndexOrThrow("date"))));
                ride.setStartHour(LocalTime.parse(cursor.getString(cursor.getColumnIndexOrThrow("start_hour"))));
                ride.setEndHour(LocalTime.parse(cursor.getString(cursor.getColumnIndexOrThrow("end_hour"))));
                ride.setPrice(cursor.getDouble(cursor.getColumnIndexOrThrow("price")));
                rides.add(ride);
                android.util.Log.d("DataBaseHelper", "Loaded ride ID: " + id + ", Date: " + ride.getDate() + ", Price: " + ride.getPrice());
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        android.util.Log.d("DataBaseHelper", "Returning " + rides.size() + " rides");
        return rides;
    }

    public void deleteAllRides() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("ride", null, null);
        db.close();
    }

    public boolean deleteRide(long rideId) {
        android.util.Log.d("DataBaseHelper", "Attempting to delete ride with ID: " + rideId);
        SQLiteDatabase db = this.getWritableDatabase();
        
        // First, let's check if the ride exists
        Cursor cursor = db.query("ride", new String[]{"id"}, "id = ?", new String[]{String.valueOf(rideId)}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        android.util.Log.d("DataBaseHelper", "Ride with ID " + rideId + " exists: " + exists);
        cursor.close();
        
        int deletedRows = db.delete("ride", "id = ?", new String[]{String.valueOf(rideId)});
        db.close();
        android.util.Log.d("DataBaseHelper", "Deleted " + deletedRows + " rows for ride ID: " + rideId);
        return deletedRows > 0;
    }

    public void insertRides(List<Ride> rides) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.beginTransaction();
            for (Ride ride : rides) {
                ContentValues values = new ContentValues();
                values.put("date", ride.getDate().toString());
                values.put("start_hour", ride.getStartHour().toString());
                values.put("end_hour", ride.getEndHour().toString());
                values.put("price", ride.getPrice());
                long id = db.insert("ride", null, values);
                ride.setId(id); // Set the ID for the imported ride
                android.util.Log.d("DataBaseHelper", "Inserted ride with ID: " + id + ", Date: " + ride.getDate() + ", Price: " + ride.getPrice());
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }
}
