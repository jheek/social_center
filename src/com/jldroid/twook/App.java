package com.jldroid.twook;

import com.jdroid.utils.StorageManager;

import android.app.Application;

public class App extends Application {
	
	private static final int CURRENT_STORAGE_VERSION = 1;
	private static final int MIN_STORAGE_VERSION = 1;
	
	private static final String STORAGE_VERSION = "storageVersion";
	
	@Override
	public void onCreate() {
		super.onCreate();
		StorageManager sm = StorageManager.getDeflaut(this);
		int storageVersion = sm.readInt(STORAGE_VERSION, -1);
		if (storageVersion < MIN_STORAGE_VERSION) {
			sm.deleteAll();
			sm.write(STORAGE_VERSION, CURRENT_STORAGE_VERSION);
			sm.flushAsync();
		}
	}
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		Globals.cleanup(this, false);
	}
}
