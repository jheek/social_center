package com.jldroid.twook.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import twitter4j.DirectMessage;
import twitter4j.HashtagEntity;
import twitter4j.Status;
import twitter4j.Tweet;
import twitter4j.UserMentionEntity;
import android.content.Context;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.Log;

import com.jdroid.utils.SortedArrayList;
import com.jdroid.utils.StorageManager.StorageBundle;
import com.jdroid.utils.TimeUtils;
import com.jldroid.twook.R;
import com.jldroid.twook.model.facebook.Comments;
import com.jldroid.twook.model.facebook.FacebookAccount;
import com.jldroid.twook.model.facebook.FacebookImage;
import com.jldroid.twook.model.facebook.Photo;
import com.jldroid.twook.model.twitter.TwitterAccount;

public final class Message implements Comparable<Message> {
	
	public static final String EXTRA_ACCOUNT = "com.jldroid.twook.ACCOUNT";
	public static final String EXTRA_MESSAGE_PRE_ID = "com.jldroid.twook.MSG_PRE_ID";
	public static final String EXTRA_MESSAGE_ID = "com.jldroid.twook.MSG_ID";
	public static final String EXTRA_MESSAGE_TYPE = "com.jldroid.twook.MSG_TYPE";
	
	public static final int TYPE_UNKNOWN = -1;
	
	public static final int TYPE_TWITTER_TIMELINE = 0;
	public static final int TYPE_TWITTER_MENTION = 1;
	public static final int TYPE_TWITTER_DIRECT_MESSAGE = 2;
	public static final int TYPE_TWITTER_SEARCH = 3;
	
	public static final int TYPE_FACEBOOK_HOME = 3;
	public static final int TYPE_FACEBOOK_NOTIFICATION = 4;
	public static final int TYPE_FACEBOOK_PHOTO = 5;
	public static final int TYPE_FACEBOOK_MESSAGE = 6;
	public static final int TYPE_FACEBOOK_GRAPH = 7;
	
	public static final int NOTIFICATION_UNKNOWN = -1;
	public static final int NOTIFICATION_STREAM = 0;
	public static final int NOTIFICATION_GROUP = 1;
	public static final int NOTIFICATION_PHOTO = 2;
	
	// nonpersistent
	public boolean isDirty = false;
	
	// general
	public long PRE_ID = -1;
	public long ID = -1;
	public int type = TYPE_UNKNOWN;
	
	public String source;
	
	public String text;
	
	public long createdTime;
	public long updatedTime;
	
	public User sender;
	public User target;
	
	private StorageBundle mBundle;
	
	public IAccount account;
	
	public int numLikes = 0;
	public boolean canLike = false;
	public boolean userLikes = false;
	
	public Comments comments;
	
	public ArrayList<FacebookImage> facebookImages;
	
	public int notificationType;
	public String notificationTargetID;
	
	private Message mNotificationTargetMessage;
	
	public Photo photo;
	
	public User[] recipients;
	
	private SpannableString mSpannable;
	
	public int[] hashtagStarts;
	public int[] hashtagEnds;
	
	public long[] mentionIds;
	public int[] mentionStarts;
	public int[] mentionEnds;
	
	private CharSequence mTitle;
	private CharSequence mInfo;
	private StringBuilder mTime = new StringBuilder();
	
	public Message() {
	}
	
	public StorageBundle peekBundle() {
		return this.mBundle;
	}
	
