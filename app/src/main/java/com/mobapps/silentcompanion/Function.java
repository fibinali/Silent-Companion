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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Calendar;

public class Function extends AppCompatActivity {
    private TextView initialTimeSelectedTV, finalTimeSelectedTV;
    private boolean isInitialTimeSet = false, isFinalTimeSet = false;
    private RadioButton vibrationRadioButton, silentRadioButton;
    private EditText scheduleName;
    private DatabaseHelper databaseHelper;
    private String lat = "";   // Default value
    private String lon = "";
    private String rad ="10";
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_function);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize DatabaseHelper
        databaseHelper = new DatabaseHelper(this);
        // Initialize schedule Name
        scheduleName = findViewById(R.id.ScheduleName);
        // Initialize Views
        Button okButton = findViewById(R.id.okButton);
        Button location = findViewById(R.id.location);
        initialTimeSelectedTV = findViewById(R.id.idTVInitialTimeSelected);
        finalTimeSelectedTV = findViewById(R.id.idTVFinalTimeSelected);

        initializeTimeViews();

        SelectMode();
        okButton.setOnClickListener(v -> okButtonClicked());
        location.setOnClickListener(view -> location());

        showDaySelectionWithCheckBoxes();


        Button cancelLocationButton = findViewById(R.id.cancel_location);
        cancelLocationButton.setOnClickListener(v -> removeLocation());

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
        initialTimeSelectedTV.setText("");
        finalTimeSelectedTV.setText("");
        Toast.makeText(Function.this, "Time is cleared", Toast.LENGTH_SHORT).show();

    }
    private void removeLocation(){
        // Clear latitude, longitude, and radius
        lat = "";
        lon = "";

        // Show a Toast message to confirm
        Toast.makeText(Function.this, "Location and radius cleared.", Toast.LENGTH_SHORT).show();
    }
    private void location() {
        Intent intent = new Intent(Function.this, MapActivity.class);
        intent.putExtra("isLocationEnabled", true);
        intent.putExtra("latitude", lat);
        intent.putExtra("longitude", lon);
        intent.putExtra("radius", rad);
        startActivityForResult(intent, 1);
    }

    private void okButtonClicked(){

        String scheduleNameText = scheduleName.getText().toString();
        String radiusText = this.rad;
        String initialTime = initialTimeSelectedTV.getText().toString();
        String finalTime = finalTimeSelectedTV.getText().toString();
        String mode = vibrationRadioButton.isChecked() ? "Vibration" : "Silent";
        String days = GetSelectedDays();

        String lat = this.lat;
        String lon = this.lon;

        // If the schedule name is empty, show a Toast
        if (scheduleNameText.isEmpty()) {
            Toast.makeText(Function.this, "Please fill in the schedule name.", Toast.LENGTH_SHORT).show();
            return; // Exit if name is not provided
        }

        // Check if days are selected
        if (days.isEmpty()) {
            Toast.makeText(Function.this, "Please select at least one day.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if either time or location is provided
        boolean isTimeProvided = !initialTime.isEmpty() && !finalTime.isEmpty();
        boolean isLocationProvided = lat != null && lon != null && !lat.isEmpty() && !lon.isEmpty();

        // Show a message if neither time nor location is provided
        if (!isTimeProvided && !isLocationProvided) {
            Toast.makeText(Function.this, "Please provide either time or location.", Toast.LENGTH_SHORT).show();
            return; // Exit if neither is provided
        }

        // Handle time checks if time is provided
        if (isTimeProvided) {
            // Ensure both times are set
            if (!isInitialTimeSet || !isFinalTimeSet) {
                Toast.makeText(Function.this, "Please set both initial and final times!", Toast.LENGTH_SHORT).show();
                return;
            }


            // Convert times to 24-hour format for comparison
            int initialTimeInMinutes = convertTo24HourFormat(initialTime);
            int finalTimeInMinutes = convertTo24HourFormat(finalTime);

            // Check if final time is greater than initial time
            if (finalTimeInMinutes <= initialTimeInMinutes) {
                Toast.makeText(Function.this, "Final time must be greater than initial time.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Insert schedule into database
        boolean isInserted = databaseHelper.insertSchedule(scheduleNameText, initialTime, finalTime, mode, days, radiusText, lat, lon);

        if (isInserted) {
            Toast.makeText(Function.this, "Schedule saved successfully!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(Function.this, "Failed to save schedule.", Toast.LENGTH_SHORT).show();
        }

        // Redirect to main activity or another activity
        Intent intent = new Intent(Function.this, MainActivity2.class);
        startActivity(intent);
    }


    //Select silent mode or silent with vibration mode
    private void SelectMode(){

        // Initialize Radio buttons for mode selection
        vibrationRadioButton = findViewById(R.id.vibrationRadioButton);
        silentRadioButton = findViewById(R.id.silentRadioButton);
        silentRadioButton.setChecked(true);
        vibrationRadioButton.setChecked(false);

        vibrationRadioButton.setOnClickListener(v -> {
            vibrationRadioButton.setChecked(true);
            silentRadioButton.setChecked(false);
        });

        silentRadioButton.setOnClickListener(v -> {
            silentRadioButton.setChecked(true);
            vibrationRadioButton.setChecked(false);
        });
    }

    private String selectedDays = ""; // Store the selected days

    private void showDaySelectionWithCheckBoxes() {
        // Checkboxes for days
        CheckBox rbEveryday = findViewById(R.id.rbEveryday);
        CheckBox rbMonday = findViewById(R.id.rbMonday);
        CheckBox rbTuesday = findViewById(R.id.rbTuesday);
        CheckBox rbWednesday = findViewById(R.id.rbWednesday);
        CheckBox rbThursday = findViewById(R.id.rbThursday);
        CheckBox rbFriday = findViewById(R.id.rbFriday);
        CheckBox rbSaturday = findViewById(R.id.rbSaturday);
        CheckBox rbSunday = findViewById(R.id.rbSunday);

        // Initialize checkboxes based on the current `selectedDays` value
        if ("Everyday".equalsIgnoreCase(selectedDays)) {
            rbEveryday.setChecked(true);
        } else {
            if (selectedDays.contains("Mon")) rbMonday.setChecked(true);
            if (selectedDays.contains("Tue")) rbTuesday.setChecked(true);
            if (selectedDays.contains("Wed")) rbWednesday.setChecked(true);
            if (selectedDays.contains("Thu")) rbThursday.setChecked(true);
            if (selectedDays.contains("Fri")) rbFriday.setChecked(true);
            if (selectedDays.contains("Sat")) rbSaturday.setChecked(true);
            if (selectedDays.contains("Sun")) rbSunday.setChecked(true);
        }

        // Uncheck individual days if "Everyday" is selected
        rbEveryday.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                rbMonday.setChecked(false);
                rbTuesday.setChecked(false);
                rbWednesday.setChecked(false);
                rbThursday.setChecked(false);
                rbFriday.setChecked(false);
                rbSaturday.setChecked(false);
                rbSunday.setChecked(false);

                // Call the "OK" logic directly when "Everyday" is selected
                processSelectedDays();
            }
        });

        // Uncheck "Everyday" if any individual day is selected
        CheckBox[] individualDays = {rbMonday, rbTuesday, rbWednesday, rbThursday, rbFriday, rbSaturday, rbSunday};
        for (CheckBox day : individualDays) {
            day.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    rbEveryday.setChecked(false);

                    // Call the "OK" logic directly when any individual day is selected
                    processSelectedDays();
                }
            });
        }
    }

    // Logic that was previously inside the OK button click
    private void processSelectedDays() {
        StringBuilder selectedDaysBuilder = new StringBuilder();

        CheckBox rbEveryday = findViewById(R.id.rbEveryday);
        CheckBox rbMonday = findViewById(R.id.rbMonday);
        CheckBox rbTuesday = findViewById(R.id.rbTuesday);
        CheckBox rbWednesday = findViewById(R.id.rbWednesday);
        CheckBox rbThursday = findViewById(R.id.rbThursday);
        CheckBox rbFriday = findViewById(R.id.rbFriday);
        CheckBox rbSaturday = findViewById(R.id.rbSaturday);
        CheckBox rbSunday = findViewById(R.id.rbSunday);

        if (rbEveryday.isChecked()) {
            selectedDaysBuilder.append("Everyday");
        } else {
            if (rbMonday.isChecked()) selectedDaysBuilder.append("Mon, ");
            if (rbTuesday.isChecked()) selectedDaysBuilder.append("Tue, ");
            if (rbWednesday.isChecked()) selectedDaysBuilder.append("Wed, ");
            if (rbThursday.isChecked()) selectedDaysBuilder.append("Thu, ");
            if (rbFriday.isChecked()) selectedDaysBuilder.append("Fri, ");
            if (rbSaturday.isChecked()) selectedDaysBuilder.append("Sat, ");
            if (rbSunday.isChecked()) selectedDaysBuilder.append("Sun, ");
        }

        // Remove trailing comma and space
        if (selectedDaysBuilder.length() > 0 && selectedDaysBuilder.toString().endsWith(", ")) {
            selectedDaysBuilder = new StringBuilder(selectedDaysBuilder.substring(0, selectedDaysBuilder.length() - 2));
        }

        // Set the selectedDays value
        selectedDays = selectedDaysBuilder.toString();

    }

    private String GetSelectedDays() {
        return selectedDays;
    }



    //set initial and final time
    private void initializeTimeViews() {

        LinearLayout initialTime =findViewById(R.id.initialtime);
        LinearLayout finalTime =findViewById(R.id.finaltime);
        // Set click listeners for the time-selected TextViews
        initialTime.setOnClickListener(v -> showTimePickerDialog(initialTimeSelectedTV, true));
        finalTime.setOnClickListener(v -> showTimePickerDialog(finalTimeSelectedTV, false));
    }


    // Show TimePicker dialog for setting time
    private void showTimePickerDialog(final TextView timeTextView, boolean isInitialTime) {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY); // Use 24-hour format for easier comparison
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(Function.this,
                (view, selectedHour, selectedMinute) -> {
                    // Determine AM/PM
                    String ampmString = (selectedHour < 12) ? "AM" : "PM";
                    int displayHour = (selectedHour > 12) ? selectedHour - 12 : selectedHour;
                    if (displayHour == 0) displayHour = 12; // Special case for 12 AM and 12 PM

                    @SuppressLint("DefaultLocale") String time = String.format("%02d:%02d %s", displayHour, selectedMinute, ampmString);

                    if (isInitialTime) {
                        isInitialTimeSet = true;
                        timeTextView.setText(time);
                    } else {
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
            Toast.makeText(this, "Location is Selected  ", Toast.LENGTH_SHORT).show();
        }
    }

}
