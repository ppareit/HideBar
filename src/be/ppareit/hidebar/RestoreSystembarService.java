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
package be.ppareit.hidebar;

import java.util.LinkedList;

import be.ppareit.hidebar.Constants.MarketType;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * This service is responsible to restore the system bar
 */
public class RestoreSystembarService extends Service {

    static final String TAG = RestoreSystembarService.class.getSimpleName();

    private final LinkedList<View> mTouchAreas = new LinkedList<View>();

    private long mBottomTouchTime = -47;
    private long mTopTouchTime = 47;

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate");

        // start specialized listener, depending on how the systembar should
        // show up
        switch (HideBarPreferences.methodToShowBar(this)) {
        case BOTTOM_TOUCH: {
            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT,
                    LayoutParams.TYPE_SYSTEM_ALERT,
                    LayoutParams.FLAG_NOT_FOCUSABLE
                            | LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.OPAQUE);
            params.alpha = 0.0f;
            params.height = 25;
            params.gravity = Gravity.BOTTOM;
            params.x = 0;
            params.y = 0;
            LinearLayout touchLayout = new LinearLayout(this);
            touchLayout.setBackgroundColor(0x00FFFFFF);
            touchLayout.setLayoutParams(new LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            if (HideBarPreferences.ghostbackButton(this)) {
                View backArea = new View(this);
                backArea.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        Log.d(TAG, "backArea touched");
                        sendBackEvent();
                        return false;
                    }
                });
                touchLayout.addView(backArea, 40, 25);
            }
            View restoreArea = new View(this);
            restoreArea.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Log.d(TAG, "touchArea touched");
                    showSystembar();
                    RestoreSystembarService.this.stopSelf();
                    return false;
                }
            });
            touchLayout.addView(restoreArea);
            wm.addView(touchLayout, params);
            mTouchAreas.add(touchLayout);
            break;
        }
        case BOTTOM_TOP_TOUCH: {
            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT,
                    LayoutParams.TYPE_SYSTEM_ALERT,
                    LayoutParams.FLAG_NOT_FOCUSABLE
                            | LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.OPAQUE);
            params.alpha = 0.0f;
            params.height = 25;
            params.gravity = Gravity.BOTTOM;
            params.x = 0;
            params.y = 0;
            LinearLayout bottomLayout = new LinearLayout(this);
            bottomLayout.setBackgroundColor(0x00FFFFFF);
            bottomLayout.setLayoutParams(new LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            if (HideBarPreferences.ghostbackButton(this)) {
                View backArea = new View(this);
                backArea.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        Log.d(TAG, "backArea touched");
                        sendBackEvent();
                        return false;
                    }
                });
                bottomLayout.addView(backArea, 40, 25);
            }
            View bottomArea = new View(this);
            bottomArea.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Log.d(TAG, "bottomArea touched");
                    mBottomTouchTime = SystemClock.uptimeMillis();
                    if (Math.abs(mBottomTouchTime - mTopTouchTime) < 2000) {
                        showSystembar();
                        RestoreSystembarService.this.stopSelf();
                    }
                    return false;
                }
            });
            bottomLayout.addView(bottomArea);
            wm.addView(bottomLayout, params);
            mTouchAreas.add(bottomLayout);

            params.gravity = Gravity.TOP;
            LinearLayout topLayout = new LinearLayout(this);
            topLayout.setBackgroundColor(0x00FFFFFF);
            topLayout.setLayoutParams(new LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            View topArea = new View(this);
            topArea.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Log.d(TAG, "topArea touched");
                    mTopTouchTime = SystemClock.uptimeMillis();
                    if (Math.abs(mBottomTouchTime - mTopTouchTime) < 2000) {
                        showSystembar();
                        RestoreSystembarService.this.stopSelf();
                    }
                    return false;
                }
            });
            topLayout.addView(topArea);
            wm.addView(topLayout, params);
            mTouchAreas.add(topLayout);
            break;

        }
        case NONE:
            break;
        }

        if (Constants.MARKETTYPE == MarketType.DEMO) {
            Log.i(TAG, "Adding the demo area");
            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT,
                    LayoutParams.TYPE_SYSTEM_ALERT,
                    LayoutParams.FLAG_NOT_FOCUSABLE
                            | LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.OPAQUE);
            params.alpha = 1.0f;
            params.x = 30;
            params.y = 30;
            params.height = 45;
            params.width = 135;
            params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
            LinearLayout layout = new LinearLayout(this);
            layout.setBackgroundColor(0x00FF0000);
            layout.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            ImageView demoArea = new ImageView(this);
            demoArea.setBackgroundColor(Color.YELLOW);
            demoArea.setImageResource(R.drawable.hidebar_demo_button);
            demoArea.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Log.d(TAG, "Demo Area touched");
                    v.setVisibility(View.GONE);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setData(Uri.parse("market://details?id=be.ppareit.hidebar"));
                    startActivity(intent);
                    return false;
                }
            });
            layout.addView(demoArea, 135, 45);
            wm.addView(layout, params);
            mTouchAreas.add(layout);
            new HideDemoButtonTask().execute(layout);
        }
    }

    /**
     * Hide the demo button after a short while
     */
    private class HideDemoButtonTask extends AsyncTask<View, Void, Void> {
        View mDemoButton = null;

        @Override
        protected Void doInBackground(View... params) {
            mDemoButton = params[0];
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            mDemoButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        while (!mTouchAreas.isEmpty()) {
            wm.removeView(mTouchAreas.pop());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand");
        // return sticky, so android will keep our service running
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind");
        return null;
    }

    private void showSystembar() {
        Log.v(TAG, "showSystembar");
        BackgroundService.showBar(true);
    }

    private void sendBackEvent() {
        Log.v(TAG, "sendBackEvent");
        try {
            new ProcessBuilder()
                    .command("su", "-c",
                            "LD_LIBRARY_PATH=/vendor/lib:/system/lib input keyevent 4")
                    .redirectErrorStream(true).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
