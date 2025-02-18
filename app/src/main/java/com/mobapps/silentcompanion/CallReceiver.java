package com.mobapps.silentcompanion;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.List;

public class CallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

            Log.d("CallReceiver", "Phone state: " + state + ", Incoming number: " + incomingNumber);

            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state) && incomingNumber != null) {
                checkAndDisableSilentMode(context, incomingNumber);
            }
        }
    }

    private void checkAndDisableSilentMode(Context context, String incomingNumber) {
        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (audioManager == null) return;

        // Get all stored contact numbers from the database
        List<ContactAdapter.Contact> contactList = databaseHelper.readAllContacts();

        for (ContactAdapter.Contact contact : contactList) {
            if (normalizeNumber(contact.getNumber()).equals(normalizeNumber(incomingNumber))) {
                Log.d("CallReceiver", "Match found: " + incomingNumber);

                if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                    Log.d("CallReceiver", "Silent mode disabled for incoming call.");

                    new Handler().postDelayed(() -> {
                        audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                        Log.d("CallReceiver", "Silent mode re-enabled after 60 seconds.");
                    }, 60000);
                }
                break;
            }
        }
    }

    private String normalizeNumber(String number) {
        if (number == null) return "";
        return number.replaceAll("\\s", "").replaceAll("[^0-9+]", "");
    }
}
