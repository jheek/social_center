package com.jldroid.twook.model;

public class TwitterMessageTable {
	
	public static final String COLUMN_ID = "id";
	public static final String COLUMN_SENDER = "sender";
	public static final String COLUMN_TEXT = "text";
	public static final String COLUMN_SOURCE = "source";
	public static final String COLUMN_CREATED = "created";
	
	public static final String CREATE = "CREATE table %s (" + COLUMN_ID + " PRIMARY INT, " + COLUMN_SENDER + " INT, " + COLUMN_TEXT + " TEXT, " + 
									COLUMN_SOURCE + " TEXT, " + COLUMN_CREATED + " INT);";
	
	public static String generateCreateSQL(String tableName) {
		return String.format(CREATE, tableName);
	}
			
}
