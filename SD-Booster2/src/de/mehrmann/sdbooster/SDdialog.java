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

import java.io.IOException;
import java.io.InputStream;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("InlinedApi")
public class SDdialog extends Dialog {

	private static final int BUILD_VERSION = 14; // Android ICS 4.0.0
	@SuppressWarnings("unused")
	private final Context context;
	private final View content;

	@SuppressWarnings("unused")
	private final MmcModell card;

	private int title;
	private ImageView uiButtonOk;
	private ImageView uiButtonCancel;

	public SDdialog(final Context context, int title) {

		super(new ContextThemeWrapper(context, R.style.DialogTheme));
		this.context = context;
		this.card = null;
		this.title = title;
		this.setCancelable(false);

		final Resources resources = context.getResources();

		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		content = inflater.inflate(R.layout.dialog_info, null);

		String titleText = resources.getString(R.string.dlg_info_label);
		this.setTitle(titleText);

		final TextView textView = (TextView) content
				.findViewById(R.id.dlg_info_general_text);

		uiButtonOk = (ImageView) content.findViewById(R.id.dlg_info_btn_ok);
		uiButtonOk.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		if (title == 3) {

			// general info

			String contentText = resources
					.getString(R.string.dlg_test_no_mount);
			textView.setText(contentText);

		} else {

			// reserved

			throw new RuntimeException();
		}

		if (android.os.Build.VERSION.SDK_INT >= BUILD_VERSION) {

			int color = context.getResources().getColor(
					android.R.color.holo_blue_light);

			View view = content.findViewById(R.id.divider);
			view.setBackgroundColor(color);
		}

		this.requestWindowFeature(Window.FEATURE_LEFT_ICON);
		this.setContentView(content);
	}

