package com.jldroid.twook.model;

import android.content.Context;

import com.jldroid.twook.R;
import com.jldroid.twook.model.AccountsManager.AccountsManagerListener;
import com.jldroid.twook.model.facebook.FacebookAccount;
import com.jldroid.twook.model.twitter.TwitterAccount;

public class MessagesProvider extends MergedColumnMessageProvider implements AccountsManagerListener {
	
	
	public MessagesProvider(Context c) {
		super();
		AccountsManager am = AccountsManager.getInstance(c);
		for (int i = 0; i < am.getAccountCount(); i++) {
			onAccountAdded(am.getAccount(i));
		}
		am.addListener(this);
	}
	
	@Override
	public String getName(Context c) {
		return c.getString(R.string.messages_name);
	}
	
	@Override
	public String getDescription(Context c) {
		return c.getString(R.string.messages_description);
	}
	
	@Override
	public int getOrder() {
		return 2;
	}
	
	@Override
	public String getStorageName() {
		return "messages";
	}
	
	@Override
	public long getID() {
		return -1;
	}
	
	@Override
	public int getWhat() {
		return SyncManager.WHAT_MESSAGES;
	}
	
	@Override
	public void onAccountAdded(IAccount pAccount) {
		if (pAccount instanceof TwitterAccount) {
			TwitterAccount ta = (TwitterAccount) pAccount;
			addProvider(ta.getDirectMessagesProvider());
		} else if (pAccount instanceof FacebookAccount) {
			FacebookAccount fa = (FacebookAccount) pAccount;
			addProvider(fa.getMessagesProvider());
		}
	}
	
	@Override
	public void onAccountRemoved(IAccount pAccount) {
		if (pAccount instanceof TwitterAccount) {
			TwitterAccount ta = (TwitterAccount) pAccount;
			removeProvider(ta.getDirectMessagesProvider());
		} else if (pAccount instanceof FacebookAccount) {
			FacebookAccount fa = (FacebookAccount) pAccount;
			removeProvider(fa.getMessagesProvider());
		}
	}
	
}
