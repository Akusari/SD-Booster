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
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;

import android.os.Handler;
import android.util.Log;

public class Mmc implements Runnable {

	private static final String BLK_NAME[] = { "mmcblk", "cardblksd" };
	private static final String BLK_PATH = "/sys/block/";

	private static final String CID = "/device/cid";
	private static final String CSD = "/device/csd";
	private static final String DATE = "/device/date";
	private static final String OEM_ID = "/device/oemid";
	private static final String MANUFACTOR_ID = "/device/manfid";
	private static final String SERIAL = "/device/serial";
	private static final String READ_AHEAD = "/bdi/read_ahead_kb";
	private static final String READ_AHEAD_STATIC = "/read_ahead_kb";
	private static final String SIZE = "/size";
	
	private static final String VIRTUAL_DEV_PATH = "/sys/devices/virtual/bdi/";
	private static final String READ_AHEAD_PATH_LIST[] = {
		
		// TODO Should be device model + Android version based

		VIRTUAL_DEV_PATH + "179:0", 
		VIRTUAL_DEV_PATH + "179:1"
	};

	private final Handler handler;
	private final int state;

	private Kernel kernel;
	
	private ArrayList<MmcModell> mmcList;
	private ArrayList<String> virtualList;

	private class MmcFileNameFilter implements FilenameFilter {

		private int type;

		public MmcFileNameFilter(int type) {
			this.type = type;
		}

		public boolean accept(File dir, String filename) {

			switch (this.type) {
			case 0:
				if (filename.contains(Mmc.BLK_NAME[0])) {
					return true;
				}
				break;

			case 1:
				if (filename.equals(Mmc.BLK_NAME[1])) {
					return true;
				}
				break;

			default:
				throw new RuntimeException();

			}

			return false;
		}
	}

	public Mmc(Handler handler, int state) {

		this.state = state;
		this.handler = handler;

		kernel = new Kernel(handler, state);
		
		mmcList = new ArrayList<MmcModell>();
		virtualList = new ArrayList<String>();
	}

	@Override
	public void run() {

		mmcList = new ArrayList<MmcModell>();
		virtualList = new ArrayList<String>();
		
		boolean detected = buildDeviceList();
		
		if (!detected) {
			detected = buildDynamicDeviceList();
		}
		
		if (!detected) {
			detected = buildStaticDeviceList();
		}

		if (detected) {
			Utils.sendMessage(this.handler, 1, 0, this.state, null);
		} else {
			Utils.sendMessage(this.handler, 1, 1, this.state, null);
		}
	}

	private boolean buildDeviceList() {

		File dir = new File(BLK_PATH + ".");

		for (int i = 0; i < BLK_NAME.length; i++) {

			for (String entry : dir.list(new MmcFileNameFilter(i))) {

				MmcModell sdCard = new MmcModell();

				sdCard.setType(i + 1);
				sdCard.setName(entry);

				sdCard.setAheadValue(getDeviceValue(entry, READ_AHEAD, false));
				sdCard.setAheadPath(BLK_PATH + entry + READ_AHEAD);
				sdCard.setSize(getDeviceValue(entry, SIZE, false));
				sdCard.setAheadUser("0");

				switch (sdCard.getType()) {

				case 1: // mmcblk
					sdCard.setCid(getDeviceValue(entry, CID, false));
					sdCard.setCsd(getDeviceValue(entry, CSD, false));
					sdCard.setDate(getDeviceValue(entry, DATE, false));
					sdCard.setOemId(getDeviceValue(entry, OEM_ID, false));
					sdCard.setManufactorId(getDeviceValue(entry, MANUFACTOR_ID,
							false));
					sdCard.setSerial(getDeviceValue(entry, SERIAL, false));
					break;

				case 2: // cardblksd
					sdCard.setCid(Utils.UNKNOWN);
					sdCard.setCsd(Utils.UNKNOWN);
					sdCard.setDate(Utils.UNKNOWN);
					sdCard.setOemId(Utils.UNKNOWN);
					sdCard.setManufactorId(Utils.UNKNOWN);

					int hash = (sdCard.getName() + sdCard.getSize()).hashCode();
					sdCard.setSerial(Integer.toHexString(hash));
					break;

				default:
					throw new RuntimeException();
				}

				if (sdCard.deviceIsUseable()) {

					// TODO list.add() doesn't work here
					// because of freaky devices like mmcblk1boot0, mmcblk0boot1

					if (sdCard.deviceIsOk()) {

						mmcList.add(sdCard);
						logCard(sdCard);
					} else {
						Log.e(Utils.TAG, "Device is not useable: MmcModell:"
								+ sdCard.toString());
					}
				} else {
					Log.e(Utils.TAG,
							"Unknown card state: MmcModell:"
									+ sdCard.toString());
				}
			}
		}

		if (mmcList.size() == 0) {
			Log.w(Utils.TAG, "No block devices found!");
			return false;
		}

		return true;
	}
	
