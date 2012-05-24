package com.jldroid.twook.activities;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.jdroid.colorpicker.AmbilWarnaDialog;
import com.jdroid.colorpicker.AmbilWarnaDialog.OnAmbilWarnaListener;
import com.jldroid.twook.R;
import com.jldroid.twook.ThemeUtils;
import com.jldroid.twook.model.AccountsManager;
import com.jldroid.twook.model.IAccount;
import com.jldroid.twook.model.facebook.FacebookAccount;
import com.jldroid.twook.model.twitter.TwitterAccount;

public class AccountPrefsActivity extends SherlockPreferenceActivity {
	
	public static final int ACCOUNT_TYPE_TWITTER = 0;
	public static final int ACCOUNT_TYPE_FACEBOOK = 1;
	
	public static final String EXTRA_ACCOUNT_TYPE = "com.jldroid.twook.ACCOUNT_TYPE";
	public static final String EXTRA_ACCOUNT_ID = "com.jldroid.twook.ACCOUNT_ID";
	
	protected AccountsManager mAM;
	protected IAccount mAccount;
	
	@Override
	protected void onCreate(Bundle pSavedInstanceState) {
		ThemeUtils.setupActivityTheme(this);
		super.onCreate(pSavedInstanceState);
		
		mAM = AccountsManager.getInstance(this);
		addPreferencesFromResource(R.xml.account_specific_prefs);
		
		final int type = getIntent().getIntExtra(EXTRA_ACCOUNT_TYPE, -1);
		final long id = getIntent().getLongExtra(EXTRA_ACCOUNT_ID, -1);
		
		switch (type) {
		case ACCOUNT_TYPE_TWITTER:
			mAccount = mAM.findTwitterAccountByID(id);
			break;
		case ACCOUNT_TYPE_FACEBOOK:
			mAccount = mAM.findFacebookAccountByID(id);
			break;
		}
		Preference deletePref = findPreference("deleteAccount");
		deletePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference pPreference) {
				switch (type) {
				case ACCOUNT_TYPE_TWITTER:
					mAM.removeTwitterAccount((TwitterAccount) mAccount);
					break;
				case ACCOUNT_TYPE_FACEBOOK:
					mAM.removeFacebookAccount((FacebookAccount) mAccount);
					break;
				}
				finish();
				return true;
			}
		});
		Preference colorPref = findPreference("color");
		colorPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference pPreference) {
				AmbilWarnaDialog dialog = new AmbilWarnaDialog(AccountPrefsActivity.this, mAccount.getColor(), new OnAmbilWarnaListener() {
					@Override
					public void onOk(AmbilWarnaDialog pDialog, int pColor) {
						mAccount.setColor(pColor);
					}
					@Override
					public void onCancel(AmbilWarnaDialog pDialog) {
					}
				});
				dialog.show();
				return true;
			}
		});
	}
}
