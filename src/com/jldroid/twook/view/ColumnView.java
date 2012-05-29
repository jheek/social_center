package com.jldroid.twook.view;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jdroid.utils.StorageManager;
import com.jdroid.utils.Threads;
import com.jldroid.twook.R;
import com.jldroid.twook.activities.ChatActivity;
import com.jldroid.twook.activities.ChatsActivity;
import com.jldroid.twook.activities.DetailsActivity;
import com.jldroid.twook.activities.MainActivity;
import com.jldroid.twook.fragments.ChatFragment;
import com.jldroid.twook.fragments.DetailsFragment;
import com.jldroid.twook.model.ColumnInfo;
import com.jldroid.twook.model.ColumnMessagesProvider;
import com.jldroid.twook.model.ColumnProviderListener;
import com.jldroid.twook.model.Message;

public class ColumnView extends RelativeLayout implements ColumnProviderListener, OnScrollListener {
	
	private MyAdpater mAdapter;
	
	protected ColumnInfo mInfo;
	protected ColumnMessagesProvider mProvider;
	
	protected ListView mListView;
	
	protected View mEmptyView;
	protected ProgressBar mEmptyPB;
	protected TextView mEmptyTV;
	
	protected RelativeLayout mOlderView;
	protected ProgressBar mLoadingOlderPB;
	
	private boolean mShowPreviews;
	private boolean mFancyAnims;
	
	public ColumnView(Context c) {
		super(c);
		init(c);
	}
	
	public ColumnView(Context pContext, AttributeSet pAttrs) {
		super(pContext, pAttrs);
		init(pContext);
	}
	
	public ColumnView(Context pContext, AttributeSet pAttrs, int pDefStyle) {
		super(pContext, pAttrs, pDefStyle);
		init(pContext);
	}
	
