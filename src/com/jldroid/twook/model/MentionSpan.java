package com.jldroid.twook.model;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Toast;

import com.jdroid.utils.Threads;
import com.jldroid.twook.R;
import com.jldroid.twook.activities.MainActivity;
import com.jldroid.twook.activities.ViewProfileActivity;
import com.jldroid.twook.fragments.ViewProfileFragment;
import com.jldroid.twook.model.facebook.FacebookAccount;
import com.jldroid.twook.model.twitter.TwitterAccount;

public class MentionSpan extends ClickableSpan {

	private IAccount mAccount;
	private long mId;
	
	private String mText;
	
	
	public MentionSpan(IAccount account, long id, String text) {
		mText = text;
		mId = id;
		mAccount = account;
	}
	
	@Override
	public void onClick(View pWidget) {
		final Activity a = (Activity) pWidget.getContext();
		if (!AccountsManager.getInstance(a).viewProfile(a, mAccount.getUser().type, mId)) {
			final ProgressDialog prog = ProgressDialog.show(a, a.getString(R.string.pb_title_loading), a.getString(R.string.pb_msg_please_wait), true, true);
			Threads.runOnNetworkThread(new Runnable() {
				@Override
				public void run() {
					final User user = mAccount.loadUserInfo(mId);
					Threads.runOnUIThread(new Runnable() {
						@Override
						public void run() {
							if (prog.isShowing()) {
								prog.dismiss();
								if (user != null) {
									AccountsManager.viewProfile(a, mAccount, user);
								} else {
									Toast.makeText(a.getApplicationContext(), R.string.failed_load_user_info, Toast.LENGTH_LONG).show();
								}
							}
						}
					});
				}
			});
		}
	}
	

}