	public SDdialog(final Context context, final Handler uiHandler, int title) {

		super(new ContextThemeWrapper(context, R.style.DialogTheme));
		this.context = context;
		this.card = null;
		this.title = title;
		this.setCancelable(false);

		final Resources resources = context.getResources();

		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		if (title == 2) {

			// License

			content = inflater.inflate(R.layout.dialog_license, null);

			String license = resources.getString(R.string.dlg_license_label);
			this.setTitle(license);
			
			final TextView textView = (TextView) content.findViewById(R.id.dlg_license_text);

			uiButtonCancel = (ImageView) content
					.findViewById(R.id.dlg_license_btn_cancel);
			uiButtonCancel.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View arg0) {
					Utils.sendMessage(uiHandler, 2, 0, 0, null);
					dismiss();
				}
			});

			uiButtonOk = (ImageView) content
					.findViewById(R.id.dlg_license_btn_ok);
			uiButtonOk.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View arg0) {
					Database dbAdapter = SDbooster.getDbInstance();
					dbAdapter.setPref(3, 1, 0);

					Utils.sendMessage(uiHandler, 3, 1, 0, null);
					dismiss();
				}
			});
					
			String licenseContent;
			try {
				
				InputStream text = context.getAssets().open("license.txt");
				StringBuilder content = new StringBuilder(); 
				byte buffer[] = new byte[1024];
				int count;
				
				while ((count = text.read(buffer, 0, 1024)) != -1) {
					content.append(new String(buffer, 0, count, "utf-8"));
				}
				
				licenseContent = content.toString();
			} catch (IOException e) {
				licenseContent = resources.getString(R.string.dlg_license_text);
			}
			textView.setText(licenseContent);
			
		} else {

			// Startup help screen

			content = inflater.inflate(R.layout.dialog_info, null);

			String titleText = resources.getString(R.string.dlg_info_label);
			this.setTitle(titleText);

			final TextView textView = (TextView) content
					.findViewById(R.id.dlg_info_general_text);

			uiButtonOk = (ImageView) content.findViewById(R.id.dlg_info_btn_ok);
			uiButtonOk.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					Utils.sendMessage(uiHandler, 3, 0, 0, null);
					dismiss();
				}
			});

			String contentText = resources.getString(R.string.dlg_setup_text);
			textView.setText(contentText);

		}

		if (android.os.Build.VERSION.SDK_INT >= BUILD_VERSION) {

			int color = context.getResources().getColor(
					android.R.color.holo_blue_light);

			View view = content.findViewById(R.id.divider);
			view.setBackgroundColor(color);
		}

		this.requestWindowFeature(Window.FEATURE_LEFT_ICON);
		this.setContentView(content);
	}

	public SDdialog(final Context context, int title, final MmcModell card,
			final Handler uiHandler, int position) {

		super(new ContextThemeWrapper(context, R.style.DialogTheme));

		this.context = context;
		this.title = title;
		this.card = card;

		final Resources resources = context.getResources();

		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		if (title == 0) {

			// Info

			content = inflater.inflate(R.layout.dialog_card_info, null);

			String info = resources.getString(R.string.dlg_info_label);
			this.setTitle(info);

			final TextView cardName = (TextView) content
					.findViewById(R.id.dlg_info_name);
			final TextView cardSize = (TextView) content
					.findViewById(R.id.dlg_info_part);
			final TextView cardCache = (TextView) content
					.findViewById(R.id.dlg_info_cache);
			final TextView cardDate = (TextView) content
					.findViewById(R.id.dlg_info_date);
			final TextView cardSerial = (TextView) content
					.findViewById(R.id.dlg_info_serial);

			uiButtonOk = (ImageView) content.findViewById(R.id.dlg_info_btn_ok);
			uiButtonOk.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View arg0) {
					dismiss();
				}
			});

			cardName.setText(card.getName());
			cardCache.setText(card.getAheadValue() + " KB");
			cardSerial.setText(card.getSerial());

			if (card.getDate().equals(Utils.UNKNOWN)) {
				cardDate.setText(resources
						.getString(R.string.dlg_card_info_unknown_label));
			} else {
				cardDate.setText(card.getDate());
			}

			if (card.getCid().equals(Utils.VIRTUAL)) {
				cardSize.setText(Utils.VIRTUAL);
			} else {
				cardSize.setText(Utils.showCardSize(card.getSize(), false)
						+ " GB");
			}

		} else {

			// setup

			content = inflater.inflate(R.layout.dialog_setup, null);

			String setup = resources.getString(R.string.dlg_setup_label);
			this.setTitle(setup);

			final TextView cardName = (TextView) content
					.findViewById(R.id.dlg_setup_name_display);

			final TextView cardCacheDisplay = (TextView) content
					.findViewById(R.id.dlg_setup_cache_display);

			final TextView cardCacheUserDisplay = (TextView) content
					.findViewById(R.id.dlg_setup_cache_user_display);

			final EditText cardCacheEdit = (EditText) content
					.findViewById(R.id.dlg_setup_cache_edit);

			final CheckBox cardOnBoot = (CheckBox) content
					.findViewById(R.id.checkbox_boot_card);

			final CheckBox cardOnMonitor = (CheckBox) content
					.findViewById(R.id.checkbox_monitor_card);

			final boolean cardOnBootState = card.isOnBoot();
			final boolean cardOnMonitorState = card.isOnMonitor();

			uiButtonCancel = (ImageView) content
					.findViewById(R.id.dlg_setup_btn_cancel);
			uiButtonCancel.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View arg0) {
					dismiss();
				}
			});

			uiButtonOk = (ImageView) content
					.findViewById(R.id.dlg_setup_btn_ok);
			uiButtonOk.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View arg0) {

					boolean onBoot = false;
					boolean onMonitor = false;
					boolean onSize = false;
					
					String size = cardCacheEdit.getText().toString();
					
					if ((cardOnBoot.isChecked() || cardOnMonitor.isChecked())) {	
						if (size.length() != 0 && (!Utils.containOnlyNumbers(size)
								|| !Utils.cacheSizeIsOk(size))) {
							
							Toast.makeText(context, resources
									.getString(R.string.dlg_setup_cache_size_error),
									Toast.LENGTH_SHORT).show();
							return;
							
						} else if (size.length() == 0 && !Utils.cacheSizeIsOk(card.getAheadUser())) {
							
							Toast.makeText(context, resources
									.getString(R.string.dlg_setup_cache_size_error),
									Toast.LENGTH_SHORT).show();
							return;
							
						}
						
					}

					if (size.length() > 0) {
						if (Utils.containOnlyNumbers(size) && Utils.cacheSizeIsOk(size)) {		
							int value = Integer.valueOf(size);
							value = Utils.alignValue(value);
	
							card.setAheadUser(String.valueOf(value));
							card.setSetup(true);
							onSize = true;	
						} else {
							Toast.makeText(context, resources
									.getString(R.string.dlg_setup_cache_size_error),
									Toast.LENGTH_SHORT).show();
							return;		
						}
					}
					
					InputMethodManager inputManager = (InputMethodManager) context
							.getSystemService(Service.INPUT_METHOD_SERVICE);

					inputManager.hideSoftInputFromWindow(
							cardCacheEdit.getWindowToken(), 0);


					if (cardOnBoot.isChecked() != cardOnBootState) {
						card.setOnBoot(cardOnBoot.isChecked());
						onBoot = true;
					}

					if (cardOnMonitor.isChecked() != cardOnMonitorState) {
						card.setOnMonitor(cardOnMonitor.isChecked());
						onMonitor = true;
					}

					if (onSize || onBoot || onMonitor) {
						Database dbAdapter = SDbooster.getDbInstance();
						dbAdapter.cardUpdate(card, onBoot, onMonitor, onSize);

						Utils.sendMessage(uiHandler, 5, 0, 0, card);
					}

					dismiss();
				}
			});

			cardCacheDisplay.setText(card.getAheadValue());
			cardName.setText(card.getName());
			cardOnBoot.setChecked(cardOnBootState);
			cardOnMonitor.setChecked(cardOnMonitorState);

			// UserValue

			String userCacheValue = card.getAheadUser();

			if (Utils.containOnlyNumbers(userCacheValue)
					&& Utils.cacheSizeIsOk(userCacheValue)) {
				cardCacheUserDisplay.setText("/" + userCacheValue);
			} else {
				cardCacheUserDisplay.setText("/" + "0");
			}
		}

		if (android.os.Build.VERSION.SDK_INT >= BUILD_VERSION) {

			int color = context.getResources().getColor(
					android.R.color.holo_blue_light);

			View view = content.findViewById(R.id.divider);
			view.setBackgroundColor(color);
		}

		this.requestWindowFeature(Window.FEATURE_LEFT_ICON);
		this.setContentView(content);
	}

	public void useIcon() {

		switch (title) {

		case 0:
		case 2:
		case 3:
		case 4:
			this.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
					R.drawable.info);
			break;

		case 1:
			this.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
					R.drawable.setup);
			break;

		default:
			throw new RuntimeException();
		}
	}

	public View getContentView() {
		return content;
	}
}
