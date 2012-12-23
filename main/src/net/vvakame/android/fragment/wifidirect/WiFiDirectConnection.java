package net.vvakame.android.fragment.wifidirect;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import net.vvakame.android.fragment.wifidirect.WiFiDirectFragment.Type;
import net.vvakame.android.fragment.wifidirect.WiFiDirectFragment.WiFiDirectCallback;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Wi-Fi Direct接続後にSocketを開き管理するためのクラス.
 * 
 * @author vvakame
 */
class WiFiDirectConnection implements Closeable {

	static final String TAG = WiFiDirectConnection.class.getSimpleName();

	final WiFiDirectConnection self = this;

	private static final int SOCKET_TIMEOUT = 5000;
	public static final int PORT = 8989;

	WiFiDirectCallback mCallback;

	// 親ソケットは可能な限り再利用する(明示的に閉じない)
	static ServerSocket mServerSocket;

	// 接続先アドレス Clientになった時だけ保持
	InetAddress mHostAddress = null;
	Socket mSocket;

	final Type mType;
	InputStream mIs;
	OutputStream mOs;

	WiFiDirectConnection(WiFiDirectCallback callback) {
		Log.i(TAG, "I'm Host!");
		mCallback = callback;
		mType = Type.SERVER;
	}

	WiFiDirectConnection(WiFiDirectCallback callback, InetAddress host) {
		Log.i(TAG, "I'm Client!");
		mCallback = callback;
		mHostAddress = host;
		mType = Type.CLIENT;
	}

	/**
	 * 通信用にソケットを開く.
	 */
	public void openSocket() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (mType == Type.SERVER) {
					openServerSocket();
				} else {
					openClientSocket();
				}
			}
		}).start();
	}

	void openServerSocket() {
		if (mServerSocket != null) {
			return;
		}
		try {
			mServerSocket = new ServerSocket();
			mServerSocket.setReuseAddress(true);
			mServerSocket.bind(new InetSocketAddress(PORT));
			Socket client = mServerSocket.accept();

			mIs = client.getInputStream();
			mOs = client.getOutputStream();

			openCallback();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	void openClientSocket() {
		mSocket = new Socket();
		try {
			mSocket.bind(null);
			mSocket.connect((new InetSocketAddress(mHostAddress, PORT)),
					SOCKET_TIMEOUT);

			mOs = mSocket.getOutputStream();
			mIs = mSocket.getInputStream();

			openCallback();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	void openCallback() {
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				mCallback.onOpenSocket(mType, mIs, mOs, self);
			}
		});
	}

	/**
	 * ソケットを閉じる.
	 */
	public void close() {
		if (mSocket != null && mSocket.isConnected()) {
			try {
				mSocket.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			mSocket = null;
		}
	}
}
