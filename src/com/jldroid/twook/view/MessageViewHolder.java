package com.jldroid.twook.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.jdroid.utils.Threads;
import com.jldroid.twook.FastBitmapDrawable;
import com.jldroid.twook.R;
import com.jldroid.twook.model.AccountsManager;
import com.jldroid.twook.model.ImageManager;
import com.jldroid.twook.model.ImageManager.DeletionTrigger;
import com.jldroid.twook.model.ImageManager.LoadBitmapCallback;
import com.jldroid.twook.model.Message;
import com.jldroid.twook.model.facebook.FacebookImage;

public class MessageViewHolder implements LoadBitmapCallback {
	
	private static final int PREVIEW_IMAGE_SIZE = 100;
	
	private static int sPreviewImageSize = -1;
	
	private Context mContext;
	
	private View mView;
	
	private TextView mSenderTV;
	private TextView mLikesTV;
	private TextView mCommentsTV;
	private TextView mTimeTV;
	private TextView mBodyTV;
	
	private View mProfileIV;
	private View mPreviewIV;
	
	//private View mColorView;
	
	private Message mMessage;
	
	private ProfileImageDrawable mProfileDrawable;
	private FastBitmapDrawable mPreviewDrawable = new FastBitmapDrawable();
	
	protected boolean mShowPreviews;
	
