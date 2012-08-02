/*******************************************************************************
 * Copyright (c) 2011 Pieter Pareit.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *     Pieter Pareit - initial API and implementation
 ******************************************************************************/
package be.ppareit.hidebar;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class BackgroundService extends Service {

    static final String TAG = BackgroundService.class.getSimpleName();

    static private Context sContext = null;
    static private Intent sIntent = null;

    private static boolean sStartupByBoot = false;

    /**
     * Only instantiate class using this method!
     */
    static public void start(Context context, boolean startupbyboot) {
        BackgroundService.sStartupByBoot = startupbyboot;
        sIntent = new Intent(context, BackgroundService.class);
        context.startService(sIntent);
        sContext = context;
    }

    static public void stop(Context context) {
        context.stopService(sIntent);
        sIntent = null;
        sContext = null;
    }

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate");

        // create the intent that can hide the statusbar
        Intent hideSystembarIntent = new Intent(this, HideSystembarReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, hideSystembarIntent, 0);

        // if started up from boot, maybe hide the statusbar
        if (sStartupByBoot && HideBarPreferences.shouldStatusbarHideAtBoot(this)) {
            try {
                pi.send();
            } catch (CanceledException e) {
                e.printStackTrace();
            }
        }
        sStartupByBoot = false;

        // create the notification used to start the intent to hide the
        // statusbar
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new Notification.Builder(this)
                .setContentTitle(getResources().getText(R.string.hidebar_notification))
                .setSmallIcon(R.drawable.ic_icon_hidebar).setOngoing(true)
                .setContentIntent(pi).getNotification();
        nm.notify(TAG, 0, notification);
        startForeground(0, notification);
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");

        // remove the notification
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancelAll();
        stopForeground(true);

        // we where asked to stop running, so make sure the user gets back his
        // status bar
        showBar(true);

        stopService(new Intent(this, RestoreSystembarService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand");

        // we just have to start service once, not keep restarting
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind");
        return null;
    }

    static void showBar(boolean makeVisible) {
        try {
            // This used to be the following nice calls:
            // service call activity 79 s16 com.android.systemui
            // am startservice -n com.android.systemui/.SystemUIService
            // but now YOU (google engineer) forced me to do such a thing!
            if (makeVisible) {
                Log.v(TAG, "showBar will show systembar");
                Runtime.getRuntime()
                        .exec(new String[] {
                                "su",
                                "-c",
                                "rm /sdcard/hidebar-lock\n"
                                        + "sleep 5\n"
                                        + "LD_LIBRARY_PATH=/vendor/lib:/system/lib am startservice -n com.android.systemui/.SystemUIService" });
                sContext.sendBroadcast(new Intent(Constants.ACTION_BARSHOWN));
            } else {
                Log.v(TAG, "showBar will hide the systembar");
                Runtime.getRuntime()
                        .exec(new String[] {
                                "su",
                                "-c",
                                "touch /sdcard/hidebar-lock\n"
                                        + "while [ -f /sdcard/hidebar-lock ]\n"
                                        + "do\n"
                                        + "killall com.android.systemui\n"
                                        + "sleep 1\n"
                                        + "done\n"
                                        + "LD_LIBRARY_PATH=/vendor/lib:/system/lib am startservice -n com.android.systemui/.SystemUIService" });
                // no proc.waitFor();
                sContext.sendBroadcast(new Intent(Constants.ACTION_BARHIDDEN));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
