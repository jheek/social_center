package com.jldroid.twook.activities;

import android.support.v4.app.Fragment;

import com.jldroid.twook.fragments.ChatFragment;

public class ChatActivity extends SingleSherlockFragmentActivity {

	public Fragment createFragment() {
		return new ChatFragment();
	};
	
}
