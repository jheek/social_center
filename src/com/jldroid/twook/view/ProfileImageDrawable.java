package com.jldroid.twook.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import com.jdroid.utils.Threads;
import com.jldroid.twook.R;
import com.jldroid.twook.model.ImageManager;
import com.jldroid.twook.model.ImageManager.DeletionTrigger;
import com.jldroid.twook.model.ImageManager.LoadBitmapCallback;
import com.jldroid.twook.model.User;

public class ProfileImageDrawable extends Drawable implements LoadBitmapCallback, Runnable {

	private static final long ANIM_DURATION = 300;
	
	private static Bitmap sTwitterIcon;
	private static Bitmap sFacebookIcon;
	
	private Drawable mLoadingDrawable;
	private User mUser;
	private String mUri;
	private Bitmap mBmd;
	private Paint mPaint = new Paint();
	
	private long mLoadCompleteTime = -1;
	
	public ProfileImageDrawable(Context c) {
		if (sTwitterIcon == null) {
			sTwitterIcon = ((BitmapDrawable) c.getResources().getDrawable(R.drawable.twitter_icon)).getBitmap();
			sFacebookIcon = ((BitmapDrawable) c.getResources().getDrawable(R.drawable.facebook_icon)).getBitmap();
		}
		mLoadingDrawable = c.getResources().getDrawable(R.drawable.no_profileimg_img);
	}
	
	public User getUser() {
		return mUser;
	}
	
	public String getUri() {
		return mUri;
	}
	
	public void setUser(Context c, User pUser, boolean large) {
		mUser = pUser;
		mUri = large ? pUser.largeProfilePictureUrl : pUser.profilePictureUrl;
		mBmd = null;
		mLoadCompleteTime = -1;
		mPaint.setAlpha(255);
		ImageManager im = ImageManager.getInstance(c);
		Bitmap bmd = im.peekImage(mUri);
		if (bmd != null) {
			mBmd = bmd;
		} else {
			im.loadProfilePicture(this, mUri, DeletionTrigger.AFTER_ONE_WEEK_UNUSED);
		}
		invalidateSelf();
	}
	
	@Override
	public void draw(Canvas pCanvas) {
		if (mBmd != null) {
			Rect bounds = getBounds();
			pCanvas.drawBitmap(mBmd, null, getBounds(), mPaint);
			Bitmap icon = mUser.type == User.TYPE_FACEBOOK ? sFacebookIcon : sTwitterIcon;
			pCanvas.drawBitmap(icon, 0, bounds.bottom - icon.getHeight(), mPaint);
		} else {
			mLoadingDrawable.draw(pCanvas);
		}
	}

	@Override
	public void setAlpha(int pAlpha) {
		mPaint.setAlpha(pAlpha);
	}

	@Override
	public void setColorFilter(ColorFilter pCf) {
		mPaint.setColorFilter(pCf);
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}
	
	@Override
	public void setBounds(int pLeft, int pTop, int pRight, int pBottom) {
		super.setBounds(pLeft, pTop, pRight, pBottom);
		mLoadingDrawable.setBounds(pLeft, pTop, pRight, pBottom);
	}

	@Override
	public int getIntrinsicWidth() {
		return mBmd != null ? mBmd.getWidth() : -1;
	}
	
	@Override
	public int getIntrinsicHeight() {
		return mBmd != null ? mBmd.getHeight() : -1;
	}
	
	@Override
	public void run() {
		if (mLoadCompleteTime != -1) {
			long dif = SystemClock.elapsedRealtime() - mLoadCompleteTime;
			if (dif < ANIM_DURATION) {
				mPaint.setAlpha((int) ((float) dif / (float) ANIM_DURATION * 255));
				Threads.getUIHandler().postDelayed(this, 15);
			} else {
				mPaint.setAlpha(255);
			}
			invalidateSelf();
			
		}
	}
	
	@Override
	public void onBitmapLoaded(final String pUri, final Bitmap pBmd) {
		Threads.runOnUIThread(new Runnable() {
			@Override
			public void run() {
				if (mUri.equals(pUri)) {
					mBmd = pBmd;
					mLoadCompleteTime = SystemClock.elapsedRealtime();
					Threads.runOnUIThread(ProfileImageDrawable.this);
					invalidateSelf();
				}
			}
		});
	}

	@Override
	public void onFailed(String pUri) {
	}

}
