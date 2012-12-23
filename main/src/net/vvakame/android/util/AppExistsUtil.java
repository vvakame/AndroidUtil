package net.vvakame.android.util;

import net.vvakame.android.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;

/**
 * Activityを助けるヘルパ
 * 
 * @author vvakame
 */
public class AppExistsUtil {
	private static Context sContext = null;
	private static String sPackageName = null;

	private static DialogInterface.OnClickListener sOnClick = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
			case android.content.DialogInterface.BUTTON_POSITIVE:
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("market://details?id=" + sPackageName));
				sContext.startActivity(intent);

				break;
			case android.content.DialogInterface.BUTTON_NEGATIVE:
				break;
			}
		}
	};

	/**
	 * 指定のIntentがNotActivityFoundExceptionにならずに使えるか調べる<BR>
	 * 処理できるアプリが存在しない場合指定のアプリをダウンロードさせる
	 * 
	 * @param context
	 * @param intent
	 *            処理できるか調べたいIntent
	 * @param appName
	 *            処理できなかったときダウンロードさせるアプリの名前
	 * @param packageName
	 *            処理できなかったときダウンロードさせるアプリのpackage名
	 * @return 処理できるならtrue, 処理できないならfalse
	 */
	public static boolean canResolveActivity(Context context, Intent intent,
			String appName, String packageName) {
		sContext = context;

		ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(
				intent, 0);

		// 対応できるAppがインストールされていればそのまま処理
		if (resolveInfo != null) {
			return true;
		}

		// なければ、Marketに取得しにいくか聞く。
		sPackageName = packageName;
		AlertDialog.Builder diagBldr = new AlertDialog.Builder(context);
		diagBldr.setTitle(context.getString(R.string.cant_resolve_intent_title,
				appName));
		diagBldr.setMessage(context
				.getString(R.string.cant_resolve_intent_message));

		diagBldr.setPositiveButton(context.getString(R.string.go_market),
				sOnClick);
		diagBldr.setNegativeButton(context.getString(R.string.ignore), sOnClick);

		diagBldr.setCancelable(true);
		diagBldr.create();
		diagBldr.show();

		return false;
	}
}
