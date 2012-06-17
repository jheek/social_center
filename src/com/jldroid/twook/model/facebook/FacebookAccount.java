package com.jldroid.twook.model.facebook;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.android.Facebook;
import com.jdroid.utils.ListUtils;
import com.jdroid.utils.SortedArrayList;
import com.jdroid.utils.StorageManager;
import com.jdroid.utils.StorageManager.StorageBundle;
import com.jdroid.utils.Threads;
import com.jldroid.twook.ChatService;
import com.jldroid.twook.ChatUtils;
import com.jldroid.twook.R;
import com.jldroid.twook.model.BaseColumnMessagesProvider;
import com.jldroid.twook.model.Chat;
import com.jldroid.twook.model.ChatMessage;
import com.jldroid.twook.model.ColumnMessagesProvider;
import com.jldroid.twook.model.IAccount;
import com.jldroid.twook.model.INetworkCallback;
import com.jldroid.twook.model.ISearchableColumn;
import com.jldroid.twook.model.Message;
import com.jldroid.twook.model.MultiNetworkCallbackWrapper;
import com.jldroid.twook.model.NetworkCallbackEnlargeStateWrapper;
import com.jldroid.twook.model.NetworkCallbackUpdateStateWrapper;
import com.jldroid.twook.model.SyncManager;
import com.jldroid.twook.model.SyncableData;
import com.jldroid.twook.model.SyncableData.BaseSyncableDataProvider;
import com.jldroid.twook.model.User;

public class FacebookAccount implements IAccount, PacketListener {

	private static final String USER_QUERY = "SELECT uid, name, pic_big, pic_square, religion, birthday, sex, hometown_location, relationship_status, quotes, about_me, website, email, friend_count, mutual_friend_count FROM user WHERE uid = ";
	private static final String PAGE_QUERY = "SELECT page_id, name, pic_big, pic_square, description, fan_count, website, general_info, phone FROM page WHERE page_id = ";
	
	private static final String USER_ALBUMS_QUERY = "SELECT aid, cover_pid, name, created, modified, can_upload, photo_count, video_count, cover_object_id FROM album WHERE owner = ";
	private static final String ALBUM_PHOTOS_QUERY = "SELECT pid, src, src_small, src_big, object_id, created, modified FROM photo WHERE aid = \"";
	
	private static final String PHOTO_MESSAGE_QUERY = "{\"likes\" : \"SELECT '' FROM like WHERE object_id = '%s'\",  \"comments\" : \"SELECT id, fromid, text, time, likes, user_likes FROM comment WHERE object_id = '%s'\", \"usernames\" : \"SELECT id, name, pic_square, pic_big FROM profile WHERE id IN (SELECT fromid FROM #comments)\"}";
	
	private static final String PHOTO_QUERY = "SELECT pid, src, src_small, src_big, object_id, created, modified FROM photo WHERE pid = \"";
	
	private String mOAuthToken;
	
	private Facebook mFacebook;
	
	private User mUser;
	private long mLastUserUpdate = -1;
	
	private ArrayList<FacebookAccountListener> mListeners = new ArrayList<FacebookAccountListener>();
	
	private SyncableData mHomeUpdates;
	private SyncableData mNotifications;
	private SyncableData mMessages;
	
	private boolean mIsUpdatingLists = false;
	private ArrayList<FacebookList> mLists = new ArrayList<FacebookList>();
	
	private boolean mIsUpdatingGroups = false;
	private ArrayList<FacebookGroup> mGroups = new ArrayList<FacebookGroup>();
	
	private SortedArrayList<User> mFriends = new SortedArrayList<User>();
	private boolean mIsUpdatingFriends = false;
	
	private ArrayList<FacebookUserProvider> mUserProviders = new ArrayList<FacebookUserProvider>();
	
	private ArrayList<FacebookSearchColumn> mSearchProviders = new ArrayList<FacebookSearchColumn>();
	
	private BaseSyncableDataProvider mHomeProvider = new BaseSyncableDataProvider() {
		
		protected SyncableData getData() {
			return mHomeUpdates;
		};
		
		@Override
		public long getID() {
			return -1;
		}
		
		@Override
		public int getWhat() {
			return SyncManager.WHAT_FACEBOOK_HOME;
		};
		
		@Override
		public void requestUpdate(final INetworkCallback pCallback) {
			setIsUpdating(true);
			updateHomeAsync(new NetworkCallbackUpdateStateWrapper(this, pCallback), false);
		}
		
		@Override
		public void requestOlderMessages(final INetworkCallback pCallback) {
			setIsEnlarging(true);
			updateHomeAsync(new NetworkCallbackEnlargeStateWrapper(this, pCallback), true);
		}
		
		@Override
		public boolean isStreaming() {
			return false;
		}
		
		@Override
		public String getName(Context c) {
			return c.getString(R.string.fb_newsfeed_name, getUser().name);
		}

		@Override
		public String getDescription(Context c) {
			return c.getString(R.string.fb_newsfeed_description);
		};
		
		public int getOrder() {
			return 0;
		};
		
		@Override
		public String getStorageName() {
			return "fbnewsfeed" + getUser().id;
		}
		
		public boolean isPersonal() {
			return false;
		};
	};
	
	private BaseSyncableDataProvider mNotificationsProvider = new BaseSyncableDataProvider() {
		
		protected SyncableData getData() {
			return mNotifications;
		};
		
		@Override
		public long getID() {
			return -1;
		}
		
		@Override
		public int getWhat() {
			return SyncManager.WHAT_FACEBOOK_NOTIFICATIONS;
		};
		
		@Override
		public void requestUpdate(final INetworkCallback pCallback) {
			setIsUpdating(true);
			updateNotificationsAsync(new NetworkCallbackUpdateStateWrapper(this, pCallback), false);
		}
		
		@Override
		public void requestOlderMessages(final INetworkCallback pCallback) {
			setIsEnlarging(true);
			updateNotificationsAsync(new NetworkCallbackEnlargeStateWrapper(this, pCallback), true);
		}
		
		@Override
		public boolean isStreaming() {
			return false;
		}
		
		@Override
		public String getName(Context c) {
			return c.getString(R.string.fb_notifications_name, getUser().name);
		}
		
		@Override
		public String getDescription(Context c) {
			return c.getString(R.string.fb_notifications_description, getUser().name);
		};
		
		public int getOrder() {
			return 1;
		};
		
		@Override
		public String getStorageName() {
			return "fbnotifications" + getUser().id;
		}
		
		@Override
		public boolean isPersonal() {
			return true;
		}
	};
	
	private BaseSyncableDataProvider mMessagesProvider = new BaseSyncableDataProvider() {
		
		protected SyncableData getData() {
			return mMessages;
		};
		
		@Override
		public long getID() {
			return -1;
		}
		
		@Override
		public int getWhat() {
			return SyncManager.WHAT_FACEBOOK_MESSAGES;
		};
		
		@Override
		public void requestUpdate(final INetworkCallback pCallback) {
			setIsUpdating(true);
			updateMessagesAsync(new NetworkCallbackUpdateStateWrapper(this, pCallback), false);
		}
		
		@Override
		public void requestOlderMessages(final INetworkCallback pCallback) {
			setIsEnlarging(true);
			updateMessagesAsync(new NetworkCallbackEnlargeStateWrapper(this, pCallback), true);
		}
		
		@Override
		public boolean isStreaming() {
			return false;
		}
		
		@Override
		public String getName(Context c) {
			return c.getString(R.string.fb_messages_name, getUser().name);
		}
		
		@Override
		public String getDescription(Context c) {
			return c.getString(R.string.fb_messages_description, getUser().name);
		};
		
		public int getOrder() {
			return 2;
		};
		
		@Override
		public String getStorageName() {
			return "fbmsgs" + getUser().id;
		}
		
		@Override
		public boolean isPersonal() {
			return true;
		}
	};
	
