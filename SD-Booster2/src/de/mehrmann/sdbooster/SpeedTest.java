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

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.ContextThemeWrapper;

public class SpeedTest {

	public native void runfNative(SpeedModell modell, String cardPath);

	private final Handler handler;
	private final String cardPath;
	private final Context context;

	private ProgressDialog progressDialog;

	@SuppressLint("HandlerLeak")
	public SpeedTest(Context context) {

		this.context = context;
		cardPath = Environment.getExternalStorageDirectory().getAbsolutePath();

		final Resources resources = context.getResources();
		String title = resources.getString(R.string.dlg_progress_label);
		String message = resources.getString(R.string.dlg_progress_text);

		progressDialog = new ProgressDialog(new ContextThemeWrapper(context, R.style.DialogTheme));
		progressDialog.setTitle(title);
		progressDialog.setMessage(message);
		progressDialog.setIcon(R.drawable.info);
		progressDialog.setCancelable(false);

		handler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				if (msg.arg1 == 0) {
					progressDialog.dismiss();
					showResult();
				}
			}
		};
	}

	public void runTest() {

		if (isOnMount()) {
			Thread thread = new Thread() {
				@Override
				public void run() {
					SpeedModell modell = new SpeedModell();
					runfNative(modell, cardPath);

					Utils.sendMessage(handler, 0, 0, 0, null);
				}
			};

			progressDialog.show();
			thread.start();
		}
	}

	private boolean isOnMount() {

		String state = Environment.getExternalStorageState();

		if (!Environment.MEDIA_MOUNTED.equals(state)) {

			SDdialog dialog = new SDdialog(context, 3);
			dialog.useIcon();
			dialog.show();

			return false;
		}

		return true;
	}

	private void showResult() {

	}

	static {
		System.loadLibrary("speed_test");
	}
}
