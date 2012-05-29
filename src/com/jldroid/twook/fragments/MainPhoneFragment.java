package com.jldroid.twook.fragments;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.jdroid.utils.Threads;
import com.jldroid.twook.R;
import com.jldroid.twook.activities.ChatsActivity;
import com.jldroid.twook.activities.ComposeActivity;
import com.jldroid.twook.activities.DonateActivity;
import com.jldroid.twook.activities.MainActivity;
import com.jldroid.twook.activities.PeopleActivity;
import com.jldroid.twook.activities.PrefsActivity;
import com.jldroid.twook.activities.SearchActivity;
import com.jldroid.twook.activities.SetupActivity;
import com.jldroid.twook.model.ColumnInfo;
import com.jldroid.twook.model.ColumnManager;
import com.jldroid.twook.model.ColumnManager.OnColumnsChangeListener;
import com.jldroid.twook.model.ColumnMessagesProvider;
import com.jldroid.twook.model.ColumnProviderListener;
import com.jldroid.twook.model.IAccount;
import com.jldroid.twook.model.INetworkCallback;
import com.jldroid.twook.view.ColumnView;

public class MainPhoneFragment extends SherlockFragment implements OnPageChangeListener, OnColumnsChangeListener, ColumnProviderListener, TabListener {
	
	protected ViewPager mViewPager;
	protected LinearLayout mColumnsHolder;
	protected RelativeLayout mEditColumnsBar;
	
	protected MyAdapter mAdapter;
	protected ArrayList<ColumnView> mActiveViews = new ArrayList<ColumnView>();
	protected ArrayList<ColumnView> mRecycleViews = new ArrayList<ColumnView>();
	protected ColumnView mCurrentView;
	
	protected ColumnManager mCM;
	
	protected boolean isMenuVisisble = true;
	
	protected MenuItem mRefreshItem;
	
