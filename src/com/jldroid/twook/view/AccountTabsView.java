package com.jldroid.twook.view;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jdroid.utils.Threads;
import com.jldroid.twook.FastBitmapDrawable;
import com.jldroid.twook.R;
import com.jldroid.twook.model.AccountsManager;
import com.jldroid.twook.model.IAccount;
import com.jldroid.twook.model.ImageManager;
import com.jldroid.twook.model.ImageManager.DeletionTrigger;
import com.jldroid.twook.model.ImageManager.LoadBitmapCallback;
import com.jldroid.twook.model.facebook.FacebookAccount;
import com.jldroid.twook.model.twitter.TwitterAccount;

public class AccountTabsView extends LinearLayout {

	protected static Bitmap sTwitterIcon;
	protected static Bitmap sFacebookIcon;
	
	protected Runnable mSelectedAccountsChangedRunnable;
	
	protected ViewGroup mHolder;
	protected TextView mCharactersLeftTV;
	
	protected ArrayList<IAccount> mSelectedAccounts = new ArrayList<IAccount>();
	
	protected boolean mSingleSelectionMode = false;
	
	public AccountTabsView(Context pContext) {
		super(pContext);
		init();
	}

	public AccountTabsView(Context pContext, AttributeSet pAttrs) {
		super(pContext, pAttrs);
		init();
	}
	
	public AccountTabsView(Context pContext, AttributeSet pAttrs, int pDefStyle) {
		super(pContext, pAttrs, pDefStyle);
		init();
	}
	
	private void init() {
		LayoutInflater.from(getContext()).inflate(R.layout.account_tabs, this);
		mHolder = (ViewGroup) findViewById(R.id.accounttabholder);
		mCharactersLeftTV = (TextView) findViewById(R.id.charactersLeftTV);
		
		mCharactersLeftTV.setVisibility(GONE);
		if (sTwitterIcon == null) {
			sTwitterIcon = ((BitmapDrawable)getResources().getDrawable(R.drawable.twitter_icon)).getBitmap();
			sFacebookIcon = ((BitmapDrawable)getResources().getDrawable(R.drawable.facebook_icon)).getBitmap();
		}
	}
	
	public void hideCharactersLeft() {
		mCharactersLeftTV.setText(null);
		mCharactersLeftTV.setVisibility(GONE);
	}
	
	public void showCharactersLeft(String text) {
		mCharactersLeftTV.setText(text);
		mCharactersLeftTV.setVisibility(VISIBLE);
	}
	
	public void setCharactersLeftColor(int color) {
		mCharactersLeftTV.setTextColor(color);
	}

	public Runnable getSelectedAccountsChangedRunnable() {
		return mSelectedAccountsChangedRunnable;
	}
	
	public void setSelectedAccountsChangedRunnable(Runnable pSelectedAccountsChangedRunnable) {
		mSelectedAccountsChangedRunnable = pSelectedAccountsChangedRunnable;
	}
	
	public void setAccounts(boolean twitter, boolean facebook) {
		mHolder.removeAllViews();
		AccountsManager am = AccountsManager.getInstance(getContext());
		if (twitter) {
			for (int i = 0; i < am.getTwitterAccountCount(); i++) {
				final TwitterAccount ta = am.getTwitterAccount(i);
				addAccount(ta);
			}
		}
		if (facebook) {
			for (int i = 0; i < am.getFacebookAccountCount(); i++) {
				final FacebookAccount fa = am.getFacebookAccount(i);
				addAccount(fa);
			}
		}
	}
	
	public void setSingleSelectionMode(boolean pSingleSelectionMode) {
		mSingleSelectionMode = pSingleSelectionMode;
	}
	
	private void addAccount(IAccount account) {
		AccountView av = new AccountView(getContext(), account);
		final int p = (int) (getResources().getDisplayMetrics().density * 5);
		av.setPadding(p, p, 0, p);
		mHolder.addView(av);
		av.getLayoutParams().width = (int) ((48 + 1 * 5) * getResources().getDisplayMetrics().density);
		av.getLayoutParams().height = (int) ( (48 + 2 * 5) * getResources().getDisplayMetrics().density);
		av.setLayoutParams(av.getLayoutParams());
		av.setChecked(!mSingleSelectionMode);
	}
	
	public ArrayList<IAccount> getSelectedAccounts() {
		return mSelectedAccounts;
	}
	
	public void setSelectedTab(int pI) {
		((AccountView) mHolder.getChildAt(pI)).setChecked(true);
	}
	
	private class AccountView extends View implements LoadBitmapCallback {
		
		private Drawable mDrawable;
		private Rect mDest = new Rect();
		
		private boolean mChecked;
		
		private Paint mPaint = new Paint();
		
		public IAccount account;
		
		public AccountView(Context c, IAccount account) {
			super(c);
			this.account = account;
			
			String uri = account.getUser().profilePictureUrl;
			
			ImageManager im = ImageManager.getInstance(c);
			Bitmap bmd = im.peekImage(uri);
			if (bmd != null) {
				mDrawable = new FastBitmapDrawable(bmd);
			} else {
				mDrawable = getResources().getDrawable(R.drawable.no_profileimg_img);
				im.loadProfilePicture(this, uri, DeletionTrigger.AFTER_ONE_MONTH_UNUSED);
			}
			
			setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View pV) {
					setChecked(!mChecked);
				}
			});
		}
		
		public boolean isChecked() {
			return mChecked;
		}
		
		public void setChecked(boolean pChecked) {
			boolean contains = mSelectedAccounts.contains(account);
			if (mSingleSelectionMode) {
				if (pChecked && !contains) {
					mChecked = pChecked;
					for (int i = 0; i < mHolder.getChildCount(); i++) {
						AccountView v = (AccountView) mHolder.getChildAt(i);
						if (v != this && v.mChecked) {
							v.mChecked = false;
							mSelectedAccounts.remove(v.account);
							v.invalidate();
						}
					}
					mSelectedAccounts.add(account);
				} else if (mSelectedAccounts.size() > 1) {
					mChecked = pChecked;
					mSelectedAccounts.remove(account);
				} else {
					return;
				}
			} else {
				mChecked = pChecked;
				if (pChecked && !contains) {
					mSelectedAccounts.add(account);
				} else if (!pChecked) {
					mSelectedAccounts.remove(account);
				}
			}
			if (mSelectedAccountsChangedRunnable != null) {
				mSelectedAccountsChangedRunnable.run();
			}
			invalidate();
		}
		
		@Override
		protected void onDraw(Canvas pCanvas) {
			super.onDraw(pCanvas);
			mPaint.setColor(Color.WHITE);
			mPaint.setAlpha(mChecked ? 255 : 128);
			
			mDest.set(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
			mDrawable.setBounds(mDest);
			mDrawable.setAlpha(mPaint.getAlpha());
			mDrawable.draw(pCanvas);
			
			Bitmap bmd = null;
			if (account instanceof TwitterAccount) {
				bmd = sTwitterIcon;
			} else if (account instanceof FacebookAccount) {
				bmd = sFacebookIcon;
			}
			mDest.right = mDest.left + bmd.getWidth();
			mDest.top = mDest.bottom - bmd.getHeight();
			pCanvas.drawBitmap(bmd, null, mDest, mPaint);
	
		}
		
		@Override
		public void onBitmapLoaded(String pUri, final Bitmap pBmd) {
			Threads.runOnUIThread(new Runnable() {
				@Override
				public void run() {
					mDrawable = new FastBitmapDrawable(pBmd);
					invalidate();
				}
			});
		}
		
		@Override
		public void onFailed(String pUri) {
		}
	}
	
}
