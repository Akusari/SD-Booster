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

public class MmcModell {

	private String name;
	private String cid;
	private String csd;
	private String date;
	private String oemId;
	private String manufactorId;
	private String serial;
	private String aheadPath;
	private String aheadValue;
	private String aheadUser;
	private String size;

	private boolean setup;
	private boolean onBoot;
	private boolean onMonitor;

	private long id;
	private int type;

	public MmcModell() {

		name = null;
		cid = null;
		csd = null;
		date = null;
		oemId = null;
		manufactorId = null;
		serial = null;
		aheadPath = null;
		aheadValue = null;
		aheadUser = null;
		size = null;

		setup = false;
		onBoot = false;
		onMonitor = false;

		id = -1;
		type = -1;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCid() {
		return cid;
	}

	public void setCid(String cid) {
		this.cid = cid;
	}

	public String getCsd() {
		return csd;
	}

	public void setCsd(String csd) {
		this.csd = csd;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getOemId() {
		return oemId;
	}

	public void setOemId(String oemId) {
		this.oemId = oemId;
	}

	public String getManufactorId() {
		return manufactorId;
	}

	public void setManufactorId(String manufactorId) {
		this.manufactorId = manufactorId;
	}

	public String getSerial() {
		return serial;
	}

	public void setSerial(String serial) {
		this.serial = serial;
	}

	public String getAheadPath() {
		return aheadPath;
	}

	public void setAheadPath(String aheadPath) {
		this.aheadPath = aheadPath;
	}

	public String getAheadValue() {
		return aheadValue;
	}

	public void setAheadValue(String aheadValue) {
		this.aheadValue = aheadValue;
	}

	public String getAheadUser() {
		return aheadUser;
	}

	public void setAheadUser(String aheadUser) {
		this.aheadUser = aheadUser;
	}

	public String getSize() {
		return size;
	}

	public void setSize(String size) {
		this.size = size;
	}

	public boolean isSetup() {
		return setup;
	}

	public void setSetup(boolean setup) {
		this.setup = setup;
	}

	public boolean isOnBoot() {
		return onBoot;
	}

	public void setOnBoot(boolean onBoot) {
		this.onBoot = onBoot;
	}

	public boolean isOnMonitor() {
		return onMonitor;
	}

	public void setOnMonitor(boolean onMonitor) {
		this.onMonitor = onMonitor;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public boolean deviceIsOk() {

		if (name == null) 		return false;
		if (serial == null) 	return false;
		if (aheadPath == null) 	return false;
		if (aheadValue == null)	return false;
		if (size == null)		return false;
		if (date == null)		return false;
		if (type < 0)			return false;
		if (cid == null)		return false; // used by ListAdapter

		/*
		 * List of types:
		 * 
		 * type 0 virtual 
		 * type 1 mmcblk 
		 * type 2 cardblksd
		 * type 3 mountpoint based (virtual)
		 */

		if (type < 2) {
			if (csd == null)			return false;
			if (oemId == null)			return false;
			if (manufactorId == null)	return false;
		}

		return true;
	}

	public boolean deviceIsUseable() {

		if (name == null)		return false;
		if (aheadPath == null)	return false;
		if (aheadValue == null)	return false;
		if (serial == null)		return false;
		if (type < 0)			return false;
		if (cid == null)		return false; // used by ListAdapter

		if (!Utils.containOnlyNumbers(aheadValue)) {
			return false;
		}

		return true;
	}

	@Override
	public String toString() {

		String modell = new String();

		modell = " name = " 		+ getValue(this.name);
		modell += " cid = " 		+ getValue(this.cid);
		modell += " csd = " 		+ getValue(this.csd);
		modell += " date = " 		+ getValue(this.date);
		modell += " oemId = " 		+ getValue(this.oemId);
		modell += " manufactorId = "+ getValue(this.manufactorId);
		modell += " serial = " 		+ getValue(this.serial);
		modell += " aheadPath = " 	+ getValue(this.aheadPath);
		modell += " aheadValue = " 	+ getValue(this.aheadValue);
		modell += " aheadUser = " 	+ getValue(this.aheadUser);
		modell += " size = " 		+ getValue(this.size);
		modell += " setup = " 		+ this.setup;
		modell += " onBoot = " 		+ this.onBoot;
		modell += " onMonitor = " 	+ this.onMonitor;
		modell += " id = " 			+ this.id;
		modell += " type = " 		+ this.type;

		return modell;
	}

	private String getValue(String data) {

		if (data == null) {
			return "null";
		} else {
			return data;
		}
	}
}
