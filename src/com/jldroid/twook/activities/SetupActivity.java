package com.jldroid.twook.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.jldroid.twook.R;
import com.jldroid.twook.ThemeUtils;
import com.jldroid.twook.fragments.setup.ISetupFragment;
import com.jldroid.twook.fragments.setup.SetupAccountsFragment;
import com.jldroid.twook.fragments.setup.SetupColumnModeFragment;
import com.jldroid.twook.fragments.setup.SetupNotificationsFragment;
import com.jldroid.twook.fragments.setup.SetupUpdateIntervalFragment;
import com.jldroid.twook.fragments.setup.SetupWelcomeFragment;

public class SetupActivity extends SherlockFragmentActivity implements OnClickListener {

	private Fragment[] mSetupFragments = {	
			new SetupWelcomeFragment(),
			new SetupAccountsFragment(), 
			new SetupUpdateIntervalFragment(),
			new SetupNotificationsFragment(),
			new SetupColumnModeFragment()};
	
	private int mCurrentPos = -1;
	
	private Button mPrevBtn;
	private Button mNextBtn;
	
	@Override
	protected void onCreate(Bundle pArg0) {
		ThemeUtils.setupActivityTheme(this);
		super.onCreate(null);
		setContentView(R.layout.setup);
		
		mPrevBtn = (Button) findViewById(R.id.prevBtn);
		mNextBtn = (Button) findViewById(R.id.nextBtn);
		
		mNextBtn.setOnClickListener(this);
		mPrevBtn.setOnClickListener(this);
		
		next();
	}
	
	@Override
	public void onClick(View pV) {
		if (pV == mPrevBtn) {
			mCurrentPos--;
			onBackPressed();
		} else if (pV == mNextBtn) {
			next();
		}
	}
	
	private void next() {
		if (mCurrentPos >= 0 && mSetupFragments[mCurrentPos] instanceof ISetupFragment) {
			ISetupFragment setupFrag = (ISetupFragment) mSetupFragments[mCurrentPos];
			if (setupFrag.isProceedAllowed()) {
				setupFrag.onProceed();
			} else {
				return;
			}
		}
		mCurrentPos++;
		if (mCurrentPos == mSetupFragments.length) {
			finish();
			startActivity(new Intent(getApplicationContext(), MainActivity.class));
			return;
		}
		mNextBtn.setText(mCurrentPos + 1 == mSetupFragments.length ? R.string.setup_finish : R.string.setup_next);
		mPrevBtn.setEnabled(mCurrentPos > 0);
		Fragment frag = mSetupFragments[mCurrentPos];
		showFragment(frag, mCurrentPos > 0, mCurrentPos > 0);
	}
	
	private void showFragment(Fragment fragment, boolean animate, boolean addToBackstack) {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.replace(R.id.setupHolder, fragment);
		if (addToBackstack) ft.addToBackStack(null);
		ft.setTransition(animate ? FragmentTransaction.TRANSIT_FRAGMENT_OPEN : FragmentTransaction.TRANSIT_NONE);
		ft.commitAllowingStateLoss();
		getSupportFragmentManager().executePendingTransactions();
	}
	
}
