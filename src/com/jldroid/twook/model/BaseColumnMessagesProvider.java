package com.jldroid.twook.model;

import java.util.ArrayList;

public abstract class BaseColumnMessagesProvider implements ColumnMessagesProvider {
	
	private boolean mIsUpdating = false;
	private boolean mIsEnlarging = false;
	
	private int mUnreadCount = 0;
	//private MergedColumnMessageProvider mParent;
	
	private ArrayList<ColumnProviderListener> mListeners = new ArrayList<ColumnProviderListener>();
	
	public BaseColumnMessagesProvider() {
	}
	
	@Override
	public Message getMessage(int pPosition) {
		return getMessages().get(pPosition);
	}
	
	@Override
	public int getMessageCount() {
		return getMessages().size();
	}
	
	public void addListener(ColumnProviderListener listener) {
		mListeners.add(listener);
	};
	
	public void removeListener(ColumnProviderListener listener) {
		mListeners.remove(listener);
	};
	
	public void dispatchChange() {
		for (int i = 0; i < mListeners.size(); i++) {
			mListeners.get(i).onMessagesChanged();
		}
	}
	
	@Override
	public int getUnreadMessageCount() {
		return mUnreadCount;
	}
	
	@Override
	public void resetUnreadMessages() {
		mUnreadCount = 0;
	}
	
	@Override
	public void addUnreadMessages(int pV) {
		mUnreadCount += pV;
	}
	
	@Override
	public boolean isUpdating() {
		return mIsUpdating;
	}
	
	@Override
	public boolean isEnlarging() {
		return mIsEnlarging;
	}
	
	protected void setIsUpdating(boolean v) {
		if (mIsUpdating != v) {
			mIsUpdating = v;
			for (int i = mListeners.size() - 1; i >= 0; i--) {
				mListeners.get(i).onUpdateStateChanged(v);
			}
		}
	}
	
	protected void setIsEnlarging(boolean v) {
		if (mIsEnlarging != v) {
			mIsEnlarging = v;
			for (int i = mListeners.size() - 1; i >= 0; i--) {
				mListeners.get(i).onEnlargingStateChanged(v);
			}
		}
	}
	
}
