package com.jldroid.twook.activities;

import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
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

public class MainActivity extends SherlockFragmentActivity implements OnBackStackChangedListener {
	
	public static final String EXTRA_COLUMN = "com.jldroid.twook.COLUMN";
	public static final String EXTRA_MESSAGE_ID = "com.jldroid.twook.ID";
	public static final String EXTRA_MESSAGE_TYPE = "com.jldroid.twook.TYPE";
	public static final String EXTRA_ACCOUNT = "com.jldroid.twook.ACCOUNT";
	public static final String EXTRA_CHAT = "com.jldroid.twook.CHAT";
	
	public static final int REQUEST_CODE_COMPOSE_PICK_IMAGE = 1000;
	public static final int REQUEST_CODE_SETUP = 1001;
	
	private SharedPreferences mPrefs;
	
	private int mCurrentRootID = 0;
	
	private boolean isStarted = false;
	
	@Override
	protected void onCreate(Bundle pArg0) {
		ThemeUtils.setupActivityTheme(this);
		super.onCreate(null);
		
		int numAccounts = AccountsManager.getInstance(getApplicationContext()).getAccountCount();
		if (numAccounts == 0) {
			showSetup();
		}
		
		setContentView(R.layout.main);
		getSupportFragmentManager().addOnBackStackChangedListener(this);
		onBackStackChanged();
		
		mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		
		handleIntent(getIntent());
		
		SyncManager.validateSync(getApplicationContext());
	}
	
	private void handleIntent(Intent intent) {
		showRootFragment(new MainPhoneFragment(), false);
		if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_SEND)) {
			Uri uri = (Uri) intent.getExtras().get(Intent.EXTRA_STREAM);
			Cursor cursor = Media.query(getContentResolver(), uri, null);
			cursor.moveToFirst();
			String path = cursor.getString(cursor.getColumnIndex(Media.DATA));
			showFragment(new ComposeFragment(new ComposeConfig(ComposeMode.STATUS_UPDATE, path)), false);
		} else if (intent.hasExtra(EXTRA_COLUMN)) {
			Fragment frag = getSupportFragmentManager().findFragmentById(R.id.fragmentHolder);
			if (frag instanceof MainPhoneFragment) {
				((MainPhoneFragment) frag).setColumn(getApplicationContext(), intent.getStringExtra(EXTRA_COLUMN));
			}
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle pOutState) {
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle pSavedInstanceState) {
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		ColumnManager.getInstance(getApplicationContext()).setInForeground(true);
		AccountsManager.getInstance(getApplicationContext()).setAvailable(true);
		
		isStarted = true;
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		isStarted = false;
		ColumnManager.getInstance(this).setInForeground(false);
		AccountsManager.getInstance(getApplicationContext()).setAvailable(false);
		if (mPrefs.getBoolean("clearCacheWhenPause", false)) {
			Globals.cleanup(getApplicationContext(), true);
		}
		ImageManager.getInstance(getApplicationContext()).flush();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem pItem) {
		if (pItem.getItemId() == android.R.id.home) {
			getSupportFragmentManager().popBackStack();
			return true;
		}
		return super.onOptionsItemSelected(pItem);
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
	
	public void showSetup() {
		finish();
		startActivity(new Intent(getApplicationContext(), SetupActivity.class));
	}
	
	public void showSettings() {
		startActivity(new Intent(getApplicationContext(), PrefsActivity.class));
	}
	
	public void showFragment(Fragment fragment) {
		showFragment(fragment, true);
	}
	
	public void showFragment(Fragment fragment, boolean animate) {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.replace(R.id.fragmentHolder, fragment);
		ft.addToBackStack(null);
		ft.setTransition(animate ? FragmentTransaction.TRANSIT_FRAGMENT_OPEN : FragmentTransaction.TRANSIT_NONE);
		ft.commitAllowingStateLoss();
		getSupportFragmentManager().executePendingTransactions();
	}
	
	public void showRootFragment(Fragment fragment, boolean animate) {
		getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.replace(R.id.fragmentHolder, fragment);
		ft.setTransition(animate ? FragmentTransaction.TRANSIT_FRAGMENT_OPEN : FragmentTransaction.TRANSIT_NONE);
		ft.commitAllowingStateLoss();
	}
	
	@Override
	public void onBackStackChanged() {
		int flags = getSupportFragmentManager().getBackStackEntryCount() > 0 ? ActionBar.DISPLAY_HOME_AS_UP : 0;
		getSupportActionBar().setDisplayOptions(flags, ActionBar.DISPLAY_HOME_AS_UP);
	}
}