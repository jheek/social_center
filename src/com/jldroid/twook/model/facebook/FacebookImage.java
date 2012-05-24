package com.jldroid.twook.model.facebook;

import java.io.IOException;
import java.net.MalformedURLException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.text.TextUtils;

import java.util.ArrayList;
import com.jdroid.utils.StorageManager.StorageBundle;

public class FacebookImage {
	
	public String href;
	public String src;
	public String bigSrc;
	
	public String pid;
	
	public int width = -1;
	public int height = -1;
	
	private StorageBundle mBundle;
	
	public FacebookImage(StorageBundle bundle) {
		this.mBundle = bundle;
		src = bundle.readString("SRC", null);
		href = bundle.readString("HREF", null);
		bigSrc = bundle.readString("BIG_SRC", null);
		pid = bundle.readString("PID", null);
		width = bundle.readInt("WIDTH", -1);
		height = bundle.readInt("HEIGHT", -1);
	}
	
	public FacebookImage(String src, String href, String pid, int w, int h) {
		this.mBundle = new StorageBundle(6);
		this.src = src;
		this.href = href;
		this.pid = pid;
		this.width = w;
		this.height = h;
	}
	
	public StorageBundle getBundle() {
		return mBundle;
	}
	
	public StorageBundle updateBundle() {
		mBundle.deleteAll();
		mBundle.write("SRC", src);
		mBundle.write("HREF", href);
		mBundle.write("BIG_SRC", bigSrc);
		mBundle.write("PID", pid);
		mBundle.write("WIDTH", width);
		mBundle.write("HEIGHT", height);
		return mBundle;
	}
	
	public static boolean loadBigSrcs(ArrayList<FacebookImage> imges, FacebookAccount account) {
		boolean dirtyMessages = false;
		String query = null;
		for (int i = 0; i < imges.size(); i++) {
			FacebookImage img = imges.get(i);
			if (TextUtils.isEmpty(img.bigSrc) && !TextUtils.isEmpty(img.pid)) {
				if (query == null) {
					query = "SELECT pid, src_big FROM photo WHERE pid='" + img.pid + "'";
				} else {
					query += " OR pid='" + img.pid + "'";
				}
			}
		}
		if (query != null) {
			Bundle bundle = new Bundle(3);
			bundle.putString("q", query);
			try {
				JSONObject json = new JSONObject(account.getFacebook().request("fql", bundle));
				JSONArray array = json.getJSONArray("data");
				for (int i = 0; i < array.length(); i++) {
					JSONObject obj = array.getJSONObject(i);
					String pid = obj.getString("pid");
					String bigSrc = obj.getString("src_big");
					for (int i2 = 0; i2 < imges.size(); i2++) {
						FacebookImage img = imges.get(i2);
						if (pid.equals(img.pid)) {
							img.bigSrc = bigSrc;
							img.updateBundle();
							dirtyMessages = true;
							break;
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		if (dirtyMessages) {
			account.getStorageManager().flushAsync(10000);
		}
		return true;
	}
}
