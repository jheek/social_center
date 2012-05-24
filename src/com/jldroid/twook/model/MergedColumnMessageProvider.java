package com.jldroid.twook.model;

import java.util.ArrayList;

import com.jdroid.utils.SortedArrayList;

public abstract class MergedColumnMessageProvider extends BaseColumnMessagesProvider {

	private SortedArrayList<Message> mMessages = new SortedArrayList<Message>();
	
	protected ArrayList<ColumnMessagesProvider> mProviders = new ArrayList<ColumnMessagesProvider>();
	private boolean mDirty = false; // TODO shared??
	
	private ColumnProviderListener mListener = new ColumnProviderListener() {
		@Override
		public void onMessagesChanged() {
			if (isUpdating() || isEnlarging()) {
				mDirty = true;
			} else {
				updateMessages();
			}
		}
		@Override
		public void onHasOlderMessagesChanged(boolean pV) {
		}
		
		@Override
		public void onUpdateStateChanged(boolean pIsUpdating) {
			updateUpdateState();
			if (!isUpdating() && mDirty) {
				mDirty = false;
				updateMessages();
			}
		}
		
		@Override
		public void onEnlargingStateChanged(boolean pIsEnlarging) {
			updateEnlargingState();
			if (!isEnlarging() && mDirty) {
				mDirty = false;
				updateMessages();
			}
		}
	};
	
	public MergedColumnMessageProvider(ColumnMessagesProvider... providers) {
		for (int i = 0; i < providers.length; i++) {
			addProvider(providers[i]);
		}
	}
	
	@Override
	public int getMessageCount() {
		return mMessages.size();
	}

	@Override
	public Message getMessage(int position) {
		return mMessages.get(position);
	}
	
	@Override
	public SortedArrayList<Message> getMessages() {
		return mMessages;
	}

	@Override
	public boolean isStreaming() {
		for (int i = 0; i < mProviders.size(); i++) {
			if (!mProviders.get(i).isStreaming()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isUpdateable() {
		for (int i = 0; i < mProviders.size(); i++) {
			if (mProviders.get(i).isUpdateable()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean hasOlderMessages() {
		for (int i = 0; i < mProviders.size(); i++) {
			if (mProviders.get(i).hasOlderMessages()) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void requestUpdate(final INetworkCallback callback) {
		final int providerCount = mProviders.size();
		if (providerCount > 0) {
			INetworkCallback _callback = callback != null ? new MultiNetworkCallbackWrapper(callback, providerCount) : null;
			for (int i = 0; i < providerCount; i++) {
				mProviders.get(i).requestUpdate(_callback);
			}
		}
	}

	@Override
	public void requestOlderMessages(final INetworkCallback callback) {
		final int providerCount = mProviders.size();
		INetworkCallback _callback = callback != null ? new MultiNetworkCallbackWrapper(callback, providerCount) : null;
		for (int i = 0; i < providerCount; i++) {
			mProviders.get(i).requestOlderMessages(_callback);
		}
	}

	public void addProvider(ColumnMessagesProvider provider) {
		mProviders.add(provider);
		provider.addListener(mListener);
		mMessages.addAll(provider.getMessages());
		dispatchChange();
	}
	
	public void removeProvider(ColumnMessagesProvider provider) {
		mProviders.remove(provider);
		provider.removeListener(mListener);
		mMessages.removeAll(provider.getMessages());
		dispatchChange();
	}
	
	private void updateMessages() {
		mMessages.clear();
		for (int i = 0; i < mProviders.size(); i++) {
			mMessages.addAll(mProviders.get(i).getMessages());
		}
		dispatchChange();
	}
	
	private void updateUpdateState() {
		for (int i = 0; i < mProviders.size(); i++) {
			if (mProviders.get(i).isUpdating()) {
				setIsUpdating(true);
				return;
			}
		}
		setIsUpdating(false);
	}
	
	private void updateEnlargingState() {
		for (int i = 0; i < mProviders.size(); i++) {
			if (mProviders.get(i).isEnlarging()) {
				setIsEnlarging(true);
				return;
			}
		}
		setIsEnlarging(false);
	}
	
	@Override
	public int getUnreadMessageCount() {
		int unreadCount = 0;
		for (int i = 0; i < mProviders.size(); i++) {
			unreadCount += mProviders.get(i).getUnreadMessageCount();
		}
		return unreadCount;
	}
	
	@Override
	public void resetUnreadMessages() {
		for (int i = 0; i < mProviders.size(); i++) {
			mProviders.get(i).resetUnreadMessages();
		}
	}
	
	@Override
	public void addUnreadMessages(int pV) {
		throw new IllegalAccessError("Cannot add unread messages to merged column provider!");
	}
	
	@Override
	public boolean isPersonal() {
		for (int i = 0; i < mProviders.size(); i++) {
			if (mProviders.get(i).isPersonal()) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public long getLastUpdate() {
		long lastUpdate = System.currentTimeMillis();
		for (int i = 0; i < mProviders.size(); i++) {
			lastUpdate = Math.min(lastUpdate, mProviders.get(i).getLastUpdate());
		}
		return lastUpdate;
	}
	
}
