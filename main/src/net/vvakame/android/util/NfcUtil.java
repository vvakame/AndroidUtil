package net.vvakame.android.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.util.Pair;

public class NfcUtil {
	private NfcUtil() {
	}

	public static NdefMessage newNdefMessage(NdefRecord... records) {
		return new NdefMessage(records);
	}

	/**
	 * AndroidSDK付属の ApiDemos の ForegroundNdefPush より抜粋. RTD が Text
	 * のNdefRecordを作成する.
	 */
	public static NdefRecord newTextRecord(String text, Locale locale,
			boolean encodeInUtf8) {
		byte[] langBytes = locale.getLanguage().getBytes(
				Charset.forName("US-ASCII"));

		Charset utfEncoding = encodeInUtf8 ? Charset.forName("UTF-8") : Charset
				.forName("UTF-16");
		byte[] textBytes = text.getBytes(utfEncoding);

		int utfBit = encodeInUtf8 ? 0 : (1 << 7);
		char status = (char) (utfBit + langBytes.length);

		byte[] data = new byte[1 + langBytes.length + textBytes.length];
		data[0] = (byte) status;
		System.arraycopy(langBytes, 0, data, 1, langBytes.length);
		System.arraycopy(textBytes, 0, data, 1 + langBytes.length,
				textBytes.length);

		return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT,
				new byte[0], data);
	}

	/**
	 * メッセージを作成する. 可能な限りmimeTypeがアプリケーション固有になるように留意する.
	 * 
	 * @param mimeType
	 *            本文のmimeType
	 * @param payload
	 *            本文
	 * @return 作成した NdefRecord
	 */
	public static NdefRecord newMimeRecord(String mimeType, byte[] payload) {
		byte[] mimeBytes = mimeType.getBytes(Charset.forName("UTF-8"));
		NdefRecord mimeRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
				mimeBytes, new byte[0], payload);
		return mimeRecord;
	}

	public static NdefRecord newAndroidApplicationRecord(String packageName) {
		return NdefRecord.createApplicationRecord(packageName);
	}

	public static NdefRecord newAndroidApplicationRecord(Context context) {
		return newAndroidApplicationRecord(context.getPackageName());
	}

	public static NdefRecord newImageRecord(Bitmap bitmap) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
		byte[] content = out.toByteArray();
		return newMimeRecord("image/jpeg", content);
	}

	public static NdefRecord newUriRecord(Uri uri) throws IOException {

		List<Pair<String, Integer>> prefixList = new ArrayList<Pair<String, Integer>>();
		prefixList.add(Pair.create("http://www.", 0x01));
		prefixList.add(Pair.create("https://www.", 0x02));
		prefixList.add(Pair.create("http://", 0x03));
		prefixList.add(Pair.create("https://", 0x04));
		prefixList.add(Pair.create("tel:", 0x05));
		prefixList.add(Pair.create("mailto:", 0x06));

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		for (Pair<String, Integer> pair : prefixList) {
			if (newUriRecordSub(os, uri, pair.first, pair.second)) {
				break;
			}
		}
		if (os.size() == 0) {
			newUriRecordSub(os, uri, "", 0x00);
		}

		return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI,
				new byte[] {}, os.toByteArray());
	}

	static boolean newUriRecordSub(ByteArrayOutputStream os, Uri uri,
			String prefix, int code) throws IOException {
		if (uri.toString().startsWith(prefix)) {
			os.write(code);
			os.write(uri.toString().substring(prefix.length())
					.getBytes(Charset.forName("UTF-8")));
			return true;
		} else {
			return false;
		}
	}
}
