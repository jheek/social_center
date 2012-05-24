package com.jldroid.twook.model.facebook;

import android.content.Context;

import com.jldroid.twook.R;
import com.jldroid.twook.model.INetworkCallback;
import com.jldroid.twook.model.NetworkCallbackEnlargeStateWrapper;
import com.jldroid.twook.model.NetworkCallbackUpdateStateWrapper;
import com.jldroid.twook.model.SyncManager;
import com.jldroid.twook.model.SyncableData;
import com.jldroid.twook.model.SyncableData.BaseSyncableDataProvider;
import com.jldroid.twook.model.User;

public class FacebookUserProvider extends BaseSyncableDataProvider {

	private FacebookAccount mAccount;
	private User mUser;
	
	private SyncableData mSyncable;
	
	FacebookUserProvider(FacebookAccount account, User user) {
		mAccount = account;
		mUser = user;
		mSyncable = new SyncableData(account, null);
	}
	
	public SyncableData getData() {
		return mSyncable;
	}
	
	@Override
	public int getWhat() {
		return SyncManager.WHAT_FACEBOOK_USER;
	}
	
	@Override
	public long getID() {
		return mUser.id;
	}
	
	@Override
	public String getName(Context c) {
		return c.getString(R.string.fb_user_name, mUser.name);
	}

	@Override
	public String getDescription(Context c) {
		return c.getString(R.string.fb_user_description, mUser.name);
	}
	
	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public boolean isStreaming() {
		return false;
	}

	@Override
	public void requestUpdate(INetworkCallback pCallback) {
		if (isUpdating()) {
			if (pCallback != null) pCallback.onSucceed(mAccount);
			return;
		}
		setIsUpdating(true);
		mAccount.updateUserStreamAsync(new NetworkCallbackUpdateStateWrapper(this, pCallback), this, mUser, mSyncable, false);
	}

	@Override
	public void requestOlderMessages(INetworkCallback pCallback) {
		if (isEnlarging()) {
			if (pCallback != null) pCallback.onSucceed(mAccount);
			return;
		}
		setIsEnlarging(true);
		mAccount.updateUserStreamAsync(new NetworkCallbackEnlargeStateWrapper(this, pCallback), this, mUser, mSyncable, true);
	}

	@Override
	public String getStorageName() {
		return null;
	}
	
	@Override
	public boolean isPersonal() {
		return false;
	}

}
