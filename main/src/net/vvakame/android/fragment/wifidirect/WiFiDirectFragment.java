package net.vvakame.android.fragment.wifidirect;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import net.vvakame.android.R;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Build;
import android.os.Looper;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Wi-Fi Direct接続管理用Fragment. Viewは持たない.
 * 
 * @author vvakame
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class WiFiDirectFragment extends Fragment implements ChannelListener,
		ConnectionInfoListener, PeerListListener {

	static final String TAG = WiFiDirectFragment.class.getSimpleName();

	/** 接続タイプ. サーバかクライアントか */
	public static enum Type {
		SERVER, CLIENT
	}

	/**
	 * コールバック取得用インタフェース
	 * 
	 * @author vvakame
	 */
	public static interface WiFiDirectCallbackPicker {
		public WiFiDirectCallback getWifiDirectActionCallback();
	}

	/**
	 * Wi-Fi Directのイベントコールバック用インタフェース
	 * 
	 * @author vvakame
	 */
	public static interface WiFiDirectCallback {
		/**
		 * 自端末の状態の変更の通知を行う.
		 * 
		 * @param thisDevice
		 *            自端末の情報
		 */
		public void onDeviceInfoChanged(WifiP2pDevice thisDevice);

		/**
		 * Wi-Fi Directが有効になった時の通知を行う.
		 */
		public void onWiFiDirectEnabled();

		/**
		 * Wi-Fi Directが無効になった時の通知を行う.
		 */
		public void onWiFiDirectDisabled();

		/**
		 * Peerの一覧が取得できた時の通知を行う.
		 * 
		 * @param deviceList
		 */
		public void onPeersAvailable(List<WifiP2pDevice> deviceList);

		/**
		 * Peerと接続した時の通知を行う. 同時に接続済Streamを引き渡す.
		 * 
		 * @param type
		 *            自分のタイプ
		 * @param is
		 *            自分→相手の通信用Stream
		 * @param os
		 *            相手→自分の通信用Stream
		 * @param socket
		 *            ソケット. ちゃんと閉じること
		 */
		public void onOpenSocket(Type type, InputStream is, OutputStream os,
				Closeable socket);

		/**
		 * 接続中のPeerと切断した時の通知を行う.
		 */
		public void onDisconnect();
	}

	WifiP2pManager mManager;
	Channel mChannel;

	final IntentFilter mIntentFilter = new IntentFilter();
	BroadcastReceiver mReceiver = null;

	WifiP2pDevice mThisDevice;

	WiFiDirectCallback mCallback;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		setHasOptionsMenu(true);

		WiFiDirectCallbackPicker picker = (WiFiDirectCallbackPicker) activity;
		mCallback = picker.getWifiDirectActionCallback();

		// BroadcastReceiverの準備
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		mIntentFilter
				.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		mIntentFilter
				.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

		// Wi-Fi Direct初期化
		mManager = (WifiP2pManager) activity
				.getSystemService(Context.WIFI_P2P_SERVICE);
		mChannel = mManager.initialize(activity, Looper.getMainLooper(), this);
	}

	@Override
	public void onChannelDisconnected() {
		// 再チャレンジ
		mChannel = mManager.initialize(getActivity(), Looper.getMainLooper(),
				this);
	}

	@Override
	public void onResume() {
		super.onResume();
		mReceiver = new WiFiDirectBroadcastReceiver(this);
		getActivity().registerReceiver(mReceiver, mIntentFilter);
	}

	@Override
	public void onPause() {
		super.onPause();
		getActivity().unregisterReceiver(mReceiver);
	}

	/**
	 * Peerを探す.
	 */
	public void discoverPeers() {
		mManager.discoverPeers(mChannel, null);
	}

	/**
	 * 発見されている各Peerについての情報をリクエストする.
	 */
	public void requestPeers() {
		mManager.requestPeers(mChannel, this);
	}

	/**
	 * 指定されたPeerに接続しにいく.
	 * 
	 * @param targetDevice
	 *            接続先のデバイス
	 */
	public void connect(WifiP2pDevice targetDevice) {
		WifiP2pConfig config = new WifiP2pConfig();
		config.deviceAddress = targetDevice.deviceAddress;
		config.wps.setup = WpsInfo.PBC; // 接続時認証の指定(Push Button Configuration)

		mManager.connect(mChannel, config, null);
	}

	/**
	 * 接続中のPeerから切断する.
	 */
	public void disconnect() {
		mCallback.onDisconnect();
		mManager.removeGroup(mChannel, null);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.action_items, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.wifi_settings) {
			startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Wi-Fi Directの状態変更の設定.
	 * 
	 * @param enabled
	 */
	void onWiFiDirectStateChanged(boolean enabled) {
		if (enabled) {
			mCallback.onWiFiDirectEnabled();
		} else {
			mCallback.onWiFiDirectDisabled();
		}
	}

	/**
	 * 各Peerの状態が分かった時のコールバック.
	 */
	@Override
	public void onPeersAvailable(WifiP2pDeviceList peers) {
		List<WifiP2pDevice> deviceList = new ArrayList<WifiP2pDevice>(
				peers.getDeviceList());
		mCallback.onPeersAvailable(deviceList);
	}

	/**
	 * 接続しているWi-Fi Directについての情報をリクエストする.
	 */
	public void requestConnectionInfo() {
		mManager.requestConnectionInfo(mChannel, this);
	}

	/**
	 * 接続情報が通知されてきた時のコールバック.
	 */
	@Override
	@SuppressWarnings("resource")
	public void onConnectionInfoAvailable(WifiP2pInfo info) {
		if (info.groupFormed && info.isGroupOwner) {
			new WiFiDirectConnection(mCallback).openSocket();
		} else if (info.groupFormed) {
			InetAddress host = info.groupOwnerAddress;
			new WiFiDirectConnection(mCallback, host).openSocket();
		}
	}

	/**
	 * 自端末の状態の設定.
	 * 
	 * @param thisDevice
	 */
	void onThisDeviceChanged(WifiP2pDevice thisDevice) {
		mThisDevice = thisDevice;
		mCallback.onDeviceInfoChanged(thisDevice);
	}
}
