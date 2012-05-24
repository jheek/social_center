package com.jldroid.twook.activities;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.facebook.android.Util;
import com.jdroid.utils.Threads;
import com.jldroid.twook.R;
import com.jldroid.twook.model.AccountsManager;
import com.jldroid.twook.model.facebook.FacebookAccount;
import com.jldroid.twook.model.facebook.FacebookConfiguration;

public class AddFacebookAccountActivity extends SherlockActivity implements DialogListener {
	
	public static final String EXTRA_RESULT = "com.jldroid.twook.RESULT";
	public static final String EXTRA_USERID = "com.jldroid.twook.USERID";
	
	protected Facebook mFacebook;
	
	protected WebView mWebView;
	
	@Override
	protected void onCreate(Bundle pSavedInstanceState) {
		super.onCreate(pSavedInstanceState);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		setContentView(mWebView = new WebView(this));
		mFacebook = new Facebook(FacebookConfiguration.APP_ID);
		
		mWebView.setVerticalScrollBarEnabled(false);
        mWebView.setHorizontalScrollBarEnabled(false);
        mWebView.setWebChromeClient(new WebChromeClient() {
        	@Override
        	public void onProgressChanged(WebView pView, int pNewProgress) {
        		setSupportProgress( Window.PROGRESS_START + (Window.PROGRESS_END - Window.PROGRESS_START) / 100 * pNewProgress);
        	}
        });
        mWebView.setWebViewClient(new WebViewClient() {
        	@Override
        	public void onPageStarted(WebView pView, String pUrl, Bitmap pFavicon) {
        		super.onPageStarted(pView, pUrl, pFavicon);
        	}
        	@Override
        	public void onPageFinished(WebView pView, String pUrl) {
        		super.onPageFinished(pView, pUrl);
        	}
        	
        	 @Override
             public boolean shouldOverrideUrlLoading(WebView view, String url) {
                 if (url.startsWith(Facebook.REDIRECT_URI)) {
                     Bundle values = Util.parseUrl(url);

                     String error = values.getString("error");
                     if (error == null) {
                         error = values.getString("error_type");
                     }

                     if (error == null) {
                    	 CookieSyncManager.getInstance().sync();
                         mFacebook.setAccessToken(values.getString(Facebook.TOKEN));
                         mFacebook.setAccessExpiresIn(values.getString(Facebook.EXPIRES));
                         if (mFacebook.isSessionValid()) {
                             onComplete(values);
                         } else {
                             onFacebookError(new FacebookError(
                                             "Failed to receive access token."));
                         }
                     } else if (error.equals("access_denied") ||
                                error.equals("OAuthAccessDeniedException")) {
                    	 onCancel();
                     } else {
                    	 onFacebookError(new FacebookError(error));
                     }

                     return true;
                 } else if (url.startsWith(Facebook.CANCEL_URI)) {
                	 onCancel();
                     return true;
                 } else if (url.contains("touch")) {
                     return false;
                 }
                 
                 startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                 return true;
             }
        });
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.loadUrl(generateAuthUrl());
		
		CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
		
        Toast.makeText(getApplicationContext(), R.string.loading_fb_auth, Toast.LENGTH_LONG).show();
	}
	
	@Override
	public void onFacebookError(FacebookError pE) {
		Toast.makeText(getApplicationContext(), R.string.error_cannot_add_fb, Toast.LENGTH_LONG).show();
		finish();
	}
	@Override
	public void onError(DialogError pE) {
		Toast.makeText(getApplicationContext(), R.string.error_cannot_add_fb, Toast.LENGTH_LONG).show();
		finish();
	}
	@Override
	public void onComplete(final Bundle pValues) {
		final ProgressDialog prog = ProgressDialog.show(this, getString(R.string.adding_fb_dialog_title), getString(R.string.pb_msg_please_wait), true, false);
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				try {
					final JSONObject json = new JSONObject(mFacebook.request("me"));
					Threads.runOnUIThread(new Runnable() {
						@Override
						public void run() {
							prog.dismiss();
							try {
								String accessToken = pValues.getString("access_token");
								long userID = json.getLong("id");
								String userName = json.getString("name");
								AccountsManager.getInstance(getApplicationContext()).addFacebookAccount(new FacebookAccount(getApplicationContext(), userID, userName, accessToken));
							} catch (JSONException e) {
								e.printStackTrace();
								Toast.makeText(getApplicationContext(), R.string.unexpected_error, Toast.LENGTH_LONG).show();
							}
							finish();
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
					Threads.runOnUIThread(new Runnable() {
						@Override
						public void run() {
							prog.dismiss();
							Toast.makeText(getApplicationContext(), R.string.error_check_network, Toast.LENGTH_LONG).show();
							finish();
						}
					});
				} 
			}
		});
	}
	@Override
	public void onCancel() {
		finish();
	}
	
	private String generateAuthUrl() {
		Bundle params = new Bundle();
		params.putString("display", "touch");
		params.putString("redirect_uri", Facebook.REDIRECT_URI);
        params.putString("scope", TextUtils.join(",", FacebookConfiguration.PERMISSIONS));
        params.putString("type", "user_agent");
        params.putString("client_id", FacebookConfiguration.APP_ID);
        return "https://m.facebook.com/dialog/oauth?" + Util.encodeUrl(params);
	}
	
}
