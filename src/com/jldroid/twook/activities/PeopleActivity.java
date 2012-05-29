package com.jldroid.twook.activities;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.jldroid.twook.fragments.PeopleFragment;

public class PeopleActivity extends SherlockFragmentActivity {

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		if (arg0 != null) {
			PeopleFragment frag = new PeopleFragment();
			frag.setArguments(getIntent().getExtras());
			getSupportFragmentManager().beginTransaction()
				.add(android.R.id.content, frag)
				.commit();
		}
	}
	
}
