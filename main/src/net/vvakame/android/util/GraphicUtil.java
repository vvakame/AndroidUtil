package net.vvakame.android.util;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class GraphicUtil {

	public static Bitmap drawableToBitmap(Drawable drawable) {
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable) drawable).getBitmap();
		}

		Canvas canvas = new Canvas();
		Bitmap bitmap = Bitmap.createBitmap(drawable.getMinimumWidth(),
				drawable.getMinimumHeight(), Config.ARGB_8888);
		canvas.setBitmap(bitmap);
		drawable.draw(canvas);
		return bitmap;
	}
}
