package com.jldroid.twook.activities;

import android.support.v4.app.Fragment;

import com.jldroid.twook.fragments.PeopleFragment;

public class PeopleActivity extends SingleSherlockFragmentActivity {

	public Fragment createFragment() {
		return new PeopleFragment();
	};
	
}
