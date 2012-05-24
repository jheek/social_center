package com.jldroid.twook.model;

public interface ISearchableColumn extends ColumnMessagesProvider {

	public IAccount getAccount();
	
	public String getQuery();
	public void setQuery(String query);
	
}
