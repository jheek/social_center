package com.jldroid.twook.model;

import java.util.ArrayList;
import com.jdroid.utils.SortedArrayList;

public abstract class BaseChatProvider implements ChatProvider {

	protected SortedArrayList<ChatMessage> mMessages = new SortedArrayList<ChatMessage>();
	
	private ArrayList<ChatProviderListener> mListeners = new ArrayList<ChatProviderListener>();
	
	protected long mUpdateInterval;
	
	public BaseChatProvider() {
	}
	
	@Override
	public SortedArrayList<ChatMessage> getMessages() {
		return mMessages;
	}

	@Override
	public void removeListener(ChatProviderListener pListener) {
		mListeners.remove(pListener);
	}

	@Override
	public void addListener(ChatProviderListener pListener) {
		mListeners.add(pListener);
	}
	
	@Override
	public long getUpdateInterval() {
		return mUpdateInterval;
	}

	@Override
	public void setUpdateInterval(long pUpdateInterval) {
		mUpdateInterval = pUpdateInterval;
	}

}
