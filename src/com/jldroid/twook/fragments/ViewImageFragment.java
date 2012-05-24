package com.jldroid.twook.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.actionbarsherlock.app.SherlockFragment;
import com.jldroid.twook.R;

public class ViewImageFragment extends SherlockFragment {

	private ImageView mImageView;
	private Bitmap mBmd;
	
	public ViewImageFragment(Bitmap bmd) {
		mBmd = bmd;
	}
	
	@Override
	public View onCreateView(LayoutInflater pInflater, ViewGroup pContainer, Bundle pSavedInstanceState) {
		View v = pInflater.inflate(R.layout.view_image, null);
		mImageView = (ImageView) v.findViewById(R.id.imageView);
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle pSavedInstanceState) {
		super.onActivityCreated(pSavedInstanceState);
		mImageView.setImageBitmap(mBmd);
		mImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
	}
	
}
