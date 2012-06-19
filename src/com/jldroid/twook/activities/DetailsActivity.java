package com.jldroid.twook.activities;


import android.support.v4.app.Fragment;

import com.jldroid.twook.fragments.DetailsFragment;

public class DetailsActivity extends SingleSherlockFragmentActivity {

	public Fragment createFragment() {
		return new DetailsFragment();
	};
	
	@Override
	public void onBackPressed() {
		DetailsFragment frag = (DetailsFragment) getFragment();
		if (!frag.onBackPressed()) {
			super.onBackPressed();
		}
	}
	
}
