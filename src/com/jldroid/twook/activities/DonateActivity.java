package com.jldroid.twook.activities;

import android.support.v4.app.Fragment;

import com.jldroid.twook.fragments.DonateFragment;

public class DonateActivity extends SingleSherlockFragmentActivity {

	public Fragment createFragment() {
		return new DonateFragment();
	};
	
}
