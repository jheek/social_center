package com.jldroid.twook.fragments;

import java.util.ArrayList;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.jdroid.utils.Threads;
import com.jdroid.utils.TimeUtils;
import com.jldroid.twook.ChatUtils;
import com.jldroid.twook.FastBitmapDrawable;
import com.jldroid.twook.R;
import com.jldroid.twook.model.Chat;
import com.jldroid.twook.model.Message;
import com.jldroid.twook.model.Chat.ChatListener;
import com.jldroid.twook.model.ChatMessage;
import com.jldroid.twook.model.ImageManager.LoadBitmapCallback;
import com.jldroid.twook.model.User;
import com.jldroid.twook.model.facebook.FacebookAccount;
import com.jldroid.twook.model.twitter.TwitterAccount;
import com.jldroid.twook.view.ColoredFadeoutDrawable;
import com.jldroid.twook.view.ProfileImageDrawable;

public class ChatFragment extends SherlockFragment implements OnClickListener, ChatListener {

	public static final String EXTRA_CHAT_MSG = "com.jldroid.twook.CHAT_MSG";
	
	private final int MAX_RECIPIENTS = 3;
	private static final int COLORED_FADEOUT_COLOR = 0xff008FD5;
	
	private ListView mListView;
	
	private EditText mMessageET;
	private ImageView mSendMsgBtn;
	
	protected Chat mChat;
	
	protected MyAdapter mAdapter;
	
