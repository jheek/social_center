package com.jldroid.twook.model.facebook;

import org.json.JSONException;
import org.json.JSONObject;

import com.jdroid.utils.StorageManager.StorageBundle;

public class Album {

	/*{
	      "aid": "100000994585060_32713", 
	      "name": "Profile Pictures", 
	      "created": 1294751105, 
	      "modified": 1318345666, 
	      "can_upload": false, 
	      "photo_count": 1, 
	      "video_count": 0
	}*/
	
	public String aid;
	public String name;
	public long coverID;
	public long createdTime;
	public long updatedTime;
	public boolean canUpload;
	public int photoCount;
	public int videoCount;
	
	private StorageBundle mBundle;
	
	public Album() {
		mBundle = new StorageBundle(7);
	}
	
	public Album(StorageBundle bundle) {
		mBundle = bundle;
		aid = bundle.readString("AID", null);
		name = bundle.readString("NAME", null);
		coverID = bundle.readLong("COVERID", -1);
		createdTime = bundle.readLong("CREATEDTIME", -1);
		updatedTime = bundle.readLong("UPDATEDTIME", -1);
		canUpload = bundle.readBool("CANUPLOAD", false);
		photoCount = bundle.readInt("PHOTOCOUNT", -1);
		videoCount = bundle.readInt("VIDEOCOUNT", -1);
	}
	
	public StorageBundle getBundle() {
		return mBundle;
	}
	
	public StorageBundle updateBundle() {
		mBundle.write("AID", aid);
		mBundle.write("NAME", name);
		mBundle.write("COVERID", coverID);
		mBundle.write("CREATEDTIME", createdTime);
		mBundle.write("UPDATEDTIME", updatedTime);
		mBundle.write("CANUPLOAD", canUpload);
		mBundle.write("PHOTOCOUNT", photoCount);
		mBundle.write("VIDEOCOUNT", videoCount);
		return mBundle;
	}
	
	public String getCoverPictureUri(FacebookAccount fbAccount) {
		return "https://graph.facebook.com/" + coverID + "/picture?type=thumbnail&access_token=" + fbAccount.getFacebook().getAccessToken();
	}
	
	public static Album parseJson(JSONObject json) {
		try {
			Album album = new Album();
			album.aid = json.getString("aid");
			album.name = json.getString("name");
			album.createdTime = json.getLong("created") * 1000;
			album.updatedTime = json.getLong("modified") * 1000;
			album.canUpload = json.getBoolean("can_upload");
			album.photoCount = json.getInt("photo_count");
			album.videoCount = json.getInt("video_count");
			album.coverID = json.getLong("cover_object_id");
			return album;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}
	
}
