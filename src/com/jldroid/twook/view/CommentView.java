package com.jldroid.twook.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jldroid.twook.Globals;
import com.jldroid.twook.R;
import com.jldroid.twook.activities.MainActivity;
import com.jldroid.twook.fragments.ViewProfileFragment;
import com.jldroid.twook.model.AccountsManager;
import com.jldroid.twook.model.ImageManager;
import com.jldroid.twook.model.ImageManager.DeletionTrigger;
import com.jldroid.twook.model.ImageManager.LoadBitmapCallback;
import com.jldroid.twook.model.Message;
import com.jldroid.twook.model.facebook.Comment;
import com.jdroid.utils.TimeUtils;

public class CommentView extends RelativeLayout implements OnClickListener {
	
	private static StringBuilder sSB = new StringBuilder(48);
	
	private ImageView mProfileIV;
	
	private TextView mSenderTV;
	private TextView mInfoTV;
	private TextView mBodyTV;
	
	protected Message mMsg;
	protected Comment mComment;
	
	private ProfileImageDrawable mProfileDrawable;
	
	public CommentView(Context c) {
		super(c);
		inflate(c, R.layout.comment, this);
		mProfileIV = (ImageView) findViewById(R.id.profileIV);
		mSenderTV = (TextView) findViewById(R.id.senderTV);
		mInfoTV = (TextView) findViewById(R.id.infoTV);
		mBodyTV = (TextView) findViewById(R.id.bodyTV);
		
		mProfileIV.setOnClickListener(this);
		mProfileIV.setImageDrawable(mProfileDrawable = new ProfileImageDrawable(c));
	}
	
	public void setComment(final Message msg, final Comment comment) {
		mMsg = msg;
		mComment = comment;
		
		mProfileDrawable.setUser(getContext(), comment.sender, false);
		
		mSenderTV.setText(comment.sender.name);
		StringBuilder sb = sSB;
		sb.setLength(0);
		sb.append(comment.likes);
		sb.append(" likes \u2022 ");
		TimeUtils.parseDuration(sb, System.currentTimeMillis() - comment.time, true);
		mInfoTV.setText(sb.toString());
		mBodyTV.setText(comment.text);
	}
	
	public Comment getComment() {
		return mComment;
	}
	
	@Override
	public void onClick(View pV) {
		if (pV == mProfileIV) {
			AccountsManager.viewProfile(getContext(), mMsg.account, mComment.sender);
		}
	}
}
