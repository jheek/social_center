package com.jldroid.twook.activities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.jldroid.twook.R;
import com.jldroid.twook.ThemeUtils;
import com.jldroid.twook.model.AccountsManager;
import com.jldroid.twook.model.ColumnInfo;
import com.jldroid.twook.model.ColumnManager;
import com.jldroid.twook.model.ColumnManager.OnColumnsChangeListener;
import com.jldroid.twook.model.ColumnMessagesProvider;
import com.jldroid.twook.model.IAccount;
import com.jldroid.twook.model.facebook.FacebookAccount;
import com.jldroid.twook.model.twitter.TwitterAccount;

public class ColumnsActivity extends SherlockPreferenceActivity implements OnColumnsChangeListener {

	@Override
	protected void onCreate(Bundle pSavedInstanceState) {
		ThemeUtils.setupActivityTheme(this);
		super.onCreate(pSavedInstanceState);
		
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		addPreferencesFromResource(R.xml.columns);
		
		AccountsManager am = AccountsManager.getInstance(getApplicationContext());
		for (int i = 0; i < am.getAccountCount(); i++) {
			IAccount account = am.getAccount(i);
			if (account instanceof TwitterAccount) {
				TwitterAccount ta = (TwitterAccount) account;
				ta.updateLists(null);
			} else if (account instanceof FacebookAccount) {
				FacebookAccount fa = (FacebookAccount) account;
				fa.updateLists(null);
				fa.updateGroups(null);
			}
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		createPrefs();
		ColumnManager.getInstance(this).addListener(this);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		ColumnManager.getInstance(this).removeListener(this);
	}
	
	@Override
	public void onColumnsChanged() {
		createPrefs();
	}
	
	private void createPrefs() {
		final ColumnManager cm = ColumnManager.getInstance(getApplicationContext());
		PreferenceScreen root = getPreferenceScreen();
		Preference infoPref = root.getPreference(0);
		root.removeAll();
		root.addPreference(infoPref);
		
		HashMap<IAccount, ArrayList<ColumnInfo>> map = new HashMap<IAccount, ArrayList<ColumnInfo>>();
		for (int i = 0; i < cm.getColumnCount(); i++) {
			ColumnInfo info = cm.getColumnInfo(i);
			if (map.containsKey(info.getAccount())) {
				map.get(info.getAccount()).add(info);
			} else {
				ArrayList<ColumnInfo> v = new ArrayList<ColumnInfo>();
				v.add(info);
				map.put(info.getAccount(), v);
			}
		}
		
		Iterable<IAccount> keys = map.keySet();
		for (IAccount account : keys) {
			ArrayList<ColumnInfo> columns = map.get(account);
			Collections.sort(columns);
			String name = account != null ? account.getUser().name : getString(R.string.combined);
			int icon = R.drawable.merged_icon;
			if (account instanceof TwitterAccount) {
				icon = R.drawable.twitter_icon;
			} else if (account instanceof FacebookAccount) {
				icon = R.drawable.facebook_icon;
			}
			final int finalIcon = icon;
			PreferenceCategory group = new PreferenceCategory(this) {
				@Override
				protected void onBindView(View pView) {
					super.onBindView(pView);
					findTextViewAndSetIcon(this, pView, finalIcon);
				}
			};
			group.setTitle(name);
			root.addPreference(group);
			group.getView(null, null);
			for (int i = 0; i < columns.size(); i++) {
				final ColumnInfo info = columns.get(i);
				ColumnMessagesProvider provider = info.getProvider();
				final OnCheckedChangeListener listener = new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton pButtonView, boolean pIsChecked) {
						cm.setColumnEnabled(info, pIsChecked);
					}
				};
				CheckBoxPreference pref = new CheckBoxPreference(this) {
					@Override
					protected void onBindView(View pView) {
						super.onBindView(pView);
						findCheckBoxAndSetListener(pView, listener);
					}
				};
				pref.setPersistent(false);
				pref.setTitle(provider.getName(this));
				pref.setSummary(provider.getDescription(this));
				pref.setChecked(info.isEnabled());
				pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference pPreference) {
						Intent intent = new Intent(getApplicationContext(), ColumnPrefsActivity.class);
						intent.putExtra(ColumnPrefsActivity.EXTRA_COLUMN_INDEX, cm.getColumnIndex(info));
						startActivity(intent);
						return true;
					}
				});
				
				group.addPreference(pref);
			}
		}
	}
	
	protected static boolean findCheckBoxAndSetListener(View v, OnCheckedChangeListener listener) {
		if (v instanceof CheckBox) {
			CheckBox cb = (CheckBox) v;
			cb.setOnCheckedChangeListener(listener);
			cb.setClickable(true);
			return true;
		} 
		if (v instanceof ViewGroup) {
			ViewGroup vg = (ViewGroup) v;
			for (int i = 0; i < vg.getChildCount(); i++) {
				if (findCheckBoxAndSetListener(vg.getChildAt(i), listener)) {
					return true;
				}
			}
		}
		return false;
	}
	
	protected static boolean findTextViewAndSetIcon(Preference pref, View v, int icon) {
		if (v instanceof TextView) {
			TextView tv = (TextView) v;
			if (tv.getText().length() == pref.getTitle().length()) {
				tv.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);
				tv.setCompoundDrawablePadding((int) (5 * v.getResources().getDisplayMetrics().density));
				return true;
			}
		} 
		if (v instanceof ViewGroup) {
			ViewGroup vg = (ViewGroup) v;
			for (int i = 0; i < vg.getChildCount(); i++) {
				if (findTextViewAndSetIcon(pref, vg.getChildAt(i), icon)) {
					return true;
				}
			}
		}
		return false;
	}
	
}
