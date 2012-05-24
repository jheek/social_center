package com.jldroid.twook.model;

import java.util.ArrayList;
import com.jdroid.utils.SortedArrayList;

public class Chat implements Comparable<Chat> {

	private IAccount mAccount;
	private long mID;
	private SortedArrayList<ChatMessage> mMessages = new SortedArrayList<ChatMessage>();
	
	private ArrayList<ChatListener> mListeners = new ArrayList<ChatListener>();
	
	private long mUpdatedTime = -1;
	
	private boolean isDirty = false;
	
	private ArrayList<User> mParticipants = new ArrayList<User>();
	
	private int mUnread = 0;
	private boolean mInForeground = false;
	
	public Chat(IAccount account, long id) {
		mAccount = account;
		mID = id;
	}
	
	public SortedArrayList<ChatMessage> getMessages() {
		return mMessages;
	}
	
	public User getLastActiveUser() {
		for (int i = mMessages.size() - 1; i >= 0; i--) {
			ChatMessage msg = mMessages.get(i);
			if (!msg.isMe) {
				return msg.sender;
			}
		}
		return null;
	}
	
	public void removeMessagesWithoutID() {
		for (int i = mMessages.size() - 1; i >= 0; i--) {
			if (mMessages.get(i).ID == -1) {
				mMessages.remove(i);
			}
		}
	}
	
	public void addMessage(ChatMessage msg, boolean useAsUpdateTime) {
		if (!mMessages.contains(msg)) {
			mMessages.add(msg);
			if (useAsUpdateTime) {
				mUpdatedTime = Math.max(mUpdatedTime, msg.time);
			}
			dispatchMessagesChanged(1);
		}
	}
	
	public void addListener(ChatListener listener) {
		mListeners.add(listener);
	}
	
	public void removeListener(ChatListener listener) {
		mListeners.remove(listener);
	}
	
	public long getID() {
		return mID;
	}
	
	public IAccount getAccount() {
		return mAccount;
	}
	
	public boolean isDirty() {
		return isDirty;
	}
	
	public void setDirty(boolean pIsDirty) {
		isDirty = pIsDirty;
	}
	
	public long getUpdatedTime() {
		return mUpdatedTime;
	}
	
	/*public void setUpdatedTime(long pUpdatedTime) {
		mUpdatedTime = pUpdatedTime;
	}*/
	
	public int getUnread() {
		return mUnread;
	}
	
	public void setUnread(int pUnread) {
		if (!mInForeground) {
			mUnread = pUnread;
		}
	}
	
	public void addUnread(int pCount) {
		setUnread(mUnread + pCount);
	}

	public boolean isInForeground() {
		return mInForeground;
	}
	
	public void setInForeground(boolean pInForeground) {
		mInForeground = pInForeground;
		if (pInForeground) {
			mUnread = 0;
		}
	}
	
	public ArrayList<User> getParticipants() {
		return mParticipants;
	}
	
	public void dispatchMessagesChanged(int newCount) {
		for (int i = mListeners.size() - 1; i >= 0; i--) {
			mListeners.get(i).onMessagesChanged(newCount);
		}
	}
	
	@Override
	public int compareTo(Chat pAnother) {
		long l1 = mUpdatedTime;
		long l2 = pAnother.mUpdatedTime;
		return l1 > l2 ? 1 : (l1 < l2 ? -1 : 0);
	}
	
	public static interface ChatListener {
		public void onMessagesChanged(int newCount);
	}
}
