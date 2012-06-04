package com.jldroid.twook.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.jdroid.utils.ListUtils;
import com.jdroid.utils.SortedArrayList;
import com.jdroid.utils.Threads;
import com.jldroid.twook.R;
import com.jldroid.twook.activities.MainActivity;
import com.jldroid.twook.model.AccountsManager;
import com.jldroid.twook.model.ColumnProviderListener;
import com.jldroid.twook.model.IAccount;
import com.jldroid.twook.model.SearchColumn;
import com.jldroid.twook.model.User;
import com.jldroid.twook.view.ColumnView;
import com.jldroid.twook.view.UserAdapter;

public class SearchFragment extends SherlockFragment implements OnPageChangeListener, TabListener, TextWatcher, ColumnProviderListener {

	public static final int TYPE_UNKNOWN = -1;
	public static final int TYPE_PEOPLE = 0;
	public static final int TYPE_MESSAGES = 1;
	
	public static final String EXTRA_QUERY = "com.jldroid.twook.QUERY";
	public static final String EXTRA_TYPE = "com.jldroid.twook.TYPE";
	
	protected static final int[] TABS = {R.string.tab_messages, R.string.tab_people};
	
	private ViewPager mViewPager;
	private MyAdapter mAdapter;
	
	protected View[] mViews = new View[2];
	
	protected ColumnView mMessagesColumnView;
	protected ListView mPeopleListView;
	
	protected EditText mSearchView;
	
	private SearchColumn mSearchColumn;
	
	private MenuItem mSearchItem;
	
	private SortedArrayList<User> mPeople = new SortedArrayList<User>(30);
	private UserAdapter mPeopleAdapter;
	
