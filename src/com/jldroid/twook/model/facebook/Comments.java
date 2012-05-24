package com.jldroid.twook.model.facebook;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.jldroid.twook.model.User;
import java.util.ArrayList;
import com.jdroid.utils.SortedArrayList;
import com.jdroid.utils.StorageManager.StorageBundle;

public class Comments {
	
	private SortedArrayList<Comment> mComments = new SortedArrayList<Comment>();
	private int mCommentCount;
	
	private boolean mCanPost;
	private boolean mCanRemove;
	
	private StorageBundle mBundle;
	
	public Comments(JSONObject json, String postID, JSONArray comments, ArrayList<User> users) {
		parseJSON(json, postID, comments, users);
	}
	
	public Comments(StorageBundle bundle) {
		if (bundle == null) {
			bundle = new StorageBundle();
		}
		mBundle = bundle;
		mCanPost = bundle.readBool("CAN_POST", true);
		mCanRemove = bundle.readBool("CAN_REMOVE", false);
		mCommentCount = bundle.readInt("COUNT", 0);
	}
	
	public StorageBundle getBundle() {
		if (this.mBundle == null) {
			this.mBundle = new StorageBundle(3);
		}
		return this.mBundle;
	}
	
	public void parseJSON(JSONObject json, String postID, JSONArray comments, ArrayList<User> users) {
		try {
			if (json != null) {
				mCanPost = json.getBoolean("can_post");
				mCanRemove = json.getBoolean("can_remove");
				mComments.ensureCapacity(mCommentCount = json.getInt("count"));
			} else {
				mCanPost = true;
				mCanRemove = false;
				mComments.ensureCapacity(comments != null ? comments.length() : 0);
			}
			if (comments != null) {
				if (postID != null) {
					for (int i = comments.length() - 1; i >= 0; i--) {
						JSONObject comment = comments.getJSONObject(i);
						if (comment.getString("post_id").equals(postID)) {
							mComments.add(new Comment(comment, users));
						}
					}
				} else {
					for (int i = comments.length() - 1; i >= 0; i--) {
						JSONObject comment = comments.getJSONObject(i);
						mComments.add(new Comment(comment, users));
					}
				}
			}
			
			updateBundle();
		} catch (JSONException e) {
			throw new RuntimeException("Cannot parse comments: " + json.toString(), e);
		}
	}
	
	public void updateBundle() {
		StorageBundle bundle = getBundle();
		bundle.write("CAN_POST", mCanPost);
		bundle.write("CAN_REMOVE", mCanRemove);
		bundle.write("COUNT", mCommentCount);
	}
	
	public boolean isCanPost() {
		return mCanPost;
	}
	
	public boolean isCanRemove() {
		return mCanRemove;
	}
	
	public int getAvailableCount() {
		return mComments.size();
	}
	
	public int getRealCount() {
		return mCommentCount;
	}
	
	public Comment getComment(int i) {
		return mComments.get(i);
	}
	
	public void addComment(Comment comment) {
		mComments.add(comment);
		mCommentCount = Math.max(mCommentCount, mComments.size());
	}
	
}
