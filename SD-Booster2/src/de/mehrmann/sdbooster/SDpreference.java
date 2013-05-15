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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.Spinner;

@SuppressLint("InlinedApi")
public class SDpreference extends Dialog {

	private static final int BUILD_VERSION = 14; // Android ICS 4.0.0

	@SuppressWarnings("unused")
	private final Context context;
	private final View content;
	private final Handler handler;

	private final CheckBox uiUpdates;
	private final CheckBox uiAlarm;
	private final CheckBox uiBoot;
	private final CheckBox uiChange;
	private final CheckBox uiMonitor;

	private final Spinner uiInterval;

	private final ImageView uiButtonRingtone;
	private final ImageView uiButtonOk;
	private final ImageView uiButtonCancel;

	private int interval;

	public SDpreference(Context context, Handler parentHandler) {
		super(new ContextThemeWrapper(context, R.style.DialogTheme));
		this.context = context;
		this.handler = parentHandler;
		this.interval = 0;

		final Resources resources = context.getResources();

		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		content = inflater.inflate(R.layout.preference, null);

		String titleText = resources.getString(R.string.dlg_preference_label);
		this.setTitle(titleText);

		uiUpdates = (CheckBox) content.findViewById(R.id.checkbox_updates);
		uiInterval = (Spinner) content.findViewById(R.id.spinner_interval);

		uiBoot = (CheckBox) content.findViewById(R.id.checkbox_note_boot);
		uiChange = (CheckBox) content.findViewById(R.id.checkbox_note_change);
		uiMonitor = (CheckBox) content.findViewById(R.id.checkbox_note_monitor);

		uiAlarm = (CheckBox) content.findViewById(R.id.checkbox_note_alarm);

		uiButtonRingtone = (ImageView) content.findViewById(R.id.btn_ringtone);
		uiButtonCancel = (ImageView) content
				.findViewById(R.id.dlg_pref_btn_cancel);
		uiButtonOk = (ImageView) content.findViewById(R.id.dlg_pref_btn_ok);

		uiUpdates.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				setOnClickable(isChecked);
			}
		});

		uiInterval.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {

				updateInterval(arg2);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				interval = 0;
			}
		});

		uiButtonRingtone.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Utils.sendMessage(handler, 4, 1, 0, null);
			}
		});

		uiButtonOk.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				updateSettings();
				dismiss();
				Utils.sendMessage(handler, 4, 0, 0, null);
			}
		});

		uiButtonCancel.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				dismiss();
			}
		});

		if (android.os.Build.VERSION.SDK_INT >= BUILD_VERSION) {

			int color = context.getResources().getColor(
					android.R.color.holo_blue_light);

			View view = content.findViewById(R.id.divider);
			view.setBackgroundColor(color);
		}

		Database dbAdapter = SDbooster.getDbInstance();

		uiUpdates.setChecked(dbAdapter.getPref(1, 1) == 1 ? true : false);
		uiAlarm.setChecked(dbAdapter.getPref(2, 1) == 1 ? true : false);
		uiBoot.setChecked(dbAdapter.getPref(3, 1) == 1 ? true : false);
		uiMonitor.setChecked(dbAdapter.getPref(4, 1) == 1 ? true : false);
		uiChange.setChecked(dbAdapter.getPref(5, 1) == 1 ? true : false);
		uiInterval.setSelection(getIntervalDb());

		this.requestWindowFeature(Window.FEATURE_LEFT_ICON);
		this.setContentView(content);
	}

	public void useIcon() {
		this.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
				R.drawable.setup);
	}

	public View getContentView() {
		return content;
	}

	private void setOnClickable(boolean action) {
		if (action == false) {

			uiAlarm.setChecked(false);
			uiBoot.setChecked(false);
			uiChange.setChecked(false);
			uiMonitor.setChecked(false);

			uiAlarm.setEnabled(false);
			uiBoot.setEnabled(false);
			uiChange.setEnabled(false);
			uiMonitor.setEnabled(false);
		} else {
			uiAlarm.setEnabled(true);
			uiBoot.setEnabled(true);
			uiChange.setEnabled(true);
			uiMonitor.setEnabled(true);
		}
	}

	private void updateSettings() {
		Database dbAdapter = SDbooster.getDbInstance();

		dbAdapter.setPref(10, uiUpdates.isChecked() ? 1 : 0, 1);
		dbAdapter.setPref(11, uiAlarm.isChecked() ? 1 : 0, 1);
		dbAdapter.setPref(12, uiBoot.isChecked() ? 1 : 0, 1);
		dbAdapter.setPref(13, uiMonitor.isChecked() ? 1 : 0, 1);
		dbAdapter.setPref(14, uiChange.isChecked() ? 1 : 0, 1);

		if (interval > 0) {
			dbAdapter.setPref(15, interval, 1);
		}
	}

	private void updateInterval(int value) {

		switch (value) {
		case 0:
			interval = 1;
			break;
		case 1:
			interval = 3;
			break;
		case 2:
			interval = 6;
			break;
		case 3:
			interval = 12;
			break;
		case 4:
			interval = 24;
			break;
		default:
			Log.e(Utils.TAG, "Update interval = " + value);
			throw new RuntimeException();

		}

		interval *= 60; // minutes
	}

	private int getIntervalDb() {

		Database dbAdapter = SDbooster.getDbInstance();
		int hours = dbAdapter.getPref(6, 1) / 60;

		switch (hours) {
		case 1:
			return 0;
		case 3:
			return 1;
		case 6:
			return 2;
		case 12:
			return 3;
		case 24:
			return 4;
		default:
			throw new RuntimeException();
		}
	}
}
