package com.jldroid.twook.model;

import java.util.ArrayList;
import java.util.TreeMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;

import com.jdroid.utils.StorageManager.StorageBundle;
import com.jldroid.twook.R;

public class User implements Comparable<User> {
	
	public static final int TYPE_UNKNOWN = -1;
	public static final int TYPE_TWITTER = 0;
	public static final int TYPE_FACEBOOK = 1;
	
	public int type = TYPE_UNKNOWN;
	public long id;
	public String name;
	public String twitterScreenName;
	public String profilePictureUrl;
	public String largeProfilePictureUrl;
	
	public String[] basicInfo;
	public String[] contactInfo;
	public String[] aboutInfo;
	
	private StorageBundle mBundle;
	
	public User() {
		mBundle = new StorageBundle(4);
	}
	
	public User(int type, long id, String name) {
		this(type, id, name, null, null);
	}
	
	public User(int type, long id, String name, String profilePicutreUrl, String largeProfilePictureUrl) {
		this();
		this.type = type;
		this.id = id;
		this.name = name;
		this.profilePictureUrl = profilePicutreUrl;
		this.largeProfilePictureUrl = largeProfilePictureUrl;
	}
	
	public User(Context c, twitter4j.User user, boolean fillDetails) {
		this();
		this.type = TYPE_TWITTER;
		this.id = user.getId();
		this.name = user.getName();
		this.twitterScreenName = user.getScreenName();
		this.profilePictureUrl = this.largeProfilePictureUrl = user.getProfileImageURL().toString();
		
		if (fillDetails) {
			basicInfo = new String[8];
			basicInfo[0] = c.getString(R.string.user_language);
			basicInfo[1] = user.getLang();
			basicInfo[2] = c.getString(R.string.user_followers);
			basicInfo[3] = Integer.toString(user.getFollowersCount());
			basicInfo[4] = c.getString(R.string.user_following);
			basicInfo[5] = Integer.toString(user.getFriendsCount());
			basicInfo[6] = c.getString(R.string.user_status_count);
			basicInfo[7] = Integer.toString(user.getStatusesCount());
			aboutInfo = new String[2];
			aboutInfo[0] = c.getString(R.string.user_description);
			aboutInfo[1] = user.getDescription();
			contactInfo = new String[2];
			contactInfo[0] = c.getString(R.string.user_profile_page);
			contactInfo[1] = "https://twitter.com/#!/" + user.getScreenName();
		}
	}
	
	public User(StorageBundle bundle) {
		mBundle = bundle;
		this.type = bundle.readInt("TYPE", TYPE_UNKNOWN);
		this.id = bundle.readLong("ID", -1);
		this.name = bundle.readString("NAME", null);
		this.twitterScreenName = bundle.readString("SCREENNAME", null);
		this.profilePictureUrl = bundle.readString("PICURL", null);
		this.largeProfilePictureUrl = bundle.readString("LARGEPICURL", null);
		this.basicInfo = mBundle.readStringArray("BASICINFO", null);
		this.contactInfo = mBundle.readStringArray("CONTACTINFO", null);
		this.aboutInfo = mBundle.readStringArray("ABOUTINFO", null);
		if (type == TYPE_FACEBOOK) {
			generateFacebookProfileUri();
		}
	}
	
	public StorageBundle getBundle() {
		return mBundle;
	}
	
	public StorageBundle updateBundle() {
		mBundle.write("TYPE", type);
		mBundle.write("ID", id);
		mBundle.write("NAME", name);
		if (type == TYPE_TWITTER) {
			mBundle.write("PICURL", profilePictureUrl);
			mBundle.write("LARGEPICURL", largeProfilePictureUrl);
			if (twitterScreenName != null) {
				mBundle.write("SCREENNAME", twitterScreenName);
			}
		}
		if (basicInfo != null) {
			mBundle.write("BASICINFO", basicInfo);
		}
		if (contactInfo != null) {
			mBundle.write("CONTACTINFO", contactInfo);
		}
		if (aboutInfo != null) {
			mBundle.write("ABOUTINFO", aboutInfo);
		}
		return mBundle;
	}
	
	public void generateFacebookProfileUri() {
		profilePictureUrl = "http://graph.facebook.com/" + id + "/picture?type=square";
		largeProfilePictureUrl = "http://graph.facebook.com/" + id + "/picture?type=large";
	}
	
	public boolean hasDetailedInfo() {
		return basicInfo != null;
	}
	
	@Override
	public int compareTo(User pAnother) {
		return name.compareToIgnoreCase(pAnother.name);
	}
	