	public StorageBundle updateBundle() {
		if (this.mBundle == null) {
			this.mBundle = new StorageBundle(isFacebook() ? 15 : 6);
		}
		StorageBundle bundle = this.mBundle;
		bundle.deleteAll();
		bundle.write("ID", ID);
		bundle.write("TYPE", type);
		bundle.write("SENDER", sender.updateBundle());
		if (target != null) bundle.write("TARGET", target.updateBundle());
		bundle.write("TEXT", text);
		bundle.write("CREATED_TIME", createdTime);
		
		if (recipients != null) {
			StorageBundle[] bundles = new StorageBundle[recipients.length];
			for (int i = 0; i < bundles.length; i++) {
				bundles[i] = recipients[i].updateBundle();
			}
			bundle.write("RECIPIENTS", bundles);
		}
    	
    	if (mentionStarts != null) {
    		bundle.write("MENTIONIDS", mentionIds);
    		bundle.write("MENTIONSTARTS", mentionStarts);
    		bundle.write("MENTIONENDS", mentionEnds);
    	}
    	if (hashtagStarts != null) {
    		bundle.write("HASHTAGSTARTS", hashtagStarts);
    		bundle.write("HASHTAGENDS", hashtagEnds);
    	}
		
		if (isTwitter()) {
			// write twitter specific data
			bundle.write("SOURCE", source);
		} else if (isFacebook()) {
			// write facebook specific data
			bundle.write("PRE_ID", PRE_ID);
			bundle.write("UPDATED_TIME", updatedTime);
			bundle.write("NUM_LIKES", numLikes);
			bundle.write("CAN_LIKE", canLike);
			if (comments != null)bundle.write("COMMENTS", comments.getBundle());
			bundle.write("USER_LIKES", userLikes);
			if (this.facebookImages != null) {
				final int l = facebookImages.size();
				StorageBundle[] bundles = new StorageBundle[l];
				for (int i = 0; i < l; i++) {
					FacebookImage img = facebookImages.get(i);
					bundles[i] = img.updateBundle();
				}
				bundle.write("FACEBOOK_IMAGES", bundles);
			}
			if (type == TYPE_FACEBOOK_NOTIFICATION) {
				bundle.write("NOTIFICATION_TYPE", notificationType);
				bundle.write("NOTIFICATION_TARGET", notificationTargetID);
			}
		}
		return bundle;
	}
	
	public boolean isTwitter() {
		return account instanceof TwitterAccount;
	}
	
	public boolean isFacebook() {
		return account instanceof FacebookAccount;
	}
	
	public String getPostID() {
		if (type == TYPE_FACEBOOK_PHOTO) {
			return Long.toString(ID);
		} else {
			if (PRE_ID != -1) {
				return String.valueOf(PRE_ID) + '_' + String.valueOf(ID);
			}
			return Long.toString(sender.id) + '_' + Long.toString(ID);
		}
	}
	
	private static final int MAX_RECIPIENTS = 3;
	
	public CharSequence getTitle(Context c) {
		if (mTitle != null) {
			return mTitle;
		}
		if (recipients != null) {
			StringBuilder sb = new StringBuilder(64);
			int count = 0;
			for (int i = 0; i < recipients.length; i++) {
				if (recipients[i].id != account.getUser().id) {
					if (count + 1 == MAX_RECIPIENTS) {
						sb.append(' ');
						sb.append(c.getString(R.string.summation_last));
						sb.append(' ');
					} else if (count != 0) {
						sb.append(c.getString(R.string.summation));
						sb.append(' ');
					}
					sb.append(recipients[i].name);
					count++;
					if (count + 1 == MAX_RECIPIENTS && recipients.length - i > 2) {
						sb.append(' ');
						sb.append(c.getString(R.string.summation_last));
						sb.append(' ');
						int numOthers = 0;
						for (int i2 = i + 1; i2 < recipients.length; i2++) {
							if (recipients[i2].id != account.getUser().id) {
								numOthers++;
							}
						}
						sb.append(numOthers == 1 ? c.getString(R.string.summation_other) : c.getString(R.string.summation_others, numOthers));
						break;
					} else if (count == MAX_RECIPIENTS) {
						break;
					}
				}
			}
			mTitle = sb;
			return sb;
		} else if (target != null) {
			return mTitle = c.getString(R.string.msg_to, sender.name, target.name);
		} else {
			return mTitle = sender.name;
		}
	}
	
