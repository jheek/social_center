package com.jldroid.twook.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.jdroid.utils.CategoryAdapter;
import com.jdroid.utils.SortedArrayList;
import com.jdroid.utils.Threads;
import com.jldroid.twook.R;
import com.jldroid.twook.activities.MainActivity;
import com.jldroid.twook.activities.ViewProfileActivity;
import com.jldroid.twook.model.IAccount;
import com.jldroid.twook.model.INetworkCallback;
import com.jldroid.twook.model.User;
import com.jldroid.twook.model.facebook.FacebookAccount;
import com.jldroid.twook.model.twitter.TwitterAccount;
import com.jldroid.twook.view.AccountTabsView;
import com.jldroid.twook.view.UserAdapter;

public class PeopleFragment extends SherlockFragment {

	private AccountTabsView mAccountTabs;
	
	private ListView mListView;
	
	private CategoryAdapter mAdapter;
	
	private IAccount mAccount;
	
	private MenuItem mRefreshItem;
	
	@Override
	public View onCreateView(LayoutInflater pInflater, ViewGroup pContainer, Bundle pSavedInstanceState) {
		View v = pInflater.inflate(R.layout.people, null);
		mAccountTabs = (AccountTabsView) v.findViewById(R.id.accountTabs);
		mListView = (ListView) v.findViewById(R.id.listView);
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle pSavedInstanceState) {
		super.onActivityCreated(pSavedInstanceState);
		
		setHasOptionsMenu(true);
		
		mAdapter = new CategoryAdapter(getActivity());
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> pParent, View pView, int pPosition, long pId) {
				Object item = mAdapter.getItem(pPosition);
				if (item instanceof User) {
					getActivity().startActivity(new Intent(getSherlockActivity().getApplicationContext(), ViewProfileActivity.class)
						.putExtra(ViewProfileFragment.EXTRA_ACCOUNT, mAccount.getUser().id)
						.putExtra(ViewProfileFragment.EXTRA_USER, ((User) item).id));
				}
			}
		});
		
		mAccountTabs.setSingleSelectionMode(true);
		mAccountTabs.setAccounts(true, true);
		mAccountTabs.setSelectedTab(0);
		
		mAccountTabs.setSelectedAccountsChangedRunnable(new Runnable() {
			@Override
			public void run() {
				populatePeople();
			}
		});
		populatePeople();
	}

	@Override
	public void onCreateOptionsMenu(Menu pMenu, MenuInflater pInflater) {
		super.onCreateOptionsMenu(pMenu, pInflater);
		mRefreshItem = pMenu.add(Menu.NONE, 1, Menu.NONE, R.string.refresh).setIcon(R.drawable.actionbar_refresh);
		mRefreshItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		getSherlockActivity().getSupportActionBar().setTitle(R.string.people);
	}
	
	protected void populatePeople() {
		mAdapter.clear();
		mAccount = mAccountTabs.getSelectedAccounts().get(0);
		if (mAccount instanceof FacebookAccount) {
			final FacebookAccount fa = (FacebookAccount) mAccount;
			mRefreshItem.setActionView(fa.isUpdatingFriends() ? new ProgressBar(getActivity()) : null);
			final SortedArrayList<User> friends = fa.getFriends();
			mAdapter.add(getString(R.string.friends), new UserAdapter(getActivity(), friends));
			if (friends.size() == 0) {
				refreshFriends();
			}
		} else if (mAccount instanceof TwitterAccount) {
			final TwitterAccount ta = (TwitterAccount) mAccount;
			mRefreshItem.setActionView(ta.isUpdatingFriends() ? new ProgressBar(getActivity()) : null);
			mAdapter.add(getString(R.string.following), new UserAdapter(getActivity(), ta.getFollowing()));
			mAdapter.add(getString(R.string.followers), new UserAdapter(getActivity(), ta.getFollowers()));
			if (ta.getFollowers().size() == 0 && ta.getFollowing().size() == 0) {
				refreshFriends();
			}
		}
	}
	
	protected void refreshFriends() {
		mRefreshItem.setActionView(new ProgressBar(getActivity()));
		mAccount.updateFriends(new INetworkCallback() {
			@Override
			public void onSucceed(IAccount pAccount) {
				Threads.runOnUIThread(new Runnable() {
					@Override
					public void run() {
						mRefreshItem.setActionView(null);
						mAdapter.notifyDataSetChanged();
					}
				});
				
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
						mRefreshItem.setActionView(null);
						Toast.makeText(getSherlockActivity().getApplicationContext(), R.string.failed_update, Toast.LENGTH_LONG).show();
					}
				});
				
			}
		});
	}
	
}
