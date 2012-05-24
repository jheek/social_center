package com.jldroid.twook.model;

import com.jldroid.twook.activities.MainActivity;
import com.jldroid.twook.fragments.SearchFragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.style.ClickableSpan;
import android.view.View;

public class HashtagSpan extends ClickableSpan {

	private String mText;
	
	public HashtagSpan(String text) {
		mText = text;
	}
	
	@Override
	public void onClick(View pWidget) {
		Activity a = (Activity) pWidget.getContext();
		if (a instanceof MainActivity) {
			MainActivity ma = (MainActivity) a;
			ma.showFragment(new SearchFragment(mText, false));
		}
	}
	

}
