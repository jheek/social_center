package com.jldroid.twook;

import android.app.Application;

public class App extends Application {
	
	@Override
	public void onCreate() {
		super.onCreate();
	}
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		Globals.cleanup(this, false);
	}
}
