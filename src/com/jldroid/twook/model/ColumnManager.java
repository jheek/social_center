package com.jldroid.twook.model;

import android.content.Context;

import com.jldroid.twook.model.AccountsManager.AccountsManagerListener;
import com.jldroid.twook.model.facebook.FacebookAccount;
import com.jldroid.twook.model.facebook.FacebookAccount.FacebookAccountListener;
import com.jldroid.twook.model.twitter.TwitterAccount;
import com.jldroid.twook.model.twitter.TwitterAccount.TwitterAccountListener;
import java.util.ArrayList;
import com.jdroid.utils.SortedArrayList;

public class ColumnManager implements AccountsManagerListener, FacebookAccountListener, TwitterAccountListener {
	
	private static final int ORDER_HOME = 0;
	private static final int ORDER_ABOUTME = 1;
	private static final int ORDER_MESSAGES = 2;
	private static final int ORDER_TWITTER = 3;
	private static final int ORDER_FACEBOOK = 4;
	
	private static ColumnManager sInstance;
	
	private Context mContext;
	
	private SortedArrayList<ColumnInfo> mColumns = new SortedArrayList<ColumnInfo>();
	private SortedArrayList<ColumnInfo> mEnabledColumns = new SortedArrayList<ColumnInfo>();
	
	private ArrayList<OnColumnsChangeListener> mListeners = new ArrayList<OnColumnsChangeListener>();
	
	private boolean mInForeground = false;
	
	private ColumnInfo mHomeColumn;
	private HomeProvider mHomeProvider;
	
	private ColumnInfo mAboutMeColumn;
	private AboutMeProvider mAboutMeProvider;
	
	private ColumnInfo mMessagesColumn;
	private MessagesProvider mMessagesProvider;
	
	public ColumnManager(Context c) {
		mContext = c;
		mHomeProvider = new HomeProvider(c);
		mHomeColumn = addColumn(ORDER_HOME, null, mHomeProvider);
		mAboutMeProvider = new AboutMeProvider(c);
		mAboutMeColumn = addColumn(ORDER_ABOUTME, null, mAboutMeProvider);
		mMessagesProvider = new MessagesProvider(c);
		mMessagesColumn = addColumn(ORDER_MESSAGES, null, mMessagesProvider);
		AccountsManager am = AccountsManager.getInstance(c);
		for (int i = 0; i < am.getAccountCount(); i++) {
			onAccountAdded(am.getAccount(i));
		}
		am.addListener(this);
	}
	
	public void setupColumns(boolean combine) {
		disableAllColumns();
		if (combine) {
			setColumnEnabled(mHomeColumn, true);
			setColumnEnabled(mAboutMeColumn, true);
			setColumnEnabled(mMessagesColumn, true);
		} else {
			AccountsManager am = AccountsManager.getInstance(mContext);
			for (int i = 0; i < am.getFacebookAccountCount(); i++) {
				FacebookAccount fa = am.getFacebookAccount(i);
				setColumnEnabled(findColumn(fa.getHomeProvider()), true);
				setColumnEnabled(findColumn(fa.getNotificationsProvider()), true);
				setColumnEnabled(findColumn(fa.getMessagesProvider()), true);
			}
			for (int i = 0; i < am.getTwitterAccountCount(); i++) {
				TwitterAccount ta = am.getTwitterAccount(i);
				setColumnEnabled(findColumn(ta.getTimelineProvider()), true);
				setColumnEnabled(findColumn(ta.getMentionsProvider()), true);
				setColumnEnabled(findColumn(ta.getDirectMessagesProvider()), true);
			}
		}
	}
	
	public void addListener(OnColumnsChangeListener listener) {
		mListeners.add(listener);
	}
	
	public void removeListener(OnColumnsChangeListener listener) {
		mListeners.remove(listener);
	}
	
	public void dispatchColumnsChanged() {
		for (int i = 0 ; i < mListeners.size(); i++) {
			mListeners.get(i).onColumnsChanged();
		}
	}
	
	public ColumnInfo getColumnInfo(int i) {
		return mColumns.get(i);
	}
	
	public int getColumnCount() {
		return mColumns.size();
	}
	
	public ColumnInfo getEnabledColumnInfo(int i) {
		return mEnabledColumns.get(i);
	}
	
	public int getEnabledColumnCount() {
		return mEnabledColumns.size();
	}

	public void setColumnEnabled(ColumnInfo info, boolean v) {
		if (v != info.isEnabled()) {
			if (v) {
				mEnabledColumns.add(info);
				SyncManager.updateColumnSync(mContext, info);
			} else {
				mEnabledColumns.remove(info);
				SyncManager.stopColumnSync(mContext, info);
			}
			info.setEnabled(v);
			dispatchColumnsChanged();
		}
	}
	