	private void init(Context c) {
		LayoutInflater.from(getContext()).inflate(R.layout.column, this);
		mListView = (ListView) findViewById(R.id.listView);
		mEmptyView = findViewById(R.id.emptyHolder);
		mEmptyPB = (ProgressBar) findViewById(R.id.emptyPB);
		mEmptyTV = (TextView) findViewById(R.id.emptyTV);
		
		float density = getResources().getDisplayMetrics().density;
		int padding = (int) (density * 5);
		
		mListView.setEmptyView(mEmptyView);
		
		mOlderView = new RelativeLayout(c);
		mLoadingOlderPB = new ProgressBar(c);
		mLoadingOlderPB.setIndeterminate(true);
		mOlderView.addView(mLoadingOlderPB);
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mLoadingOlderPB.getLayoutParams();
		params.addRule(RelativeLayout.CENTER_HORIZONTAL);
		params.width = params.height = (int) (density * 24f);
		mLoadingOlderPB.setPadding(padding, padding, padding, padding);
		mListView.addFooterView(mOlderView);
		
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> pArg0, View pArg1, int pArg2, long pArg3) {
				showDetails(mProvider.getMessage(pArg2));
			}
		});
		
		mListView.setItemsCanFocus(false);
		mListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_DISABLED);
		
		mAdapter = new MyAdpater();
		mListView.setAdapter(mAdapter);
		mListView.setOnScrollListener(this);
		
		onStart();
	}
	
	private Runnable mNotifyDataSetChangedRunnable = new Runnable() {
		@Override
		public void run() {
			notifyDataSetChanged();
		}
	};
	
	private void notifyDataSetChanged() {
		mAdapter.notifyDataSetChanged();
		restoreLastReadMessage();
	}
	
	public void onStart() {
		if (mProvider != null) {
			mProvider.addListener(this);
			onUpdateStateChanged(mProvider.isUpdating());
			notifyDataSetChanged();
		}
		if (!isInEditMode()) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
			mShowPreviews = prefs.getBoolean("showPreviews", true);
			mFancyAnims = prefs.getBoolean("fancyAnimations", false);
		}
		restoreLastReadMessage();
	}
	
	public void onStop() {
		if (mProvider != null) {
			mProvider.removeListener(this);
		}
		updateLastReadMessage();
	}
	
	@Override
	public void onScroll(AbsListView pView, int pFirstVisibleItem, int pVisibleItemCount, int pTotalItemCount) {
		updateLastReadMessage();
	}
	
	@Override
	public void onScrollStateChanged(AbsListView pView, int pScrollState) {
	}
	
	private void updateLastReadMessage() {
		if (mInfo != null) {
			int pos = mListView.getFirstVisiblePosition();
			if (pos >= 0 && pos < mProvider.getMessageCount()) {
				Message msg = mProvider.getMessage(pos);
				View firstView = mListView.getChildAt(0);
				mInfo.setLastReadMessage(msg.ID, msg.type, firstView != null ? firstView.getTop() : 0);
			}
		}
	}
	
	private void restoreLastReadMessage() {
		if (mInfo != null) {
			long id = mInfo.getLastReadMessageID();
			int type = mInfo.getLastReadMessageType();
			int top = mInfo.getLastReadMessageTop();
			if (type != -1) {
				final int l = mProvider.getMessageCount();
				for (int i = 0; i < l; i++) {
					Message msg = mProvider.getMessage(i);
					if (msg.ID == id && msg.type == type) {
						mListView.setSelectionFromTop(i, top);
						break;
					}
				}
			}
		}
	}
	
	@Override
	public void setEnabled(boolean pEnabled) {
		super.setEnabled(pEnabled);
		mListView.setEnabled(pEnabled);
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent pEv) {
		return !isEnabled();
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent pEvent) {
		if (!isEnabled()) {
			return false;
		}
		return super.onTouchEvent(pEvent);
	}
	
	@Override
	public void onMessagesChanged() {
		Threads.runOnUIThread(mNotifyDataSetChangedRunnable);
	}

	@Override
	public void onHasOlderMessagesChanged(boolean pV) {
	}
	
	@Override
	public void onUpdateStateChanged(final boolean pIsUpdating) {
		Threads.runOnUIThread(new Runnable() {
			@Override
			public void run() {
				mEmptyPB.setVisibility(pIsUpdating ? VISIBLE : GONE);
				mEmptyTV.setText(pIsUpdating ? null : "No updates available");
			}
		});
	}
	
	@Override
	public void onEnlargingStateChanged(final boolean pIsEnlarging) {
		Threads.runOnUIThread(new Runnable() {
			@Override
			public void run() {
				mLoadingOlderPB.setVisibility(pIsEnlarging ? VISIBLE : GONE);
			}
		});
	}
	
	protected void showDetails(final Message msg) {
		if (msg.isChat()) {
			getContext().startActivity(new Intent(getContext(), ChatActivity.class)
					.putExtra(ChatFragment.EXTRA_CHAT_MSG, Message.createMessageBundle(null, msg)));
		} else if (msg.isDetailsMessageLocalAvailable()) {
			startDetails(msg.getDetailsMessage());
		} else {
			final ProgressDialog dialog = ProgressDialog.show(getContext(), "Loading original message", "Please wait a moment...", true, true);
			Threads.runOnNetworkThread(new Runnable() {
				@Override
				public void run() {
					if (dialog.isShowing()) {
						dialog.dismiss();
						final Message detailsMsg = msg.getDetailsMessage();
						Threads.runOnUIThread(new Runnable() {
							@Override
							public void run() {
								if (detailsMsg != null) {
									startDetails(detailsMsg);
								} else {
									Toast.makeText(getContext().getApplicationContext(), "Failed to load details", Toast.LENGTH_LONG).show();
								}
							}
						});
					}
				}
			});
		}
	}
	
	protected void startDetails(final Message msg) {
		getContext().startActivity(new Intent(getContext(), DetailsActivity.class)
				.putExtra(DetailsFragment.EXTRA_MSG, Message.createMessageBundle(null, msg)));
	}

	public String getName() {
		return mProvider.getName(getContext());
	}
	
	public ColumnMessagesProvider getProvider() {
		return mProvider;
	}
	
	public ColumnInfo getInfo() {
		return mInfo;
	}
	
	public void setProvider(ColumnInfo info) {
		setProvider(info, true);
	}
	
	
	
	public void setProvider(ColumnInfo info, boolean restorePos) {
		mInfo = info;
		setProvider(info.getProvider(), restorePos);
	}
	
	public void setProvider(ColumnMessagesProvider provider, boolean restorePos) {
		if (mProvider != null) {
			mProvider.removeListener(this);
		}
		mProvider = provider;
		mProvider.addListener(this);
		onUpdateStateChanged(mProvider.isUpdating());
		notifyDataSetChanged();
		if (mProvider.getMessageCount() == 0) {
			mProvider.requestUpdate(null);
		} else if (restorePos) {
			restoreLastReadMessage();
		}
	}
	
	public void scrollToTop() {
		mListView.setSelection(0);
	}
	
	private class MyAdpater extends BaseAdapter {

		@Override
		public int getCount() {
			return mProvider != null ? mProvider.getMessageCount() : 0;
		}

		@Override
		public Message getItem(int position) {
			return mProvider.getMessage(position);
		}

		@Override
		public long getItemId(int position) {
			return getItem(position).ID;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (position == getCount() - 1) {
				if (mProvider.hasOlderMessages() && !mProvider.isEnlarging()) {
					mProvider.requestOlderMessages(null);
				}
			}
			
			Message msg = getItem(position);
			MessageViewHolder holder = convertView != null ? (MessageViewHolder) convertView.getTag() : null;
			if (holder == null) {
				holder = new MessageViewHolder(getContext());
				
			}
			holder.setShowPreviews(mShowPreviews);
			holder.setMessage(msg);
			if (mFancyAnims) {
				AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
				alphaAnimation.setDuration(200);
				holder.getView().startAnimation(alphaAnimation);
			}
			return holder.getView();
		}
		
		@Override
		public boolean hasStableIds() {
			return true;
		}
		
	}
	
}
