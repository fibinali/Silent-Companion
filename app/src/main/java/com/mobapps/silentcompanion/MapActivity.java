package com.mobapps.silentcompanion;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

public class MapActivity extends AppCompatActivity {

    private MapView mapView;
    private TextView coordinatesTextView;
    private FusedLocationProviderClient fusedLocationClient;
    private Marker currentMarker;
    private GeoPoint lastKnownLocation;
    private boolean isMarkerPlaced = false;

    private GeoPoint markedLocation;
    private boolean gpsEnabled = false;
    private boolean internetEnabled = false;
    private boolean isLocationEnabled;

    private int selectedRadius ;

    private Polygon markerPolygon;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().setOsmdroidBasePath(getFilesDir());
        Configuration.getInstance().setOsmdroidTileCache(getCacheDir());

        setContentView(R.layout.activity_map);

        mapView = findViewById(R.id.mapView);
        coordinatesTextView = findViewById(R.id.coordinatesTextView);
        Button btnOk = findViewById(R.id.btnOk);
        ImageButton btnGoToCurrentLocation = findViewById(R.id.btnGoToCurrentLocation);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapView.setMultiTouchControls(true);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.getController().setZoom(19.0);

        checkGpsAndRequestEnable();

        // Retrieve the latitude and longitude as strings
        String latitudeStr = getIntent().getStringExtra("latitude");
        String longitudeStr = getIntent().getStringExtra("longitude");
        String radiusString = getIntent().getStringExtra("radius");
        if (radiusString != null && !radiusString.isEmpty()) {
            selectedRadius = Integer.parseInt(radiusString);
        } else {
            selectedRadius = 10; // Set a default value if radiusString is empty or null
        }
        double latitude = Double.NaN;
        double longitude = Double.NaN;

        if (latitudeStr != null && longitudeStr != null) {
            try {
                latitude = Double.parseDouble(latitudeStr);
                longitude = Double.parseDouble(longitudeStr);
            } catch (NumberFormatException e) {
            }
        }

        // Determine if latitude and longitude were provided
        if (!Double.isNaN(latitude) && !Double.isNaN(longitude)) {
            GeoPoint providedLocation = new GeoPoint(latitude, longitude);
            moveToLocation(providedLocation);
            markedLocation = providedLocation;

            // Update the TextView with the provided location
            coordinatesTextView.setText(String.format("Provided Location: Lat %s, Lon %s", latitude, longitude));
        } else {
            // If no location is provided, fall back to the current location
            Toast.makeText(this, "Showing Current Location.Wait 10 Seconds...", Toast.LENGTH_SHORT).show();
            moveToLocation(lastKnownLocation);


        }

        // Button to go to current location
        btnGoToCurrentLocation.setOnClickListener(v -> {
            if (lastKnownLocation != null) {
                // Move to current location
                moveToLocation(lastKnownLocation);

                // Update TextView with current location coordinates
                coordinatesTextView.setText(String.format("Current Location: Lat %s, Lon %s", lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()));
            } else {
                Toast.makeText(this, "Current location not available yet.", Toast.LENGTH_SHORT).show();
            }
        });

        // Touch listener to place a custom marker
        mapView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                // Get the tapped location
                GeoPoint tappedPoint = (GeoPoint) mapView.getProjection().fromPixels((int) event.getX(), (int) event.getY());

                // Update the global variable for marked location
                markedLocation = tappedPoint;

                // Remove the old marker if it exists
                if (currentMarker != null) {
                    mapView.getOverlays().remove(currentMarker);
                }

