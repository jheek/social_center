package com.jldroid.twook.model.facebook;

import com.jldroid.twook.PrivateKeys;

public class FacebookConfiguration {
	
	public static final String APP_ID = PrivateKeys.FACEBOOK_APP_ID;
	public static final String APP_SECRET = PrivateKeys.FACEBOOK_APP_SECRET;
	
	public static final String[] PERMISSIONS = {
		"read_stream", "publish_stream", "read_mailbox",
		"user_groups",
		"offline_access", 
		"email",
		"user_photos", "friends_photos", 
		"user_questions", "friends_questions",
		"user_relationships", "friends_relationships",
		"user_religion_politics", "friends_religion_politics",
		"user_about_me", "friends_about_me",
		"user_birthday", "friends_birthday",
		"user_hometown", "friends_hometown",
		"user_location", "friends_location",
		"user_website", "friends_website",
		"user_likes", "friends_likes",
		"read_friendlists", 
		"user_groups",
		"manage_notifications",
		"xmpp_login"};
	
}