	private boolean buildDynamicDeviceList() {
		
		ArrayList<String> deviceList = new ArrayList<String>();
		Kernel.addSDMount(deviceList);
			
		for (String cardPath : deviceList) {
			
			if (new File(cardPath).exists()) {
				
				String data[] = cardPath.split("/");
				
				if (data.length >= 6) {
					if (data[3].contains(Utils.VIRTUAL)) {
						Log.i(Utils.TAG, "Virtual device " + cardPath + " exist");
						virtualList.add(cardPath);
						continue;
					}
				}
				
				Log.i(Utils.TAG, "Device " + cardPath + " exist");
				
				String name;
				if (data.length > 0) {
					name = data[data.length - 1];
					Log.i(Utils.TAG, "Device name: " + name);
				} else {
					Log.i(Utils.TAG, "Device name: " + cardPath + " rejected");
					continue;
				}
				
				MmcModell sdCard = new MmcModell();
						
				sdCard.setType(3);
				sdCard.setName(name);		
				sdCard.setCid(Utils.UNKNOWN);
				sdCard.setCsd(Utils.UNKNOWN);
				sdCard.setDate(Utils.UNKNOWN);
				sdCard.setOemId(Utils.UNKNOWN);
				sdCard.setManufactorId(Utils.UNKNOWN);
				
				sdCard.setAheadValue(getDeviceValue(name, READ_AHEAD, false));
				sdCard.setAheadPath(BLK_PATH + name + READ_AHEAD);
				sdCard.setSize(getDeviceValue(name, SIZE, false));
				sdCard.setAheadUser("0");
				sdCard.setSerial(getDeviceValue(name, SERIAL, false));
				
				try {
					if (sdCard.getSerial() == null) {
						int hash = (sdCard.getName() + sdCard.getSize()).hashCode();
						sdCard.setSerial(Integer.toHexString(hash));		
					}
				} catch (Exception e) {
					Log.w(Utils.TAG, "buildDynamicDeviceList(): " + e.toString());
				}
						
				if (sdCard.deviceIsOk()) {
					mmcList.add(sdCard);
					logCard(sdCard);
				} else {
					Log.w(Utils.TAG, "Device is not useable: MmcModell:"
							+ sdCard.toString());
				}	
			} else {
				Log.i(Utils.TAG, "Device " + cardPath + " rejected");
			}
		}
		
		if (mmcList.size() == 0) {
			Log.w(Utils.TAG, "No dynamic devices found!");
			return false;
		}
		
		return true;
	}