	public CharSequence getInfo() {
		if (mInfo != null) {
			return mInfo;
		}
		switch (type) {
		case Message.TYPE_FACEBOOK_HOME:
			StringBuilder infoSB = new StringBuilder();
			int commentCount = comments != null ? comments.getRealCount() : 0;
			if (numLikes > 0) {
				infoSB.append(numLikes);
				infoSB.append(numLikes == 1 ? " like" : " likes");
			}
			if (numLikes > 0 && commentCount > 0) {
				infoSB.append(" \u2022 ");
			}
			if (commentCount > 0) {
				infoSB.append(commentCount);
				infoSB.append(commentCount == 1 ? " comment" : " comments");
			}
			return mInfo = infoSB;
		default:
			return null;
		}
	}
	
	public CharSequence getBody() {
		if (text == null) {
			return "";
		}
		if (mSpannable == null || !mSpannable.toString().equals(text)) {
			mSpannable = new SpannableString(text);
			Linkify.addLinks(mSpannable, Linkify.EMAIL_ADDRESSES|Linkify.WEB_URLS);
			final int l = hashtagStarts != null ? hashtagStarts.length : 0;
			for (int i = 0; i < l; i++) {
				try {
					int start = hashtagStarts[i];
					int end = hashtagEnds[i];
					HashtagSpan span = new HashtagSpan(text.substring(start, end));
			        mSpannable.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				} catch (StringIndexOutOfBoundsException e) {
					// do nothing
				}
			}
			final int l2 = mentionStarts != null ? mentionStarts.length : 0;
			for (int i = 0; i < l2; i++) {
				try {
					int start = mentionStarts[i];
					int end = mentionEnds[i];
					long id = mentionIds[i];
					MentionSpan span = new MentionSpan(account, id, text.substring(start, end));
			        mSpannable.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				} catch (StringIndexOutOfBoundsException e) {
					// do nothing
				}
			}
		}
		return mSpannable;
	}
	
	public CharSequence getTime() {
		mTime.setLength(0);
		TimeUtils.parseDuration(mTime, System.currentTimeMillis() - createdTime, false);
		return mTime;
	}
	
	public boolean isChat() {
		return type == TYPE_FACEBOOK_MESSAGE || type == TYPE_TWITTER_DIRECT_MESSAGE;
	}
	
	public Chat getChat() {
		return account.findMessageChat(this);
	}
	
	public boolean isDetailsMessageLocalAvailable() {
		return type != TYPE_FACEBOOK_NOTIFICATION || mNotificationTargetMessage != null;
	}
	
	public Message getDetailsMessage() {
		if (type == Message.TYPE_FACEBOOK_NOTIFICATION) {
			FacebookAccount facebookAccount = (FacebookAccount) account;
			if (mNotificationTargetMessage != null) {
				return mNotificationTargetMessage;
			}
			switch (notificationType) {
			case Message.NOTIFICATION_STREAM:
				SortedArrayList<Message> msgs = facebookAccount.getHome();
				String ID = notificationTargetID;
				long senderID = Long.parseLong(ID.substring(0, ID.indexOf('_')));
				long postID = Long.parseLong(ID.substring(ID.indexOf('_') + 1));
				for (int i = msgs.size() - 1; i >= 0; i--) {
					Message msg = msgs.get(i);
					if (msg.ID == postID && msg.sender.id == senderID) {
						if (msg.updatedTime < createdTime) {
							msg.isDirty = true;
						}
						mNotificationTargetMessage = msg;
						return msg;
					}
				}
				return mNotificationTargetMessage = facebookAccount.loadStreamUpdate(ID);
			case Message.NOTIFICATION_PHOTO:
				photo = facebookAccount.loadPhoto(notificationTargetID);
				if (photo != null) {
					Message msg = photo.loadMessage();
					if (msg != null) {
						mNotificationTargetMessage = msg;
						return msg;
					}
				}
				return null;
			case Message.NOTIFICATION_GROUP:
				// TODO
				return null;
			default:
				// do nothing...
				return null;
			}
		} else {
			return this;
		}
	}
	
