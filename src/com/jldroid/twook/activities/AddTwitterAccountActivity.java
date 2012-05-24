package com.jldroid.twook.activities;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.jdroid.utils.Threads;
import com.jldroid.twook.R;
import com.jldroid.twook.ThemeUtils;
import com.jldroid.twook.model.AccountsManager;
import com.jldroid.twook.model.twitter.TwitterAccount;
import com.jldroid.twook.model.twitter.TwitterConfiguration;

public class AddTwitterAccountActivity extends SherlockActivity {
	
private static final String TWITTER_CALLBACK_URL = "http://www.result.com";
	
	protected Twitter mTwitter;
	protected RequestToken mRequestToken;
	
	protected WebView mWebView;
	
	protected ProgressDialog mWaitDialog;
	
	
	@Override
	protected void onCreate(Bundle pSavedInstanceState) {
		super.onCreate(pSavedInstanceState);
		ThemeUtils.setupActivityTheme(this);
		
		mWebView = new WebView(this);
		setContentView(mWebView);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		//mWebView.addJavascriptInterface(new JavascriptInterface(), "PINOUT");
		mWebView.setWebViewClient(new WebViewClient() {
        	@Override
        	public boolean shouldOverrideUrlLoading(WebView view, String url) {
        		view.loadUrl(url);
        		return true;
        	}
        	
        	@Override
        	public void onPageStarted(WebView view, String url, Bitmap favicon) {
        		super.onPageStarted(view, url, favicon);
        		if (url.startsWith(TWITTER_CALLBACK_URL)) {
        			mWebView.stopLoading();
        			mWebView.setVisibility(View.INVISIBLE);
        			Uri uri = Uri.parse(url);
        			String token = uri.getQueryParameter("oauth_token");
        			String verifier = uri.getQueryParameter("oauth_verifier");
        			mWaitDialog = ProgressDialog.show(AddTwitterAccountActivity.this, 
        					getString(R.string.add_twitter_dialog_title), 
        					getString(R.string.pb_msg_please_wait), 
        					true, 
        					false);
        			parseResult(token, verifier);
        		}
        	}
	    });
		loadAuthPage();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mWebView.clearCache(true);
	}
	
	public void loadAuthPage() {
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				try {
		            mTwitter = new TwitterFactory().getInstance();
		            mTwitter.setOAuthConsumer(TwitterConfiguration.OAUTH_CONSUMER_KEY, TwitterConfiguration.OAUTH_CONSUMER_SECRET);
		            mRequestToken = mTwitter.getOAuthRequestToken(TWITTER_CALLBACK_URL);
		            Threads.runOnUIThread(new Runnable() {
						@Override
						public void run() {
							mWebView.loadUrl(mRequestToken.getAuthorizationURL());
				            mWebView.requestFocus();
						}
					});
		        } catch (TwitterException e) {
		        	e.printStackTrace();
		        }
			}
		});
	}
    
	public void parseResult(final String oauthToken, final String verifier) {
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				try {
					final AccessToken token = mTwitter.getOAuthAccessToken(mRequestToken, verifier);
    				User user = mTwitter.verifyCredentials();
    				AccountsManager.getInstance(AddTwitterAccountActivity.this).addTwitterAccount(new TwitterAccount(AddTwitterAccountActivity.this.getApplicationContext(), 
    								user.getId(), user.getName(), user.getProfileImageURL().toString(), 
    								token.getToken(), token.getTokenSecret()));
    				if (mWaitDialog != null) {
    					mWaitDialog.dismiss();
    					mWaitDialog = null;
    				}
					Threads.runOnUIThread(new Runnable() {
						@Override
						public void run() {
							finish();
						}
					});
				} catch (TwitterException e) {
					e.printStackTrace();
					if (mWaitDialog != null) {
    					mWaitDialog.dismiss();
    					mWaitDialog = null;
    				}
					Threads.runOnUIThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(getApplicationContext(), R.string.failed_add_twitter, Toast.LENGTH_LONG).show();
							finish();
						}
					});
				}
			}
		});
	}
}