	protected MenuItem mRefreshItem;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Message msg = Message.findMessage(getActivity(), getArguments().getBundle(EXTRA_CHAT_MSG));
		if (msg != null) {
			mChat = msg.getChat();
		}
		if (mChat == null) {
			getActivity().onBackPressed();
			// TODO handle this someway...
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater pInflater, ViewGroup pContainer, Bundle pSavedInstanceState) {
		View v = pInflater.inflate(R.layout.chat, null);
		mListView = (ListView) v.findViewById(R.id.listView);
		mMessageET = (EditText) v.findViewById(R.id.messageET);
		mSendMsgBtn = (ImageView) v.findViewById(R.id.sendMessageBtn);
		
		mSendMsgBtn.setBackgroundDrawable(new ColoredFadeoutDrawable(COLORED_FADEOUT_COLOR));
		mListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle pSavedInstanceState) {
		super.onActivityCreated(pSavedInstanceState);
		
		setHasOptionsMenu(true);
		
		if (mChat.isDirty()) {
			refresh();
		}
		mListView.setAdapter(mAdapter = new MyAdapter());
		mListView.setSelection(mAdapter.getCount() - 1);
		
		mSendMsgBtn.setOnClickListener(this);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu pMenu, MenuInflater pInflater) {
		super.onCreateOptionsMenu(pMenu, pInflater);
		if (mChat.getAccount() instanceof FacebookAccount) {
			mRefreshItem = pMenu.add(Menu.NONE, 1, Menu.NONE, "Refresh");
			mRefreshItem.setIcon(R.drawable.actionbar_refresh);
			mRefreshItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem pItem) {
		switch (pItem.getItemId()) {
		case 1: // refresh
			refresh();
			break;
		default:
			break;
		}
		return true;
	}
	
	protected void refresh() {
		 mRefreshItem.setActionView(R.layout.ab_progressbar);
		 Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				final boolean result = ((FacebookAccount)mChat.getAccount()).updateChat(mChat, false);
				Threads.runOnUIThread(new Runnable() {
					@Override
					public void run() {
						mRefreshItem.setActionView(null);
						if (result) {
							mChat.setDirty(false);
							mListView.setSelection(mAdapter.getCount() - 1);
						} else {
							Toast.makeText(getSherlockActivity().getApplicationContext(), R.string.failed_update, Toast.LENGTH_LONG).show();
						}
					}
				});
			}
		});
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		ArrayList<User> recipients = mChat.getParticipants();
		StringBuilder sb = new StringBuilder();
		int count = 0;
		for (int i = 0; i < recipients.size(); i++) {
			if (recipients.get(i).id != mChat.getAccount().getUser().id) {
				if (count + 1 == MAX_RECIPIENTS) {
					sb.append(getString(R.string.summation_last));
				} else if (count != 0) {
					sb.append(getString(R.string.summation));
				}
				sb.append(recipients.get(i).name);
				count++;
				if (count + 1 == MAX_RECIPIENTS && recipients.size() - i > 2) {
					sb.append(getString(R.string.summation_last));
					int numOthers = 0;
					for (int i2 = i + 1; i2 < recipients.size(); i2++) {
						if (recipients.get(i2).id != mChat.getAccount().getUser().id) {
							numOthers++;
						}
					}
					sb.append(getString(R.string.summation_others, numOthers));
					break;
				} else if (count == MAX_RECIPIENTS) {
					break;
				}
			}
		}
		getSherlockActivity().getSupportActionBar().setTitle(sb.toString());
		mChat.addListener(this);
		mChat.setInForeground(true);
		ChatUtils.updateChatNotification(getActivity().getApplicationContext(), mChat);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		mChat.removeListener(this);
		mChat.setInForeground(false);
	}
	
	@Override
	public void onMessagesChanged(int pNewCount) {
		Threads.runOnUIThread(new Runnable() {
			@Override
			public void run() {
				mAdapter.notifyDataSetChanged();
			}
		});
	}
	
	@Override
	public void onClick(View pV) {
		if (pV == mSendMsgBtn) {
			((ColoredFadeoutDrawable) mSendMsgBtn.getBackground()).startFadeout();
			final String text = mMessageET.getText().toString();
			if (mChat.getAccount() instanceof TwitterAccount && text.length() > 140) {
				Toast.makeText(getSherlockActivity().getApplicationContext(), getString(R.string.msg_too_long, 140), Toast.LENGTH_LONG).show();
				return;
			} else if (text.length() == 0) {
				Toast.makeText(getSherlockActivity().getApplicationContext(), R.string.msg_cant_send_empty, Toast.LENGTH_LONG).show();
				return;
			}
			final ProgressDialog dialog = ProgressDialog.show(getActivity(), getString(R.string.msg_sending), getString(R.string.pb_msg_please_wait), true, false);
			Threads.runOnNetworkThread(new Runnable() {
				@Override
				public void run() {
					final boolean r = mChat.getAccount().sendChatMsg(mChat, text);
					dialog.dismiss();
					Threads.runOnUIThread(new Runnable() {
						@Override
						public void run() {
							if (r) {
								mMessageET.setText("");
							} else {
								Toast.makeText(getSherlockActivity().getApplicationContext(), R.string.failed_send_msg, Toast.LENGTH_LONG).show();
							}
						}
					});
				}
			});
		}
	}

	private class MyAdapter extends BaseAdapter {
		
		public MyAdapter() {
			
		}
		
		@Override
		public int getCount() {
			return mChat.getMessages().size();
		}

		@Override
		public ChatMessage getItem(int pPosition) {
			return mChat.getMessages().get(pPosition);
		}

		@Override
		public long getItemId(int pPosition) {
			return getItem(pPosition).ID;
		}

		@Override
		public View getView(int pPosition, View pConvertView, ViewGroup pParent) {
			ChatMessage msg = getItem(pPosition);
			View v = pConvertView;
			Holder h = null;
			if (v == null) {
				v = getActivity().getLayoutInflater().inflate(R.layout.chat_msg, null);
				h = new Holder();
				v.setTag(h);
				h.senderTV = (TextView) v.findViewById(R.id.senderTV);
				h.contentTV = (TextView) v.findViewById(R.id.contentTV);
				h.timeTV = (TextView) v.findViewById(R.id.timeTV);
				h.profileIV = v.findViewById(R.id.profileIV);
				h.profileDrawable = new ProfileImageDrawable(getActivity());
				h.profileIV.setBackgroundDrawable(h.profileDrawable);
			} else {
				h = (Holder) v.getTag();
			}
			h.profileDrawable.setUser(getActivity(), msg.sender, false);
			h.senderTV.setText(msg.sender.name);
			h.contentTV.setText(msg.text);
			h.timeTV.setText(TimeUtils.parseDuration(System.currentTimeMillis() - msg.time, false));
			return v;
		}
		
	}
	
	private static class Holder implements LoadBitmapCallback {
		
		public TextView senderTV;
		public TextView contentTV;
		public TextView timeTV;
		
		public View profileIV;
		
		public ProfileImageDrawable profileDrawable;
		
		public String currentProfileUri;
		
		@Override
		public void onBitmapLoaded(final String pUri, final Bitmap pBmd) {
			Threads.runOnUIThread(new Runnable() {
				@Override
				public void run() {
					if (currentProfileUri.equals(pUri)) {
						profileIV.setBackgroundDrawable(new FastBitmapDrawable(pBmd));
					}
				}
			});
		}
		
		@Override
		public void onFailed(String pUri) {
		}
	}
	
}
