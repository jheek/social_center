package com.jldroid.twook.model;

public interface INetworkCallback {
	public void onSucceed(IAccount account);
	public void onFailed(IAccount account);
	public void onNoNetwork(IAccount account);
}