	@Override
	public int compareTo(final Message another) {
		final long l1 = createdTime;
		final long l2 = another.createdTime;
		return l1 > l2 ? -1 : (l1 < l2 ? 1 : 0);
	}
	
	@Override
	public int hashCode() {
		return (int)ID;
	}
	
	@Override
	public boolean equals(Object o) {
		Message m = (Message) o;
		return m.PRE_ID == PRE_ID &&  m.ID == ID && m.type == type;
	}
	
	public static Message parseBundle(IAccount account, StorageBundle bundle) {
		Message msg = new Message();
		msg.account = account;
		msg.mBundle = bundle;
		msg.type = bundle.readInt("TYPE", TYPE_UNKNOWN);
		msg.ID = bundle.readLong("ID", -1);
		msg.text = bundle.readString("TEXT", "");
		
    	msg.sender = new User(bundle.readBundle("SENDER", null));
    	StorageBundle targetBundle = bundle.readBundle("TARGET", null);
    	msg.target = targetBundle != null ? new User(targetBundle) : null;
    	
    	StorageBundle[] recipientsBundles = bundle.readBundleArray("RECIPIENTS", null);
    	if (recipientsBundles != null) {
    		msg.recipients = new User[recipientsBundles.length];
    		for (int i = 0; i < recipientsBundles.length; i++) {
    			msg.recipients[i] = new User(recipientsBundles[i]);
    		}
    	}
    	
    	msg.mentionIds = bundle.readLongArray("MENTIONIDS", null);
    	msg.mentionStarts = bundle.readIntArray("MENTIONSTARTS", null);
    	msg.mentionEnds = bundle.readIntArray("MENTIONENDS", null);
    	
    	msg.hashtagStarts = bundle.readIntArray("HASHTAGSTARTS", null);
    	msg.hashtagEnds = bundle.readIntArray("HASHTAGENDS", null);
    	
		msg.createdTime = bundle.readLong("CREATED_TIME", -1);
    	if (msg.isTwitter()) {
			// read twitter specific data
    		msg.source = bundle.readString("SOURCE", null);
    		msg.updatedTime = msg.createdTime;
		} else if (msg.isFacebook()) {
			// read facebook specific data
			msg.PRE_ID = bundle.readLong("PRE_ID", -1);
			msg.updatedTime = bundle.readLong("UPDATED_TIME", -1);
			msg.numLikes = bundle.readInt("NUM_LIKES", 0);
			msg.canLike = bundle.readBool("CAN_LIKE", true);
			msg.userLikes = bundle.readBool("USER_LIKES", false);
			msg.comments = new Comments(bundle.readBundle("COMMENTS", null));
			StorageBundle[] facebookImageBundles = bundle.readBundleArray("FACEBOOK_IMAGES", null);
	    	if (facebookImageBundles != null) {
	    		final int l = facebookImageBundles.length;
	    		msg.facebookImages = new ArrayList<FacebookImage>(l);
	    		for (int i = 0; i < l; i++) {
	    			msg.facebookImages.add(new FacebookImage(facebookImageBundles[i]));
	    		}
	    	}
	    	if (msg.type == TYPE_FACEBOOK_NOTIFICATION) {
	    		msg.notificationType = bundle.readInt("NOTIFICATION_TYPE", NOTIFICATION_UNKNOWN);
	    		msg.notificationTargetID = bundle.readString("NOTIFICATION_TARGET", null);
	    	}
		}
    	return msg;
	}
	
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+SSSS");
	
