package com.mobapps.silentcompanion;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class Settings extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 100;
    private Switch darkModeSwitch;
    private Switch notificationSwitch;
    private SharedPreferences sharedPreferences;
    private Spinner languageSpinner;

    private static final String PREFS_NAME = "prefs";
    private static final String FIRST_LAUNCH_KEY = "first_launch";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        // Apply window insets for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI components
        RelativeLayout contactTextView = findViewById(R.id.emergency_contact);
        RelativeLayout privacyPolicy = findViewById(R.id.privacy);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Enable the back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);

        // Dark Mode Switch
        darkModeSwitch = findViewById(R.id.darkmode);
        // Restore switch state from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("user_preferences", MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        darkModeSwitch.setChecked(isDarkMode);

        initializeDarkModeSwitch(sharedPreferences);

        // Notification Switch
        notificationSwitch = findViewById(R.id.notification_switch);
        initializeNotificationSwitch();

        // Contact TextView listener
        contactTextView.setOnClickListener(v -> btnContact());
        privacyPolicy.setOnClickListener(v -> Privacy());

        languageSpinner = findViewById(R.id.language_spinner);

        setupLanguageSpinner();

        RelativeLayout tutorialTextView = findViewById(R.id.tutorial); // Replace with your TextView ID

        // Check if it's the first launch
        if (isFirstLaunch(this)) {
            showTutorialDialog(this);
        }

        tutorialTextView.setOnClickListener(v -> {
            showTutorialDialog(this);
        });

        RelativeLayout appInfoButton = findViewById(R.id.appInfoButton);
        appInfoButton.setOnClickListener(v -> {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });
    }

    // Static method for first launch check
    public static boolean isFirstLaunch(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean(FIRST_LAUNCH_KEY, true);

        if (isFirstLaunch) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(FIRST_LAUNCH_KEY, false);
            editor.apply();
        }

        return isFirstLaunch;
    }

    // Static method for showing the tutorial dialog
    public static void showTutorialDialog(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);  // Use context to get LayoutInflater
        View view = inflater.inflate(R.layout.alert_box_layout, null);

        Button closeButton = view.findViewById(R.id.close_button);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);  // Use context for Builder
        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();

        closeButton.setOnClickListener(v -> dialog.dismiss());
    }

    public void btnContact() {
        Intent intent = new Intent(Settings.this, ContactListActivity.class);
        startActivity(intent);
    }
    public void Privacy() {
        Intent intent = new Intent(Settings.this, privacy.class);
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
                notificationSwitch.setChecked(false);
            }
        }
    }

    // Initialize the dark mode switch
    private void initializeDarkModeSwitch(SharedPreferences sharedPreferences) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                editor.putBoolean("dark_mode", true);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                editor.putBoolean("dark_mode", false);
            }
            editor.apply();
        });
    }

    // Notification switch initialization
    private void initializeNotificationSwitch() {
        boolean areNotificationsEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled();
        notificationSwitch.setChecked(areNotificationsEnabled && sharedPreferences.getBoolean("notificationsEnabled", true));

        notificationSwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("notificationsEnabled", isChecked);
            editor.apply();

            if (isChecked) {
                // Request notification permission for Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                                != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            NOTIFICATION_PERMISSION_REQUEST_CODE);
                } else {
                    Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Disable notifications across the app
                cancelAllNotifications();
                Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Cancel all active notifications
    private void cancelAllNotifications() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancelAll();  // Cancels all active notifications
    }

    private void setupLanguageSpinner() {
        // Load the language array from resources
        String[] languages = getResources().getStringArray(R.array.languages);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);

        // Setup the spinner adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);

        // Retrieve and set the previously selected language position
        int savedLanguagePosition = sharedPreferences.getInt("language_position", 0); // Default: 0 (Default System)
        languageSpinner.setSelection(savedLanguagePosition);

        // Handle language selection
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                // Save the selected language position
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("language_position", position);
                editor.apply();

                // Update the app's language
                if (position == 0) { // Default System
                    resetToSystemLanguage();
                } else {
                    setAppLanguage(getLanguageCode(position));
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setAppLanguage(String languageCode) {
        // Check if the language is already set
        String currentLanguage = Locale.getDefault().getLanguage();
        if (currentLanguage.equals(languageCode)) {
            return; // No need to change the language if it's already set
        }

        // Set the selected language
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        // Update all UI components that need refreshing
        recreateUI();

    }

    private void resetToSystemLanguage() {
        // Get the current system language
        String systemLanguage = Locale.getDefault().getLanguage();

        // Check if the system language is already set
        String currentLanguage = Locale.getDefault().getLanguage();
        if (currentLanguage.equals(systemLanguage)) {
            return; // No need to reset if it's already the system language
        }

        // Reset to system's default language
        Locale defaultLocale = Locale.getDefault();
        Configuration config = new Configuration();
        config.setLocale(defaultLocale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        // Update all UI components that need refreshing
        recreateUI();

    }


    static String getLanguageCode(int position) {
        // Map spinner positions to language codes
        String[] languageCodes = {
                "",           //Default system
                "ar",        // Arabic1
                "en",        // English1
                "zh",        // Chinese
                "es",        // Spanish
                "hi",        // Hindi1
                "bn",        // Bengali1
                "pt",        // Portuguese
                "ru",        // Russian
                "ja",        // Japanese
                "de",        // German1
                "tr",        // Turkish
                "ko",        // Korean
                "it",        // Italian
                "ml",        // Malayalam
                "th",        // Thai
                "uk",        // Ukrainian
                "ur",        // Urdu
                "nl",        // Dutch
                "el",        // Greek1
                "in",        // Indonesian
                "ta",        // Tamil
                "sr"         // Serbian
        };
        return languageCodes[position];
    }
    private void recreateUI() {
        // Recreate the activity to apply the language change
        // This will reapply the language settings without restarting the app
        recreate();
    }
}
