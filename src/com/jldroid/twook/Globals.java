package com.jldroid.twook;

import android.content.Context;

import com.jdroid.utils.StorageManager;
import com.jldroid.twook.model.ImageManager;

public class Globals {
	
	public static final long MIN_TIME_ON_STORAGE = 24 * 3600000;
	public static final int MIN_COUNT_ON_STORAGE = 40;
	
	public static final long GLOBAL_SYNC_INTERVAL = 24 * 3600000;
	
	public static void cleanup(Context c, boolean gc) {
		StorageManager sm = StorageManager.peekDefault();
		if (sm != null) {
			sm.optimize();
		}
		sm = null;
		ImageManager.getInstance(c).releaseCache();
		if (gc) {
			Runtime.getRuntime().gc();
		}
	}
	
	private Globals() {
	}
	
}
