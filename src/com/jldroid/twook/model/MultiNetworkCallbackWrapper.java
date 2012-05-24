package com.jldroid.twook.model;

public class MultiNetworkCallbackWrapper implements INetworkCallback {

	private int mSucceedCount = 0;
	private boolean mHasFail = false;
	
	private INetworkCallback mCallback;
	private int mCount;
	
	public MultiNetworkCallbackWrapper(INetworkCallback callback, int count) {
		mCallback = callback;
		mCount = count;
	}
	
	@Override
	public void onSucceed(IAccount pAccount) {
		mSucceedCount++;
		if (mSucceedCount == mCount) {
			if (mCallback != null) mCallback.onSucceed(pAccount);
		}
	}
	
	@Override
	public void onNoNetwork(IAccount pAccount) {
		if (!mHasFail) {
			mHasFail = true;
			if (mCallback != null) mCallback.onNoNetwork(pAccount);
		}
	}
	
	@Override
	public void onFailed(IAccount pAccount) {
		if (!mHasFail) {
			mHasFail = true;
			if (mCallback != null) mCallback.onFailed(pAccount);
		}
	}

}
