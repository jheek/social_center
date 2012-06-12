package com.jldroid.twook.fragments;

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
import com.jdroid.utils.SortedArrayList;
import com.jdroid.utils.Threads;
import com.jdroid.utils.TimeUtils;
import com.jldroid.twook.ChatUtils;
import com.jldroid.twook.FastBitmapDrawable;
import com.jldroid.twook.R;
import com.jldroid.twook.model.AccountsManager;
import com.jldroid.twook.model.Chat;
import com.jldroid.twook.model.Chat.ChatListener;
import com.jldroid.twook.model.ChatMessage;
import com.jldroid.twook.model.IAccount;
import com.jldroid.twook.model.ImageManager.LoadBitmapCallback;
import com.jldroid.twook.model.Message;
import com.jldroid.twook.model.facebook.FacebookAccount;
import com.jldroid.twook.model.twitter.TwitterAccount;
import com.jldroid.twook.view.ColoredFadeoutDrawable;
import com.jldroid.twook.view.ProfileImageDrawable;

public class ChatFragment extends SherlockFragment implements OnClickListener, ChatListener {

	public static final String EXTRA_CHAT_MSG = "com.jldroid.twook.CHAT_MSG";
	
	public static final String EXTRA_ACCOUNT = "com.jldroid.twook.ACCOUNT";
	public static final String EXTRA_CHAT = "com.jldroid.twook.CHAT";
	
	private static final int COLORED_FADEOUT_COLOR = 0xff008FD5;
	
	private ListView mListView;
	
	private EditText mMessageET;
	private ImageView mSendMsgBtn;
	
	protected Message mMsg;
	protected Chat mChat;
	
	protected MyAdapter mAdapter;
	
	protected MenuItem mRefreshItem;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments().containsKey(EXTRA_CHAT_MSG)) {
			mMsg = Message.findMessage(getActivity(), getArguments().getBundle(EXTRA_CHAT_MSG));
			if (mMsg != null) {
				mChat = mMsg.getChat();
			}
		} else {
			IAccount account = AccountsManager.getInstance(getActivity()).findAccount(getArguments().getLong(EXTRA_ACCOUNT));
			long chatID = getArguments().getLong(EXTRA_CHAT);
			SortedArrayList<Chat> chats = account.getChats();
			for (int i = chats.size() - 1; i >= 0; i--) {
				Chat chat = chats.get(i);
				if (chat.getID() == chatID) {
					mChat = chat;
					mMsg = account.findChatMessage(chat);
					break;
				}
			}
		}
		
		if (mChat == null) {
			getActivity().finish();
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
		getSherlockActivity().getSupportActionBar().setTitle(mMsg.getTitle(getActivity()));
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
