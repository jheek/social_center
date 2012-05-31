package com.jldroid.twook.activities;

import android.support.v4.app.Fragment;

import com.jldroid.twook.fragments.ChatsFragment;
import com.jldroid.twook.fragments.ComposeFragment;

public class ComposeActivity extends SingleSherlockFragmentActivity {

	public Fragment createFragment() {
		return new ComposeFragment();
	};
	
}
