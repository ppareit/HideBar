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

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.stericson.RootTools.CommandCapture;
import com.stericson.RootTools.RootTools;

/**
 * Class with global information about the specific device.
 */
public enum Device {

    INSTANCE;

    private static String TAG = Device.class.getSimpleName();

    private boolean mHasBeenInitialized = false;
    private Context mAppContext = null;

    // flag if the systembar is currently visible, assume at start this is true
    private boolean mSystembarVisible = true;

    static public void initialize(Context appContext) {
        if (INSTANCE.mHasBeenInitialized == true) {
            Log.e(TAG, "Initializing already initialized class " + TAG);
            // throw new IllegalStateException(
            // "Trying to initialize already initialized class " + TAG);
        }
        INSTANCE.mHasBeenInitialized = true;
        INSTANCE.mAppContext = appContext;
    }

    static public Device getInstance() {
        INSTANCE.checkInitialized();
        return INSTANCE;
    }

    private void checkInitialized() {
        if (mHasBeenInitialized == false)
            throw new IllegalStateException("Singleton class " + TAG
                    + " is not yet initialized");
    }

    public enum AndroidVersion {
        HC, ICS, JB, UNKNOWN
    };

    public AndroidVersion getAndroidVersion() {
        checkInitialized();
        Log.v(TAG, "getAndroidVersion called");
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (11 <= sdk && sdk <= 13) {
            Log.v(TAG, "We are running on HoneyComb");
            return AndroidVersion.HC;
        } else if (14 <= sdk && sdk <= 15) {
            Log.v(TAG, "We are running on IceCreamSandwich");
            return AndroidVersion.ICS;
        } else if (16 == sdk) {
            Log.v(TAG, "We are running on JellyBean");
            return AndroidVersion.JB;
        } else {
            Log.v(TAG, "We don't know what we are running on");
            return AndroidVersion.UNKNOWN;
        }
    }

    public void showSystembar(boolean makeVisible) {
        checkInitialized();
        try {
            // depending on makeVisible, show or hide the bar
            if (makeVisible) {
                Log.v(TAG, "showBar will show systembar");
                // execute in correct environment
                CommandCapture command;
                if (getAndroidVersion() == AndroidVersion.HC) {
                    command = new CommandCapture(0,
                            "am startservice -n com.android.systemui/.SystemUIService");
                } else {
                    command = new CommandCapture(0, "rm /sdcard/hidebar-lock\n"
                            + "sleep 3\n" + "LD_LIBRARY_PATH=/vendor/lib:/system/lib "
                            + "am startservice -n com.android.systemui/.SystemUIService");
                }
                // RootTools.getShell(true).closeAll();
                RootTools.getShell(true).add(command).waitForFinish();
                // we just shown the bar, set flag to visible
                mSystembarVisible = true;
                // let everybody know that now the bar is visible
                mAppContext.sendBroadcast(new Intent(Constants.ACTION_BARSHOWN));
            } else {
                Log.v(TAG, "showBar will hide the systembar");
                // execute in correct environment
                CommandCapture command;
                if (getAndroidVersion() == AndroidVersion.HC) {
                    command = new CommandCapture(0,
                            "LD_LIBRARY_PATH=/vendor/lib:/system/lib "
                                    + "service call activity 79 s16 com.android.systemui");
                } else if (RootTools.isBusyboxAvailable()) {
                    command = new CommandCapture(0, "touch /sdcard/hidebar-lock\n"
                            + "while [ -f /sdcard/hidebar-lock ]\n" + "do\n"
                            + "busybox killall com.android.systemui\n" + "sleep 1\n"
                            + "done &\n");
                } else {
                    command = new CommandCapture(0, "touch /sdcard/hidebar-lock\n"
                            + "while [ -f /sdcard/hidebar-lock ]\n" + "do\n"
                            + "killall com.android.systemui\n" + "sleep 1\n" + "done &\n");
                }
                RootTools.getShell(true).add(command);
                // no proc.waitFor();
                // we just hide the bar, set flag to not visible
                mSystembarVisible = false;
                // now let everybody know that the bar has been hidden
                mAppContext.sendBroadcast(new Intent(Constants.ACTION_BARHIDDEN));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return true is the systembar is visible or false when it is not visible
     */
    public boolean isSystembarVisible() {
        checkInitialized();
        // TODO: this might be improved by using 'ps ...' to see if the systemui process
        // is running and by checking the /sdcard/hidebar-lock file
        return mSystembarVisible;
    }

    public void sendBackEvent() {
        Log.v(TAG, "sendBackEvent");
        try {
            CommandCapture command = new CommandCapture(0,
                    "LD_LIBRARY_PATH=/vendor/lib:/system/lib input keyevent 4");
            RootTools.getShell(true).add(command);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
