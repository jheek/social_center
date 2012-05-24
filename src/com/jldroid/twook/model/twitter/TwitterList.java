package com.jldroid.twook.model.twitter;

import com.jdroid.utils.StorageManager.StorageBundle;

public class TwitterList implements Comparable<TwitterList> {

	public int id;
	public String name;
	
	private StorageBundle mBundle;
	
	public TwitterList(int id, String name) {
		this.mBundle = new StorageBundle(2);
		this.id = id;
		this.name = name;
	}
	
	public TwitterList(StorageBundle bundle) {
		this.mBundle = bundle;
		this.id = bundle.readInt("ID", -1);
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
	public int compareTo(TwitterList pAnother) {
		return name.compareTo(pAnother.name);
	}
}
