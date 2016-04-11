package com.jtdev.webcomicreader.helpers;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.jtdev.webcomicreader.models.Webcomic;

public class DatabaseHelper extends SQLiteOpenHelper
{
	private Context context;

	//Database
	private static final int DATABASE_VERSION = 5;
	public static final String DATABASE_NAME = "webcomicDb";
	private static final String TABLE_WEBCOMICS = "webcomics";

	//Columns
	private static final String KEY_ALIAS = "alias";
	private static final String KEY_URL = "url";
	private static final String KEY_METHOD = "method";

	private static final String KEY_ID = "id";
	private static final String KEY_STRUCTURE = "structureFind";
	private static final String KEY_PREVIOUS = "previousID";
	private static final String KEY_NEXT = "nextID";

	private static final String CREATE_TABLE_WEBCOMICS = "CREATE TABLE "
			+ TABLE_WEBCOMICS + "(" + KEY_ALIAS + " TEXT PRIMARY KEY,"
								   + KEY_URL + " TEXT,"
								   + KEY_METHOD + " TEXT,"
								   + KEY_ID + " TEXT,"
								   + KEY_STRUCTURE + " TEXT,"
								   + KEY_PREVIOUS + " TEXT,"
								   + KEY_NEXT + " TEXT" + ")";

	private static DatabaseHelper helper;
	public static DatabaseHelper getInstance(Context context)
	{
		if (helper == null) helper = new DatabaseHelper(context.getApplicationContext());
		return helper;
	}

	private DatabaseHelper(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.context = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		db.execSQL(CREATE_TABLE_WEBCOMICS);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		//TODO Database upgrade migration
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEBCOMICS);

		onCreate(db);
	}

	public void createWebcomic(Webcomic webcomic)
	{
		SQLiteDatabase db = getWritableDatabase();

		db.insert(TABLE_WEBCOMICS, null, putWebcomic(webcomic));
	}

	public void deleteTable()
	{
		SQLiteDatabase db = getWritableDatabase();

		db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEBCOMICS);
	}

	public Webcomic getWebcomic(String alias)
	{
		SQLiteDatabase db = getReadableDatabase();

		String selectQuery = "SELECT  * FROM " + TABLE_WEBCOMICS + " WHERE " + KEY_ALIAS + " = '" + alias + "'";
		Cursor c = db.rawQuery(selectQuery, null);

		if (c == null || c.getCount() == 0) return null;

		c.moveToFirst();
		return getWebcomic(c);
	}

	public List<Webcomic> getAllWebcomics()
	{
		List<Webcomic> webcomics = new ArrayList<>();
		String selectQuery = "SELECT  * FROM " + TABLE_WEBCOMICS;

		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.rawQuery(selectQuery, null);

		if (c.moveToFirst())
		{
			do
			{
				webcomics.add(getWebcomic(c));
			}
			while (c.moveToNext());
		}

		return webcomics;
	}

	public int getWebcomicsCount()
	{
		String countQuery = "SELECT  * FROM " + TABLE_WEBCOMICS;
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.rawQuery(countQuery, null);

		int count = cursor.getCount();
		cursor.close();

		return count;
	}

	public int updateWebcomic(Webcomic webcomic)
	{
		SQLiteDatabase db = getWritableDatabase();

		return db.update(TABLE_WEBCOMICS, putWebcomic(webcomic), KEY_ALIAS + " = ?", new String[]{webcomic.getAlias()});
	}

	public void deleteWebcomic(String alias)
	{
		SQLiteDatabase db = getWritableDatabase();
		db.delete(TABLE_WEBCOMICS, KEY_ALIAS + " = ?", new String[]{alias});
		BackupManager.getInstance(context).dataChanged();
	}

	public void closeDB()
	{
		SQLiteDatabase db = getReadableDatabase();
		if (db != null && db.isOpen())
			db.close();
	}

	private ContentValues putWebcomic(Webcomic webcomic)
	{
		ContentValues values = new ContentValues();
		values.put(KEY_URL, webcomic.getUrlString());
		values.put(KEY_ALIAS, webcomic.getAlias());
		values.put(KEY_METHOD, webcomic.getMethod());
		values.put(KEY_ID, webcomic.getId());
		values.put(KEY_STRUCTURE, webcomic.getStructure());
		values.put(KEY_PREVIOUS, webcomic.getPreviousStructure());
		values.put(KEY_NEXT, webcomic.getNextStructure());

		return values;
	}

	private Webcomic getWebcomic(Cursor c)
	{
		try
		{
			Webcomic webcomic = new Webcomic();
			webcomic.setUrlString(getString(c,KEY_URL));
			webcomic.setAlias(getString(c,KEY_ALIAS));
			webcomic.setMethod(getString(c,KEY_METHOD));
			webcomic.setId(getString(c,KEY_ID));
			webcomic.setStructure(getString(c,KEY_STRUCTURE));
			webcomic.setPreviousStructure(getString(c,KEY_PREVIOUS));
			webcomic.setNextStructure(getString(c,KEY_NEXT));
			return webcomic;
		}
		catch (MalformedURLException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	private String getString(Cursor c, String name)
	{
		int columnIndex = c.getColumnIndex(name);
		if (columnIndex != -1) return c.getString(columnIndex);
		return null;
	}

	@Override
	public SQLiteDatabase getWritableDatabase()
	{
		synchronized (BackupAgent.lock)
		{
			BackupManager.getInstance(context).dataChanged();
			return super.getWritableDatabase();
		}
	}

	@Override
	public SQLiteDatabase getReadableDatabase()
	{
		synchronized (BackupAgent.lock)
		{
			return super.getReadableDatabase();
		}
	}
}

