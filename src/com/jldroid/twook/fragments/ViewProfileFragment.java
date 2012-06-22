package com.jldroid.twook.fragments;

import twitter4j.TwitterException;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;
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
import com.jldroid.twook.model.AccountsManager;
import com.jldroid.twook.model.IAccount;
import com.jldroid.twook.model.ImageManager;
import com.jldroid.twook.model.ImageManager.DeletionTrigger;
import com.jldroid.twook.model.ImageManager.LoadBitmapCallback;
import com.jldroid.twook.model.User;
import com.jldroid.twook.model.facebook.FacebookAccount;
import com.jldroid.twook.model.twitter.TwitterAccount;
import com.jldroid.twook.view.AlbumsView;
import com.jldroid.twook.view.ColumnView;

public class ViewProfileFragment extends SherlockFragment implements OnPageChangeListener, OnClickListener, TabListener {
	
	public static final String EXTRA_ACCOUNT = "com.jldroid.twook.ACCOUNT";
	public static final String EXTRA_USER_ID = "com.jldroid.twook.USER_ID";
	public static final String EXTRA_USER_NAME = "com.jldroid.twook.USER_NAME";
	public static final String EXTRA_USER_PIC = "com.jldroid.twook.USER_PIC";
	public static final String EXTRA_USER_PIC_LARGE = "com.jldroid.twook.USER_PIC_LARGE";
	
	private static final int[] TABS_FACEBOOK = {R.string.tab_info, R.string.tab_updates, R.string.tab_pictures};
	private static final int[] TABS_TWITTER = {R.string.tab_info, R.string.tab_timeline};
 	
	private int[] mTabs;
	
	protected IAccount mAccount;
	protected User mUser;
	
	private ViewPager mViewPager;
	
	private View mInfoView;
	private ColumnView mUpdatesView;
	private AlbumsView mPhotosView;
	
	// info view
	private ScrollView mInfoScrollView;
	private ProgressBar mInfoPB;
	private ImageView mInfoProfileView;
	private TextView mInfoNameTV;
	
	private MyAdapter mAdapter;
	
	private View[] mViews;
	
	protected Bitmap mProfileBmd;
	