	public void disableAllColumns() {
		for (int i = mEnabledColumns.size() - 1; i >= 0; i--) {
			setColumnEnabled(mEnabledColumns.get(i), false);
		}
	}
	
	public void setInForeground(boolean v) {
		mInForeground = v;
		for (int i = 0; i < mColumns.size(); i++) {
			mColumns.get(i).setInForeground(v);
		}
	}
	
	public boolean isInForeground() {
		return mInForeground;
	}
	
	private ColumnInfo findColumn(ColumnMessagesProvider provider) {
		for (int i = 0; i < mColumns.size(); i++) {
			ColumnInfo info = mColumns.get(i);
			if (info.getProvider() == provider) {
				return info;
			}
		}
		return null;
	}
	
	private ColumnInfo addColumn(int order, IAccount account, ColumnMessagesProvider provider) {
		ColumnInfo info = new ColumnInfo(mContext, order, account, provider);
		mColumns.add(info);
		if (info.isEnabled()) {
			mEnabledColumns.add(info);
			SyncManager.updateColumnSync(mContext, info);
		}
		dispatchColumnsChanged();
		return info;
	}
	
	private void removeColumn(ColumnMessagesProvider provider) {
		for (int i = 0; i < mColumns.size(); i++) {
			ColumnInfo info = mColumns.get(i);
			if (info.getProvider() == provider) {
				mColumns.remove(info);
				mEnabledColumns.remove(info);
				SyncManager.stopColumnSync(mContext, info);
				dispatchColumnsChanged();
				return;
			}
		}
	}
	
	public int getColumnIndex(ColumnInfo pInfo) {
		return mColumns.indexOf(pInfo);
	}
	
	private int getAccountOrder(IAccount account) {
		return account instanceof TwitterAccount ? ORDER_TWITTER : ORDER_FACEBOOK;
	}
	
	@Override
	public void onAccountAdded(IAccount pAccount) {
		int order = getAccountOrder(pAccount);
		ArrayList<ColumnMessagesProvider> providers = pAccount.getProviders();
		for (int i = 0; i < providers.size(); i++) {
			addColumn(order, pAccount, providers.get(i));
		}
		if (pAccount instanceof FacebookAccount) {
			((FacebookAccount) pAccount).addListener(this);
		} else if (pAccount instanceof TwitterAccount) {
			((TwitterAccount) pAccount).addListener(this);
		}
	}
	
	@Override
	public void onAccountRemoved(IAccount pAccount) {
		ArrayList<ColumnMessagesProvider> providers = pAccount.getProviders();
		for (int i = 0; i < providers.size(); i++) {
			removeColumn(providers.get(i));
		}
		if (pAccount instanceof FacebookAccount) {
			((FacebookAccount) pAccount).removeListener(this);
		} else if (pAccount instanceof TwitterAccount) {
			((TwitterAccount) pAccount).removeListener(this);
		}
	}
	
	private void handleProvidersChanged(IAccount account) {
		for (int i = mColumns.size() - 1; i >= 0; i--) {
			ColumnInfo column = mColumns.get(i);
			if (column.getAccount() == account) {
				removeColumn(column.getProvider());
			}
		}
		int order = getAccountOrder(account);
		ArrayList<ColumnMessagesProvider> providers = account.getProviders();
		for (int i = 0; i < providers.size(); i++) {
			addColumn(order, account, providers.get(i));
		}
	}
	
	@Override
	public void onProvidersChanged(FacebookAccount pAccount) {
		handleProvidersChanged(pAccount);
	}
	
	@Override
	public void onProvidersChanged(TwitterAccount pAccount) {
		handleProvidersChanged(pAccount);
	}
	
	public ColumnInfo getHomeColumn() {
		return mHomeColumn;
	}

	public HomeProvider getHomeProvider() {
		return mHomeProvider;
	}

	public ColumnInfo getAboutMeColumn() {
		return mAboutMeColumn;
	}

	public AboutMeProvider getAboutMeProvider() {
		return mAboutMeProvider;
	}

	public ColumnInfo getMessagesColumn() {
		return mMessagesColumn;
	}

	public MessagesProvider getMessagesProvider() {
		return mMessagesProvider;
	}

	public static synchronized ColumnManager getInstance(Context c) {
		if (sInstance == null) {
			sInstance = new ColumnManager(c.getApplicationContext());
		}
		return sInstance;
	}
	
	public static synchronized ColumnManager peekInstance() {
		return sInstance;
	}
	
	public static interface OnColumnsChangeListener {
		public void onColumnsChanged();
	}
	
	@Override
	public void onDirectMessagesChanged(int pNewCount) {
	}
	@Override
	public void onMentionsChanged(int pNewCount) {
	}
	@Override
	public void onTimelineChanged(int pNewCount) {
	}
}
