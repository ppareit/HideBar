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
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;


/**
 * @author ppareit
 *
 * This preference screen is the main way to interact with the user. Instead of using
 * a SharedPreferences.onSharedPreferenceChangeListener, all the changes are cached
 * here. This enables us to directly make changes, otherwise we would have to wait till
 * the user closes the preference screen.
 *
 */
public class HideBarPreferences extends PreferenceActivity {

    private static final String TAG = HideBarPreferences.class.getSimpleName();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        final CheckBoxPreference shouldrunPref =
            (CheckBoxPreference) findPreference("shouldrun_preference");
        final CheckBoxPreference runatbootPref =
            (CheckBoxPreference) findPreference("runatboot_preference");
        runatbootPref.setEnabled(shouldServiceRun());
        final CheckBoxPreference hideatbootPref =
            (CheckBoxPreference) findPreference("hideatboot_preference");
        hideatbootPref.setEnabled(shouldServiceRunAtBoot(this));
        shouldrunPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean shouldrun = (Boolean) newValue;
                Context context = getApplicationContext();
                if (shouldrun) {
                    BackgroundService.start(context, false);
                } else {
                    BackgroundService.stop(context);
                }
                runatbootPref.setEnabled(shouldrun);
                hideatbootPref.setEnabled(runatbootPref.isEnabled() && runatbootPref.isChecked());
                return true;
            }
        });
        runatbootPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                hideatbootPref.setEnabled((Boolean) newValue);
                return true;
            }
        });

        final Preference showMethodPreference = findPreference("showbar_method_preference");
        showMethodPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue.equals("NONE")) {
                    Resources res = getResources();
                    Toast.makeText(getApplicationContext(),
                            res.getText(R.string.showbar_method_none_warning),
                            Toast.LENGTH_LONG).show();
                }
                if (shouldServiceRun()) {
                    BackgroundService.stop(getApplicationContext());
                    BackgroundService.start(getApplicationContext(), false);
                }
                return true;
            }
        });

        final Preference aboutPreference = findPreference("about_preference");
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

        if (shouldServiceRun()) {
            BackgroundService.start(getApplicationContext(), false);
        }
    }

    private boolean shouldServiceRun() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getBoolean("shouldrun_preference", true);
    }

    static public boolean shouldServiceRunAtBoot(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean("runatboot_preference", true) &&
            sp.getBoolean("shouldrun_preference", true);
    }

    static public boolean shouldStatusbarHideAtBoot(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean("runatboot_preference", true) &&
            sp.getBoolean("shouldrun_preference", true) &&
            sp.getBoolean("hideatboot_preference", false);
    }

    public enum ShowMethod {
        NONE,
        BOTTOM_TOUCH,
        BOTTOM_TOP_TOUCH;
    }

    static public ShowMethod methodToShowBar(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String method = sp.getString("showbar_method_preference", "BOTTOM_TOUCH");
        return ShowMethod.valueOf(method);
    }

    static public boolean ghostbackButton(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean("ghostback_preference", false);
    }
}













