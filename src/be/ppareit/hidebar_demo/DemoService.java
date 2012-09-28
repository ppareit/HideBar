/*******************************************************************************
 * Copyright (c) 2012 Pieter Pareit.
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
package be.ppareit.hidebar_demo;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class DemoService extends Service {

    static final String TAG = DemoService.class.getSimpleName();

    private LinearLayout mDemoButton = null;

    /**
     * When we receive that the bar gets hidden (this receiver is defined in the
     * manifest), we start the demo service is this is an unpaid application.
     */
    public static class BarHiddenReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "onReceive broadcast: " + intent.getAction());
            context.startService(new Intent(context, DemoService.class));
        }

    }

    /**
     * When we receive that the bar is shown again, we stop this service so that that the
     * notification that we are running the demo version is removed
     */
    BroadcastReceiver mBarShownReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "onReceive broadcast: " + intent.getAction());
            stopSelf();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBarShownReceiver);
        if (mDemoButton != null) {
            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            wm.removeView(mDemoButton);
            mDemoButton = null;
        }
    }

    @Override
    public void onLowMemory() {
        // this service is not that important, so stop ourself when
        // system resources are tight
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT,
                LayoutParams.TYPE_SYSTEM_ALERT, LayoutParams.FLAG_NOT_FOCUSABLE
                        | LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, PixelFormat.OPAQUE);
        params.alpha = 1.0f;
        params.x = 30;
        params.y = 30;
        params.height = 45;
        params.width = 135;
        params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        mDemoButton = new LinearLayout(this);
        mDemoButton.setBackgroundColor(0x00FF0000);
        mDemoButton.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));
        final ImageView demoArea = new ImageView(this);
        demoArea.setImageResource(R.drawable.hidebar_demo_button);
        demoArea.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d(TAG, "Demo Area touched");
                // we stop our service (this will remove any windows)
                stopSelf();
                // start the market at our application
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("market://details?id=be.ppareit.hidebar"));
                try {
                    // this can fail if there is no market installed
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG,
                            "Failed to lauch the market. Market probably not installed");
                    e.printStackTrace();
                }
                return false;
            }
        });
        mDemoButton.addView(demoArea, 135, 45);
        wm.addView(mDemoButton, params);
        new HideDemoButtonTask().execute();
        // start listening for the broadcast that the bar is reshow
        IntentFilter barShownIntentFilter = new IntentFilter(Constants.ACTION_BARSHOWN);
        registerReceiver(mBarShownReceiver, barShownIntentFilter);
        // for each time the bar is hidden, increment the demo startups
        incrNumberOfDemoStartups(getApplicationContext());
        // if android kills this service, there is no need to be restarted
        return START_NOT_STICKY;
    }

    /**
     * Hide the demo button after a short while
     */
    private class HideDemoButtonTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                long time = 10000 + 1000 * numberOfDemoStartups(getApplicationContext());
                Thread.sleep(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    private static int numberOfDemoStartups(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getInt("number_of_demo_startups", 0);
    }

    private static void incrNumberOfDemoStartups(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = sp.edit();
        edit.putInt("number_of_demo_startups",
                sp.getInt("number_of_demo_startups", 0) + 1);
        edit.apply();
    }

}
