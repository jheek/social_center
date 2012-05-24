package com.jldroid.twook.model;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.app.NotificationCompat.Builder;

import com.jldroid.twook.R;
import com.jldroid.twook.activities.MainActivity;
import com.jldroid.twook.model.ImageManager.DeletionTrigger;
import com.jldroid.twook.model.ImageManager.LoadBitmapCallback;

public class UnreadNotificationManager {
	
	private static UnreadNotificationManager sInstance = null;
	
	private Context mContext;
	
	private UnreadNotificationManager(Context c) {
		mContext = c;
	}
	
	public static UnreadNotificationManager getInstance(Context c) {
		if (sInstance == null) {
			sInstance = new UnreadNotificationManager(c.getApplicationContext());
		}
		return sInstance;
	}
	
	public static UnreadNotificationManager peekInstance() {
		return sInstance;
	}
	
	private void updateNotification(ColumnMessagesProvider column, boolean sound, String ringtone, boolean vibrate, boolean led, Bitmap largeBmd) {
		int count = column.getUnreadMessageCount();
		
		Intent intent = new Intent(mContext, MainActivity.class)
			.putExtra(MainActivity.EXTRA_COLUMN, column.getStorageName())
			.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setData(Uri.parse("column:/" + column.getStorageName()));
		
		String msg = mContext.getString(R.string.notification_unread_msg, count, column.getName(mContext));
		
		Notification not = new Builder(mContext)
			.setAutoCancel(true)
			.setDefaults((vibrate ? Notification.DEFAULT_VIBRATE : 0) | (led ? Notification.DEFAULT_LIGHTS : 0))
			.setSound(sound ? Uri.parse(ringtone) : null)
			.setWhen(System.currentTimeMillis())
			.setNumber(count)
			.setSmallIcon(R.drawable.notification_icon)
			.setLargeIcon(largeBmd)
			.setContentIntent(PendingIntent.getActivity(mContext, 0, intent, 0))
			.setContentTitle(mContext.getString(R.string.notification_unread_title, column.getName(mContext)))
			.setContentText(msg)
			.setTicker(msg)
			.getNotification();
		getNotificationManager().notify(column.getStorageName().hashCode(), not);
	}
	
	public void updateNotification(final ColumnMessagesProvider column, final boolean sound, final String ringtone, final boolean vibrate, final boolean led) {
		int count = column.getUnreadMessageCount();
		if (count == 1 && column.getMessageCount() > 0) {
			Message lastMsg = column.getMessage(0);
			ImageManager im = ImageManager.getInstance(mContext);
			im.loadProfilePicture(new LoadBitmapCallback() {
				@Override
				public void onFailed(String pUri) {
					onBitmapLoaded(pUri, null);
				}
				@Override
				public void onBitmapLoaded(String pUri, Bitmap pBmd) {
					updateNotification(column, sound, ringtone, vibrate, led, pBmd);
				}
			}, lastMsg.sender.profilePictureUrl, DeletionTrigger.AFTER_ONE_WEEK_UNUSED);
		} else if (count == 0) {
			removeNotification(column);
		} else {
			updateNotification(column, sound, ringtone, vibrate, led, null);
		}
	}
	
	public void removeNotification(ColumnMessagesProvider column) {
		getNotificationManager().cancel(column.getStorageName().hashCode());
	}
	
	private NotificationManager getNotificationManager() {
		return (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
	}
}
