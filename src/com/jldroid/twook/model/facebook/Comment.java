package com.jldroid.twook.model.facebook;

import org.json.JSONException;
import org.json.JSONObject;

import com.jldroid.twook.model.User;
import java.util.ArrayList;
import com.jdroid.utils.StorageManager.StorageBundle;

public class Comment implements Comparable<Comment> {
	
	public long ID;
	public User sender;
	public long time;
	public String text;
	public int likes;
	public boolean userLikes;
	
	private StorageBundle mBundle;
	
	/*
	 *sample of a comment {
                "fromid": 100000994585060, 
                "time": 1320504021, 
                "text": "ben je aan het filosoferen maarten?", 
                "likes": 0, 
                "user_likes": false
              }
	 */
	
	public Comment(JSONObject json, ArrayList<User> users) {
		try {
			String IDStr = json.getString("id");
			ID = Long.parseLong(IDStr.substring(IDStr.lastIndexOf('_') + 1));
			sender = User.findByID(users, json.getLong("fromid"));
			time = json.getLong("time") * 1000l;
			text = json.getString("text");
			likes = json.getInt("likes");
			userLikes = json.getBoolean("user_likes");
			updateBundle();
		} catch (JSONException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot parse comment: " + json.toString());
		}
	}
	
	public Comment(StorageBundle bundle) {
		this.mBundle = bundle;
		ID = bundle.readLong("ID", -1);
		sender = new User(bundle.readBundle("SENDER", null));
		time = bundle.readLong("TIME", -1);
		text = bundle.readString("TEXT", null);
		likes = bundle.readInt("LIKES", 0);
		userLikes = bundle.readBool("USERLIKES", false);
	}
	
	public Comment(User user, String text) {
		this.sender = user;
		this.time = System.currentTimeMillis();
		this.text = text;
		this.likes = 0;
		this.userLikes = false;
		updateBundle();
	}
	
	public StorageBundle getBundle() {
		if (mBundle == null) {
			mBundle = new StorageBundle(5);
		}
		return mBundle;
	}
	
	public void updateBundle() {
		StorageBundle bundle = getBundle();
		bundle.deleteAll();
		bundle.write("ID", ID);
		bundle.write("SENDER", sender.updateBundle());
		bundle.write("TIME", time);
		bundle.write("TEXT", text);
		bundle.write("LIKES", likes);
		bundle.write("USERLIKES", userLikes);
	}
	
	@Override
	public int compareTo(Comment another) {
		long l1 = this.time;
		long l2 = another.time;
		return l1 > l2 ? 1 : (l1 < l2 ? -1 : 0) ;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (sender == null ? 0 : sender.hashCode());
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		result = prime * result + (int) (time ^ (time >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Comment))
			return false;
		Comment other = (Comment) obj;
		if (!sender.equals(other.sender))
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		if (time != other.time)
			return false;
		return true;
	}
	
}
