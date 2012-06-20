package net.vvakame.android.fragment;

import java.nio.charset.Charset;

import net.vvakame.android.R;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Beamを簡単に扱うためのFragment.
 * 
 * @author vvakame
 */
public class BeamFragment extends Fragment implements
		CreateNdefMessageCallback, OnNdefPushCompleteCallback {

	/**
	 * {@link BeamActionCallback} を取得するためのPicker.
	 * 
	 * @author vvakame
	 */
	public static interface BeamActionCallbackPicker {
		public BeamActionCallback getBeamActionCallback();
	}

	/**
	 * Beamに関する何かしらのイベントが発生した場合のコールバック.
	 * 
	 * @author vvakame
	 */
	public static interface BeamActionCallback {
		/** NFC がサポートされていない時のコールバック. */
		public void onNfcNotSupported();

		/** NFC が有効になっていない時のコールバック. */
		public void onNfcDisabled();

		/** Beamでメッセージを受け取った時のコールバック. */
		public void onBeamReceived(NdefMessage msg);

		/** Beamで送るメッセージを生成する. */
		public NdefMessage onBeamSendPreprocess();

		/** Beamでメッセージ送信を行った時のコールバック. */
		public void onBeamSendComplete();
	}

	final Charset UTF8 = Charset.forName("UTF-8");

	NfcAdapter mNfcAdapter;

	BeamActionCallback mCallback;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	/**
	 * FragmentがActivityに関連付けられた時のイベント. コールバックの取得やNFCの初期化などを行う.
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		BeamActionCallbackPicker picker = (BeamActionCallbackPicker) activity;
		mCallback = picker.getBeamActionCallback();

		mNfcAdapter = NfcAdapter.getDefaultAdapter(activity);
		if (mNfcAdapter == null) {
			mCallback.onNfcNotSupported();
			return;
		} else if (mNfcAdapter.isEnabled() == false) {
			mCallback.onNfcDisabled();
		}

		mNfcAdapter.setNdefPushMessageCallback(this, activity);
		mNfcAdapter.setOnNdefPushCompleteCallback(this, activity);
	}

	/**
	 * NdefMessageが要求された時 (=Beamで送るメッセージを要求された時) の動作.
	 */
	@Override
	public NdefMessage createNdefMessage(NfcEvent event) {
		NdefMessage msg = mCallback.onBeamSendPreprocess();
		return msg;
	}

	/**
	 * Beam成功時のコールバック. そのまま {@link BeamActionCallback} に渡す.<br>
	 * onNdefPushComplete はUIスレッド以外で実行されるのでUIスレッドで実行するようにしてやる.
	 */
	@Override
	public void onNdefPushComplete(NfcEvent event) {
		new Handler(Looper.getMainLooper()) {
			@Override
			public void dispatchMessage(Message msg) {
				mCallback.onBeamSendComplete();
			}

		}.sendEmptyMessage(0);
	}

	int mIntentHashCode = 0;

	/**
	 * NDEF通知のIntentで、かつ処理済でなければ処理する.
	 */
	@Override
	public void onResume() {
		super.onResume();

		Intent intent = getActivity().getIntent();
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())
				&& mIntentHashCode != intent.hashCode()) {
			processIntent(intent);
			mIntentHashCode = intent.hashCode();
		}
	}

	/**
	 * NdefMessageが含まれるIntentの処理を行う.
	 * アプリ内で複数の手段のためにNFCを利用する場合、もう少し細かく内容のチェックをしたほうがよい.
	 */
	void processIntent(Intent intent) {
		Parcelable[] rawMsgs = intent
				.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

		// TODO 0番だけ見るのでいいんだっけ？
		NdefMessage msg = (NdefMessage) rawMsgs[0];
		mCallback.onBeamReceived(msg);
	}

	/**
	 * メニューを生成する. NFCが未サポートの場合、メニューを表示しない.
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (mNfcAdapter == null) {
			return;
		}
		inflater.inflate(R.menu.shortmessaging_beam, menu);
	}

	/**
	 * メニューが選択された場合の動作.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (R.id.beam_settings == item.getItemId()) {
			// 下記でBeamのON/OFF設定画面へ遷移可能だが、ICS 4.0.1 では遷移先が発見できず例外が発生する.
			// 4.0.3 なら大丈夫.
			// Intent intent = new Intent(Settings.ACTION_NFCSHARING_SETTINGS);
			Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);

			startActivity(intent);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
}
