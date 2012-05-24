package com.jldroid.twook.model.twitter;

import android.content.Context;

import com.jldroid.twook.R;
import com.jldroid.twook.model.INetworkCallback;
import com.jldroid.twook.model.NetworkCallbackEnlargeStateWrapper;
import com.jldroid.twook.model.NetworkCallbackUpdateStateWrapper;
import com.jldroid.twook.model.SyncManager;
import com.jldroid.twook.model.SyncableData;
import com.jldroid.twook.model.SyncableData.BaseSyncableDataProvider;

public class TwitterListProvider extends BaseSyncableDataProvider {

	protected SyncableData mSyncable;
	protected TwitterList mList;
	
	private TwitterAccount mAccount;
	
	public TwitterListProvider(TwitterAccount account, SyncableData syncable, TwitterList list) {
		super();
		mAccount = account;
		mSyncable = syncable;
		mList = list;
	}
	
	public SyncableData getData() {
		return mSyncable;
	}
	
	@Override
	public long getID() {
		return mList.id;
	}
	
	@Override
	public int getWhat() {
		return SyncManager.WHAT_TWITTER_LIST;
	}
	
	@Override
	public String getName(Context c) {
		return c.getString(R.string.twitter_list_name, mList.name);
	}

	@Override
	public String getDescription(Context c) {
		return c.getString(R.string.twitter_list_description);
	}

	@Override
	public int getOrder() {
		return 99;
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
		mAccount.updateUserList(new NetworkCallbackUpdateStateWrapper(this, pCallback), this, false);
	}

	@Override
	public void requestOlderMessages(INetworkCallback pCallback) {
		if (isEnlarging()) {
			if (pCallback != null) pCallback.onSucceed(mAccount);
			return;
		}
		setIsEnlarging(true);
		mAccount.updateUserList(new NetworkCallbackEnlargeStateWrapper(this, pCallback), this, true);
	}

	@Override
	public String getStorageName() {
		return mAccount.getUser().id + "_LIST_" + mList.name;
	}
	
	@Override
	public boolean isPersonal() {
		return false;
	}

}
