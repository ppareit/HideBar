package be.ppareit.hidebar;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;
import be.ppareit.android.GlobalTouchListener;

public class RestoreSystembarService extends Service {

    static final String TAG = RestoreSystembarService.class.getSimpleName();

    private GlobalTouchListener touchListener = null;
    private GlobalTouchListener ghostBackButtonTouchListener = null;

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate");

        // start specialized listener, depending on how the systembar should show up
        switch (HideBarPreferences.methodToShowBar(this)) {
        case BOTTOM_TOUCH: {
            // make a touch listener, on correct touch we show the statusbar and stop
            touchListener = new GlobalTouchListener(this) {
                @Override
                public void onTouchEvent(MotionEvent event) {
                    if (event.getAction() != MotionEvent.ACTION_DOWN) return;
                    WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
                    Display display = wm.getDefaultDisplay();
                    if (event.getY() > display.getHeight() - 20) {
                        Log.v(TAG, "Swipe Up detected");
                        showSystembar();
                        RestoreSystembarService.this.stopSelf();
                    }
                }
            };
            touchListener.startListening();
            break;
        }
        case BOTTOM_TOP_TOUCH: {
            touchListener = new GlobalTouchListener(this) {
                // as long as these two are initial different, 47 is just random number
                long bottomTime = -47;
                long topTime = 47;
                @Override
                public void onTouchEvent(MotionEvent event) {
                    if (event.getAction() != MotionEvent.ACTION_DOWN) return;
                    WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
                    Display display = wm.getDefaultDisplay();
                    if (event.getY() > display.getHeight() - 20) {
                        bottomTime = event.getEventTime();
                    } else if (event.getY() < 20) {
                        topTime = event.getEventTime();
                    }
                    if (Math.abs(bottomTime - topTime) < 5) {
                        // top and bottom touch close in time
                        showSystembar();
                        RestoreSystembarService.this.stopSelf();
                    }
                }
            };
            touchListener.startListening();
            break;

        }
        case NONE:
            break;
        }
        if (HideBarPreferences.ghostbackButton(this)) {
            ghostBackButtonTouchListener = new GlobalTouchListener(this) {
                long lastTime = -1;
                boolean ignoreOne = false;
                @Override
                public void onTouchEvent(MotionEvent event) {
                    WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
                    Display display = wm.getDefaultDisplay();
                    long currentTime = SystemClock.uptimeMillis();
                    // only do ghost back in left bottom corner
                    if (event.getX() > 20 || event.getY() < display.getHeight() - 20)
                        return;
                    // only do a ghost back once every 2 seconds
                    if (lastTime > currentTime - 2000)
                        return;
                    // BUG: java process buffers, so lot of events are  received much
                    // to late, this ignores one event and this helps a lot
                    if (ignoreOne) {
                        lastTime = currentTime;
                        ignoreOne = false;
                        return;
                    }
                    Log.v(TAG, "Executing a ghost back event");
                    lastTime = currentTime;
                    ignoreOne = true;
                    sendBackEvent();
                }
            };
            ghostBackButtonTouchListener.startListening();
        }

    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        touchListener.stopListening();
        touchListener = null;
        if (ghostBackButtonTouchListener != null) {
            ghostBackButtonTouchListener.stopListening();
            ghostBackButtonTouchListener = null;
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
        try {
            Process proc = Runtime.getRuntime().exec(new String[]{
                    "am","startservice","-n","com.android.systemui/.SystemUIService"});
            proc.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendBackEvent() {
        Log.v(TAG, "sendBackEvent");
        try {
            new ProcessBuilder()
            .command("su", "-c", "input keyevent 4")
            .redirectErrorStream(true)
            .start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
