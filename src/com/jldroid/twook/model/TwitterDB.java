package com.jldroid.twook.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TwitterDB extends SQLiteOpenHelper {

	private static final String DB_NAME = "twitter.db";
	private static final int DB_VERSION = 1;
	
	private static final String TABLE_USERS = "users";
	
	
	
	public TwitterDB(Context c) {
		super(c, DB_NAME, null, DB_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase pDb) {
		pDb.execSQL(UserTable.generateCreateSQL(TABLE_USERS));
	}

	@Override
	public void onUpgrade(SQLiteDatabase pDb, int pOldVersion, int pNewVersion) {
		
	}

}
