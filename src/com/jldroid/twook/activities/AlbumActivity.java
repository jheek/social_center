package com.jldroid.twook.activities;

import android.support.v4.app.Fragment;

import com.jldroid.twook.fragments.AlbumFragment;

public class AlbumActivity extends SingleSherlockFragmentActivity {

	public Fragment createFragment() {
		return new AlbumFragment();
	};
	
}
