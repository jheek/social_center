package com.jldroid.twook.fragments;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.jdroid.utils.StorageManager.StorageBundle;
import com.jdroid.utils.Threads;
import com.jdroid.utils.TimeUtils;
import com.jldroid.twook.R;
import com.jldroid.twook.activities.ComposeActivity;
import com.jldroid.twook.activities.DetailsActivity;
import com.jldroid.twook.activities.MainActivity;
import com.jldroid.twook.activities.ViewProfileActivity;
import com.jldroid.twook.fragments.ComposeFragment.ComposeConfig;
import com.jldroid.twook.fragments.ComposeFragment.ComposeMode;
import com.jldroid.twook.model.AccountsManager;
import com.jldroid.twook.model.IAccount;
import com.jldroid.twook.model.ImageManager;
import com.jldroid.twook.model.ImageManager.DeletionTrigger;
import com.jldroid.twook.model.ImageManager.LoadBitmapCallback;
import com.jldroid.twook.model.Message;
import com.jldroid.twook.model.User;
import com.jldroid.twook.model.facebook.Comment;
import com.jldroid.twook.model.facebook.FacebookAccount;
import com.jldroid.twook.model.facebook.FacebookImage;
import com.jldroid.twook.view.ColoredFadeoutDrawable;
import com.jldroid.twook.view.CommentView;
import com.jldroid.twook.view.ProfileImageDrawable;
import com.jldroid.twook.view.UserAdapter;

public class DetailsFragment extends SherlockFragment {

	public static final String EXTRA_MSG = "com.jldroid.twook.MSG";
	
	private static final int COLORED_FADEOUT_COLOR = 0xff008FD5;
	
	// views
	private ListView mListView;
	private LinearLayout mComposeCommentHolder;
	private EditText mCommentET;
	private ImageView mSendCommentBtn;
	
	// header
	private ViewGroup mHeader;
	private LinearLayout mMessageBox;
	private View mProfileIV;
	private TextView mSenderTV;
	private TextView mMsgTV;
	private TextView mInfoTV;
	
	private LinearLayout mAttachmentsHolder;
	private View mAttachmentsLoadingView;

	private ProfileImageDrawable mProfileDrawable;
	
	private Message mMessage;
	
	private MyAdapter mAdapter;
	
	private DetailsFragment mFragment;
	
	private MenuItem mRefreshItem;
	private MenuItem mLikeItem;
	private MenuItem mLikesItem;
	private MenuItem mCommentItem;
	
