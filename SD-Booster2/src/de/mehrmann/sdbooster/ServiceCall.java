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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ServiceCall extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		Intent service = new Intent(context, ServiceStart.class);
		service.putExtra(Utils.BOOT, false);

		String action = intent.getAction().toString();

		if (action.equals(Utils.MEDIA_MOUNTED)) {

			service.putExtra(Utils.CHANGE, true);
			service.putExtra(Utils.MONITOR, false);

		} else if (action.equals(Utils.MEDIA_MONITOR)) {

			service.putExtra(Utils.MONITOR, true);
			service.putExtra(Utils.CHANGE, false);

		} else {
			throw new RuntimeException();
		}

		context.startService(service);
	}
}
