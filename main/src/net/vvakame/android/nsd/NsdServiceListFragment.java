package net.vvakame.android.nsd;

import java.util.List;

import net.vvakame.android.R;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * {@link NsdServiceInfo} を一覧表示するための {@link ListFragment}。
 * あんまり使わないほうがいいと思います。
 * @author vvakame
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class NsdServiceListFragment extends ListFragment {
	/**
	 * コールバック取得用インタフェース。
	 * 
	 * @author vvakame
	 */
	public static interface NsdServiceListEventCallbackPicker {
		public NsdServiceListEventCallback getCallback(
				NsdServiceListFragment nsdServiceListFragment);
	}

	/**
	 * リストのイベントコールバック用インタフェース。
	 * 
	 * @author vvakame
	 */
	public static interface NsdServiceListEventCallback {
		public void onItemClicked(NsdServiceInfo data);
	}

	NsdServiceListEventCallback mCallback;

	NsdServiceListAdapter mAdapter;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (activity instanceof NsdServiceListEventCallback) {
			mCallback = (NsdServiceListEventCallback) activity;
		} else if (activity instanceof NsdServiceListEventCallbackPicker) {
			NsdServiceListEventCallbackPicker picker = (NsdServiceListEventCallbackPicker) activity;
			mCallback = picker.getCallback(this);
		}
		if (mCallback == null) {
			throw new NullPointerException(
					"can't get NsdListEventCallback from activity.");
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.service_not_found));
		mAdapter = new NsdServiceListAdapter(getActivity());
		setListAdapter(mAdapter);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		NsdServiceInfo data = (NsdServiceInfo) getListAdapter().getItem(
				position);
		mCallback.onItemClicked(data);
	}

	/**
	 * 表示する {@link NsdServiceInfo} のリストを更新する。
	 * 
	 * @param list
	 *            新しい {@link NsdServiceInfo} のリスト
	 */
	public void refreshList(final List<NsdServiceInfo> list) {
		mAdapter.clear();
		mAdapter.addAll(list);
	}

	static class NsdServiceListAdapter extends ArrayAdapter<NsdServiceInfo> {
		final LayoutInflater mInflater;

		public NsdServiceListAdapter(Context context) {
			super(context, 0);
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;

			if (convertView == null) {
				view = mInflater.inflate(R.layout.service_row, parent, false);
			} else {
				view = convertView;
			}

			NsdServiceInfo data = getItem(position);
			TextView textView = (TextView) view.findViewById(R.id.name);
			textView.setText(data.getServiceName());

			return view;
		}
	}
}
