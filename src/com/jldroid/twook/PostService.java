package com.jldroid.twook;

import java.io.File;

import twitter4j.StatusUpdate;
import twitter4j.TwitterException;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.media.ImageUpload;
import twitter4j.media.ImageUploadFactory;
import twitter4j.media.MediaProvider;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;

import com.jdroid.utils.Threads;
import com.jldroid.twook.activities.MainActivity;
import com.jldroid.twook.model.PendingPost;
import com.jldroid.twook.model.PendingPostManager;
import com.jldroid.twook.model.twitter.TwitterConfiguration;

public class PostService extends Service {

	public static String ACTION_POST_PENDING = "com.jldroid.twook.POST_PENDING";
	
	private static Object sWaitObj = new Object();
	
	protected Notification mNotification;
	
	protected void setLatestEventInfo(final String title, final String msg, final PendingIntent intent) {
		Threads.runOnUIThread(new Runnable() {
			@Override
			public void run() {
				mNotification.setLatestEventInfo(getApplicationContext(), title, msg, intent);
				NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
				nm.notify(10, mNotification);
			}
		});
	}
	
	protected void setupForeground(final PendingIntent intent) {
		Threads.runOnUIThread(new Runnable() {
			@Override
			public void run() {
				if (mNotification == null) {
					mNotification = new Notification(R.drawable.notification_icon, "Updating status", System.currentTimeMillis());
					mNotification.setLatestEventInfo(getApplicationContext(), "Updating status", "Initializing...", intent);
					startForeground(10, mNotification);
				}
			}
		});
	}
	
	@Override
	public int onStartCommand(Intent pIntent, int pFlags, final int pStartId) {
		final PendingIntent homeIntent = PendingIntent.getActivity(this, 0, 
				new Intent(getApplicationContext(), MainActivity.class)
				.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0);
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				PendingPostManager ppm = PendingPostManager.getInstance(getApplicationContext());
				synchronized (sWaitObj) {
					while (true) {
						final PendingPost post = ppm.next();
						ppm.removeFromQeue(post);
						if (post == null) {
							break;
						}
						if (post.twitterAccount == null && post.facebookAccount == null) {
							continue;
						}
						setupForeground(homeIntent);
						setLatestEventInfo("Updating status", post.twitterAccount != null ? "Updating Twitter status..." : "Updating Facebook status...", homeIntent);
						switch (post.type) {
						case PendingPost.TYPE_TWITTER_REPLY:
						case PendingPost.TYPE_TWITTER_STATUS_UPDATE:
							boolean error = false;
							if (!TextUtils.isEmpty(post.imgPath)) {
								setLatestEventInfo("Uploading image", "Uploading image to twitpic", homeIntent);
								Configuration conf = new ConfigurationBuilder()
								    .setMediaProviderAPIKey( TwitterConfiguration.TWITPIC_API_KEY )
								    .setOAuthConsumerKey( TwitterConfiguration.OAUTH_CONSUMER_KEY )
								    .setOAuthConsumerSecret( TwitterConfiguration.OAUTH_CONSUMER_SECRET )
								    .setOAuthAccessToken(post.twitterAccount.getOAuthToken())
								    .setOAuthAccessTokenSecret(post.twitterAccount.getOAuthSecret())
								    .build();
								ImageUpload imageUpload = new ImageUploadFactory(conf).getInstance(MediaProvider.TWITPIC);
								try {
									String imgUrl = imageUpload.upload(new File(post.imgPath), post.text);
									String text = post.text;
									if (!TextUtils.isEmpty(text) && !Character.isSpaceChar(text.charAt(text.length() - 1))) {
										text = text + ' ';
									}
									text = text + imgUrl;
									post.text = text;
								} catch (TwitterException e) {
									e.printStackTrace();
									error = true;
								}
							}
							StatusUpdate statusUpdate = new StatusUpdate(post.text);
							if (post.type == PendingPost.TYPE_TWITTER_REPLY) {
								statusUpdate.setInReplyToStatusId(post.twitterTargetID);
							}
							if (!error) {
								error = !post.twitterAccount.updateStatus(statusUpdate);
							}
							if (error) {
								Threads.runOnUIThread(new Runnable() {
									@Override
									public void run() {
										Notification errorNot = new Notification(R.drawable.notification_icon, "Error while updating Twitter status", System.currentTimeMillis());
										errorNot.setLatestEventInfo(PostService.this, 
												"Twiter status update error", "Error while updating status of " + post.twitterAccount.getUser().name + ". Touch to retry", 
												homeIntent); // TODO
										NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
										nm.notify("POSTSERVICE", post.twitterAccount.hashCode(), errorNot);
									}
								});
							}
							break;
						case PendingPost.TYPE_TWITTER_RETWEET:
							if (!post.twitterAccount.retweetStatus(post.twitterTargetID)) {
								Threads.runOnUIThread(new Runnable() {
									@Override
									public void run() {
										Notification errorNot = new Notification(R.drawable.notification_icon, "Error while retweeting status", System.currentTimeMillis());
										errorNot.setLatestEventInfo(PostService.this, 
												"Twiter retweet error", "Error while retweeting status.", 
												homeIntent); // TODO
										NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
										nm.notify("POSTSERVICE", post.twitterAccount.hashCode(), errorNot);
									}
								});
							}
							break;
						case PendingPost.TYPE_FACEBOOK_WALL_POST:
						case PendingPost.TYPE_FACEBOOK_STATUS_UPDATE:
							if (!post.facebookAccount.postFeedUpdate(
									post.type != PendingPost.TYPE_FACEBOOK_STATUS_UPDATE ? Long.toString(post.facebookTargetID) : "me", 
									post.text, post.imgPath)) {
								Threads.runOnUIThread(new Runnable() {
									@Override
									public void run() {
										Notification errorNot = new Notification(R.drawable.notification_icon, "Error while updating Facebook status", System.currentTimeMillis());
										errorNot.setLatestEventInfo(PostService.this, 
												"Facebook status update error", "Error while updating status of " + post.facebookAccount.getUser().name + ". Touch to retry", 
												homeIntent); // TODO
										NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
										nm.notify("POSTSERVICE", post.facebookAccount.hashCode(), errorNot);
									}
								});
							}
							break;
						default:
							break;
						}
					}
				}
				Threads.runOnUIThread(new Runnable() {
					@Override
					public void run() {
						if (!PendingPostManager.getInstance(getApplicationContext()).hasPendingPosts() && stopSelfResult(pStartId)) {
							stopForeground(true);
							mNotification = null;
						}
					}
				});
			}
		});
		return START_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent pIntent) {
		return null;
	}

}
