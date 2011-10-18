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
package be.ppareit.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Context;
import android.graphics.Point;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.WindowManager;


/**
 * @author ppareit
 *
 * Intercepts all touch events on the screen. No need to have an active view visible. This
 * Requires root access, so only use it if you absolutely have to.
 *
 */
public abstract class GlobalTouchListener {

    static final String TAG = GlobalTouchListener.class.getSimpleName();

    // display needed to normalize coordinates depending on rotation
    private final Display display;

    private final Context context;

    // the touch events are received in a separate thread
    Thread touchEventThread = null;
    volatile boolean keepGettingTouchEvents = false;

    boolean inverted = false;

    public GlobalTouchListener(Context context) {
        this.display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
        .getDefaultDisplay();
        this.context = context;
    }

    Runnable getTouchEvents = new Runnable() {

        @Override
        public void run() {
            Process process = null;
            try {
                process = new ProcessBuilder()
                .command("su", "-c", "getevent")
                .redirectErrorStream(true)
                .start();
                BufferedReader stdout = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                // keep track when touch started
                long downTime = -1;
                // keep track of position
                Point pos = new Point();
                // is this a down event of up event
                int action = MotionEvent.ACTION_CANCEL;
                // the file that contains touch input
                String deviceFile = null;
                // for some devices, the touch up must be detected by 2 times
                // /dev/input/event?: 0000 0002 00000000
                boolean gaveTouch = false;
                while (keepGettingTouchEvents) {
                    String line = stdout.readLine();
                    if (line.startsWith("add device")) {
                        // the output enumerates all devices, we want all touch input so
                        // look for that device, currently I only know atmel-maxtouch but
                        // more might be needed, thus adding new devices is probably here
                        // TODO: it is possible to do the device registering outside loop
                        String testDeviceFile = line.split(": ")[1];
                        line = readLineAndCheckStart(stdout, "  name:     \"");
                        String device = line.split("\"")[1];
                        Log.v(TAG, "Possible device: " + device);
                        if (!device.equals("atmel-maxtouch") && !device.equals("it7260")
                                && !device.equals("qtouch-touchscreen")
                                && !device.equals("egalax_i2c"))
                            continue;
                        deviceFile = testDeviceFile;
                        if (device.equals("qtouch-touchscreen"))
                            inverted = true;
                    } else if (line.startsWith(deviceFile + ": 0003 ")) {
                        // this is touch
                        gaveTouch = false; // not yet given the touch event
                        String components[] = line.split(" ");
                        String key = components[2];
                        int val = Integer.parseInt(components[3], 16);
                        if (key.equals("0035")) {
                            pos.x = val;
                        } else if (key.equals("0036")) {
                            pos.y = val;
                        } else if (key.equals("0030")) {
                            // pressure (I guess)
                            if (val > 0) {
                                action = MotionEvent.ACTION_DOWN;
                                if (downTime == -1) downTime = SystemClock.uptimeMillis();
                            } else {
                                action = MotionEvent.ACTION_UP;
                            }
                        }
                    } else if (line.equals(deviceFile + ": 0000 0002 00000000")) {
                        // now was the event complete
                        if (action==MotionEvent.ACTION_UP && downTime == -1) {
                            continue; // we just did an up
                        }
                        // check if we just gave a touch event, if so, send touch up
                        if (gaveTouch)
                            action = MotionEvent.ACTION_UP;
                        final long eventTime = SystemClock.uptimeMillis();
                        pos = normalizeScreenPosition(pos);
                        MotionEvent event = MotionEvent.obtain(downTime, eventTime,
                                action, pos.x, pos.y, 0);
                        if (action==MotionEvent.ACTION_UP) downTime = -1;
                        onTouchEvent(event);
                        gaveTouch = true; // now we just gave the touch event
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                process.destroy();
            }
        }

        private Point normalizeScreenPosition(Point pos) {
            int rotation = display.getRotation();
            /*
            if (inverted) {
                switch (rotation) {
                case Surface.ROTATION_180:
                    rotation = Surface.ROTATION_0;
                    break;
                case Surface.ROTATION_270:
                    rotation = Surface.ROTATION_90;
                    break;
                }
            }
            */
            switch (rotation) {
            case Surface.ROTATION_0:
                //Log.v(TAG, "ROTATION_0 " + pos.x + " " + pos.y);
                return pos;
            case Surface.ROTATION_90:
                //Log.v(TAG, "ROTATION_90 " + pos.x + " " + pos.y);
                return new Point(pos.y, display.getHeight() - pos.x);
            case Surface.ROTATION_180:
                //Log.v(TAG, "ROTATION_180 " + pos.x + " " + pos.y);
                return new Point(display.getWidth() - pos.x, display.getHeight() - pos.y);
            case Surface.ROTATION_270:
                //Log.v(TAG, "ROTATION_270 " + pos.x + " " + pos.y);
                return new Point(display.getWidth() - pos.y, pos.x);
            }
            assert false : "Cannot be reached";
            return null;
        }

        private String readLineAndCheckStart(BufferedReader in, String startsWith)
        throws IOException {
            String line = in.readLine();
            if (! line.startsWith(startsWith)) {
                throw new IOException();
            }
            return line;
        }
    };

    /**
     * Call to start listening to touch events.
     */
    public void startListening() {
        Log.v(TAG, "startListening");
        assert touchEventThread == null : "Touch event thread not null";
        touchEventThread = new Thread(getTouchEvents);
        keepGettingTouchEvents = true;
        touchEventThread.start();
    }

    /**
     * Call to stop listening to touch events.
     */
    public void stopListening() {
        Log.v(TAG, "stopListening");
        assert touchEventThread != null : "Touch event thread null";
        boolean retry = true;
        keepGettingTouchEvents = false;
        while (retry) {
            try {
                // the thread probably is blocked on input, so timeout quickly
                // TODO: find a better way to stop the thread (I'm not sure it is stopped)
                touchEventThread.join(100);
                retry = false;
            } catch (InterruptedException e) {
                // swallow
            }
        }
        touchEventThread = null;
    }

    /**
     * Called when a touch event is dispatched. It is not possible to hold the event. So
     * in all cases the underlying view will also receive the touch event.
     *
     * @param event The MotionEvent object containing information about the event.
     */
    abstract public void onTouchEvent(MotionEvent event);

}
