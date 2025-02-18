package com.mobapps.silentcompanion;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    private final Context context;
    private final List<DatabaseHelper.Schedule> scheduleList;
    private final DatabaseHelper databaseHelper;

    public ScheduleAdapter(Context context, List<DatabaseHelper.Schedule> scheduleList) {
        this.context = context;
        this.scheduleList = scheduleList;
        this.databaseHelper = new DatabaseHelper(context); // Initialize DatabaseHelper
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.currentschedules, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DatabaseHelper.Schedule schedule = scheduleList.get(position);
        holder.scheduleName.setText(schedule.getName().toUpperCase());
        holder.initialTime.setText(schedule.getInitialTime());
        holder.finalTime.setText(schedule.getFinalTime());
        holder.mode.setText(schedule.getMode());
        holder.days.setText(schedule.getDays());
        if (schedule.getLatitude().isEmpty() && schedule.getLongitude().isEmpty()) {
            holder.locationIcon.setVisibility(View.GONE);
        } else {
            holder.locationIcon.setVisibility(View.VISIBLE); // Optional: Hide if condition is false
        }

        // Retrieve the switch state from the database (stored state)
        if (schedule.getSwitchState() == 1) {
            holder.scheduleSwitch.setChecked(true); // Set switch to ON if state is 1
        } else {
            holder.scheduleSwitch.setChecked(false); // Set switch to OFF if state is 0
        }

        // Handle item click for updating
        holder.itemView.setOnClickListener(v -> {
            if (holder.scheduleSwitch.isChecked()) { // Check if the switch is ON
                Toast.makeText(context, "Your schedule is active. Turn it off to update.", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(context, UpdateActivity.class);
                intent.putExtra("id", schedule.getId());
                intent.putExtra("name", schedule.getName());
                intent.putExtra("initialTime", schedule.getInitialTime());
                intent.putExtra("finalTime", schedule.getFinalTime());
                intent.putExtra("mode", schedule.getMode());
                intent.putExtra("days", schedule.getDays());
                intent.putExtra("latitude", schedule.getLatitude());
                intent.putExtra("longitude", schedule.getLongitude());
                intent.putExtra("radius", schedule.getRadius());


                context.startActivity(intent);
            }
        });


        // Handle delete icon click
        holder.deleteIcon.setOnClickListener(v -> {
            if (holder.scheduleSwitch.isChecked()) { // Check if the switch is ON
                Toast.makeText(context, "Your schedule is active. Turn it off to delete.", Toast.LENGTH_SHORT).show();
            } else {
                DatabaseHelper databaseHelper = new DatabaseHelper(context);
                boolean isDeleted = databaseHelper.deleteSchedule(schedule.getId());
                if (isDeleted) {
                    final int deletedPosition = position;
                    final DatabaseHelper.Schedule deletedSchedule = scheduleList.get(deletedPosition); // Store deleted schedule

                    scheduleList.remove(deletedPosition); // Remove item from list
                    notifyItemRemoved(deletedPosition); // Notify adapter
                    notifyItemRangeChanged(deletedPosition, scheduleList.size());

                    Snackbar.make(v, "Schedule deleted", Snackbar.LENGTH_LONG)
                            .setAction("Undo", view -> {
                                // Undo delete by adding the item back
                                scheduleList.add(deletedPosition, deletedSchedule);
                                notifyItemInserted(deletedPosition);
                                notifyItemRangeChanged(deletedPosition, scheduleList.size());

                                // Add the schedule back to the database
                                databaseHelper.addSchedule(deletedSchedule);
                            })
                            .setActionTextColor(ContextCompat.getColor(context, R.color.white)) // Set action text color
                            .setTextColor(ContextCompat.getColor(context, R.color.black)) // Set main text color
                            .setBackgroundTint(ContextCompat.getColor(context, R.color.MainColor)) // Set background color
                            .show();

                    Toast.makeText(context, "Schedule deleted successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Failed to delete schedule", Toast.LENGTH_SHORT).show();
                }
            }
        });

        holder.scheduleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Update the switch state in the database
            DatabaseHelper db = new DatabaseHelper(context);
            int switchState = isChecked ? 1 : 0; // 1 for ON, 0 for OFF
            db.updateScheduleSwitchState(schedule.getId(), switchState);

            // Get the initial and final time from the schedule
            String initialTime = schedule.getInitialTime();
            String finalTime = schedule.getFinalTime();
            String days = schedule.getDays();
            String mode = schedule.getMode();
            int id = schedule.getId();
            String latitude = schedule.getLatitude();
            String longitude = schedule.getLongitude();
            String radius = schedule.getRadius();

            // Create an Intent to send data to SilentModeService
            Intent serviceIntent = new Intent(context, SilentModeService.class);
            serviceIntent.putExtra("id",id);
            serviceIntent.putExtra("initialTime", initialTime);
            serviceIntent.putExtra("finalTime", finalTime);
            serviceIntent.putExtra("days", days);
            serviceIntent.putExtra("mode", mode);
            serviceIntent.putExtra("latitude", latitude);
            serviceIntent.putExtra("longitude", longitude);
            serviceIntent.putExtra("radius", radius);
            serviceIntent.putExtra("isChecked", isChecked); // To know whether to enable or disable silent mode

            // Start the service
            context.startService(serviceIntent);
        });

    }

    @Override
    public int getItemCount() {
        return scheduleList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView scheduleName, initialTime, finalTime, mode,days;
        ImageView deleteIcon,locationIcon; // Reference to the delete icon
        Switch scheduleSwitch; // Reference to the switch

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            scheduleName = itemView.findViewById(R.id.idScheduleName);
            initialTime = itemView.findViewById(R.id.idInitialTime);
            finalTime = itemView.findViewById(R.id.idFinalTime);
            mode = itemView.findViewById(R.id.idMode);
            deleteIcon = itemView.findViewById(R.id.deleteIcon); // Reference to the delete icon
            scheduleSwitch = itemView.findViewById(R.id.idSwitch); // Updated to use scheduleSwitch
            days = itemView.findViewById(R.id.idDays);
            locationIcon = itemView.findViewById(R.id.locationIcon);


        }
    }

}


