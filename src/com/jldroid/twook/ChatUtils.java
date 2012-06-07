package com.jldroid.twook;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.app.NotificationCompat.Builder;

import com.jldroid.twook.activities.ChatActivity;
import com.jldroid.twook.fragments.ChatFragment;
import com.jldroid.twook.model.Chat;
import com.jldroid.twook.model.ImageManager;
import com.jldroid.twook.model.ImageManager.DeletionTrigger;
import com.jldroid.twook.model.ImageManager.LoadBitmapCallback;
import com.jldroid.twook.model.User;

public class ChatUtils {

	public static void updateChatNotification(final Context c, final Chat chat) {
		if (chat.getUnread() > 0 && chat.getLastActiveUser() != null) {
			final User from = chat.getLastActiveUser();
			ImageManager im = ImageManager.getInstance(c);
			im.loadProfilePicture(new LoadBitmapCallback() {
				@Override
				public void onFailed(String pUri) {
					onBitmapLoaded(pUri, null);
				}
				
				@Override
				public void onBitmapLoaded(String pUri, Bitmap pBmd) {
					updateChatNotification(c, chat, from, pBmd);
				}
			}, from.profilePictureUrl, DeletionTrigger.AFTER_ONE_WEEK_UNUSED);
		} else {
			NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
			nm.cancel("CHATS", (int) (chat.getID() & 0xFFFFFFFF));
		}
	}
	
	
	private static void updateChatNotification(Context c, Chat chat, User from, Bitmap largeBmd) {
		NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
		int unread = chat.getUnread();
		String msg = unread == 1 ? c.getString(R.string.chat_notification_msg_single, from.name) : c.getString(R.string.chat_notification_msg_multi, unread, from.name);
		Intent intent = new Intent(c, ChatActivity.class)
			.putExtra(ChatFragment.EXTRA_CHAT, chat.getID())
			.putExtra(ChatFragment.EXTRA_ACCOUNT, chat.getAccount().getUser().id)
			.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setData(Uri.parse("chat:/" + chat.getID()));
		Notification not = new Builder(c)
			.setWhen(System.currentTimeMillis())
			.setAutoCancel(true)
			.setDefaults(Notification.DEFAULT_ALL)
			.setContentIntent(PendingIntent.getActivity(c, 0, intent, 0))
			.setNumber(unread)
			.setSmallIcon(R.drawable.notification_icon)
			.setLargeIcon(largeBmd)
			.setContentTitle(c.getString(R.string.chat_notification_title))
			.setContentText(msg)
			.setTicker(msg)
			.getNotification();
		nm.notify("CHATS", (int) (chat.getID() & 0xFFFFFFFF), not);
	}
	
}
