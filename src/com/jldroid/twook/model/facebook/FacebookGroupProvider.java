package com.jldroid.twook.model.facebook;

import android.content.Context;

import com.jldroid.twook.R;
import com.jldroid.twook.model.INetworkCallback;
import com.jldroid.twook.model.NetworkCallbackEnlargeStateWrapper;
import com.jldroid.twook.model.NetworkCallbackUpdateStateWrapper;
import com.jldroid.twook.model.SyncManager;
import com.jldroid.twook.model.SyncableData;
import com.jldroid.twook.model.SyncableData.BaseSyncableDataProvider;

public class FacebookGroupProvider extends BaseSyncableDataProvider {

	protected SyncableData mSyncable;
	protected FacebookGroup mGroup;
	
	private FacebookAccount mAccount;
	
	public FacebookGroupProvider(FacebookAccount account, SyncableData syncable, FacebookGroup group) {
		super();
		mAccount = account;
		mSyncable = syncable;
		mGroup = group;
	}
	
	@Override
	protected SyncableData getData() {
		return mSyncable;
	}
	
	@Override
	public int getWhat() {
		return SyncManager.WHAT_FACEBOOK_GROUP;
	}
	
	@Override
	public long getID() {
		return mGroup.id;
	}
	
	@Override
	public String getName(Context c) {
		return c.getString(R.string.fb_group_name, mGroup.name);
	}

	@Override
	public String getDescription(Context c) {
		return c.getString(R.string.fb_group_description);
	}

	@Override
	public int getOrder() {
		return 50;
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
		mAccount.updateGroupAsync(new NetworkCallbackUpdateStateWrapper(this, pCallback), this, false);
	}

	@Override
	public void requestOlderMessages(INetworkCallback pCallback) {
		if (isEnlarging()) {
			if (pCallback != null) pCallback.onSucceed(mAccount);
			return;
		}
		setIsEnlarging(true);
		mAccount.updateGroupAsync(new NetworkCallbackEnlargeStateWrapper(this, pCallback), this, true);
	}

	@Override
	public String getStorageName() {
		return mAccount.getUser().id + "_GROUP_" + mGroup.name;
	}
	
	@Override
	public boolean isPersonal() {
		return false;
	}

}