	private ArrayList<SyncableData> mSyncables = new ArrayList<SyncableData>();
	private ArrayList<ColumnMessagesProvider> mProviders = new ArrayList<ColumnMessagesProvider>();
	
	private Context mContext;
	private StorageBundle mBundle;
	
	private StorageManager mSM;
	
	private int mColor = 0xFFFFFFFF;
	
	private SortedArrayList<Chat> mChats = new SortedArrayList<Chat>();
	
	private XMPPConnection mConnection;
	private ConnectionConfiguration mConfig;
	private boolean isChatConnected = false;
	
	private boolean mAvailable = false;
	
	public FacebookAccount(Context c, long userID, String name, String token) {
		this.mContext = c;
		this.mBundle = new StorageBundle();
		
		mUser = new User(User.TYPE_FACEBOOK, userID, name);
		mUser.generateFacebookProfileUri();
		
		setUser(mUser);
		setOAuthToken(token);
		
		initSyncableData();
		
		updateGlobalData(null);
		updateHomeAsync(null, false);
		updateNotificationsAsync(null, false);
		updateMessagesAsync(null, false);
	}
	
	public FacebookAccount(Context c, StorageBundle bundle) {
		this.mContext = c;
		this.mBundle = bundle;
		this.mUser = new User(bundle.readBundle("USER", null));
		mColor = bundle.readInt("COLOR", 0xFFFFFFFF);
		mLastUserUpdate = bundle.readLong("LAST_USER_UPDATE", -1);
		mOAuthToken = bundle.readString("TOKEN", null);
		
		StorageBundle[] lists = bundle.readBundleArray("LISTS");
		for (int i = 0; i < lists.length; i++) {
			mLists.add(new FacebookList(lists[i]));
		}
		
		StorageBundle[] groups = bundle.readBundleArray("GROUPS");
		for (int i = 0; i < groups.length; i++) {
			mGroups.add(new FacebookGroup(groups[i]));
		}
		
		StorageBundle[] friends = getStorageManager().readBundleArray("FRIENDS", null);
		if (friends != null) {
			mFriends.ensureCapacity(friends.length);
			for (int i = 0; i < friends.length; i++) {
				mFriends.add(new User(friends[i]));
			}
		}
		
		initSyncableData();
	}
	
	private void initSyncableData() {
		boolean v = getStorageManager().readObj("HOME_UPDATES", null) instanceof StorageBundle;
		StorageBundle homeUpdates = v ? getStorageManager().readBundle("HOME_UPDATES", null) : null;
		StorageBundle notifications = v ? getStorageManager().readBundle("NOTIFICATIONS", null) : null;
		StorageBundle messages = v ? getStorageManager().readBundle("MESSAGES", null) : null;
		mHomeUpdates = new SyncableData(this, homeUpdates);
		mNotifications = new SyncableData(this, notifications);
		mMessages = new SyncableData(this, messages);
		if (homeUpdates == null || notifications == null || messages == null) {
			homeUpdates = mHomeUpdates.getBundle();
			notifications = mNotifications.getBundle();
			messages = mMessages.getBundle();
			getStorageManager().deleteAll();
			getStorageManager().write("HOME_UPDATES", homeUpdates);
			getStorageManager().write("NOTIFICATIONS", notifications);
			getStorageManager().write("MESSAGES", messages);
			getStorageManager().flushAsync();
		}
		createSyncablesAndProviders();
		
		final int l = mMessages.list.size();
		for (int i = 0; i < l; i++) {
			Message msg = mMessages.list.get(i);
			addMessageToChats(msg);
		}
		
		setupChat();
		startChat();
	}
	
	private void setupChat() {
		mConfig = new ConnectionConfiguration("chat.facebook.com", 5222);
		SASLAuthentication.registerSASLMechanism("X-FACEBOOK-PLATFORM", SASLXFacebookPlatformMechanism.class);
	    SASLAuthentication.supportSASLMechanism("X-FACEBOOK-PLATFORM", 0);
	}
	
