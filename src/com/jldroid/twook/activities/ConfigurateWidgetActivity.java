package com.jldroid.twook.activities;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListActivity;
import com.jdroid.utils.StorageManager;
import com.jldroid.twook.R;
import com.jldroid.twook.ThemeUtils;
import com.jldroid.twook.model.ColumnInfo;
import com.jldroid.twook.model.ColumnManager;
import com.jldroid.twook.widget.ColumnWidgetProvider;

public class ConfigurateWidgetActivity extends SherlockListActivity {
	
	protected ColumnManager mCM;
	
	protected int mAppWidgetId;
	
	@Override
	protected void onCreate(Bundle pSavedInstanceState) {
		super.onCreate(pSavedInstanceState);
		ThemeUtils.setupActivityTheme(this);
		
		mCM = ColumnManager.getInstance(this);
		
		if (VERSION.SDK_INT < 11) {
			Toast.makeText(getApplicationContext(), R.string.higher_api_lvl_needed, Toast.LENGTH_LONG).show();
			setResult(RESULT_CANCELED);
			finish();
		}
		
		mAppWidgetId = getIntent().getExtras().getInt(
	            AppWidgetManager.EXTRA_APPWIDGET_ID, 
	            AppWidgetManager.INVALID_APPWIDGET_ID);
		
		getListView().setAdapter(new MyAdapter());
		getListView().setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> pParent, View pView, int pPosition, long pId) {
				StorageManager sm = StorageManager.getDeflaut(getApplicationContext());
				ColumnInfo info = mCM.getColumnInfo(pPosition);
				sm.write("WIDGET" + mAppWidgetId, info.getProvider().getStorageName());
				sm.flushAsync(0);
				
				Intent resultValue = new Intent();
				resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
				setResult(RESULT_OK, resultValue);
				finish();
				
				Intent updateIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
				updateIntent.setComponent(new ComponentName(getApplicationContext(), ColumnWidgetProvider.class));
				updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {mAppWidgetId});
				getApplicationContext().sendBroadcast(updateIntent);
			}
		});
	}
	
	private class MyAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mCM.getColumnCount();
		}

		@Override
		public ColumnInfo getItem(int pPosition) {
			return mCM.getColumnInfo(pPosition);
		}

		@Override
		public long getItemId(int pPosition) {
			return getItem(pPosition).getProvider().getStorageName().hashCode();
		}

		@Override
		public View getView(int pPosition, View pConvertView, ViewGroup pParent) {
			final ColumnInfo info = getItem(pPosition);
			View v = pConvertView;
			if (v == null) {
				v =  getLayoutInflater().inflate(R.layout.selectable_item, null);
			}
			TextView tv = (TextView) v.findViewById(R.id.textView);
			CheckBox cb = (CheckBox) v.findViewById(R.id.checkBox);
			cb.setVisibility(View.INVISIBLE);
			tv.setText(info.getProvider().getName(getApplicationContext()));
			return v;
		}
		
	}
}
