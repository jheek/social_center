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

public class TwitterTimelineProvider extends BaseSyncableDataProvider implements TwitterAccountListener {
	
	private TwitterAccount mTwitterAccount;
	
	public TwitterTimelineProvider(TwitterAccount account) {
		this.mTwitterAccount = account;
		this.mTwitterAccount.addListener(this);
	}
	
	@Override
	protected SyncableData getData() {
		return mTwitterAccount.getTimeline();
	}
	
	@Override
	public void onTimelineChanged(int pNewCount) {
		addUnreadMessages(pNewCount);
		dispatchChange();
	}
	
	@Override
	public void onDirectMessagesChanged(int pNewCount) {
	}
	
	@Override
	public void onMentionsChanged(int pNewCount) {
	}
	
	@Override
	public void onProvidersChanged(TwitterAccount pAccount) {
	}
	
	@Override
	public long getID() {
		return -1;
	}
	
	@Override
	public int getWhat() {
		return SyncManager.WHAT_TWITTER_TIMELINE;
	}
	
	@Override
	public String getName(Context c) {
		return c.getString(R.string.twitter_timeline_name, mTwitterAccount.getUser().name);
	}
	
	@Override
	public String getDescription(Context c) {
		return c.getString(R.string.twitter_timeline_description);
	}
	
	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void requestUpdate(INetworkCallback callback) {
		if (!isUpdating()) {
			setIsUpdating(true);
			mTwitterAccount.updateTimeline(new NetworkCallbackUpdateStateWrapper(this, callback), false);
		}
	}
	
	@Override
	public void requestOlderMessages(INetworkCallback callback) {
		if (!isEnlarging()) {
			setIsEnlarging(true);
			mTwitterAccount.updateTimeline(new NetworkCallbackEnlargeStateWrapper(this, callback), true);
		}
	}
	
	@Override
	public boolean isStreaming() {
		return mTwitterAccount.isStreaming();
	}
	
	@Override
	public String getStorageName() {
		return "twtimeline" + mTwitterAccount.getUser().id;
	}
	
	@Override
	public boolean isPersonal() {
		return false;
	}
	
}
