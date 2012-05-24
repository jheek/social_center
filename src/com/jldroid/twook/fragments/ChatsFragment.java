package com.jldroid.twook.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.jldroid.twook.R;
import com.jldroid.twook.model.ColumnManager;
import com.jldroid.twook.model.MessagesProvider;
import com.jldroid.twook.view.ColumnView;

public class ChatsFragment extends SherlockFragment {

	private ColumnView mColumnView;
	
	private MessagesProvider mChats;
	
	@Override
	public View onCreateView(LayoutInflater pInflater, ViewGroup pContainer, Bundle pSavedInstanceState) {
		View v = pInflater.inflate(R.layout.chats, null);
		mColumnView = (ColumnView) v.findViewById(R.id.columnView);
		return v;
	}

	@Override
	public void onActivityCreated(Bundle pSavedInstanceState) {
		super.onActivityCreated(pSavedInstanceState);
		ActionBar ab = getActionBar();
		ab.setTitle(R.string.chats);
		ab.setIcon(R.drawable.ab_icon_messages);
		
		mChats = ColumnManager.getInstance(getApplicationContext()).getMessagesProvider();
		
		mColumnView.setProvider(mChats, false);
	}
	
}
