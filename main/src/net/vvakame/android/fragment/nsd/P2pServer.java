package net.vvakame.android.fragment.nsd;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

/**
 * 接続待ちするためのサーバソケットを生成+管理するクラス。
 * 
 * @author vvakame
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
class P2pServer {
	static final String TAG = P2pServer.class.getSimpleName();

	NsdFragment mNsdFragment;

	Thread mThread = null;
	ServerSocket mServerSocket = null;

	P2pServer(NsdFragment nsdFragment) {
		mNsdFragment = nsdFragment;

		mThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					mServerSocket = new ServerSocket(0);
					Log.d(TAG,
							"open server socket. port"
									+ mServerSocket.getLocalPort());
					mNsdFragment.registerService(mServerSocket.getLocalPort());

					Socket socket = mServerSocket.accept();
					Log.d(TAG, "client come here!");
					mServerSocket.close();

					mNsdFragment.mCallback.onRecommendedToDetach(mNsdFragment);
					mNsdFragment.mCallback.onConnectAsServer(
							socket.getOutputStream(), socket.getInputStream());
				} catch (IOException e) {
					Log.d(TAG, "ServerSocket: ", e);
				}
			}
		});
		mThread.start();
	}

	/**
	 * 後始末。
	 */
	public void tearDown() {
		mThread.interrupt();
	}
}