	@Override
	public void onCreate(Bundle pSavedInstanceState) {
		super.onCreate(pSavedInstanceState);
		mMessage = Message.findMessage(getActivity(), getArguments().getBundle(EXTRA_MSG));
		if (mMessage == null) {
			getActivity().finish();
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater pInflater, ViewGroup pContainer, Bundle pSavedInstanceState) {
		View v = pInflater.inflate(R.layout.details, null);
		View h = mHeader = (ViewGroup) pInflater.inflate(R.layout.details_header, null);
		//View f = mFooter = (ViewGroup) pInflater.inflate(R.layout.details_footer, null);
		
		mListView = (ListView) v.findViewById(R.id.listView);
		
		mProfileIV = h.findViewById(R.id.profileIV);
		mMsgTV = (TextView) h.findViewById(R.id.msgTV);
		mInfoTV = (TextView) h.findViewById(R.id.infoTV);
		
		mMessageBox = (LinearLayout) h.findViewById(R.id.messageBox);
		mAttachmentsHolder = (LinearLayout) h.findViewById(R.id.attachmentsHolder);
		mAttachmentsLoadingView = h.findViewById(R.id.attachmentsLoadingView);
		mSenderTV = (TextView) h.findViewById(R.id.senderTV);
		
		mComposeCommentHolder = (LinearLayout) v.findViewById(R.id.composeCommentHolder);
		mCommentET = (EditText) v.findViewById(R.id.commentET);
		mSendCommentBtn = (ImageView) v.findViewById(R.id.sendCommentBtn);
		
		mProfileDrawable = new ProfileImageDrawable(getActivity());
		mProfileIV.setBackgroundDrawable(mProfileDrawable);
		
		TypedArray a = getActivity().obtainStyledAttributes(null, R.styleable.DetailsView, R.attr.DetailsViewStyle, 0);
		/*mLikesBtn.setBackgroundDrawable(a.getDrawable(R.styleable.DetailsView_likesBackground));
		mLikesBtn.setTextColor(a.getColor(R.styleable.DetailsView_likesTextColor, -1));*/
		if (VERSION.SDK_INT < 11) {
			Drawable commentsBG = a.getDrawable(R.styleable.DetailsView_commentsBackground);
			if (commentsBG != null) {
				mListView.setSelector(commentsBG);
			}
		}
		a.recycle();
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle pSavedInstanceState) {
		super.onActivityCreated(pSavedInstanceState);
		setHasOptionsMenu(true);
		if (mMessage.sender != null) {
			mSenderTV.setText(mMessage.getTitle(getActivity()));
		} else {
			mSenderTV.setVisibility(View.GONE);
		}
		
		mAdapter = new MyAdapter();
		mListView.addHeaderView(mHeader);
		mListView.setAdapter(mAdapter);
		mSendCommentBtn.setBackgroundDrawable(new ColoredFadeoutDrawable(COLORED_FADEOUT_COLOR));

		boolean isFacebookPhoto = mMessage.type == Message.TYPE_FACEBOOK_PHOTO;
		
		if (mMessage.isTwitter()) {
			mComposeCommentHolder.setVisibility(View.GONE);
			mAttachmentsHolder.setVisibility(View.GONE);
		} else if (mMessage.isFacebook()) {
			
			if (isFacebookPhoto) {
				mMessageBox.setVisibility(View.GONE);
				mHeader.removeView(mAttachmentsHolder);
				mHeader.addView(mAttachmentsHolder, 0);
			}
		}
		
		mProfileIV.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				AccountsManager.viewProfile(getActivity(), mMessage.account, mMessage.sender);
			}
		});
		
		mSendCommentBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View pV) {
				((ColoredFadeoutDrawable)pV.getBackground()).startFadeout();
				final String update = mCommentET.getText().toString();
				if (TextUtils.isEmpty(update)) {
					Toast.makeText(getActivity().getApplicationContext(), "Cannot post a comment without text", Toast.LENGTH_LONG).show();
				} else {
					mCommentET.setText("");
					final ProgressDialog dialog = ProgressDialog.show(getActivity(), "Posting comment", "Please wait a moment...", true, false);
					Threads.runOnNetworkThread(new Runnable() {
						@Override
						public void run() {
							final boolean result = ((FacebookAccount) mMessage.account).postComment(mMessage, update);
							Threads.runOnUIThread(new Runnable() {
								@Override
								public void run() {
									dialog.dismiss();
									if (result) {
										updateComments();
									} else {
										Toast.makeText(getActivity().getApplicationContext(), "Failed to post comment", Toast.LENGTH_LONG).show();
									}
								}
							});
						}
					});
				}
			}
		});
		
		updateAll();
		if (mMessage.isDirty || (mMessage.comments != null && mMessage.comments.getAvailableCount() != mMessage.comments.getRealCount())) {
			refresh();
		}
		
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> pParent, View pView, int pPosition, long pId) {
				if (pView instanceof CommentView) {
					final Comment comment = ((CommentView) pView).getComment();
					String[] ops = new String[] {"Show likes", comment.userLikes ? "Unlike" : "Like"};
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, ops);
					new Builder(getActivity())
						.setAdapter(adapter, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface pDialog, int pWhich) {
								switch (pWhich) {
								case 0:
									showLikes(comment);
									break;
								case 1:
									likeComment(comment);
								default:
									break;
								}
							}
						}).show();
				}
			}
		});
	}
	
	@Override
	public void onStop() {
		super.onStop();
		ImageManager im = ImageManager.getInstance(getActivity());
		if (mMessage.sender != null) im.cancelLoad(mMessage.sender.profilePictureUrl);
		if (mMessage.facebookImages != null) {
			for (int i = 0; i < mMessage.facebookImages.size(); i++) {
				im.cancelLoad(mMessage.facebookImages.get(i).bigSrc);
			}
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu pMenu, MenuInflater pInflater) {
		super.onCreateOptionsMenu(pMenu, pInflater);
		pMenu.add(Menu.NONE, 1, Menu.NONE, R.string.share).setIcon(R.drawable.details_share).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
		if (mMessage.isFacebook()) {
			mRefreshItem = pMenu.add(Menu.NONE, 2, Menu.NONE, R.string.refresh).setIcon(R.drawable.actionbar_refresh).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
			mLikeItem = pMenu.add(Menu.NONE, 3, Menu.NONE, R.string.like).setIcon(mMessage.userLikes ? R.drawable.details_dislike : R.drawable.details_like).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
			mLikesItem = pMenu.add(Menu.NONE, 4, Menu.NONE, R.string.likes).setIcon(R.drawable.details_like).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS|MenuItem.SHOW_AS_ACTION_WITH_TEXT);
			pMenu.add(Menu.NONE, 5, Menu.NONE, R.string.comment).setIcon(R.drawable.details_reply).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
		} else {
			pMenu.add(Menu.NONE, 6, Menu.NONE, R.string.retweet).setIcon(R.drawable.details_retweet).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
			pMenu.add(Menu.NONE, 7, Menu.NONE, R.string.reply).setIcon(R.drawable.details_reply).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
			pMenu.add(Menu.NONE, 8, Menu.NONE, R.string.favorite).setIcon(R.drawable.details_favorite).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem pItem) {
		switch (pItem.getItemId()) {
		case 1: // share
			break;
		case 2: // refresh
			refresh();
			break;
		case 3: // like
			mLikeItem.setActionView(R.layout.ab_progressbar);
			Threads.runOnNetworkThread(new Runnable() {
				public void run() {
					FacebookAccount fa = (FacebookAccount) mMessage.account;
					final boolean result = mMessage.type == Message.TYPE_FACEBOOK_PHOTO ? 
						fa.likePhoto(mMessage) :
						fa.likeMessage(mMessage);
					Threads.runOnUIThread(new Runnable() {
						public void run() {
							mLikeItem.setActionView(null);
							mLikeItem.setIcon(mMessage.userLikes ? R.drawable.details_dislike : R.drawable.details_like);
							if (result) {
								updateLikes();
							} else {
								Toast.makeText(getActivity().getApplicationContext(), 
										"Cannot like this message. Check your network connection and try again", Toast.LENGTH_LONG).show();
							}
						}
					});
				}
			});
			break;
		case 4: // likes
			showLikes(null);
			break;
		case 5: // comment
			break;
		case 6: // retweet
			ComposeConfig config = new ComposeConfig(ComposeMode.TWITTER_RETWEET, mMessage.ID, mMessage.sender);
			config.template = mMessage.text;
			getActivity().startActivity(new Intent(getActivity(), ComposeActivity.class).putExtra(ComposeFragment.EXTRA_CONFIG, config.asBundle()));
			break;
		case 7: // reply
			config = new ComposeConfig(ComposeMode.TWITTER_REPLY, mMessage.ID, mMessage.sender);
			getActivity().startActivity(new Intent(getActivity(), ComposeActivity.class).putExtra(ComposeFragment.EXTRA_CONFIG, config.asBundle()));
			break;
		case 8: // favorite
			break;
		default:
			return false;
		}
		return true;
	}
	
	private void setRefreshing(boolean v) {
		if (v) {
			mRefreshItem.setActionView(R.layout.ab_progressbar);
		} else {
			mRefreshItem.setActionView(null);
		}
	}
	
	public void refresh() {
		setRefreshing(true);
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				final Message msg = mMessage.photo != null ?
						mMessage.photo.reloadMessage() : 
						((FacebookAccount)mMessage.account).loadStreamUpdate(mMessage.getPostID());
				Threads.runOnUIThread(new Runnable() {
					@Override
					public void run() {
						setRefreshing(false);
						if (msg != null) {
							mMessage = msg;
							updateBasics();
							updateLikes();
							updateComments();
						} else {
							Toast.makeText(getActivity().getApplicationContext(), "Failed to refresh", Toast.LENGTH_LONG).show();
						}
					}
				});
			}
		});
	}
	
	protected void likeComment(final Comment comment) {
		final boolean oldHasLiked = comment.userLikes;
		final ProgressDialog loadingDialog = ProgressDialog.show(getActivity(), null, "Please wait a moment...", true, false);
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				final boolean result = ((FacebookAccount)mMessage.account).likeComment(mMessage, comment);
				Threads.runOnUIThread(new Runnable() {
					@Override
					public void run() {
						loadingDialog.dismiss();
						if (result) {
							updateComments();
						} else {
							Toast.makeText(getActivity().getApplicationContext(), 
									oldHasLiked ? "Failed to unlike comment" : "Failed to like comment", Toast.LENGTH_LONG).show();
						}
					}
				});
			}
		});
	}
	
	protected void showLikes(final Comment comment) {
		final ProgressDialog loadingDialog = ProgressDialog.show(getActivity(), "Loading likes", "Please wait a moment...", true, true);
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				FacebookAccount fa = ((FacebookAccount)mMessage.account);
				final User[] likes = comment != null ? 
						fa.getCommentLikes(mMessage, comment) : 
							fa.getMessageLikes(mMessage);
				Threads.runOnUIThread(new Runnable() {
					@Override
					public void run() {
						if (loadingDialog.isShowing()) {
							loadingDialog.dismiss();
							if (likes != null) {
								Arrays.sort(likes);
								if (comment != null) {
									updateComments();
								} else {
									updateLikes();
								}
								Builder builder = new Builder(getActivity())
									.setTitle("Likes")
									.setPositiveButton("Close", null);
								if (likes.length > 0) {
									builder.setAdapter(new UserAdapter(getActivity(), likes), new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface pDialog, int pWhich) {
											AccountsManager.viewProfile(getActivity(), mMessage.account, likes[pWhich]);
										}
									});
								} else {
									builder.setMessage("This message doesn't have any likes yet");
								}
								builder.show();
							} else {
								Toast.makeText(getActivity().getApplicationContext(), "Failed to load likes.", Toast.LENGTH_LONG).show();
							}
						}
					}
				});
			}
		});
	}
	
	protected void updateAll() {
		updateBasics();
		updateLikes();
		updateComments();
		updateImages();
	}
	
	protected void updateComments() {
		mAdapter.notifyDataSetChanged();
	}
	
	protected void updateBasics() {
		if (mMessage.sender != null) {
			mProfileDrawable.setUser(getActivity(), mMessage.sender, false);
			mMsgTV.setText(mMessage.getBody());
			mMsgTV.setMovementMethod(LinkMovementMethod.getInstance());
			if (!TextUtils.isEmpty(mMessage.source)) {
				mInfoTV.setText( TimeUtils.parseDuration(System.currentTimeMillis() - mMessage.createdTime, true) + " via " + mMessage.source);
			} else {
				mInfoTV.setText( TimeUtils.parseDuration(System.currentTimeMillis() - mMessage.createdTime, true) );
			}
		}
	}
	
	protected void updateLikes() {
		int likes = mMessage.numLikes;
		if (mLikesItem != null) {
			mLikesItem.setTitle(String.valueOf(likes));
		}
	}
	
	protected void updateImages() {
		mAttachmentsHolder.removeAllViews();
		if (mMessage.facebookImages != null && mMessage.facebookImages.size() > 0) {
			Threads.runOnNetworkThread(new Runnable() {
				@Override
				public void run() {
					final ArrayList<FacebookImage> images = mMessage.facebookImages;
					if (FacebookImage.loadBigSrcs(images, (FacebookAccount) mMessage.account)) {
						ImageManager im = ImageManager.getInstance(getActivity());
						for (int i = 0; i < images.size(); i++) {
							final FacebookImage img = images.get(i);
							if (img.bigSrc == null) {
								continue;
							}
							im.loadImage(new LoadBitmapCallback() {
								@Override
								public void onFailed(String pUri) {}
								
								@Override
								public void onBitmapLoaded(String pUri, final Bitmap pBmd) {
									Threads.runOnUIThread(new Runnable() {
										public void run() {
											if (getActivity() != null) {
												mAttachmentsLoadingView.setVisibility(View.GONE);
												ImageView iv = new ImageView(getActivity());
												iv.setScaleType(ScaleType.FIT_XY);
												iv.setOnClickListener(new OnClickListener() {
													@Override
													public void onClick(View pV) {
														startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(img.href)).setType("image/*"));
													}
												});
												iv.setImageBitmap(pBmd);
												mAttachmentsHolder.addView(iv);
												ViewGroup.LayoutParams params = iv.getLayoutParams();
												float ratio = (float)pBmd.getWidth() / (float)pBmd.getHeight();
												params.width = LayoutParams.FILL_PARENT;
												params.height = (int) (getResources().getDisplayMetrics().widthPixels / ratio);
												iv.setLayoutParams(params);
											}
										}
									});
								}
							}, img.bigSrc, DeletionTrigger.IMMEDIATELY, ImageManager.REF_WEAK, -1, -1, 50);
						}
					} else {
						Threads.runOnUIThread(new Runnable() {
							public void run() {
								mAttachmentsLoadingView.setVisibility(View.GONE);
								Toast.makeText(getActivity().getApplicationContext(), "Failed to load images", Toast.LENGTH_LONG).show();
							}
						});
					}
				}
			});
		} else {
			mAttachmentsHolder.setVisibility(View.GONE);
			mAttachmentsLoadingView.setVisibility(View.GONE);
		}
	}
	
	private class MyAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mMessage.comments != null ? mMessage.comments.getAvailableCount() : 0;
		}

		@Override
		public Comment getItem(int pos) {
			return mMessage.comments.getComment(pos);
		}

		@Override
		public long getItemId(int pos) {
			return getItem(pos).hashCode();
		}

		@Override
		public View getView(int pos, View pConvertView, ViewGroup pParent) {
			Comment comment = getItem(pos);
			CommentView v = (CommentView) pConvertView;
			if (v == null) {
				v = new CommentView(getActivity());
			}
			v.setComment(mMessage, comment);
			return v;
		}
		
		@Override
		public boolean hasStableIds() {
			return true;
		}
		
	}
	
}
