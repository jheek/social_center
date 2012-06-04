package com.jldroid.twook.model;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.jldroid.twook.activities.MainActivity;
import com.jldroid.twook.activities.ViewProfileActivity;
import com.jldroid.twook.fragments.ViewProfileFragment;
import com.jldroid.twook.model.facebook.FacebookAccount;
import com.jldroid.twook.model.twitter.TwitterAccount;
import com.jdroid.utils.AbsStorageObject;
import java.util.ArrayList;
import com.jdroid.utils.SortedArrayList;
import com.jdroid.utils.StorageManager;
import com.jdroid.utils.StorageManager.StorageBundle;

public class AccountsManager {
	
	private static AccountsManager sSingleton;
	
	private Context mContext;
	
	private ArrayList<IAccount> mAccounts;
	private ArrayList<TwitterAccount> mTwitterAccounts;
	private ArrayList<FacebookAccount> mFacebookAccounts;
	
	private ArrayList<AccountsManagerListener> mListeners = new ArrayList<AccountsManagerListener>();
	
	private boolean mAvailable = false;
	
	private AccountsManager(Context c) {
		mContext = c;
		StorageManager sm = StorageManager.getDeflaut(c);
		
		StorageBundle[] twitterAccounts = sm.readBundleArray("TWITTER_ACCOUNTS", AbsStorageObject.EMPTY_BUNDLE_ARRAY);
		StorageBundle[] facebookAccounts = sm.readBundleArray("FACEBOOK_ACCOUNTS", AbsStorageObject.EMPTY_BUNDLE_ARRAY);
		
		mAccounts = new ArrayList<IAccount>(twitterAccounts.length + facebookAccounts.length);
		mTwitterAccounts = new ArrayList<TwitterAccount>(twitterAccounts.length);
		mFacebookAccounts = new ArrayList<FacebookAccount>(facebookAccounts.length);
		for (int i = 0; i < twitterAccounts.length; i++) {
			if (twitterAccounts[i] == null) {
				continue;
			}
			TwitterAccount ta = new TwitterAccount(mContext, twitterAccounts[i]);
			mTwitterAccounts.add(ta);
			mAccounts.add(ta);
		}
		for (int i = 0; i < facebookAccounts.length; i++) {
			if (facebookAccounts[i] == null) {
				continue;
			}
			FacebookAccount fa = new FacebookAccount(mContext, facebookAccounts[i]);
			mFacebookAccounts.add(fa);
			mAccounts.add(fa);
		}
	}
	
	public boolean viewProfile(Context c, User user) {
		return viewProfile(c, user.type, user.id);
	}
	
	public boolean viewProfile(Context c, int type, long id) {
		User user = null;
		if (type == User.TYPE_TWITTER || type == User.TYPE_UNKNOWN) {
			for (int i = 0; i < mTwitterAccounts.size(); i++) {
				TwitterAccount ta = mTwitterAccounts.get(i);
				user = User.findByID(ta.getFollowing(), id);
				if (user != null) {
					viewProfile(c, ta, user);
					return true;
				}
				user = User.findByID(ta.getFollowers(), id);
				if (user != null) {
					viewProfile(c, ta, user);
					return true;
				}
			}
		}
		if (type == User.TYPE_FACEBOOK || type == User.TYPE_UNKNOWN) {
			for (int i = 0; i < mFacebookAccounts.size(); i++) {
				FacebookAccount fa = mFacebookAccounts.get(i);
				user = User.findByID(fa.getFriends(), id);
				if (user != null) {
					viewProfile(c, fa, user);
					return true;
				}
			}
		}
		return false;
	}
	public static void viewProfile(Context c, IAccount account, User user) {
		c.startActivity(new Intent(c, ViewProfileActivity.class)
			.putExtra(ViewProfileFragment.EXTRA_ACCOUNT, account.getUser().id)
			.putExtra(ViewProfileFragment.EXTRA_USER_ID, user.id)
			.putExtra(ViewProfileFragment.EXTRA_USER_NAME, user.name)
			.putExtra(ViewProfileFragment.EXTRA_USER_PIC, user.profilePictureUrl)
			.putExtra(ViewProfileFragment.EXTRA_USER_PIC_LARGE, user.largeProfilePictureUrl));
	}
	
	public User findUser(int type, long id) {
		User user = null;
		if (type == User.TYPE_TWITTER || type == User.TYPE_UNKNOWN) {
			for (int i = 0; i < mTwitterAccounts.size(); i++) {
				TwitterAccount ta = mTwitterAccounts.get(i);
				user = User.findByID(ta.getFollowing(), id);
				if (user != null) return user;
				user = User.findByID(ta.getFollowers(), id);
				if (user != null) return user;
			}
		}
		if (type == User.TYPE_FACEBOOK || type == User.TYPE_UNKNOWN) {
			for (int i = 0; i < mFacebookAccounts.size(); i++) {
				FacebookAccount fa = mFacebookAccounts.get(i);
				user = User.findByID(fa.getFriends(), id);
				if (user != null) return user;
			}
		}
		return null;
	}
	
	
	public SortedArrayList<User> findUsers(String query) {
		SortedArrayList<User> r = new SortedArrayList<User>(8);
		for (int i = 0; i < mTwitterAccounts.size(); i++) {
			TwitterAccount ta = mTwitterAccounts.get(i);
			findUsers(r, ta.getFollowing(), query);
			findUsers(r, ta.getFollowers(), query);
		}
		for (int i = 0; i < mFacebookAccounts.size(); i++) {
			FacebookAccount fa = mFacebookAccounts.get(i);
			findUsers(r, fa.getFriends(), query);
		}
		return r;
	}
	
