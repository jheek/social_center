package com.jldroid.twook.model.facebook;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;

import com.jldroid.twook.model.Message;

public class Photo {

	public String pid;
	public String src;
	public String srcSmall;
	public String srcBig;
	public long objectId;
	public long created;
	public long updated;
	public FacebookAccount facebookAccount;
	
	private Message mMessage;
	
	public Photo() {
	}
	
	public Photo(FacebookAccount fa, Bundle bundle) {
		facebookAccount = fa;
		pid = bundle.getString("pid");
		src = bundle.getString("src");
		srcSmall = bundle.getString("srcSmall");
		srcBig = bundle.getString("srcBig");
		objectId = bundle.getLong("objectId");
		created = bundle.getLong("created");
		updated = bundle.getLong("updated");
	}
	
	public Message peekMessage() {
		return mMessage;
	}
	
	public Message loadMessage() {
		if (mMessage == null) {
			reloadMessage();
		}
		return mMessage;
	}
	
	public Message reloadMessage() {
		Message msg = facebookAccount.loadPhotoAsMessage(this);
		if (msg != null) {
			mMessage = msg;
		}
		return msg;
	}
	
	public static Photo parseFacebookAlbumPhoto(FacebookAccount account, JSONObject json) {
		try {
			Photo photo = new Photo();
			photo.facebookAccount = account;
			photo.pid = json.getString("pid");
			photo.src = json.getString("src");
			photo.srcSmall = json.getString("src_small");
			photo.srcBig = json.getString("src_big");
			photo.objectId = json.getLong("object_id");
			photo.created = json.getLong("created") * 1000;
			photo.updated = json.getLong("modified") * 1000;
			return photo;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Bundle asBundle() {
		Bundle bundle = new Bundle(7);
		bundle.putString("pid", pid);
		bundle.putString("src", src);
		bundle.putString("srcSmall", srcSmall);
		bundle.putString("srcBig", srcBig);
		bundle.putLong("objectId", objectId);
		bundle.putLong("created", created);
		bundle.putLong("updated", updated);
		return bundle;
	}
	
}
