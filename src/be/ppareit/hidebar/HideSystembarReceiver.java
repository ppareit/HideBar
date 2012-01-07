package be.ppareit.hidebar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class HideSystembarReceiver extends BroadcastReceiver {

    private static final String TAG = HideSystembarReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "onReceive");
        // we received the intent to hide the statusbar
        hideSystembar();
        // start the restore systembar service
        context.startService(new Intent(context, RestoreSystembarService.class));
    }

    private void hideSystembar() {
        Log.v(TAG, "hideSystembar");
        try {
            Process proc = Runtime.getRuntime().exec(new String[]{
                    "su","-c","service call activity 79 s16 com.android.systemui"});
            proc.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
