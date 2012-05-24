package com.jldroid.twook.view;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.jdroid.utils.Threads;
import com.jldroid.twook.R;
import com.jldroid.twook.activities.AlbumActivity;
import com.jldroid.twook.activities.MainActivity;
import com.jldroid.twook.fragments.AlbumFragment;
import com.jldroid.twook.model.User;
import com.jldroid.twook.model.facebook.Album;
import com.jldroid.twook.model.facebook.FacebookAccount;

public class AlbumsView extends RelativeLayout implements OnItemClickListener {

	private ListView mListView;
	private ProgressBar mPB;
	
	private FacebookAccount mFBAccount;
	private User mUser;
	protected Album[] mAlbums;
	
	private MyAdapter mAdapter;
	
	public AlbumsView(Context pContext) {
		super(pContext);
		LayoutInflater.from(pContext).inflate(R.layout.albumns_list, this);
		mListView = (ListView) findViewById(R.id.listView);
		mPB = (ProgressBar) findViewById(R.id.pb);
		setIsLoading(true);
		
		mAdapter = new MyAdapter();
		mListView.setAdapter(mAdapter);
		
		mListView.setOnItemClickListener(this);
	}
	
	@Override
	public void onItemClick(AdapterView<?> pParent, View pView, int pPosition, long pId) {
		getContext().startActivity(new Intent(getContext(), AlbumActivity.class)
				.putExtra(AlbumFragment.EXTRA_FB_ACCOUNT, mFBAccount.getUser().id)
				.putExtra(AlbumFragment.EXTRA_ALBUM, mAlbums[pPosition].updateBundle().getBytes()));
	}

	private void setIsLoading(boolean v) {
		mListView.setVisibility(v ? INVISIBLE : VISIBLE);
		mPB.setVisibility(v ? VISIBLE : GONE);
	}
	
	public void setUser(FacebookAccount pFacebookAccount, User pUser) {
		mFBAccount = pFacebookAccount;
		mUser = pUser;
		mAlbums = null;
		mAdapter.notifyDataSetChanged();
		loadUserAlbums();
	}

	private void loadUserAlbums() {
		setIsLoading(true);
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				final Album[] albums = mFBAccount.loadUserAlbums(mUser);
				Threads.runOnUIThread(new Runnable() {
					@Override
					public void run() {
						if (albums != null) {
							mAlbums = albums;
							mAdapter.notifyDataSetChanged();
							setIsLoading(false);
						} else {
							// TODO show text...
							Toast.makeText(getContext().getApplicationContext(), "Failed to load albums", Toast.LENGTH_LONG).show();
						}
					}
				});
			}
		});
	}
	
	private class MyAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mAlbums != null ? mAlbums.length : 0;
		}

		@Override
		public Album getItem(int pPosition) {
			return mAlbums[pPosition];
		}

		@Override
		public long getItemId(int pPosition) {
			return getItem(pPosition).hashCode();
		}

		@Override
		public View getView(int pPosition, View pConvertView, ViewGroup pParent) {
			Album album = getItem(pPosition);
			AlbumListItemView v = (AlbumListItemView) pConvertView;
			if (v == null) {
				v = new AlbumListItemView(getContext());
			}
			v.setAlbum(mFBAccount, album);
			return v;
		}
		
	}
	
}