	public void stopChat() {
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				if (isChatConnected) {
					mConnection.disconnect();
					mConnection = null;
					isChatConnected = false;
				}
			}
		});
	}
	
	@Override
	public boolean sendChatMsg(Chat pChat, String pText) {
		startChat();
		if (isChatConnected) {
			String to = null;
			final int l = pChat.getParticipants().size();
			if (l != 2) {
				return false; // TODO
			}
			for (int i = 0; i < l; i++) {
				if (pChat.getParticipants().get(i).id != mUser.id) {
					to = "-" + pChat.getParticipants().get(i).id + "@chat.facebook.com";
					break;
				}
			}
			if (to != null) {
				org.jivesoftware.smack.packet.Message msg = new org.jivesoftware.smack.packet.Message(to, Type.chat);
				msg.setBody(pText);
				mConnection.sendPacket(msg);
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void processPacket(Packet pArg0) {
		org.jivesoftware.smack.packet.Message msg = (org.jivesoftware.smack.packet.Message) pArg0;
		String fromStr = msg.getFrom();
		final long fromID = fromStr != null ? Long.parseLong(fromStr.substring(1, fromStr.indexOf('@'))) : mUser.id;
		String toStr = msg.getTo();
		long toID = Long.parseLong(toStr.substring(1, toStr.indexOf('@')));
		long threadID = TextUtils.isEmpty(msg.getThread()) ? -1 : Long.parseLong(msg.getThread());
		String text = msg.getBody();
		Log.i("JDROID", String.format("MESSAGE RECEIVED thread: %s body: %s sender: %s xml: %s", msg.getThread(), msg.getBody(), msg.getFrom(), msg.toXML()));
		if (TextUtils.isEmpty(text)) {
			return;
		}
		
		boolean added = false;
		for (int i = 0; i < mChats.size(); i++) {
			final Chat chat = mChats.get(i);
			ArrayList<User> p = chat.getParticipants();
			if ((threadID != -1 && chat.getID() == threadID) || 
				(threadID == -1 && fromID != mUser.id && p.size() == 2 && User.containsUserByID(p, fromID)) ||
				(threadID == -1 && fromID == mUser.id && p.size() == 2 && User.containsUserByID(p, toID))) {
				added = true;
				final ChatMessage chatMsg = new ChatMessage(-1, User.findByID(p, fromID), fromID == mUser.id, text, System.currentTimeMillis());
				chat.addMessage(chatMsg, false);
				Threads.runOnUIThread(new Runnable() {
					@Override
					public void run() {
						updateChatMessage(chat);
						if (fromID != mUser.id) {
							chat.addUnread(1);
							ChatUtils.updateChatNotification(mContext, chat);
						}
						mMessagesProvider.dispatchChange();
						dispatchMessagesChanged(1);
					}
				});
				break;
			}
		}
		if (!added) {
			updateMessagesAsync(null, false);
		}
	}
	
	protected void updateChatMessage(Chat chat) {
		if (chat.getMessages().size() > 0) {
			ChatMessage chatMsg = chat.getMessages().get(chat.getMessages().size() - 1);
			final int l = mMessages.list.size();
			for (int i = 0; i < l; i++) {
				Message msg = mMessages.list.get(i);
				if (msg.ID == chat.getID()) {
					msg.text = chatMsg.text;
					msg.sender = chatMsg.sender;
					msg.createdTime = msg.updatedTime = chatMsg.time;
					break;
				}
			}
		}
	}
	
	public void startChat() {
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				if (!isChatConnected) {
					try {
						XMPPConnection connection = new XMPPConnection(mConfig);
						connection.connect();
						if (!connection.isConnected()) {
							return;
						}
						connection.login(FacebookConfiguration.APP_ID, getOAuthToken(), "Social Center");
						if (!connection.isAuthenticated()) {
							return;
						}
						connection.addPacketListener(FacebookAccount.this, new PacketTypeFilter(org.jivesoftware.smack.packet.Message.class));
						connection.addPacketSendingListener(FacebookAccount.this, new PacketTypeFilter(org.jivesoftware.smack.packet.Message.class));
						connection.getRoster().addRosterListener(new RosterListener() {
							@Override
							public void presenceChanged(Presence pArg0) {
								Log.i("FBCHAT", "PRESENCE CHANGED from: " + mConnection.getRoster().getEntry(pArg0.getFrom()).getName()  + " type: " + pArg0.getType() + " status: " + pArg0.getStatus() + " mode: " + pArg0.getMode());
							}
							
							@Override
							public void entriesUpdated(Collection<String> pArg0) {
								Log.i("FBCHAT", "ENTRIES UPDATED");
							}
							
							@Override
							public void entriesDeleted(Collection<String> pArg0) {
							}
							
							@Override
							public void entriesAdded(Collection<String> pArg0) {
								Log.i("FBCHAT", "ENTRIES ADDED");
							}
						});
						connection.addConnectionListener(new ConnectionListener() {
							@Override
							public void reconnectionSuccessful() {
							}
							
							@Override
							public void reconnectionFailed(Exception pArg0) {
								connectionClosed();
							}
							
							@Override
							public void reconnectingIn(int pArg0) {
							}
							
							@Override
							public void connectionClosedOnError(Exception pArg0) {
								connectionClosed();
							}
							
							@Override
							public void connectionClosed() {
								if (mConnection != null || isChatConnected) {
									isChatConnected = false;
									mConnection = null;
									startChat();
								}
							}
						});
						if (connection.isAuthenticated()) {
							updatePresence();
							mConnection = connection;
							isChatConnected = true;
							Threads.runOnUIThread(new Runnable() {
								@Override
								public void run() {
									mContext.startService(new Intent(mContext.getApplicationContext(), ChatService.class));
								}
							});
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
	}
	
	public boolean isAvailable() {
		return mAvailable;
	}
	
	public void setAvailable(boolean pAvailable) {
		if (mAvailable != pAvailable) {
			mAvailable = pAvailable;
			updatePresence();
		}
	}
	
	private void updatePresence() {
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				if(isChatConnected) {
					try {
						Presence presence = new Presence(mAvailable ? org.jivesoftware.smack.packet.Presence.Type.available : 
							org.jivesoftware.smack.packet.Presence.Type.unavailable);
						presence.setMode(mAvailable ? Mode.available : Mode.away);
						mConnection.sendPacket(presence);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
	}
	
	public StorageManager getStorageManager() {
		if (mSM == null) {
			mSM = new StorageManager(mContext, "fa" + mUser.id);
		}
		return mSM;
	}
	
	public Facebook getFacebook() {
		if (mFacebook == null || TextUtils.isEmpty(mFacebook.getAccessToken())) {
			mFacebook = new Facebook(FacebookConfiguration.APP_ID);
			mFacebook.setAccessToken(mOAuthToken);
			mFacebook.setAccessExpires(0);
		}
		return mFacebook;
	}
	
	@Override
	public ArrayList<ColumnMessagesProvider> getProviders() {
		return mProviders;
	}
	
	private void createSyncablesAndProviders() {
		mSyncables.clear();
		mProviders.clear();
		mSyncables.add(mHomeUpdates);
		mSyncables.add(mNotifications);
		mSyncables.add(mMessages);
		mProviders.add(mHomeProvider);
		mProviders.add(mNotificationsProvider);
		mProviders.add(mMessagesProvider);
		
		Collections.sort(mLists);
		for (int i = 0; i < mLists.size(); i++) {
			FacebookList list = mLists.get(i);
			String storageName = "LIST_" + list.name;
			StorageBundle bundle = getStorageManager().readBundle(storageName, null);
			SyncableData syncable = new SyncableData(this, bundle);
			if (bundle == null) {
				bundle = syncable.getBundle();
				getStorageManager().write(storageName, bundle);
				getStorageManager().flushAsync();
			}
			FacebookListProvider provider = new FacebookListProvider(this, syncable, list);
			mSyncables.add(syncable);
			mProviders.add(provider);
		}
		
		Collections.sort(mGroups);
		for (int i = 0; i < mGroups.size(); i++) {
			FacebookGroup group = mGroups.get(i);
			String storageName = "GROUP_" + group.name;
			StorageBundle bundle = getStorageManager().readBundle(storageName, null);
			SyncableData syncable = new SyncableData(this, bundle);
			if (bundle == null) {
				bundle = syncable.getBundle();
				getStorageManager().write(storageName, bundle);
				getStorageManager().flushAsync();
			}
			FacebookGroupProvider provider = new FacebookGroupProvider(this, syncable, group);
			mSyncables.add(syncable);
			mProviders.add(provider);
		}
	}
	
	public SortedArrayList<Message> getHome() {
		return mHomeUpdates.list;
	}
	
	public SortedArrayList<Message> getNotifications() {
		return mNotifications.list;
	}
	
	public SortedArrayList<Message> getMessages() {
		return mMessages.list;
	}
	
	public BaseColumnMessagesProvider getHomeProvider() {
		return mHomeProvider;
	}
	
	public BaseColumnMessagesProvider getNotificationsProvider() {
		return mNotificationsProvider;
	}
	
	public BaseSyncableDataProvider getMessagesProvider() {
		return mMessagesProvider;
	}
	
	@Override
	public User getUser() {
		return mUser;
	}
	
	private void setUser(User user) {
		mUser = user;
		mBundle.write("USER", user.updateBundle());
		StorageManager.getDeflaut(mContext).flushAsync();
	}
	
	public String getOAuthToken() {
		return mOAuthToken;
	}

	public void setOAuthToken(String token) {
		this.mOAuthToken = token;
		mBundle.write("TOKEN", mOAuthToken);
	}
	
	public StorageBundle getBundle() {
		return mBundle;
	}
	
	public int getColor() {
		return mColor;
	}
	
	@Override
	public void setColor(int pColor) {
		mColor = pColor;
		this.mBundle.write("COLOR", pColor);
	}
	
	private void updateUserAsync(final INetworkCallback callback) {
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				try {
					Bundle params = new Bundle(2);
					params.putString("q", "SELECT id, name, pic_square, pic_big, FROM profile WHERE id = me();");
					JSONObject json = new JSONObject(getFacebook().request("fql", params));
					JSONObject result = json.getJSONArray("data").getJSONObject(0);
					if(mUser.id == -1) {
						mUser.id = result.getLong("id");
					}
					String name = result.getString("name");
					mUser.name = name;
					mUser = User.parseFacebookUser(null, result, false);
					mLastUserUpdate = System.currentTimeMillis();
					mBundle.write("LAST_USER_UPDATE", mLastUserUpdate);
					setUser(mUser);
					StorageManager.getDeflaut(mContext).flush();
					callback.onSucceed(FacebookAccount.this);
				} catch (Exception e) {
					e.printStackTrace();
					callback.onFailed(FacebookAccount.this);
				}
			}
		});
	}
	
	public void updateLists(final INetworkCallback callback) {
		if (mIsUpdatingLists) {
			if (callback != null) callback.onSucceed(FacebookAccount.this);
			return;
		}
		mIsUpdatingLists = true;
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				try {
					final JSONArray json = new JSONObject(getFacebook().request("me/friendlists")).getJSONArray("data");
					final ArrayList<FacebookList> newLists = new ArrayList<FacebookList>(json.length());
					for (int i = 0; i < json.length(); i++) {
						JSONObject obj = json.getJSONObject(i);
						newLists.add(new FacebookList(obj.getLong("id"), obj.getString("name")));
					}
					
					Threads.runOnUIThread(new Runnable() {
						@Override
						public void run() {
							ListUtils.clone(newLists, mLists);
							StorageBundle[] bundles = new StorageBundle[mLists.size()];
							for (int i = 0; i < bundles.length; i++){
								bundles[i] = mLists.get(i).updateBundle();
							}
							mBundle.write("LISTS", bundles);
							StorageManager.getDeflaut(mContext).flushAsync();
							
							createSyncablesAndProviders();
							SyncManager.updateAccountColumnsSync(mContext, FacebookAccount.this);
							dispatchProvidersChanged();
							
							mIsUpdatingLists = false;
							if (callback != null) callback.onSucceed(FacebookAccount.this);
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
					mIsUpdatingLists = false;
					if (callback != null) callback.onFailed(FacebookAccount.this);
				}
			}
		});
	}
	
	public void updateGroups(final INetworkCallback callback) {
		if (mIsUpdatingGroups) {
			if (callback != null) callback.onSucceed(FacebookAccount.this);
			return;
		}
		mIsUpdatingGroups = true;
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				try {
					final JSONArray json = new JSONObject(getFacebook().request("me/groups")).getJSONArray("data");
					final ArrayList<FacebookGroup> newGroups = new ArrayList<FacebookGroup>(json.length());
					for (int i = 0; i < json.length(); i++) {
						JSONObject obj = json.getJSONObject(i);
						newGroups.add(new FacebookGroup(obj.getLong("id"), obj.getString("name")));
					}
					
					Threads.runOnUIThread(new Runnable() {
						@Override
						public void run() {
							ListUtils.clone(newGroups, mGroups);
							StorageBundle[] bundles = new StorageBundle[mGroups.size()];
							for (int i = 0; i < bundles.length; i++){
								bundles[i] = mGroups.get(i).updateBundle();
							}
							mBundle.write("GROUPS", bundles);
							StorageManager.getDeflaut(mContext).flushAsync();
							
							createSyncablesAndProviders();
							SyncManager.updateAccountColumnsSync(mContext, FacebookAccount.this);
							dispatchProvidersChanged();
							
							mIsUpdatingGroups = false;
							if (callback != null) callback.onSucceed(FacebookAccount.this);
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
					mIsUpdatingGroups = false;
					if (callback != null) callback.onFailed(FacebookAccount.this);
				}
			}
		});
	}
	
	@Override
	public void updateGlobalData(INetworkCallback pCallback) {
		MultiNetworkCallbackWrapper wrapper = new MultiNetworkCallbackWrapper(pCallback, 4);
		updateUserAsync(wrapper);
		updateFriends(wrapper);
		updateLists(wrapper);
		updateGroups(wrapper);
	}
	
	@Override
	public SortedArrayList<Chat> getChats() {
		return mChats;
	}
	
	@Override
	public Chat findMessageChat(Message msg) {
		final int l = mChats.size();
		for (int i = 0; i < l; i++) {
			Chat chat = mChats.get(i);
			if (msg.ID == chat.getID()) {
				return chat;
			}
		}
		return null;
	}
	
	@Override
	public Message findChatMessage(Chat chat) {
		SortedArrayList<Message> msgs = mMessages.list;
		for (int i = 0; i < msgs.size(); i++) {
			Message msg = msgs.get(i);
			if (msg.ID == chat.getID()) {
				return msg;
			}
		}
		return null;
	}
	
	public boolean updateChat(final Chat chat, boolean enlarge) {
		try {
			StringBuilder query = new StringBuilder("{\"messages\" : \"SELECT message_id, author_id, body, created_time FROM message WHERE thread_id = ");
			query.append(chat.getID());
			int limit = -1;
			final int cl = chat.getMessages().size();
			if (chat.getUpdatedTime() != -1 && !enlarge) {
				query.append(" AND created_time > ");
				query.append(chat.getUpdatedTime() / 1000);
				limit = 500;
			} else if (enlarge && cl > 0) {
				query.append(" AND created_time < ");
				query.append(chat.getMessages().get(0).time / 1000);
				limit = 50;
			}
			if (limit != -1) {
				query.append(" LIMIT ");
				query.append(limit);
			}
			query.append("\", \"usernames\" : \"SELECT id, name, pic_square, pic_big FROM profile WHERE id IN (SELECT author_id FROM #messages)\"}");
			Bundle params = new Bundle(3);
			params.putString("q", query.toString());
			JSONArray json = new JSONObject(getFacebook().request("fql", params)).getJSONArray("data");
			
			JSONArray messages = null, usernames = null;
			for (int i = 0; i < json.length(); i++) {
				JSONObject queryResult = json.getJSONObject(i);
				String name = queryResult.getString("name");
				if (name.equals("messages")) {
					messages = queryResult.getJSONArray("fql_result_set");
				} else if (name.equals("usernames")) {
					usernames = queryResult.getJSONArray("fql_result_set");
				}
			}
			
			ArrayList<User> users = new ArrayList<User>(usernames.length());
			for (int i = usernames.length() - 1; i >= 0; i--) {
				users.add(User.parseFacebookUser(null, usernames.getJSONObject(i), false));
			}
			final int l = messages.length();
			final ChatMessage[] newMsgs = new ChatMessage[l];
			for (int i = 0; i < l; i++) {
				JSONObject msgJson = messages.getJSONObject(i);
				String idStr = msgJson.getString("message_id");
				long id = Long.parseLong(idStr.substring(idStr.indexOf('_') + 1));
				User sender = User.findByID(users, msgJson.getLong("author_id"));
				ChatMessage msg = new ChatMessage(id, sender, sender.id == mUser.id, msgJson.getString("body"), msgJson.getLong("created_time") * 1000l);
				newMsgs[i] = msg;
			}
			Threads.runOnUIThread(new Runnable() {
				@Override
				public void run() {
					chat.removeMessagesWithoutID();
					for (int i = 0; i < newMsgs.length; i++) {
						chat.addMessage(newMsgs[i], true);
						chat.addUnread(1);
					}
					updateChatMessage(chat);
					if (chat.getMessages().size() > 0) {
						ChatUtils.updateChatNotification(mContext, chat);
					}
				}
			});
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private int parseStreamUpdates(SortedArrayList<Message> target, JSONArray data, String updatesName, String usernamesName, String commentsName) {
		try {
			JSONArray updates = null, usernames = null, comments = null;
			for (int i = 0; i < data.length(); i++) {
				JSONObject queryResult = data.getJSONObject(i);
				String name = queryResult.getString("name");
				if (name.equals(updatesName)) {
					updates = queryResult.getJSONArray("fql_result_set");
				} else if (name.equals(usernamesName)) {
					usernames = queryResult.getJSONArray("fql_result_set");
				} else if (name.equals(commentsName)) {
					comments = queryResult.getJSONArray("fql_result_set");
				}
			}
			
			ArrayList<User> users = new ArrayList<User>(usernames.length());
			for (int i = usernames.length() - 1; i >= 0; i--) {
				users.add(User.parseFacebookUser(null, usernames.getJSONObject(i), false));
			}
			int newCount = 0;
			final int l = updates.length();
			for (int i = 0; i < l; i++) {
				Message msg = Message.parseFacebookStreamUpdate(this, updates.getJSONObject(i), comments, users);
				if (msg != null && (!TextUtils.isEmpty(msg.text) || (msg.facebookImages != null && msg.facebookImages.size() > 0))) {
					msg.updateBundle();
					int index = target.indexOf(msg);
					if (index == -1) {
						target.add(msg);
						newCount++;
					} else {
						target.set(index, msg);
					}
				}
			}
			return newCount;
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	private int parseGraphUpdates(SortedArrayList<Message> target, JSONArray data) {
		try {
			int newCount = 0;
			final int l = data.length();
			for (int i = 0; i < l; i++) {
				Message msg = Message.parseFacebookGraphUpdate(this, data.getJSONObject(i));
				if (msg != null && (!TextUtils.isEmpty(msg.text) || (msg.facebookImages != null && msg.facebookImages.size() > 0))) {
					msg.updateBundle();
					int index = target.indexOf(msg);
					if (index == -1) {
						target.add(msg);
						newCount++;
					} else {
						target.set(index, msg);
					}
				}
			}
			return newCount;
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	private static final int FLAG_ENLARGE = 2;
	private static final int FLAG_USE_LAST_UPDATE = 4;
	
	private String generateStreamQuery(SyncableData syncable, String filterKey, String sourceId, String searchQuery, int flags) {
		boolean enlarge = (flags & FLAG_ENLARGE) != 0;
		boolean useLastUpdate = (flags & FLAG_USE_LAST_UPDATE) != 0;
		
		StringBuilder query = new StringBuilder("{\"updates\" : \"SELECT post_id, attachment, actor_id, target_id, created_time, updated_time, message, comments, likes FROM stream WHERE ");
		if (filterKey != null) {
			query.append(" filter_key IN ");
			query.append(filterKey);
		} else if (sourceId != null) {
			query.append(" source_id = ");
			query.append(sourceId);
		} else if (searchQuery != null) {
			query.append(" strpos(lower(message),'");
			query.append(searchQuery);
			query.append("') >=0 AND uid IN (SELECT uid2 FROM friend WHERE uid1=me())");
		} else {
			throw new IllegalStateException("SourceID, filterkey and search query are all empty!");
		}
		long lastUpdate = syncable.getLastUpdate();
		int limit = 50;
		if (syncable.list.size() > 0 && !enlarge && !useLastUpdate) {
			long maxUpdatedTime = Long.MIN_VALUE;
			for (int i = syncable.list.size() - 1; i >= 0; i--) {
				long updatedTime = syncable.list.get(i).updatedTime;
				if (updatedTime > maxUpdatedTime) {
					maxUpdatedTime = updatedTime;
				}
			}
			query.append(" AND updated_time > ");
			query.append(maxUpdatedTime / 1000);
			limit = 300;
		} else if (useLastUpdate && !enlarge && lastUpdate != -1) {
			query.append(" AND updated_time > ");
			query.append(lastUpdate / 1000);
			limit = 300;
		} else if (enlarge) {
			query.append(" AND created_time < ");
			query.append(syncable.list.get(syncable.list.size() - 1).createdTime / 1000);
		}
		
		query.append(" LIMIT ");
		query.append(limit);
		
		query.append("\", \"usernames\" : \"SELECT id, name, pic_square, pic_big FROM profile WHERE id IN (SELECT actor_id FROM #updates) OR id IN (SELECT target_id FROM #updates)\"}");
		return query.toString();
	}
	
	private Bundle generateGraphQuery(SyncableData syncable, String search, int flags) {
		boolean enlarge = (flags & FLAG_ENLARGE) != 0;
		boolean useLastUpdate = (flags & FLAG_USE_LAST_UPDATE) != 0;
		Bundle params = new Bundle();
		params.putString("q", search);
		long lastUpdate = syncable.getLastUpdate();
		int limit = 50;
		if (syncable.list.size() > 0 && !enlarge && !useLastUpdate) {
			long maxUpdatedTime = Long.MIN_VALUE;
			for (int i = syncable.list.size() - 1; i >= 0; i--) {
				long updatedTime = syncable.list.get(i).updatedTime;
				if (updatedTime > maxUpdatedTime) {
					maxUpdatedTime = updatedTime;
				}
			}
			params.putString("since", String.valueOf(maxUpdatedTime / 1000));
			limit = 300;
		} else if (useLastUpdate && !enlarge && lastUpdate != -1) {
			params.putString("since", String.valueOf(lastUpdate / 1000));
			limit = 300;
		} else if (enlarge) {
			params.putString("until", String.valueOf(syncable.list.get(syncable.list.size() - 1).createdTime / 1000));
		}
		params.putString("limit", String.valueOf(limit));
		return params;
	}
	
	public void updateHomeAsync(final INetworkCallback callback, final boolean enlarge) {
		String query = generateStreamQuery(mHomeUpdates, "(SELECT filter_key FROM stream_filter WHERE uid = me() AND type = 'newsfeed')", null, null, enlarge ? FLAG_ENLARGE : 0);
		updateStreamAsync(callback, mHomeProvider, query, mHomeUpdates, enlarge);
	}
	
	public void updateListAsync(INetworkCallback callback, long id, boolean enlarge) {
		for (int i = 0; i < mProviders.size(); i++) {
			if (mProviders.get(i) instanceof FacebookListProvider) {
				FacebookListProvider provider = (FacebookListProvider) mProviders.get(i);
				if (provider.mList.id == id) {
					updateListAsync(callback, provider, enlarge);
					return;
				}
			}
		}
		callback.onSucceed(this);
	}
	
	public void updateListAsync(final INetworkCallback callback, final FacebookListProvider provider, final boolean enlarge) {
		String query = generateStreamQuery(provider.mSyncable, "(SELECT filter_key FROM stream_filter WHERE uid = me() AND name = '" + provider.mList.name + "')", null, null, enlarge ? FLAG_ENLARGE : 0);
		updateStreamAsync(callback, provider, query, provider.mSyncable, enlarge);
	}
	
	public void updateGroupAsync(INetworkCallback callback, long id, boolean enlarge) {
		for (int i = 0; i < mProviders.size(); i++) {
			if (mProviders.get(i) instanceof FacebookGroupProvider) {
				FacebookGroupProvider provider = (FacebookGroupProvider) mProviders.get(i);
				if (provider.mGroup.id == id) {
					updateGroupAsync(callback, provider, enlarge);
					return;
				}
			}
		}
		callback.onSucceed(this);
	}
	
	public void updateGroupAsync(final INetworkCallback callback, final FacebookGroupProvider provider, final boolean enlarge) {
		String query = generateStreamQuery(provider.mSyncable, null, String.valueOf(provider.mGroup.id), null, enlarge ? FLAG_ENLARGE : 0);
		updateStreamAsync(callback, provider, query, provider.mSyncable, enlarge);
	}
	
	public void updateUserStreamAsync(final INetworkCallback callback, final FacebookUserProvider column, final User user, final SyncableData syncable, final boolean enlarge) {
		updateStreamAsync(callback, column, generateStreamQuery(syncable, null, String.valueOf(user.id), null, enlarge ? FLAG_ENLARGE : 0), syncable, enlarge);
	}
	
	private void updateStreamAsync(INetworkCallback callback, BaseSyncableDataProvider column, String query, SyncableData syncable, boolean enlarge) {
		Bundle params = new Bundle(3);
		params.putString("q", query);
		updateStreamAsync(callback, column, "fql", params, syncable, enlarge);
	}
	
	private void updateStreamAsync(final INetworkCallback callback, final BaseSyncableDataProvider column, final String graphPath, 
										final Bundle params, final SyncableData syncable, final boolean enlarge) {
		if (enlarge) {
			if (syncable.isEnlarging) {
				if (callback != null) callback.onSucceed(this);
				return;
			} else {
				syncable.isEnlarging = true;
			}
		} else {
			if (syncable.isUpdating) {
				if (callback != null) callback.onSucceed(this);
				return;
			} else {
				syncable.isUpdating = true;
			}
		}
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				updateStream(callback, column, graphPath, params, syncable, enlarge && syncable.list.size() > 0);
			}
		});
	}
	
	private void updateStream(final INetworkCallback callback, final BaseSyncableDataProvider column, final String graphPath, final Bundle params, final SyncableData syncable, final boolean enlarge) {
		try {
			final boolean isFql = graphPath.equals("fql");
			JSONObject json = new JSONObject(getFacebook().request(graphPath, params));
			
			final JSONArray data = json.getJSONArray("data");
			
			Threads.runOnAsyncThread(new Runnable() {
				@Override
				public void run() {
					ListUtils.clone(syncable.list, syncable.newList);
					final int newCount = isFql ? parseStreamUpdates(syncable.newList, data, "updates", "usernames", "comments") :
												  parseGraphUpdates(syncable.newList, data);
					Threads.runOnUIThread(new Runnable() {
						@Override
						public void run() {
							syncable.swap();
							if (mSyncables.contains(syncable)) {
								syncable.updateMessages();
								getStorageManager().flushAsync();
							}
							if (!enlarge) {
								column.addUnreadMessages(newCount);
								syncable.setLastUpdate(System.currentTimeMillis());
							}
							if (enlarge && newCount == 0) {
								syncable.hasOlder = false;
							}
							
							column.dispatchChange();
							
							if (enlarge) {
								syncable.isEnlarging = false;
							} else {
								syncable.isUpdating = false;
							}
							if (callback != null) callback.onSucceed(FacebookAccount.this);
						}
					});
				}
			});
			
		} catch (Exception e) {
			e.printStackTrace();
			if (callback != null) callback.onFailed(FacebookAccount.this);
			if (enlarge) {
				syncable.isEnlarging = false;
			} else {
				syncable.isUpdating = false;
			}
		}
	}
	
	@Override
	public ISearchableColumn addSearchColumn(String pQuery) {
		FacebookSearchColumn column = new FacebookSearchColumn(this, pQuery, null);
		mSearchProviders.add(column);
		return column;
	}
	
	@Override
	public void removeSearchColumn(ISearchableColumn column) {
		mSearchProviders.remove(column);
	}
	
	public void updateSearchAsync(final INetworkCallback callback, final FacebookSearchColumn column, final SyncableData syncable, final boolean enlarge) {
		updateStreamAsync(callback, column, "search", generateGraphQuery(syncable, column.getQuery(), (enlarge ? FLAG_ENLARGE : 0)|FLAG_USE_LAST_UPDATE), syncable, enlarge);
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
	public void searchPeopleLocal(SortedArrayList<User> pTarget, String pQuery) {
		User.findByQuery(pTarget, mFriends, pQuery.toLowerCase());
	}
	
	@Override
	public boolean searchPeople(SortedArrayList<User> pTarget, String pQuery) {
		try {
			Bundle params = new Bundle(4);
			params.putString("type", "user");
			params.putString("q", pQuery);
			JSONObject json = new JSONObject(getFacebook().request("search", params));
			JSONArray data = json.getJSONArray("data");
			for (int i = data.length() - 1; i >= 0; i--) {
				User user = User.parseFacebookUser(null, data.getJSONObject(i), false);
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
	
	public Album[] loadUserAlbums(User user) {
		try {
			StringBuilder query = new StringBuilder(USER_ALBUMS_QUERY);
			query.append(user.id);
			Bundle params = new Bundle(3);
			params.putString("q", query.toString());
			JSONArray json = new JSONObject(getFacebook().request("fql", params)).getJSONArray("data");
			final int l = json.length();
			Album[] albums = new Album[l];
			for (int i = 0; i < l; i++) {
				albums[i] = Album.parseJson(json.getJSONObject(i));
			}
			return albums;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Photo[] loadAlbumPhotos(Album album) {
		try {
			StringBuilder query = new StringBuilder(ALBUM_PHOTOS_QUERY);
			query.append(album.aid);
			query.append('"');
			Bundle params = new Bundle(3);
			params.putString("q", query.toString());
			JSONArray json = new JSONObject(getFacebook().request("fql", params)).getJSONArray("data");
			final int l = json.length();
			Photo[] photos = new Photo[l];
			for (int i = 0; i < l; i++) {
				photos[i] = Photo.parseFacebookAlbumPhoto(this, json.getJSONObject(i));
			}
			return photos;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Photo loadPhoto(String objectID) {
		try {
			StringBuilder query = new StringBuilder(PHOTO_QUERY);
			query.append(objectID);
			query.append('"');
			Bundle params = new Bundle(3);
			params.putString("q", query.toString());
			JSONArray json = new JSONObject(getFacebook().request("fql", params)).getJSONArray("data");
			return Photo.parseFacebookAlbumPhoto(this, json.getJSONObject(0));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Message loadPhotoAsMessage(Photo photo) {
		try {
			String id = Long.toString(photo.objectId);
			String query = String.format(PHOTO_MESSAGE_QUERY, id, id);
			Bundle params = new Bundle(3);
			params.putString("q", query);
			JSONArray data = new JSONObject(getFacebook().request("fql", params)).getJSONArray("data");
			
			JSONArray likes = null, usernames = null, commentsJson = null;
			for (int i = 0; i < data.length(); i++) {
				JSONObject queryResult = data.getJSONObject(i);
				JSONArray result = queryResult.getJSONArray("fql_result_set");
				String name = queryResult.getString("name");
				if (name.equals("likes")) {
					likes = result;
				} else if (name.equals("comments")) {
					commentsJson = result;
				} else if (name.equals("usernames")) {
					usernames = result;
				}
			}
			ArrayList<User> users = new ArrayList<User>(usernames.length());
			for (int i = usernames.length() - 1; i >= 0; i--) {
				users.add(User.parseFacebookUser(null, usernames.getJSONObject(i), false));
			}
			
			int numLikes = likes.length();
			Comments comments = new Comments(null, null, commentsJson, users);
			
			Message msg = new Message();
			msg.account = this;
			msg.photo = photo;
			msg.type = Message.TYPE_FACEBOOK_PHOTO;
			msg.ID = photo.objectId;
			msg.canLike = true;
			msg.numLikes = numLikes;
			msg.comments = comments;
			msg.createdTime = photo.created;
			msg.updatedTime = photo.updated;
			msg.facebookImages = new ArrayList<FacebookImage>(1);
			FacebookImage img = new FacebookImage(photo.src, null, photo.pid, -1, -1);
			img.bigSrc = photo.srcBig;
			msg.facebookImages.add(img);
			return msg;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void updateMessagesAsync(final INetworkCallback callback, final boolean enlarge) {
		if (mMessages.isUpdating || mMessages.isEnlarging || (enlarge && !mMessages.hasOlder)) {
			if (callback != null)callback.onSucceed(this);
			return;
		}
		if (enlarge && mMessages.list.size() > 0) {
			mMessages.isEnlarging = true;
		} else {
			mMessages.isUpdating = false;
		}
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				updateMessages(callback, enlarge && mMessages.list.size() > 0);
			}
		});
	}
	
	
	private void updateMessages(final INetworkCallback callback, final boolean enlarge) {
		try {
			Bundle params = new Bundle(3);
			StringBuilder query = new StringBuilder("{\"messages\" : \"SELECT thread_id, recipients, snippet, snippet_author, updated_time FROM thread WHERE folder_id = 0 ");
			if (mMessages.getLastUpdate() != -1 && !enlarge) {
				query.append("AND updated_time >= ");
				query.append(mMessages.getLastUpdate() / 1000);
			} else if (enlarge) {
				query.append(" AND updated_time < ");
				query.append(mMessages.list.get(mMessages.list.size() - 1).createdTime / 1000);
			}
			query.append(" LIMIT 100\", \"usernames\" : \"SELECT id, name, pic_square, pic_big FROM profile WHERE id IN (SELECT snippet_author FROM #messages) OR id IN (SELECT recipients FROM #messages);\"}");
			params.putString("q", query.toString());
			final long updateTime = System.currentTimeMillis();
			JSONObject json = new JSONObject(getFacebook().request("fql", params));
			JSONArray data = json.getJSONArray("data");
			
			JSONArray messages = null, usernames = null;
			
			for (int i = 0; i < data.length(); i++) {
				JSONObject queryResult = data.getJSONObject(i);
				String name = queryResult.getString("name");
				JSONArray result = queryResult.getJSONArray("fql_result_set");
				if (name.equals("messages")) {
					messages = result;
				} else if (name.equals("usernames")) {
					usernames = result;
				}
			}
			if (messages != null && usernames != null) {
				ArrayList<User> users = new ArrayList<User>(usernames.length());
				for (int i = usernames.length() - 1; i >= 0; i--) {
					users.add(User.parseFacebookUser(null, usernames.getJSONObject(i), false));
				}
				ListUtils.clone(mMessages.list, mMessages.newList);
				int newCount = 0;
				for (int i = messages.length() - 1; i >= 0; i--) {
					JSONObject message = messages.getJSONObject(i);
					Message msg = Message.parseFacebookMessage(this, message, users);
					if (msg.sender == null) {
						continue; // workaround for facebook bug
					}
					msg.updateBundle();
					int index = mMessages.newList.indexOf(msg);
					if (index != -1) {
						mMessages.newList.remove(index);
					}
					mMessages.newList.add(msg);
					addMessageToChats(msg);
					newCount++;
				}
				final int finalNewCount = newCount;
				Threads.runOnUIThread(new Runnable() {
					@Override
					public void run() {
						mMessages.swap();

						mMessages.setLastUpdate(updateTime);
						dispatchMessagesChanged(finalNewCount);
						
						if (enlarge && finalNewCount == 0) {
							mMessages.hasOlder = false;
						}
						
						mMessages.isUpdating = false;
						mMessages.isEnlarging = false;
						if (!enlarge) {
							mMessagesProvider.addUnreadMessages(finalNewCount);
						}
						mMessagesProvider.dispatchChange();
						if (callback != null) callback.onSucceed(FacebookAccount.this);
					}
				});
			} else {
				throw new Exception();
			}
		} catch (Exception e) {
			e.printStackTrace();
			mMessages.isUpdating = false;
			mMessages.isEnlarging = false;
			if (callback != null) callback.onFailed(FacebookAccount.this);
		}
	}
	
	protected void addMessageToChats(Message msg) {
		long id = msg.ID;
		final int l = mChats.size();
		for (int i = 0; i < l; i++) {
			Chat chat = mChats.get(i);
			if (chat.getID() == id) {
				if (chat.getUpdatedTime() < msg.updatedTime) {
					chat.setDirty(true);
					//chat.setUpdatedTime(msg.updatedTime);
					Collections.sort(mChats);
				}
				return;
			}
		}
		Chat chat = new Chat(this, msg.ID);
		for (int i = 0; i < msg.recipients.length; i++) {
			chat.getParticipants().add(msg.recipients[i]);
		}
		chat.setDirty(true);
		//chat.setUpdatedTime(msg.updatedTime);
		mChats.add(chat);
	}
	
	public void updateNotificationsAsync(final INetworkCallback callback, final boolean enlarge) {
		if (mNotifications.isUpdating || mNotifications.isEnlarging) {
			if (callback != null)callback.onSucceed(this);
			return;
		}
		if (enlarge && mNotifications.list.size() > 0) {
			mNotifications.isEnlarging = true;
		} else {
			mNotifications.isUpdating = false;
		}
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				updateNotifications(callback, enlarge && mNotifications.list.size() > 0);
			}
		});
	}
	
	
	private void updateNotifications(final INetworkCallback callback, final boolean enlarge) {
		try {
			Bundle params = new Bundle(3);
			StringBuilder query = new StringBuilder("{\"usernames\" : \"SELECT id, name, pic_square, pic_big FROM profile WHERE id IN (SELECT sender_id FROM #notifications)\", ");
			query.append("\"notifications\" : \"SELECT notification_id, sender_id, title_text, object_id, object_type, created_time FROM notification WHERE recipient_id= me() ");		
			if (mNotifications.getLastUpdate() != -1 && !enlarge) {
				query.append("AND created_time >= ");
				query.append(mNotifications.getLastUpdate() / 1000);
			} else if (enlarge) {
				query.append(" AND created_time < ");
				query.append(mNotifications.list.get(mNotifications.list.size() - 1).createdTime / 1000);
			}
			query.append(" LIMIT 100\"}");
			params.putString("q", query.toString());
			final long updateTime = System.currentTimeMillis();
			JSONObject json = new JSONObject(getFacebook().request("fql", params));
			JSONArray data = json.getJSONArray("data");
			
			JSONArray notifications = null, usernames = null;
			
			for (int i = 0; i < data.length(); i++) {
				JSONObject queryResult = data.getJSONObject(i);
				String name = queryResult.getString("name");
				JSONArray result = queryResult.getJSONArray("fql_result_set");
				if (name.equals("notifications")) {
					notifications = result;
				} else if (queryResult.getString("name").equals("usernames")) {
					usernames = result;
				}
			}
			if (notifications != null && usernames != null) {
				ArrayList<User> users = new ArrayList<User>(usernames.length());
				for (int i = usernames.length() - 1; i >= 0; i--) {
					users.add(User.parseFacebookUser(null, usernames.getJSONObject(i), false));
				}
				ListUtils.clone(mNotifications.list, mNotifications.newList);
				int newCount = 0;
				for (int i = notifications.length() - 1; i >= 0; i--) {
					JSONObject notification = notifications.getJSONObject(i);
					Message msg = Message.parseFacebookNotification(this, notification, users);
					msg.updateBundle();
					int index = mNotifications.newList.indexOf(msg);
					if (index == -1) {
						mNotifications.newList.add(msg);
						newCount++;
					} else {
						mNotifications.newList.set(index, msg);
					}
				}
				final int finalNewCount = newCount;
				Threads.runOnUIThread(new Runnable() {
					@Override
					public void run() {
						mNotifications.swap();

						mNotifications.setLastUpdate(updateTime);
						dispatchNotificationsChanged(finalNewCount);
						
						if (enlarge && finalNewCount == 0) {
							mNotifications.hasOlder = false;
						}
						
						mNotifications.isUpdating = false;
						mNotifications.isEnlarging = false;
						if (!enlarge) {
							mNotificationsProvider.addUnreadMessages(finalNewCount);
						}
						mNotificationsProvider.dispatchChange();
						if (callback != null) callback.onSucceed(FacebookAccount.this);
					}
				});
			} else {
				throw new Exception();
			}
		} catch (Exception e) {
			e.printStackTrace();
			mNotifications.isUpdating = false;
			mNotifications.isEnlarging = false;
			if (callback != null) callback.onFailed(FacebookAccount.this);
		}
	}
	
	public void deleteSavedMessages() {
		for (int i = 0; i < mSyncables.size(); i++) {
			((SyncableData)mSyncables.get(i)).clear();
			((BaseColumnMessagesProvider) mProviders.get(i)).dispatchChange();
		}
		StorageManager.getDeflaut(mContext).flushAsync();
		getStorageManager().flushAsync();
	}
	
	public boolean postFeedUpdate(String target, String text, String imgPath) {
		try {
			boolean hasImage = !TextUtils.isEmpty(imgPath);
			Bundle params = new Bundle(4);
			params.putString("message", text);
			if (hasImage) {
				Bitmap bmd = BitmapFactory.decodeFile(imgPath);
				ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 * 512);
				bmd.compress(CompressFormat.JPEG, 80, baos);
				bmd.recycle();
				byte[] bytes = baos.toByteArray();
				baos = null;
				params.putByteArray("picture", bytes);
			}
			getFacebook().request(target + (hasImage ? "/photos" : "/feed"), params, "POST");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean likeMessage(Message msg) {
		try {
			Bundle params = new Bundle(3);
			getFacebook().request(msg.getPostID() + "/likes", params, "POST");
			if (msg.userLikes) {
				msg.userLikes = false;
				msg.numLikes--;
			} else {
				msg.userLikes = true;
				msg.numLikes++;
			}
			msg.updateBundle();
			getStorageManager().flushAsync();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean likePhoto(Message msg) {
		try {
			Bundle params = new Bundle(3);
			getFacebook().request(msg.photo.objectId + "/likes", params, "POST");
			if (msg != null) {
				if (msg.userLikes) {
					msg.userLikes = false;
					msg.numLikes--;
				} else {
					msg.userLikes = true;
					msg.numLikes++;
				}
				msg.updateBundle();
				getStorageManager().flushAsync();
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean likeComment(Message msg, Comment comment) {
		try {
			Bundle params = new Bundle(1);
			getFacebook().request(msg.getPostID() + '_' + comment.ID + "/likes", params, "POST");
			if (comment.userLikes) {
				comment.userLikes = false;
				comment.likes--;
			} else {
				comment.userLikes = true;
				comment.likes++;
			}
			comment.updateBundle();
			msg.comments.updateBundle();
			msg.updateBundle();
			getStorageManager().flushAsync();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public SortedArrayList<User> getFriends() {
		return mFriends;
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
					Bundle params = new Bundle(3);
					JSONArray friendsJson = new JSONObject(getFacebook().request("me/friends", params)).getJSONArray("data");
					final int l = friendsJson.length();
					final SortedArrayList<User> friends = new SortedArrayList<User>(l);
					for (int i = 0; i < l; i++) {
						friends.add(User.parseFacebookUser(null, friendsJson.getJSONObject(i), false));
					}
					StorageBundle[] bundles = new StorageBundle[friends.size()];
					for (int i = 0; i < bundles.length; i++) {
						bundles[i] = friends.get(i).updateBundle();
					}
					getStorageManager().write("FRIENDS", bundles);
					getStorageManager().flushAsync();
					Threads.runOnUIThread(new Runnable() {
						@Override
						public void run() {
							ListUtils.clone(friends, mFriends);
							mIsUpdatingFriends = false;
							callback.onSucceed(FacebookAccount.this);
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
					mIsUpdatingFriends = false;
					callback.onFailed(FacebookAccount.this);
				}
			}
		});
	}
	
	public User[] getMessageLikes(Message msg) {
		try {
			Bundle params = new Bundle(3);
			params.putString("limit", "50");
			JSONArray likesJson = new JSONObject(getFacebook().request(msg.getPostID() + "/likes", params)).getJSONArray("data");
			User[] users = new User[likesJson.length()];
			for (int i = likesJson.length() - 1; i >= 0; i--) {
				users[i] = User.parseFacebookUser(null, likesJson.getJSONObject(i), false);
			}
			if (users.length > msg.numLikes) {
				msg.numLikes = users.length;
				msg.updateBundle();
				getStorageManager().flushAsync(10000);
			}
			return users;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public User[] getCommentLikes(Message msg, Comment comment) {
		try {
			Bundle params = new Bundle(1);
			params.putString("limit", "50");
			JSONArray likesJson = new JSONObject(getFacebook().request(msg.getPostID() + '_' + comment.ID + "/likes", params)).getJSONArray("data");
			User[] users = new User[likesJson.length()];
			for (int i = likesJson.length() - 1; i >= 0; i--) {
				users[i] = User.parseFacebookUser(null, likesJson.getJSONObject(i), false);
			}
			if (users.length > comment.likes) {
				comment.likes = users.length;
				comment.updateBundle();
				getStorageManager().flushAsync(10000);
			}
			return users;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Message loadStreamUpdate(String ID) {
		try {
			String query = "{\"update\" : \"SELECT post_id, attachment, actor_id, target_id, created_time, updated_time, message, comments, likes, description FROM stream WHERE post_id = '" + ID + "'\"" +
					", \"comments\" : \"SELECT id, post_id, fromid, text, time, likes, user_likes FROM comment WHERE post_id IN (SELECT post_id FROM #update)\", \"usernames\" : \"SELECT id, name, pic_square, pic_big FROM profile WHERE id IN (SELECT actor_id FROM #update) OR id IN (SELECT fromid FROM #comments)\"}";
			
			Bundle params = new Bundle(3);
			params.putString("q", query);
			
			JSONObject json = new JSONObject(getFacebook().request("fql", params));
			JSONArray data = json.getJSONArray("data");
			
			JSONArray update = null;
			JSONArray usernames = null;
			JSONArray comments = null;
			for (int i = 0; i < data.length(); i++) {
				JSONObject queryResult = data.getJSONObject(i);
				if (queryResult.getString("name").equals("update")) {
					update = queryResult.getJSONArray("fql_result_set");
				} else if (queryResult.getString("name").equals("usernames")) {
					usernames = queryResult.getJSONArray("fql_result_set");
				} else if (queryResult.getString("name").equals("comments")) {
					comments = queryResult.getJSONArray("fql_result_set");
				}
			}
			if (update != null && usernames != null && comments != null) {
				ArrayList<User> users = new ArrayList<User>(usernames.length());
				for (int i = usernames.length() - 1; i >= 0; i--) {
					users.add(User.parseFacebookUser(null, usernames.getJSONObject(i), false));
				}
				Message msg = Message.parseFacebookStreamUpdate(this, update.getJSONObject(0), comments, users);
				msg.updateBundle();
				
				final int l = mHomeUpdates.list.size();
				for (int i = 0; i < l; i++) {
					Message msg2 = mHomeUpdates.list.get(i);
					if (msg.ID == msg2.ID && msg.sender.id == msg2.sender.id) {
						mHomeUpdates.list.set(i, msg);
						mHomeProvider.dispatchChange();
						dispatchHomeChanged(0);
						break;
					}
				}
				return msg;
			} else {
				throw new Exception("Wrong result format");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public User loadUserInfo(long ID) {
		try {
			String query = "{\"user\" : \"" + USER_QUERY + "'" + ID + "'\", \"page\" : \"" + PAGE_QUERY + "'" + ID + "'\"}";
			Bundle params = new Bundle(3);
			params.putString("q", query);
			JSONArray json = new JSONObject(getFacebook().request("fql", params)).getJSONArray("data");
			JSONArray user = null;
			JSONArray page = null;
			for (int i = 0; i < json.length(); i++) {
				JSONObject queryResult = json.getJSONObject(i);
				String name = queryResult.getString("name");
				JSONArray data = queryResult.getJSONArray("fql_result_set");
				if (name.equals("user")) {
					user = data;
				} else if (name.equals("page")) {
					page = data;
				}
			}
			if (user.length() > 0) {
				return User.parseFacebookUser(mContext, user.getJSONObject(0), true);
			} else if (page.length() > 0) {
				return User.parseFacebookPage(mContext, page.getJSONObject(0));
			} else {
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public boolean postComment(Message msg, String comment) {
		try {
			Bundle params = new Bundle(2);
			params.putString("message", comment);
			getFacebook().request(msg.getPostID() + "/comments", params, "POST");
			msg.comments.addComment(new Comment(mUser, comment));
			msg.comments.updateBundle();
			getStorageManager().flushAsync();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	@Override
	public ColumnMessagesProvider addUserProvider(User user) {
		FacebookUserProvider provider = new FacebookUserProvider(this, user);
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
		for (int i = mSearchProviders.size() - 1; i >= 0; i--) {
			msg = findMessage(mSearchProviders.get(i).getMessages(), msgPreID, msgID, type);
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
		return User.findByID(mFriends, id);
	}
	
	private Runnable mUpdateHomeStorage = new Runnable() {
		@Override
		public void run() {
			mHomeUpdates.updateMessages();
			getStorageManager().flushAsync();
		}
	};
	
	private Runnable mUpdateNotificationsStorage = new Runnable() {
		@Override
		public void run() {
			mNotifications.updateMessages();
			getStorageManager().flushAsync();
		}
	};
	
	private Runnable mUpdateMessagesStorage = new Runnable() {
		@Override
		public void run() {
			mMessages.updateMessages();
			getStorageManager().flushAsync();
		}
	};
	
	private void dispatchHomeChanged(int newCount) {
		Threads.runOnAsyncThread(mUpdateHomeStorage);
	}
	
	private void dispatchNotificationsChanged(int newCount) {
		Threads.runOnAsyncThread(mUpdateNotificationsStorage);
	}
	
	private void dispatchMessagesChanged(int newCount) {
		Threads.runOnAsyncThread(mUpdateMessagesStorage);
	}
	
	public boolean removeListener(FacebookAccountListener listener) {
		return mListeners.remove(listener);
	}
	
	public void addListener(FacebookAccountListener listener) {
		mListeners.add(listener);
	}
	
	public void dispatchProvidersChanged() {
		for (int i = 0; i < mListeners.size(); i++) {
			mListeners.get(i).onProvidersChanged(this);
		}
	}
	
	public static interface FacebookAccountListener {
		public void onProvidersChanged(FacebookAccount account);
	}
}
