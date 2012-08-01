package com.jldroid.twook.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jdroid.utils.Threads;
import com.jldroid.twook.R;
import com.jldroid.twook.model.ImageManager;
import com.jldroid.twook.model.ImageManager.DeletionTrigger;
import com.jldroid.twook.model.ImageManager.LoadBitmapCallback;
import com.jldroid.twook.model.facebook.Album;
import com.jldroid.twook.model.facebook.FacebookAccount;

public class AlbumListItemView extends RelativeLayout implements LoadBitmapCallback {

	private ImageView mPreviewIV;
	
	private TextView mTitleTV;
	private TextView mInfoTV;
	
	private FacebookAccount mFBAccount;
	private Album mAlbum;
	
	public AlbumListItemView(Context pContext) {
		super(pContext);
		LayoutInflater.from(pContext).inflate(R.layout.album_list_item, this);
		mPreviewIV = (ImageView) findViewById(R.id.albumPreviewIV);
		mTitleTV = (TextView) findViewById(R.id.titleTV);
		mInfoTV = (TextView) findViewById(R.id.infoTV);
	}
	
	public Album getAlbum() {
		return mAlbum;
	}
	
	public void setAlbum(FacebookAccount fbAccount, Album pAlbum) {
		mFBAccount = fbAccount;
		mAlbum = pAlbum;
		mTitleTV.setText(pAlbum.name);
		mInfoTV.setText(pAlbum.photoCount + " photos");
		
		ImageManager im = ImageManager.getInstance(getContext());
		String uri = pAlbum.getCoverPictureUri(fbAccount);
		Bitmap bmd = im.peekImage(uri);
		if (bmd != null) {
			mPreviewIV.setImageBitmap(bmd);
		} else {
			mPreviewIV.setImageResource(R.drawable.album_image);
			im.loadImage(this, uri, DeletionTrigger.AFTER_ONE_DAY_UNUSED, -1, -1, 50);
		}
	}
	
	@Override
	public void onBitmapLoaded(final String pUri, final Bitmap pBmd) {
		Threads.runOnUIThread(new Runnable() {
			@Override
			public void run() {
				if (pUri.equals(mAlbum.getCoverPictureUri(mFBAccount))) {
					mPreviewIV.setImageBitmap(pBmd);
				}
			}
		});
	}
	@Override
	public void onFailed(String pUri) {
		
	}
	
}