	@Override
	public View onCreateView(LayoutInflater pInflater, ViewGroup pContainer, Bundle pSavedInstanceState) {
		View v = pInflater.inflate(R.layout.main_phone, null);
		mViewPager = (ViewPager) v.findViewById(R.id.viewpager);
		
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle pSavedInstanceState) {
		super.onActivityCreated(pSavedInstanceState);
		mCM = ColumnManager.getInstance(getActivity());
		
		setHasOptionsMenu(true);
		
        mAdapter = new MyAdapter();
	    
    	mViewPager.setAdapter(mAdapter);
    	
        mViewPager.setOnPageChangeListener(this);
        
        if (getArguments() != null && getArguments().containsKey(MainActivity.EXTRA_COLUMN)) {
			setColumn(getActivity(), getArguments().getString(MainActivity.EXTRA_COLUMN));
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu pMenu, MenuInflater pInflater) {
		super.onCreateOptionsMenu(pMenu, pInflater);
		mRefreshItem = pMenu.add(Menu.NONE, 1, Menu.NONE, R.string.refresh).setIcon(R.drawable.actionbar_refresh).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
		pMenu.add(Menu.NONE, 2, Menu.NONE, R.string.search);
		pMenu.add(Menu.NONE, 6, Menu.NONE, R.string.compose).setIcon(R.drawable.actionbar_compose).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		pMenu.add(Menu.NONE, 7, Menu.NONE, R.string.chat).setIcon(R.drawable.actionbar_chat).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		pMenu.add(Menu.NONE, 8, Menu.NONE, R.string.people).setIcon(R.drawable.actionbar_people).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		pMenu.add(Menu.NONE, 3, Menu.NONE, R.string.settings);
		pMenu.add(Menu.NONE, 4, Menu.NONE, R.string.setup); // TODO remove
		pMenu.add(Menu.NONE, 5, Menu.NONE, R.string.donate);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem pItem) {
		Activity a = getActivity();
		switch (pItem.getItemId()) {
		case 1: // refresh
			int currentItem = mViewPager.getCurrentItem();
			if (currentItem >= 0 && currentItem < mCM.getEnabledColumnCount()) {
				mCM.getEnabledColumnInfo(mViewPager.getCurrentItem()).getProvider().requestUpdate(new INetworkCallback() {
					@Override
					public void onSucceed(IAccount pAccount) {
					}
					@Override
					public void onNoNetwork(IAccount pAccount) {
						onFailed(pAccount);
					}
					@Override
					public void onFailed(IAccount pAccount) {
						Threads.runOnUIThread(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(getActivity().getApplicationContext(), R.string.error_check_network, Toast.LENGTH_LONG).show();
							}
						});
					}
				});
			}
			break;
		case 2: // search
			a.startActivity(new Intent(a.getApplicationContext(), SearchActivity.class));
			break;
		case 3: // Settings
			a.startActivity(new Intent(a.getApplicationContext(), PrefsActivity.class));
			break;
		case 4: // setup
			a.startActivity(new Intent(a.getApplicationContext(), SetupActivity.class));
			break;
		case 5: // donate
			a.startActivity(new Intent(a.getApplicationContext(), DonateActivity.class));
			break;
		case 6: // compose
			a.startActivity(new Intent(a.getApplicationContext(), ComposeActivity.class));
			break;
		case 7: // chat
			a.startActivity(new Intent(a.getApplicationContext(), ChatsActivity.class));
			break;
		case 8: // people
			a.startActivity(new Intent(a.getApplicationContext(), PeopleActivity.class));
			break;
		default:
			break;
		}
		return true;
	}
	
	public void setColumn(Context c, String storageName) {
		if (getActivity() == null || mViewPager == null) {
			return;
		}
		mCM = ColumnManager.getInstance(c);
		for (int i = 0; i < mCM.getEnabledColumnCount(); i++) {
        	ColumnInfo info = mCM.getEnabledColumnInfo(i);
        	if (info.getProvider().getStorageName().equals(storageName)) {
        		mViewPager.setCurrentItem(i);
        		break;
        	}
        }
	}
	
	@Override
	public void onStart() {
		super.onStart();
		mCM.addListener(this);
		for (int i = 0; i < mActiveViews.size(); i++) {
			ColumnView cv = mActiveViews.get(i);
			cv.getProvider().addListener(this);
			cv.onStart();
		}
		onColumnsChanged();
		
		if (mViewPager.getCurrentItem() != -1 && mViewPager.getCurrentItem() < mCM.getEnabledColumnCount()) {
			ColumnMessagesProvider provider = mCM.getEnabledColumnInfo(mViewPager.getCurrentItem()).getProvider();
			checkIsUpdating(provider);
			checkUpdate(provider);
		}
		
		getSherlockActivity().getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		getSherlockActivity().getSupportActionBar().setDisplayShowTitleEnabled(false);
		getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		mCM.removeListener(this);
		for (int i = 0; i < mActiveViews.size(); i++) {
			ColumnView cv = mActiveViews.get(i);
			cv.getProvider().removeListener(this);
			cv.onStop();
		}
		
		getSherlockActivity().getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		getSherlockActivity().getSupportActionBar().setDisplayShowTitleEnabled(true);
		getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(true);
	}
	
	@Override
	public void onColumnsChanged() {
		getSherlockActivity().getSupportActionBar().removeAllTabs();
		for (int i = 0; i < mCM.getEnabledColumnCount(); i++) {
			ColumnInfo info = mCM.getEnabledColumnInfo(i);
			Tab tab = getSherlockActivity().getSupportActionBar().newTab();
			tab.setText(info.getProvider().getName(getActivity()));
			tab.setTabListener(this);
			getSherlockActivity().getSupportActionBar().addTab(tab);
		}
		mAdapter.notifyDataSetChanged();
	}
	
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }
    
    @Override
    public void onPageScrollStateChanged(int pArg0) {
    }
    
    @Override
	public void onPageSelected(int pArg0) {
    	ColumnMessagesProvider provider = mCM.getEnabledColumnInfo(pArg0).getProvider();
    	getSherlockActivity().getSupportActionBar().setSelectedNavigationItem(pArg0);
    	checkIsUpdating(provider);
    	checkUpdate(provider);
	}
    
    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    	mCurrentView.scrollToTop();
    }
    
    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
    	mViewPager.setCurrentItem(tab.getPosition());
    }
    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }
    
    private void checkIsUpdating(ColumnMessagesProvider provider) {
    	if (provider.isUpdating()) {
			mRefreshItem.setActionView(R.layout.ab_progressbar);
		} else {
			mRefreshItem.setActionView(null);
		}
    }
    
    private void checkUpdate(ColumnMessagesProvider provider) {
    	ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo network = cm.getActiveNetworkInfo();
    	boolean isWifi = network != null ? network.getType() == ConnectivityManager.TYPE_WIFI : false;
    	long dif = System.currentTimeMillis() - provider.getLastUpdate();
    	int minutesAgo = (int) (dif / 60000);
    	if (!provider.isUpdating() && minutesAgo > (isWifi ? 2 : 15)) {
    		provider.requestUpdate(null);
    	}
    }
    
    @Override
    public void onHasOlderMessagesChanged(boolean pV) {
    }
    
    @Override
    public void onMessagesChanged() {
    }
    
    @Override
    public void onEnlargingStateChanged(boolean pIsEnlarging) {
    }
    
    private Runnable mUpdateRefreshRunnable = new Runnable() {
		@Override
		public void run() {
			checkIsUpdating(mCM.getEnabledColumnInfo(mViewPager.getCurrentItem()).getProvider());
		}
	};
    
    @Override
    public void onUpdateStateChanged(boolean pIsUpdating) {
    	Threads.runOnUIThread(mUpdateRefreshRunnable);
    }
    
    private class MyAdapter extends PagerAdapter {
    	
    	@Override
    	public void setPrimaryItem(ViewGroup container, int position, Object object) {
    		super.setPrimaryItem(container, position, object);
    		mCurrentView = (ColumnView) object;
    	}
		@Override
		public int getCount() {
			return mCM.getEnabledColumnCount();
		}

		@Override
		public boolean isViewFromObject(View pArg0, Object pArg1) {
			return pArg0 == pArg1;
		}
		
		@Override
		public Object instantiateItem(ViewGroup pContainer, int pPosition) {
			ColumnView cv;
			if (mRecycleViews.size() > 0 ) {
				cv = mRecycleViews.remove(mRecycleViews.size() - 1);
			} else {
				cv = new ColumnView(getActivity());
			}
			ColumnInfo info = mCM.getEnabledColumnInfo(pPosition);
			cv.setTag(info);
			cv.setProvider(info);
			cv.getProvider().addListener(MainPhoneFragment.this);
			pContainer.addView(cv);
			mActiveViews.add(cv);
			if (pPosition == mViewPager.getCurrentItem()) {
				checkIsUpdating(info.getProvider());
			}
			return cv;
		}
		
		@Override
		public void destroyItem(ViewGroup pContainer, int pPosition, Object pObject) {
			ColumnView cv = (ColumnView) pObject;
			pContainer.removeView(cv);
			cv.onStop();
			cv.getProvider().removeListener(MainPhoneFragment.this);
			mRecycleViews.add(cv);
			mActiveViews.remove(cv);
		}
		
		@Override
		public int getItemPosition(Object pObject) {
			ColumnView cv = (ColumnView) pObject;
			ColumnInfo info = cv.getInfo();
			ColumnMessagesProvider provider = cv.getProvider();
			for (int i = 0; i < mCM.getEnabledColumnCount(); i++) {
				if (mCM.getEnabledColumnInfo(i) == info && provider == info.getProvider()) {
					return i;
				}
			}
			return PagerAdapter.POSITION_NONE;
		}
    }
}
