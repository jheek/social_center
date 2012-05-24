package com.jldroid.twook.model.twitter;

import com.jdroid.utils.StorageManager.StorageBundle;

public class TwitterUser {
	
	public long id;
	public String name;
	public String profilePictureUrl;
	
	private StorageBundle mBundle;
	
	public TwitterUser() {
		mBundle = new StorageBundle(3);
	}
	
	public TwitterUser(long id, String name, String profilePicutreUrl) {
		this();
		this.id = id;
		this.name = name;
		this.profilePictureUrl = profilePicutreUrl;
	}
	
	public TwitterUser(StorageBundle bundle) {
		mBundle = bundle;
		this.id = bundle.readLong("ID", -1);
		this.name = bundle.readString("NAME", null);
		this.profilePictureUrl = bundle.readString("PICURL", null);
	}
	
	public StorageBundle getBundle() {
		return mBundle;
	}
	
	public StorageBundle updateBundle() {
		mBundle.write("ID", id);
		mBundle.write("NAME", name);
		mBundle.write("PICURL", profilePictureUrl);
		return mBundle;
	}
}
