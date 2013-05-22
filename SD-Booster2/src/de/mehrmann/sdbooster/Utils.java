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
 */

package de.mehrmann.sdbooster;

import java.text.NumberFormat;

import android.os.Handler;
import android.os.Message;

public abstract class Utils {

	public static final String RINGTONE = "content://settings/system/notification_sound";
	public static final String MEDIA_MOUNTED = "android.intent.action.MEDIA_MOUNTED";
	public static final String MEDIA_MONITOR = "android.intent.action.MEDIA_MONITOR";

	public static final String HELP_EN = "http://www.homerchess.com/sd-booster/SD-Booster-v2-eng.html";
	public static final String HELP_DE = "http://www.homerchess.com/sd-booster/SD-Booster-v2-ger.html";

	public static final String BOOT = "boot";
	public static final String MONITOR = "monitor";
	public static final String VIRTUAL = "virtual";
	public static final String CHANGE = "change";
	public static final String SERIAL = "serial";
	public static final String UNKNOWN = "unknown";

	public static final String TAG = "SD-Booster";

	public static void sendMessage(final Handler handler, int arg1, int arg2,
			int what, Object obj) {

		Message msg = new Message();

		msg.arg1 = arg1;
		msg.arg2 = arg2;
		msg.what = what;

		if (obj != null) {
			msg.obj = obj;
		}

		handler.sendMessage(msg);
	}

	public static String showCardSize(String size, boolean round) {

		String value = new String("0");

		if (size == null || size.length() < 4) {
			return value;
		}

		for (int i = 0; i < size.length(); i++) {
			if (!Character.isDigit(size.charAt(i))) {
				return value;
			}
		}

		long bytes = Long.valueOf(size) * 512;
		double gigaBytes = Double.valueOf(bytes / 1024);

		gigaBytes /= 1024;
		gigaBytes /= 1024;

		if (!round) {
			NumberFormat nf = NumberFormat.getInstance();
			nf.setMinimumFractionDigits(2);
			nf.setMaximumFractionDigits(2);
			value = nf.format(gigaBytes);
		} else {
			value = String.valueOf(Math.round(gigaBytes));
		}

		return value;
	}

	public static boolean containOnlyNumbers(String string) {

		if (string == null || string.length() == 0 || string.length() > 4)
			return false;

		for (int i = 0; i < string.length(); i++) {

			if (Character.isDigit(string.charAt(i)) == false) {
				return false;
			}
		}

		return true;
	}

	public static boolean containNoNumbers(String string) {

		if (string == null || string.length() == 0)
			return false;

		for (int i = 0; i < string.length(); i++) {

			if (Character.isLetter(string.charAt(i)) == false) {
				return false;
			}
		}

		return true;
	}

	public static boolean cacheSizeIsOk(String size) {

		int value = Integer.parseInt(size);

		if (value < 128 || value > 8192) {
			return false;
		}

		return true;
	}

	public static int alignValue(int value) {
		
		// off_t or int_t on x86 32Bit = 4

		value /= 4;
		value *= 4;

		return value;
	}
}