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

public class SpeedModell {

	double writeSpeed;
	double readSpeed;
	int usedTime;

	public SpeedModell() {
		writeSpeed = 0.0;
		readSpeed = 0.0;
		usedTime = 0;
	}

	public double getWriteSpeed() {
		return writeSpeed;
	}

	public void setWriteSpeed(double writeSpeed) {
		this.writeSpeed = writeSpeed;
	}

	public double getReadSpeed() {
		return readSpeed;
	}

	public void setReadSpeed(double readSpeed) {
		this.readSpeed = readSpeed;
	}

	public int getUsedTime() {
		return usedTime;
	}

	public void setUsedTime(int usedTime) {
		this.usedTime = usedTime;
	}
}
