package com.appsauthority.appwiz.fragments;

/* Copyright (C)
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Kevin Irish Antonio <irish.antonio@yahoo.com>, February 2014
 */
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.appauthority.appwiz.fragments.HomeFragment;
import com.appsauthority.appwiz.EShopDetailActivity;
import com.appsauthority.appwiz.adapters.EShopListAdapter;
import com.appsauthority.appwiz.adapters.FilterListAdapter;
import com.appsauthority.appwiz.custom.MyLocation;
import com.appsauthority.appwiz.custom.MyLocation.LocationResult;
import com.appsauthority.appwiz.models.CategoryObject;
import com.appsauthority.appwiz.models.Product;
import com.appsauthority.appwiz.models.ProductResponse;
import com.appsauthority.appwiz.models.Retailer;
import com.appsauthority.appwiz.utils.Constants;
import com.appsauthority.appwiz.utils.HTTPHandler;
import com.appsauthority.appwiz.utils.Helper;
import com.appsauthority.appwiz.utils.Utils;
import com.google.gson.Gson;
import com.offpeaksale.restaurants.R;

public class EShopListFragment extends Fragment {

	private View view;
	private ProgressDialog progressDialog;
	private ListView listview;
	private TextView tvNoSearchFound;
	private EShopListAdapter adapter;
	public List<Product> productList = new ArrayList<Product>();
	private Context context;
	Retailer retailer;
	List<String> filterOptions;
	FilterListAdapter filterAdapter;
	public CategoryObject category;
	private SharedPreferences spref;

	List<String> listFilterKey;
	int selectedIndex = 0, filterIndex = 0;
	public String searchedKeyWord;
	RelativeLayout rlLocationOption;

	EditText etCurrentLocation;
	AutoCompleteTextView etTargetLocation;

	RadioButton rdCurrentLocation, rdTargetLocation;

	LinearLayout llTabContainer;

	HorizontalScrollView horizontalScrollView;
	int width;
	int selectedTabIndex = 0;

	MyLocation myLocation;
	LocationResult result;
	private double lat, lng;
	String curreentAddess;
	String targetedAddress;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		listFilterKey = new ArrayList<String>();

		listFilterKey.add("rate");
		listFilterKey.add("new");
		listFilterKey.add("low");
		listFilterKey.add("high");
		listFilterKey.add("none");

