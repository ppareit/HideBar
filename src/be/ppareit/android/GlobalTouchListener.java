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

import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;

public abstract class GlobalTouchListener {

    static final String TAG = GlobalTouchListener.class.getSimpleName();

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
                long downTime = -1;
                while (keepGettingTouchEvents) {
                    String line = in.readLine();
                    if (line.equals("/dev/input/event1: 0003 0039 00000000")) {
                        if (downTime == -1) downTime = SystemClock.uptimeMillis();
                        final long eventTime = SystemClock.uptimeMillis();
                        line = readLineAndCheckStart(in, "/dev/input/event1: 0003 0030 ");
                        line = readLineAndCheckStart(in, "/dev/input/event1: 0003 0035 ");
                        String xCoord = line.substring(line.lastIndexOf(' ') + 1);
                        line = readLineAndCheckStart(in, "/dev/input/event1: 0003 0036 ");
                        String yCoord = line.substring(line.lastIndexOf(' ') + 1);
                        line = readLineAndCheckStart(in, "/dev/input/event1: 0000 0002 00000000");
                        int x = Integer.parseInt(xCoord, 16);
                        int y = Integer.parseInt(yCoord, 16);
                        MotionEvent event = MotionEvent.obtain(downTime, eventTime,
                                MotionEvent.ACTION_DOWN, x, y, 0);
                        Log.v(TAG, "Downevent: " + x + " " + y);
                        onTouchEvent(event);
                    } else if (line.equals("/dev/input/event1: 0000 0002 00000000")) {
                        final long eventTime = SystemClock.uptimeMillis();
                        MotionEvent event = MotionEvent.obtain(downTime, eventTime,
                                MotionEvent.ACTION_UP, 0, 0, 0);
                        onTouchEvent(event);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                process.destroy();
            }

        }

        String readLineAndCheckStart(DataInputStream in, String startsWith)
        throws IOException {
            String line = in.readLine();
            if (! line.startsWith(startsWith)) {
                throw new IOException();
            }
            return line;
        }
    };

    Thread touchEventThread = null;
    boolean keepGettingTouchEvents = false;

    public void startListening() {
        Log.v(TAG, "startListening");
        assert touchEventThread == null : "Touch event thread not null";
        touchEventThread = new Thread(getTouchEvents);
        keepGettingTouchEvents = true;
        touchEventThread.start();
    }

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


    abstract public void onTouchEvent(MotionEvent event);

}
