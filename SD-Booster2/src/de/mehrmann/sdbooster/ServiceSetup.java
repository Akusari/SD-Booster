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

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class ServiceSetup {

	private final Context context;

	public ServiceSetup(Context context) {
		this.context = context;
	}

	public void configuration(boolean delete) {

		Intent intent = new Intent(context, ServiceCall.class);
		intent.setAction(Utils.MEDIA_MONITOR);

		PendingIntent pending = PendingIntent.getBroadcast(context, 0, intent,
				PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager manager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);

		if (delete) {
			manager.cancel(pending);
		}

		Database dbAdapter = new Database(context);
		long period;

		if (dbAdapter.getPref(1, 1) == 0) {
			dbAdapter.close();
			return;
		} else {
			period = (dbAdapter.getPref(6, 1) * 60) * 1000;
			dbAdapter.close();
		}

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.SECOND, 60);

		manager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
				cal.getTimeInMillis(), period, pending);
	}
}
