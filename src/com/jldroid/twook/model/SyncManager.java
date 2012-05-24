package com.jldroid.twook.model;

import java.util.GregorianCalendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.jldroid.twook.ChatService;
import com.jldroid.twook.Globals;

public class SyncManager extends BroadcastReceiver {

	public static final String ACTION_SYNC = "com.jldroid.twook.SYNC";
	
	public static final int WHAT_TWITTER_TIMELINE = 0;
	public static final int WHAT_TWITTER_MENTIONS = 1;
	public static final int WHAT_TWITTER_DIRECT_MESSAGES = 2;
	public static final int WHAT_TWITTER_SEARCH = 3;
	public static final int WHAT_TWITTER_LIST = 4;
	public static final int WHAT_TWITTER_USER = 5;
	
	public static final int WHAT_FACEBOOK_HOME = 10;
	public static final int WHAT_FACEBOOK_NOTIFICATIONS = 11;
	public static final int WHAT_FACEBOOK_MESSAGES = 12;
	public static final int WHAT_FACEBOOK_LIST = 13;
	public static final int WHAT_FACEBOOK_GROUP = 14;
	public static final int WHAT_FACEBOOK_USER = 15;
	public static final int WHAT_FACEBOOK_SEARCH = 16;
	
	public static final int WHAT_HOME = 20;
	public static final int WHAT_ABOUT_ME = 21;
	public static final int WHAT_MESSAGES = 22;
	public static final int WHAT_SEARCH = 23;
	
	private static final Uri GLOBAL_SYNC_URI = Uri.parse("sync://global/");
	public static final Uri COLUMN_SYNC_URI = Uri.parse("sync://column/");
	
	private static boolean mHasSetupSync = false;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		validateSync(context);
		if (AccountsManager.getInstance(context).getFacebookAccountCount() > 0) {
			context.startService(new Intent(context, ChatService.class));
		}
	}
	
	public static void validateSync(Context c) {
		if (!mHasSetupSync) {
			updateColumnsSync(c);
			updateGlobalSync(c);
			mHasSetupSync = true;
		}
	}
	
	public static void updateColumnsSync(Context c) {
		ColumnManager cm = ColumnManager.getInstance(c);
		for (int i = cm.getEnabledColumnCount() - 1; i >= 0; i--) {
			updateColumnSync(c, cm.getEnabledColumnInfo(i));
		}
	}
	
	public static void updateAccountColumnsSync(Context c, IAccount account) {
		ColumnManager cm = ColumnManager.getInstance(c);
		for (int i = cm.getEnabledColumnCount() - 1; i >= 0; i--) {
			ColumnInfo info = cm.getEnabledColumnInfo(i);
			if (info.getAccount() == account) {
				updateColumnSync(c, info);
			}
		}
	}

	private static long getSyncInterval(Context c, ColumnInfo info) {
		long interval = info.getSyncInterval();
		if (interval == -2) {
			interval = PreferenceManager.getDefaultSharedPreferences(c).getLong("defaultSyncInterval", -1);
			interval = interval != -1 ? interval * 60000l : -1;
		}
		return interval;
	}
	
	public static void updateColumnSync(Context c, ColumnInfo info) {
		AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
		PendingIntent pi = createSyncIntent(c, info);
		am.cancel(pi);
		long interval = getSyncInterval(c, info);
		if (interval != -1) {
			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), interval, pi);
		}
	}
	
	private static long converTimeToMillis(String timeStr) {
		GregorianCalendar cal = new GregorianCalendar();
		cal.set(GregorianCalendar.HOUR_OF_DAY, Integer.parseInt(timeStr.substring(0, timeStr.indexOf(':'))));
		cal.set(GregorianCalendar.MINUTE, Integer.parseInt(timeStr.substring(timeStr.indexOf(':') + 1)));
		return cal.getTimeInMillis();
	}
	
	// 0 = not 1 = this day 2 = next day
	private static int isNextUpdateInTimeout(long nextSyncTime, long timeoutStart, long timeoutEnd) {
		final long day = 3600000 * 24;
		if (timeoutStart < timeoutEnd && nextSyncTime > timeoutStart && nextSyncTime < timeoutEnd) {
			return 1;
		} else if (nextSyncTime > timeoutStart && nextSyncTime < timeoutEnd + day) {
			return 2;
		} else if (nextSyncTime > timeoutStart - day && nextSyncTime < timeoutEnd) {
			return 1;
		} else {
			return 0;
		}
	}
	
	public static void checkColumnSyncTimeout(Context c, ColumnInfo info) {
		long interval = getSyncInterval(c, info);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		if (prefs.getBoolean("timeoutEnabled", false)) {
			String startTime = prefs.getString("timeoutStart", "00:00");
			String endTime = prefs.getString("timeoutEnd", "00:00");
			long timeoutStart = converTimeToMillis(startTime);
			long timeoutEnd = converTimeToMillis(endTime);
			if (interval != -1 && timeoutStart != timeoutEnd) {
				long nextSyncTime = System.currentTimeMillis() + interval;
				int r = isNextUpdateInTimeout(nextSyncTime, timeoutStart, timeoutEnd);
				if (r != 0) {
					if (r == 2) {
						timeoutEnd += 3600000 * 24;
					}
					AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
					PendingIntent pi = createSyncIntent(c, info);
					am.cancel(pi);
					am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 
							SystemClock.elapsedRealtime() + timeoutEnd - System.currentTimeMillis(), interval, pi);
					return;
				}
			}
		} 
		if (prefs.getBoolean("timeout2Enabled", false)) {
			String startTime = prefs.getString("timeout2Start", "00:00");
			String endTime = prefs.getString("timeout2End", "00:00");
			long timeoutStart = converTimeToMillis(startTime);
			long timeoutEnd = converTimeToMillis(endTime);
			if (interval != -1 && timeoutStart != timeoutEnd) {
				long nextSyncTime = System.currentTimeMillis() + interval;
				int r = isNextUpdateInTimeout(nextSyncTime, timeoutStart, timeoutEnd);
				if (r != 0) {
					if (r == 2) {
						timeoutEnd += 3600000 * 24;
					}
					AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
					PendingIntent pi = createSyncIntent(c, info);
					am.cancel(pi);
					am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 
							SystemClock.elapsedRealtime() + timeoutEnd - System.currentTimeMillis(), interval, pi);
				}
			}
		}
	}
	
	private static PendingIntent createSyncIntent(Context c, ColumnInfo info) {
		long accountID = info.getAccount() != null ? info.getAccount().getUser().id : -1;
		Intent intent = new Intent(ACTION_SYNC, COLUMN_SYNC_URI.buildUpon()
					.appendPath(String.valueOf(accountID))
					.appendPath(String.valueOf(info.getProvider().getWhat()))
					.appendPath(String.valueOf(info.getProvider().getID()))
					.build());
		return PendingIntent.getService(c, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
	}
	
	public static void stopColumnSync(Context c, ColumnInfo info) {
		AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
		am.cancel(createSyncIntent(c, info));
	}
	
	public static void stopSync(Context c, Intent intent) {
		AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
		am.cancel(PendingIntent.getService(c, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
	}
	
	private static void updateGlobalSync(Context c) {
		Intent intent = new Intent(ACTION_SYNC, GLOBAL_SYNC_URI);
		PendingIntent pi = PendingIntent.getService(c, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
		am.cancel(pi);
		am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 
				SystemClock.elapsedRealtime() + Globals.GLOBAL_SYNC_INTERVAL, 
				Globals.GLOBAL_SYNC_INTERVAL, 
				pi);
	}

}