	@Override
	public String toString() {
		if (type == TYPE_TWITTER) {
			return '@' + name;
		} else {
			return name;
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		result = prime * result + type;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof User))
			return false;
		User other = (User) obj;
		if (id != other.id)
			return false;
		if (type != other.type)
			return false;
		return true;
	}
	
	public static void findByQuery(ArrayList<User> target, ArrayList<User> haystack, String needle) {
		for (int i = haystack.size() - 1; i >= 0; i--) {
			User user = haystack.get(i);
			if (user.name.toLowerCase().indexOf(needle) >= 0 && !target.contains(user)) {
				target.add(user);
			}
		}
	}

	public static User findByID(ArrayList<User> users, long id) {
		for (int i = users.size() - 1; i >= 0; i--) {
			User user = users.get(i);
			if (user.id == id) {
				return user;
			}
		}
		return null;
	}
	
	public static boolean containsUserByID(ArrayList<User> users, long id) {
		return findByID(users, id) != null;
	}
	
	public static User findByID(TreeMap<Long, User> users, long id) {
		return users.get(id);
	}
	
	public static User parseFacebookUser(Context c, JSONObject json, boolean details) {
		try {
			User user = new User();
			user.type = TYPE_FACEBOOK;
			user.id = json.getLong(details ? "uid" : "id");
			if (details) {
				user.id = json.getLong("uid");
			} else {
				long idLong = json.optLong("id", Long.MIN_VALUE);
				if (idLong != Long.MIN_VALUE) {
					user.id = idLong;
				} else {
					user.id = Long.parseLong(json.getString("id"));
				}
				user.id = json.getLong("id");
			}
			user.name = json.getString("name");
			if (details) {
				user.profilePictureUrl = json.getString("pic_square");
				user.largeProfilePictureUrl = json.getString("pic_big");
				ArrayList<String> list = new ArrayList<String>(10);
				addInfoString(c, list, json, R.string.user_religion, "religion");
				addInfoString(c, list, json, R.string.user_birthday, "birthday");
				addInfoString(c, list, json, R.string.user_sex, "sex");
				addInfoString(c, list, json, R.string.user_relation_status, "relationship_status");
				user.basicInfo = copyInfo(list);
				list.clear();
				addInfoString(c, list, json, R.string.user_friends, "friend_count");
				addInfoString(c, list, json, R.string.user_mutual_friends, "mutual_friend_count");
				addInfoString(c, list, json, R.string.user_quotes, "quotes");
				addInfoString(c, list, json, R.string.user_about_me, "about_me");
				user.aboutInfo = copyInfo(list);
				list.clear();
				addInfoString(c, list, json, R.string.user_website, "website");
				addInfoString(c, list, json, R.string.user_email, "email");
				JSONObject homeTownJson = json.optJSONObject("hometown_location");
				if (homeTownJson != null) {
					addInfoString(c, list, homeTownJson, R.string.user_hometown, "name");
				}
				user.contactInfo = copyInfo(list);
				user.updateBundle();
			} else {
				if (json.has("pic_square") && json.has("pic_big")) {
					user.profilePictureUrl = json.getString("pic_square");
					user.largeProfilePictureUrl = json.getString("pic_big");
				} else {
					user.generateFacebookProfileUri();
				}
			}
			return user;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static User parseFacebookPage(Context c, JSONObject json) {
		try {
			User user = new User();
			user.id = json.getLong("page_id");
			user.name = json.getString("name");
			user.profilePictureUrl = json.getString("pic_square");
			user.largeProfilePictureUrl = json.getString("pic_big");
			ArrayList<String> list = new ArrayList<String>(4);
			addInfoString(c, list, json, R.string.user_general_info, "general_info");
			addInfoInt(c, list, json, R.string.user_fan_count, "fan_count");
			user.basicInfo = copyInfo(list);
			list.clear();
			addInfoString(c, list, json, R.string.user_description, "description");
			user.aboutInfo = copyInfo(list);
			list.clear();
			addInfoString(c, list, json, R.string.user_website, "website");
			addInfoString(c, list, json, R.string.user_phone, "phone");
			user.contactInfo = copyInfo(list);
			user.updateBundle();
			return user;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private static void addInfoString(Context c, ArrayList<String> target, JSONObject json, int name, String jsonName) {
		if (json.isNull(jsonName)) {
			return;
		}
		String value = json.optString(jsonName);
		if (!TextUtils.isEmpty(value)) {
			target.add(c.getString(name));
			target.add(value);
		}
	}
	
	private static void addInfoInt(Context c, ArrayList<String> target, JSONObject json, int name, String jsonName) {
		if (json.isNull(jsonName)) {
			return;
		}
		int value = json.optInt(jsonName, Integer.MIN_VALUE);
		if (value != Integer.MIN_VALUE) {
			target.add(c.getString(name));
			target.add(Integer.toString(value));
		}
	}
	
	private static String[] copyInfo(ArrayList<String> list) {
		String[] r = new String[list.size()];
		for (int i = list.size() - 1; i >= 0; i--) {
			r[i] = list.get(i);
		}
		return r;
	}
	
}
