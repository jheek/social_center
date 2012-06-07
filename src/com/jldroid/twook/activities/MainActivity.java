package com.jldroid.twook.activities;

import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Images.Media;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.widget.ArrayAdapter;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.jldroid.twook.Globals;
import com.jldroid.twook.R;
import com.jldroid.twook.ThemeUtils;
import com.jldroid.twook.fragments.ChatFragment;
import com.jldroid.twook.fragments.ChatsFragment;
import com.jldroid.twook.fragments.ComposeFragment;
import com.jldroid.twook.fragments.ComposeFragment.ComposeConfig;
import com.jldroid.twook.fragments.ComposeFragment.ComposeMode;
import com.jldroid.twook.fragments.DetailsFragment;
import com.jldroid.twook.fragments.MainPhoneFragment;
import com.jldroid.twook.fragments.PeopleFragment;
import com.jldroid.twook.model.AccountsManager;
import com.jldroid.twook.model.Chat;
import com.jldroid.twook.model.ColumnInfo;
import com.jldroid.twook.model.ColumnManager;
import com.jldroid.twook.model.IAccount;
import com.jldroid.twook.model.ImageManager;
import com.jldroid.twook.model.Message;
import com.jldroid.twook.model.SyncManager;
import java.util.ArrayList;
import com.jdroid.utils.SortedArrayList;
import com.jdroid.utils.StorageManager;

public class MainActivity extends SherlockFragmentActivity {
	
	public static final String EXTRA_COLUMN = "com.jldroid.twook.COLUMN";
	public static final String EXTRA_MESSAGE_ID = "com.jldroid.twook.ID";
	public static final String EXTRA_MESSAGE_TYPE = "com.jldroid.twook.TYPE";
	public static final String EXTRA_ACCOUNT = "com.jldroid.twook.ACCOUNT";
	public static final String EXTRA_CHAT = "com.jldroid.twook.CHAT";
	
	public static final int REQUEST_CODE_COMPOSE_PICK_IMAGE = 1000;
	public static final int REQUEST_CODE_SETUP = 1001;
	
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
	protected void onActivityResult(int pRequestCode, int pResultCode, Intent pData) {
		switch (pRequestCode) {
		case REQUEST_CODE_COMPOSE_PICK_IMAGE:
			ComposeFragment composeFragment = (ComposeFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentHolder);
			if (composeFragment != null)
				composeFragment.onPickImageResult(pResultCode, pData);
			break;
		default:
			break;
		}
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
	}
}