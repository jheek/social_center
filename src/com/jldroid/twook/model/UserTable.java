package com.jldroid.twook.model;

public class UserTable {
	
	
	
	public static final String COLUMN_ID = "id";
	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_SCREENNAME = "screenname";
	public static final String COLUMN_PROFILE_URL = "profileurl";
	public static final String COLUMN_LARGE_PROFILE_URL = "largeprofileurl";
	
	private static final String CREATE = "CREATE TABLE %s (" + COLUMN_ID + "PRIMARY INT, " + COLUMN_NAME + " TEXT, " + 
											COLUMN_SCREENNAME + " TEXT, " + COLUMN_PROFILE_URL + " TEXT, " + COLUMN_LARGE_PROFILE_URL + " TEXT);";
	
	
	public static String generateCreateSQL(String tableName) {
		return String.format(CREATE, tableName);
	}
}
