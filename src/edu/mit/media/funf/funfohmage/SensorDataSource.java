package edu.mit.media.funf.funfohmage;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class SensorDataSource {

	// Database fields
	private SQLiteDatabase database;
	private MySQLiteHelper dbHelper;
	private String[] allColumns = { MySQLiteHelper.COLUMN_ID,
			MySQLiteHelper.COLUMN_DATA };

	public SensorDataSource(Context context) {
		dbHelper = new MySQLiteHelper(context);
	}

	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public SensorData createData(String data) {
		ContentValues values = new ContentValues();
		values.put(MySQLiteHelper.COLUMN_DATA, data);
		long insertId = database.insert(MySQLiteHelper.TABLE_DATA, null,
				values);
		Cursor cursor = database.query(MySQLiteHelper.TABLE_DATA,
				allColumns, MySQLiteHelper.COLUMN_ID + " = " + insertId, null,
				null, null, null);
		cursor.moveToFirst();
		SensorData newComment = cursorToData(cursor);
		cursor.close();
		return newComment;
	}

	public void deleteData(SensorData comment) {
		long id = comment.getId();
		System.out.println("Comment deleted with id: " + id);
		database.delete(MySQLiteHelper.TABLE_DATA, MySQLiteHelper.COLUMN_ID
				+ " = " + id, null);
	}

	public List<SensorData> getAllData() {
		List<SensorData> sensorData = new ArrayList<SensorData>();

		Cursor cursor = database.query(MySQLiteHelper.TABLE_DATA,
				allColumns, null, null, null, null, null);

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			SensorData comment = cursorToData(cursor);
			sensorData.add(comment);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return sensorData;
	}

	private SensorData cursorToData(Cursor cursor) {
		SensorData sensorData = new SensorData();
		sensorData.setId(cursor.getLong(0));
		sensorData.setData(cursor.getString(1));
		return sensorData;
	}
}