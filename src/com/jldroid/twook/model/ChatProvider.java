package com.jldroid.twook.model;

import com.jdroid.utils.SortedArrayList;

public interface ChatProvider {

	public static final int REALTIME = 0;
	public static final int NO_UPDATES = -1;
	
	public SortedArrayList<ChatMessage> getMessages();
	
	public void removeListener(ChatProviderListener listener);
	public void addListener(ChatProviderListener listener);
	
	public void sendMessage(String text, INetworkCallback callback);
	
	public long getUpdateInterval();
	public void setUpdateInterval(long updateInterval);
	
	public boolean isRealtimeSupported();
	
}
