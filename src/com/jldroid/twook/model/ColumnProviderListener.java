package com.jldroid.twook.model;

public interface ColumnProviderListener {
	
	public void onMessagesChanged();
	public void onHasOlderMessagesChanged(boolean v);
	
	public void onUpdateStateChanged(boolean isUpdating);
	public void onEnlargingStateChanged(boolean isEnlarging);
	
}
