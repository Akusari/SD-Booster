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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.os.Handler;
import android.util.Log;

public class Kernel implements Runnable {

	public boolean allOnBoot;
	public boolean allOnMonitor;

	private static final String SWITCH_USER = "su";
	private static final String EXIT_SHELL = "exit $?";

	private final int state;

	private String cacheSize;
	private ArrayList<MmcModell> list;

	private final Handler handler;

	public Kernel(Handler handler, int state) {
		this.handler = handler;
		this.state = state;

		allOnBoot = false;
		allOnMonitor = false;
	}

	@Override
	public void run() {

		int result;

		/*
		 * state 0 = on GUI 
		 * state 1 = on Service (boot) 
		 * state 2 = on Service (monitor)
		 * state 3 = on Service (change)
		 */

		if (state == 0 || state == 1) {
			result = setCache(list, cacheSize);
		} else if (state == 2) {
			result = doMonitor(list, cacheSize);
		} else {
			result = setCache(list, cacheSize);
		}

		if (result == 3) {
			Utils.sendMessage(handler, 0, 2, 1, null);
		} else {
			Utils.sendMessage(handler, 0, result, 0, null);
		}
	}

	public void setCacheSize(String size) {
		cacheSize = size;
	}

	public void setArrayList(ArrayList<MmcModell> cards) {
		list = cards;
	}

	public static String getLogcat() {

		// android.permission.READ_LOGS not needed

		String log = null;

		try {

			Process process = Runtime.getRuntime().exec(SWITCH_USER);
			DataOutputStream os = new DataOutputStream(
					process.getOutputStream());
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(process.getInputStream()));

			os.writeBytes("logcat -d" + "\n");
			os.writeBytes(EXIT_SHELL + "\n");
			os.flush();

			StringBuilder logcat = new StringBuilder();
			String line;

			while ((line = bufferedReader.readLine()) != null) {
				if (line.contains(Utils.TAG)) {
					logcat.append(line + "\n");
				}
			}

			log = logcat.toString();

			os.close();
			bufferedReader.close();

		} catch (Exception e) {
			Log.w(Utils.TAG, "getLogcat(): " + e.toString());
		}

