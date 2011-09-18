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

import java.io.IOException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;
import be.ppareit.android.GlobalTouchListener;

public class BackgroundService extends Service {

    static final String TAG = BackgroundService.class.getSimpleName();
    static final String HIDE_ACTION = "be.ppareit.hidebar.HIDE_ACTION";

    class HideReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "HideReceiver.onReceive");
            showBar(false);
            new GlobalTouchListener(BackgroundService.this) {
                @Override
                public void onTouchEvent(MotionEvent event) {
                    if (event.getAction() != MotionEvent.ACTION_DOWN) return;
                    WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
                    Display display = wm.getDefaultDisplay();
                    if (event.getY() > display.getHeight() - 20) {
                        showBar(true);
                        stopListening();
                    }
                }
            }.startListening();
        }
    }

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate");

        showBar(true);

        registerReceiver(new HideReceiver(), new IntentFilter(HIDE_ACTION));

        Intent hideBarIntent = new Intent(HIDE_ACTION);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, hideBarIntent, 0);

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new Notification.Builder(this)
        .setContentTitle(getResources().getText(R.string.hidebar_notification))
        .setSmallIcon(R.drawable.ic_icon_hidebar)
        .setOngoing(true)
        .setContentIntent(pi)
        .getNotification();
        nm.notify(TAG, 0, notification);
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand");
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind");
        return null;
    }

    protected void showBar(boolean makeVisible) {
        try {
            if (makeVisible) {
                Process proc;
                proc = Runtime.getRuntime().exec(new String[]{
                        "am","startservice","-n","com.android.systemui/.SystemUIService"});
                proc.waitFor();
            } else {
                Process proc = Runtime.getRuntime().exec(new String[]{
                        "su","-c","service call activity 79 s16 com.android.systemui"});
                proc.waitFor();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
