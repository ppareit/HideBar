package be.ppareit.hidebar;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import be.ppareit.hidebar.Constants.MarketType;

public class DemoService extends Service {

    static final String TAG = DemoService.class.getSimpleName();

    LinearLayout mDemoButton = null;
    BarShownReceiver mBarShownReceiver = null;

    public static class BarHiddenReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive broadcast: " + Constants.ACTION_BARHIDDEN);
            if (Constants.MARKETTYPE != MarketType.DEMO)
                return;
            context.startService(new Intent(context, DemoService.class));
        }

    }

    /**
     * When we receive that the bar is shown again, we stop this service so that
     * that the notification that we are running the demo version is removed
     */
    private class BarShownReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "onReceive broadcast: " + intent.getAction());
            stopSelf();
        }

    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        if (mBarShownReceiver != null) {
            unregisterReceiver(mBarShownReceiver);
            mBarShownReceiver = null;
        }
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
                startActivity(intent);
                return false;
            }
        });
        mDemoButton.addView(demoArea, 135, 45);
        wm.addView(mDemoButton, params);
        new HideDemoButtonTask().execute();
        // start listening for the broadcast that the bar is reshow
        mBarShownReceiver = new BarShownReceiver();
        IntentFilter barShownIntentFilter = new IntentFilter(Constants.ACTION_BARSHOWN);
        registerReceiver(mBarShownReceiver, barShownIntentFilter);
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
                Thread.sleep(10000);
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

}
