package com.mobapps.silentcompanion;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "silent_companion.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_NAME = "Schedules";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_SCHEDULE_NAME = "schedule_name";
    public static final String COLUMN_INITIAL_TIME = "initial_time";
    public static final String COLUMN_FINAL_TIME = "final_time";
    public static final String COLUMN_MODE = "mode";
    public static final String COLUMN_DAYS = "days";
    public static final String COLUMN_RADIUS = "radius";
    public static final String COLUMN_LATITUDE= "latitude";
    public static final String COLUMN_LONGITUDE= "longitude";
    public static final String COLUMN_SWITCH_STATE = "switch_state";

    public static final String TABLE_NAME1 = "contacts";
    public static final String CONTACT_ID = "id";
    public static final String CONTACT_NAME = "contact_name";
    public static final String CONTACT_NUMBER = "contact_number";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_SCHEDULE_NAME + " TEXT, " +
                COLUMN_INITIAL_TIME + " TEXT, " +
                COLUMN_FINAL_TIME + " TEXT, " +
                COLUMN_MODE + " TEXT, " +
                COLUMN_DAYS + " TEXT, " +
                COLUMN_RADIUS + " TEXT, " +
                COLUMN_LATITUDE + " TEXT, " +
                COLUMN_LONGITUDE + " TEXT, " +
                COLUMN_SWITCH_STATE + " INTEGER DEFAULT 0)";

        db.execSQL(CREATE_TABLE);

        String CREATE_CONTACT_TABLE = "CREATE TABLE " + TABLE_NAME1 + " (" +
                CONTACT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                CONTACT_NAME + " TEXT, " +
                CONTACT_NUMBER + " TEXT )";
        db.execSQL(CREATE_CONTACT_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME1);
        onCreate(db);
    }


    public static class Schedule {
        private final Integer id;
        private final String name;
        private final String initialTime;
        private final String finalTime;
        private final String mode;
        private final String days;
        private final String radius;
        private final String latitude;
        private final String longitude;
        private final int switchState;

        // Constructor
        public Schedule(int id, String name, String initialTime, String finalTime, String mode, String days, String radius, String latitude,String longitude,int switchState) {
            this.id = id;
            this.name = name;
            this.initialTime = initialTime;
            this.finalTime = finalTime;
            this.mode = mode;
            this.days = days;
            this.radius = radius;
            this.latitude = latitude;
            this.longitude = longitude;
            this.switchState = switchState;
        }

        // Getter methods
        public Integer getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getInitialTime() {
            return initialTime;
        }

        public String getFinalTime() {
            return finalTime;
        }

        public String getMode() {
            return mode;
        }

        public String getDays() {
            return days;
        }

        public String getRadius() {
            return radius;
        }

        public String getLatitude() {
            return latitude;
        }
        public String getLongitude(){
            return longitude;
        }
        public int getSwitchState() {
            return switchState;
        }

    }

    public List<Schedule> readAllSchedules() {
        List<Schedule> scheduleList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);

        if (cursor.moveToFirst()) {
            do {
                Schedule schedule = new Schedule(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SCHEDULE_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INITIAL_TIME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FINAL_TIME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MODE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DAYS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RADIUS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SWITCH_STATE))
                );
                scheduleList.add(schedule);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return scheduleList;
    }


    public List<ContactAdapter.Contact> readAllContacts() {
        List<ContactAdapter.Contact> contactList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME1, null);

        if (cursor.moveToFirst()) {
            do {
                ContactAdapter.Contact contact = new ContactAdapter.Contact(
                        cursor.getInt(cursor.getColumnIndexOrThrow(CONTACT_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(CONTACT_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(CONTACT_NUMBER))
                );
                contactList.add(contact);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return contactList;
    }




    public boolean insertSchedule(String scheduleName, String initialTime, String finalTime, String mode,String days ,String radius,String latitude,String longitude ) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SCHEDULE_NAME, scheduleName);
        values.put(COLUMN_INITIAL_TIME, initialTime);
        values.put(COLUMN_FINAL_TIME, finalTime);
        values.put(COLUMN_MODE, mode);
        values.put(COLUMN_DAYS, days);
        values.put(COLUMN_DAYS, days);
        values.put(COLUMN_RADIUS, radius);
        values.put(COLUMN_LATITUDE, latitude);
        values.put(COLUMN_LONGITUDE, longitude);

        long result = db.insert(TABLE_NAME, null, values);
        return result != -1; // Return true if insertion is successful
    }
    public boolean insertContact(String contact_name, String contact_number) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(CONTACT_NAME, contact_name);
        values.put(CONTACT_NUMBER, contact_number);
        long result = db.insert(TABLE_NAME1, null, values);
        return result != -1; // Return true if insertion is successful
    }


    public boolean updateSchedule(int id, String scheduleName, String initialTime, String finalTime, String mode,String days,String latitude,String longitude,String radius) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SCHEDULE_NAME, scheduleName);
        values.put(COLUMN_INITIAL_TIME, initialTime);
        values.put(COLUMN_FINAL_TIME, finalTime);
        values.put(COLUMN_MODE, mode);
        values.put(COLUMN_DAYS, days);
        values.put(COLUMN_LATITUDE, latitude);
        values.put(COLUMN_LONGITUDE ,longitude);
        values.put(COLUMN_RADIUS, radius);

        int result = db.update(TABLE_NAME, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        return result > 0; // Returns true if update is successful
    }

    public boolean deleteSchedule(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_NAME, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        return result > 0; // Returns true if deletion is successful
    }

    public boolean deleteContact(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_NAME1, CONTACT_ID + " = ?", new String[]{String.valueOf(id)});
        return result > 0; // Returns true if deletion is successful
    }
    public boolean updateScheduleSwitchState(int id, int switchState) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SWITCH_STATE, switchState); // Update the switch state
        int result = db.update(TABLE_NAME, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        return result > 0; // Returns true if update was successful
    }

    public boolean addSchedule(Schedule schedule) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // Add the schedule fields to the ContentValues
        values.put(COLUMN_SCHEDULE_NAME, schedule.getName());
        values.put(COLUMN_INITIAL_TIME, schedule.getInitialTime());
        values.put(COLUMN_FINAL_TIME, schedule.getFinalTime());
        values.put(COLUMN_MODE, schedule.getMode());
        values.put(COLUMN_DAYS, schedule.getDays());
        values.put(COLUMN_RADIUS, schedule.getRadius());
        values.put(COLUMN_LATITUDE, schedule.getLatitude());
        values.put(COLUMN_LONGITUDE, schedule.getLongitude());
        values.put(COLUMN_SWITCH_STATE, schedule.getSwitchState());

        // Insert the new schedule into the database
        long result = db.insert(TABLE_NAME, null, values);
        db.close();

        // Return true if insertion was successful
        return result != -1;
    }





}
