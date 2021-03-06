package com.jldroid.twook.fragments.setup;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.actionbarsherlock.app.SherlockListFragment;
import com.jldroid.twook.R;

public class SetupUpdateIntervalFragment extends SherlockListFragment implements ISetupFragment {

	@Override
	public View onCreateView(LayoutInflater pInflater, ViewGroup pContainer, Bundle pSavedInstanceState) {
		return pInflater.inflate(R.layout.setup_update_interval, null);
	}
	
	@Override
	public void onActivityCreated(Bundle pSavedInstanceState) {
		super.onActivityCreated(pSavedInstanceState);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_single_choice, getActivity().getResources().getStringArray(R.array.default_sync_intervals));
		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		getListView().setAdapter(adapter);
		getListView().setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> pParent, View pView, int pPosition, long pId) {
				PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
					.putLong("defaultSyncInterval", Long.parseLong(getActivity().getResources().getStringArray(R.array.default_sync_interval_values)[pPosition]))
					.commit();
			}
		});
	}
	
	@Override
	public boolean isProceedAllowed() {
		if (getListView().getCheckedItemPosition() == ListView.INVALID_POSITION) {
			Toast.makeText(getActivity(), R.string.choose_an_option, Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}
	
	@Override
	public void onProceed() {
	}
	
}
