package com.jldroid.twook.model.twitter;

import android.content.Context;

import com.jldroid.twook.R;
import com.jldroid.twook.model.INetworkCallback;
import com.jldroid.twook.model.NetworkCallbackEnlargeStateWrapper;
import com.jldroid.twook.model.NetworkCallbackUpdateStateWrapper;
import com.jldroid.twook.model.SyncManager;
import com.jldroid.twook.model.SyncableData;
import com.jldroid.twook.model.SyncableData.BaseSyncableDataProvider;
import com.jldroid.twook.model.User;

public class TwitterUserProvider extends BaseSyncableDataProvider {

	private TwitterAccount mAccount;
	private User mUser;
	
	private SyncableData mSyncable;
	
	public TwitterUserProvider(TwitterAccount account, User user) {
		mAccount = account;
		mUser = user;
		mSyncable = new SyncableData(mAccount, null);
	}
	
	public SyncableData getData() {
		return mSyncable;
	}
	
	@Override
	public long getID() {
		return mUser.id;
	}
	
	@Override
	public int getWhat() {
		return SyncManager.WHAT_TWITTER_USER;
	}
	
	@Override
	public String getName(Context c) {
		return c.getString(R.string.twitter_user_name, mUser.name);
	}
	
	@Override
	public String getDescription(Context c) {
		return c.getString(R.string.twitter_user_description, mUser.name);
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
	public boolean isUpdateable() {
		return true;
	}

	@Override
	public void requestUpdate(INetworkCallback pCallback) {
		if (isUpdating()) {
			if (pCallback != null) pCallback.onSucceed(mAccount);
			return;
		}
		setIsUpdating(true);
		mAccount.updateUserTimeline(new NetworkCallbackUpdateStateWrapper(this, pCallback), this, mUser, mSyncable, false);
	}

	@Override
	public void requestOlderMessages(INetworkCallback pCallback) {
		if (isEnlarging()) {
			if (pCallback != null) pCallback.onSucceed(mAccount);
			return;
		}
		setIsEnlarging(true);
		mAccount.updateUserTimeline(new NetworkCallbackEnlargeStateWrapper(this, pCallback), this, mUser, mSyncable, true);
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
