package com.jldroid.twook.model;

import android.content.Context;

import com.jldroid.twook.R;
import java.util.ArrayList;

public class SearchColumn extends MergedColumnMessageProvider {

	private ArrayList<IAccount> mAccounts = new ArrayList<IAccount>();
	private String mQuery;
	
	public SearchColumn() {
		super();
	}
	
	@Override
	public String getName(Context c) {
		return c.getString(R.string.search_name, mQuery);
	}
	
	@Override
	public String getDescription(Context c) {
		return c.getString(R.string.search_description);
	}

	@Override
	public long getID() {
		return 0;
	}

	@Override
	public int getWhat() {
		return SyncManager.WHAT_SEARCH;
	}

	@Override
	public int getOrder() {
		return 3;
	}

	@Override
	public String getStorageName() {
		return "search" + mQuery;
	}
	
	public String getQuery() {
		return mQuery;
	}
	
	public void setQuery(String pQuery) {
		mQuery = pQuery;
		for (int i = 0; i < mProviders.size(); i++) {
			ISearchableColumn column = (ISearchableColumn) mProviders.get(i);
			column.setQuery(pQuery);
		}
	}
	
	public void addAccount(IAccount account) {
		mAccounts.add(account);
		addProvider(account.createSearchColumn(mQuery));
	}
	
	public void removeAccount(IAccount account) {
		mAccounts.remove(account);
		for (int i = 0; i < mAccounts.size(); i++) {
			ISearchableColumn column = (ISearchableColumn) mProviders.get(i);
			if (column.getAccount() == account) {
				removeProvider(column);
				break;
			}
		}
	}
}
