package com.jldroid.twook.model.facebook;

import com.jdroid.utils.StorageManager.StorageBundle;

public class FacebookGroup implements Comparable<FacebookGroup> {
	
	public long id;
	public String name;
	
	private StorageBundle mBundle;
	
	public FacebookGroup(long id, String name) {
		this.mBundle = new StorageBundle(2);
		this.id = id;
		this.name = name;
	}
	
	public FacebookGroup(StorageBundle bundle) {
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
	public int compareTo(FacebookGroup pAnother) {
		return name.compareTo(pAnother.name);
	}
}
