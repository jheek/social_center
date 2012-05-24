package com.jldroid.twook.model.twitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import twitter4j.DirectMessage;
import twitter4j.IDs;
import twitter4j.PagableResponseList;
import twitter4j.Paging;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusUpdate;
import twitter4j.Tweet;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.UserList;
import twitter4j.UserMentionEntity;
import twitter4j.auth.AccessToken;
import android.content.Context;
import android.os.SystemClock;

import com.jdroid.utils.ListUtils;
import com.jdroid.utils.SortedArrayList;
import com.jdroid.utils.StorageManager;
import com.jdroid.utils.StorageManager.StorageBundle;
import com.jdroid.utils.Threads;
import com.jldroid.twook.model.Chat;
import com.jldroid.twook.model.ChatMessage;
import com.jldroid.twook.model.ColumnMessagesProvider;
import com.jldroid.twook.model.IAccount;
import com.jldroid.twook.model.INetworkCallback;
import com.jldroid.twook.model.ISearchableColumn;
import com.jldroid.twook.model.Message;
import com.jldroid.twook.model.MultiNetworkCallbackWrapper;
import com.jldroid.twook.model.SyncManager;
import com.jldroid.twook.model.SyncableData;
import com.jldroid.twook.model.User;
import com.jldroid.twook.model.facebook.FacebookUserProvider;

public class TwitterAccount implements IAccount {
	
	private String mOAuthToken;
	private String mOAuthSecret;
	
	private Twitter mTwitter;
	private twitter4j.User mTwitterUser;
	
	private User mUser;
	
	private ArrayList<TwitterAccountListener> mListeners = new ArrayList<TwitterAccountListener>();
	
	private SyncableData mTimeline;
	private SyncableData mMentions;
	private SyncableData mDirectMessages;
	
	private TwitterStream mTwitterStream;
	
	private boolean mStreamingEnabled = false;
	private boolean mStreaming;
	private long mStreamStartTime;

	private StorageBundle mBundle;
	
	private Context mContext;
	
	private StorageManager mSM;
	
	private int mColor;
	
	private SortedArrayList<User> mFollowers = new SortedArrayList<User>();
	private SortedArrayList<User> mFollowing = new SortedArrayList<User>();
	private boolean mIsUpdatingFriends = false;
	
	private TwitterTimelineProvider mTimelineProvider = new TwitterTimelineProvider(this);
	private TwitterMentionsProvider mMentionsProvider = new TwitterMentionsProvider(this);
	private TwitterDirectMessagesProvider mDirectMessagesProvider = new TwitterDirectMessagesProvider(this);
	
	private ArrayList<TwitterList> mLists = new ArrayList<TwitterList>();
	private boolean mIsUpdatingLists = false;
	
	private ArrayList<SyncableData> mSyncables = new ArrayList<SyncableData>();
	private ArrayList<ColumnMessagesProvider> mProviders = new ArrayList<ColumnMessagesProvider>();
	
	private ArrayList<TwitterUserProvider> mUserProviders = new ArrayList<TwitterUserProvider>();
	
	public TwitterAccount(Context c, long id, String name, String profilePictureUri, String token, String secret) {
		this.mContext = c;
		this.mBundle = new StorageBundle();
		
		setUser(new User(User.TYPE_TWITTER, id, name, profilePictureUri, profilePictureUri));
		setOAuthToken(token);
		setOAuthSecret(secret);
		
		initSyncableData();
		
		updateTimeline(null, false);
		updateMentions(null, false);
		updateDirectMessages(null, false);
	}
	
	// _SYNC_INTERVAL 
	
	public TwitterAccount(Context c, StorageBundle bundle) {
		this.mContext = c;
		this.mBundle = bundle;
		mUser = new User(bundle.readBundle("USER", null));
		mColor = bundle.readInt("COLOR", 0xFFFFFFFF);
		mOAuthToken = bundle.readString("TOKEN", null);
		mOAuthSecret = bundle.readString("SECRET", null);
		
		StorageBundle[] lists = bundle.readBundleArray("LISTS");
		for (int i = 0; i < lists.length; i++) {
			mLists.add(new TwitterList(lists[i]));
		}
		
		StorageBundle[] followers = getStorageManager().readBundleArray("FOLLOWERS");
		StorageBundle[] following = getStorageManager().readBundleArray("FOLLOWING");
		mFollowers.ensureCapacity(followers.length);
		for (int i = 0; i < followers.length; i++) {
			mFollowers.add(new User(followers[i]));
		}
		mFollowing.ensureCapacity(following.length);
		for (int i = 0; i < following.length; i++) {
			mFollowing.add(new User(following[i]));
		}
		
		initSyncableData();
	}
	
