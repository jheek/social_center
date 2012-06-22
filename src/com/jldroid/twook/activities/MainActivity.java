package com.jldroid.twook.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.jdroid.utils.StorageManager;
import com.jldroid.twook.Globals;
import com.jldroid.twook.ThemeUtils;
import com.jldroid.twook.fragments.MainPhoneFragment;
import com.jldroid.twook.model.AccountsManager;
import com.jldroid.twook.model.ColumnManager;
import com.jldroid.twook.model.ImageManager;
import com.jldroid.twook.model.SyncManager;

public class MainActivity extends SherlockFragmentActivity {
	
	public static final String EXTRA_COLUMN = "com.jldroid.twook.COLUMN";
	
	private SharedPreferences mPrefs;
	
	private MainPhoneFragment mFrag;
	
	@Override
	protected void onCreate(Bundle pArg0) {
		ThemeUtils.setupActivityTheme(this);
		super.onCreate(null);
		
		int numAccounts = AccountsManager.getInstance(getApplicationContext()).getAccountCount();
		if (numAccounts == 0) {
			finish();
			startActivity(new Intent(getApplicationContext(), SetupActivity.class));
			return;
		}
		
		mFrag = new MainPhoneFragment();
		mFrag.setArguments(getIntent().getExtras());
		
		getSupportFragmentManager().beginTransaction()
			.add(android.R.id.content, mFrag)
			.commit();
		
		mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		
		SyncManager.validateSync(getApplicationContext());
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		if (intent.hasExtra(EXTRA_COLUMN)) {
			mFrag.setColumn(getApplicationContext(), intent.getStringExtra(EXTRA_COLUMN));
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		ColumnManager.getInstance(getApplicationContext()).setInForeground(true);
		AccountsManager.getInstance(getApplicationContext()).setAvailable(true);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		ColumnManager.getInstance(this).setInForeground(false);
		AccountsManager.getInstance(getApplicationContext()).setAvailable(false);
		if (mPrefs.getBoolean("clearCacheWhenPause", false)) {
			Globals.cleanup(getApplicationContext(), true);
		}
		ImageManager.getInstance(getApplicationContext()).flush();
		StorageManager.getDeflaut(getApplicationContext()).flushAsync();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
	}
}