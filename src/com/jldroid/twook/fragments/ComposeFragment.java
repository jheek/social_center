package com.jldroid.twook.fragments;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.jldroid.twook.R;
import com.jldroid.twook.activities.MainActivity;
import com.jldroid.twook.model.IAccount;
import com.jldroid.twook.model.PendingPost;
import com.jldroid.twook.model.PendingPostManager;
import com.jldroid.twook.model.User;
import com.jldroid.twook.model.facebook.FacebookAccount;
import com.jldroid.twook.model.twitter.TwitterAccount;
import com.jldroid.twook.view.AccountTabsView;
import com.jldroid.twook.view.UserAdapter;
import java.util.ArrayList;
import com.jdroid.utils.SortedArrayList;

public class ComposeFragment extends SherlockFragment {

	private static final int CHARACTERS_LEFT_GOOD_COLOR = 0xFF669900;
	private static final int CHARACTERS_LEFT_NORMAL_COLOR = 0xFFF88017;
	private static final int CHARACTERS_LEFT_BAD_COLOR = 0xFFE41B17;
	
	private AccountTabsView mAccountTabs;
	private AutoCompleteTextView mUpdateText;
	
	private LinearLayout mAttachmantsHolder;
	
	private ImageAttachmentView mImageAttachment;
	
	private ComposeConfig mConfig;
	
	private MenuItem mAddAttachmentItem;
	
	public ComposeFragment() {
		this(new ComposeConfig(ComposeMode.STATUS_UPDATE));
	}
	
	public ComposeFragment(ComposeConfig config) {
		mConfig = config;
	}
	
	@Override
	public View onCreateView(LayoutInflater pInflater, ViewGroup pContainer, Bundle pSavedInstanceState) {
		View v = pInflater.inflate(R.layout.compose, null);
		mUpdateText = (AutoCompleteTextView) v.findViewById(R.id.updateText);
		mAttachmantsHolder = (LinearLayout) v.findViewById(R.id.attachmentHolder);
		mAccountTabs = (AccountTabsView) v.findViewById(R.id.accountTabs);
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle pSavedInstanceState) {
		super.onActivityCreated(pSavedInstanceState);
		setHasOptionsMenu(true);
		
		mUpdateText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence pS, int pStart, int pBefore, int pCount) {
			}
			
			@Override
			public void beforeTextChanged(CharSequence pS, int pStart, int pCount, int pAfter) {
			}
			