	private boolean buildStaticDeviceList() {
		
		for (String staticPath : READ_AHEAD_PATH_LIST) {
			virtualList.add(staticPath);
		}
		
		for (String cardPath : virtualList) {
			
			if (new File(cardPath).exists()) {
				
				MmcModell sdCard = new MmcModell();
				String data[] = cardPath.split("/");
				
				// data[3] = virtual
				// data[5] = 179:0
				
				sdCard.setType(0);
				sdCard.setName(data[5]);
				sdCard.setCid(data[3]);
				sdCard.setCsd(data[3]);
				sdCard.setDate(data[3]);
				sdCard.setOemId(data[3]);
				sdCard.setManufactorId(data[3]);
				sdCard.setSerial(data[3]);
				sdCard.setAheadValue(getDeviceValue(cardPath, READ_AHEAD_STATIC, true));
				sdCard.setAheadPath(cardPath + READ_AHEAD_STATIC);
				sdCard.setSize(data[3]);	
				sdCard.setAheadUser("0");
				
				// safty first
				
				if (new File(sdCard.getAheadPath()).exists()) {
					mmcList.add(sdCard);
					logCard(sdCard);
				} else {
					Log.e(Utils.TAG, "Ahead path is wrong! " + "MmcModell: "
							+ sdCard.toString());
				}
			}
		}
				
		if (mmcList.size() == 0) {
			Log.w(Utils.TAG, "No virtual devices found!");
			return false;
		}
		
		return true;	
	}
	
	public void setToKernel(int cacheSize) {

		kernel.allOnBoot = false;
		kernel.allOnMonitor = false;
		kernel.setArrayList(mmcList);
		kernel.setCacheSize(String.valueOf(cacheSize));

		Thread thread = new Thread(kernel);
		thread.start();
	}

	public void setToKernel(int cacheSize, boolean allOnBoot) {

		kernel.allOnBoot = allOnBoot;
		kernel.allOnMonitor = false;
		kernel.setArrayList(mmcList);
		kernel.setCacheSize(String.valueOf(cacheSize));

		Thread thread = new Thread(kernel);
		thread.start();
	}

	public void monitorToKernel(int cacheSize, boolean allOnMonitor) {

		kernel.allOnBoot = false;
		kernel.allOnMonitor = allOnMonitor;
		kernel.setArrayList(mmcList);
		kernel.setCacheSize(String.valueOf(cacheSize));

		Thread thread = new Thread(kernel);
		thread.start();
	}

	public void changeToKernel(int cacheSize, boolean allOnMonitor,
			boolean allOnBoot) {

		kernel.allOnBoot = allOnBoot;
		kernel.allOnMonitor = allOnMonitor;
		kernel.setArrayList(mmcList);
		kernel.setCacheSize(String.valueOf(cacheSize));

		Thread thread = new Thread(kernel);
		thread.start();
	}

	public ArrayList<MmcModell> getList() {
		return mmcList;
	}
	
	public int getListSize() {
		return mmcList.size();	
	}
	
	private void logCard(final MmcModell card) {
		
		Log.i(Utils.TAG, "Found: " 
				+ card.getName() + "/" 
				+ card.getCid() + "/"
				+ card.getCsd() + "/" 
				+ card.getDate() + "/"
				+ card.getOemId() + "/"
				+ card.getManufactorId() + "/"
				+ card.getSerial() + "/"
				+ card.getAheadValue() + "/" + "p"
				+ card.getAheadPath() + "/"
				+ card.getSize() + "/"
				+ card.getType());	
	}

	@SuppressWarnings("deprecation")
	private String getDeviceValue(String device, String path, boolean virtual) {

		File file = null;
		String value = null;
		DataInputStream data = null;
		FileInputStream input = null;
		BufferedInputStream buffer = null;

		if (!virtual) {
			file = new File(BLK_PATH + device + path);
		} else {
			file = new File(device + path);
		}

		try {

			input = new FileInputStream(file);
			buffer = new BufferedInputStream(input);
			data = new DataInputStream(buffer);

			value = data.readLine();

			if (data != null) data.close();
			if (buffer != null) buffer.close();
			if (input != null) input.close();

		} catch (Exception e) {
			Log.e(Utils.TAG, "MMC: " + e.toString());
			return value;
		}

		return value;
	}
}