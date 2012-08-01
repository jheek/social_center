package com.jldroid.twook.widget;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.jldroid.twook.R;
import com.jldroid.twook.model.ColumnMessagesProvider;
import com.jldroid.twook.model.ColumnProviderListener;
import com.jldroid.twook.model.ImageManager;
import com.jldroid.twook.model.ImageManager.DeletionTrigger;
import com.jldroid.twook.model.ImageManager.LoadBitmapCallback;
import com.jldroid.twook.model.Message;

@SuppressLint("NewApi")
public class ColumnWidgetService extends RemoteViewsService {
	
	@Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StackRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

@SuppressLint("NewApi")
class StackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory, LoadBitmapCallback, ColumnProviderListener {
	
    private ColumnMessagesProvider mProvider;
    private Context mContext;
    private int mAppWidgetId;

    private Object sWaitObj = new Object();
    
    public StackRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        mProvider = ColumnWidgetProvider.getWidgetColumn(context, mAppWidgetId).getProvider();
    }

    public void onCreate() {
    	mProvider.addListener(this);
    }

    public void onDestroy() {
    	mProvider.removeListener(this);
    }

    public int getCount() {
        return mProvider.getMessageCount();
    }

    @Override
    public void onBitmapLoaded(String pUri, Bitmap pBmd) {
    	synchronized (sWaitObj) {
			sWaitObj.notify();
		}
    }
    
    @Override
    public void onFailed(String pUri) {
    	synchronized (sWaitObj) {
			sWaitObj.notify();
		}
    }
    
    @Override
    public void onUpdateStateChanged(boolean pIsUpdating) {
    	AppWidgetManager awm = AppWidgetManager.getInstance(mContext);
		RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_layout);
		rv.setViewVisibility(R.id.pb, pIsUpdating ? View.VISIBLE : View.GONE);
		rv.setViewVisibility(R.id.refreshBtn, pIsUpdating ? View.GONE : View.VISIBLE);
		awm.updateAppWidget(mAppWidgetId, rv);
    }
    
    @Override
    public void onEnlargingStateChanged(boolean pIsEnlarging) {
    }
    
    @Override
    public void onHasOlderMessagesChanged(boolean pV) {
    }
    
    @Override
    public void onMessagesChanged() {
    	AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
    	appWidgetManager.notifyAppWidgetViewDataChanged(mAppWidgetId, R.id.listView);
    }
    
    public RemoteViews getViewAt(int position) {
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_item);
        
        Message msg = mProvider.getMessage(position);
        ImageManager im = ImageManager.getInstance(mContext);
        
        String uri = msg.sender.profilePictureUrl;
        Bitmap profileBmd = im.peekImage(uri);
        if (profileBmd == null) {
        	im.loadProfilePicture(this, uri, DeletionTrigger.AFTER_ONE_WEEK_UNUSED);
        	synchronized (sWaitObj) {
				try {
					sWaitObj.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
        	profileBmd = im.peekImage(uri);
        }
        if (profileBmd != null) {
        	 rv.setImageViewBitmap(R.id.profileIV, profileBmd);
        } else {
        	rv.setImageViewResource(R.id.profileIV, R.drawable.no_profileimg_img);
        }
        rv.setImageViewResource(R.id.profileTypeIV, msg.isFacebook() ? R.drawable.facebook_icon : R.drawable.twitter_icon);
        
        Bitmap previewBmd = null;
        if (msg.facebookImages != null && msg.facebookImages.size() > 0) {
        	uri = msg.facebookImages.get(0).src;
        	previewBmd = im.peekImage(uri);
        	if (previewBmd == null) {
        		im.loadImage(this, uri, DeletionTrigger.AFTER_ONE_DAY_UNUSED, -1, -1, 80);
            	synchronized (sWaitObj) {
    				try {
    					sWaitObj.wait();
    				} catch (InterruptedException e) {
    					e.printStackTrace();
    				}
    			}
            	profileBmd = im.peekImage(uri);
        	}
        	if (previewBmd != null) {
        		rv.setImageViewBitmap(R.id.previewIV, previewBmd);
        	}
        }
        
        rv.setTextViewText(R.id.senderTV, msg.getTitle(mContext));
        rv.setTextViewText(R.id.infoTV, msg.getInfo());
        rv.setTextViewText(R.id.contentTV, msg.getBody());
        
        Intent fillInIntent = new Intent();
        fillInIntent.putExtra(ColumnWidgetProvider.EXTRA_MSG, Message.createMessageBundle(null, msg));
        rv.setOnClickFillInIntent(R.id.widget_item, fillInIntent);
        return rv;
    }

    public RemoteViews getLoadingView() {
        return null;
    }

    public int getViewTypeCount() {
        return 1;
    }

    public long getItemId(int position) {
        return mProvider.getMessage(position).ID;
    }

    public boolean hasStableIds() {
        return true;
    }

    public void onDataSetChanged() {
    	
    }
}
