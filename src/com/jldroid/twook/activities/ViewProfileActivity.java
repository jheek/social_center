package com.jldroid.twook.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.jldroid.twook.fragments.ChatsFragment;
import com.jldroid.twook.fragments.ViewProfileFragment;

public class ViewProfileActivity extends SingleSherlockFragmentActivity {

	public Fragment createFragment() {
		return new ViewProfileFragment();
	};
	
}
