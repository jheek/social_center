package com.jldroid.twook.fragments;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
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
import com.jdroid.utils.Threads;
import com.jdroid.utils.TimeUtils;
import com.jldroid.twook.R;
import com.jldroid.twook.activities.ComposeActivity;
import com.jldroid.twook.activities.ViewImageActivity;
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
import com.jldroid.twook.model.facebook.Photo;
import com.jldroid.twook.view.ColoredFadeoutDrawable;
import com.jldroid.twook.view.CommentView;
import com.jldroid.twook.view.ProfileImageDrawable;
import com.jldroid.twook.view.UserAdapter;

public class DetailsFragment extends SherlockFragment {

	public static final String EXTRA_ACCOUNT = "com.jldroid.twook.ACCOUNT";
	public static final String EXTRA_MSG = "com.jldroid.twook.MSG";
	public static final String EXTRA_PHOTO = "com.jldroid.twook.PHOTO";
	
	private static final int COLORED_FADEOUT_COLOR = 0xff008FD5;
	
	// views
	private ListView mListView;
	
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
	private Photo mPhoto;
	
	private MyAdapter mAdapter;
	
	private MenuItem mRefreshItem;
	private MenuItem mLikeItem;
	private MenuItem mLikesItem;
	
	private MenuItem mCommentItem;
	private boolean isCommenting = false;
	