		return log;
	}

	public static String showSystem() {

		StringBuilder devices = new StringBuilder();

		try {
			File file = new File("/sys");

			if (!file.exists()) {
				devices.append("SYS fs doesn't exist\n");
				return devices.toString();
			}

			file = new File("/sys/block");

			if (!file.exists()) {
				devices.append("Block doesn't exist\n");
				return devices.toString();
			}

			devices.append("List:\n");

			for (String dir : file.list()) {

				devices.append(dir + "\n");

				if (Utils.containNoNumbers(dir)
						|| (dir.contains("0") && !dir.contains("ram")
								&& !dir.contains("loop") && !dir
									.contains("dm-"))) {

					File card = new File("/sys/block/" + dir);

					for (File entry : card.listFiles()) {
						devices.append("     "
								+ entry.getName()
								+ (entry.isDirectory() == true ? "(dir)"
										: "(file)") + "\n");
					}
				}
			}

		} catch (Exception e) {
			Log.w(Utils.TAG, "showSystem(): " + e.toString());
			return null;
		}

		return devices.toString();
	}
	
	public static void addSDMount(final ArrayList<String> mountPoints) {
			
		try {
			
			Process process = Runtime.getRuntime().exec("mount");
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(process.getInputStream()));
			
			String line;		
			while ((line = bufferedReader.readLine()) != null) {
				
				if (line.contains("vfat") && !line.contains("secure")) {
					String path = (line.split(" "))[0];
					
					if (path.startsWith("/dev/block/vold/")) {
						path = path.replace("/dev/block/vold/", "/sys/devices/virtual/bdi/");
					} else if (path.startsWith("/dev/block")) {
						path = path.replace("/dev/block", "/sys/block");
					}
					Log.i(Utils.TAG, "Device " + path + " added");
					mountPoints.add(path);				
				}
			}
			
			bufferedReader.close();
			process.destroy();
			
		} catch (Exception e) {
			Log.w(Utils.TAG, "addSDMount(): " + e.toString());
		}
	}

	private int setCache(ArrayList<MmcModell> list, String cacheValue) {

		String cardPath = null;
		String commandLine = null;
		String value = null;
		int result = 0;
		boolean cardFree = false;
		boolean cardFailed = false;
		boolean cardBased;

		if (!cacheValue.equals("0")) {
			cardBased = false;
		} else {
			cardBased = true;
		}

		for (int i = 0; i < list.size(); i++) {

			MmcModell card = list.get(i);

			if (card.deviceIsUseable()) {

				cardPath = card.getAheadPath();

				if (new File(cardPath).exists() == false) {

					Log.e(Utils.TAG, "setCache(): Device doesn't exist");
					continue;
				}

				if (result == 0) {
					result = 1;
				}

				if (cardBased) {
					value = card.getAheadUser();
				} else {
					value = cacheValue;
				}

				if (state == 1) {
					if (!allOnBoot && !card.isOnBoot()) {
						Utils.sendMessage(handler, 0, 5, 0, card);
						cardFree = true;
						continue;
					}
				}

				if (!card.isSetup() && value.equals("0")) {
					Utils.sendMessage(handler, 0, 5, 0, card);
					cardFree = true;
					continue;
				}

				if (!Utils.cacheSizeIsOk(value)) {
					int what;

					if (cardBased) {
						what = 1;
					} else {
						what = 0;
					}

					Utils.sendMessage(handler, 0, 5, what, card);
					cardFree = true;
					continue;
				}

				commandLine = new String("echo " + value + " > " + cardPath);
				Log.i(Utils.TAG, "setCache(): " + commandLine);

				if (setCommand(commandLine, true) == false) {
					Log.i(Utils.TAG, "setCommand(): failed");
					cardFailed = true;
					Utils.sendMessage(handler, 0, 4, 0, card);
				} else {
					result = 2;
					card.setAheadValue(value);

					if (cardBased) {
						Utils.sendMessage(handler, 0, 3, 0, card);
					}
				}
			}
		}

		if (result < 2) {
			if (result == 1 && cardFree && !cardFailed) {
				result = 2; // HACK
			}
		}

		if (cardFailed) {
			result = 3;
		}

		return result;
	}

	private int doMonitor(ArrayList<MmcModell> list, String cacheValue) {

		String cardPath = null;
		String commandLine = null;
		String value = null;
		int result = 0;
		boolean cardFree = false;
		boolean cardFailed = false;
		boolean cardBased;

		if (!cacheValue.equals("0")) {
			cardBased = false;
		} else {
			cardBased = true;
		}

		for (int i = 0; i < list.size(); i++) {

			MmcModell card = list.get(i);

			if (card.deviceIsUseable()) {

				cardPath = card.getAheadPath();

				if (new File(cardPath).exists() == false) {
					Log.i(Utils.TAG, "doMonitor(): Device doesn't exist");
					continue;
				}

				if (result == 0) {
					result = 1;
				}

				if (cardBased) {
					value = card.getAheadUser();
				} else {
					value = cacheValue;
				}

				if (!allOnMonitor && !card.isOnMonitor()) {
					Utils.sendMessage(handler, 0, 5, 0, card);
					cardFree = true;
					continue;
				}

				if (value.equals(card.getAheadValue())) {
					Log.i(Utils.TAG, "Device " + card.getName() + " is ok");
					cardFree = true;
					continue;
				}

				if (!Utils.cacheSizeIsOk(value)) {
					int what;

					if (cardBased) {
						what = 1;
					} else {
						what = 0;
					}

					Utils.sendMessage(handler, 0, 5, what, card);
					cardFree = true;
					continue;
				}

				commandLine = new String("echo " + value + " > " + cardPath);
				Log.i(Utils.TAG, "setCache(): " + commandLine);

				if (setCommand(commandLine, true) == false) {
					Log.i(Utils.TAG, "setCommand(): failed");
					cardFailed = true;
					Utils.sendMessage(handler, 0, 4, 0, card);
				} else {
					result = 2;
					card.setAheadValue(value);

					if (cardBased) {
						Utils.sendMessage(handler, 0, 3, 0, card);
					}
				}
			}
		}

		if (result < 2) {
			if (result == 1 && cardFree && !cardFailed) {
				result = 2; // HACK
			}
		}

		if (cardFailed) {
			result = 3;
		}

		return result;
	}

	private boolean setCommand(String command, boolean needRoot) {

		// TODO do thread based (helper) and abort after x seconds

		Process process = null;
		DataOutputStream os = null;
		int rc = -1;

		try {

			if (needRoot == true) {

				process = Runtime.getRuntime().exec(SWITCH_USER);

				os = new DataOutputStream(process.getOutputStream());
				os.writeBytes(command + "\n");
				os.writeBytes(EXIT_SHELL + "\n");
				os.flush();
			} else {
				process = Runtime.getRuntime().exec(
						command + ";" + EXIT_SHELL + "\n");
			}

			process.waitFor(); // TODO ugly in a single thread
			rc = process.exitValue();

			if (rc > 0) {
				return false;
			}

		} catch (Exception e) {

			Log.w(Utils.TAG, "setCommand() exception: " + e.getMessage());
			return false;
		}

		// cleanup

		finally {

			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					Log.w(Utils.TAG,
							"setCommand() exception: " + e.getMessage());
				}
			}

			if (process != null && rc == -1) {
				process.destroy();
			}
		}

		return true;
	}
}