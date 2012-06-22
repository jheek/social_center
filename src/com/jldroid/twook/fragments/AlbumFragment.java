package com.jldroid.twook.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.jdroid.utils.StorageManager.StorageBundle;
import com.jdroid.utils.Threads;
import com.jldroid.twook.R;
import com.jldroid.twook.activities.DetailsActivity;
import com.jldroid.twook.model.AccountsManager;
import com.jldroid.twook.model.ImageManager;
import com.jldroid.twook.model.facebook.Album;
import com.jldroid.twook.model.facebook.FacebookAccount;
import com.jldroid.twook.model.facebook.Photo;
import com.jldroid.twook.view.PhotoGridItemView;

public class AlbumFragment extends SherlockFragment implements OnItemClickListener {

	public static final String EXTRA_FB_ACCOUNT = "com.jldroid.twook.FB_ACCOUNT";
	public static final String EXTRA_ALBUM = "com.jldroid.twook.ALBUM";
	
	private FacebookAccount mFBAccount;
	private Album mAlbum;
	
	protected Photo[] mPhotos;
	
	private GridView mGridView;
	private ProgressBar mPB;
	
	private MyAdapter mAdapter;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mFBAccount = AccountsManager.getInstance(getActivity()).findFacebookAccountByID(getArguments().getLong(EXTRA_FB_ACCOUNT));
		mAlbum = new Album(StorageBundle.create(getArguments().getByteArray(EXTRA_ALBUM)));
	};
	
	@Override
	public View onCreateView(LayoutInflater pInflater, ViewGroup pContainer, Bundle pSavedInstanceState) {
		View v = pInflater.inflate(R.layout.album_grid, null);
		mGridView = (GridView) v.findViewById(R.id.gridView);
		mPB = (ProgressBar) v.findViewById(R.id.pb);
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle pSavedInstanceState) {
		super.onActivityCreated(pSavedInstanceState);
		getSherlockActivity().getSupportActionBar().setTitle(mAlbum.name);
		
		mGridView.setAdapter(mAdapter = new MyAdapter());
		
		float d = getActivity().getResources().getDisplayMetrics().density;
		
		mGridView.setColumnWidth((int) (d * 100));
		mGridView.setNumColumns(GridView.AUTO_FIT);
		mGridView.setStretchMode(GridView.STRETCH_SPACING);
		
		mGridView.setVerticalSpacing((int) (d * 5));
		
		mGridView.setOnItemClickListener(this);
		
		loadPhotos();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mPhotos != null) {
			ImageManager im = ImageManager.getInstance(getActivity());
			for (int i = 0; i < mPhotos.length; i++) {
				im.unloadImage(mPhotos[i].src);
			}
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> pParent, View pView, int pPosition, long pId) {
		getActivity().startActivity(new Intent(getActivity(), DetailsActivity.class)
			.putExtra(DetailsFragment.EXTRA_ACCOUNT, mFBAccount.getUser().id)
			.putExtra(DetailsFragment.EXTRA_PHOTO, mPhotos[pPosition].asBundle()));
	}
	
	private void loadPhotos() {
		setIsLoading(true);
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				final Photo[] photos = mFBAccount.loadAlbumPhotos(mAlbum);
				Threads.runOnUIThread(new Runnable() {
					@Override
					public void run() {
						if (photos != null) {
							setIsLoading(false);
							mPhotos = photos;
							mAdapter.notifyDataSetChanged();
						} else {
							Toast.makeText(getActivity().getApplicationContext(), R.string.failed_load_photos, Toast.LENGTH_LONG).show();
						}
					}
				});
			}
		});
	}
	
	private void setIsLoading(boolean v) {
		mGridView.setVisibility(v ? View.INVISIBLE : View.VISIBLE);
		mPB.setVisibility(v ? View.VISIBLE : View.GONE);
	}
	
	private class MyAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mPhotos != null ? mPhotos.length : 0;
		}

		@Override
		public Photo getItem(int pPosition) {
			return mPhotos[pPosition];
		}

		@Override
		public long getItemId(int pPosition) {
			return getItem(pPosition).hashCode();
		}

		@Override
		public View getView(int pPosition, View pConvertView, ViewGroup pParent) {
			Photo photo = getItem(pPosition);
			PhotoGridItemView v = (PhotoGridItemView) pConvertView;
			if (v == null) {
				v = new PhotoGridItemView(getActivity());
				float d = getActivity().getResources().getDisplayMetrics().density;
				v.setLayoutParams(new AbsListView.LayoutParams((int) (d * 100), (int) (d * 100)));
			}
			v.setPhoto(photo);
			return v;
		}
		
	}
	
}
