package net.vvakame.android.fragment.applist;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.vvakame.android.R;
import net.vvakame.android.fragment.CustomListFragment;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.androidquery.AQuery;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ApplicationListFragment extends CustomListFragment {

	public static interface ApplicationEventCallbackPicker {
		public ApplicationEventCallback getApplicationEventCallback();
	}

	public static interface ApplicationEventCallback {
		public void onApplicationClicked(AppData data);
	}

	/** 表示するアプリのデータを保持するクラス */
	public static class AppData {
		final public String appName;
		final public Drawable icon;
		final public String packageName;
		final public String className;

		public AppData(String appName, Drawable icon, String packageName,
				String className) {
			this.appName = appName;
			this.icon = icon;
			this.packageName = packageName;
			this.className = className;
		}
	}

	ApplicationEventCallback mCallback;

	ApplicationListAdapter mAdapter;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		ApplicationEventCallbackPicker picker;
		if (activity instanceof ApplicationEventCallbackPicker) {
			picker = (ApplicationEventCallbackPicker) activity;
		} else {
			throw new IllegalArgumentException();
		}

		ApplicationEventCallback callback = picker
				.getApplicationEventCallback();
		if (callback == null) {
			throw new IllegalArgumentException();
		}

		mCallback = callback;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
		mAdapter = new ApplicationListAdapter(getActivity());
		setListAdapter(mAdapter);

		setListShown(false);
		getLoaderManager().initLoader(0, null, new DataLoaderCallbacks());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.app_list, container);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		AppData data = (AppData) getListAdapter().getItem(position);
		mCallback.onApplicationClicked(data);
	}

	class ApplicationListAdapter extends ArrayAdapter<AppData> {
		final LayoutInflater mInflater;

		public ApplicationListAdapter(Context context) {
			super(context, 0);
			mInflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;

			if (convertView == null) {
				view = mInflater.inflate(R.layout.application_list_row, parent,
						false);
				mInflater.cloneInContext(getActivity());
			} else {
				view = convertView;
			}

			AppData data = getItem(position);

			AQuery aq = new AQuery(view);
			aq.id(R.id.icon).image(data.icon);
			aq.id(R.id.app_name).text(data.appName);

			return view;
		}
	}

	class DataLoaderCallbacks implements
			LoaderManager.LoaderCallbacks<List<AppData>> {
		@Override
		public Loader<List<AppData>> onCreateLoader(int id, Bundle args) {
			return new ApplicationListLoader(getActivity());
		}

		@Override
		public void onLoadFinished(Loader<List<AppData>> loader,
				List<AppData> data) {

			Collections.sort(data, new Comparator<AppData>() {
				@Override
				public int compare(AppData lhs, AppData rhs) {
					Collator collator = Collator.getInstance();
					int result = collator.compare(lhs.appName, rhs.appName);
					if (result == 0) {
						result = lhs.packageName.compareTo(rhs.packageName);
					}
					return result;
				}
			});

			mAdapter.addAll(data);
			if (isResumed()) {
				setListShown(true);
			} else {
				setListShownNoAnimation(true);
			}
		}

		@Override
		public void onLoaderReset(Loader<List<AppData>> loader) {
			mAdapter.clear();
		}
	}
}
