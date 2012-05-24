package com.jldroid.twook.model;

import android.content.Context;
import android.preference.PreferenceManager;
import android.provider.Settings.System;

import com.jdroid.utils.StorageManager;
import com.jdroid.utils.StorageManager.StorageBundle;

public class ColumnInfo implements Comparable<ColumnInfo>, ColumnProviderListener {
	
	private Context mContext;
	private StorageBundle mBundle;
	
	private IAccount mAccount;
	private ColumnMessagesProvider mProvider;
	
	// last read
	private long mLastReadMessageID;
	private int mLastReadMessageType;
	private int mLastReadMessageTop;
	
	private int mOrder;
	
	private boolean isEnabled = false;
	
	private boolean mInForeground = false;
	
	private int mShowNotification;
	private boolean mSound;
	private String mRingtoneUri;
	private boolean mVibrate;
	private boolean mLED;
	
	private long mSyncInterval;
	
	public ColumnInfo(Context c, int order, IAccount account, ColumnMessagesProvider provider) {
		mContext = c;
		mOrder = order;
		mAccount = account;
		mProvider = provider;
		mBundle = StorageManager.getDeflaut(c).readBundle(provider.getStorageName(), null);
		if (mBundle == null) {
			mBundle = new StorageBundle(2);
			StorageManager.getDeflaut(c).write(provider.getStorageName(), mBundle);
			StorageManager.getDeflaut(c).flushAsync(500);
		}
		mLastReadMessageID = mBundle.readLong("LAST_READ_ID", -1);
		mLastReadMessageType = mBundle.readInt("LAST_READ_TYPE", -1);
		mLastReadMessageTop = mBundle.readInt("LAST_READ_TOP", 0);
		
		mSyncInterval = mBundle.readLong("SYNC_INTERVAL", -2);
		
		// account = null when special column special columns should be enabled by default
		isEnabled = mBundle.readBool("ENABLED", false);
		if (mBundle.contains("SHOWNOTIFICATION")) {
			mShowNotification = mBundle.readBool("SHOWNOTIFICATION", false) ? 1 : 0;
		} else {
			mShowNotification = -1;
		}
		mSound = mBundle.readBool("SOUND", true);
		mRingtoneUri = mBundle.readString("RINGTONE", System.DEFAULT_NOTIFICATION_URI.toString());
		mVibrate = mBundle.readBool("VIBRATE", true);
		mLED = mBundle.readBool("LED", true);
		
		provider.addListener(this);
		
		// TODO mProvider.setUnreadMessageCount(mBundle.readInt("UNREAD", 0));
	}
	
	public IAccount getAccount() {
		return mAccount;
	}

	public void setLastReadMessage(long id, int type, int top) {
		mLastReadMessageID = id;
		mLastReadMessageType = type;
		mLastReadMessageTop = top;
		mBundle.write("LAST_READ_ID", id);
		mBundle.write("LAST_READ_TYPE", type);
		mBundle.write("LAST_READ_TOP", top);
	}
	
	public long getLastReadMessageID() {
		return mLastReadMessageID;
	}
	
	public int getLastReadMessageType() {
		return mLastReadMessageType;
	}
	
	public int getLastReadMessageTop() {
		return mLastReadMessageTop;
	}
	
	public long getSyncInterval() {
		return mSyncInterval;
	}
	
	public void setSyncInterval(long pSyncInterval) {
		if (mSyncInterval != pSyncInterval) {
			mSyncInterval = pSyncInterval;
			mBundle.write("SYNC_INTERVAL", pSyncInterval);
			StorageManager.getDeflaut(mContext).flushAsync();
			SyncManager.updateColumnSync(mContext, this);
		}
	}
	
	public ColumnMessagesProvider getProvider() {
		return mProvider;
	}
	
	public boolean isEnabled() {
		return isEnabled;
	}
	
	public boolean isShowNotification() {
		if (mShowNotification != -1) {
			return mShowNotification == 1;
		} else {
			int defaultNotification = PreferenceManager.getDefaultSharedPreferences(mContext).getInt("defaultNotification", 0);
			switch (defaultNotification) {
			case 0: // Always
				return true;
			case 1: // personal
				return mProvider.isPersonal();
			case 2: // never
				return false;
			default:
				throw new RuntimeException("Unknown default notification: " + defaultNotification);
			}
		}
	}
	
	public boolean isSound() {
		return mSound;
	}
	
	public String getRingtoneUri() {
		return mRingtoneUri;
	}
	
	public boolean isVibrate() {
		return mVibrate;
	}
	
	public boolean isLED() {
		return mLED;
	}
	
	public void setShowNotification(boolean pShowNotification) {
		mShowNotification = pShowNotification ? 1 : 0;
		mBundle.write("SHOWNOTIFICATION", pShowNotification);
		StorageManager.getDeflaut(mContext).flushAsync();
	}
	
	public void setSound(boolean pSound) {
		mSound = pSound;
		mBundle.write("SOUND", pSound);
		StorageManager.getDeflaut(mContext).flushAsync();
	}
	
	public void setRingtoneUri(String pRingtoneUri) {
		mRingtoneUri = pRingtoneUri;
		mBundle.write("RINGTONE", pRingtoneUri);
		StorageManager.getDeflaut(mContext).flushAsync();
	}
	
	public void setVibrate(boolean pVibrate) {
		mVibrate = pVibrate;
		mBundle.write("VIBRATE", pVibrate);
		StorageManager.getDeflaut(mContext).flushAsync();
	}
	
	public void setLED(boolean pLED) {
		mLED = pLED;
		mBundle.write("LED", pLED);
		StorageManager.getDeflaut(mContext).flushAsync();
	}
	
	protected void setEnabled(boolean pIsEnabled) {
		isEnabled = pIsEnabled;
		mBundle.write("ENABLED", pIsEnabled);
		StorageManager.getDeflaut(mContext).flushAsync();
	}
	
	public void setInForeground(boolean v) {
		if (mInForeground != v) {
			mInForeground = v;
			updateNotification();
		}
	}
	
	private void updateNotification() {
		if (mInForeground) {
			mProvider.resetUnreadMessages();
		}
		if (isShowNotification() && isEnabled) {
			UnreadNotificationManager.getInstance(mContext).updateNotification(mProvider, mSound, mRingtoneUri, mVibrate, mLED);
		} else {
			UnreadNotificationManager.getInstance(mContext).removeNotification(mProvider);
		}
		
	}
	
	@Override
	public void onHasOlderMessagesChanged(boolean pV) {
	}
	
	@Override
	public void onUpdateStateChanged(boolean pIsUpdating) {
	}
	
	@Override
	public void onEnlargingStateChanged(boolean pIsEnlarging) {
	}
	
	@Override
	public void onMessagesChanged() {
		updateNotification();
	}
	
	@Override
	public int compareTo(ColumnInfo pAnother) {
		int i1 = mOrder;
		int i2 = pAnother.mOrder;
		if (i1 == i2) {
			i1 = mProvider.getOrder();
			i2 = pAnother.mProvider.getOrder();
		}
		return i1 > i2 ? 1 : (i1 < i2 ? -1 : 0);
	}
	
}
