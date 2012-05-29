package com.jldroid.twook.activities;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.jldroid.twook.fragments.ComposeFragment;

public class ComposeActivity extends SherlockFragmentActivity {

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		if (arg0 != null) {
			ComposeFragment frag = new ComposeFragment();
			frag.setArguments(getIntent().getExtras());
			getSupportFragmentManager().beginTransaction()
				.add(android.R.id.content, frag)
				.commit();
		}
	}
	
}
