package com.mobapps.silentcompanion;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationListener;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;

public class SilentModeService extends Service {
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private boolean isSilentModeActive = false;
    private final List<String[]> switchDataList = new ArrayList<>();
    private final Handler handler = new Handler();
    private final Runnable silentModeChecker = new Runnable() {
        @Override
        public void run() {
            handleSilentModeState();
            handler.postDelayed(this, 1000); // Adjust interval as needed
        }
    };

    public void turnOnSilentMode(Context context, String currentMode) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null && notificationManager.isNotificationPolicyAccessGranted()) {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                if ("vibration".equalsIgnoreCase(currentMode)) {
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                    Toast.makeText(context, "Vibration mode activated. To enable silent with vibration, please adjust your settings if needed.", Toast.LENGTH_LONG).show();
                } else {
                    // Default to silent mode
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                }
            }
        } else {
            // Permission not granted, ask user to enable it
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if ("vibration".equalsIgnoreCase(currentMode)) {
                Toast.makeText(context, "Grant DND access for vibration mode in settings.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "Grant DND access permission in settings.", Toast.LENGTH_LONG).show();
            }
            startActivity(intent);
        }
    }


    public static void turnOffSilentMode(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            boolean isChecked = intent.getBooleanExtra("isChecked", false);
            String initialTime = intent.getStringExtra("initialTime");
            String finalTime = intent.getStringExtra("finalTime");
            String days = intent.getStringExtra("days");
            String latitude = intent.getStringExtra("latitude");
            String longitude = intent.getStringExtra("longitude");
            String radius = intent.getStringExtra("radius");
            String mode = intent.getStringExtra("mode");

            if (isChecked) {
                switchDataList.add(new String[]{initialTime, finalTime, days, latitude, longitude, radius,mode});
            } else {
                switchDataList.removeIf(schedule -> schedule[0].equals(initialTime) && schedule[1].equals(finalTime) && schedule[2].equals(days));
            }
        }

        // Stop service if no schedules are active
        if (switchDataList.isEmpty()) {
            if (isSilentModeActive) {
                turnOffSilentMode(getApplicationContext()); // Turn off silent mode
                isSilentModeActive = false;
            }
            stopForeground(true); // Stop the foreground service
            stopSelf(); // Stop the service
            return START_NOT_STICKY; // Prevent service from restarting
        }

        // Restart the checker if necessary
        handler.removeCallbacks(silentModeChecker);
        handler.post(silentModeChecker);

        // Ensure the service runs in the foreground
        updateNotification("Service Running", "Silent Companion is active.");
        return START_STICKY; // Ensures the service restarts after being killed
    }

    private void handleSilentModeState() {
        boolean shouldTurnOnSilentMode = false;
        boolean locationInRange = false; // To track if location is within range
        String activeInitialTime = "";
        String activeFinalTime = "";
        String currentMode = "";
        int caseType = 0;

        // Iterate through the schedules
        for (String[] schedule : switchDataList) {
            String startTime = schedule[0];
            String endTime = schedule[1];
            String scheduleDays = schedule[2];
            String latitude = schedule[3];
            String longitude = schedule[4];
            String radius = schedule[5];
            String mode = schedule[6];

            // Time-only case
            if (latitude.isEmpty() && longitude.isEmpty() && !startTime.isEmpty() && !endTime.isEmpty()) {
                if (isTimeForSilentMode(startTime, endTime) && isCurrentDayInArray(scheduleDays)) {
                    shouldTurnOnSilentMode = true;
                    activeInitialTime = startTime;
                    activeFinalTime = endTime;
                    currentMode = mode;
                    caseType = 1;
                    break;
                }
            }
            // Location-only case
            else if (!latitude.isEmpty() && !longitude.isEmpty() && startTime.isEmpty() && endTime.isEmpty()) {
                if (isCurrentDayInArray(scheduleDays) && isLocationInRange(latitude, longitude, radius)) {
                    shouldTurnOnSilentMode = true;
                    locationInRange = true;
                    currentMode = mode;
                    caseType = 2;
                    break;
                }
            }
            // Time and Location case
            else if (!latitude.isEmpty() && !longitude.isEmpty() && !startTime.isEmpty() && !endTime.isEmpty()) {
                if (isTimeForSilentMode(startTime, endTime) && isCurrentDayInArray(scheduleDays) && isLocationInRange(latitude, longitude, radius)) {
                    shouldTurnOnSilentMode = true;
                    activeInitialTime = startTime;
                    activeFinalTime = endTime;
                    currentMode = mode;
                    locationInRange = true;
                    caseType = 3;
                    break;
                }
            }
        }

        // Handle silent mode based on caseType
        if (caseType == 1) {
            handleSilentMode(shouldTurnOnSilentMode, activeInitialTime, activeFinalTime,currentMode);
        } else if (caseType == 2) {
            handleSilentModeWithLocationOnly(shouldTurnOnSilentMode, locationInRange,currentMode);
        } else if (caseType == 3) {
            handleSilentModeWithTimeAndLocation(shouldTurnOnSilentMode, locationInRange, activeInitialTime, activeFinalTime,currentMode);
        }

        // If no schedules are active, ensure silent mode is off
        if (!shouldTurnOnSilentMode && isSilentModeActive) {
            turnOffSilentMode(getApplicationContext());
            isSilentModeActive = false;
            stopNotification();
        }
    }

    private void handleSilentMode(boolean shouldTurnOnSilentMode, String activeInitialTime, String activeFinalTime, String currentMode) {
        if (shouldTurnOnSilentMode && !isSilentModeActive) {
            turnOnSilentMode(getApplicationContext(), currentMode);
            isSilentModeActive = true;
            updateNotification("Silent Mode Activated", "Silent mode is ON from " + activeInitialTime + " to " + activeFinalTime);
        } else if (!shouldTurnOnSilentMode && isSilentModeActive) {
            turnOffSilentMode(getApplicationContext());
            isSilentModeActive = false;
            stopNotification();
        }
    }

    // Method to handle silent mode for Location-only case
    private void handleSilentModeWithLocationOnly(boolean shouldTurnOnSilentMode, boolean locationInRange,String currentMode) {
        if (shouldTurnOnSilentMode && locationInRange && !isSilentModeActive) {
            turnOnSilentMode(getApplicationContext(), currentMode);
            isSilentModeActive = true;
            updateNotification("Silent Mode Activated", "Silent mode is ON at the location");
        } else if ((!shouldTurnOnSilentMode || !locationInRange) && isSilentModeActive) {
            turnOffSilentMode(getApplicationContext());
            isSilentModeActive = false;
            stopNotification();
        }
    }

    // Method to handle silent mode for Time and Location case
    private void handleSilentModeWithTimeAndLocation(boolean shouldTurnOnSilentMode, boolean locationInRange, String activeInitialTime, String activeFinalTime,String currentMode) {
        if (shouldTurnOnSilentMode && locationInRange && !isSilentModeActive) {
            turnOnSilentMode(getApplicationContext(), currentMode);
            isSilentModeActive = true;
            updateNotification("Silent Mode Activated", "Silent mode is ON from " + activeInitialTime + " to " + activeFinalTime + " at the location");
        } else if ((!shouldTurnOnSilentMode || !locationInRange) && isSilentModeActive) {
            turnOffSilentMode(getApplicationContext());
            isSilentModeActive = false;
            stopNotification();
        }
    }


    public static boolean isTimeForSilentMode(String initialTime, String finalTime) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        String currentTimeString = timeFormat.format(calendar.getTime());

        try {
            Date initial = timeFormat.parse(initialTime);
            Date finalT = timeFormat.parse(finalTime);
            Date current = timeFormat.parse(currentTimeString);

            if (current != null && initial != null && finalT != null) {
                if (finalT.before(initial)) {
                    if (current.after(initial) || current.before(finalT) || current.equals(finalT)) {
                        return true;
                    }
                } else {
                    if ((current.after(initial) || current.equals(initial)) && !current.after(finalT)) {
                        return true;
                    }
                }
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isCurrentDayInArray(String daysArray) {
        String currentDay = new SimpleDateFormat("EEE", Locale.getDefault()).format(new Date());

        if ("everyday".equalsIgnoreCase(daysArray.trim())) {
            return true;
        }

        String[] days = daysArray.split(",");
        for (String day : days) {
            if (currentDay.equalsIgnoreCase(day.trim())) {
                return true;
            }
        }

        return false;
    }
    private void updateNotification(String title, String content) {
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean notificationsEnabled = sharedPreferences.getBoolean("notificationsEnabled", true);

        if (!notificationsEnabled) {
            return; // Do not show the notification if notifications are disabled
        }

        createNotificationChannel(); // Ensure channel is created

        Intent notificationIntent = new Intent(this, MainActivity2.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, "silent_mode_channel") // Match channel ID
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_notification) // Ensure this icon exists
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW) // Prevents heads-up notifications
                .build();

        startForeground(1, notification); // Start foreground service
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "silent_mode_channel",  // Channel ID (must match in NotificationCompat.Builder)
                    "Silent Mode Notifications",  // Channel Name
                    NotificationManager.IMPORTANCE_LOW // Prevents sound or vibration
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }



    private void stopNotification() {
        stopForeground(true);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                    .setMinUpdateIntervalMillis(5000)
                    .build();

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        return;
                    }
                    for (Location location : locationResult.getLocations()) {
                        Log.d("LocationListener", "Location changed: " + location.getLatitude() + ", " + location.getLongitude());
                    }
                }
            };

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    public boolean isLocationInRange(String latitude, String longitude, String radius) {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e("SilentModeService", "Location permissions not granted.");
                return false;
            }

            double latitudeDouble = Double.parseDouble(latitude);
            double longitudeDouble = Double.parseDouble(longitude);
            float radiusFloat = Float.parseFloat(radius);

            Task<Location> locationTask = fusedLocationClient.getLastLocation();
            while (!locationTask.isComplete()) {
                // Wait for location result
            }

            Location currentLocation = locationTask.getResult();
            if (currentLocation != null) {
                Location targetLocation = new Location("TargetLocation");
                targetLocation.setLatitude(latitudeDouble);
                targetLocation.setLongitude(longitudeDouble);

                float distance = currentLocation.distanceTo(targetLocation);
                return distance <= radiusFloat;
            }
        } catch (Exception e) {
            Log.e("SilentModeService", "Error calculating location: " + e.getMessage());
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(silentModeChecker);

        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
