package com.mobapps.silentcompanion;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Calendar;

public class UpdateActivity extends AppCompatActivity {
    private TextView initialTimeText, finalTimeText;
    private boolean isInitialTimeSet = true, isFinalTimeSet = true;
    private RadioButton vibrationRadioButton, silentRadioButton;
    private EditText scheduleNameInput,radiusinput;
    private DatabaseHelper databaseHelper;
    private int scheduleId;
    // Default value
    private String lon,rad ,lat;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize DatabaseHelper
        databaseHelper = new DatabaseHelper(this);

        // Get the schedule details passed from MainActivity2 or previous activity
        Intent intent = getIntent();
        scheduleId = intent.getIntExtra("id", -1);
        String scheduleName = intent.getStringExtra("name");
        String initialTime = intent.getStringExtra("initialTime");
        String finalTime = intent.getStringExtra("finalTime");
        String mode = intent.getStringExtra("mode");
        String days = intent.getStringExtra("days");
        lat = intent.getStringExtra("latitude");
        lon = intent.getStringExtra("longitude");
        rad = intent.getStringExtra("radius");


        SelectMode(mode);
        initializeTimeViews(initialTime,finalTime);
        SetSelectedDays(days);

        // Initialize Views
        scheduleNameInput = findViewById(R.id.ScheduleName);
        scheduleNameInput.setText(scheduleName);// Assuming your layout has this ID
        Button location = findViewById(R.id.location);
        selectedDays();


        // Button to update the schedule
        Button updateButton = findViewById(R.id.updatebtn);
        updateButton.setOnClickListener(v -> {
            updateButtonClicked();
        });
        location.setOnClickListener(view -> {
            location();
        });

