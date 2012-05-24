package com.jldroid.twook.activities;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.jldroid.twook.fragments.DetailsFragment;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

public class DetailsActivity extends SherlockFragmentActivity {

	
	@Override
	protected void onCreate(Bundle pArg0) {
		super.onCreate(pArg0);
		if (pArg0 == null) {
			DetailsFragment frag = new DetailsFragment();
			frag.setArguments(getIntent().getExtras());
			getSupportFragmentManager().beginTransaction()
				.add(android.R.id.content, frag)
				.commit();
		}
	}
	
}
