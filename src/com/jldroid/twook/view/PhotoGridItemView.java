package com.jldroid.twook.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import com.jdroid.utils.Threads;
import com.jldroid.twook.model.ImageManager;
import com.jldroid.twook.model.ImageManager.DeletionTrigger;
import com.jldroid.twook.model.ImageManager.LoadBitmapCallback;
import com.jldroid.twook.model.facebook.Photo;

public class PhotoGridItemView extends ImageView implements LoadBitmapCallback {
	
	private Photo mPhoto;
	private Bitmap mBmd;
	
	public PhotoGridItemView(Context c) {
		super(c);
		
		setScaleType(ScaleType.CENTER_CROP);
	}
	
	public Photo getPhoto() {
		return mPhoto;
	}
	
	public void setPhoto(Photo pPhoto) {
		mPhoto = pPhoto;
		String uri = pPhoto.src;
		ImageManager im = ImageManager.getInstance(getContext());
		mBmd = im.peekImage(uri);
		if (mBmd != null) {
			setImageBitmap(mBmd);
		} else {
			setImageDrawable(null);
			im.loadImage(this, uri, DeletionTrigger.IMMEDIATELY, ImageManager.REF_SOFT, (int) (getResources().getDisplayMetrics().density * 100), -1, -1);
		}
	}
	
	@Override
	public void onBitmapLoaded(final String pUri, final Bitmap pBmd) {
		Threads.runOnUIThread(new Runnable() {
			@Override
			public void run() {
				if (mPhoto.src.equals(pUri)) {
					setImageBitmap(mBmd = pBmd);
				}
			}
		});
	}
	
	@Override
	public void onFailed(String pUri) {
		
	}
	
}
