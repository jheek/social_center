package com.jldroid.twook.view;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.jldroid.twook.model.User;
import java.util.ArrayList;
import com.jdroid.utils.SortedArrayList;

public class UserAdapter extends BaseAdapter implements SectionIndexer {

	private Context mContext;
	protected SortedArrayList<User> mUsers;
	private int mSize;
	
	private String[] mSections;
	
	public UserAdapter(Context c, User[] users) {
		this(c, new SortedArrayList<User>(users));
	}
	
	public UserAdapter(Context c, SortedArrayList<User> users) {
		mContext = c;
		mUsers = users;
		mSize = (int) (c.getResources().getDisplayMetrics().density * 48);
	}
	
	@Override
	public int getCount() {
		return mUsers.size();
	}

	@Override
	public User getItem(int pPosition) {
		return mUsers.get(pPosition);
	}

	@Override
	public long getItemId(int pPosition) {
		return getItem(pPosition).id;
	}

	@Override
	public View getView(int pPosition, View pConvertView, ViewGroup pParent) {
		User user = getItem(pPosition);
		TextView tv = (TextView) pConvertView;
		if (tv == null) {
			tv = new TextView(mContext);
			tv.setGravity(Gravity.CENTER_VERTICAL);
			tv.setTextSize(17);
			ProfileImageDrawable pid = new ProfileImageDrawable(mContext);
			pid.setBounds(0, 0, mSize, mSize);
			tv.setCompoundDrawables(pid, null, null, null);
		}
		tv.setTag(user);
		tv.setText(user.name);
		ProfileImageDrawable pid = (ProfileImageDrawable) tv.getCompoundDrawables()[0];
		pid.setUser(mContext, user, false);
		
		return tv;
	}

	@Override
	public int getPositionForSection(int pSection) {
		final int l = mUsers.size();
		final String letter = mSections[pSection];
		for (int i = 0; i < l; i++) {
			String firstLetter = mUsers.get(i).name.substring(0, 1);
			if (firstLetter.equalsIgnoreCase(letter)) {
				return i;
			}
		}
		throw new RuntimeException("This is impossible");
	}
	
	@Override
	public int getSectionForPosition(int pPosition) {
		String letter = mUsers.get(pPosition).name.substring(0, 1);
		final int l = mSections.length;
		for (int i = 0; i < l; i++) {
			if (letter.equalsIgnoreCase(mSections[i])) {
				return i;
			}
		}
		throw new RuntimeException("This is impossible");
	}
	
	@Override
	public Object[] getSections() {
		if (mSections == null) {
			ArrayList<String> letters = new ArrayList<String>(26);
			String prevChar = "";
			for (int i = 0; i < mUsers.size(); i++) {
				String firstChar = mUsers.get(i).name.substring(0, 1);
				if (!prevChar.equalsIgnoreCase(firstChar)) {
					letters.add(firstChar.toUpperCase());
					prevChar = firstChar;
				}
			}
			String[] ar = new String[letters.size()];
			for (int i = 0; i < letters.size(); i++) {
				ar[i] = letters.get(i);
			}
			mSections = ar;
		}
		return mSections;
	}
	
	@Override
	public void notifyDataSetChanged() {
		mSections = null;
		super.notifyDataSetChanged();
	}
	
}
