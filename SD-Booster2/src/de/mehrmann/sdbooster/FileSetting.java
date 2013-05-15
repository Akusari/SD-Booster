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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Properties;

import android.content.Context;
import android.util.Log;

public class FileSetting {

	private static final String fileName = "sd-booster.prop";

	private final String path;
	private final File file;
	private final Database db;

	private Properties prop;

	public FileSetting(Context context, Database db) {

		this.path = context.getFilesDir().getAbsolutePath();
		this.file = new File(path, fileName);
		this.db = db;
	}

	public static boolean exists(Context context) {

		String path = context.getFilesDir().getAbsolutePath();
		File file = new File(path, fileName);

		return file.exists();
	}

	public void setGlobalProperties() {

		if (!file.exists())
			return;

		prop = new Properties();

		try {
			BufferedInputStream stream = new BufferedInputStream(
					new FileInputStream(file));

			prop.load(stream);
			stream.close();

			// Global settings

			readGlobalSettings();

		} catch (Exception e) {
			Log.e(Utils.TAG, "getProperties(): " + e.toString());
		}
	}

	public void setCardProperties(final ArrayList<MmcModell> cards) {

		if (!file.exists())
			return;

		int deviceNumber = 0;
		prop = new Properties();

		try {
			BufferedInputStream stream = new BufferedInputStream(
					new FileInputStream(file));

			prop.load(stream);
			stream.close();

			// Device based settings

			String number = prop.getProperty("number");

			if (Utils.containOnlyNumbers(number)) {
				deviceNumber = Integer.valueOf(number);
			} else {
				Log.e(Utils.TAG, fileName + ": Wrong number of cards!");
				file.delete();
				return;
			}

			// loop through devices

			for (int i = 0; i < deviceNumber; i++) {

				MmcModell device = null;
				String deviceName = prop.getProperty("device_" + i + "_name");

				if (deviceName == null) {
					Log.w(Utils.TAG, fileName + ": Name of " + "device_" + i
							+ "_name is null!");
					continue;
				}

				for (MmcModell item : cards) {
					if (item.getName().equals(deviceName)) {
						device = item;
					}
				}

				if (device == null) {
					Log.w(Utils.TAG, fileName + ": Device " + deviceName
							+ "not found!");
					continue;
				}

				int boot = Integer.valueOf(prop.getProperty("device_" + i
						+ "_boot", "0"));
				int monitor = Integer.valueOf(prop.getProperty("device_" + i
						+ "_monitor", "0"));
				String size = prop.getProperty("device_" + i + "_size", "0");

				boolean setOnBoot = boot == 1 ? true : false;
				boolean setOnMonitor = monitor == 1 ? true : false;
				boolean setOnCache = false;

				if (setOnBoot) {
					device.setOnBoot(true);
				}

				if (setOnMonitor) {
					device.setOnMonitor(true);
				}

				if (Utils.containOnlyNumbers(size) && Utils.cacheSizeIsOk(size)) {

					int alignValue = Utils.alignValue(Integer.valueOf(size));
					device.setAheadUser(String.valueOf(alignValue));
					setOnCache = true;
				}

				Log.i(Utils.TAG,
						fileName + ": Device configured: " + device.toString());
				db.cardUpdate(device, setOnBoot, setOnMonitor, setOnCache);
			}

			Log.i(Utils.TAG, "Properties done");

			// delete properties

			file.delete();

		} catch (Exception e) {
			Log.e(Utils.TAG, "getProperties(): " + e.toString());
			file.delete();
		}
	}

	private void readGlobalSettings() {

		String setAllonCache = prop.getProperty("setAllonCache", "0");
		String setAllonBoot = prop.getProperty("setAllonBoot", "0");
		String setAllonMonitor = prop.getProperty("setAllonMonitor", "0");
		String size = prop.getProperty("setAllCacheSize", "0");

		if (setAllonCache.equals("1")) {
			if (Utils.containOnlyNumbers(size) && Utils.cacheSizeIsOk(size)) {

				int alignValue = Utils.alignValue(Integer.valueOf(size));
				db.setPref(5, alignValue, 0);
				db.setPref(4, 1, 0);
			} else {
				Log.e(Utils.TAG, fileName + ": Wrong cache size for all");
			}
		}

		if (setAllonBoot.equals("1")) {
			db.setPref(1, 1, 0);
		}

		if (setAllonMonitor.equals("1")) {
			db.setPref(2, 1, 0);
		}

		Log.i(Utils.TAG, "Properties done");
	}
}
