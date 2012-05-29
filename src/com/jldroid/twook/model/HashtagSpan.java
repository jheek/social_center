package com.jldroid.twook.model;

import android.content.Context;
import android.content.Intent;
import android.text.style.ClickableSpan;
import android.view.View;

import com.jldroid.twook.activities.SearchActivity;
import com.jldroid.twook.fragments.SearchFragment;

public class HashtagSpan extends ClickableSpan {

	private String mText;
	
	public HashtagSpan(String text) {
		mText = text;
	}
	
	@Override
	public void onClick(View pWidget) {
		Context c = pWidget.getContext();
		pWidget.getContext().startActivity(new Intent(c, SearchActivity.class)
				.putExtra(SearchFragment.EXTRA_QUERY, mText)
				.putExtra(SearchFragment.EXTRA_TYPE, SearchFragment.TYPE_MESSAGES));
	}
	

}
