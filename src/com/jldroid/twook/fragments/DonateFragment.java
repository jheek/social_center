package com.jldroid.twook.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.jdroid.utils.Threads;
import com.jldroid.twook.BillingService;
import com.jldroid.twook.BillingService.RequestPurchase;
import com.jldroid.twook.BillingService.RestoreTransactions;
import com.jldroid.twook.Consts;
import com.jldroid.twook.Consts.PurchaseState;
import com.jldroid.twook.Consts.ResponseCode;
import com.jldroid.twook.PurchaseObserver;
import com.jldroid.twook.R;
import com.jldroid.twook.ResponseHandler;

public class DonateFragment extends SherlockListFragment implements OnItemClickListener {

	private PurchaseObserver mObserver;
	
	private BillingService mService;
	
    /** An array of product list entries for the products that can be purchased. */
    private static final CatalogEntry[] CATALOG = new CatalogEntry[] {
        new CatalogEntry("donation_0100", R.string.donation_100, Managed.UNMANAGED),
        new CatalogEntry("donation_250", R.string.donation_250, Managed.UNMANAGED),
        new CatalogEntry("donation_500",R.string.donation_500, Managed.UNMANAGED),
        new CatalogEntry("donation_1000", R.string.donation_1000, Managed.UNMANAGED)
    };
	
	@Override
	public View onCreateView(LayoutInflater pInflater, ViewGroup pContainer, Bundle pSavedInstanceState) {
		return pInflater.inflate(R.layout.donate, null);
	}
	
	@Override
	public void onActivityCreated(Bundle pSavedInstanceState) {
		super.onActivityCreated(pSavedInstanceState);
		mService = new BillingService();
		mService.setContext(getActivity());
		
		mObserver = new MyObserver(getActivity(), Threads.getUIHandler());
		
		if (!mService.checkBillingSupported()) {
			getActivity().onBackPressed();
			Toast.makeText(getActivity().getApplicationContext(), "You can't donate because your device doesn't support in-app billing", Toast.LENGTH_LONG).show();
			return;
		}
		
		getListView().setAdapter(new MyAdapter());
		getListView().setOnItemClickListener(this);
	}
	
	@Override
	public void onItemClick(AdapterView<?> pParent, View pView, int pPosition, long pId) {
		CatalogEntry item = CATALOG[pPosition];
		if (!mService.requestPurchase(item.sku, null)) {
			Toast.makeText(getActivity(), "An unexpected error ocurred!", Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		ResponseHandler.register(mObserver);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		ResponseHandler.unregister(mObserver);
	}
	
	private class MyAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return CATALOG.length;
		}

		@Override
		public CatalogEntry getItem(int pPosition) {
			return CATALOG[pPosition];
		}

		@Override
		public long getItemId(int pPosition) {
			return getItem(pPosition).sku.hashCode();
		}

		@Override
		public View getView(int pPosition, View pConvertView, ViewGroup pParent) {
			CatalogEntry item = getItem(pPosition);
			TextView tv = (TextView) pConvertView;
			if (tv == null) {
				tv = new TextView(getActivity());
				int p = (int) (getActivity().getResources().getDisplayMetrics().density * 5);
				tv.setPadding(p, p * 2, p * 2, p);
			}
			tv.setText(item.nameId);
			return tv;
		}
		
	}
	
	private class MyObserver extends PurchaseObserver {

		public MyObserver(Activity pActivity, Handler pHandler) {
			super(pActivity, pHandler);
		}

		@Override
		public void onBillingSupported(boolean pSupported) {
		}

		@Override
		public void onPurchaseStateChange(PurchaseState pPurchaseState, String pItemId, int pQuantity, long pPurchaseTime, String pDeveloperPayload) {
			
		}

		@Override
		public void onRequestPurchaseResponse(RequestPurchase request, ResponseCode responseCode) {
			if (Consts.DEBUG) {
                Log.d("JDROID", request.mProductId + ": " + responseCode);
            }
            if (responseCode == ResponseCode.RESULT_OK) {
                if (Consts.DEBUG) {
                    Log.i("JDROID", "purchase was successfully sent to server");
                }
                Toast.makeText(getActivity().getApplicationContext(), "Thanks for your donation!", Toast.LENGTH_LONG).show();
            } else if (responseCode == ResponseCode.RESULT_USER_CANCELED) {
                if (Consts.DEBUG) {
                    Log.i("JDROID", "user canceled purchase");
                }
                Toast.makeText(getActivity().getApplicationContext(), "You canceled your donation", Toast.LENGTH_LONG).show();
            } else {
                if (Consts.DEBUG) {
                    Log.i("JDROID", "purchase failed");
                }
                Toast.makeText(getActivity().getApplicationContext(), "Failed to donate!", Toast.LENGTH_LONG).show();
            }
		}

		@Override
		public void onRestoreTransactionsResponse(RestoreTransactions pRequest, ResponseCode pResponseCode) {
			
		}
		
	}
	
	private static class CatalogEntry {
		
        public String sku;
        public int nameId;
        public Managed managed;

        public CatalogEntry(String sku, int nameId, Managed managed) {
            this.sku = sku;
            this.nameId = nameId;
            this.managed = managed;
        }
    }
	
	private enum Managed { MANAGED, UNMANAGED }
	
}
