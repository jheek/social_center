package com.jldroid.twook.view;

import android.graphics.drawable.ColorDrawable;
import android.os.SystemClock;

import com.jdroid.utils.Threads;

public class ColoredFadeoutDrawable extends ColorDrawable {
	
	private static final int FADEOUT_INTERVAL = 5;
	private static final float FADOUT_DURATION = 250;
	
	protected long mFadeoutStartTime = -1;
	protected boolean mAnimating = false;
	
	private Runnable mFadeoutRunnable = new Runnable() {
		@Override
		public void run() {
			int newAlpha = 255 - (int) ((float)(SystemClock.elapsedRealtime() - mFadeoutStartTime) / FADOUT_DURATION * 255f);
			if (newAlpha <= 0) {
				setAlpha(0);
				mAnimating = false;
			} else {
				setAlpha(newAlpha);
				Threads.getUIHandler().postDelayed(mFadeoutRunnable, FADEOUT_INTERVAL);
			}
			invalidateSelf();
		}
	};
	
	public ColoredFadeoutDrawable(int color) {
		super(color);
		setAlpha(0);
	}
	
	public void startFadeout() {
		mAnimating = true;
		mFadeoutStartTime = SystemClock.elapsedRealtime();
		setAlpha(255);
		Threads.runOnUIThread(mFadeoutRunnable);
		invalidateSelf();
	}
	
	public void stopFadeout() {
		mAnimating = false;
		setAlpha(0);
		Threads.getUIHandler().removeCallbacks(mFadeoutRunnable);
		invalidateSelf();
	}
	
	@Override
	public boolean isStateful() {
		return true;
	}
	
	@Override
	protected boolean onStateChange(int[] pState) {
		for (int i = 0; i < pState.length; i++) {
			int state = pState[i];
			if (state == android.R.attr.state_pressed && !mAnimating) {
				setAlpha(255);
				invalidateSelf();
				return true;
			}
		}
		if (!mAnimating) {
			setAlpha(0);
			invalidateSelf();
			return true;
		}
		return super.onStateChange(pState);
	}
	
	public boolean isAnimating() {
		return mAnimating;
	}
}