	@Override
	public void onCreate(Bundle pSavedInstanceState) {
		super.onCreate(pSavedInstanceState);
		AccountsManager am = AccountsManager.getInstance(getActivity());
		IAccount account = getArguments().containsKey(EXTRA_ACCOUNT) ? am.findAccount(getArguments().getLong(EXTRA_ACCOUNT)) : null;
		mMessage = getArguments().containsKey(EXTRA_MSG) ? Message.findMessage(getActivity(), getArguments().getBundle(EXTRA_MSG)) : null;
		mPhoto = getArguments().containsKey(EXTRA_PHOTO) ? new Photo((FacebookAccount) account, getArguments().getBundle(EXTRA_PHOTO)) : null;
		if (mMessage == null && mPhoto == null) {
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
		
		mProfileDrawable = new ProfileImageDrawable(getActivity());
		mProfileIV.setBackgroundDrawable(mProfileDrawable);
		
		mProfileIV.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				AccountsManager.viewProfile(getActivity(), mMessage.account, mMessage.sender);
			}
		});
		
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
		
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle pSavedInstanceState) {
		super.onActivityCreated(pSavedInstanceState);
		setHasOptionsMenu(true);
		
		mAdapter = new MyAdapter();
		mListView.addHeaderView(mHeader);
		mListView.setAdapter(mAdapter);

		if (mMessage != null) {
			updateAll();
			if (mMessage.isDirty || (mMessage.comments != null && mMessage.comments.getAvailableCount() != mMessage.comments.getRealCount())) {
				refresh();
			}
		} else if (mPhoto != null) {
			final ProgressDialog prog = ProgressDialog.show(getActivity(), getString(R.string.pb_title_loading), getString(R.string.pb_msg_please_wait), true, false);
			Threads.runOnNetworkThread(new Runnable() {
				@Override
				public void run() {
					final Message msg = mPhoto.loadMessage();
					prog.dismiss();
					Threads.runOnUIThread(new Runnable() {
						@Override
						public void run() {
							if (msg != null) {
								mMessage = msg;
								updateAll();
							} else {
								Toast.makeText(getActivity().getApplicationContext(), R.string.failed_load_photo, Toast.LENGTH_LONG).show();
								getActivity().finish();
							}
						}
					});
				}
			});
		}
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
	
	public void setCommenting(boolean v) {
		this.isCommenting = v;
		getSherlockActivity().invalidateOptionsMenu();
	}
	
	@Override
	public void onCreateOptionsMenu(Menu pMenu, MenuInflater pInflater) {
		super.onCreateOptionsMenu(pMenu, pInflater);
		if (mMessage == null) {
			return;
		}
		if (!isCommenting) {
			pMenu.add(Menu.NONE, 1, Menu.NONE, R.string.share).setIcon(R.drawable.details_share).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
			if (mMessage.isFacebook()) {
				mRefreshItem = pMenu.add(Menu.NONE, 2, Menu.NONE, R.string.refresh).setIcon(R.drawable.actionbar_refresh).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
				mLikeItem = pMenu.add(Menu.NONE, 3, Menu.NONE, R.string.like).setIcon(mMessage.userLikes ? R.drawable.details_dislike : R.drawable.details_like).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
				mLikesItem = pMenu.add(Menu.NONE, 4, Menu.NONE, R.string.likes).setIcon(new LikesDrawable()).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
				pMenu.add(Menu.NONE, 5, Menu.NONE, R.string.comment).setIcon(R.drawable.details_reply).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
			} else {
				pMenu.add(Menu.NONE, 6, Menu.NONE, R.string.retweet).setIcon(R.drawable.details_retweet).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
				pMenu.add(Menu.NONE, 7, Menu.NONE, R.string.reply).setIcon(R.drawable.details_reply).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
				pMenu.add(Menu.NONE, 8, Menu.NONE, R.string.favorite).setIcon(R.drawable.details_favorite).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}
		} else {
			MenuItem item = pMenu.add(Menu.NONE, 10, Menu.NONE, R.string.comment).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
			mCommentItem = item;
			item.setActionView(R.layout.send_comment);
			View sendCommentBtn = item.getActionView().findViewById(R.id.sendCommentBtn);
			sendCommentBtn.setBackgroundDrawable(new ColoredFadeoutDrawable(COLORED_FADEOUT_COLOR));
			sendCommentBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					((ColoredFadeoutDrawable) v.getBackground()).startFadeout();
					final EditText commentET = (EditText) mCommentItem.getActionView().findViewById(R.id.commentET);
					final String update = commentET.getText().toString();
					if (TextUtils.isEmpty(update)) {
						Toast.makeText(getActivity().getApplicationContext(), "Cannot post a comment without text", Toast.LENGTH_LONG).show();
					} else {
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
											commentET.setText("");
											setCommenting(false);
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
			setCommenting(true);
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
	
	/**
	 * 
	 * @return returns true when the back press was handled
	 */
	public boolean onBackPressed() {
		if (isCommenting) {
			setCommenting(false);
			return true;
		}
		return false;
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
		getSherlockActivity().invalidateOptionsMenu();
		updateBasics();
		updateLikes();
		updateComments();
		updateImages();
	}
	
	protected void updateComments() {
		mAdapter.notifyDataSetChanged();
	}
	
	protected void updateBasics() {
		mSenderTV.setVisibility(mMessage.sender != null ? View.VISIBLE : View.GONE);
		if (mMessage.sender != null) {
			mSenderTV.setText(mMessage.getTitle(getActivity()));
			mProfileDrawable.setUser(getActivity(), mMessage.sender, false);
		}
		mMsgTV.setText(mMessage.getBody());
		mMsgTV.setMovementMethod(LinkMovementMethod.getInstance());
		if (!TextUtils.isEmpty(mMessage.source)) {
			mInfoTV.setText( TimeUtils.parseDuration(System.currentTimeMillis() - mMessage.createdTime, true) + getString(R.string.via) + mMessage.source);
		} else {
			mInfoTV.setText( TimeUtils.parseDuration(System.currentTimeMillis() - mMessage.createdTime, true) );
		}
		
		if (mMessage.isTwitter()) {
			mAttachmentsHolder.setVisibility(View.GONE);
		} else if (mMessage.isFacebook()) {
			if (mMessage.type == Message.TYPE_FACEBOOK_PHOTO) {
				mMessageBox.setVisibility(View.GONE);
				mHeader.removeView(mAttachmentsHolder);
				mHeader.addView(mAttachmentsHolder, 0);
			}
		}
	}
	
	protected void updateLikes() {
		if (mLikesItem != null) {
			mLikesItem.getIcon().invalidateSelf();
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
								public void onBitmapLoaded(final String pUri, final Bitmap pBmd) {
									Threads.runOnUIThread(new Runnable() {
										public void run() {
											if (getActivity() != null) {
												mAttachmentsLoadingView.setVisibility(View.GONE);
												ImageView iv = new ImageView(getActivity());
												iv.setScaleType(ScaleType.FIT_XY);
												iv.setOnClickListener(new OnClickListener() {
													@Override
													public void onClick(View pV) {
														startActivity(new Intent(getActivity(), ViewImageActivity.class).putExtra(ViewImageFragment.EXTRA_IMG_URL, pUri));
													}
												});
												iv.setImageBitmap(pBmd);
												mAttachmentsHolder.addView(iv);
												ViewGroup.LayoutParams params = iv.getLayoutParams();
												float ratio = (float)pBmd.getWidth() / (float)pBmd.getHeight();
												params.width = LayoutParams.MATCH_PARENT;
												params.height = (int) (getResources().getDisplayMetrics().widthPixels / ratio);
												iv.setLayoutParams(params);
											}
										}
									});
								}
							}, img.bigSrc, DeletionTrigger.IMMEDIATELY, -1, -1, 50);
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
	
	private class LikesDrawable extends Drawable {

		private Drawable mLikesIcon;
		
		private int mNumLikes = -1;
		private String mLikesString;
		
		private Paint mTextPaint = new Paint();
		
		public LikesDrawable() {
			mLikesIcon = getResources().getDrawable(R.drawable.details_like);
			mTextPaint.setColor(Color.BLACK);
			mTextPaint.setShadowLayer(2, 0, 0, Color.WHITE);
			mTextPaint.setTextAlign(Align.CENTER);
			mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
			mTextPaint.setTextSize(15 * getResources().getDisplayMetrics().density);
		}
		
		@Override
		public void draw(Canvas canvas) {
			mLikesIcon.draw(canvas);
			Rect bounds = getBounds();
			if (mNumLikes != mMessage.numLikes) {
				mNumLikes = mMessage.numLikes;
				mLikesString = String.valueOf(mNumLikes);
			}
			canvas.drawText(mLikesString, bounds.width() / 2, bounds.bottom - (bounds.height() - mTextPaint.getTextSize()) / 2, mTextPaint);
		}

		@Override
		public void setAlpha(int alpha) {
			mLikesIcon.setAlpha(alpha);
		}

		@Override
		public void setColorFilter(ColorFilter cf) {
			mLikesIcon.setColorFilter(cf);
		}
		
		@Override
		public boolean setState(int[] stateSet) {
			return super.setState(stateSet) | mLikesIcon.setState(stateSet);
		}
		
		@Override
		public void setBounds(int left, int top, int right, int bottom) {
			super.setBounds(left, top, right, bottom);
			mLikesIcon.setBounds(left, top, right, bottom);
		}
		
		@Override
		public int getIntrinsicWidth() {
			return mLikesIcon.getIntrinsicWidth();
		}
		
		@Override
		public int getIntrinsicHeight() {
			return mLikesIcon.getIntrinsicHeight();
		}

		@Override
		public int getOpacity() {
			return PixelFormat.TRANSLUCENT;
		}
		
	}
 	
	private class MyAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mMessage != null && mMessage.comments != null ? mMessage.comments.getAvailableCount() : 0;
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
