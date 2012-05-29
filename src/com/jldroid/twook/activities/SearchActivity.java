package com.jldroid.twook.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.jldroid.twook.R;
import com.jldroid.twook.fragments.SearchFragment;

public class SearchActivity extends SherlockFragmentActivity {

	@Override
	protected void onCreate(Bundle pArg0) {
		super.onCreate(pArg0);
		setContentView(R.layout.fragment_holder);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		Fragment frag = new SearchFragment();
		frag.setArguments(getIntent().getExtras());
		getSupportFragmentManager().beginTransaction()
			.add(R.id.fragmentHolder, frag)
			.commitAllowingStateLoss();
	}
	
}