	private boolean isPeopleUpdating = false;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater pInflater, ViewGroup pContainer, Bundle pSavedInstanceState) {
		mViewPager = new ViewPager(getActivity());
		mMessagesColumnView = new ColumnView(getActivity());
		mPeopleListView = new ListView(getActivity());
		mViews[0] = mMessagesColumnView;
		mViews[1] = mPeopleListView;
		mViewPager.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		mSearchView = new EditText(getSherlockActivity().getSupportActionBar().getThemedContext());
		mSearchView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		mSearchView.setHint(R.string.search_hint);
		mSearchView.setTextColor(Color.WHITE);
		mSearchView.setBackgroundResource(R.drawable.edit_text_holo_light);
		mSearchView.addTextChangedListener(this);
		mSearchView.setInputType(EditorInfo.TYPE_CLASS_TEXT);
		mSearchView.setMaxLines(1);
		mSearchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
		mSearchView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
		    @Override
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
		            doSearch();
		            return true;
		        }
		        return false;
		    }
		});
		return mViewPager;
	}
	
	@Override
	public void onActivityCreated(Bundle pSavedInstanceState) {
		super.onActivityCreated(pSavedInstanceState);
		mAdapter = new MyAdapter();
		
		mViewPager.setAdapter(mAdapter);
		mViewPager.setOnPageChangeListener(this);
		
		mSearchColumn = new SearchColumn();
		AccountsManager am = AccountsManager.getInstance(getSherlockActivity().getApplicationContext());
		for (int i = 0; i < am.getAccountCount(); i++) {
			mSearchColumn.addAccount(am.getAccount(i));
		}
		
		mMessagesColumnView.setProvider(mSearchColumn, false);
		
		mPeopleListView.setAdapter(mPeopleAdapter = new UserAdapter(getActivity(), mPeople));
		mPeopleListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> pParent, View pView, int pPosition, long pId) {
				AccountsManager am = AccountsManager.getInstance(getSherlockActivity().getApplicationContext());
				User user = mPeople.get(pPosition);
				if (!am.viewProfile((MainActivity) getActivity(), user)) {
					IAccount account = null;
					if (user.type == User.TYPE_FACEBOOK) {
						account = am.getFacebookAccount(0);
					} else if (user.type == User.TYPE_TWITTER) {
						account = am.getTwitterAccount(0);
					}
					if (account != null)
						AccountsManager.viewProfile((MainActivity) getActivity(), account, user);
				}
			}
		});
		
		mSearchColumn.addListener(this);
		
		getSherlockActivity().getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		for (int i = 0; i < TABS.length; i++) {
			getSherlockActivity().getSupportActionBar().addTab(getSherlockActivity().getSupportActionBar().newTab().setText(TABS[i]).setTabListener(this));
		}
		if (getArguments() != null) {
			switch (getArguments().getInt(EXTRA_TYPE, TYPE_UNKNOWN)) {
			case TYPE_PEOPLE:
				getSherlockActivity().getSupportActionBar().setSelectedNavigationItem(1);
				break;
			case TYPE_MESSAGES:
			case TYPE_UNKNOWN:
				getSherlockActivity().getSupportActionBar().setSelectedNavigationItem(0);
			default:
				break;
			}
		}
		getSherlockActivity().getSupportActionBar().setCustomView(mSearchView);
		getSherlockActivity().getSupportActionBar().setDisplayShowCustomEnabled(true);
		mSearchView.requestFocus();
		
		setHasOptionsMenu(true);
		
		String q = getArguments() != null ? getArguments().getString(EXTRA_QUERY) : null;
		if (q != null) {
			mSearchView.setText(q);
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu pMenu, MenuInflater pInflater) {
		super.onCreateOptionsMenu(pMenu, pInflater);
		mSearchItem = pMenu.add(Menu.NONE, 1, Menu.NONE, R.string.search).setIcon(R.drawable.actionbar_search);
		mSearchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		onUpdateStateChanged(mSearchColumn.isUpdating());
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem pItem) {
		if (pItem.getItemId() == 1) {
			doSearch();
		}
		return true;
	}
	
	@Override
	public void onStart() {
		super.onStart();
        mMessagesColumnView.onStart();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		mMessagesColumnView.onStop();
	}
	
	protected void checkIsUpdating() {
		if (mSearchItem != null && getSherlockActivity().getSupportActionBar() != null) {
			Threads.runOnUIThread(new Runnable() {
				@Override
				public void run() {
					int pos = getSherlockActivity().getSupportActionBar().getSelectedNavigationIndex();
					if ((pos == 0 && mSearchColumn.isUpdating()) || (pos == 1 && isPeopleUpdating)) {
						mSearchItem.setActionView(R.layout.ab_progressbar);
					} else {
						mSearchItem.setActionView(null);
					}
				}
			});
		}
	}
	
	@Override
	public void onEnlargingStateChanged(boolean pIsEnlarging) {
	}
	@Override
	public void onHasOlderMessagesChanged(boolean pV) {
	}
	@Override
	public void onMessagesChanged() {
	}
	@Override
	public void onUpdateStateChanged(final boolean pIsUpdating) {
		checkIsUpdating();
	}
	
	protected void doSearch() {
		int pos = getSherlockActivity().getSupportActionBar().getSelectedTab().getPosition();
		if (pos == 0) {
			mSearchColumn.requestUpdate(null);
		} else if (pos == 1) {
			doPeopleSearch();
		}
	}
	
	protected void doLocalPeopleSearch() {
		String query = mSearchView.getText().toString();
		mPeople.clear();
		if (!TextUtils.isEmpty(query)) {
			AccountsManager am = AccountsManager.getInstance(getSherlockActivity().getApplicationContext());
			for (int i = am.getAccountCount() - 1; i >= 0; i--) {
				am.getAccount(i).searchPeopleLocal(mPeople, query);
			}
		}
		mPeopleAdapter.notifyDataSetChanged();
	}
	
	protected void doPeopleSearch() {
		if (!isPeopleUpdating) {
			isPeopleUpdating = true;
			checkIsUpdating();
			Threads.runOnNetworkThread(new Runnable() {
				@Override
				public void run() {
					final SortedArrayList<User> newPeople = new SortedArrayList<User>(mPeople.size());
					ListUtils.clone(mPeople, newPeople);
					String query = mSearchView.getText().toString();
					if (!TextUtils.isEmpty(query)) {
						AccountsManager am = AccountsManager.getInstance(getSherlockActivity().getApplicationContext());
						if (am.getFacebookAccountCount() > 0) {
							am.getFacebookAccount(0).searchPeople(newPeople, query);
						}
						if (am.getTwitterAccountCount() > 0) {
							am.getTwitterAccount(0).searchPeople(newPeople, query);
						}
					}
					Threads.runOnUIThread(new Runnable() {
						@Override
						public void run() {
							ListUtils.clone(newPeople, mPeople);
							mPeopleAdapter.notifyDataSetChanged();
							isPeopleUpdating = false;
							checkIsUpdating();
						}
					});
				}
			});
		}
	}
	
	@Override
	public void afterTextChanged(Editable pS) {
		if (mSearchColumn != null) {
			mSearchColumn.setQuery(pS.toString());
			doLocalPeopleSearch();
		}
	}
	@Override
	public void beforeTextChanged(CharSequence pS, int pStart, int pCount, int pAfter) {
	}
	@Override
	public void onTextChanged(CharSequence pS, int pStart, int pBefore, int pCount) {
	}
	
	@Override
	public void onPageScrolled(int pArg0, float pArg1, int pArg2) {
	}
	@Override
	public void onPageScrollStateChanged(int pArg0) {
	}
	@Override
	public void onPageSelected(int pArg0) {
		getSherlockActivity().getSupportActionBar().setSelectedNavigationItem(pArg0);
		checkIsUpdating();
	}
	
	@Override
	public void onTabReselected(Tab pTab, FragmentTransaction pFt) {
	}
	@Override
	public void onTabSelected(Tab pTab, FragmentTransaction pFt) {
		mViewPager.setCurrentItem(pTab.getPosition());
	}
	@Override
	public void onTabUnselected(Tab pTab, FragmentTransaction pFt) {
		
	}
	
	private class MyAdapter extends PagerAdapter {

		@Override
		public int getCount() {
			return TABS.length;
		}

		@Override
		public boolean isViewFromObject(View pArg0, Object pArg1) {
			return pArg0 == pArg1;
		}
		
		@Override
		public Object instantiateItem(ViewGroup pContainer, int pPosition) {
			View v = mViews[pPosition];
			pContainer.addView(v);
			v.getLayoutParams().width = v.getLayoutParams().height = LayoutParams.MATCH_PARENT;
			v.setLayoutParams(v.getLayoutParams());
			return v;
		}
		
		@Override
		public void destroyItem(ViewGroup pContainer, int pPosition, Object pObject) {
			View v = mViews[pPosition];
			pContainer.removeView(v);
		}
		
		@Override
		public CharSequence getPageTitle(int pPosition) {
			return getString(TABS[pPosition]);
		}
		
	}
}
