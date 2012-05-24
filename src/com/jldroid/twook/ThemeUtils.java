package com.jldroid.twook;

import com.jldroid.twook.activities.MainActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Process;
import android.preference.PreferenceManager;

public class ThemeUtils {
	
	private static final String DEFAULT_THEME = "light";
	private static final int DEFAULT_THEME_ID = R.style.CustomTheme_Light;
	
	private static String sCurrentTheme;
	private static int sCurrentThemeId = 0;
	
	public static void setupActivityTheme(Activity a) {
		if (sCurrentTheme == null) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(a);
			sCurrentTheme = prefs.getString("theme", DEFAULT_THEME);
			if (sCurrentTheme.equals("light")) {
				sCurrentThemeId = R.style.CustomTheme_Light;
			} else if (sCurrentTheme.equals("dark")) {
				sCurrentThemeId = R.style.CustomTheme_Dark;
			} else {
				sCurrentTheme = DEFAULT_THEME;
				sCurrentThemeId = DEFAULT_THEME_ID;
			}
		}
		a.setTheme(sCurrentThemeId);
	}
	
	public static void setTheme(Context c, String theme) {
		if (!sCurrentTheme.equals(theme)) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
			prefs.edit()
				.putString("theme", theme)
				.commit();
			Process.killProcess(Process.myPid());
			c.startActivity(new Intent(c.getApplicationContext(), MainActivity.class));
		}
	}
	
	public static String getCurrentTheme() {
		return sCurrentTheme;
	}
	
}
