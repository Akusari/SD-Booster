/*
 *  Copyright (C) 2013 Daniel Mehrmann (Akusari)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.mehrmann.sdbooster;

import java.io.File;
import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class Database {

	private static final int VERSION = 1;
	private static final String NAME = "sdbooster";

	private static final String TABLE_CARDS = "cards";
	private static final String COL_ID = "_id";
	private static final String COL_NAME = "name";
	private static final String COL_SERIAL = "serial";
	private static final String COL_PATH = "path";
	private static final String COL_CACHE = "cache";
	private static final String COL_BOOT = "boot";
	private static final String COL_MONITOR = "monitor";

	private static final String TABLE_PREF = "prefs";
	private static final String COL_BOOT_ALL = "boot";
	private static final String COL_MONITOR_ALL = "monitor";
	private static final String COL_LICENSE = "license";
	private static final String COL_SIZE_ALL = "size";
	private static final String COL_CACHE_ALL = "cache";

	private static final String TABLE_NOTE = "note";
	private static final String COL_NOTE_UPDATE = "background";
	private static final String COL_NOTE_ALARM = "alarm";
	private static final String COL_NOTE_BOOT = "boot";
	private static final String COL_NOTE_MONITOR = "monitor";
	private static final String COL_NOTE_CHANGE = "change";
	private static final String COL_NOTE_INTERVAL = "interval";
	private static final String COL_NOTE_RINGTONE = "ringtone";
	private static final String COL_NOTE_RESERVED = "reserved";

	private static final String CREATE_TABLE_CARDS = "create table "
			+ TABLE_CARDS + " (" + COL_ID
			+ " integer primary key autoincrement, " + COL_NAME
			+ " text not null, " + COL_SERIAL + " text not null, " + COL_PATH
			+ " text not null, " + COL_CACHE + " text not null, " + COL_BOOT
			+ " integer, " + COL_MONITOR + " integer);";

	private static final String CREATE_TABLE_PREF = "create table "
			+ TABLE_PREF + " (" + COL_ID
			+ " integer primary key autoincrement, " + COL_BOOT_ALL
			+ " integer, " + COL_MONITOR_ALL + " integer, " + COL_LICENSE
			+ " integer, " + COL_SIZE_ALL + " integer, " + COL_CACHE_ALL
			+ " integer);";

	private static final String CREATE_TABLE_NOTE = "create table "
			+ TABLE_NOTE + " (" + COL_ID
			+ " integer primary key autoincrement, " + COL_NOTE_UPDATE
			+ " integer, " + COL_NOTE_ALARM + " integer, " + COL_NOTE_BOOT
			+ " integer, " + COL_NOTE_MONITOR + " integer, " + COL_NOTE_CHANGE
			+ " integer, " + COL_NOTE_INTERVAL + " integer, "
			+ COL_NOTE_RINGTONE + " text not null, " + COL_NOTE_RESERVED
			+ " text not null);";

	private Context context;
	private DatabaseHelper helper;
	private SQLiteDatabase db;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, NAME, null, VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase database) {

			try {
				database.execSQL(CREATE_TABLE_CARDS);
				database.execSQL(CREATE_TABLE_PREF);
				database.execSQL(CREATE_TABLE_NOTE);
				if (!initDb(database)) {
					throw new RuntimeException();
				}
			} catch (SQLException e) {
				Log.e(Utils.TAG, "Database exception = " + e.toString());
				throw new RuntimeException();
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
			// TODO Auto-generated method stub
		}
	}

	public Database(Context ctx) {

		context = ctx;
		helper = new DatabaseHelper(context);

		open();
		migration();
		properties();
	}

	private void open() {
		db = helper.getWritableDatabase();
	}

	public void close() {
		db.close();
	}

	public boolean isOpen() {
		return db.isOpen();
	}

	private static boolean initDb(SQLiteDatabase db) {

		boolean init = true;
		ContentValues values = new ContentValues();

		values.put(COL_BOOT_ALL, 0);
		values.put(COL_MONITOR_ALL, 0);
		values.put(COL_LICENSE, 0);
		values.put(COL_SIZE_ALL, 0);
		values.put(COL_CACHE_ALL, 128);

		if (db.insert(TABLE_PREF, null, values) < 0) {
			init = false;
		}

		values = new ContentValues();

		values.put(COL_NOTE_UPDATE, 1);
		values.put(COL_NOTE_ALARM, 0);
		values.put(COL_NOTE_BOOT, 0);
		values.put(COL_NOTE_MONITOR, 1);
		values.put(COL_NOTE_CHANGE, 0);
		values.put(COL_NOTE_INTERVAL, 360);
		values.put(COL_NOTE_RINGTONE, Utils.RINGTONE);
		values.put(COL_NOTE_RESERVED, "0");

		if (db.insert(TABLE_NOTE, null, values) < 0) {
			init = false;
		}

		return init;
	}

	public void setPref(int index, int data, int table) {

		ContentValues values = new ContentValues();

		switch (index) {

		// pref table

		case 1:
			values.put(COL_BOOT_ALL, data);
			break;
		case 2:
			values.put(COL_MONITOR_ALL, data);
			break;
		case 3:
			values.put(COL_LICENSE, data);
			break;
		case 4:
			values.put(COL_SIZE_ALL, data);
			break;
		case 5:
			values.put(COL_CACHE_ALL, data);
			break;

		// note table

		case 10:
			values.put(COL_NOTE_UPDATE, data);
			break;
		case 11:
			values.put(COL_NOTE_ALARM, data);
			break;
		case 12:
			values.put(COL_NOTE_BOOT, data);
			break;
		case 13:
			values.put(COL_NOTE_MONITOR, data);
			break;
		case 14:
			values.put(COL_NOTE_CHANGE, data);
			break;
		case 15:
			values.put(COL_NOTE_INTERVAL, data);
			break;

		default:
			throw new RuntimeException();
		}

		if (table == 0) {

			if (index > 5) {
				throw new RuntimeException();
			}
			if (db.update(TABLE_PREF, values, COL_ID + " = 1", null) == 0) {
				throw new RuntimeException();
			}

		} else {

			if (index < 10) {
				throw new RuntimeException();
			}
			if (db.update(TABLE_NOTE, values, COL_ID + " = 1", null) == 0) {
				throw new RuntimeException();
			}
		}
	}

	public int getPref(int index, int table) {

		int value = -1;
		Cursor cursor;

		if (table == 0) {
			cursor = db.query(TABLE_PREF,
					new String[] { COL_ID, COL_BOOT_ALL, COL_MONITOR_ALL,
							COL_LICENSE, COL_SIZE_ALL, COL_CACHE_ALL }, null,
					null, null, null, null);
		} else {
			cursor = db.query(TABLE_NOTE, new String[] { COL_ID,
					COL_NOTE_UPDATE, COL_NOTE_ALARM, COL_NOTE_BOOT,
					COL_NOTE_MONITOR, COL_NOTE_CHANGE, COL_NOTE_INTERVAL },
					null, null, null, null, null);
		}

		if (cursor.moveToFirst()) {
			value = cursor.getInt(index);
		} else {
			throw new RuntimeException();
		}

		cursor.close();
		return value;
	}

	public void setRingTone(String tone) {

		ContentValues values = new ContentValues();
		values.put(COL_NOTE_RINGTONE, tone);

		db.update(TABLE_NOTE, values, COL_ID + " = 1", null);
	}

	public String getRingTone() {

		String tone = null;

		Cursor cursor = db.query(TABLE_NOTE,
				new String[] { COL_NOTE_RINGTONE }, null, null, null, null,
				null);

		if (cursor.moveToFirst()) {
			tone = cursor.getString(0);
		} else {
			throw new RuntimeException();
		}

		cursor.close();
		return tone;
	}

	public long cardInsert(final MmcModell card) {

		if (!card.deviceIsOk()) {
			Log.e(Utils.TAG, "cardInsidert(): Device is not ok!");
			Log.d(Utils.TAG, "MmcModell: " + card.toString());
			throw new RuntimeException();
		}

		ContentValues values = new ContentValues();

		values.put(COL_NAME, card.getName());
		values.put(COL_SERIAL, card.getSerial());
		values.put(COL_PATH, card.getAheadPath());
		values.put(COL_CACHE, card.getAheadUser());
		values.put(COL_BOOT, card.isOnBoot());
		values.put(COL_MONITOR, card.isOnMonitor());

		return db.insert(TABLE_CARDS, null, values);
	}

	public void cardUpdate(final MmcModell card, boolean boot, boolean monitor,
			boolean cache) {

		if (!card.deviceIsOk()) {
			Log.e(Utils.TAG, "cardUpdate(): Device is not ok!");
			Log.d(Utils.TAG, "MmcModell: " + card.toString());
			throw new RuntimeException();
		}

		long rowId = card.getId();

		if (rowId < 0) {
			Log.e(Utils.TAG, "cardUpdate(): Device includes no database id!");
			throw new RuntimeException();
		}

		ContentValues values = new ContentValues();

		if (cache) {
			values.put(COL_CACHE, card.getAheadUser());
		}

		if (boot) {
			values.put(COL_BOOT, card.isOnBoot());
		}

		if (monitor) {
			values.put(COL_MONITOR, card.isOnMonitor());
		}

		if (values.size() == 0) {
			Log.e(Utils.TAG, "cardUpdate(): No values!");
			throw new RuntimeException();
		}

		db.update(TABLE_CARDS, values, COL_ID + " = " + rowId, null);
	}

	public MmcModell getCard(String tag, boolean serial) {

		MmcModell modell = null;
		String column;

		if (serial) {
			column = COL_SERIAL;
		} else {
			column = COL_NAME;
		}

		Cursor cursor = db.query(TABLE_CARDS, new String[] { COL_ID, COL_NAME,
				COL_SERIAL, COL_PATH, COL_CACHE, COL_BOOT, COL_MONITOR },
				column + " = " + "'" + tag + "'", null, null, null, null);

		if (cursor.moveToFirst()) {
			modell = new MmcModell();

			modell.setId(cursor.getLong(0));
			modell.setName(cursor.getString(1));
			modell.setSerial(cursor.getString(2));
			modell.setAheadPath(cursor.getString(3));
			modell.setAheadUser(cursor.getString(4));
			modell.setOnBoot(cursor.getInt(5) == 1 ? true : false);
			modell.setOnMonitor(cursor.getInt(6) == 1 ? true : false);
		}

		cursor.close();
		return modell;
	}

	public MmcModell getCard(String name) {

		MmcModell modell = null;

		Cursor cursor = db.query(TABLE_CARDS, new String[] { COL_ID, COL_NAME,
				COL_SERIAL, COL_PATH, COL_CACHE, COL_BOOT, COL_MONITOR },
				COL_NAME + " = " + "'" + name + "'", null, null, null, null);

		if (cursor.moveToFirst()) {
			modell = new MmcModell();

			modell.setId(cursor.getLong(0));
			modell.setName(cursor.getString(1));
			modell.setSerial(cursor.getString(2));
			modell.setAheadPath(cursor.getString(3));
			modell.setAheadUser(cursor.getString(4));
			modell.setOnBoot(cursor.getInt(5) == 1 ? true : false);
			modell.setOnMonitor(cursor.getInt(6) == 1 ? true : false);
		}

		cursor.close();
		return modell;
	}

	public ArrayList<MmcModell> getCards() {

		Cursor cursor = db.query(TABLE_CARDS, new String[] { COL_ID, COL_NAME,
				COL_SERIAL, COL_PATH, COL_CACHE, COL_BOOT, COL_MONITOR }, null,
				null, null, null, null);

		ArrayList<MmcModell> list = setDbDataToModell(cursor);

		cursor.close();
		return list;
	}

	public boolean cardExist(final MmcModell card) {

		Cursor cursor = db.query(TABLE_CARDS, new String[] { COL_ID, COL_NAME,
				COL_SERIAL, COL_PATH, COL_CACHE, COL_BOOT, COL_MONITOR },
				COL_SERIAL + " = " + "'" + card.getSerial() + "'", null, null,
				null, null);

		boolean value = cursor.moveToFirst();

		cursor.close();
		return value;
	}

	private ArrayList<MmcModell> setDbDataToModell(Cursor cursor) {

		ArrayList<MmcModell> list = null;

		if (cursor == null)
			return list;

		if (cursor.getCount() > 0) {

			cursor.moveToFirst();
			list = new ArrayList<MmcModell>();

			do {

				MmcModell card = new MmcModell();

				card.setId(cursor.getLong(0));
				card.setName(cursor.getString(1));
				card.setSerial(cursor.getString(2));
				card.setAheadPath(cursor.getString(3));
				card.setAheadUser(cursor.getString(4));
				card.setOnBoot(cursor.getInt(5) == 1 ? true : false);
				card.setOnMonitor(cursor.getInt(6) == 1 ? true : false);

				list.add(card);

			} while (cursor.moveToNext());
		}

		return list;
	}

	private void migration() {

		// SD-Booster version 1.x to 2.x

		final String OLD_NAME = "sd_boost";
		final String OLD_TABLE = "setting";

		Cursor cursor = null;
		SQLiteDatabase db = null;
		File file = context.getDatabasePath(OLD_NAME);

		if (file.exists()) {
			try {
				db = SQLiteDatabase.openDatabase(file.getAbsolutePath(), null,
						SQLiteDatabase.OPEN_READONLY);

				// cache and onBoot

				cursor = db.query(true, OLD_TABLE, new String[] { "_id",
						"cache", "boot" }, "_id" + "=" + 1, null, null, null,
						null, null);

				if (cursor != null) {
					cursor.moveToFirst();

					String size = cursor.getString(cursor
							.getColumnIndex("cache"));

					if (Utils.containOnlyNumbers(size)
							&& Utils.cacheSizeIsOk(size)) {
						Log.i(Utils.TAG, "Database migration cacheSize = "
								+ size);
						this.setPref(5, Integer.valueOf(size), 0);
						this.setPref(4, 1, 0);
					}

					String onBoot = cursor.getString(cursor
							.getColumnIndex("boot"));

					if (onBoot.equals("true")) {
						Log.i(Utils.TAG, "Database migration onBoot = "
								+ onBoot);
						this.setPref(1, 1, 0);
					}
				}

				// License

				cursor = db.query(true, OLD_TABLE, new String[] { "_id",
						"cache", "boot" }, "_id" + "=" + 3, null, null, null,
						null, null);

				if (cursor != null) {
					cursor.moveToFirst();

					String license = cursor.getString(cursor
							.getColumnIndex("boot"));

					if (license.equals("true")) {
						Log.i(Utils.TAG, "Database migration License = "
								+ license);
						this.setPref(3, 1, 0);
					}
				}

			} catch (Exception e) {
				Log.e(Utils.TAG, "Database exception = " + e.toString());
			}

			finally {
				if (cursor != null)
					cursor.close();
				if (db != null)
					db.close();

				file.delete();
			}
		}
	}

	private void properties() {

		FileSetting prop = new FileSetting(context, this);
		prop.setGlobalProperties();
	}
}