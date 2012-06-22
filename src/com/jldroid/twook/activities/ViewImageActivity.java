package com.jldroid.twook.activities;

import com.jldroid.twook.fragments.ViewImageFragment;

import android.support.v4.app.Fragment;

public class ViewImageActivity extends SingleSherlockFragmentActivity {

	@Override
	public Fragment createFragment() {
		return new ViewImageFragment();
	}

}
