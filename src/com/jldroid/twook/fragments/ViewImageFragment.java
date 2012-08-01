package com.jldroid.twook.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.jdroid.utils.Threads;
import com.jldroid.twook.R;
import com.jldroid.twook.model.ImageManager;
import com.jldroid.twook.model.ImageManager.DeletionTrigger;
import com.jldroid.twook.model.ImageManager.LoadBitmapCallback;

public class ViewImageFragment extends SherlockFragment implements LoadBitmapCallback {
	
	
	public static final String EXTRA_IMG_URL = "com.jldroid.twook.IMG_URL";

	private ImageView mImageView;
	private ProgressBar mPB;
	
	private String mUrl;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mUrl = getArguments().getString(EXTRA_IMG_URL);
	}
	
	@Override
	public View onCreateView(LayoutInflater pInflater, ViewGroup pContainer, Bundle pSavedInstanceState) {
		View v = pInflater.inflate(R.layout.view_image, null);
		mImageView = (ImageView) v.findViewById(R.id.imageView);
		mPB = (ProgressBar) v.findViewById(R.id.pb);
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle pSavedInstanceState) {
		super.onActivityCreated(pSavedInstanceState);
		ImageManager im = ImageManager.getInstance(getActivity());
		im.loadImage(this, mUrl, DeletionTrigger.IMMEDIATELY, -1, -1, 100);
	}
	
	@Override
	public void onBitmapLoaded(String uri, final Bitmap bmd) {
		Threads.runOnUIThread(new Runnable() {
			@Override
			public void run() {
				mImageView.setImageBitmap(bmd);
				mPB.setVisibility(View.GONE);
			}
		});
	}
	
	@Override
	public void onFailed(String uri) {
		Threads.runOnUIThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getActivity().getApplicationContext(), R.string.failed_load_photo, Toast.LENGTH_LONG).show();
				getActivity().finish();
			}
		});
	}
}
