package com.jldroid.twook.model;

import com.jldroid.twook.PostService;
import java.util.ArrayList;
import com.jdroid.utils.StorageManager;
import com.jdroid.utils.StorageManager.StorageBundle;

import android.content.Context;
import android.content.Intent;

public class PendingPostManager {
	
	private static PendingPostManager sInstance;
	
	private Context mContext;
	
	private ArrayList<PendingPost> mPendingPosts = new ArrayList<PendingPost>();
	private ArrayList<StorageBundle> mBundles;
	
	public PendingPostManager(Context c) {
		mContext = c;
		StorageManager sm = StorageManager.getDeflaut(c);
		mBundles = sm.readBundleArrayList("PENDING_POSTS");
		for (int i = 0; i < mBundles.size(); i++) {
			mPendingPosts.add(new PendingPost(c, mBundles.get(i)));
		}
		if (mPendingPosts.size() > 0) {
			startPostService();
		}
	}
	
	public synchronized void add(PendingPost post) {
		mPendingPosts.add(post);
		mBundles.add(post.updateBundle());
		updateStorage();
		startPostService();
	}
	
	public synchronized PendingPost next() {
		if (mPendingPosts.size() == 0) {
			return null;
		}
		return mPendingPosts.get(0);
	}
	
	public synchronized void removeFromQeue(PendingPost post) {
		int index = mPendingPosts.indexOf(post);
		if (index != -1) {
			mPendingPosts.remove(index);
			mBundles.remove(index);
			updateStorage();
		}
	}
	
	public synchronized boolean hasPendingPosts() {
		return mPendingPosts.size() > 0;
	}
	
	private void updateStorage() {
		StorageManager sm = StorageManager.getDeflaut(mContext);
		sm.writeBundleArray("PENDING_POSTS", mBundles);
		sm.flushAsync();
	}
	
	private void startPostService() {
		mContext.startService(new Intent(PostService.ACTION_POST_PENDING));
	}
	
	public synchronized static PendingPostManager getInstance(Context c) {
		if (sInstance == null) {
			sInstance = new PendingPostManager(c.getApplicationContext());
		}
		return sInstance;
	}
}
