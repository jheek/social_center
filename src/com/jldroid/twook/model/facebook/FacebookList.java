package com.jldroid.twook.model.facebook;

import com.jdroid.utils.StorageManager.StorageBundle;

public class FacebookList implements Comparable<FacebookList> {
	
	public long id;
	public String name;
	
	private StorageBundle mBundle;
	
	public FacebookList(long id, String name) {
		this.mBundle = new StorageBundle(2);
		this.id = id;
		this.name = name;
	}
	
	public FacebookList(StorageBundle bundle) {
		this.mBundle = bundle;
		this.id = bundle.readLong("ID", -1);
		this.name = bundle.readString("NAME", null);
	}
	
	public StorageBundle peekBundle() {
		return mBundle;
	}
	
	public StorageBundle updateBundle() {
		mBundle.write("ID", id);
		mBundle.write("NAME", name);
		return mBundle;
	}
	
	@Override
	public int compareTo(FacebookList pAnother) {
		return name.compareTo(pAnother.name);
	}
}
