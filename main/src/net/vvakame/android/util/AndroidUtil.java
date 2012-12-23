package net.vvakame.android.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class AndroidUtil {
	private static final String MY_PACKAGE_PREFIX = "net.vvakame";
	private static final String EXCLUDE_CLASS = AndroidUtil.class
			.getCanonicalName();

	/**
	 * 呼び出し元のメソッド名を取得し返します。<BR>
	 * MY_PACKAGE_PREFIX に先頭一致するパッケージが取得されます。
	 * 
	 * @return メソッド名
	 */
	public static String getMethodName() {
		StackTraceElement[] stacks = Thread.currentThread().getStackTrace();

		for (StackTraceElement stack : stacks) {
			String stackClass = stack.getClassName();
			if (stackClass.startsWith(MY_PACKAGE_PREFIX)
					&& !EXCLUDE_CLASS.equals(stackClass)) {
				return stack.getMethodName();
			}
		}

		return null;
	}

	/**
	 * 呼び出し元のクラス名とメソッド名、行番号を取得し返します。<BR>
	 * MY_PACKAGE_PREFIX に先頭一致するパッケージが取得されます。
	 * 
	 * @return クラス名#メソッド名/L行番号
	 */
	public static String getStackName() {
		StackTraceElement[] stacks = Thread.currentThread().getStackTrace();

		for (StackTraceElement stack : stacks) {
			String stackClass = stack.getClassName();
			if (stackClass.startsWith(MY_PACKAGE_PREFIX)
					&& !EXCLUDE_CLASS.equals(stackClass)) {

				StringBuilder stb = new StringBuilder();
				stb.append(stack.getFileName().replace(".java", ""));
				stb.append("#");
				stb.append(stack.getMethodName());
				stb.append("/L");
				stb.append(stack.getLineNumber());

				return stb.toString();
			}
		}

		return null;
	}

	/**
	 * 例外の発生箇所と例外の内容を取得し返します。
	 * 
	 * @param e
	 *            文字列化したい例外
	 * @return クラス名#メソッド名/L行番号, 例外クラス名=メッセージ
	 */
	public static String getExceptionLog(Exception e) {
		String ret = getStackName() + ", " + e.getClass().getSimpleName() + "="
				+ e.getMessage() + " - " + e.getStackTrace();
		return ret;
	}

	/**
	 * 指定されたディレクトリを再起的に削除します。
	 * 
	 * @param target
	 *            削除対象ディレクトリ
	 */
	public static void deleteDir(File target) {
		if (target.exists() == false) {
			return;
		}

		if (target.isFile()) {
			target.delete();
		}

		if (target.isDirectory()) {
			File[] files = target.listFiles();
			for (int i = 0; i < files.length; i++) {
				deleteDir(files[i]);
			}
			target.delete();
		}
	}

	public static void copyFile(File src, File dest) throws IOException {
		FileInputStream fileInputStream = new FileInputStream(src);
		FileChannel srcChannel = fileInputStream.getChannel();
		FileOutputStream fileOutputStream = new FileOutputStream(dest);
		FileChannel destChannel = fileOutputStream.getChannel();
		srcChannel.transferTo(0, srcChannel.size(), destChannel);
		srcChannel.close();
		destChannel.close();
		fileInputStream.close();
		fileOutputStream.close();
	}
}