	private void initSyncableData() {
		boolean v = getStorageManager().readObj("TIMELINE", null) instanceof StorageBundle;
		StorageBundle timeline = v ? getStorageManager().readBundle("TIMELINE", null) : null;
		StorageBundle mentions = v ? getStorageManager().readBundle("MENTIONS", null) : null;
		StorageBundle directMessages = v ? getStorageManager().readBundle("DIRECT_MESSAGES", null) : null;
		mTimeline = new SyncableData(this, timeline);
		mMentions = new SyncableData(this, mentions);
		mDirectMessages = new SyncableData(this, directMessages);
		if (timeline == null || mentions == null || directMessages == null) {
			timeline = mTimeline.getBundle();
			mentions = mMentions.getBundle();
			directMessages = mDirectMessages.getBundle();
			getStorageManager().deleteAll();
			getStorageManager().write("TIMELINE", timeline);
			getStorageManager().write("MENTIONS", mentions);
			getStorageManager().write("DIRECT_MESSAGES", directMessages);
			getStorageManager().flushAsync();
		}
		createSyncablesAndProviders();
		
		for (int i = mDirectMessages.list.size() - 1; i >= 0; i--) {
			addDirectMessageToChat(mDirectMessages.list.get(i));
		}
	}

	private StorageManager getStorageManager() {
		if (mSM == null) {
			mSM = new StorageManager(mContext, "ta" + mUser.id);
		}
		return mSM;
	}
	
	public String getOAuthToken() {
		return mOAuthToken;
	}

	public void setOAuthToken(String token) {
		this.mOAuthToken = token;
		mBundle.write("TOKEN", mOAuthToken);
	}

	public String getOAuthSecret() {
		return mOAuthSecret;
	}

	public void setOAuthSecret(String secret) {
		this.mOAuthSecret = secret;
		mBundle.write("SECRET", mOAuthSecret);
	}
	
	@Override
	public int getColor() {
		return mColor;
	}
	
	public void setColor(int pColor) {
		mColor = pColor;
		mBundle.write("COLOR", pColor);
	}
	
	public StorageBundle getBundle() {
		return mBundle;
	}
	
	public Twitter getTwitter() throws TwitterException {
		if (mTwitter == null) {
			mTwitter = new TwitterFactory().getInstance();
			mTwitter.setOAuthConsumer(TwitterConfiguration.OAUTH_CONSUMER_KEY, TwitterConfiguration.OAUTH_CONSUMER_SECRET);
			mTwitter.setOAuthAccessToken(new AccessToken(mOAuthToken, mOAuthSecret));
			mTwitter.verifyCredentials();
		}
		return mTwitter;
	}
	
	public User getTwitterUser() throws TwitterException {
		if (mTwitterUser == null) {
			Twitter twitter = getTwitter();
			mTwitterUser = twitter.verifyCredentials();
		}
		return mUser;
	}
	
	@Override
	public User getUser() {
		return mUser;
	}
	
	public void setUser(User pUser) {
		mUser = pUser;
		mBundle.write("USER", pUser.updateBundle());
		StorageManager.getDeflaut(mContext).flushAsync();
	}
	
	@Override
	public ArrayList<ColumnMessagesProvider> getProviders() {
		return mProviders;
	}
	
	private void createSyncablesAndProviders() {
		mSyncables.clear();
		mProviders.clear();
		mSyncables.add(mTimeline);
		mSyncables.add(mMentions);
		mSyncables.add(mDirectMessages);
		mProviders.add(mTimelineProvider);
		mProviders.add(mMentionsProvider);
		mProviders.add(mDirectMessagesProvider);
		
		Collections.sort(mLists);
		for (int i = 0; i < mLists.size(); i++) {
			TwitterList list = mLists.get(i);
			String storageName = "LIST_" + list.name;
			StorageBundle bundle = getStorageManager().readBundle(storageName, null);
			SyncableData syncable = new SyncableData(this, bundle);
			if (bundle == null) {
				bundle = syncable.getBundle();
				getStorageManager().write(storageName, bundle);
				getStorageManager().flushAsync();
			}
			TwitterListProvider provider = new TwitterListProvider(this, syncable, list);
			mSyncables.add(syncable);
			mProviders.add(provider);
		}
	}
	
	public TwitterTimelineProvider getTimelineProvider() {
		return mTimelineProvider;
	}
	
	public TwitterMentionsProvider getMentionsProvider() {
		return mMentionsProvider;
	}
	
	public TwitterDirectMessagesProvider getDirectMessagesProvider() {
		return mDirectMessagesProvider;
	}
	
	public SyncableData getTimeline() {
		return mTimeline;
	}
	
	public SyncableData getMentions() {
		return mMentions;
	}
	
	public SyncableData getDirectMessages() {
		return mDirectMessages;
	}
	
	public boolean isStreaming() {
		return mStreaming;
	}
	
	public boolean isStreamingEnabled() {
		return mStreamingEnabled;
	}
	
	public void setStreamingEnabled(boolean v) {
		this.mStreamingEnabled = v;
		if (v && !mStreaming) {
			startStreaming();
		} else if (!v && mStreaming) {
			stopStreaming();
		}
	}