	public static Message parseFacebookGraphUpdate(IAccount account, JSONObject json) {
		Message msg = new Message();
		try {
			String postID = json.getString("id");
			msg.account = account;
			msg.PRE_ID = Long.parseLong(postID.substring(0, postID.indexOf('_')));
			msg.ID = Long.parseLong(postID.substring(postID.indexOf('_') + 1));
			msg.type = TYPE_FACEBOOK_GRAPH;
			msg.sender = User.parseFacebookUser(null, json.getJSONObject("from"), false);
			msg.target = json.isNull("to") ? null : User.parseFacebookUser(null, json.getJSONObject("to"), false);
			msg.text = json.optString("message", "");
			if (msg.text.equals("null")) {
				msg.text = "";
			}
			final int l = msg.text.length();
			if (l < 10 && l > 0) {
				boolean isOnlySpaceChar = true;
				for (int i = 0; i < l; i++) {
					if (!Character.isSpaceChar(msg.text.charAt(i))) {
						isOnlySpaceChar = false;
						break;
					}
				}
				if (isOnlySpaceChar) {
					msg.text = null;
				}
			}
			
			msg.createdTime = DATE_FORMAT.parse(json.getString("created_time")).getTime();
			msg.updatedTime = DATE_FORMAT.parse(json.getString("updated_time")).getTime();
			JSONObject likes = json.optJSONObject("likes");
			if (likes != null) {
				msg.canLike = likes.optBoolean("can_like", true);
				msg.userLikes = likes.optBoolean("user_likes", false);
				msg.numLikes = likes.optInt("count", 0);
			} else {
				msg.canLike = true;
				msg.userLikes = false;
				msg.numLikes = 0;
			}
			String picture = json.optString("picture", null);
			String link = json.optString("link", null);
			
			if (picture != null) {
				msg.facebookImages = new ArrayList<FacebookImage>(1);
				msg.facebookImages.add(new FacebookImage(picture, link, null, -1, -1));
			} else if (link != null) {
				msg.text += " " + link;
			}
			return msg;
		} catch (JSONException e) {
			Log.wtf("JDROID", e);
			return null;
		} catch (ParseException e) {
			Log.wtf("JDROID", e);
			return null;
		}
	}
	
	public static Message parseFacebookStreamUpdate(IAccount account, JSONObject json, JSONArray comments, ArrayList<User> users) {
		Message msg = new Message();
		try {
			String postID = json.getString("post_id");
			msg.account = account;
			msg.PRE_ID = Long.parseLong(postID.substring(0, postID.indexOf('_')));
			msg.ID = Long.parseLong(postID.substring(postID.indexOf('_') + 1));
			msg.type = TYPE_FACEBOOK_HOME;
			msg.sender = User.findByID(users, json.getLong("actor_id"));
			msg.target = json.isNull("target_id") ? null : User.findByID(users, json.getLong("target_id"));
			msg.text = json.isNull("message") ? "" : json.getString("message");
			
			final int l = msg.text.length();
			if (l > 0 && l < 10) {
				boolean isOnlySpaceChar = true;
				for (int i = 0; i < l; i++) {
					if (!Character.isSpaceChar(msg.text.charAt(i))) {
						isOnlySpaceChar = false;
						break;
					}
				}
				if (isOnlySpaceChar) {
					msg.text = "";
				}
			}
			
			msg.createdTime = json.getLong("created_time") * 1000l;
			msg.updatedTime = json.getLong("updated_time") * 1000l;
			JSONObject msgComments = json.getJSONObject("comments");
			if (msgComments.getInt("count") > 0) {
				msg.comments = new Comments(msgComments, postID, comments, users);
			}
			JSONObject likes = json.getJSONObject("likes");
			msg.canLike = likes.getBoolean("can_like");
			msg.userLikes = likes.optBoolean("user_likes", false);
			msg.numLikes = likes.optInt("count", 0);
			JSONObject attachmentJson = json.getJSONObject("attachment");
			JSONArray media = attachmentJson.optJSONArray("media");
			if (media != null) {
				msg.facebookImages = new ArrayList<FacebookImage>(media.length());
				for (int i = 0; i < media.length(); i++) {
					JSONObject mediaObj = media.getJSONObject(i);
					String type = mediaObj.getString("type");
					if (type.equals("photo")) {
						String src = mediaObj.getString("src");
						String href = mediaObj.getString("href");
						JSONObject photo = mediaObj.getJSONObject("photo");
						String pid = photo.getString("pid");
						int w = photo.getInt("width");
						int h = photo.getInt("height");
						msg.facebookImages.add(new FacebookImage(src, href, pid, w, h));
					} else if (type.equals("video") || type.equals("link")) {
						String href = mediaObj.optString("href");
						if (TextUtils.isEmpty(href)) {
							href = attachmentJson.optString("href");
						}
						if (!TextUtils.isEmpty(href)) {
							msg.text = msg.text + " " + mediaObj.optString("href");
						}
					}
				}
			}
			return msg;
		} catch (JSONException e) {
			Log.wtf("JDROID", e);
			return null;
		}
	}
	
