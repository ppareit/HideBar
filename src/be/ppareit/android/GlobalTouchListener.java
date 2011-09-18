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

import java.io.DataInputStream;
import java.io.IOException;

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

    // the touch events are received in a separate thread
    Thread touchEventThread = null;
    volatile boolean keepGettingTouchEvents = false;


    public GlobalTouchListener(Context context) {
        this.display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
        .getDefaultDisplay();
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
                DataInputStream in = new DataInputStream(process.getInputStream());
                // keep track when touch started
                long downTime = -1;
                // keep track of position
                Point pos = new Point();
                while (keepGettingTouchEvents) {
                    String line = in.readLine();
                    if (line.equals("/dev/input/event1: 0003 0039 00000000")) {
                        // this is touch
                        if (downTime == -1) downTime = SystemClock.uptimeMillis();
                        final long eventTime = SystemClock.uptimeMillis();
                        line = readLineAndCheckStart(in, "/dev/input/event1: 0003 0030 ");
                        line = readLineAndCheckStart(in, "/dev/input/event1: 0003 0035 ");
                        String xCoord = line.substring(line.lastIndexOf(' ') + 1);
                        line = readLineAndCheckStart(in, "/dev/input/event1: 0003 0036 ");
                        String yCoord = line.substring(line.lastIndexOf(' ') + 1);
                        line = readLineAndCheckStart(in,
                                "/dev/input/event1: 0000 0002 00000000");
                        pos.x = Integer.parseInt(xCoord, 16);
                        pos.y = Integer.parseInt(yCoord, 16);
                        pos = normalizeScreenPosition(pos);
                        MotionEvent event = MotionEvent.obtain(downTime, eventTime,
                                MotionEvent.ACTION_DOWN, pos.x, pos.y, 0);
                        // Log.v(TAG, "Downevent: " + pos.x + " " + pos.y);
                        onTouchEvent(event);
                    } else if (line.equals("/dev/input/event1: 0000 0002 00000000")) {
                        // this event by itself is stop touching
                        // now use the starting downtime, and last known x and y position
                        final long eventTime = SystemClock.uptimeMillis();
                        MotionEvent event = MotionEvent.obtain(downTime, eventTime,
                                MotionEvent.ACTION_UP, pos.x, pos.y, 0);
                        downTime = -1;
                        onTouchEvent(event);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                process.destroy();
            }

        }

        private Point normalizeScreenPosition(Point pos) {
            final int rotation = display.getRotation();
            switch (rotation) {
            case Surface.ROTATION_0:
                return pos;
            case Surface.ROTATION_90:
                return new Point(pos.y, display.getHeight() - pos.x);
            case Surface.ROTATION_180:
                return new Point(display.getWidth() - pos.x, display.getHeight() - pos.y);
            case Surface.ROTATION_270:
                return new Point(display.getWidth() - pos.y, pos.x);
            }
            assert false : "Cannot be reached";
            return null;
        }

        private String readLineAndCheckStart(DataInputStream in, String startsWith)
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
                touchEventThread.join();
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
