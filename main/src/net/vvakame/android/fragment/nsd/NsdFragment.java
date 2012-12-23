package net.vvakame.android.fragment.nsd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdManager.DiscoveryListener;
import android.net.nsd.NsdManager.RegistrationListener;
import android.net.nsd.NsdManager.ResolveListener;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * NSD 周りの管理を行う {@link Fragment}。自身のポートを開いてサービスとして登録、他サービスの発見、接続などを行う。
 * 一回切断するたびに、一回Fragmentを捨てたほうがいいです。(上手いこと再接続できない…)
 * @author vvakame
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class NsdFragment extends Fragment {

	public static final String TAG = NsdFragment.class.getSimpleName();

	final NsdFragment self = this;

	/**
	 * コールバック取得用インタフェース。
	 * 
	 * @author vvakame
	 */
	public interface NsdEventCallbackPicker {
		public NsdEventCallback getCallback(NsdFragment nsdFragment);
	}

	/**
	 * NSD関連イベントのイベントコールバック用インタフェース。
	 * 
	 * @author vvakame
	 * 
	 */
	public interface NsdEventCallback {
		/**
		 * 端末のサービス名を取得する。
		 * 
		 * @return
		 */
		public String getServiceName();

		/**
		 * NSDのサービス発見状況に変更があった時のコールバック。
		 * 
		 * @param serviceList
		 *            更新後のサービス一覧
		 */
		public void onUpdate(List<NsdServiceInfo> serviceList);

		/**
		 * 自分がサーバとして接続された場合のコールバック。
		 * 
		 * @param send
		 *            送信用 {@link OutputStream}
		 * @param recv
		 *            受信用 {@link InputStream}
		 */
		public void onConnectAsServer(OutputStream send, InputStream recv);

		/**
		 * 自分がクライアントとして接続した場合のコールバック。
		 * 
		 * @param send
		 *            送信用 {@link OutputStream}
		 * @param recv
		 *            受信用 {@link InputStream}
		 */
		public void onConnectAsClient(OutputStream send, InputStream recv);

		/**
		 * 接続後、 Fragment を detach した方がいい場合のコールバック
		 * 
		 * @param nsdFragment
		 */
		public void onRecommendedToDetach(NsdFragment nsdFragment);
	}

	NsdManager mNsdManager;

	DiscoveryListener mDiscoveryListener;
	RegistrationListener mRegistrationListener;

	Handler mHandler = new Handler(Looper.getMainLooper());
	NsdEventCallback mCallback;

	/** 既知のサービス一覧 */
	List<NsdServiceInfo> mServiceList = new ArrayList<NsdServiceInfo>();

	/** サービスタイプ */
	static String sServiceType;

	/** 自身がサーバとして動作する時の受け待ち用ソケット */
	P2pServer mConnection = null;
	/** サービス名。端末独自の名前 (+ システムの都合)。 */
	String mServiceName;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (sServiceType == null) {
			sServiceType = "_"
					+ activity.getPackageName().replaceAll("\\.", "-")
					+ "._tcp.";
			Log.d(TAG, sServiceType);
		}

		if (activity instanceof NsdEventCallback) {
			mCallback = (NsdEventCallback) activity;
		} else if (activity instanceof NsdEventCallbackPicker) {
			NsdEventCallbackPicker picker = (NsdEventCallbackPicker) activity;
			mCallback = picker.getCallback(this);
		}
		if (mCallback == null) {
			throw new NullPointerException(
					"can't get NsdEventCallback from activity.");
		}

		mNsdManager = (NsdManager) activity
				.getSystemService(Context.NSD_SERVICE);

		mServiceName = mCallback.getServiceName();
		mConnection = new P2pServer(this); // 内部で registerService を呼び出す

		mDiscoveryListener = generateDiscoveryListener();
		mNsdManager.discoverServices(sServiceType, NsdManager.PROTOCOL_DNS_SD,
				mDiscoveryListener);
	}

	@Override
	public void onDetach() {
		if (mRegistrationListener != null) {
			mNsdManager.unregisterService(mRegistrationListener);
			mConnection.tearDown();
			mConnection = null;
		}
		mNsdManager.stopServiceDiscovery(mDiscoveryListener);

		super.onDetach();
	}

	/**
	 * 指定された {@link NsdServiceInfo} を解決し、接続を行います。
	 * 
	 * @param data
	 */
	public void connect(NsdServiceInfo data) {
		ResolveListener listener = generateResolveListener();
		mNsdManager.resolveService(data, listener);
	}

	void registerService(int port) {
		if (mServiceName == null) {
			throw new IllegalStateException();
		}
		NsdServiceInfo serviceInfo = new NsdServiceInfo();
		serviceInfo.setPort(port);
		serviceInfo.setServiceName(mServiceName);
		serviceInfo.setServiceType(sServiceType);

		Log.d(TAG, "registerService service=" + serviceInfo);

		mRegistrationListener = generateRegistrationListener();
		mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD,
				mRegistrationListener);
	}

	RegistrationListener generateRegistrationListener() {
		return new NsdManager.RegistrationListener() {

			@Override
			public void onServiceRegistered(NsdServiceInfo service) {
				Log.d(TAG, "RegistrationListener#onServiceRegistered service="
						+ service);
			}

			@Override
			public void onRegistrationFailed(NsdServiceInfo service,
					int errorCode) {
				Log.d(TAG,
						"RegistrationListener#onRegistrationFailed errorCode="
								+ errorCode);
			}

			@Override
			public void onServiceUnregistered(NsdServiceInfo service) {
				Log.d(TAG,
						"RegistrationListener#onServiceUnregistered service="
								+ service);
			}

			@Override
			public void onUnregistrationFailed(NsdServiceInfo service,
					int errorCode) {
				Log.d(TAG,
						"RegistrationListener#onUnregistrationFailed errorCode="
								+ errorCode);
			}
		};
	}

	DiscoveryListener generateDiscoveryListener() {
		return new NsdManager.DiscoveryListener() {

			@Override
			public void onDiscoveryStarted(String regType) {
				Log.d(TAG, "DiscoveryListener#onDiscoveryStarted " + regType);
			}

			@Override
			public void onStartDiscoveryFailed(String serviceType, int errorCode) {
				Log.d(TAG,
						"DiscoveryListener#onStartDiscoveryFailed errorCode="
								+ errorCode);
				mNsdManager.stopServiceDiscovery(this);
			}

			@Override
			public void onServiceFound(final NsdServiceInfo service) {
				Log.d(TAG, "DiscoveryListener#onServiceFound service="
						+ service);
				if (!service.getServiceType().equals(sServiceType)) {
					Log.d(TAG,
							"Unknown Service Type: " + service.getServiceType());
				} else if (service.getServiceName().startsWith(mServiceName)) {
					// 少し乱暴だけど、同一マシンが "わかめ (2)" とかいって複数検出されるので。
					Log.d(TAG, "Same machine: " + mServiceName);
				} else {
					Log.d(TAG, "New service found: " + mServiceList.size());
					addToList(service);
				}
			}

			@Override
			public void onServiceLost(final NsdServiceInfo service) {
				removeFromList(service);
			}

			@Override
			public void onDiscoveryStopped(String serviceType) {
				Log.d(TAG, "DiscoveryListener#onDiscoveryStopped "
						+ serviceType);
			}

			@Override
			public void onStopDiscoveryFailed(String serviceType, int errorCode) {
				Log.d(TAG, "DiscoveryListener#onStopDiscoveryFailed errorCode="
						+ errorCode);
				mNsdManager.stopServiceDiscovery(this);
			}
		};
	}

	ResolveListener generateResolveListener() {
		return new NsdManager.ResolveListener() {
			@Override
			public void onServiceResolved(NsdServiceInfo service) {
				Log.d(TAG, "ResolveListener#onServiceResolved service="
						+ service);

				if (service.getServiceName().equals(mServiceName)) {
					Log.d(TAG, "Same machine.");
					return;
				}

				try {
					Socket socket = new Socket(service.getHost(),
							service.getPort());
					mCallback.onRecommendedToDetach(self);
					mCallback.onConnectAsClient(socket.getOutputStream(),
							socket.getInputStream());
				} catch (IOException e) {
					Log.w(TAG, "raise IOException.", e);
					removeFromList(service);
				}
			}

			@Override
			public void onResolveFailed(NsdServiceInfo service, int errorCode) {
				Log.d(TAG, "ResolveListener#onResolveFailed errorCode="
						+ errorCode);
				removeFromList(service);
			}
		};
	}

	void addToList(NsdServiceInfo target) {
		String name = target.getServiceName();
		for (NsdServiceInfo info : mServiceList) {
			if (info.getServiceName().equals(name)) {
				mServiceList.remove(info);
				break;
			}
		}
		mServiceList.add(target);
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				mCallback.onUpdate(mServiceList);
			}
		});
	}

	void removeFromList(NsdServiceInfo target) {
		String name = target.getServiceName();
		// 半角スペースが別の文字に置換されて取得されてしまい、正しく一致できないため補正
		name = name.replaceAll("\\\\032", " ").replace("\\", "");
		for (NsdServiceInfo info : mServiceList) {
			if (info.getServiceName().equals(name)) {
				mServiceList.remove(info);
				removeFromList(target);
				return;
			}
		}
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				mCallback.onUpdate(mServiceList);
			}
		});
	}
}
