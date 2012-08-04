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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;

import android.util.Log;

/**
 * Class with global information about the specific device.
 */
public final class Device {

    private static String TAG = Device.class.getSimpleName();

    static boolean sRootHasBeenChecked = false;
    static boolean sDeviceIsRooted = false;

    static public boolean isRooted() {

        Log.v(TAG, "isRooted called");

        if (sRootHasBeenChecked) {
            Log.v(TAG, "Result for isRooted is cached: " + sDeviceIsRooted);
            return sDeviceIsRooted;
        }

        // // first try
        // Log.v(TAG, "Checking if device is rooted with the os build tags");
        // String tags = android.os.Build.TAGS;
        // if (tags != null && tags.contains("test-keys")) {
        // Log.v(TAG, "Device seems rooted");
        // sRootHasBeenChecked = true;
        // sDeviceIsRooted = true;
        // return true;
        // }

        // second try
        Log.v(TAG, "Checking if device is rooted by checking if Superuser is available");
        try {
            File file = new File("/system/app/Superuser.apk");
            if (file.exists()) {
                Log.v(TAG, "Device seems rooted");
                sRootHasBeenChecked = true;
                sDeviceIsRooted = true;
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // third try
        Log.v(TAG, "Checking if device is rooted by checking if su is available");
        try {
            // get the existing environment
            ArrayList<String> envlist = new ArrayList<String>();
            Map<String, String> env = System.getenv();
            for (String envName : env.keySet()) {
                envlist.add(envName + "=" + env.get(envName));
            }
            String[] envp = (String[]) envlist.toArray(new String[0]);
            // execute which su
            Process proc = Runtime.getRuntime().exec(
                    new String[] { "/system/xbin/which", "su" }, envp);
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    proc.getInputStream()));
            // if we receive location, we are on a rooted device
            // TODO: can break if the executable is on the device, but non working
            if (in.readLine() != null) {
                Log.v(TAG, "Device seems rooted");
                sRootHasBeenChecked = true;
                sDeviceIsRooted = true;
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        sRootHasBeenChecked = true;
        sDeviceIsRooted = false;
        return false;

    }

}
