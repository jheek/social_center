package com.jldroid.twook.activities;

import android.support.v4.app.Fragment;

import com.jldroid.twook.fragments.ChatsFragment;

public class ChatsActivity extends SingleSherlockFragmentActivity {

	public Fragment createFragment() {
		return new ChatsFragment();
	};
	
}
