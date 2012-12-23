package net.vvakame.android.fragment.wifidirect;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.util.Log;

/**
 * Wi-Fi Directによる状態変更の通知を受け取るBroadcastReceiver.
 * 
 * @author vvakame
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

	static final String TAG = WiFiDirectBroadcastReceiver.class.getSimpleName();

	WiFiDirectFragment mFragment;

	public WiFiDirectBroadcastReceiver(WiFiDirectFragment fragment) {
		mFragment = fragment;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Log.d(TAG, "onReceive action=" + action);

		if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
			// Wi-Fi Direct 接続状態変更の通知
			int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,
					WifiP2pManager.WIFI_P2P_STATE_DISABLED);

			switch (state) {
			case WifiP2pManager.WIFI_P2P_STATE_ENABLED:
				mFragment.onWiFiDirectStateChanged(true);
				break;
			case WifiP2pManager.WIFI_P2P_STATE_DISABLED:
				mFragment.onWiFiDirectStateChanged(false);
				break;
			default:
				Log.e(TAG, "Unhandled wifi state " + state);
				break;
			}

		} else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
			// Peerの状態が変わった場合の通知
			mFragment.requestPeers();

		} else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION
				.equals(action)) {
			// Wi-Fi Direct接続状態変更の通知
			NetworkInfo networkInfo = (NetworkInfo) intent
					.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

			if (networkInfo.isConnected()) {
				mFragment.requestConnectionInfo();
			} else {
				mFragment.disconnect();
			}

		} else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION
				.equals(action)) {
			// Wi-Fi Direct 自端末の状態変更の通知
			WifiP2pDevice device = (WifiP2pDevice) intent
					.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
			mFragment.onThisDeviceChanged(device);
		}
	}
}
