package com.jldroid.twook.model;

import com.jldroid.twook.Globals;
import com.jdroid.utils.SortedArrayList;
import com.jdroid.utils.StorageManager.StorageBundle;

public class SyncableData {
	
	public SortedArrayList<Message> list = new SortedArrayList<Message>();
	public SortedArrayList<Message> newList = new SortedArrayList<Message>();
	
	public boolean isUpdating = false;
	public boolean isEnlarging = false;
	
	public boolean hasOlder = true;
	
	protected long lastUpdate = -1;
	
	private StorageBundle mBundle;
	
	public SyncableData(IAccount account, StorageBundle bundle) {
		if (bundle == null) {
			bundle = new StorageBundle(3);
		}
		mBundle = bundle;
		lastUpdate = bundle.readLong("LAST_UPDATE", -1);
		StorageBundle[] bundles = bundle.readBundleArray("MESSAGES", null);
		if (bundles != null) {
			final int l = bundles.length;
			ensureCapacity(l);
			for (int i = 0; i < l; i++) {
				StorageBundle msgBundle = bundles[i];
				Message msg = Message.parseBundle(account, msgBundle);
				this.list.add(msg);
			}
		}
	}
	
	public void clear() {
		list.clear();
		setLastUpdate(-1);
		hasOlder = true;
		updateMessages();
	}
	
	public void updateMessages() {
		updateMessages(Globals.MIN_TIME_ON_STORAGE, Globals.MIN_COUNT_ON_STORAGE);
	}
	
	public void updateMessages(long minTime, int minCount) {
		final int l = list.size();
		minTime = minTime != -1 ? System.currentTimeMillis() - minTime : System.currentTimeMillis();
		int bundlesLength = 0;
		while (bundlesLength < l) {
			Message msg = list.get(bundlesLength);
			if (msg.createdTime < minTime && bundlesLength >= minCount) {
				break;
			}
			bundlesLength++;
		}
		StorageBundle[] bundles = new StorageBundle[bundlesLength];
		for (int i = bundles.length - 1; i >= 0; i--) {
			bundles[i] = list.get(i).peekBundle();
		}
		mBundle.write("MESSAGES", bundles);
	}
	
	public void ensureCapacity(int l) {
		list.ensureCapacity(l);
		newList.ensureCapacity(l);
	}
	
	public void swap() {
		SortedArrayList<Message> swap = list;
		list = newList;
		newList = swap;
		newList.clear();
	}
	
	public long getLastUpdate() {
		return lastUpdate;
	}
	
	public void setLastUpdate(long pLastUpdate) {
		lastUpdate = pLastUpdate;
		mBundle.write("LAST_UPDATE", pLastUpdate);
	}
	
	public void setSyncInterval(long pSyncInterval) {
		
	}
	
	public StorageBundle getBundle() {
		return mBundle;
	}
	
	public static abstract class BaseSyncableDataProvider extends BaseColumnMessagesProvider {

		
		public BaseSyncableDataProvider() {
		}
		
		protected abstract SyncableData getData();

		@Override
		public SortedArrayList<Message> getMessages() {
			return getData().list;
		}

		@Override
		public boolean isUpdateable() {
			return true;
		}

		@Override
		public boolean hasOlderMessages() {
			return getData().hasOlder;
		}
		
		@Override
		public long getLastUpdate() {
			return getData().lastUpdate;
		}
		
		@Override
		public void dispatchChange() {
			super.dispatchChange();
		}
	}
	
}
