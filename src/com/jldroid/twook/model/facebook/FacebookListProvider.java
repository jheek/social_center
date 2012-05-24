package com.jldroid.twook.model.facebook;

import android.content.Context;

import com.jldroid.twook.R;
import com.jldroid.twook.model.BaseColumnMessagesProvider;
import com.jldroid.twook.model.INetworkCallback;
import com.jldroid.twook.model.Message;
import com.jldroid.twook.model.NetworkCallbackEnlargeStateWrapper;
import com.jldroid.twook.model.NetworkCallbackUpdateStateWrapper;
import com.jldroid.twook.model.SyncManager;
import com.jldroid.twook.model.SyncableData;
import com.jldroid.twook.model.SyncableData.BaseSyncableDataProvider;
import com.jdroid.utils.SortedArrayList;

public class FacebookListProvider extends BaseSyncableDataProvider {

	protected SyncableData mSyncable;
	protected FacebookList mList;
	
	private FacebookAccount mAccount;
	
	public FacebookListProvider(FacebookAccount account, SyncableData syncable, FacebookList list) {
		super();
		mAccount = account;
		mSyncable = syncable;
		mList = list;
	}
	
	public SyncableData getData() {
		return mSyncable;
	}
	
	@Override
	public int getWhat() {
		return SyncManager.WHAT_FACEBOOK_LIST;
	}
	
	@Override
	public long getID() {
		return mList.id;
	}
	
	@Override
	public String getName(Context c) {
		return c.getString(R.string.fb_list_name, mList.name);
	}

	@Override
	public String getDescription(Context c) {
		return c.getString(R.string.fb_list_description);
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
		mAccount.updateListAsync(new NetworkCallbackUpdateStateWrapper(this, pCallback), this, false);
	}

	@Override
	public void requestOlderMessages(INetworkCallback pCallback) {
		if (isEnlarging()) {
			if (pCallback != null) pCallback.onSucceed(mAccount);
			return;
		}
		setIsEnlarging(true);
		mAccount.updateListAsync(new NetworkCallbackEnlargeStateWrapper(this, pCallback), this, true);
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
