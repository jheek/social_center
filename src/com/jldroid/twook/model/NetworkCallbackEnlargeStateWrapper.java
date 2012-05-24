package com.jldroid.twook.model;

public class NetworkCallbackEnlargeStateWrapper implements INetworkCallback {

	private BaseColumnMessagesProvider mProvider;
	private INetworkCallback mInnerCallback;
	
	public NetworkCallbackEnlargeStateWrapper(BaseColumnMessagesProvider provider, INetworkCallback innerCallback) {
		mProvider = provider;
		if (mInnerCallback != null) mInnerCallback = innerCallback;
	}
	
	@Override
	public void onSucceed(IAccount account) {
		mProvider.setIsEnlarging(false);
		if (mInnerCallback != null) mInnerCallback.onSucceed(account);
	}

	@Override
	public void onFailed(IAccount account) {
		mProvider.setIsEnlarging(false);
		if (mInnerCallback != null) mInnerCallback.onFailed(account);
	}

	@Override
	public void onNoNetwork(IAccount account) {
		mProvider.setIsEnlarging(false);
		if (mInnerCallback != null) mInnerCallback.onNoNetwork(account);
	}

}
