package com.jldroid.twook.model.twitter;

import android.content.Context;

import com.jldroid.twook.R;
import com.jldroid.twook.model.INetworkCallback;
import com.jldroid.twook.model.NetworkCallbackEnlargeStateWrapper;
import com.jldroid.twook.model.NetworkCallbackUpdateStateWrapper;
import com.jldroid.twook.model.SyncManager;
import com.jldroid.twook.model.SyncableData;
import com.jldroid.twook.model.SyncableData.BaseSyncableDataProvider;
import com.jldroid.twook.model.twitter.TwitterAccount.TwitterAccountListener;

public class TwitterMentionsProvider extends BaseSyncableDataProvider implements TwitterAccountListener {

	private TwitterAccount mTwitterAccount;
	
	public TwitterMentionsProvider(TwitterAccount account) {
		this.mTwitterAccount = account;
		this.mTwitterAccount.addListener(this);
	}
	
	@Override
	protected SyncableData getData() {
		return mTwitterAccount.getMentions();
	}
	
	@Override
	public void onTimelineChanged(int pNewCount) {
	}
	
	@Override
	public void onDirectMessagesChanged(int pNewCount) {
	}
	
	@Override
	public void onProvidersChanged(TwitterAccount pAccount) {
	}
	
	@Override
	public void onMentionsChanged(int pNewCount) {
		addUnreadMessages(pNewCount);
		dispatchChange();
	}
	
	@Override
	public long getID() {
		return -1;
	}
	
	@Override
	public int getWhat() {
		return SyncManager.WHAT_TWITTER_MENTIONS;
	}
	
	@Override
	public String getName(Context c) {
		return c.getString(R.string.twitter_mentions_name, mTwitterAccount.getUser().name);
	}
	
	@Override
	public String getDescription(Context c) {
		return c.getString(R.string.twitter_mentions_description);
	}
	
	@Override
	public int getOrder() {
		return 1;
	}

	@Override
	public void requestUpdate(INetworkCallback callback) {
		if (!isUpdating()) {
			setIsUpdating(true);
			mTwitterAccount.updateMentions(new NetworkCallbackUpdateStateWrapper(this, callback), false);
		}
	}
	
	@Override
	public void requestOlderMessages(INetworkCallback callback) {
		if (!isEnlarging()) {
			setIsEnlarging(true);
			mTwitterAccount.updateMentions(new NetworkCallbackEnlargeStateWrapper(this, callback), true);
		}
	}
	
	@Override
	public boolean isStreaming() {
		return mTwitterAccount.isStreaming();
	}
	
	@Override
	public String getStorageName() {
		return "twmentions" + mTwitterAccount.getUser().id;
	}
	
	@Override
	public boolean isPersonal() {
		return true;
	}

}