	private boolean createStream() {
		 try {
	    	TwitterStream twitterStream = new TwitterStreamFactory().getInstance();
		    twitterStream.addListener(new MyUserStreamListener());
		    twitterStream.setOAuthConsumer(TwitterConfiguration.OAUTH_CONSUMER_KEY, TwitterConfiguration.OAUTH_CONSUMER_SECRET);
			twitterStream.setOAuthAccessToken(getTwitter().getOAuthAccessToken());
			this.mTwitterStream = twitterStream;
			return true;
		 } catch (TwitterException e) {
			 e.printStackTrace(); // TODO add error handling
		 }
		 return false;
	}
	
	private void startStreaming() {
		if (this.mTwitterStream == null) {
			createStream();
		}
		if (this.mTwitterStream != null && !mStreaming) {
			this.mTwitterStream.user();
			this.mStreaming = true;
			this.mStreamStartTime = SystemClock.elapsedRealtime();
		}
	}
	
	private void stopStreaming() {
		if (this.mTwitterStream != null && mStreaming) {
			this.mTwitterStream.cleanUp();
			this.mStreaming = false;
		}
	}
	
	@Override
	public void updateGlobalData(INetworkCallback pCallback) {
		MultiNetworkCallbackWrapper wrapper = new MultiNetworkCallbackWrapper(pCallback, 2);
		updateFriends(wrapper);
		updateLists(wrapper);
	}
	
