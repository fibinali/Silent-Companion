package com.mobapps.silentcompanion;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize dark mode
        initializeDarkMode();

        initializeLanguage();

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Navigate to MainActivity2 after delay
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            Intent intent = new Intent(MainActivity.this, MainActivity2.class);
            startActivity(intent);
            finish();
        }, 2000);


    }

    private void initializeDarkMode() {
        // Load dark mode preference
        SharedPreferences sharedPreferences = getSharedPreferences("user_preferences", MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);

        // Get the current mode
        int currentMode = AppCompatDelegate.getDefaultNightMode();

        // Apply the mode only if it doesn't match the preference
        int expectedMode = isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        if (currentMode != expectedMode) {
            AppCompatDelegate.setDefaultNightMode(expectedMode);
        }
    }

    private void initializeLanguage() {
        // Load the saved language position
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        int savedLanguagePosition = sharedPreferences.getInt("language_position", 0); // Default: 0 (Default System)

        // Check if the saved language is different from the current language
        String savedLanguageCode = Settings.getLanguageCode(savedLanguagePosition);
        String currentLanguageCode = Locale.getDefault().getLanguage();

        // Apply the saved language only if it's different from the current language
        if (!savedLanguageCode.isEmpty() && !savedLanguageCode.equals(currentLanguageCode)) {
            setAppLanguage(savedLanguageCode);
        }
    }

    private void setAppLanguage(String languageCode) {
        // Set the selected language
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

    }


}
