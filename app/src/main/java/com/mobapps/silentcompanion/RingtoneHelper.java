package com.mobapps.silentcompanion;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

public class RingtoneHelper {
    private Ringtone ringtone;

    public void playDefaultRingtone(Context context) {
        Uri ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE);
        ringtone = RingtoneManager.getRingtone(context, ringtoneUri);

        if (ringtone != null) {
            ringtone.play();
        }
    }

    public void stopRingtone() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
    }
}
