package com.jldroid.twook.activities;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.RingtonePreference;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.jldroid.twook.R;
import com.jldroid.twook.ThemeUtils;
import com.jldroid.twook.model.ColumnInfo;
import com.jldroid.twook.model.ColumnManager;

public class ColumnPrefsActivity extends SherlockPreferenceActivity implements OnPreferenceChangeListener {

	public static final String EXTRA_COLUMN_INDEX = "com.jldroid.twook.COLUMN_INDEX";
	
	private ColumnInfo mInfo;
	
	private CheckBoxPreference mNotificationEnabled;
	private CheckBoxPreference mSoundEnabled;
	private CheckBoxPreference mVibrateEnabled;
	private CheckBoxPreference mLEDEnabled;
	private RingtonePreference mRingtonePref;
	
	private ListPreference mSyncIntervalPref;
	
	@Override
	protected void onCreate(Bundle pSavedInstanceState) {
		ThemeUtils.setupActivityTheme(this);
		super.onCreate(pSavedInstanceState);
		
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		addPreferencesFromResource(R.xml.column_prefs);
		
		mInfo = ColumnManager.getInstance(this).getColumnInfo(getIntent().getIntExtra(EXTRA_COLUMN_INDEX, -1));
		
		setTitle(mInfo.getProvider().getName(this));
		
		mNotificationEnabled = (CheckBoxPreference) findPreference("notificationEnabled");
		mSoundEnabled = (CheckBoxPreference) findPreference("notificationSound");
		mVibrateEnabled = (CheckBoxPreference) findPreference("notificationVibrate");
		mLEDEnabled = (CheckBoxPreference) findPreference("notificationLED");
		mRingtonePref = (RingtonePreference) findPreference("notificationRingtone");
		mSyncIntervalPref = (ListPreference) findPreference("syncInterval");
		
		mNotificationEnabled.setChecked(mInfo.isShowNotification());
		mSoundEnabled.setChecked(mInfo.isSound());
		mVibrateEnabled.setChecked(mInfo.isVibrate());
		mLEDEnabled.setChecked(mInfo.isLED());
		
		mRingtonePref.setShowSilent(false);
		mRingtonePref.setRingtoneType(2);
		mRingtonePref.setDefaultValue(mInfo.getRingtoneUri());
		
		mSyncIntervalPref.setValue(mInfo.getSyncInterval() > 0 ? Long.toString(mInfo.getSyncInterval() / 60000) : Long.toString(mInfo.getSyncInterval()));
		mSyncIntervalPref.setOnPreferenceChangeListener(this);
		
		mNotificationEnabled.setOnPreferenceChangeListener(this);
		mSoundEnabled.setOnPreferenceChangeListener(this);
		mVibrateEnabled.setOnPreferenceChangeListener(this);
		mLEDEnabled.setOnPreferenceChangeListener(this);
		mRingtonePref.setOnPreferenceChangeListener(this);
		
		mRingtonePref.setEnabled(mInfo.isShowNotification() && mInfo.isSound());
	}
	
	@Override
	public boolean onPreferenceChange(Preference pPreference, Object pNewValue) {
		boolean v = pNewValue instanceof Boolean ? (Boolean)pNewValue : false;
		if (pPreference == mNotificationEnabled) {
			mInfo.setShowNotification(v);
		} else if (pPreference == mSoundEnabled) {
			mInfo.setSound(v);
		} else if (pPreference == mVibrateEnabled) {
			mInfo.setVibrate(v);
		} else if (pPreference == mLEDEnabled) {
			mInfo.setLED(v);
		} else if (pPreference == mRingtonePref) {
			String ringtoneUri = (String) pNewValue;
			mInfo.setRingtoneUri(ringtoneUri);
		} else if (pPreference == mSyncIntervalPref) {
			long syncIntervalMinutes = Long.parseLong((String)pNewValue);
			mInfo.setSyncInterval(syncIntervalMinutes > 0 ? syncIntervalMinutes * 60000l : syncIntervalMinutes);
			return true;
		}
		mRingtonePref.setEnabled(mInfo.isShowNotification() && mInfo.isSound());
		return true;
	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}
	
}