        Button cancelLocationButton = findViewById(R.id.cancel_location);
        cancelLocationButton.setOnClickListener(v -> {
            rmlocation();
        });
        ImageButton timeClose = findViewById(R.id.timeClose);
        timeClose.setOnClickListener(v -> removeTime());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Enable the back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }
    private void removeTime(){
        initialTimeText.setText("");
        finalTimeText.setText("");
        Toast.makeText(UpdateActivity.this, "Time is cleared", Toast.LENGTH_SHORT).show();
    }
    private void rmlocation(){
        // Clear latitude, longitude, and radius
        lat = "";
        lon = "";

        // Show a Toast message to confirm
        Toast.makeText(UpdateActivity.this, "Location and radius cleared.", Toast.LENGTH_SHORT).show();
    }
    private void location() {
        Intent intent = new Intent(UpdateActivity.this, MapActivity.class);
        intent.putExtra("latitude", lat);
        intent.putExtra("longitude", lon);
        intent.putExtra("radius", rad);
        startActivityForResult(intent, 1);
    }


    private void updateButtonClicked(){
        String updatedScheduleName = scheduleNameInput.getText().toString();
        String updatedInitialTime = initialTimeText.getText().toString();
        String updatedFinalTime = finalTimeText.getText().toString();
        String updatedMode = vibrationRadioButton.isChecked() ? "Vibration" : "Silent";
        String updateDays = GetSelectedDays();
        String latitude = this.lat;
        String longitude = this.lon;
        String radiusText = this.rad;

        // If the schedule name is empty, show a Toast
        if (updatedScheduleName.isEmpty()) {
            Toast.makeText(UpdateActivity.this, "Please fill in the schedule name.", Toast.LENGTH_SHORT).show();
            return; // Exit if name is not provided
        }
        // Check if days are selected
        if (updateDays.isEmpty()) {
            Toast.makeText(UpdateActivity.this, "Please select at least one day.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if either time or location is provided
        boolean isTimeProvided = !updatedInitialTime.isEmpty() && !updatedFinalTime.isEmpty();
        boolean isLocationProvided = lat != null && lon != null && !lat.isEmpty() && !lon.isEmpty();
        // Show a message if neither time nor location is provided
        if (!isTimeProvided && !isLocationProvided) {
            Toast.makeText(UpdateActivity.this, "Please provide either time or location.", Toast.LENGTH_SHORT).show();
            return; // Exit if neither is provided
        }

        // Handle time checks if time is provided
        if (isTimeProvided) {
            // Ensure both times are set
            if (!isInitialTimeSet || !isFinalTimeSet) {
                Toast.makeText(UpdateActivity.this, "Please set both initial and final times!", Toast.LENGTH_SHORT).show();
                return;
            }


            // Convert times to 24-hour format for comparison
            int initialTimeInMinutes = convertTo24HourFormat(updatedInitialTime);
            int finalTimeInMinutes = convertTo24HourFormat(updatedFinalTime);

            // Check if final time is greater than initial time
            if (finalTimeInMinutes <= initialTimeInMinutes) {
                Toast.makeText(UpdateActivity.this, "Final time must be greater than initial time.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Update the schedule in the database
        boolean isUpdated = databaseHelper.updateSchedule(scheduleId, updatedScheduleName, updatedInitialTime, updatedFinalTime, updatedMode,updateDays,latitude,longitude,radiusText);

        if (isUpdated) {
            Toast.makeText(UpdateActivity.this, "Schedule Updated", Toast.LENGTH_SHORT).show();
            finish();  // Close the activity after update
        } else {
            Toast.makeText(UpdateActivity.this, "Failed to Update", Toast.LENGTH_SHORT).show();
        }

    }


    private void initializeTimeViews(String initialTime,String finalTime){

        initialTimeText = findViewById(R.id.idTVInitialTimeSelected);
        finalTimeText = findViewById(R.id.idTVFinalTimeSelected);

        // Set initial texts
        initialTimeText.setText(initialTime);
        finalTimeText.setText(finalTime);
        LinearLayout initialTimes =findViewById(R.id.initialtime);
        LinearLayout finalTimes =findViewById(R.id.finaltime);
        initialTimes.setOnClickListener(v -> showTimePickerDialog(initialTimeText, true));
        finalTimes.setOnClickListener(v -> showTimePickerDialog(finalTimeText, false));
    }


    private void SelectMode(String mode){
        // Set the radio buttons based on the mode
        vibrationRadioButton = findViewById(R.id.vibrationRadioButton);
        silentRadioButton = findViewById(R.id.silentRadioButton);

        if ("Silent".equals(mode)) {
            silentRadioButton.setChecked(true);
        } else {
            vibrationRadioButton.setChecked(true);
        }
        // Set the radio button listeners
        vibrationRadioButton.setOnClickListener(v -> {
            vibrationRadioButton.setChecked(true);
            silentRadioButton.setChecked(false);
        });

        silentRadioButton.setOnClickListener(v -> {
            silentRadioButton.setChecked(true);
            vibrationRadioButton.setChecked(false);
        });
    }

    private String updateDays = ""; // Store the selected days

    // Show the BottomSheetDialog to select days
    private void selectedDays() {
        CheckBox rbEveryday = findViewById(R.id.rbEveryday);
        CheckBox rbMonday = findViewById(R.id.rbMonday);
        CheckBox rbTuesday = findViewById(R.id.rbTuesday);
        CheckBox rbWednesday = findViewById(R.id.rbWednesday);
        CheckBox rbThursday = findViewById(R.id.rbThursday);
        CheckBox rbFriday = findViewById(R.id.rbFriday);
        CheckBox rbSaturday = findViewById(R.id.rbSaturday);
        CheckBox rbSunday = findViewById(R.id.rbSunday);

        CheckBox[] individualDays = {rbMonday, rbTuesday, rbWednesday, rbThursday, rbFriday, rbSaturday, rbSunday};

        // Set initial values
        SetSelectedDays(updateDays, rbEveryday, individualDays);

        // Listener for "Everyday"
        rbEveryday.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                for (CheckBox day : individualDays) {
                    day.setChecked(false);
                }
            }
            updateDays = GetSelectedDays(rbEveryday, individualDays);
        });

        // Listener for individual days
        for (CheckBox day : individualDays) {
            day.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    rbEveryday.setChecked(false);
                }
                updateDays = GetSelectedDays(rbEveryday, individualDays);
            });
        }
    }

    // Get the selected days
    private String GetSelectedDays(CheckBox rbEveryday, CheckBox... days) {
        StringBuilder selectedDaysBuilder = new StringBuilder();
        if (rbEveryday.isChecked()) {
            selectedDaysBuilder.append("Everyday");
        } else {
            String[] dayAbbreviations = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
            for (int i = 0; i < days.length; i++) {
                if (days[i].isChecked()) {
                    if (selectedDaysBuilder.length() > 0) {
                        selectedDaysBuilder.append(", ");
                    }
                    selectedDaysBuilder.append(dayAbbreviations[i]);
                }
            }
        }
        return selectedDaysBuilder.toString();
    }
    private String GetSelectedDays() {
        return updateDays; // Return the current selection
    }
    private void SetSelectedDays(String selectedDays) {
        updateDays = selectedDays;
    }
    // Set the selected days
    private void SetSelectedDays(String selectedDays, CheckBox rbEveryday, CheckBox... days) {
        rbEveryday.setChecked(false);
        for (CheckBox day : days) {
            day.setChecked(false);
        }
        if (selectedDays == null || selectedDays.isEmpty()) {
            return; // No selection to apply
        }
        if ("Everyday".equals(selectedDays)) {
            rbEveryday.setChecked(true);
        } else {
            String[] dayAbbreviations = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
            String[] selectedArray = selectedDays.split(", ");
            for (String selected : selectedArray) {
                for (int i = 0; i < dayAbbreviations.length; i++) {
                    if (selected.equals(dayAbbreviations[i])) {
                        days[i].setChecked(true);
                    }
                }
            }
        }
    }




    // Show TimePicker dialog for setting time
    private void showTimePickerDialog(final TextView timeTextView, boolean isInitialTime) {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY); // Use 24-hour format for easier comparison
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(UpdateActivity.this,
                (view, selectedHour, selectedMinute) -> {
                    // Determine AM/PM
                    String ampmString = (selectedHour < 12) ? "AM" : "PM";
                    int displayHour = (selectedHour > 12) ? selectedHour - 12 : selectedHour;
                    if (displayHour == 0) displayHour = 12; // Special case for 12 AM and 12 PM

                    @SuppressLint("DefaultLocale") String time = String.format("%02d:%02d %s", displayHour, selectedMinute, ampmString);

                    if (isInitialTime) {
                        // Set initial time
                        isInitialTimeSet = true;
                        timeTextView.setText(time);
                    } else {
                        // Set final time
                        isFinalTimeSet = true;
                        timeTextView.setText(time);
                    }
                }, hour, minute, false); // false for 24-hour format

        timePickerDialog.show();
    }

    // Helper method to convert time (AM/PM format) to minutes since midnight
    private int convertTo24HourFormat(String time) {
        int hour = Integer.parseInt(time.split(":")[0].trim());
        int minute = Integer.parseInt(time.split(":")[1].split(" ")[0].trim());
        String ampm = time.split(" ")[1].trim();

        if (ampm.equalsIgnoreCase("PM") && hour != 12) {
            hour += 12; // Convert PM hour to 24-hour format
        } else if (ampm.equalsIgnoreCase("AM") && hour == 12) {
            hour = 0; // Convert 12 AM to 00:00
        }

        return hour * 60 + minute; // Return total minutes from midnight
    }
    // Handle back button clicks
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // Go back to the previous activity
        return true;
    }
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
            Intent intent = new Intent(this, Settings.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            double markedLatitude = data.getDoubleExtra("markedLatitude", 0.0);
            double markedLongitude = data.getDoubleExtra("markedLongitude", 0.0);
            int radius = data.getIntExtra("selectedRadius", 10);

            // Convert the latitude and longitude to String
            lat = String.valueOf(markedLatitude);
            lon = String.valueOf(markedLongitude);
            rad = String.valueOf(radius);
            // Use the marked location here
            // For example, update the UI with the new location
            Toast.makeText(this, "Location is Selected ", Toast.LENGTH_SHORT).show();
        }
    }
}

