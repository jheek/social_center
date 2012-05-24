package com.jldroid.twook.model;

import android.content.Context;

import com.jldroid.twook.R;
import com.jdroid.utils.SortedArrayList;

public class EmptyMessageProvider extends BaseColumnMessagesProvider implements ISearchableColumn {

	private static final SortedArrayList<Message> NO_MESSAGES = new SortedArrayList<Message>(0);

	private IAccount mAccount;
	
	public EmptyMessageProvider(IAccount account) {
		mAccount = account;
	}
	
	@Override
	public long getID() {
		return -1;
	}
	
	@Override
	public int getWhat() {
		return -1;
	}
	
	@Override
	public int getMessageCount() {
		return 0;
	}

	@Override
	public Message getMessage(int pPosition) {
		return null;
	}

	@Override
	public boolean isStreaming() {
		return false;
	}

	@Override
	public boolean isUpdateable() {
		return false;
	}

	@Override
	public boolean hasOlderMessages() {
		return false;
	}

	@Override
	public void requestUpdate(INetworkCallback callback) {
		if (callback != null)
			callback.onSucceed(null);
	}

	@Override
	public void requestOlderMessages(INetworkCallback callback) {
		if (callback != null)
			callback.onSucceed(null);
	}
	
	@Override
	public SortedArrayList<Message> getMessages() {
		return NO_MESSAGES;
	}
	
	@Override
	public String getName(Context c) {
		return c.getString(R.string.empty_name);
	}
	
	@Override
	public String getDescription(Context c) {
		return c.getString(R.string.empty_description);
	}
	
	@Override
	public int getOrder() {
		return 0;
	}
	
	@Override
	public String getStorageName() {
		return "empty";
	}
	
	@Override
	public boolean isPersonal() {
		return false;
	}
	
	@Override
	public long getLastUpdate() {
		return System.currentTimeMillis();
	}

	
	@Override
	public IAccount getAccount() {
		return mAccount;
	}
	
	@Override
	public String getQuery() {
		return "";
	}
	
	@Override
	public void setQuery(String pQuery) {
	}
	
}