	private MenuItem mChangeRelationMenuItem;
	private int mIsFriend = -1;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAccount = AccountsManager.getInstance(getActivity()).findAccount(getArguments().getLong(EXTRA_ACCOUNT));
		mUser = mAccount.findUser(getArguments().getLong(EXTRA_USER_ID));
		if (mUser == null) {
			mUser = new User(mAccount instanceof FacebookAccount ? User.TYPE_FACEBOOK : User.TYPE_TWITTER, getArguments().getLong(EXTRA_USER_ID), getArguments().getString(EXTRA_USER_NAME));
			mUser.profilePictureUrl = getArguments().getString(EXTRA_USER_PIC);
			mUser.largeProfilePictureUrl = getArguments().getString(EXTRA_USER_PIC_LARGE);
		}
		mTabs = mAccount instanceof FacebookAccount ? TABS_FACEBOOK : TABS_TWITTER;
	}
	
	@Override
	public View onCreateView(LayoutInflater pInflater, ViewGroup pContainer, Bundle pSavedInstanceState) {
		View v = pInflater.inflate(R.layout.profileview, null);
		mViewPager = (ViewPager) v.findViewById(R.id.viewpager);
		
		mInfoView = pInflater.inflate(R.layout.viewprofile_info, null);
		mUpdatesView = new ColumnView(getActivity());
		mPhotosView = mAccount instanceof FacebookAccount ? new AlbumsView(getActivity()) : null;
		
		mViews = new View[] {mInfoView, mUpdatesView, mPhotosView};
		
		mInfoScrollView = (ScrollView) mInfoView.findViewById(R.id.scrollView);
		mInfoPB = (ProgressBar) mInfoView.findViewById(R.id.progressBar);
		mInfoProfileView = (ImageView) mInfoView.findViewById(R.id.profileIV);
		mInfoNameTV = (TextView) mInfoView.findViewById(R.id.nameTV);
		
		return v;
	}
	@Override
	public void onActivityCreated(Bundle pSavedInstanceState) {
		super.onActivityCreated(pSavedInstanceState);
		setHasOptionsMenu(true);
		
		mAdapter = new MyAdapter();
		mViewPager.setAdapter(mAdapter);
		
		mInfoScrollView.setVisibility(mUser.hasDetailedInfo() ? View.VISIBLE : View.GONE);
		mInfoPB.setVisibility(mUser.hasDetailedInfo() ? View.GONE : View.VISIBLE);
		
		mViewPager.setOnPageChangeListener(this);
		mViewPager.setCurrentItem(0);
		
		mUpdatesView.setProvider(mAccount.addUserProvider(mUser), true);
		
		if (mAccount instanceof FacebookAccount) mPhotosView.setUser((FacebookAccount) mAccount, mUser); // this will automaticly trigger an update...
		
		ImageManager im = ImageManager.getInstance(getActivity());
		im.loadImage(new LoadBitmapCallback() {
			@Override
			public void onFailed(String pUri) {
			}
			@Override
			public void onBitmapLoaded(String pUri, Bitmap pBmd) {
				mProfileBmd = pBmd;
				Threads.runOnUIThread(new Runnable() {
					@Override
					public void run() {
						mInfoProfileView.setImageBitmap(mProfileBmd);
						mInfoProfileView.setOnClickListener(ViewProfileFragment.this);
					}
				});
			}
		}, mUser.largeProfilePictureUrl != null ? mUser.largeProfilePictureUrl : mUser.profilePictureUrl, DeletionTrigger.IMMEDIATELY, ImageManager.REF_SOFT, (int) (getResources().getDisplayMetrics().density * 100), -1, 80);
		
		mInfoNameTV.setText(mUser.name);
		
		if (mUser.type == User.TYPE_TWITTER) {
			Threads.runOnNetworkThread(new Runnable() {
				@Override
				public void run() {
					TwitterAccount ta = (TwitterAccount) mAccount;
					try {
						final boolean isFollowing = ta.getTwitter().existsFriendship(String.valueOf(mAccount.getUser().id), String.valueOf(mUser.id));
						Threads.runOnUIThread(new Runnable() {
							@Override
							public void run() {
								mIsFriend = isFollowing ? 1 : 0;
								updateChangeRelationItem();
							}
						});
					} catch (TwitterException e) {
						e.printStackTrace();
					}
				}
			});
		} else if (mUser.type == User.TYPE_FACEBOOK){
			
		}
		
		if (mUser.hasDetailedInfo()) {
			populateInfo();
		} else {
			loadInfo();
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		getSherlockActivity().getSupportActionBar().setTitle(mUser.name);
		getSherlockActivity().getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		int selected = getSherlockActivity().getSupportActionBar().getSelectedNavigationIndex();
		
		for (int i = 0; i < mTabs.length; i++) {
			getSherlockActivity().getSupportActionBar().addTab(getSherlockActivity().getSupportActionBar().newTab().setText(mTabs[i]).setTabListener(this));
		}
		if (selected >= 0 && selected < mTabs.length) {
			getSherlockActivity().getSupportActionBar().setSelectedNavigationItem(selected);
		}
	}
	
	@Override
	public void onStop() {
		super.onStop();
		getSherlockActivity().getSupportActionBar().removeAllTabs();
		getSherlockActivity().getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mAccount.removeUserProvider(mUpdatesView.getProvider());
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
	
	@Override
	public void onCreateOptionsMenu(Menu pMenu, MenuInflater pInflater) {
		super.onCreateOptionsMenu(pMenu, pInflater);
		mChangeRelationMenuItem = pMenu.add(Menu.NONE, 1, Menu.NONE, "");
		updateChangeRelationItem();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem pItem) {
		switch (pItem.getItemId()) {
		case 1:
			Threads.runOnNetworkThread(new Runnable() {
				public void run() {
					if (mUser.type == User.TYPE_TWITTER) {
						TwitterAccount ta = (TwitterAccount) mAccount;
						try {
							final twitter4j.User user = mIsFriend == 1 ? ta.getTwitter().destroyFriendship(mUser.id) : ta.getTwitter().createFriendship(mUser.id);
							Threads.runOnUIThread(new Runnable() {
								@Override
								public void run() {
									if (mIsFriend == 1) {
										mIsFriend = 0;
										Toast.makeText(getSherlockActivity().getApplicationContext(), getString(R.string.unfollowed, mUser.name), Toast.LENGTH_LONG).show();
									} else {
										mIsFriend = 1;
										Toast.makeText(getSherlockActivity().getApplicationContext(), getString(user.isFollowRequestSent() ? R.string.follow_requested : R.string.following, mUser.name), Toast.LENGTH_LONG).show();
									}
									updateChangeRelationItem();
								}
							});
						} catch (TwitterException e) {
							e.printStackTrace();
							Threads.runOnUIThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(getSherlockActivity().getApplicationContext(), getString(mIsFriend == 1 ? R.string.failed_unfollow : R.string.failed_follow, mUser.name), Toast.LENGTH_LONG).show();
								}
							});
						}
					}
				}
			});
			break;
		default:
			break;
		}
		return true;
	}
	
	private void updateChangeRelationItem() {
		if (mChangeRelationMenuItem != null) {
			mChangeRelationMenuItem.setVisible(mIsFriend != -1);
			if (mAccount instanceof FacebookAccount) {
				mChangeRelationMenuItem.setTitle(mIsFriend == 1 ? R.string.unfriend : R.string.send_friend_request);
			} else if (mAccount instanceof TwitterAccount) {
				mChangeRelationMenuItem.setTitle(mIsFriend == 1 ? R.string.unfollow : R.string.follow);
			}
		}
	}
	
	@Override
	public void onClick(View pV) {
		if (pV == mInfoProfileView) {
			// TODO ((MainActivity) getActivity()).showFragment(new ViewImageFragment(mProfileBmd));
		}
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
	}
	
	private void loadInfo() {
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				final User user = mAccount.loadUserInfo(mUser.id);
				Threads.runOnUIThread(new Runnable() {
					@Override
					public void run() {
						if (getActivity() == null) {
							return;
						}
						if (user != null) {
							mUser = user;
							mInfoScrollView.setVisibility(View.VISIBLE);
							mInfoPB.setVisibility(View.GONE);
							populateInfo();
						} else {
							Toast.makeText(getActivity(), R.string.failed_load_info, Toast.LENGTH_LONG).show(); // TODO show in textview
							mInfoPB.setVisibility(View.GONE);
						}
					}
				});
			}
		});
	}
	
	private void populateInfo() {
		populateTable(R.id.aboutInfoTable, "About " + mUser.name, mUser.aboutInfo);
		populateTable(R.id.basicInfoTable, "Basic Information", mUser.basicInfo);
		populateTable(R.id.aboutInfoTable, "Other Information", mUser.aboutInfo);
		populateTable(R.id.contactInfoTable, "Contact", mUser.contactInfo);
	}
	
	private void populateTable(int id, String title, String[] data) {
		View v = mInfoView.findViewById(id);
		TextView tv = (TextView) v.findViewById(R.id.titleTV);
		TableLayout table = (TableLayout) v.findViewById(R.id.table);
		tv.setText(title);
		table.removeAllViews();
		for (int i = 0; i < data.length; i += 2) {
			String key = data[i];
			String value = data[i + 1];
			TableRow row = new TableRow(getActivity());
			TextView keyTV = new TextView(getActivity());
			TextView valueTV = new TextView(getActivity());
			valueTV.setAutoLinkMask(Linkify.ALL);
			keyTV.setText(key);
			valueTV.setText(value);
			row.addView(keyTV);
			row.addView(valueTV);
			
			TableRow.LayoutParams params = (TableRow.LayoutParams) valueTV.getLayoutParams();
			params.weight = 1;
			params.width = 0;
			valueTV.setLayoutParams(params);
			
			table.addView(row);
			TableLayout.LayoutParams params2 = (TableLayout.LayoutParams) row.getLayoutParams();
			params2.width = LayoutParams.MATCH_PARENT;
			row.setLayoutParams(params2);
		}
	}
	
	private class MyAdapter extends PagerAdapter {

		@Override
		public int getCount() {
			return mAccount instanceof FacebookAccount ? 3 : 2;
		}

		@Override
		public Object instantiateItem(ViewGroup pContainer, int pPosition) {
			View v = mViews[pPosition];
			pContainer.addView(v);
			return v;
		}
		
		@Override
		public void destroyItem(ViewGroup pContainer, int pPosition, Object pObject) {
			pContainer.removeView((View) pObject);
		}
		
		@Override
		public boolean isViewFromObject(View view, Object obj) {
			return view == obj;
		}
		
	}
	
	
}
