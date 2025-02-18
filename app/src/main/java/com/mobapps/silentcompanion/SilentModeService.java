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
public class SilentModeService extends Service {

    private static final int REQUEST_LOCATION_PERMISSION = 1;
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

    public void turnOnSilentMode(Context context) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null && notificationManager.isNotificationPolicyAccessGranted()) {
            // Permission granted, change ringer mode
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            }
        } else {
            // Permission not granted, ask user to enable it
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            Toast.makeText(this, "Grant DND access permission in settings.", Toast.LENGTH_LONG).show();
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

            if (isChecked) {
                switchDataList.add(new String[]{initialTime, finalTime, days, latitude, longitude, radius});
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
        int caseType = 0;

        // Iterate through the schedules
        for (String[] schedule : switchDataList) {
            String startTime = schedule[0];
            String endTime = schedule[1];
            String scheduleDays = schedule[2];
            String latitude = schedule[3];
            String longitude = schedule[4];
            String radius = schedule[5];

            // Time-only case
            if (latitude.isEmpty() && longitude.isEmpty() && !startTime.isEmpty() && !endTime.isEmpty()) {
                if (isTimeForSilentMode(startTime, endTime) && isCurrentDayInArray(scheduleDays)) {
                    shouldTurnOnSilentMode = true;
                    activeInitialTime = startTime;
                    activeFinalTime = endTime;
                    caseType = 1;
                    break;
                }
            }
            // Location-only case
            else if (!latitude.isEmpty() && !longitude.isEmpty() && startTime.isEmpty() && endTime.isEmpty()) {
                if (isCurrentDayInArray(scheduleDays) && isLocationInRange(latitude, longitude, radius)) {
                    shouldTurnOnSilentMode = true;
                    locationInRange = true;
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
                    locationInRange = true;
                    caseType = 3;
                    break;
                }
            }
        }

        // Handle silent mode based on caseType
        if (caseType == 1) {
            handleSilentMode(shouldTurnOnSilentMode, activeInitialTime, activeFinalTime);
        } else if (caseType == 2) {
            handleSilentModeWithLocationOnly(shouldTurnOnSilentMode, locationInRange);
        } else if (caseType == 3) {
            handleSilentModeWithTimeAndLocation(shouldTurnOnSilentMode, locationInRange, activeInitialTime, activeFinalTime);
        }

        // If no schedules are active, ensure silent mode is off
        if (!shouldTurnOnSilentMode && isSilentModeActive) {
            turnOffSilentMode(getApplicationContext());
            isSilentModeActive = false;
            stopNotification();
        }
    }

    // Method to handle silent mode for Time-only case
    private void handleSilentMode(boolean shouldTurnOnSilentMode, String activeInitialTime, String activeFinalTime) {
        if (shouldTurnOnSilentMode && !isSilentModeActive) {
            turnOnSilentMode(getApplicationContext());
            isSilentModeActive = true;
            updateNotification("Silent Mode Activated", "Silent mode is ON from " + activeInitialTime + " to " + activeFinalTime);
        } else if (!shouldTurnOnSilentMode && isSilentModeActive) {
            turnOffSilentMode(getApplicationContext());
            isSilentModeActive = false;
            stopNotification();
        }
    }

    // Method to handle silent mode for Location-only case
    private void handleSilentModeWithLocationOnly(boolean shouldTurnOnSilentMode, boolean locationInRange) {
        if (shouldTurnOnSilentMode && locationInRange && !isSilentModeActive) {
            turnOnSilentMode(getApplicationContext());
            isSilentModeActive = true;
            updateNotification("Silent Mode Activated", "Silent mode is ON at the location");
        } else if ((!shouldTurnOnSilentMode || !locationInRange) && isSilentModeActive) {
            turnOffSilentMode(getApplicationContext());
            isSilentModeActive = false;
            stopNotification();
        }
    }

    // Method to handle silent mode for Time and Location case
    private void handleSilentModeWithTimeAndLocation(boolean shouldTurnOnSilentMode, boolean locationInRange, String activeInitialTime, String activeFinalTime) {
        if (shouldTurnOnSilentMode && locationInRange && !isSilentModeActive) {
            turnOnSilentMode(getApplicationContext());
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

    // Method to check if location is in range
    public boolean isLocationInRange(String latitude, String longitude, String radius) {
        try {
            // Check if permission is granted
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Permission not granted, return false
                Log.e("SilentModeService", "Location permissions not granted.");
                return false; // Handle the case where permission isn't granted
            }

            double latitudeDouble = Double.parseDouble(latitude);
            double longitudeDouble = Double.parseDouble(longitude);
            float radiusFloat = Float.parseFloat(radius);

            // Get the location manager system service
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            // Check if the location service is available
            if (locationManager == null) {
                Log.e("SilentModeService", "Location Manager is not available");
                return false;
            }

            Location currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (currentLocation != null) {
                Location targetLocation = new Location("TargetLocation");
                targetLocation.setLatitude(latitudeDouble);
                targetLocation.setLongitude(longitudeDouble);

                // Calculate the distance between current location and target location
                float distance = currentLocation.distanceTo(targetLocation);

                return distance <= radiusFloat; // True if within range
            }
        } catch (SecurityException e) {
            Log.e("SilentModeService", "Location permission error: " + e.getMessage());
        } catch (Exception e) {
            Log.e("SilentModeService", "Error calculating location: " + e.getMessage());
        }
        return false; // Default if there's an error or location is out of range
    }

    // LocationListener to handle location updates
    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            // Handle location change here
            Log.d("LocationListener", "Location changed: " + location.getLatitude() + ", " + location.getLongitude());
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // Handle status change if needed
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            // Handle provider enabled if needed
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            // Handle provider disabled if needed
        }
    };




    @Override
    public void onCreate() {
        super.onCreate();

        // Register the LocationListener when the service is created
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // Register the LocationListener
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10, locationListener);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(silentModeChecker);
        stopNotification();
        // Unregister the LocationListener when the service is destroyed
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(locationListener);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