	public void updateLists(final INetworkCallback callback) {
		if (mIsUpdatingLists) {
			if (callback != null) callback.onSucceed(TwitterAccount.this);
			return;
		}
		mIsUpdatingLists = true;
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				try {
					final ArrayList<UserList> allLists = new ArrayList<UserList>();
					PagableResponseList<UserList> lists = null;
					while (lists == null || lists.hasNext()) {
						lists = getTwitter().getUserLists(mUser.id, lists != null ? lists.getNextCursor() : -1);
						allLists.addAll(lists);
					}
					Threads.runOnUIThread(new Runnable() {
						@Override
						public void run() {
							mLists.clear();
							for (int i = 0; i < allLists.size(); i++) {
								UserList list = allLists.get(i);
								mLists.add(new TwitterList(list.getId(), list.getName()));
							}
							
							StorageBundle[] bundles = new StorageBundle[mLists.size()];
							for (int i = 0; i < bundles.length; i++){
								bundles[i] = mLists.get(i).updateBundle();
							}
							mBundle.write("LISTS", bundles);
							StorageManager.getDeflaut(mContext).flushAsync();
							
							createSyncablesAndProviders();
							SyncManager.updateAccountColumnsSync(mContext, TwitterAccount.this);
							dispatchProvidersChanged();
							
							mIsUpdatingLists = false;
							if (callback != null) callback.onSucceed(TwitterAccount.this);
						}
					});
				} catch (TwitterException e) {
					e.printStackTrace();
					mIsUpdatingLists = false;
					if (callback != null) callback.onFailed(TwitterAccount.this);
				}
			}
		});
	}
	
	public SortedArrayList<User> getFollowers() {
		return mFollowers;
	}
	
	public SortedArrayList<User> getFollowing() {
		return mFollowing;
	}
	
	private void parseIDs(SortedArrayList<User> target, long[] IDs) throws TwitterException {
		long[] tmp = new long[100];
		for (int i = 0; i < IDs.length; i += 100) {
			int l = Math.min(100, IDs.length - i);
			System.arraycopy(IDs, i, tmp, 0, l);
			ResponseList<twitter4j.User> users = getTwitter().lookupUsers(tmp);
			for (int i2 = 0; i2 < users.size(); i2++) {
				target.add(new User(null, users.get(i2), false));
			}
		}
	}
	
	public boolean isUpdatingFriends() {
		return mIsUpdatingFriends;
	}
	
	public void updateFriends(final INetworkCallback callback) {
		if (mIsUpdatingFriends) {
			callback.onSucceed(this);
			return;
		}
		mIsUpdatingFriends = true;
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				try {
					final SortedArrayList<User> followers = new SortedArrayList<User>();
					IDs followerIds = null;
					while (followerIds == null || followerIds.hasNext()) {
						followerIds = getTwitter().getFollowersIDs(mUser.id, followerIds != null ? followerIds.getNextCursor() : -1);
						parseIDs(followers, followerIds.getIDs());
					}
					StorageBundle[] bundles = new StorageBundle[followers.size()];
					for (int i = 0; i < followers.size(); i++) {
						bundles[i] = followers.get(i).updateBundle();
					}
					getStorageManager().write("FOLLOWERS", bundles);
					final SortedArrayList<User> following = new SortedArrayList<User>();
					IDs followingIds = null;
					while (followingIds == null || followingIds.hasNext()) {
						followingIds = getTwitter().getFriendsIDs(mUser.id, followingIds != null ? followingIds.getNextCursor() : -1);
						parseIDs(following, followingIds.getIDs());
					}
					bundles = new StorageBundle[following.size()];
					for (int i = 0; i < following.size(); i++) {
						bundles[i] = following.get(i).updateBundle();
					}
					getStorageManager().write("FOLLOWING", bundles);
					getStorageManager().flushAsync();
					Threads.runOnUIThread(new Runnable() {
						@Override
						public void run() {
							ListUtils.clone(followers, mFollowers);
							ListUtils.clone(following, mFollowing);
							mIsUpdatingFriends = false;
							callback.onSucceed(TwitterAccount.this);
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
					mIsUpdatingFriends = false;
					callback.onFailed(TwitterAccount.this);
				}
			}
		});
	}
	
	public void updateTimeline(final INetworkCallback callback, final boolean enlarge) {
		if (enlarge) {
			if(mTimeline.isEnlarging) {
				if (callback != null) callback.onSucceed(this);
				return;
			}
			mTimeline.isEnlarging = true;
		} else {
			if ((mStreaming && mStreamStartTime < mTimeline.getLastUpdate()) || mTimeline.isUpdating ) {
				if (callback != null) callback.onSucceed(this);
				return;
			}
			mTimeline.isUpdating = true;
		}
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				Paging paging = new Paging();
				paging.setPage(1);
				if (mTimeline.list.size() == 0) {
					paging.setCount(30);
				} else if (enlarge) {
					paging.setCount(30);
					paging.setMaxId(mTimeline.list.get(mTimeline.list.size() - 1).ID - 1);
				} else {
					paging.setCount(200);
					paging.setSinceId(mTimeline.list.get(0).ID + 1);
				}
				try {
					final ResponseList<Status> updates = getTwitter().getHomeTimeline(paging);
					Threads.runOnAsyncThread(new Runnable() {
						@Override
						public void run() {
							final int l = updates.size();
							if (l != 200) {
								ListUtils.clone(mTimeline.list, mTimeline.newList);
							}
							for (int i = 0; i < l; i++) {
								Message msg = Message.parseTwitterMessage(TwitterAccount.this, updates.get(i));
								if (!mTimeline.newList.contains(msg)) {
									msg.updateBundle();
									mTimeline.newList.add(msg);
								}
							}
							Threads.runOnUIThread(new Runnable() {
								@Override
								public void run() {
									mTimeline.swap();
									
									mTimeline.setLastUpdate(System.currentTimeMillis());
									
									if (enlarge && l == 0) {
										mTimeline.hasOlder = false;
									}
									if (enlarge) 
										mTimeline.isEnlarging = false;
									else 
										mTimeline.isUpdating = false;
									
									dispatchTimelineChanged(enlarge ? 0 : l);
									mTimeline.isUpdating = false;
									if (callback != null) callback.onSucceed(TwitterAccount.this);
								}
							});
						}
					});
				} catch (TwitterException e) {
					e.printStackTrace();
					if (enlarge)
						mTimeline.isEnlarging = false;
					else 
						mTimeline.isUpdating = false;
					if (callback != null) callback.onFailed(TwitterAccount.this);
				}
			}
		});
	}
	
	public void updateMentions(final INetworkCallback callback, final boolean enlarge) {
		if (enlarge) {
			if(mMentions.isEnlarging) {
				if (callback != null) callback.onSucceed(this);
				return;
			}
			mMentions.isEnlarging = true;
		} else {
			if ((mStreaming && mStreamStartTime < mMentions.getLastUpdate()) || mMentions.isUpdating ) {
				if (callback != null) callback.onSucceed(this);
				return;
			}
			mMentions.isUpdating = true;
		}
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				Paging paging = new Paging();
				paging.setPage(1);
				if (mMentions.list.size() == 0) {
					paging.setCount(30);
				} else if (enlarge) {
					paging.setCount(30);
					paging.setMaxId(mMentions.list.get(mMentions.list.size() - 1).ID - 1);
				} else {
					paging.setCount(200);
					paging.setSinceId(mMentions.list.get(0).ID + 1);
				}
				try {
					final ResponseList<Status> updates = getTwitter().getMentions(paging);
					Threads.runOnAsyncThread(new Runnable() {
						@Override
						public void run() {
							final int l = updates.size();
							if (l != 200) {
								ListUtils.clone(mMentions.list, mMentions.newList);
							}
							for (int i = 0; i < l; i++) {
								Message msg = Message.parseTwitterMessage(TwitterAccount.this, updates.get(i));
								if (!mMentions.newList.contains(msg)) {
									msg.updateBundle();
									mMentions.newList.add(msg);
								}
							}
							Threads.runOnUIThread(new Runnable() {
								@Override
								public void run() {
									mMentions.swap();
									
									mMentions.setLastUpdate(System.currentTimeMillis());
									
									if (enlarge && l == 0) {
										mMentions.hasOlder = false;
									}
									if (enlarge) 
										mMentions.isEnlarging = false;
									else 
										mMentions.isUpdating = false;
									
									dispatchMentionsChanged(enlarge ? 0 : l);
									mMentions.isUpdating = false;
									if (callback != null) callback.onSucceed(TwitterAccount.this);
								}
							});
						}
					});
				} catch (TwitterException e) {
					e.printStackTrace();
					if (enlarge) 
						mMentions.isEnlarging = false;
					else 
						mMentions.isUpdating = false;
					if (callback != null) callback.onFailed(TwitterAccount.this);
				}
			}
		});
	}
	
	public void updateDirectMessages(final INetworkCallback callback, final boolean enlarge) {
		if (enlarge) {
			if(mDirectMessages.isEnlarging) {
				if (callback != null) callback.onSucceed(this);
				return;
			}
			mDirectMessages.isEnlarging = true;
		} else {
			if ((mStreaming && mStreamStartTime < mDirectMessages.getLastUpdate()) || mDirectMessages.isUpdating ) {
				if (callback != null) callback.onSucceed(this);
				return;
			}
			mDirectMessages.isUpdating = true;
		}
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				Paging paging = new Paging();
				paging.setPage(1);
				if (mDirectMessages.list.size() == 0) {
					paging.setCount(30);
				} else if (enlarge) {
					paging.setCount(30);
					paging.setMaxId(mDirectMessages.list.get(mDirectMessages.list.size() - 1).ID - 1);
				} else {
					paging.setCount(200);
					paging.setSinceId(mDirectMessages.list.get(0).ID + 1);
				}
				try {
					final ResponseList<DirectMessage> receivedMsgs = getTwitter().getDirectMessages(paging);
					final ResponseList<DirectMessage> sentMsgs = getTwitter().getSentDirectMessages(paging);
					Threads.runOnAsyncThread(new Runnable() {
						@Override
						public void run() {
							ListUtils.clone(mDirectMessages.list, mDirectMessages.newList);
							final int l = receivedMsgs.size();
							for (int i = 0; i < l; i++) {
								Message msg = Message.parseTwitterDirectMessage(TwitterAccount.this, receivedMsgs.get(i));
								if (!mDirectMessages.newList.contains(msg)) {
									msg.updateBundle();
									mDirectMessages.newList.add(msg);
									addDirectMessageToChat(msg);
								}
							}
							final int l2 = sentMsgs.size();
							for (int i = 0; i < l2; i++) {
								Message msg = Message.parseTwitterDirectMessage(TwitterAccount.this, sentMsgs.get(i));
								if (!mDirectMessages.newList.contains(msg)) {
									msg.updateBundle();
									mDirectMessages.newList.add(msg);
									addDirectMessageToChat(msg);
								}
							}
							final int totalL = l + l2;
							Threads.runOnUIThread(new Runnable() {
								@Override
								public void run() {
									mDirectMessages.swap();
									
									mDirectMessages.setLastUpdate(System.currentTimeMillis());
									
									if (enlarge && totalL == 0) {
										mDirectMessages.hasOlder = false;
									}
									if (enlarge) 
										mDirectMessages.isEnlarging = false;
									else 
										mDirectMessages.isUpdating = false;
									
									dispatchDirectMessagesChanged(enlarge ? 0 : totalL);
									
									if (callback != null) callback.onSucceed(TwitterAccount.this);
								}
							});
						}
					});
				} catch (TwitterException e) {
					e.printStackTrace();
					if (enlarge) 
						mDirectMessages.isEnlarging = false;
					else 
						mDirectMessages.isUpdating = false;
					if (callback != null) callback.onFailed(TwitterAccount.this);
				}
			}
		});
	}
	
	public void updateUserList(final INetworkCallback callback, int id, boolean enlarge) {
		for (int i = 0; i < mProviders.size(); i++) {
			if (mProviders.get(i) instanceof TwitterListProvider) {
				TwitterListProvider listProvider = (TwitterListProvider) mProviders.get(i);
				if (listProvider.mList.id == id) {
					updateUserList(callback, listProvider, enlarge);
					return;
				}
			}
		}
		callback.onSucceed(this);
	}
	
	public void updateUserList(final INetworkCallback callback, final TwitterListProvider provider, final boolean enlarge) {
		SyncableData syncable = provider.mSyncable;
		if (enlarge) {
			if (syncable.isEnlarging) {
				if (callback != null) callback.onSucceed(this);
				return;
			}
			syncable.isEnlarging = true;
		} else {
			if (syncable.isUpdating) {
				if (callback != null) callback.onSucceed(this);
				return;
			}
			syncable.isUpdating = true;
		}
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				final SyncableData syncable = provider.mSyncable;
				final TwitterList list = provider.mList;
				Paging paging = new Paging();
				paging.setPage(1);
				if (syncable.list.size() == 0) {
					paging.setCount(30);
				} else if (enlarge) {
					paging.setCount(30);
					paging.setMaxId(syncable.list.get(syncable.list.size() - 1).ID);
				} else {
					paging.setCount(200);
					paging.setSinceId(syncable.list.get(0).ID);
				}
				try {
					final ResponseList<Status> updates = getTwitter().getUserListStatuses(list.id, paging);
					Threads.runOnAsyncThread(new Runnable() {
						@Override
						public void run() {
							final int l = updates.size();
							if (l == 200) {
								syncable.newList.clear();
							} else {
								ListUtils.clone(syncable.list, syncable.newList);
							}
							for (int i = 0; i < l; i++) {
								Message msg = Message.parseTwitterMessage(TwitterAccount.this, updates.get(i));
								if (!syncable.newList.contains(msg)) {
									msg.updateBundle();
									syncable.newList.add(msg);
								}
							}
							Threads.runOnUIThread(new Runnable() {
								@Override
								public void run() {
									syncable.swap();
									syncable.updateMessages();
									getStorageManager().flushAsync();
									
									if (enlarge && l == 0) {
										syncable.hasOlder = false;
									}
									
									if (enlarge) {
										syncable.isEnlarging = false;
									} else {
										syncable.isUpdating = false;
									}
									
									provider.addUnreadMessages(enlarge ? 0 : l);
									provider.dispatchChange();
									if (callback != null) callback.onSucceed(TwitterAccount.this);
								}
							});
						}
					});
				} catch (TwitterException e) {
					e.printStackTrace();
					if (enlarge) {
						syncable.isEnlarging = false;
					} else {
						syncable.isUpdating = false;
					}
					if (callback != null) callback.onFailed(TwitterAccount.this);
				}
			}
		});
	}
	
	public void updateUserTimeline(final INetworkCallback callback, 
			final TwitterUserProvider column, final User user, final SyncableData syncable, final boolean enlarge) {
		
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				Paging paging = new Paging();
				paging.setPage(1);
				if (syncable.list.size() == 0) {
					paging.setCount(30);
				} else if (enlarge) {
					paging.setCount(30);
					paging.setMaxId(syncable.list.get(syncable.list.size() - 1).ID);
				} else {
					paging.setCount(200);
					paging.setSinceId(syncable.list.get(0).ID);
				}
				try {
					final ResponseList<Status> updates = getTwitter().getUserTimeline(user.id, paging);
					Threads.runOnAsyncThread(new Runnable() {
						@Override
						public void run() {
							final int l = updates.size();
							if (l < 200) {
								ListUtils.clone(syncable.list, syncable.newList);
							}
							for (int i = 0; i < l; i++) {
								Message msg = Message.parseTwitterMessage(TwitterAccount.this, updates.get(i));
								if (!syncable.newList.contains(msg)) {
									msg.updateBundle();
									syncable.newList.add(msg);
								}
							}
							Threads.runOnUIThread(new Runnable() {
								@Override
								public void run() {
									if (enlarge && l == 0) {
										syncable.hasOlder = false;
									}
									syncable.swap();
									column.addUnreadMessages(enlarge ? 0 : l);
									column.dispatchChange();
									if (callback != null) callback.onSucceed(TwitterAccount.this);
								}
							});
						}
					});
				} catch (TwitterException e) {
					e.printStackTrace();
					if (callback != null) callback.onFailed(TwitterAccount.this);
				}
			}
		});
	}
	
	public void updateSearch(final INetworkCallback callback, final TwitterSearchColumn column, final SyncableData syncable, final boolean enlarge) {
		if (enlarge) {
			if(syncable.isEnlarging) {
				if (callback != null) callback.onSucceed(this);
				return;
			}
			syncable.isEnlarging = true;
		} else {
			if ((mStreaming && mStreamStartTime < syncable.getLastUpdate()) || syncable.isUpdating ) {
				if (callback != null) callback.onSucceed(this);
				return;
			}
			syncable.isUpdating = true;
		}
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				Paging paging = new Paging();
				paging.setPage(1);
				if (syncable.list.size() == 0) {
					paging.setCount(30);
				} else if (enlarge) {
					paging.setCount(30);
					paging.setMaxId(syncable.list.get(syncable.list.size() - 1).ID - 1);
				} else {
					paging.setCount(200);
					paging.setSinceId(syncable.list.get(0).ID + 1);
				}
				try {
					final QueryResult result = getTwitter().search(new Query(column.getQuery()));
					Threads.runOnAsyncThread(new Runnable() {
						@Override
						public void run() {
							List<Tweet> updates = result.getTweets();
							final int l = updates.size();
							if (l != 200) {
								ListUtils.clone(syncable.list, syncable.newList);
							}
							for (int i = 0; i < l; i++) {
								Message msg = Message.parseTwitterTweet(TwitterAccount.this, updates.get(i));
								if (!syncable.newList.contains(msg)) {
									msg.updateBundle();
									syncable.newList.add(msg);
								}
							}
							Threads.runOnUIThread(new Runnable() {
								@Override
								public void run() {
									syncable.swap();
									
									syncable.setLastUpdate(System.currentTimeMillis());
									
									if (enlarge && l == 0) {
										syncable.hasOlder = false;
									}
									if (enlarge) 
										syncable.isEnlarging = false;
									else 
										syncable.isUpdating = false;
									
									dispatchTimelineChanged(enlarge ? 0 : l);
									syncable.isUpdating = false;
									if (callback != null) callback.onSucceed(TwitterAccount.this);
								}
							});
						}
					});
				} catch (TwitterException e) {
					e.printStackTrace();
					if (enlarge)
						syncable.isEnlarging = false;
					else 
						syncable.isUpdating = false;
					if (callback != null) callback.onFailed(TwitterAccount.this);
				}
			}
		});
	}
	
	@Override
	public ISearchableColumn createSearchColumn(String pQuery) {
		return new TwitterSearchColumn(this, pQuery, null);
	}
	
	public void searchLocal(SortedArrayList<Message> target, String query) {
		query = query.toLowerCase();
		for (int i = 0; i < mSyncables.size(); i++) {
			SyncableData syncable = mSyncables.get(i);
			searchLocal(target, syncable.list, query);
		}
	}
	
	private static void searchLocal(SortedArrayList<Message> target, SortedArrayList<Message> haystack, String needle) {
		for (int i = haystack.size() - 1; i >= 0; i--) {
			Message msg = haystack.get(i);
			if (msg.text.toLowerCase().contains(needle) && !target.contains(msg)) {
				target.add(msg);
			}
		}
	}

	@Override
	public boolean searchPeople(SortedArrayList<User> pTarget, String pQuery) {
		try {
			ResponseList<twitter4j.User> users = getTwitter().searchUsers(pQuery, 0);
			for (int i = users.size() - 1; i >= 0; i--) {
				User user = new User(null, users.get(i), false);
				if (!pTarget.contains(user)) {
					pTarget.add(user);
				}
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	@Override
	public void searchPeopleLocal(SortedArrayList<User> pTarget, String pQuery) {
		pQuery = pQuery.toLowerCase();
		User.findByQuery(pTarget, mFollowers, pQuery);
		User.findByQuery(pTarget, mFollowing, pQuery);
	}
	
	public User loadUserInfo(long id) {
		try {
			twitter4j.User user4j = getTwitter().showUser(id);
			User user = new User(mContext, user4j, true);
			return user;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void deleteSavedMessages() {
		mTimeline.clear();
		mMentions.clear();
		mDirectMessages.clear();
		StorageManager sm = getStorageManager();
		sm.flushAsync();
		dispatchTimelineChanged(0);
		dispatchMentionsChanged(0);
		dispatchDirectMessagesChanged(0);
	}
	
	public void addListener(TwitterAccountListener listener) {
		this.mListeners.add(listener);
	}
	
	public boolean removeListener(TwitterAccountListener listener) {
		return this.mListeners.remove(listener);
	}
	
	// TODO add status to timeline
	public boolean updateStatus(final StatusUpdate update) {
		try {
			getTwitter().updateStatus(update);
			return true;
		} catch (TwitterException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean retweetStatus(final long statusID) {
		try {
			getTwitter().retweetStatus(statusID);
			return true;
		} catch (TwitterException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	protected SortedArrayList<Chat> mChats = new SortedArrayList<Chat>();
	
	protected long getChatID(Message msg) {
		return msg.sender.id != mUser.id ? msg.sender.id : msg.target.id;
	}
	
	protected void addDirectMessageToChat(Message msg) {
		long chatID = getChatID(msg);
		ChatMessage chatMsg = new ChatMessage(this, msg);
		for (int i = mChats.size() - 1; i >= 0; i--) {
			Chat chat = mChats.get(i);
			if (chat.getID() == chatID) {
				chat.addMessage(chatMsg, true);
				Collections.sort(mChats);
				return;
			}
		}
		Chat chat = new Chat(this, chatID);
		chat.addMessage(chatMsg, true);
		mChats.add(chat);
	}
	
	@Override
	public SortedArrayList<Chat> getChats() {
		return mChats;
	}
	
	@Override
	public Chat findMessageChat(Message pMsg) {
		long chatID = getChatID(pMsg);
		for (int i = mChats.size() - 1; i >= 0; i--) {
			Chat chat = mChats.get(i);
			if (chat.getID() == chatID) {
				return chat;
			}
		}
		return null;
	}
	
	@Override
	public boolean sendChatMsg(Chat pChat, String pText) {
		return sendDirectMessage(pChat.getID(), pText);
	}
	
	@Override
	public ColumnMessagesProvider addUserProvider(User user) {
		TwitterUserProvider provider = new TwitterUserProvider(this, user);
		mUserProviders.add(provider);
		return provider;
	}
	@Override
	public void removeUserProvider(ColumnMessagesProvider provider) {
		mUserProviders.remove(provider);
	}
	
	@Override
	public Message findMessage(long msgPreID, long msgID, int type) {
		Message msg;
		for (int i = mUserProviders.size() - 1; i >= 0; i--) {
			msg = findMessage(mUserProviders.get(i).getMessages(), msgPreID, msgID, type);
			if (msg != null) {
				return msg;
			}
		}
		for (int i = mProviders.size() - 1; i >= 0; i--) {
			msg = findMessage(mProviders.get(i).getMessages(), msgPreID, msgID, type);
			if (msg != null) {
				return msg;
			}
		}
		return null;
	}
	
	public static Message findMessage(SortedArrayList<Message> msgs, long msgPreID, long msgID, int type) {
		final int l = msgs.size();
		for (int i = 0; i < l; i++) {
			Message msg = msgs.get(i);
			if (msg.type == type && msg.PRE_ID == msgPreID && msg.ID == msgID) {
				return msg;
			}
		}
		return null;
	}
	
	@Override
	public User findUser(long id) {
		User user = User.findByID(mFollowers, id);
		if (user != null) {
			return user;
		}
		return User.findByID(mFollowing, id);
	}
	
	private Runnable mUpdateTimelineStorage = new Runnable() {
		@Override
		public void run() {
			mTimeline.updateMessages();
			StorageManager sm = getStorageManager();
			sm.flushAsync();
		}
	};
	
	private Runnable mUpdateMentionsStorage = new Runnable() {
		@Override
		public void run() {
			mMentions.updateMessages();
			StorageManager sm = getStorageManager();
			sm.flushAsync();
		}
	};
	
	private Runnable mUpdateDirectMessagesStorage = new Runnable() {
		@Override
		public void run() {
			mDirectMessages.updateMessages();
			StorageManager sm = getStorageManager();
			sm.flushAsync();
		}
	};
	
	protected void dispatchTimelineChanged(int newCount) {
		for (int i = mListeners.size() - 1; i >= 0; i--) {
			mListeners.get(i).onTimelineChanged(newCount);
		}
		Threads.runOnAsyncThread(mUpdateTimelineStorage);
	}
	protected void dispatchMentionsChanged(int newCount) {
		for (int i = mListeners.size() - 1; i >= 0; i--) {
			mListeners.get(i).onMentionsChanged(newCount);
		}
		Threads.runOnAsyncThread(mUpdateMentionsStorage);
	}
	protected void dispatchDirectMessagesChanged(int newCount) {
		for (int i = mListeners.size() - 1; i >= 0; i--) {
			mListeners.get(i).onDirectMessagesChanged(newCount);
		}
		Threads.runOnAsyncThread(mUpdateDirectMessagesStorage);
	}
	
	protected void dispatchProvidersChanged() {
		for (int i = mListeners.size() - 1; i >= 0; i--) {
			mListeners.get(i).onProvidersChanged(this);
		}
	}
	
	public boolean sendDirectMessage(long target, String text) {
		try {
			DirectMessage directMsg = getTwitter().sendDirectMessage(target, text);
			Message msg = Message.parseTwitterDirectMessage(this, directMsg);
			addDirectMessageToChat(msg);
			return true;
		} catch (TwitterException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private class MyUserStreamListener extends SimpleUserStreamListener {
		@Override
		public void onDeletionNotice(final StatusDeletionNotice arg0) {
			Threads.runOnUIThread(new Runnable() {
				@Override
				public void run() {
					long id = arg0.getStatusId();
		        	final int l = mTimeline.list.size();
		        	for (int i = 0; i < l; i++) {
		        		if (mTimeline.list.get(i).ID == id) {
		        			mTimeline.list.remove(i);
		        			mTimeline.setLastUpdate(System.currentTimeMillis());
		        			dispatchTimelineChanged(0);
		        			break;
		        		}
		        	}
				}
			});
		}
		
		@Override
		public void onStatus(final Status status) {
			Threads.runOnUIThread(new Runnable() {
				@Override
				public void run() {
					boolean isMention = false;
					UserMentionEntity[] mentions = status.getUserMentionEntities();
					if (mentions != null) {
						for (int i=0; i < mentions.length; i++) {
							if (mUser.id == mentions[i].getId()) {
								isMention = true;
								break;
							}
						}
					}
					Message msg = Message.parseTwitterMessage(TwitterAccount.this, status);
					if (isMention) {
						msg.type = Message.TYPE_TWITTER_MENTION;
						mMentions.list.add(msg);
						dispatchMentionsChanged(1);
					} else {
						msg.type = Message.TYPE_TWITTER_TIMELINE;
						mTimeline.list.add(msg);
						dispatchTimelineChanged(1);
					}
				}
			});
		}
		
		@Override
		public void onDirectMessage(DirectMessage arg0) {
			// TODO
		}
	}
	
	public static interface TwitterAccountListener {
		public void onTimelineChanged(int newCount);
		public void onMentionsChanged(int newCount);
		public void onDirectMessagesChanged(int newCount);
		
		public void onProvidersChanged(TwitterAccount account);
	}
}
