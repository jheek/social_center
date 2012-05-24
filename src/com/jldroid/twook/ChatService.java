package com.jldroid.twook;

import com.jldroid.twook.model.AccountsManager;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ChatService extends Service {

	@Override
	public int onStartCommand(Intent pIntent, int pFlags, int pStartId) {
		AccountsManager.getInstance(getApplicationContext());
		return START_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent pIntent) {
		return null;
	}

}
