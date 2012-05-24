package com.jldroid.twook.model;

import android.content.Context;

import com.jdroid.utils.SortedArrayList;


public interface ColumnMessagesProvider {

	public long getID();
	public int getWhat();
	
	public String getName(Context c);
	public String getDescription(Context c);
	
	public int getOrder();
	
	public Message getMessage(int position);
	public SortedArrayList<Message> getMessages();
	public int getMessageCount();
	
	public boolean isUpdating();
	public boolean isStreaming();
	public boolean isEnlarging();
	public boolean isUpdateable();
	public boolean hasOlderMessages();
	
	public void addListener(ColumnProviderListener listener);
	public void removeListener(ColumnProviderListener listener);
	
	public void requestUpdate(INetworkCallback callback);
	public void requestOlderMessages(INetworkCallback callback);
	
	public int getUnreadMessageCount();
	public void addUnreadMessages(int v);
	public void resetUnreadMessages();
	
	public String getStorageName();
	
	public boolean isPersonal();
	
	public long getLastUpdate();
}