	private static void findUsers(SortedArrayList<User> target, ArrayList<User> haystack, String needle) {
		final int l = haystack.size();
		for (int i = 0; i < l; i++) {
			User user = haystack.get(i);
			if (user.name.toLowerCase().indexOf(needle) >= 0) {
				if (!target.contains(user)) {
					target.add(user);
				}
			}
		}
	}
	
	public int getTwitterAccountCount() {
		return mTwitterAccounts.size();
	}
	
	public void removeTwitterAccount(TwitterAccount account) {
		if (mTwitterAccounts.remove(account)) {
			removeAccount(account);
		}
	}
	
	public void addTwitterAccount(TwitterAccount account) {
		mTwitterAccounts.add(account);
		addAccount(account);
	}
	
	public TwitterAccount getTwitterAccount(int index) {
		return mTwitterAccounts.get(index);
	}
	
	public TwitterAccount findTwitterAccountByID(long id) {
		for (int i = 0; i < mTwitterAccounts.size(); i++) {
			if (mTwitterAccounts.get(i).getUser().id == id) {
				return mTwitterAccounts.get(i);
			}
		}
		return null;
	}
	
	public TwitterAccount[] getAllTwitterAccounts() {
		TwitterAccount[] accounts = new TwitterAccount[mTwitterAccounts.size()];
		for (int i = 0; i < accounts.length; i++) {
			accounts[i] = mTwitterAccounts.get(i);
		}
		return accounts;
	}
	

	public int indexOfTwitterAccount(TwitterAccount pTwitterAccount) {
		return mTwitterAccounts.indexOf(pTwitterAccount);
	}
	
	public int getFacebookAccountCount() {
		return mFacebookAccounts.size();
	}
	
	public void addFacebookAccount(FacebookAccount account) {
		mFacebookAccounts.add(account);
		addAccount(account);
		account.setAvailable(mAvailable);
	}
	
	public boolean isAvailable() {
		return mAvailable;
	}
	
	public void setAvailable(boolean pAvailable) {
		mAvailable = pAvailable;
		for (int i = mFacebookAccounts.size() - 1; i >= 0; i--) {
			mFacebookAccounts.get(i).setAvailable(pAvailable);
		}
	}
	
	public void removeFacebookAccount(FacebookAccount account) {
		if (mFacebookAccounts.remove(account)) {
			removeAccount(account);
		}
	}
	
	public FacebookAccount getFacebookAccount(int index) {
		return mFacebookAccounts.get(index);
	}
	
	public FacebookAccount findFacebookAccountByID(long id) {
		for (int i = 0; i < mFacebookAccounts.size(); i++) {
			if (mFacebookAccounts.get(i).getUser().id == id) {
				return mFacebookAccounts.get(i);
			}
		}
		return null;
	}
	
	public FacebookAccount[] getAllFacebookAccounts() {
		FacebookAccount[] accounts = new FacebookAccount[mFacebookAccounts.size()];
		for (int i = 0; i < accounts.length; i++) {
			accounts[i] = mFacebookAccounts.get(i);
		}
		return accounts;
	}
	
	public int indexOfFacebookAccount(FacebookAccount facebookAccount) {
		return mFacebookAccounts.indexOf(facebookAccount);
	}
	
	public IAccount getAccount(int i) {
		return mAccounts.get(i);
	}
	
	public IAccount findAccount(long id) {
		for (int i = mAccounts.size() - 1; i >= 0; i--) {
			IAccount account = mAccounts.get(i);
			if (account.getUser().id == id) {
				return account;
			}
		}
		return null;
	}
	
	public int getAccountCount() {
		return mAccounts.size();
	}
	
	private void addAccount(IAccount account) {
		mAccounts.add(account);
		saveToStorage();
		for (int i = 0; i < mListeners.size(); i++) {
			mListeners.get(i).onAccountAdded(account);
		}
		account.updateGlobalData(null);
	}
	
	private void removeAccount(IAccount account) {
		mAccounts.remove(account);
		saveToStorage();
		for (int i = 0; i < mListeners.size(); i++) {
			mListeners.get(i).onAccountRemoved(account);
		}
	}
	
	public void addListener(AccountsManagerListener listener) {
		mListeners.add(listener);
	}
	
	public void removeListener(AccountsManagerListener listener) {
		mListeners.remove(listener);
	}
	
	private void saveToStorage() {
		StorageBundle[] twitterAccounts = new StorageBundle[mTwitterAccounts.size()];
		for (int i = 0; i < mTwitterAccounts.size(); i++) {
			twitterAccounts[i] = mTwitterAccounts.get(i).getBundle();
		}
		StorageBundle[] facebookAccounts = new StorageBundle[mFacebookAccounts.size()];
		for (int i = 0; i < mFacebookAccounts.size(); i++) {
			facebookAccounts[i] = mFacebookAccounts.get(i).getBundle();
		}
		StorageManager sm = StorageManager.getDeflaut(mContext);
		sm.write("TWITTER_ACCOUNTS", twitterAccounts);
		sm.write("FACEBOOK_ACCOUNTS", facebookAccounts);
		sm.flush();
	}
	
	public static synchronized AccountsManager getInstance(Context c) {
		if (sSingleton == null) {
			sSingleton = new AccountsManager(c.getApplicationContext());
		}
		return sSingleton;
	}
	
	public static interface AccountsManagerListener {
		
		public void onAccountAdded(IAccount account);
		public void onAccountRemoved(IAccount account);
	}

}
