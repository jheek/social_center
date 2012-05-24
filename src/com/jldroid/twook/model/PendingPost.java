package com.jldroid.twook.model;

import android.content.Context;

import com.jldroid.twook.model.facebook.FacebookAccount;
import com.jldroid.twook.model.twitter.TwitterAccount;
import com.jdroid.utils.StorageManager.StorageBundle;

public class PendingPost {
	
	public static final int TYPE_TWITTER_STATUS_UPDATE = 0;
	public static final int TYPE_TWITTER_REPLY = 1;
	public static final int TYPE_TWITTER_RETWEET = 2;
	public static final int TYPE_FACEBOOK_STATUS_UPDATE = 3;
	public static final int TYPE_FACEBOOK_WALL_POST = 4;
	
	public TwitterAccount twitterAccount;
	public FacebookAccount facebookAccount;
	
	public int type;
	
	public String text;
	public String imgPath;
	
	public long twitterTargetID;
	public long facebookTargetID;
	
	private StorageBundle mBundle;
	
	public PendingPost(int type, String text) {
		this.type = type;
		this.text = text;
		this.mBundle = new StorageBundle();
	}
	
	public PendingPost(Context c, StorageBundle bundle) {
		this.mBundle = bundle;
		long twitterAccountID = bundle.readLong("TWITTER_ACCOUNT", -1);
		long facebookAccountID = bundle.readLong("FACEBOOK_ACCOUNT", -1);
		type = bundle.readInt("TYPE", -1);
		text = bundle.readString("TEXT", null);
		imgPath = bundle.readString("IMGPATH", null);
		twitterTargetID = bundle.readLong("TWITTERID", -1);
		facebookTargetID = bundle.readLong("FACEBOOKID", -1);
		AccountsManager am = AccountsManager.getInstance(c);
		twitterAccount = am.findTwitterAccountByID(twitterAccountID);
		facebookAccount = am.findFacebookAccountByID(facebookAccountID);
	}
	
	public StorageBundle getBundle() {
		return mBundle;
	}
	
	public StorageBundle updateBundle() {
		mBundle.deleteAll();
		mBundle.write("TYPE", type);
		mBundle.write("TEXT", text);
		mBundle.write("IMGPATH", imgPath);
		mBundle.write("TWITTERID", twitterTargetID);
		mBundle.write("FACEBOOKID", facebookTargetID);
		if (twitterAccount != null) {
			mBundle.write("TWITTER_ACCOUNT", twitterAccount.getUser().id);
		}
		if (facebookAccount != null) {
			mBundle.write("FACEBOOK_ACCOUNT", facebookAccount.getUser().id);
		}
		return mBundle;
	}
}