	public static Message parseFacebookNotification(IAccount account, JSONObject json, ArrayList<User> users) {
		Message msg = new Message();
		try {
			msg.account = account;
			msg.type = TYPE_FACEBOOK_NOTIFICATION;
			msg.ID = Long.parseLong(json.getString("notification_id"));
			msg.sender = User.findByID(users, json.getLong("sender_id"));
			msg.text = json.getString("title_text");
			msg.notificationTargetID = json.getString("object_id");
			msg.createdTime = msg.updatedTime = json.getLong("created_time") * 1000;
			String type = json.getString("object_type");
			if (type.equals("stream")) {
				msg.notificationType = NOTIFICATION_STREAM;
			} else if (type.equals("photo")) {
				msg.notificationType = NOTIFICATION_PHOTO;
			} else if (type.equals("group")) {
				msg.notificationType = NOTIFICATION_GROUP;
			} else {
				msg.notificationType = NOTIFICATION_UNKNOWN;
			}
			return msg;
		} catch (JSONException e) {
			return null;
		}
	}
	
	public static Message parseFacebookMessage(IAccount account, JSONObject json, ArrayList<User> users) {
		Message msg = new Message();
		try {
			msg.account = account;
			msg.type = TYPE_FACEBOOK_MESSAGE;
			msg.ID = Long.parseLong(json.getString("thread_id"));
			msg.sender = User.findByID(users, json.getLong("snippet_author"));
			msg.text = json.getString("snippet");
			msg.createdTime = msg.updatedTime = json.getLong("updated_time") * 1000;
			JSONArray recipientsJson = json.getJSONArray("recipients");
			msg.recipients = new User[recipientsJson.length()];
			for (int i = 0; i < recipientsJson.length(); i++) {
				msg.recipients[i] = User.findByID(users, recipientsJson.getLong(i));
			}
			return msg;
		} catch (JSONException e) {
			return null;
		}
	}
	
	public static Message parseTwitterTweet(IAccount account, Tweet status) {
		Message msg = new Message();
		msg.account = account;
		msg.type = TYPE_TWITTER_SEARCH;
		msg.ID = status.getId();
		msg.text = status.getText();
		User from = new User(User.TYPE_TWITTER, status.getFromUserId(), status.getFromUser());
		from.profilePictureUrl = from.largeProfilePictureUrl = status.getProfileImageUrl();
		msg.sender = from;
		msg.createdTime = status.getCreatedAt().getTime();
		msg.updatedTime = msg.createdTime;
		msg.source = status.getSource();
    	int index = msg.source.indexOf('>');
    	if (index != -1) {
    		msg.source = msg.source.substring(index + 1, msg.source.indexOf('<', index));
    	}
    	final int l = status.getHashtagEntities() != null ? status.getHashtagEntities().length : 0;
    	if (l > 0) {
    		msg.hashtagStarts = new int[l];
        	msg.hashtagEnds = new int[l];
        	for (int i = 0; i < l; i++) {
        		HashtagEntity hashtag = status.getHashtagEntities()[i];
        		msg.hashtagStarts[i] = hashtag.getStart();
        		msg.hashtagEnds[i] = hashtag.getEnd();
        	}
    	}
    	
    	final int l2 = status.getUserMentionEntities() != null ? status.getUserMentionEntities().length : 0;
    	if (l2 > 0) {
    		msg.mentionStarts = new int[l2];
        	msg.mentionEnds = new int[l2];
        	msg.mentionIds = new long[l2];
        	for (int i = 0; i < l2; i++) {
        		UserMentionEntity mention = status.getUserMentionEntities()[i];
        		msg.mentionStarts[i] = mention.getStart();
        		msg.mentionEnds[i] = mention.getEnd();
        		msg.mentionIds[i] = mention.getId();
        	}
    	}
    	return msg;
	}
	