                // Place a new marker
                currentMarker = new Marker(mapView);
                currentMarker.setPosition(tappedPoint);
                Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_location);
                currentMarker.setIcon(drawable);
                mapView.getOverlays().add(currentMarker);

                // Create or update the polygon with the new location and radius
                createPolygonOverlay(tappedPoint, selectedRadius);

                // Update the TextView with coordinates
                coordinatesTextView.setText(String.format("Marked Location: Lat %s, Lon %s",
                        tappedPoint.getLatitude(), tappedPoint.getLongitude()));

                // Refresh the map
                mapView.invalidate();
            }
            return false;
        });

        // OK button to return data to FunctionActivity
        btnOk.setOnClickListener(v -> {
            if (markedLocation != null) {
                Intent intent = new Intent();
                intent.putExtra("markedLatitude", markedLocation.getLatitude());
                intent.putExtra("markedLongitude", markedLocation.getLongitude());
                intent.putExtra("selectedRadius", selectedRadius);
                setResult(RESULT_OK, intent); // Set result to send back to the calling activity
            } else {
                Toast.makeText(this, "No location marked", Toast.LENGTH_SHORT).show();
            }
            finish();
        });

        setupRadiusSpinner();

    }

    private void checkGpsAndRequestEnable() {
        if (!isGpsEnabled()) {
            new AlertDialog.Builder(this)
                    .setMessage("GPS is disabled. Do you want to enable it?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, 100);
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        Toast.makeText(this, "GPS is required for this feature", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .show();
        } else {
            gpsEnabled = true;
            checkInternetAndRequestEnable();
        }
    }

    private void checkInternetAndRequestEnable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo == null || !networkInfo.isConnected()) {
            new AlertDialog.Builder(this)
                    .setMessage("Internet is disabled. Do you want to enable it?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
                        startActivityForResult(intent, 101);
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        Toast.makeText(this, "Internet is required for this feature", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .show();
        } else {
            internetEnabled = true;
            requestLocationUpdates();
        }
    }

    private boolean isGpsEnabled() {
        return gpsEnabled || Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF) != Settings.Secure.LOCATION_MODE_OFF;
    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull com.google.android.gms.location.LocationResult locationResult) {
            super.onLocationResult(locationResult);
            if (!locationResult.getLocations().isEmpty()) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    GeoPoint currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                    lastKnownLocation = currentLocation;

                    // Set markedLatitude and markedLongitude to the current location
                    markedLocation = currentLocation;

                    if (!isMarkerPlaced) {
                        moveToLocation(currentLocation);
                        isMarkerPlaced = true;
                    }

                    if (markedLocation == null) {
                        coordinatesTextView.setText(String.format("Current Location: Lat %s, Lon %s", currentLocation.getLatitude(), currentLocation.getLongitude()));
                    }
                }
            }
        }
    };

    private void moveToLocation(GeoPoint location) {
        if (location == null) {
            // Handle the case when location is null, for example by using a default location
            Log.e("MapActivity", "Received null location, using default location.");
            location = new GeoPoint(0.0, 0.0);  // Default location (lat, lon)
        }

        if (currentMarker == null) {
            currentMarker = new Marker(mapView);
            currentMarker.setTitle("You are here");

            // Set the custom icon for the initial marker
            Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_location);
            currentMarker.setIcon(drawable);

            mapView.getOverlays().add(currentMarker);
        }

        currentMarker.setPosition(location);
        mapView.getController().setCenter(location);

        createPolygonOverlay(location, selectedRadius);
        mapView.invalidate();
    }


    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocationUpdates();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100) {
            gpsEnabled = isGpsEnabled();
            if (gpsEnabled) {
                checkInternetAndRequestEnable();
            } else {
                Toast.makeText(this, "GPS is still disabled", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 101) {
            internetEnabled = isInternetEnabled();
            if (internetEnabled) {
                checkGpsAndRequestEnable();
            } else {
                Toast.makeText(this, "Internet is still disabled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isInternetEnabled() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    // Method to set up the Radius Spinner
    private void setupRadiusSpinner() {
        Spinner spinnerRadius = findViewById(R.id.spinnerRadius);

        // Set up the adapter for the Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.radius_values, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRadius.setAdapter(adapter);

        // Set the listener to capture the selected radius
        spinnerRadius.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // Get the selected radius from the spinner
                String selectedItem = parentView.getItemAtPosition(position).toString();
                selectedRadius = Integer.parseInt(selectedItem.replace("m", ""));
                Toast.makeText(MapActivity.this, "Selected Radius: " + selectedRadius + " meters", Toast.LENGTH_SHORT).show();

                // Update the polygon if a location is already marked
                if (markedLocation != null) {
                    createPolygonOverlay(markedLocation, selectedRadius);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing
            }
        });

        // Set the spinner to the previous radius value if it exists
        if (selectedRadius != 0) {
            String radiusValue = selectedRadius + "m";
            int position = adapter.getPosition(radiusValue);
            spinnerRadius.setSelection(position);
        }
    }

    // Method to handle polygon overlay creation and updates
    private void createPolygonOverlay(GeoPoint centerPoint, double radius) {
        // Remove the old polygon if it exists
        if (markerPolygon != null) {
            mapView.getOverlays().remove(markerPolygon);
        }

        // Create a new polygon overlay
        markerPolygon = new Polygon(mapView);
        markerPolygon.setPoints(Polygon.pointsAsCircle(centerPoint, radius));

        // Customize the polygon appearance
        markerPolygon.getOutlinePaint().setColor(ContextCompat.getColor(this, R.color.polygon_outline));
        markerPolygon.getFillPaint().setColor(ContextCompat.getColor(this, R.color.polygon_fill));
        markerPolygon.getFillPaint().setAlpha(50);

        // Add the polygon to the map
        mapView.getOverlays().add(markerPolygon);

        // Refresh the map
        mapView.invalidate();
    }
}
