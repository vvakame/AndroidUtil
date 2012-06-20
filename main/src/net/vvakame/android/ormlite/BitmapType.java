package net.vvakame.android.ormlite;

import java.io.ByteArrayOutputStream;
import java.sql.SQLException;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.BaseDataType;
import com.j256.ormlite.support.DatabaseResults;

public class BitmapType extends BaseDataType {

	public BitmapType() {
		super(SqlType.BYTE_ARRAY, new Class<?>[] { Bitmap.class });
	}

	public static BitmapType getSingleton() {
		return new BitmapType();
	}

	@Override
	public Object parseDefaultString(FieldType arg0, String arg1)
			throws SQLException {
		return null;
	}

	@Override
	public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos)
			throws SQLException {
		return super.sqlArgToJava(fieldType, sqlArg, columnPos);
	}

	@Override
	public Object javaToSqlArg(FieldType fieldType, Object javaObject)
			throws SQLException {
		Bitmap bitmap = (Bitmap) javaObject;
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		bitmap.compress(CompressFormat.PNG, 100, os);
		return os.toByteArray();
	}

	@Override
	public Object resultToJava(FieldType fieldType, DatabaseResults results,
			int columnPos) throws SQLException {
		byte[] bytes = results.getBytes(columnPos);
		return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
	}

	@Override
	public Object resultToSqlArg(FieldType arg0, DatabaseResults arg1, int arg2)
			throws SQLException {
		return null;
	}
}
