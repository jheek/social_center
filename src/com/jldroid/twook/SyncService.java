package com.jldroid.twook;

import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.jdroid.utils.Threads;
import com.jldroid.twook.model.AccountsManager;
import com.jldroid.twook.model.ColumnInfo;
import com.jldroid.twook.model.ColumnManager;
import com.jldroid.twook.model.IAccount;
import com.jldroid.twook.model.INetworkCallback;
import com.jldroid.twook.model.SyncManager;

public class SyncService extends Service implements INetworkCallback {
	
	protected int mSyncCount = 0;
	
	@Override
	public void onSucceed(IAccount pAccount) {
		Threads.runOnUIThread(new Runnable() {
			@Override
			public void run() {
				mSyncCount--;
				if (mSyncCount == 0) {
					stopSelf();
				}
			}
		});
	}
	
	@Override
	public void onFailed(IAccount pAccount) {
		onSucceed(pAccount);
	}
	
	@Override
	public void onNoNetwork(IAccount pAccount) {
		onFailed(pAccount);
	}
	
	@Override
	public int onStartCommand(final Intent intent, int pFlags, int pStartId) {
		if (intent == null) {
			return START_NOT_STICKY;
		}
		if (intent.getAction().equals(SyncManager.ACTION_SYNC)) {
			List<String> segments = intent.getData().getPathSegments();
			if (segments.size() != 3) {
				return START_NOT_STICKY;
			}
			ColumnManager cm = ColumnManager.getInstance(this);
			AccountsManager am = AccountsManager.getInstance(this);
			
			String host = intent.getData().getHost();
			if (host.equals("column")) {
				long accountID = Long.parseLong(segments.get(0));
				IAccount account = am.findAccount(accountID);
				if (account == null && accountID != -1) {
					SyncManager.stopSync(getApplicationContext(), intent);
					return START_NOT_STICKY;
				}
				int what = Integer.parseInt(segments.get(1));
				long id = Long.parseLong(segments.get(2));
				
				for (int i = cm.getEnabledColumnCount() - 1; i >= 0; i--) {
					ColumnInfo info = cm.getEnabledColumnInfo(i);
					if (info.getProvider().getWhat() == what && info.getProvider().getID() == id && info.getAccount() == account) {
						info.getProvider().requestUpdate(this);
						mSyncCount++;
						SyncManager.checkColumnSyncTimeout(getApplicationContext(), info);
						return START_NOT_STICKY;
					}
				}
				SyncManager.stopSync(getApplicationContext(), intent);
			} else if (host.equals("global")) {
				for (int i = 0; i < am.getAccountCount(); i++) {
					IAccount account = am.getAccount(i);
					account.updateGlobalData(null);
				}
			}
		} else {
			if (mSyncCount == 0) {
				stopSelf(pStartId);
			}
		}
		return START_NOT_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent pIntent) {
		return null;
	}
}
