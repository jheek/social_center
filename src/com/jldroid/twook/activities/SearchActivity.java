package com.jldroid.twook.activities;

import android.support.v4.app.Fragment;

import com.jldroid.twook.fragments.SearchFragment;

public class SearchActivity extends SingleSherlockFragmentActivity {

	public Fragment createFragment() {
		return new SearchFragment();
	};
	
}
