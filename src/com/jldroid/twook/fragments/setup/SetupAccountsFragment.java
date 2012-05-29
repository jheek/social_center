package com.jldroid.twook.fragments.setup;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.jdroid.utils.Threads;
import com.jldroid.twook.R;
import com.jldroid.twook.activities.AddFacebookAccountActivity;
import com.jldroid.twook.activities.AddTwitterAccountActivity;
import com.jldroid.twook.model.AccountsManager;
import com.jldroid.twook.model.AccountsManager.AccountsManagerListener;
import com.jldroid.twook.model.IAccount;
import com.jldroid.twook.model.User;

public class SetupAccountsFragment extends SherlockListFragment implements ISetupFragment {

	protected AccountsManager mAM;
	
	private MyAdapter mAdapter;
	
	@Override
	public View onCreateView(LayoutInflater pInflater, ViewGroup pContainer, Bundle pSavedInstanceState) {
		return pInflater.inflate(R.layout.setup_accounts, null);
	}
	
	@Override
	public void onActivityCreated(Bundle pSavedInstanceState) {
		super.onActivityCreated(pSavedInstanceState);
		mAM = AccountsManager.getInstance(getActivity());
		setHasOptionsMenu(true);
		getListView().setAdapter(mAdapter = new MyAdapter());
	}
	
	@Override
	public boolean isProceedAllowed() {
		if (mAM.getAccountCount() == 0) {
			Toast.makeText(getActivity().getApplicationContext(), R.string.add_at_least_one_account, Toast.LENGTH_LONG).show();
		}
		return mAM.getAccountCount() > 0;
	}
	
	@Override
	public void onProceed() {
	}
	
	@Override
	public void onCreateOptionsMenu(Menu pMenu, MenuInflater pInflater) {
		super.onCreateOptionsMenu(pMenu, pInflater);
		pMenu.add(Menu.NONE, 1, Menu.NONE, R.string.add_twitter).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		pMenu.add(Menu.NONE, 2, Menu.NONE, R.string.add_facebook).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		mAM.addListener(mAdapter);
		mAdapter.notifyDataSetChanged();
	}
	
	@Override
	public void onStop() {
		super.onStop();
	}
	
	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem pItem) {
		switch (pItem.getItemId()) {
		case 1: // add twitter
			getActivity().startActivity(new Intent(getActivity(), AddTwitterAccountActivity.class));
			break;
		case 2: // add facebook
			getActivity().startActivity(new Intent(getActivity(), AddFacebookAccountActivity.class));
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(pItem);
	}
	
	private class MyAdapter extends BaseAdapter implements AccountsManagerListener, Runnable {

		@Override
		public int getCount() {
			return mAM.getAccountCount();
		}

		@Override
		public IAccount getItem(int pPosition) {
			return mAM.getAccount(pPosition);
		}

		@Override
		public long getItemId(int pPosition) {
			return getItem(pPosition).getUser().id;
		}

		@Override
		public View getView(int pPosition, View pConvertView, ViewGroup pParent) {
			IAccount account = getItem(pPosition);
			TextView tv = (TextView) pConvertView;
			if (tv == null) {
				tv = (TextView) LayoutInflater.from(getActivity()).inflate(R.layout.account_item, null);
			}
			tv.setText(account.getUser().name);
			tv.setCompoundDrawablesWithIntrinsicBounds(account.getUser().type == User.TYPE_TWITTER ? R.drawable.twitter_icon_big : R.drawable.facebook_icon_big, 0, 0, 0);
			return tv;
		}
		
		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public void run() {
			notifyDataSetChanged();
		}
		
		@Override
		public void onAccountAdded(IAccount pAccount) {
			Threads.runOnUIThread(this);
		}

		@Override
		public void onAccountRemoved(IAccount pAccount) {
			Threads.runOnUIThread(this);
		}
		
	}
	
}
