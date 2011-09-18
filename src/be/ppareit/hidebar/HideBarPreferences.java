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

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;

public class HideBarPreferences extends PreferenceActivity {

    private static final String TAG = HideBarPreferences.class.getSimpleName();


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate'ing");
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        Preference aboutPreference = findPreference("about_preference");
        aboutPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Dialog dlg = new Dialog(HideBarPreferences.this);
                dlg.setContentView(R.layout.about);
                dlg.setTitle(getResources().getText(R.string.about_label));
                dlg.show();
                return true;
            }
        });

        startService(new Intent(this, BackgroundService.class));

        Log.v(TAG, "onCreate'ed");
    }
}













