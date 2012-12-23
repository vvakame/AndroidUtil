package net.vvakame.android.fragment.nfc;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;

import net.vvakame.android.R;
import net.vvakame.android.util.NfcUtil;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

/**
 * NFCを簡単に扱うためのFragment。<br>
 * {@link Activity} に {@link NfcActionCallbackPicker} を実装し、
 * {@link NfcActionCallback} を何かに実装して返してあげてください。<br>
 * 後は、コールバックの引数や返り値にあった処理を実装するだけでOKです。<br>
 * {@link NfcActionCallback#onTagArrival(NdefBuilderBase)} で利用する
 * {@link NdefBuilderBase} について少し解説しておきます。<br>
 * {@link NdefBuilderBase} はNDEF未初期化でも、NDEF書込可能なタグでも透過的に扱えるようにしたラッパクラスです。<br>
 * 可能なのは、タグのサイズの読取りと読込専用タグにすることと、書込するNdefMessageを指定することだけです。
 * 
 * @author vvakame
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class NfcFragment extends Fragment {

	static final String TAG = NfcFragment.class.getSimpleName();

	/**
	 * {@link NfcActionCallback} を取得するためのPicker.
	 * 
	 * @author vvakame
	 */
	public static interface NfcActionCallbackPicker {
		public NfcActionCallback getNfcActionCallback();
	}

	/**
	 * NFCに関する何かしらのイベントが発生した場合のコールバック.
	 * 
	 * @author vvakame
	 */
	public interface NfcActionCallback {
		/** NFC がサポートされていない時のコールバック. */
		public void onNfcNotSupported();

		/** NFC が有効になっていない時のコールバック. */
		public void onNfcDisabled();

		public void onTagArrival(NdefBuilderBase builder);

		public void onTagWriteCompleted();

		public void onTagWriteFailure();
	}

	public interface NdefBuilder {
		public Integer getSize();

		public boolean isLock();

		public void write(NdefMessage ndefMessage);

		public void lock();

		public Tag getTag();
	}

	final Charset UTF8 = Charset.forName("UTF-8");

	NfcAdapter mNfcAdapter;
	NfcActionCallback mCallback;

	/**
	 * FragmentがActivityに関連付けられた時のイベント. コールバックの取得やNFCの初期化などを行う.
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		NfcActionCallbackPicker picker = (NfcActionCallbackPicker) activity;
		mCallback = picker.getNfcActionCallback();

		mNfcAdapter = NfcAdapter.getDefaultAdapter(activity);
		if (mNfcAdapter == null) {
			mCallback.onNfcNotSupported();
		} else if (mNfcAdapter.isEnabled() == false) {
			mCallback.onNfcDisabled();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		// NfcAdapter#enableForegroundDispatch を設定
		enableForegroundDispatch();
	}

	/**
	 * Ndef または NdefFormatable を TechListに持つタグだけ反応させる.
	 */
	public void enableForegroundDispatch() {
		final Activity activity = getActivity();
		IntentFilter[] filters = makeFilter();
		String[][] techLists = makeTechLists();
		PendingIntent pendingIntent = PendingIntent.getActivity(activity, 0,
				new Intent(activity, activity.getClass()), 0);
		if (mNfcAdapter != null) {
			mNfcAdapter.enableForegroundDispatch(activity, pendingIntent,
					filters, techLists);
		}
	}

	IntentFilter[] makeFilter() {
		IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
		IntentFilter[] filters = new IntentFilter[] { tech, tech };
		return filters;
	}

	String[][] makeTechLists() {
		String[] ndef = new String[] { Ndef.class.getName() };
		String[] ndefFormatable = new String[] { NdefFormatable.class.getName() };
		String[][] techLists = new String[][] { ndef, ndefFormatable };
		return techLists;
	}

	@Override
	public void onPause() {
		super.onPause();
		// NfcAdapter#enableForegroundDispatch を解除
		if (mNfcAdapter != null) {
			mNfcAdapter.disableForegroundDispatch(getActivity());
		}
	}

	public void onNewIntent(Intent intent) {
		String action = intent.getAction();
		// NfcAdapter#enableForegroundDispatch で指定したタイプのタグが取れたら処理する.
		if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
			Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			writeNdefMessage(tag);
		}
	}

	/** タグに NdefMessage を書きこむ. */
	void writeNdefMessage(Tag tag) {
		try {
			if (Arrays.asList(tag.getTechList()).contains(
					NdefFormatable.class.getName())) {
				// TechListにNdefFormatable が含まれていたら

				NdefFormatable ndef = NdefFormatable.get(tag);
				NdefBuilderForNdefFormatable builder = new NdefBuilderForNdefFormatable(
						ndef);
				mCallback.onTagArrival(builder);
				if (builder.mNdefMessage == null) {
					ndef.close();
					return;
				}

				try {
					// 未接続だったら接続.
					if (!ndef.isConnected()) {
						ndef.connect();
					}
					// 書込
					if (builder.doLock) {
						ndef.formatReadOnly(builder.mNdefMessage);
					} else {
						ndef.format(builder.mNdefMessage);
					}
					// 書き込めたことをToastで通知！
					mCallback.onTagWriteCompleted();
				} catch (IOException e) {
					Log.d(TAG, "NdefFormatable IO Exception", e);
					ndef.close();
					// メモ 例外発生した場合は未初期化状態のまま
					if (!ndef.isConnected()) {
						ndef.connect();
					}
					// 書込
					NdefRecord ndefRecord = NfcUtil.newTextRecord(
							"AppTagMaker", Locale.US, true);
					NdefMessage ndefMessage = new NdefMessage(
							new NdefRecord[] { ndefRecord });
					// ここで再度例外が発生するんだけどとりあえず初期化されるからいいや…
					ndef.format(ndefMessage);

					Toast.makeText(getActivity(),
							R.string.tag_write_error_undefined,
							Toast.LENGTH_SHORT).show();
				} finally {
					ndef.close();
				}
			} else if (Arrays.asList(tag.getTechList()).contains(
					Ndef.class.getName())) {
				// TechListにNdef が含まれていたら

				Ndef ndef = Ndef.get(tag);
				NdefBuilderForNdef builder = new NdefBuilderForNdef(ndef);
				mCallback.onTagArrival(builder);
				if (builder.mNdefMessage == null) {
					ndef.close();
					return;
				}

				try {
					// 未接続だったら接続.
					if (!ndef.isConnected()) {
						ndef.connect();
					}
					// 書込可能かチェック
					if (ndef.isWritable()) {
						// 書込
						ndef.writeNdefMessage(builder.mNdefMessage);
						if (builder.doLock) {
							ndef.makeReadOnly();
						}
						// 書き込めたことをToastで通知！
						mCallback.onTagWriteCompleted();
					}
				} finally {
					ndef.close();
				}
			}
		} catch (FormatException e) {
			Log.e(this.getClass().getSimpleName(), "FormatException", e);
			mCallback.onTagWriteFailure();
		} catch (IOException e) {
			Log.e(this.getClass().getSimpleName(), "IOException", e);
			mCallback.onTagWriteFailure();
		}
	}

	public static abstract class NdefBuilderBase implements NdefBuilder {

		Tag mTag;

		NdefMessage mNdefMessage;
		boolean doLock = false;

		NdefBuilderBase(Tag tag) {
			mTag = tag;
		}

		@Override
		public final void write(NdefMessage ndefMessage) {
			mNdefMessage = ndefMessage;
		}

		@Override
		public final void lock() {
			doLock = true;
		}

		@Override
		public final Tag getTag() {
			return mTag;
		}
	}

	public static class NdefBuilderForNdef extends NdefBuilderBase {

		Ndef mNdef;

		NdefBuilderForNdef(Ndef ndef) {
			super(ndef.getTag());
			mNdef = ndef;
		}

		@Override
		public Integer getSize() {
			return mNdef.getMaxSize();
		}

		@Override
		public boolean isLock() {
			return !mNdef.isWritable();
		}
	}

	public static class NdefBuilderForNdefFormatable extends NdefBuilderBase {

		NdefFormatable mNdef;

		NdefBuilderForNdefFormatable(NdefFormatable ndef) {
			super(ndef.getTag());
			mNdef = ndef;
		}

		@Override
		public Integer getSize() {
			return null;
		}

		@Override
		public boolean isLock() {
			return false;
		}
	}

	public static class NdefBuilderUnsupported extends NdefBuilderBase {

		NdefBuilderUnsupported(Tag tag) {
			super(tag);
		}

		@Override
		public Integer getSize() {
			return null;
		}

		@Override
		public boolean isLock() {
			return true;
		}
	}
}
