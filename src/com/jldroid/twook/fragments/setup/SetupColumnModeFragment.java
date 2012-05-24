package com.jldroid.twook.fragments.setup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.jldroid.twook.R;
import com.jldroid.twook.model.ColumnManager;

public class SetupColumnModeFragment extends SherlockListFragment implements ISetupFragment {

	private static final int[] OPTIONS = {R.string.merged, R.string.split};
	
	@Override
	public View onCreateView(LayoutInflater pInflater, ViewGroup pContainer, Bundle pSavedInstanceState) {
		return pInflater.inflate(R.layout.setup_column_mode, null);
	}
	
	@Override
	public void onActivityCreated(Bundle pSavedInstanceState) {
		super.onActivityCreated(pSavedInstanceState);
		String[] options = new String[OPTIONS.length];
		for (int i = 0; i < options.length; i++) {
			options[i] = getString(OPTIONS[i]);
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_single_choice, options);
		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		getListView().setAdapter(adapter);
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
		ColumnManager cm = ColumnManager.getInstance(getActivity());
		cm.setupColumns(getListView().getCheckedItemPosition() == 0);
	}
	
}
