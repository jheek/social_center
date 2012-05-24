package com.jldroid.twook.fragments.setup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.jldroid.twook.R;

public class SetupWelcomeFragment extends SherlockFragment {

	@Override
	public View onCreateView(LayoutInflater pInflater, ViewGroup pContainer, Bundle pSavedInstanceState) {
		View v = pInflater.inflate(R.layout.setup_welcome, null);
		return v;
	}
	
}
