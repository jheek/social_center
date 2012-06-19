package com.jldroid.twook.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

public abstract class SingleSherlockFragmentActivity extends SherlockFragmentActivity {

	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		if (arg0 == null) {
			Fragment frag = createFragment();
			frag.setArguments(getIntent().getExtras());
			getSupportFragmentManager().beginTransaction()
				.add(android.R.id.content, frag)
				.commit();
		}
	};
	
	@Override
	public boolean onOptionsItemSelected(MenuItem pItem) {
		if (pItem.getItemId() == android.R.id.home) {
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(pItem);
	}
	
	public abstract Fragment createFragment();
	
	public Fragment getFragment() {
		return getSupportFragmentManager().findFragmentById(android.R.id.content);
	}
	
}