		context = getActivity();
		retailer = Helper.getSharedHelper().reatiler;
		spref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		if (view != null) {
			ViewGroup parent = (ViewGroup) view.getParent();
			if (parent != null)
				parent.removeView(view);

		} else {
			view = inflater.inflate(R.layout.fragment_eshop_list, container,
					false);
			initailizeView(view);
		}
		myLocation = new MyLocation();
		checkforLocation();
		getAddressFromCoordinate(12.01, 77.0);
		return view;
	}

	void initailizeView(View view) {
		llTabContainer = (LinearLayout) view.findViewById(R.id.llTabContainer);
		horizontalScrollView = (HorizontalScrollView) view
				.findViewById(R.id.horizontalScrollView);
		listview = (ListView) view.findViewById(R.id.lv_eshop);
		tvNoSearchFound = (TextView) view.findViewById(R.id.tvNoSearchFound);
		tvNoSearchFound.setVisibility(View.GONE);
		try {
			Bundle bundle = getArguments();
			if (bundle.containsKey("ID")) {
				int id = bundle.getInt("ID");

				Log.i("ID", id + "");
			}
			if (bundle.containsKey("ProductList")) {
				productList = (ArrayList<Product>) bundle
						.getSerializable("ProductList");
				// loadListView(0);
			}
		} catch (Exception e) {
		}

		listview.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				ImageView image = (ImageView) view.findViewById(R.id.iv_eshop);
				int width = image.getWidth();
				int height = image.getHeight();
				Product product = (Product) parent.getItemAtPosition(position);

				Intent intent = new Intent(context, EShopDetailActivity.class);
				intent.putExtra("product", product);
				startActivity(intent);
			}
		});
		loadListView(0);
		TextView tvFilter = (TextView) view.findViewById(R.id.tvFilter);
		tvFilter.setTypeface(Helper.getSharedHelper().normalFont);
		RelativeLayout rlFilter = (RelativeLayout) view
				.findViewById(R.id.rlFilter);
		rlLocationOption = (RelativeLayout) view
				.findViewById(R.id.rlLocationOption);
		rlFilter.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				showFilterDialog();
			}
		});

		rlLocationOption.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				showLoctionDialog();
			}
		});

		filterOptions = new ArrayList<String>();

		filterOptions.add("Popularity");
		filterOptions.add("Latest");
		filterOptions.add("Lowest Price");
		filterOptions.add("Highest Price");
		filterOptions.add("None");
		if (searchedKeyWord != null) {
			rlFilter.setVisibility(View.GONE);
		}
		new Handler().postDelayed(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				getActivity().runOnUiThread(new Runnable() {
					public void run() {
						initializeTab();

					}
				});

			}
		}, 500);
	}

	public void refreshList() {
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		new AsyncAllProducts().execute();
	}

	public void loadListView(int id) {
		// sqLiteHelper.openDataBase();
		// productList = sqLiteHelper.getProducts(id);
		// sqLiteHelper.close();

		adapter = new EShopListAdapter(context, R.layout.row_eshop, productList);

		listview.setAdapter(adapter);

	}

	void showFilterDialog() {

		try {
			DisplayMetrics metrics = getResources().getDisplayMetrics();
			int width = metrics.widthPixels;

			final Dialog dialog = new Dialog(context);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dialog.setCancelable(false);
			dialog.setContentView(R.layout.dialog_filter);

			dialog.getWindow().setLayout((6 * width) / 7,
					LayoutParams.WRAP_CONTENT);

			RelativeLayout btnClose = (RelativeLayout) dialog
					.findViewById(R.id.btnClose);
			TextView tvFilterTitle = (TextView) dialog
					.findViewById(R.id.tvFilterTitle);
			ListView lvFilter = (ListView) dialog.findViewById(R.id.lvFilter);

			View lineTop = (View) dialog.findViewById(R.id.lineTop);
			lineTop.setBackgroundColor(Color.parseColor("#"
					+ retailer.getHeaderColor()));
			Button btnFilter = (Button) dialog.findViewById(R.id.btnFilter);

			btnFilter.setBackgroundDrawable(Helper.getSharedHelper()
					.getGradientDrawable(retailer.getHeaderColor()));

			btnFilter.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					// TODO Auto-generated method stub
					filterIndex = selectedIndex;
					new AsyncAllProducts().execute();
					dialog.dismiss();
				}
			});

			btnClose.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {

					dialog.dismiss();
				}
			});
			Helper.getSharedHelper().filterIndex = filterIndex;
			btnFilter.setTextColor(Color.parseColor("#"
					+ retailer.getRetailerTextColor()));

			tvFilterTitle.setTypeface(Helper.getSharedHelper().boldFont);

			filterAdapter = new FilterListAdapter(getActivity(),
					R.layout.filter_list_item, filterOptions);
			lvFilter.setAdapter(filterAdapter);

			lvFilter.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int position, long arg3) {
					// TODO Auto-generated method stub
					selectedIndex = position;
					Helper.getSharedHelper().filterIndex = selectedIndex;
					filterAdapter.notifyDataSetChanged();
				}
			});
			;

			dialog.show();
		} catch (Exception e) {
		}

	}

	void showLoctionDialog() {

		try {
			DisplayMetrics metrics = getResources().getDisplayMetrics();
			int width = metrics.widthPixels;

			final Dialog dialog = new Dialog(context);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dialog.setCancelable(true);
			dialog.setContentView(R.layout.dialog_location_option);

			dialog.getWindow().setLayout(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT);

			Button btnFilter = (Button) dialog.findViewById(R.id.btnFilter);

			btnFilter.setBackgroundDrawable(Helper.getSharedHelper()
					.getGradientDrawable(retailer.getHeaderColor()));

			btnFilter.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					// TODO Auto-generated method stub
					filterIndex = selectedIndex;
					new AsyncAllProducts().execute();
					dialog.dismiss();
				}
			});

			rdCurrentLocation = (RadioButton) dialog
					.findViewById(R.id.rdCurrentLocation);
			rdTargetLocation = (RadioButton) dialog
					.findViewById(R.id.rdTargetLocation);
			etCurrentLocation = (EditText) dialog
					.findViewById(R.id.etCurrentLocation);
			etTargetLocation = (AutoCompleteTextView) dialog
					.findViewById(R.id.etTargetLocation);
			etCurrentLocation.setText(curreentAddess);

			rdCurrentLocation.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					rdCurrentLocation.setChecked(true);
					rdTargetLocation.setChecked(false);
				}
			});

			rdTargetLocation.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					rdCurrentLocation.setChecked(false);
					rdTargetLocation.setChecked(true);
				}
			});

			etCurrentLocation.setBackgroundDrawable(Helper.getSharedHelper()
					.getGradientDrawableEditText(retailer.getHeaderColor()));
			etTargetLocation.setBackgroundDrawable(Helper.getSharedHelper()
					.getGradientDrawableEditText(retailer.getHeaderColor()));

			Helper.getSharedHelper().filterIndex = filterIndex;
			btnFilter.setTextColor(Color.parseColor("#"
					+ retailer.getRetailerTextColor()));

			dialog.show();
		} catch (Exception e) {
		}

	}

	private final class AsyncAllProducts extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected void onPreExecute() {
			showLoadingDialog();
			String eshopConatent = null;
			if (category == null) {
				category = new CategoryObject();
				category.id = "279";
				category.category_name = "OffPeakSale";
			}
			if (category != null) {
				eshopConatent = spref.getString(category.id, "");
			}

			if (searchedKeyWord == null && filterIndex == 0
					&& eshopConatent != null
					&& !eshopConatent.equalsIgnoreCase("")) {
				try {
					JSONObject jsonObject = new JSONObject(eshopConatent);
					Boolean status = parseJSON(jsonObject);
					if (status) {
						adapter.clear();
						adapter.addAll(productList);
						adapter.notifyDataSetChanged();
						dismissLoadingDialog();
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {

			}
		}

		@Override
		protected Boolean doInBackground(Void... params) {

			JSONObject param = new JSONObject();
			if (Utils.hasNetworkConnection(getActivity()
					.getApplicationContext())) {
				try {
					param.put(Constants.PARAM_RETAILER_ID,
							Constants.RETAILER_ID);
					if (category != null) {
						param.put(Constants.PARAM_CATEGORY_ID, category.id);
					}
					// if (filterIndex > 0) {
					param.put(Constants.PARAM_FILTER_ID,
							listFilterKey.get(filterIndex));
					// }
					if (searchedKeyWord != null) {
						param.put(Constants.PARAM_KEYWORD_ID, searchedKeyWord);
					}
					JSONObject jsonObject = HTTPHandler.defaultHandler()
							.doPost(Constants.URL_GET_ALL_PRODUCTS, param);

					if (searchedKeyWord == null && jsonObject != null
							&& filterIndex == 0) {
						spref.edit()
								.putString(category.id, jsonObject.toString())
								.commit();
					}
					if (jsonObject != null) {
						return parseJSON(jsonObject);
					}
					return false;
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			} else {
				return false;
			}

		}

		@Override
		protected void onPostExecute(Boolean status) {
			if (status) {
				adapter.clear();
				adapter.addAll(productList);
				adapter.notifyDataSetChanged();
				tvNoSearchFound.setVisibility(View.GONE);
				listview.setVisibility(View.VISIBLE);
			}
			if (productList.size() == 0 && searchedKeyWord != null) {
				tvNoSearchFound.setVisibility(View.VISIBLE);
				listview.setVisibility(View.GONE);

			}
			dismissLoadingDialog();
		}
	}

	Boolean parseJSON(JSONObject jsonObject) {

		Gson gson = new Gson();
		if (jsonObject.has("currency_code")) {
			try {
				String currencyCode = jsonObject.getString("currency_code")
						.toString();
				Helper.getSharedHelper().reatiler.defaultCurrency = currencyCode;
				// Helper.getSharedHelper().currency_code = currencyCode;
				// Helper.getSharedHelper().getCurrencySymbol();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (jsonObject.has("enableRewards")) {
			try {
				String enableRewards = jsonObject.getString("enableRewards")
						.toString();
				Helper.getSharedHelper().reatiler.enableRewards = enableRewards;
				// Helper.getSharedHelper().currency_code = currencyCode;
				// Helper.getSharedHelper().getCurrencySymbol();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (jsonObject.has("enableCreditCode")) {
			try {
				String enableCreditCode = jsonObject.getString(
						"enableCreditCode").toString();
				Helper.getSharedHelper().enableCreditCode = enableCreditCode;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (jsonObject.has("enableRating")) {
			try {
				String enableRating = jsonObject.getString("enableRating")
						.toString();
				Helper.getSharedHelper().enableRating = enableRating;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (jsonObject.has("enableShoppingCart")) {
			try {
				String enableShoppingCart = jsonObject.getString(
						"enableShoppingCart").toString();
				Helper.getSharedHelper().enableShoppingCart = enableShoppingCart;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (jsonObject.has("disablePayment")) {
			try {
				String disablePayment = jsonObject.getString("disablePayment")
						.toString();
				Helper.getSharedHelper().disablePayment = disablePayment;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		ProductResponse data = gson.fromJson(jsonObject.toString(),
				ProductResponse.class);
		if (data == null) {
			return false;
		}

		if (data.getErrorCode() != null && data.getErrorCode().equals("1")) {

			productList = new ArrayList<Product>();

			productList = data.getData();

			for (int index = 0; index < productList.size(); index++) {

				Product product = productList.get(index);
				if (product.getNewPrice().contains(".")) {
					Helper.getSharedHelper().isDecialFromat = true;
					break;
				} else {
					// Helper.getSharedHelper().isDecialFromat = false;
				}
			}

			return true;
		} else {
			return false;
		}
	}

	public void showLoadingDialog() {
		try {
			if (progressDialog == null || !progressDialog.isShowing()) {
				progressDialog = new ProgressDialog(context);
				progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				progressDialog.setMessage("Loading...");
				// progressDialog.setCancelable(false);
				progressDialog.setCanceledOnTouchOutside(false);
				progressDialog.show();
			}
		} catch (Exception ex) {
			Log.e("",
					"Could not display progress dialog because the activity that called it is no longer active");
		}
	}

	public void dismissLoadingDialog() {
		try {
			progressDialog.dismiss();
		} catch (Exception ex) {
			Log.e("",
					"Could not dismiss progress dialog because the activity that called it is no longer active");
		}
	}

	void initializeTab() {

		List<String> tabs = new ArrayList<String>();
		tabs.add("Morning");
		tabs.add("Afternoon");
		tabs.add("Night");
		tabs.add("Midnight");

		llTabContainer.removeAllViews();
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay()
				.getMetrics(displaymetrics);
		int screewidth = displaymetrics.widthPixels;
		if (tabs.size() >= 2) {
			width = (int) (screewidth / 2.5);
			// width = (int) (width - width*.25);
		} else {
			width = (int) (screewidth / 2);
		}

		LayoutInflater inflater = (LayoutInflater) getActivity()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		for (int i = 0; i < tabs.size(); i++) {
			View vwReview = inflater.inflate(R.layout.product_detail_tab_item,
					null);
			RelativeLayout tabView = (RelativeLayout) vwReview
					.findViewById(R.id.item);
			TextView tvTabName = (TextView) tabView
					.findViewById(R.id.tvItemName);
			View underLineView = (View) tabView
					.findViewById(R.id.vwTabUnderline);
			tvTabName.getLayoutParams().width = width;
			tvTabName.setText(tabs.get(i));
			if (i == selectedTabIndex) {
				underLineView.setBackgroundColor(Color.parseColor("#F2"
						+ Helper.getSharedHelper().reatiler.getHeaderColor()));
			} else {
				underLineView.setBackgroundColor(Color.TRANSPARENT);
			}
			llTabContainer.addView(tabView);
			tabView.setTag(i);
			tabView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					int tag = (Integer) v.getTag();
					selectedTabIndex = tag;
					initializeTab();
				}
			});
		}

		horizontalScrollView.smoothScrollTo(width * selectedTabIndex, 0);

	}

	void checkforLocation() {
		LocationResult locationResult = new LocationResult() {
			@Override
			public void gotLocation(Location location) {

				if (location != null) {

					lat = location.getLatitude();
					lng = location.getLongitude();

					Constants.LAT = lat;
					Constants.LNG = lng;
					getAddressFromCoordinate(lat, lng);
					myLocation.cancelTimer();
				} else {

					Criteria criteria = new Criteria();
					LocationManager locMan = (LocationManager) getActivity()
							.getSystemService(Context.LOCATION_SERVICE);
					String curLoc = locMan.getBestProvider(criteria, true);
					location = locMan.getLastKnownLocation(curLoc);
					if (location != null) {

						lat = location.getLatitude();
						lng = location.getLongitude();

						Constants.LAT = lat;
						Constants.LNG = lng;
						getAddressFromCoordinate(lat, lng);
						myLocation.cancelTimer();

					}

				}
			}
		};
		if (myLocation.getLocation(getActivity().getApplicationContext(),
				locationResult)) {

		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle("Information")
					.setMessage("Enable location services")
					.setCancelable(true)
					.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
									showLoctionDialog();
								}
							})
					.setPositiveButton("Enable",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									try {
										Intent gpsOptionsIntent = new Intent(
												android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
										startActivity(gpsOptionsIntent);
									} catch (Exception e) {
										// e.printStackTrace();
									}

								}
							});
			builder.show();
		}

	}

	void getAddressFromCoordinate(double latitude, double longitude) {
		Geocoder geocoder = new Geocoder(context, Locale.getDefault());

		List<Address> addresses;
		try {
			addresses = geocoder.getFromLocation(latitude, longitude,
					1);
			if (addresses.size()>0) {
				Address address = addresses.get(0);
				
				curreentAddess = address.getAddressLine(0);
//				String city = address.getLocality();
//				String state = address.getAdminArea();
//				String zip = address.getPostalCode();
//				String country = address.getCountryName();
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}
}
