package com.jldroid.twook;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import com.jdroid.utils.Threads;

public class FastBitmapDrawable extends Drawable implements Runnable {

	private static final long ANIM_DURATION = 300;
	
	private static Paint sPaint = new Paint();
	
	private Bitmap mBmd;
	private int mAlpha = 255;
	
	private long mTime = -1;
	
	public FastBitmapDrawable() {
		
	}
	
	public FastBitmapDrawable(Bitmap bmd) {
		setBitmap(bmd, false);
	}
	
	public Bitmap getBitmap() {
		return mBmd;
	}
	
	public void setBitmap(Bitmap pBmd, boolean animate) {
		mBmd = pBmd;
		mTime = animate ? SystemClock.elapsedRealtime() : -1;
		if (animate) {
			Threads.runOnUIThread(this);
		}
		invalidateSelf();
	}
	
	public int getAlpha() {
		return mAlpha;
	}
	
	@Override
	public void setAlpha(int pAlpha) {
		mAlpha = pAlpha;
		invalidateSelf();
	}
	
	@Override
	public void draw(Canvas pCanvas) {
		if (mBmd != null) {
			sPaint.setAlpha(mAlpha);
			pCanvas.drawBitmap(mBmd, null, getBounds(), mAlpha != 255 ? sPaint : null);
		}
	}
	@Override
	public void setColorFilter(ColorFilter pCf) {
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
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
		if (mTime != -1) {
			long dif = SystemClock.elapsedRealtime() - mTime;
			if (dif < ANIM_DURATION) {
				mAlpha = (int) ((float) dif / (float) ANIM_DURATION * 255);
				Threads.getUIHandler().postDelayed(this, 15);
			} else {
				mAlpha = 255;
			}
			invalidateSelf();
		}
	}

}
