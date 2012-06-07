package com.jldroid.twook.widget;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import com.jdroid.utils.StorageManager;
import com.jldroid.twook.R;
import com.jldroid.twook.activities.DetailsActivity;
import com.jldroid.twook.fragments.DetailsFragment;
import com.jldroid.twook.model.ColumnInfo;
import com.jldroid.twook.model.ColumnManager;
import com.jldroid.twook.model.ColumnMessagesProvider;

public class ColumnWidgetProvider extends AppWidgetProvider {
	
    public static final String CLICK_ACTION = "com.jldroid.twook.TOAST_ACTION";
    public static final String REFRESH_ACTION = "com.jldroid.twook.REFRESH_ACTION";
    
    public static final String EXTRA_MSG = "com.jldroid.twook.EXTRA_MSG";

    public static ColumnInfo getWidgetColumn(Context c, int appWidgetId) {
    	ColumnManager cm = ColumnManager.getInstance(c);
    	String name = StorageManager.getDeflaut(c).readString("WIDGET" + appWidgetId, "");
    	for (int i = 0; i < cm.getColumnCount(); i++) {
    		ColumnInfo info = cm.getColumnInfo(i);
    		if (info.getProvider().getStorageName().equals(name)) {
    			return info;
    		}
    	}
    	return null;
    }
    
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(CLICK_ACTION)) {
            Bundle msg = intent.getBundleExtra(EXTRA_MSG);
            context.startActivity(new Intent(context.getApplicationContext(), DetailsActivity.class)
            		.putExtra(DetailsFragment.EXTRA_MSG, msg)
            		.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else if (intent.getAction().equals(REFRESH_ACTION)) {
        	int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            ColumnInfo column = getWidgetColumn(context, appWidgetId);
            column.getProvider().requestUpdate(null);
        }
        super.onReceive(context, intent);
    }

    @TargetApi(11)
	@Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // update each of the widgets with the remote adapter
        for (int i = 0; i < appWidgetIds.length; ++i) {

        	ColumnInfo columnInfo = getWidgetColumn(context, appWidgetIds[i]);
        	if (columnInfo == null) {
        		//Log.e("JDROID", "Failed to find column for: " + appWidgetIds[i]);
        		continue;
        	}
        	ColumnMessagesProvider provider = columnInfo.getProvider();
        	
            Intent intent = new Intent(context, ColumnWidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            
            boolean isUpdating = provider.isUpdating();
            rv.setViewVisibility(R.id.pb, isUpdating ? View.VISIBLE : View.GONE);
    		rv.setViewVisibility(R.id.refreshBtn, isUpdating ? View.GONE : View.VISIBLE);
            
            rv.setTextViewText(R.id.titleTV, provider.getName(context));
            
            rv.setRemoteAdapter(appWidgetIds[i], R.id.listView, intent);
            rv.setEmptyView(R.id.listView, R.id.empty_view);

            Intent toastIntent = new Intent(context, ColumnWidgetProvider.class);
            toastIntent.setAction(ColumnWidgetProvider.CLICK_ACTION);
            toastIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            toastIntent.setData(Uri.parse(toastIntent.toUri(Intent.URI_INTENT_SCHEME)));
            PendingIntent toastPendingIntent = PendingIntent.getBroadcast(context, 0, toastIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setPendingIntentTemplate(R.id.listView, toastPendingIntent);

            Intent refreshIntent = new Intent(context, ColumnWidgetProvider.class);
            refreshIntent.setAction(ColumnWidgetProvider.REFRESH_ACTION);
            refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            refreshIntent.setData(Uri.parse(refreshIntent.toUri(Intent.URI_INTENT_SCHEME)));
            PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.refreshBtn, refreshPendingIntent);
            
            appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
}
