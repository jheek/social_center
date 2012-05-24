package com.jldroid.twook.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.jldroid.twook.R;
import com.jldroid.twook.ThemeUtils;
import com.jldroid.twook.model.AccountsManager;
import com.jldroid.twook.model.IAccount;
import com.jldroid.twook.model.ImageManager;
import com.jldroid.twook.model.SyncManager;
import com.jldroid.twook.model.twitter.TwitterAccount;

public class PrefsActivity extends SherlockPreferenceActivity {
	
	@Override
	protected void onCreate(Bundle pSavedInstanceState) {
		ThemeUtils.setupActivityTheme(this);
		super.onCreate(pSavedInstanceState);
		
		addPreferencesFromResource(R.xml.prefs);
		findPreference("columns").setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference pPreference) {
				startActivity(new Intent(getApplicationContext(), ColumnsActivity.class));
				return true;
			}
		});
		findPreference("deleteCache").setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference pPreference) {
				ImageManager.getInstance(getApplicationContext()).deleteAll();
				AccountsManager am = AccountsManager.getInstance(getApplicationContext());
				final int l = am.getTwitterAccountCount();
				for (int i = 0; i < l; i++) {
					am.getTwitterAccount(i).deleteSavedMessages();
				}
				final int l2 = am.getFacebookAccountCount();
				for (int i = 0; i < l2; i++) {
					am.getFacebookAccount(i).deleteSavedMessages();
				}
				return true;
			}
		});
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		ListPreference defaultSyncIntervalPref = (ListPreference) findPreference("defaultSyncInterval");
		defaultSyncIntervalPref.setValue(String.valueOf(prefs.getLong("defaultSyncInterval", -1)));
		defaultSyncIntervalPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference pPreference, Object pNewValue) {
				pPreference.getPreferenceManager().getSharedPreferences().edit()
					.putLong(pPreference.getKey(), Long.parseLong((String) pNewValue))
					.commit();
				SyncManager.updateColumnsSync(getApplicationContext());
				return true;
			}
		});
		ListPreference defaultNotificationPref = (ListPreference) findPreference("defaultNotification");
		defaultNotificationPref.setValue(String.valueOf(prefs.getInt("defaultNotification", 0)));
		defaultNotificationPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference pPreference, Object pNewValue) {
				pPreference.getPreferenceManager().getSharedPreferences().edit()
					.putInt(pPreference.getKey(), Integer.parseInt((String) pNewValue))
					.commit();
				return true;
			}
		});
		/*ListPreference themePref = (ListPreference) findPreference("theme");
		themePref.setValue(ThemeUtils.getCurrentTheme());
		themePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference pPreference, Object pNewValue) {
				ThemeUtils.setTheme(getApplicationContext(), (String) pNewValue);
				return true;
			}
		});*/
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Preference addTwitterPref = findPreference("addTwitterAccount");
		Preference addFacebookPref = findPreference("addFacebookAccount");
		addTwitterPref.setIntent(new Intent(getApplicationContext(), AddTwitterAccountActivity.class));
		addFacebookPref.setIntent(new Intent(getApplicationContext(), AddFacebookAccountActivity.class));
		
		AccountsManager am = AccountsManager.getInstance(this);
		PreferenceCategory twitterAccountsHolder = (PreferenceCategory) findPreference("twitterAccounts");
		PreferenceCategory facebookAccountsHolder = (PreferenceCategory) findPreference("facebookAccounts");
		twitterAccountsHolder.removeAll();
		facebookAccountsHolder.removeAll();
		twitterAccountsHolder.addPreference(addTwitterPref);
		facebookAccountsHolder.addPreference(addFacebookPref);
		
		for (int i = 0; i < am.getAccountCount(); i++) {
			IAccount account = am.getAccount(i);
			int type = account instanceof TwitterAccount ? AccountPrefsActivity.ACCOUNT_TYPE_TWITTER : AccountPrefsActivity.ACCOUNT_TYPE_FACEBOOK;
			PreferenceCategory holder = account instanceof TwitterAccount ? twitterAccountsHolder : facebookAccountsHolder;
			Preference pref = new Preference(this);
			pref.setTitle(account.getUser().name);
			pref.setOrder(1);
			holder.addPreference(pref);
			pref.setIntent(new Intent(getApplicationContext(), AccountPrefsActivity.class)
				.putExtra(AccountPrefsActivity.EXTRA_ACCOUNT_TYPE, type)
				.putExtra(AccountPrefsActivity.EXTRA_ACCOUNT_ID, account.getUser().id));
		}
	}
}