			@Override
			public void afterTextChanged(Editable pS) {
				updateCharactersLeft();
			}
		});
		mUpdateText.setAdapter(new MyAdapter());
		mUpdateText.setThreshold(1);
		
		mAccountTabs.setAccounts(mConfig.mode.isTwitterAllowed(), mConfig.mode.isFacebookAllowed());
		mAccountTabs.setSelectedAccountsChangedRunnable(new Runnable() {
			@Override
			public void run() {
				updateCharactersLeft();
			}
		});
		
		setImage(mConfig.imgPath);
		mUpdateText.setText(mConfig.template);
		
		switch (mConfig.mode) {
		case FACEBOOK_WALL_POST:
			break;
		case STATUS_UPDATE:
			break;
		case TWITTER_REPLY:
			mUpdateText.setText('@' + mConfig.targetUser.twitterScreenName + ' ' + mUpdateText.getText().toString());
			break;
		case TWITTER_RETWEET:
			break;
		default:
			break;
		}
		
		if (!mConfig.mode.isTextAllowed()) {
			mUpdateText.setEnabled(false);
			mAccountTabs.hideCharactersLeft();
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		getSherlockActivity().getSupportActionBar().setTitle(R.string.compose);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu pMenu, MenuInflater pInflater) {
		super.onCreateOptionsMenu(pMenu, pInflater);
		if (mConfig.mode.isTextAllowed()) {
			mAddAttachmentItem = pMenu.add(Menu.NONE, 1, Menu.NONE, R.string.add_attachment).setIcon(R.drawable.actionbar_add_attachment);
			mAddAttachmentItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			mAddAttachmentItem.setVisible(mImageAttachment == null);
		}
		pMenu.add(Menu.NONE, 2, Menu.NONE, R.string.post).setIcon(R.drawable.actionbar_send).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem pItem) {
		switch (pItem.getItemId()) {
		case 1: // add attachment
			addPicture();
			break;
		case 2: // post
			updateStatus();
			break;
		default:
			break;
		}
		return true;
	}
	
	private void addPicture() {
		Intent intent = new Intent(Intent.ACTION_PICK);
		intent.setType("image/*");
		((getActivity())).startActivityForResult(intent, MainActivity.REQUEST_CODE_COMPOSE_PICK_IMAGE);
	}
	
	public void onPickImageResult(int resultCode, Intent pData) {
		if (resultCode == Activity.RESULT_OK) {
			Uri data = pData.getData();
			Cursor cursor = Media.query(getActivity().getContentResolver(), data, null);
			cursor.moveToFirst();
			String imgData = cursor.getString(cursor.getColumnIndex(Media.DATA));
			cursor.close();
			
			setImage(imgData);
		}
	}
	
	private void updateStatus() {
		String update = mUpdateText.getText().toString();
		String imgPath = mImageAttachment != null ? mImageAttachment.mPath : null;
		int max = getMaxCharacterCount();
		if (update.length() > max && max != -1) {
			Toast.makeText(getActivity().getApplicationContext(), "You're message contains too many characters!", Toast.LENGTH_LONG).show();
			return;
		}
		if (!TextUtils.isEmpty(update) || !TextUtils.isEmpty(imgPath)) {
			PendingPostManager ppm = PendingPostManager.getInstance(getActivity());
			ArrayList<IAccount> accounts = mAccountTabs.getSelectedAccounts();
			for (int i = 0; i < accounts.size(); i++) {
				IAccount account = accounts.get(i);
				if (account instanceof TwitterAccount) {
					TwitterAccount ta = (TwitterAccount) account;
					PendingPost post = null;
					switch (mConfig.mode) {
					case STATUS_UPDATE:
						post = new PendingPost(PendingPost.TYPE_TWITTER_STATUS_UPDATE, update);
						post.twitterAccount = ta;
						post.imgPath = imgPath;
						break;
					case TWITTER_RETWEET:
						post = new PendingPost(PendingPost.TYPE_TWITTER_RETWEET, null);
						post.twitterAccount = ta;
						post.twitterTargetID = mConfig.targetID;
						break;
					case TWITTER_REPLY:
						post = new PendingPost(PendingPost.TYPE_TWITTER_STATUS_UPDATE, update);
						post.twitterAccount = ta;
						post.imgPath = imgPath;
						post.twitterTargetID = mConfig.targetID;
						break;
					default:
						break;
					}
					ppm.add(post);
				} else if (account instanceof FacebookAccount) {
					FacebookAccount fa = (FacebookAccount) account;
					PendingPost post = new PendingPost(mConfig.mode == ComposeMode.FACEBOOK_WALL_POST ? 
							PendingPost.TYPE_FACEBOOK_WALL_POST : PendingPost.TYPE_FACEBOOK_STATUS_UPDATE, update);
					post.facebookAccount = fa;
					post.imgPath = imgPath;
					if (mConfig.mode == ComposeMode.FACEBOOK_WALL_POST) {
						post.facebookTargetID = mConfig.targetID;
					}
					ppm.add(post);
				}
			}
			MainActivity a = (MainActivity) getActivity();
			a.onBackPressed();
		} else {
			Toast.makeText(getActivity().getSherlockActivity().getApplicationContext(), R.string.update_cannot_be_empty, Toast.LENGTH_LONG).show();
		}
	}
	
	private void setImage(final String path) {
		if (mAddAttachmentItem != null) mAddAttachmentItem.setVisible(path == null);
		if (path != null) {
			mImageAttachment = new ImageAttachmentView(getActivity(), path);
			mAttachmantsHolder.addView(mImageAttachment);
		} else {
			mAttachmantsHolder.removeView(mImageAttachment);
			mImageAttachment = null;
		}
	}
	
	private int getMaxCharacterCount() {
		int max = -1;
		ArrayList<IAccount> accounts = mAccountTabs.getSelectedAccounts();
		for (int i = 0; i < accounts.size(); i++) {
			IAccount account = accounts.get(i);
			if (account instanceof TwitterAccount) {
				max = mImageAttachment != null ? 140 - 25 : 140;
				break;
			}
		}
		return max;
	}
	
	protected void updateCharactersLeft() {
		if (mConfig.mode == ComposeMode.TWITTER_RETWEET) {
			return;
		}
		int max = getMaxCharacterCount();
		if (max == -1) {
			mAccountTabs.hideCharactersLeft();
		} else {
			int left = max - mUpdateText.getEditableText().length();
			mAccountTabs.showCharactersLeft(Integer.toString(left));
			int c;
			if (left > 30) {
				c = CHARACTERS_LEFT_GOOD_COLOR;
			} else if (left >= 0) {
				c = CHARACTERS_LEFT_NORMAL_COLOR;
			} else {
				c = CHARACTERS_LEFT_BAD_COLOR;
			}
			mAccountTabs.setCharactersLeftColor(c);
		}
	}
	
	private class ImageAttachmentView extends RelativeLayout {
		
		private ImageView mPreviewIV;
		private ImageView mRemoveBtn;
		
		private TextView mNameTV;
		private TextView mInfoTV;
		
		private String mPath;
		private File mFile;
		
		public ImageAttachmentView(Context c, String path) {
			super(c);
			LayoutInflater.from(c).inflate(R.layout.image_attachment, this);
			mPreviewIV = (ImageView) findViewById(R.id.previewIV);
			mRemoveBtn = (ImageView) findViewById(R.id.removeBtn);
			mNameTV = (TextView) findViewById(R.id.titleTV);
			mInfoTV = (TextView) findViewById(R.id.infoTV);
			
			mPath = path;
			mFile = new File(path);
			
			mNameTV.setText(mFile.getName());
			mInfoTV.setText((int) (mFile.length() / 1024) + "Kb");
			
			int size = (int) (getResources().getDisplayMetrics().density * 72);
			
			Bitmap bmd = BitmapFactory.decodeFile(path);
			Bitmap thumbnail = ThumbnailUtils.extractThumbnail(bmd, size, size);
			bmd.recycle();
			
			mPreviewIV.setImageBitmap(thumbnail);
			
			mRemoveBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View pV) {
					setImage(null);
				}
			});
		}
	}

	
	
	public static class ComposeConfig {
		public ComposeMode mode;
		
		public long targetID;
		public User targetUser;
		
		public String template;
		public String imgPath;
		
		public ComposeConfig(ComposeMode mode) {
			this(mode, -1, null, null);
		}
		
		public ComposeConfig(ComposeMode mode, long targetID, User targetUser) {
			this(mode, targetID, targetUser, null);
		}
		
		public ComposeConfig(ComposeMode mode, String imgPath) {
			this(mode, -1, null, imgPath);
		}
		
		public ComposeConfig(ComposeMode mode, long targetID, User targetUser, String imgPath) {
			this.mode = mode;
			this.targetID = targetID;
			this.targetUser = targetUser;
			this.imgPath = imgPath;
		}
	}
	
	public static enum ComposeMode {
		STATUS_UPDATE(true, true, true),
		TWITTER_REPLY(true, false, true),
		TWITTER_RETWEET(true, false, false),
		FACEBOOK_WALL_POST(false, true, true);
		
		private boolean isFacebookAllowed;
		private boolean isTwitterAllowed;
		private boolean isTextAllowed;
		
		private ComposeMode(boolean twitterAllowed, boolean facebookAllowed, boolean allowText) {
			isTwitterAllowed = twitterAllowed;
			isFacebookAllowed = facebookAllowed;
			isTextAllowed = allowText;
		}
		
		public boolean isFacebookAllowed() {
			return isFacebookAllowed;
		}
		
		public boolean isTwitterAllowed() {
			return isTwitterAllowed;
		}
		
		public boolean isTextAllowed() {
			return isTextAllowed;
		}
	}
	
	private class MyAdapter extends UserAdapter implements Filterable {
		
		public MyAdapter() {
			super(getActivity(), new SortedArrayList<User>());
		}
		
		private Filter mFilter = new Filter() {
			
			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence pConstraint, FilterResults pResults) {
				mUsers = (SortedArrayList<User>) pResults.values;
				notifyDataSetChanged();
			}
			
			@Override
			protected FilterResults performFiltering(CharSequence pConstraint) {
				SortedArrayList<User> items = new SortedArrayList<User>();
				ArrayList<IAccount> accounts = mAccountTabs.getSelectedAccounts();
				String filter = pConstraint != null ? pConstraint.toString().toLowerCase() : "";
				for (int i = 0; i < accounts.size(); i++) {
					IAccount account = accounts.get(i);
					if (account instanceof TwitterAccount) {
						if (filter.startsWith("@")) {
							filter = filter.substring(1);
							TwitterAccount ta = (TwitterAccount) account;
							ArrayList<User> followers = ta.getFollowers();
							ArrayList<User> following = ta.getFollowing();
							for (int i2 = followers.size() - 1; i2 >= 0; i2--) {
								User user = followers.get(i2);
								if (user.name.toLowerCase().startsWith(filter) && !items.contains(user)) {
									items.add(user);
								}
							}
							for (int i2 = following.size() - 1; i2 >= 0; i2--) {
								User user = following.get(i2);
								if (user.name.toLowerCase().startsWith(filter) && !items.contains(user)) {
									items.add(user);
								}
							}
						}
					} else if (account instanceof FacebookAccount) {
						if (filter.length() > 3) {
							FacebookAccount fa = (FacebookAccount) account;
							ArrayList<User> friends = fa.getFriends();
							for (int i2 = friends.size() - 1; i2 >= 0; i2--) {
								User user = friends.get(i2);
								if (user.name.toLowerCase().startsWith(filter) && !items.contains(user)) {
									items.add(user);
								}
							}
						}
					}
				}
				FilterResults r = new FilterResults();
				r.values = items;
				r.count = items.size();
				return r;
			}
		};

		@Override
		public Filter getFilter() {
			return mFilter;
		}
		
	}
	
}
