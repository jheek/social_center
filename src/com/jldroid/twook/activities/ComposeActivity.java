package com.jldroid.twook.activities;

import android.content.Intent;
import android.support.v4.app.Fragment;

import com.jldroid.twook.R;
import com.jldroid.twook.fragments.ChatsFragment;
import com.jldroid.twook.fragments.ComposeFragment;

public class ComposeActivity extends SingleSherlockFragmentActivity {

	public static final int REQUEST_CODE_COMPOSE_PICK_IMAGE = 1000;
	
	public Fragment createFragment() {
		return new ComposeFragment();
	}
	
	@Override
	protected void onActivityResult(int pRequestCode, int pResultCode, Intent pData) {
		switch (pRequestCode) {
		case REQUEST_CODE_COMPOSE_PICK_IMAGE:
			ComposeFragment composeFragment = (ComposeFragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
			if (composeFragment != null)
				composeFragment.onPickImageResult(pResultCode, pData);
			break;
		default:
			break;
		}
	}
	
}
