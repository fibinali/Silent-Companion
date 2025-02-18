package com.mobapps.silentcompanion;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class MainActivity2 extends AppCompatActivity {
    private static final int REQUEST_CODE_CONTACTS = 101;
    private static final int REQUEST_CODE_CALL_LOG = 102;
    private static final int REQUEST_CODE_STORAGE = 103;

    private static final int LOCATION_REQUEST_CODE = 1001; // Any unique request code


    private RecyclerView recyclerView;
    private DatabaseHelper databaseHelper;

    private static final int REQUEST_CODE_PERMISSIONS = 200;
    private static final String TAG = "DialerApp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2); // Ensure this matches your layout file
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageButton addButton = findViewById(R.id.addbutton);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false); // Disable the title
        }

        recyclerView = findViewById(R.id.recycleview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        databaseHelper = new DatabaseHelper(this);

        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity2.this, Function.class);
            startActivity(intent);
        });

        boolean isFirstLaunch = Settings.isFirstLaunch(this); // `this` refers to the current activity context
        if (isFirstLaunch) {
            // Optionally show the tutorial dialog
            Settings.showTutorialDialog(this);
        }
        //checkAndEnableLocation();

        // Request runtime permissions
        requestPermissions();

        // Request Default Dialer
        requestDefaultDialer();

        requestLocation();

        requestContactsPermission();
        requestCallLogPermission();
        requestStoragePermission();
    }
    private void requestLocation(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10+
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.FOREGROUND_SERVICE_LOCATION
            }, LOCATION_REQUEST_CODE);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        List<DatabaseHelper.Schedule> scheduleList = databaseHelper.readAllSchedules();
        ScheduleAdapter scheduleAdapter = new ScheduleAdapter(this, scheduleList);
        recyclerView.setAdapter(scheduleAdapter);
    }

    /*private void checkAndEnableLocation() {
        // Check for location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }

        // Check if location services are enabled
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Snackbar.make(findViewById(android.R.id.content), "Location is turned off. Enable it for the app to work properly.", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Enable", v -> {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }).show();
        }
    }

    private final ActivityResultLauncher<String> requestLocationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Snackbar.make(findViewById(android.R.id.content), "Location permission is required for this app.", Snackbar.LENGTH_LONG).show();
                } else {
                    checkAndEnableLocation();
                }
            });
    */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the toolbar
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // Open the Settings page
            Intent intent = new Intent(this, com.mobapps.silentcompanion.Settings.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_CODE_PERMISSIONS);
        }
    }

    // Register ActivityResultLauncher for the default dialer request
    private final ActivityResultLauncher<Intent> defaultDialerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.d(TAG, "App set as default dialer successfully.");
                } else {
                    Log.d(TAG, "Failed to set app as default dialer.");
                }
            }
    );

    private void requestDefaultDialer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = (RoleManager) getSystemService(Context.ROLE_SERVICE);
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) &&
                    !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER);
                defaultDialerLauncher.launch(intent); // Launch intent using ActivityResultLauncher
            } else {
                Log.d(TAG, "Already set as default dialer.");
            }
        } else {
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
            startActivity(intent); // For older versions, open default apps settings
        }
    }

    private void requestContactsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_CODE_CONTACTS);
        }
    }

    private void requestCallLogPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALL_LOG}, REQUEST_CODE_CALL_LOG);
        }
    }

    private void requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permissions", "Contacts permission granted.");
            } else {
                Log.d("Permissions", "Contacts permission denied.");
            }
        } else if (requestCode == REQUEST_CODE_CALL_LOG) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permissions", "Call Log permission granted.");
            } else {
                Log.d("Permissions", "Call Log permission denied.");
            }
        } else if (requestCode == REQUEST_CODE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permissions", "Storage permission granted.");
            } else {
                Log.d("Permissions", "Storage permission denied.");
            }
        }
        else if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission granted.");
            } else {
                Toast.makeText(this, "Permission required to detect incoming calls.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
