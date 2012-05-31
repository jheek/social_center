package com.jldroid.twook.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.jldroid.twook.fragments.AlbumFragment;
import com.jldroid.twook.fragments.ChatFragment;

public class ChatActivity extends SingleSherlockFragmentActivity {

	public Fragment createFragment() {
		return new ChatFragment();
	};
	
}