	public MessageViewHolder(Context c) {
		mContext = c;
		mView = LayoutInflater.from(c).inflate(R.layout.message, null);
		mSenderTV = (TextView) mView.findViewById(R.id.senderTV);
		mLikesTV = (TextView) mView.findViewById(R.id.likesTV);
		mCommentsTV = (TextView) mView.findViewById(R.id.commentsTV);
		mTimeTV = (TextView) mView.findViewById(R.id.timeTV);
		mBodyTV = (TextView) mView.findViewById(R.id.bodyTV);
		mProfileIV = mView.findViewById(R.id.profileIV);
		mPreviewIV = mView.findViewById(R.id.previewIV);
		//mColorView = mView.findViewById(R.id.colorView);
		
		mView.setTag(this);
		
		mProfileIV.setBackgroundDrawable(mProfileDrawable = new ProfileImageDrawable(c));
		
		mProfileIV.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View pV) {
				AccountsManager.viewProfile(pV.getContext(), mMessage.account, mMessage.sender);
			}
		});
		
		if (sPreviewImageSize == -1) {
			float d = c.getResources().getDisplayMetrics().density;
			sPreviewImageSize = (int) (PREVIEW_IMAGE_SIZE * d);
		}
	}
	
	public void setMessage(Message msg) {
		final ImageManager im = ImageManager.getInstance(mContext);
		if (mMessage != null) {
			im.cancelLoad(mMessage.sender.profilePictureUrl);
			if (mMessage.facebookImages != null && mMessage.facebookImages.size() > 0) {
				im.cancelLoad(mMessage.facebookImages.get(0).src);
			}
		}
		
		this.mMessage = msg;
		
		mSenderTV.setText(msg.getTitle(mContext));
		mBodyTV.setText(msg.getBody());
		mTimeTV.setText(msg.getTime());
		
		mLikesTV.setVisibility(msg.numLikes > 0 ? View.VISIBLE : View.GONE);
		mLikesTV.setText(String.valueOf(msg.numLikes));
		int numComments = msg.comments != null ? msg.comments.getRealCount() : 0;
		mCommentsTV.setVisibility(numComments > 0 ? View.VISIBLE : View.GONE);
		mCommentsTV.setText(String.valueOf(numComments));
		
		/*int color = mMessage.account.getColor();
		if ((color & 0x00FFFFFF) == 0x00FFFFFF || color == 0x00000000) {
			mColorView.setVisibility(View.GONE);
		} else {
			mColorView.setVisibility(View.VISIBLE);
			mColorView.setBackgroundColor(color);
		}*/
		
		mProfileDrawable.setUser(mContext, msg.sender, false);
		
		if (mShowPreviews && msg.facebookImages != null && msg.facebookImages.size() > 0) {
			mPreviewIV.setVisibility(View.VISIBLE);
			final FacebookImage img = msg.facebookImages.get(0);
			Bitmap previewBmd = im.peekImage(img.src);
			if (previewBmd == null) {
				if (img.width != -1 && img.height != -1) {
					float ratio = (float) img.width / (float) img.height;
					int h = (int) (sPreviewImageSize / ratio);
					mPreviewDrawable.setBounds(0, 0, sPreviewImageSize, h);
					mPreviewIV.getLayoutParams().width = sPreviewImageSize;
					mPreviewIV.getLayoutParams().height = h;
					mPreviewIV.setLayoutParams(mPreviewIV.getLayoutParams());
				} else {
					mPreviewIV.getLayoutParams().width = mPreviewIV.getLayoutParams().height = sPreviewImageSize;
					mPreviewIV.setLayoutParams(mPreviewIV.getLayoutParams());
				}
				mPreviewIV.setBackgroundResource(R.drawable.no_profileimg_img);
				im.loadImage(this, img.src, DeletionTrigger.AFTER_ONE_DAY_UNUSED, ImageManager.REF_WEAK, -1, -1, 80);
			} else {
				mPreviewDrawable.setBitmap(previewBmd, false);
				mPreviewIV.setBackgroundDrawable(mPreviewDrawable);
				setPreviewBounds(previewBmd);
			}
		} else {
			mPreviewIV.setVisibility(View.GONE);
		}
	}
	
	private void setPreviewBounds(Bitmap bmd) {
		float w = bmd.getWidth();
		float h = bmd.getHeight();
		float ratio = w / h;
		mPreviewIV.getLayoutParams().width = sPreviewImageSize;
		mPreviewIV.getLayoutParams().height = (int) (sPreviewImageSize / ratio);
		mPreviewIV.setLayoutParams(mPreviewIV.getLayoutParams());
	}
	
	public void setShowPreviews(boolean pShowPreviews) {
		mShowPreviews = pShowPreviews;
	}
	
	public View getView() {
		return mView;
	}
	
	@Override
	public void onBitmapLoaded(final String pUri, final Bitmap pBmd) {
		Threads.runOnUIThread(new Runnable() {
			@Override
			public void run() {
				if (mMessage.facebookImages != null && mMessage.facebookImages.size() > 0 && pUri.equals(mMessage.facebookImages.get(0).src)) {
					mPreviewDrawable.setBitmap(pBmd, true);
					mPreviewIV.setBackgroundDrawable(mPreviewDrawable);
					setPreviewBounds(pBmd);
				}
			}
		});
	}
	
	@Override
	public void onFailed(final String pUri) {
		Threads.runOnUIThread(new Runnable() {
			@Override
			public void run() {
				if (mMessage.facebookImages != null && mMessage.facebookImages.size() > 0 && pUri.equals(mMessage.facebookImages.get(0).src)) {
					mPreviewIV.setVisibility(View.GONE);
				}
			}
		});
	}
	
	/*private static final int ANIMATION_DURATION = 500;
	
	private static final int PREVIEW_IMAGE_SIZE = 100;
	private static int sPreviewImageSize = -1;
	
	private static Paint sPaint = new Paint();
	private static TextPaint sTitlePaint = new TextPaint();
	private static TextPaint sBodyPaint = new TextPaint();
	private static TextPaint sInfoPaint = new TextPaint();
	private static TextPaint sTimePaint = new TextPaint();
	
	private static FontMetrics sTimeFontMetrics;
	
	private static Xfermode sXfermode = new PorterDuffXfermode(Mode.SRC_OVER);
	
	static {
		sPaint.setColor(Color.WHITE);
		
		sTitlePaint.setColor(Color.BLACK);
		sBodyPaint.setColor(Color.BLACK);
		sInfoPaint.setColor(Color.GRAY);
		sTimePaint.setColor(Color.GRAY);
		
		sTitlePaint.setXfermode(sXfermode);
		sBodyPaint.setXfermode(sXfermode);
		sInfoPaint.setXfermode(sXfermode);
		sTimePaint.setXfermode(sXfermode);
		
		sTitlePaint.setAntiAlias(true);
		sBodyPaint.setAntiAlias(true);
		sInfoPaint.setAntiAlias(true);
		sTimePaint.setAntiAlias(true);
		
		sTitlePaint.setTypeface(Typeface.DEFAULT_BOLD);
	}
	
	private static Bitmap sNoProfilePictureBmd;
	private static Bitmap sTwitterIcon;
	private static Bitmap sFacebookIcon;
	private static Bitmap sTimeIcon;
	
	private static Rect sBounds = new Rect();
	
	protected Message mMessage;
	
	protected boolean mShowPreviews;
	
	protected Bitmap mProfilePictureBmd;
	protected Bitmap mAccountTypeBmd;
	protected Bitmap mImagePreviewBmd;
	
	private int mColor;
	
	private String mTitleStr;
	private int[] mTitleOffsets = new int[2];
	private int[] mTitleEnds = new int[2];
	private int mTitleLineCount = 0;
	
	private String mBodyStr;
	private int[] mBodyOffsets = new int[6];
	private int[] mBodyEnds = new int[6];
	private int mBodyLineCount = 0;
	
	private String mInfoStr;
	
	private String mTimeStr;
	private int mTimeWidth;
	
	private int mImagePreviewHeight;
	
	private long mSetProfilePictureTime = -1;
	private long mSetImagePreviewTime = -1;
	
	private GestureDetector mGestureDetector = new GestureDetector(this);
	
	public MessageView(Context c) {
		super(c);
		
		if (sNoProfilePictureBmd == null) {
			sNoProfilePictureBmd = ((BitmapDrawable)getResources().getDrawable(R.drawable.no_profileimg_img)).getBitmap();
			sTwitterIcon = ((BitmapDrawable)getResources().getDrawable(R.drawable.twitter_icon)).getBitmap();
			sFacebookIcon = ((BitmapDrawable)getResources().getDrawable(R.drawable.facebook_icon)).getBitmap();
			sTimeIcon = ((BitmapDrawable)getResources().getDrawable(R.drawable.time_ago)).getBitmap();
			
			sPreviewImageSize = (int) (getResources().getDisplayMetrics().density * PREVIEW_IMAGE_SIZE);
			
			float sp = getResources().getDisplayMetrics().scaledDensity;
			sTitlePaint.setTextSize((int)(16 * sp));
			sBodyPaint.setTextSize((int)(14 * sp));
			sInfoPaint.setTextSize((int)(13 * sp));
			sTimePaint.setTextSize((int)(11 * sp));
			
			final TypedArray a = c.obtainStyledAttributes(null, R.styleable.MessageView, R.attr.MessageViewStyle, 0);
			sTitlePaint.setColor(a.getColor(R.styleable.MessageView_messageTitleColor, -1));
			sBodyPaint.setColor(a.getColor(R.styleable.MessageView_messageBodyColor, -1));
			sInfoPaint.setColor(a.getColor(R.styleable.MessageView_messageInfoColor, -1));
			sTimePaint.setColor(a.getColor(R.styleable.MessageView_messageTimeColor, -1));
			a.recycle();
			sTimeFontMetrics = sTimePaint.getFontMetrics();
		}
	}
	
	public void setShowPreviews(boolean pShowPreviews) {
		mShowPreviews = pShowPreviews;
	}
	
	public void setMessage(final Message msg) {
		if (mMessage != null) {
			ImageManager im = ImageManager.getInstance(getContext());
			im.cancelLoad(mMessage.sender.profilePictureUrl);
			if (mMessage.facebookImages != null && mMessage.facebookImages.size() > 0) {
				im.cancelLoad(mMessage.facebookImages.get(0).src);
			}
		}
		
		this.mMessage = msg;
		mColor = msg.account.getColor();
		
		mAccountTypeBmd = msg.isFacebook() ? sFacebookIcon : sTwitterIcon;
		
		mTitleStr = msg.getTitle();
		mBodyStr = msg.text != null ? msg.text : "";
		mInfoStr = msg.generateInfoString();
		mTimeStr = msg.generateTimeString();
		
		final ImageManager im = ImageManager.getInstance(getContext());
		final String uri = msg.sender.profilePictureUrl;
		mSetProfilePictureTime = -1;
		mProfilePictureBmd = im.peekImage(uri);
		if (mProfilePictureBmd == null) {
			mProfilePictureBmd = sNoProfilePictureBmd;
			im.loadProfilePicture(this, uri, DeletionTrigger.AFTER_ONE_WEEK_UNUSED);
		}
		
		mImagePreviewHeight = -1;
		mImagePreviewBmd = null;
		mSetImagePreviewTime = -1;
		if (mShowPreviews && msg.facebookImages != null && msg.facebookImages.size() > 0) {
			final FacebookImage img = msg.facebookImages.get(0);
			float ratio = (float) img.width / (float) img.height;
			mImagePreviewHeight = (int) (sPreviewImageSize / ratio);
			mImagePreviewBmd = im.peekImage(img.src);
			if (mImagePreviewBmd == null) {
				im.loadImage(this, img.src, DeletionTrigger.AFTER_ONE_DAY_UNUSED, ImageManager.REF_HARD, -1, -1, 80);
			} 
		}
		requestLayout();
		invalidate();
	}
	
	private static int measureText(String text, TextPaint paint, int[] offsets, int[] ends, float maxWidth, int maxLines) {
		int i = 0;
		int lineCount = 0;
		for (int i2 = 0; i2 < maxLines; i2++) {
			int numChars = paint.breakText(text, i, text.length(), true, maxWidth, null);
			if (i + numChars < text.length() && !Character.isSpaceChar(text.charAt(i + numChars))) {
				int oldChars = numChars;
				while (!Character.isSpaceChar(text.charAt(i + numChars - 1))) {
					numChars--;
					if (numChars == 0) {
						numChars = oldChars;
						break;
					}
				}
			}
			offsets[i2] = i;
			ends[i2] = i + numChars;
			lineCount++;
			i += numChars;
			if (i == text.length()) {
				break;
			}
		}
		return lineCount;
	}
	
	@Override
	protected void onMeasure(int pWidthMeasureSpec, int pHeightMeasureSpec) {
		int w = MeasureSpec.getSize(pWidthMeasureSpec);
		float h = 0;
		float d = getResources().getDisplayMetrics().density;
		float p = 5 * d;
		float tp = 4 * d;
		
		h += 0;
		
		sTimePaint.getTextBounds(mTimeStr, 0, mTimeStr.length(), sBounds);
		mTimeWidth = sBounds.width();
		float maxTitleWidth = w - mProfilePictureBmd.getWidth() - p * 2 - mTimeWidth - sTimeIcon.getWidth();
		float maxBodyWidth = w - mProfilePictureBmd.getWidth() - p * 3;
		mTitleLineCount = measureText(mTitleStr, sTitlePaint, mTitleOffsets, mTitleEnds, maxTitleWidth, mTitleOffsets.length);
		mBodyLineCount = measureText(mBodyStr, sBodyPaint, mBodyOffsets, mBodyEnds, maxBodyWidth, mBodyOffsets.length);
		
		h += mTitleLineCount * sTitlePaint.getTextSize() + mBodyLineCount * sBodyPaint.getTextSize() + tp * (mTitleLineCount + mBodyLineCount) + p;
		if (mImagePreviewHeight != -1) {
			h += p + mImagePreviewHeight;
		}
		if (mInfoStr != null) {
			h += sInfoPaint.getTextSize() + tp;
		}
		
		h = Math.max((float)mProfilePictureBmd.getHeight() + p, h);
		
		h += p;
		setMeasuredDimension(w, (int) h);
	}
	
	private static final Rect _dest = new Rect();
	@Override
	protected void onDraw(Canvas pCanvas) {
		super.onDraw(pCanvas);
		if (this.mMessage != null) {
			float d = getResources().getDisplayMetrics().density;
			float p = 5 * d;
			float tp = 4 * d;
			boolean invalidate = false;
			sPaint.setAlpha(255);
			long currentTime = SystemClock.elapsedRealtime();
			if (mSetProfilePictureTime != -1) {
				if (currentTime - mSetProfilePictureTime > ANIMATION_DURATION) {
					mSetProfilePictureTime = -1;
				} else {
					sPaint.setAlpha( (int) ((float)(currentTime - mSetProfilePictureTime) / (float)ANIMATION_DURATION * 255f));
					pCanvas.drawBitmap(sNoProfilePictureBmd, p, p, null);
					pCanvas.drawBitmap(mAccountTypeBmd, p, p + mProfilePictureBmd.getHeight() - mAccountTypeBmd.getHeight(), null);
				}
				invalidate = true;
			}
			pCanvas.drawBitmap(mProfilePictureBmd, p, p, sPaint);
			pCanvas.drawBitmap(mAccountTypeBmd, p, p + mProfilePictureBmd.getHeight() - mAccountTypeBmd.getHeight(), sPaint);
			
			if (mColor != 0x00000000 && mColor != 0xFFFFFFFF) {
				sPaint.setColor(mColor);
				float size = sTitlePaint.getTextSize();
				pCanvas.drawRect(getWidth() - size - p, getHeight() - size - p, getWidth() - p, getHeight() - p, sPaint);
				sPaint.setColor(Color.WHITE);
			}
			
			float textOffsetX = mProfilePictureBmd.getWidth() + p * 2;
			float textOffsetY = 0;
			
			for (int i = 0; i < mTitleLineCount; i++) {
				textOffsetY += sTitlePaint.getTextSize() + tp;
				pCanvas.drawText(mTitleStr, mTitleOffsets[i], mTitleEnds[i], textOffsetX, textOffsetY, sTitlePaint);
			}
			textOffsetY += p;
			
			float timeH = -sTimeFontMetrics.ascent + sTimeFontMetrics.descent;
			
			pCanvas.drawBitmap(sTimeIcon, getWidth() - mTimeWidth - p * 1.5f - sTimeIcon.getWidth(), tp + (timeH - sTimeIcon.getHeight()) / 2f, null);
			pCanvas.drawText(mTimeStr, getWidth() - mTimeWidth - p, tp + sTimePaint.getTextSize(), sTimePaint);
			
			for (int i = 0; i < mBodyLineCount; i++) {
				textOffsetY += sBodyPaint.getTextSize() + tp;
				pCanvas.drawText(mBodyStr, mBodyOffsets[i], mBodyEnds[i], textOffsetX, textOffsetY, sBodyPaint);
			}
			
			if (mImagePreviewBmd != null) {
				sPaint.setAlpha(255);
				if (mSetImagePreviewTime != -1) {
					if (currentTime - mSetImagePreviewTime > ANIMATION_DURATION) {
						mSetImagePreviewTime = -1;
					} else {
						sPaint.setAlpha( (int) ((float)(currentTime - mSetImagePreviewTime) / (float)ANIMATION_DURATION * 255f));
					}
					invalidate = true;
				}
				textOffsetY += p;
				Rect dest = _dest;
				dest.set((int)textOffsetX, (int)textOffsetY, (int)textOffsetX + sPreviewImageSize, (int)textOffsetY + mImagePreviewHeight);
				pCanvas.drawBitmap(mImagePreviewBmd, null, dest, sPaint);
				textOffsetY += mImagePreviewHeight;
			}
			
			if (mInfoStr != null) {
				textOffsetY += sInfoPaint.getTextSize() + tp;
				pCanvas.drawText(mInfoStr, textOffsetX, textOffsetY, sInfoPaint);
			}
			
			if (invalidate) {
				invalidate();
			}
		}
	}
	
	@Override
	public void onBitmapLoaded(final String pUri, final Bitmap pBmd) {
		Threads.runOnUIThread(new Runnable() {
			@Override
			public void run() {
				if (mMessage.facebookImages != null && mMessage.facebookImages.size() > 0 && pUri.equals(mMessage.facebookImages.get(0).src)) {
					mImagePreviewBmd = pBmd;
					mSetImagePreviewTime = SystemClock.elapsedRealtime();
					invalidate();
				} else if (mMessage.sender.profilePictureUrl.equals(pUri)) {
					mProfilePictureBmd = pBmd;
					mSetProfilePictureTime = SystemClock.elapsedRealtime();
					invalidate();
				}
			}
		});
	}
	
	@Override
	public void onFailed(final String pUri) {
		Threads.runOnUIThread(new Runnable() {
			@Override
			public void run() {
				if (mMessage.facebookImages != null && mMessage.facebookImages.size() > 0 && pUri.equals(mMessage.facebookImages.get(0).src)) {
					mImagePreviewHeight = -1;
					requestLayout();
				}
			}
		});
	}
	
	private boolean isTouchingProfile(float x, float y) {
		return x > 0 && y > 0 && x < mProfilePictureBmd.getWidth() && y < mProfilePictureBmd.getHeight();
	}
	
	@Override
	public boolean onDown(MotionEvent pE) {
		return isTouchingProfile(pE.getX(), pE.getY());
	}
	
	@Override
	public boolean onFling(MotionEvent pE1, MotionEvent pE2, float pVelocityX, float pVelocityY) {
		return true;
	}
	
	@Override
	public void onLongPress(MotionEvent pE) {
	}
	@Override
	public boolean onScroll(MotionEvent pE1, MotionEvent pE2, float pDistanceX, float pDistanceY) {
		return false;
	}
	
	@Override
	public void onShowPress(MotionEvent pE) {
	}
	
	@Override
	public boolean onSingleTapUp(MotionEvent pE) {
		if (isTouchingProfile(pE.getX(), pE.getY())) {
			((MainActivity) getContext()).showFragment(new ViewProfileFragment(mMessage.account, mMessage.sender));
			return true;
		}
		return false;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent pEvent) {
		return mGestureDetector.onTouchEvent(pEvent);
	}*/
	
}
