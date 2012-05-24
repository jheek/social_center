package com.jldroid.twook.model;

import com.jldroid.twook.R;
import com.jldroid.twook.model.AccountsManager.AccountsManagerListener;
import com.jldroid.twook.model.facebook.FacebookAccount;
import com.jldroid.twook.model.twitter.TwitterAccount;

import android.content.Context;

public class HomeProvider extends MergedColumnMessageProvider implements AccountsManagerListener {
	
	public HomeProvider(Context c) {
		super();
		AccountsManager am = AccountsManager.getInstance(c);
		for (int i = 0; i < am.getAccountCount(); i++) {
			onAccountAdded(am.getAccount(i));
		}
		am.addListener(this);
	}
	
	@Override
	public String getName(Context c) {
		return c.getString(R.string.home_name);
	}
	
	@Override
	public String getDescription(Context c) {
		return c.getString(R.string.home_description);
	}
	
	@Override
	public String getStorageName() {
		return "home";
	}
	
	@Override
	public int getOrder() {
		return 0;
	}
	
	@Override
	public long getID() {
		return -1;
	}
	
	@Override
	public int getWhat() {
		return SyncManager.WHAT_HOME;
	}
	
	@Override
	public void onAccountAdded(IAccount pAccount) {
		if (pAccount instanceof TwitterAccount) {
			TwitterAccount ta = (TwitterAccount) pAccount;
			addProvider(ta.getTimelineProvider());
		} else if (pAccount instanceof FacebookAccount) {
			FacebookAccount fa = (FacebookAccount) pAccount;
			addProvider(fa.getHomeProvider());
		}
	}
	
	@Override
	public void onAccountRemoved(IAccount pAccount) {
		if (pAccount instanceof TwitterAccount) {
			TwitterAccount ta = (TwitterAccount) pAccount;
			removeProvider(ta.getTimelineProvider());
		} else if (pAccount instanceof FacebookAccount) {
			FacebookAccount fa = (FacebookAccount) pAccount;
			removeProvider(fa.getHomeProvider());
		}
	}
	
}