	public static Message parseTwitterMessage(IAccount account, Status status) {
		Message msg = new Message();
		msg.account = account;
		msg.type = TYPE_TWITTER_TIMELINE;
		msg.ID = status.getId();
		msg.text = status.getText();
		msg.sender = new User(null, status.getUser(), false);
		msg.createdTime = status.getCreatedAt().getTime();
		msg.updatedTime = msg.createdTime;
		msg.source = status.getSource();
    	int index = msg.source.indexOf('>');
    	if (index != -1) {
    		msg.source = msg.source.substring(index + 1, msg.source.indexOf('<', index));
    	}
    	final int l = status.getHashtagEntities().length;
    	if (l > 0) {
    		msg.hashtagStarts = new int[l];
        	msg.hashtagEnds = new int[l];
        	for (int i = 0; i < l; i++) {
        		HashtagEntity hashtag = status.getHashtagEntities()[i];
        		msg.hashtagStarts[i] = hashtag.getStart();
        		msg.hashtagEnds[i] = hashtag.getEnd();
        	}
    	}
    	
    	final int l2 = status.getUserMentionEntities().length;
    	if (l2 > 0) {
    		msg.mentionStarts = new int[l2];
        	msg.mentionEnds = new int[l2];
        	msg.mentionIds = new long[l2];
        	for (int i = 0; i < l2; i++) {
        		UserMentionEntity mention = status.getUserMentionEntities()[i];
        		msg.mentionStarts[i] = mention.getStart();
        		msg.mentionEnds[i] = mention.getEnd();
        		msg.mentionIds[i] = mention.getId();
        	}
    	}
    	return msg;
	}
	
	public static Message parseTwitterDirectMessage(IAccount account, DirectMessage directMsg) {
		Message msg = new Message();
		msg.account = account;
		msg.type = TYPE_TWITTER_DIRECT_MESSAGE;
		msg.ID = directMsg.getId();
		msg.text = directMsg.getText();
		msg.sender = new User(null, directMsg.getSender(), false);
		msg.target = new User(null, directMsg.getRecipient(), false);
		msg.createdTime = directMsg.getCreatedAt().getTime();
		msg.updatedTime = msg.createdTime;
    	return msg;
	}
	
	public static Bundle createMessageBundle(Bundle bundle, Message msg) {
		if (bundle == null) {
			bundle = new Bundle(4);
		}
		bundle.putLong(EXTRA_ACCOUNT, msg.account.getUser().id);
		bundle.putLong(EXTRA_MESSAGE_ID, msg.ID);
		bundle.putLong(EXTRA_MESSAGE_PRE_ID, msg.PRE_ID);
		bundle.putInt(EXTRA_MESSAGE_TYPE, msg.type);
		return bundle;
	}
	
	public static Message findMessage(Context c, Bundle bundle) {
		long accountID = bundle.getLong(EXTRA_ACCOUNT);
		long msgID = bundle.getLong(EXTRA_MESSAGE_ID);
		long msgPreID = bundle.getLong(EXTRA_MESSAGE_PRE_ID);
		int type = bundle.getInt(EXTRA_MESSAGE_TYPE);
		IAccount account = AccountsManager.getInstance(c).findAccount(accountID);
		if (account != null) {
			return account.findMessage(msgPreID, msgID, type);
		}
		return null;
	}
	
}
