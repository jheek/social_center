package com.jldroid.twook.model.facebook;

import android.content.Context;
import android.text.TextUtils;

import com.jldroid.twook.R;
import com.jldroid.twook.model.IAccount;
import com.jldroid.twook.model.INetworkCallback;
import com.jldroid.twook.model.ISearchableColumn;
import com.jldroid.twook.model.NetworkCallbackEnlargeStateWrapper;
import com.jldroid.twook.model.NetworkCallbackUpdateStateWrapper;
import com.jldroid.twook.model.SyncableData;
import com.jldroid.twook.model.SyncableData.BaseSyncableDataProvider;
import com.jdroid.utils.StorageManager.StorageBundle;

public class FacebookSearchColumn extends BaseSyncableDataProvider implements ISearchableColumn {

	private String mQuery;
	private FacebookAccount mAccount;
	
	private SyncableData mSyncable;
	
	public FacebookSearchColumn(FacebookAccount account, String query, StorageBundle bundle) {
		mAccount = account;
		mQuery = query;
		mSyncable = new SyncableData(account, bundle);
		if (bundle == null && !TextUtils.isEmpty(query)) {
			doLocalSearch();
		}
	}
	
	@Override
	public long getID() {
		return 0;
	}

	@Override
	public int getWhat() {
		return 0;
	}

	@Override
	public String getName(Context c) {
		return c.getString(R.string.fb_search_name, mQuery);
	}

	@Override
	public String getDescription(Context c) {
		return c.getString(R.string.fb_search_description);
	}

	@Override
	public int getOrder() {
		return 3;
	}

	@Override
	public boolean isStreaming() {
		return false;
	}
	
	private void doLocalSearch() {
		if (!TextUtils.isEmpty(mQuery)) {
			mAccount.searchLocal(mSyncable.list, mQuery);
		}
		dispatchChange();
	}

	@Override
	public void requestUpdate(final INetworkCallback pCallback) {
		if (TextUtils.isEmpty(mQuery)) {
			if (pCallback != null) pCallback.onSucceed(mAccount);
			return;
		}
		setIsUpdating(true);
		mAccount.updateSearchAsync(new NetworkCallbackUpdateStateWrapper(this, pCallback), this, mSyncable, false);
	}
	
	@Override
	public void requestOlderMessages(final INetworkCallback pCallback) {
		if (TextUtils.isEmpty(mQuery) || mSyncable.getLastUpdate() == -1) {
			if (pCallback != null) pCallback.onSucceed(mAccount);
			return;
		}
		setIsEnlarging(true);
		mAccount.updateSearchAsync(new NetworkCallbackEnlargeStateWrapper(this, pCallback), this, mSyncable, false);
	}
	
	@Override
	public boolean hasOlderMessages() {
		return mSyncable.hasOlder && mSyncable.getLastUpdate() != -1;
	}

	@Override
	public String getStorageName() {
		return "fbs" + mQuery;
	}

	@Override
	public boolean isPersonal() {
		return false;
	}

	@Override
	public IAccount getAccount() {
		return mAccount;
	}

	@Override
	public String getQuery() {
		return mQuery;
	}

	@Override
	public void setQuery(String pQuery) {
		mQuery = pQuery;
		mSyncable.clear();
		doLocalSearch();
	}

	@Override
	protected SyncableData getData() {
		return mSyncable;
	}

}
