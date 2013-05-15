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

import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SDbooster extends Activity {

	private ImageView uiButtonApply;
	private ImageView uiButtonExit;
	private ImageView uiButtonHelp;
	private ImageView uiButtonSetting;
	private ImageView uiButtonReport;

	private CheckBox uiBoxCache;
	private CheckBox uiBoxBoot;
	private CheckBox uiBoxMonitor;

	private EditText uiCacheSize;

	private LinearLayout uiNoCardList;
	private LinearLayout uiCardList;

	private static Handler uiHandler;
	private static Database dbAdapter;
	private static Context context;

	private boolean mmcDetection;

	private ListAdapter adapter;
	private Mmc cards;

	private OnCheckedChangeListener uiBoxListener;

	private String ringtone;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setFormat(PixelFormat.RGBA_8888);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);

		setContentView(R.layout.main);
		init();
	}

	@Override
	public void onStart() {
		super.onStart();
		context = this;
	}

	@Override
	public void onRestart() {
		super.onRestart();
		context = this;
	}

	@Override
	public void onResume() {
		super.onResume();
		context = this;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		// TODO clean impl

		if (data != null) {
			Uri uri = data
					.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

			if (uri != null) {
				ringtone = "content://media" + uri.getEncodedPath();
			}
		}
	}

	@SuppressLint("HandlerLeak")
	private void init() {

		context = this;
		mmcDetection = false;

		Database dbAdapter = getDbInstance();

		uiButtonApply = (ImageView) findViewById(R.id.btn_green);
		uiButtonExit = (ImageView) findViewById(R.id.btn_red);
		uiButtonHelp = (ImageView) findViewById(R.id.btn_blue);
		uiButtonSetting = (ImageView) findViewById(R.id.btn_orange);
		uiButtonReport = (ImageView) findViewById(R.id.btn_report);

		uiBoxCache = (CheckBox) findViewById(R.id.cbx_cache_all);
		uiBoxBoot = (CheckBox) findViewById(R.id.cbx_boot_all);
		uiBoxMonitor = (CheckBox) findViewById(R.id.cbx_monitor_all);

		uiCacheSize = (EditText) findViewById(R.id.cache_edit);

		uiNoCardList = (LinearLayout) findViewById(R.id.card_list_error);
		uiCardList = (LinearLayout) findViewById(R.id.card_list);

		uiBoxListener = new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton view, boolean isChecked) {

				int value = isChecked == true ? 1 : 0;
				setPref((String) view.getTag(), value);

				if (value == 1) {
					showMessage(4, null);
				} else {
					showMessage(8, null);
				}

				String tag = (String) view.getTag();

				// logical verification

				if (tag.equals("cbx_monitor_all")) {
					if (isChecked) {
						Database dbAdapter = getDbInstance();
						if (dbAdapter.getPref(1, 1) == 0) {
							showMessage(10, null);
						}
					}
				}

				// EditText

				if (tag.equals("cbx_cache_all")) {
					if (isChecked) {
						Database dbAdapter = getDbInstance();
						int allSize = dbAdapter.getPref(5, 0);
						if (allSize >= 128 && allSize <= 8192 && allSize != 128) {
							uiCacheSize.setText(String.valueOf(allSize));
						}
					} else {
						uiCacheSize.setText(null);
					}
				}
			}
		};

		uiHandler = new Handler() {

			/*
			 * arg1 0 = Kernel messages 
			 * arg1 1 = MMC detection 
			 * arg1 2 = App exit
			 * arg1 3 = License 
			 * arg1 4 = Preferences 
			 * arg1 5 = card setup
			 * 
			 * @see android.os.Handler#handleMessage(android.os.Message)
			 */

			@Override
			public void handleMessage(Message msg) {

				if (msg.arg1 == 0) {

					// kernel

					if (msg.arg2 == 0) {
						showMessage(1, null);
					} else if (msg.arg2 == 1) {
						showMessage(3, null);

					} else if (msg.arg2 == 2) {

						// all ok

						Thread thread = new Thread(cards);
						thread.start();

						if (msg.what == 0) {
							showMessage(2, null);
						}

					} else if (msg.arg2 == 3) {

						// device ok

						MmcModell card = (MmcModell) msg.obj;
						String text = getString(R.string.msg_kernel_cache_p1)
								+ " " + card.getName() + " "
								+ getString(R.string.msg_kernel_cache_p2) + " "
								+ card.getAheadUser() + " "
								+ getString(R.string.msg_kernel_cache_p3);

						showMessage(7, text);

					} else if (msg.arg2 == 4) {

						// device failed

						MmcModell card = (MmcModell) msg.obj;
						String text = getString(R.string.msg_kernel_cache_p1)
								+ " " + card.getName() + " "
								+ getString(R.string.msg_kernel_cache_p4);

						showMessage(7, text);

					} else if (msg.arg2 == 5) {

						// device skipped

						MmcModell card = (MmcModell) msg.obj;
						String text = getString(R.string.msg_kernel_device_skip_p1)
								+ " "
								+ card.getName()
								+ " "
								+ getString(R.string.msg_kernel_device_skip_p1);

						Log.i(Utils.TAG, text + "." + " (reason = " + msg.what
								+ ")");
					}

				} else if (msg.arg1 == 1) {

					// MMC detection

					if (msg.arg2 == 0) {
						mmcDetection = true;

						// sd-booster.prop

						FileSetting prop = new FileSetting(context,
								getDbInstance());
						prop.setCardProperties(cards.getList());

						listToAdapter();
						adapterToView();

						if (uiNoCardList.getVisibility() == View.VISIBLE) {
							uiNoCardList.setVisibility(View.GONE);
							uiCardList.setVisibility(View.VISIBLE);
						}

						// EditText

						if (uiBoxCache.isChecked()) {

							Database dbAdapter = getDbInstance();
							int allSize = dbAdapter.getPref(5, 0); // User size

							if (allSize >= 128 && allSize <= 8192
									&& allSize != 128) {
								uiCacheSize.setText(String.valueOf(allSize));
							}
						} else {
							uiCacheSize.setText(null);
						}

						ServiceSetup setup = new ServiceSetup(context);
						setup.configuration(true);

						// notification

						Bundle bundle = getIntent().getExtras();
						String serial = null;

						if (bundle != null) {
							serial = bundle.getString(Utils.SERIAL);
						}

						if (serial != null) {
							showSetup(serial);
						}

					} else {
						mmcDetection = false;
						showMessage(1, null);

						if (uiNoCardList.getVisibility() == View.GONE) {
							uiNoCardList.setVisibility(View.VISIBLE);
							uiCardList.setVisibility(View.GONE);
						}
					}
				} else if (msg.arg1 == 2) {

					// App exit

					finish();
				} else if (msg.arg1 == 3) {

					if (msg.arg2 == 0) {

						// License accepted

						Thread thread = new Thread(cards);
						thread.start();
					} else {

						// help screen

						SDdialog dialog = new SDdialog(context, uiHandler, 4);
						dialog.useIcon();
						dialog.show();
					}
				} else if (msg.arg1 == 4) {

					// Preferences

					if (msg.arg2 == 0) {

						if (ringtone != null) {
							Database dbAdapter = getDbInstance();
							dbAdapter.setRingTone(ringtone);
						}
						showMessage(9, null);

						// logical check

						boolean monitor = uiBoxMonitor.isChecked();

						if (!monitor) {
							for (int i = 0; i < adapter.getCount(); i++) {
								MmcModell card = adapter.getItem(i);

								if (card.isOnMonitor()) {
									monitor = true;
									break;
								}
							}
						}

						if (monitor) {
							Database dbAdapter = getDbInstance();
							if (dbAdapter.getPref(1, 1) == 0) {
								showMessage(10, null);
							}
						}

						// background updates

						ServiceSetup setup = new ServiceSetup(context);
						setup.configuration(true);

					} else if (msg.arg2 == 1) {
						ringtoneChooser();
					}
				} else if (msg.arg1 == 5) {

					// card setup finished

					showMessage(9, null);

					MmcModell card = (MmcModell) msg.obj;

					if (card.isOnMonitor()) {
						Database dbAdapter = getDbInstance();
						if (dbAdapter.getPref(1, 1) == 0) {
							showMessage(10, null);
						}
					}
				}
			}
		};

		uiButtonApply.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {

				if (!mmcDetection) {
					showMessage(1, null);
					return;
				}

				if (uiBoxCache.isChecked()) {
					String size = uiCacheSize.getText().toString();

					if (size.length() < 3 || !Utils.containOnlyNumbers(size)
							|| !Utils.cacheSizeIsOk(size)) {

						showMessage(5, null);
						return;
					}

					int value = Integer.valueOf(size);

					setPref("size", value);
					cards.setToKernel(value);

				} else {

					boolean setup = false;

					for (int i = 0; i < adapter.getCount(); i++) {
						if (adapter.getItem(i).isSetup()) {
							setup = true;
							break;
						}
					}

					if (setup) {
						cards.setToKernel(0);
					} else {
						showMessage(6, null);
					}
				}
			}
		});

		uiButtonExit.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				finish();
			}
		});

		uiButtonHelp.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {

				Uri uri;
				String locale = getResources().getConfiguration().locale
						.getDisplayName();

				if (locale.contains("Deutsch") == true) {
					uri = Uri.parse(Utils.HELP_DE);
				} else {
					uri = Uri.parse(Utils.HELP_EN);
				}

				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(uri);

				try {
					startActivity(intent);
				} catch (ActivityNotFoundException e) {
					Log.e(Utils.TAG, "No browser installed");
				}
			}
		});

		uiButtonSetting.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {

				SDpreference dialog = new SDpreference(context, uiHandler);
				dialog.useIcon();
				dialog.show();
			}
		});

		uiButtonReport.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				sendReport();
			}
		});

		uiBoxCache.setChecked(dbAdapter.getPref(4, 0) == 1 ? true : false);
		uiBoxCache.setOnCheckedChangeListener(uiBoxListener);
		uiBoxBoot.setChecked(dbAdapter.getPref(1, 0) == 1 ? true : false);
		uiBoxBoot.setOnCheckedChangeListener(uiBoxListener);
		uiBoxMonitor.setChecked(dbAdapter.getPref(2, 0) == 1 ? true : false);
		uiBoxMonitor.setOnCheckedChangeListener(uiBoxListener);

		uiCacheSize
				.setOnEditorActionListener(new TextView.OnEditorActionListener() {

					@Override
					public boolean onEditorAction(TextView v, int actionId,
							KeyEvent event) {

						if (actionId == EditorInfo.IME_ACTION_DONE) {
							String input = v.getText().toString();

							if (input.length() < 3
									|| !Utils.containOnlyNumbers(input)
									|| !Utils.cacheSizeIsOk(input)) {

								showMessage(5, null);
								v.setText(null);
							}
						}
						return false;
					}
				});

		adapter = new ListAdapter(this, uiHandler);
		cards = new Mmc(uiHandler, 0);

		// License

		if (dbAdapter.getPref(3, 0) == 0) {
			SDdialog dialog = new SDdialog(context, uiHandler, 2);
			dialog.useIcon();
			dialog.show();
		} else {
			Thread thread = new Thread(cards);
			thread.start();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	public static Database getDbInstance() {

		if (dbAdapter == null) {
			dbAdapter = new Database(context);
		}

		return dbAdapter;
	}

	private void listToAdapter() {

		MmcModell dbCard;
		Database db = getDbInstance();
		adapter.clear();

		for (MmcModell card : cards.getList()) {

			if (!db.cardExist(card)) {
				card.setId(db.cardInsert(card));

				String text = card.getName() + " "
						+ getString(R.string.msg_new_card);

				showMessage(7, text);
			} else {
				String serial = card.getSerial();

				if (serial.equals(Utils.VIRTUAL)) {
					dbCard = db.getCard(card.getName(), false);
				} else {
					dbCard = db.getCard(card.getSerial(), true);
				}
				card.setOnBoot(dbCard.isOnBoot());
				card.setOnMonitor(dbCard.isOnMonitor());
				card.setId(dbCard.getId());
				card.setAheadUser(dbCard.getAheadUser());
			}

			adapter.add(card);
		}
	}

	private void adapterToView() {

		uiCardList.removeAllViews();

		for (int i = 0; i < adapter.getCount(); i++) {
			uiCardList.addView(adapter.getView(i, null, null));

			if (i + 1 < adapter.getCount()) {
				uiCardList.addView(adapter.getDivider());
			}
		}

		uiCardList.refreshDrawableState();
	}

	private void showSetup(String serial) {

		int position = -1;

		for (int i = 0; i < adapter.getCount(); i++) {
			if (adapter.getItem(i).getSerial().equals(serial)) {
				position = i;
				break;
			}
		}

		if (position != -1) {
			final MmcModell card = adapter.getItem(position);

			SDdialog dialog = new SDdialog(this, 1, card, uiHandler, position);
			dialog.useIcon();
			dialog.show();
		}
	}

	private void setPref(String tag, int value) {

		int index = -1;

		if (tag.equals("cbx_cache_all")) {
			index = 4;
		} else if (tag.equals("cbx_boot_all")) {
			index = 1;
		} else if (tag.equals("cbx_monitor_all")) {
			index = 2;
		} else if (tag.equals("license")) {
			index = 3;
		} else if (tag.equals("size")) {
			index = 5;
			value = Utils.alignValue(value);
		} else {
			throw new RuntimeException();
		}

		Database dbAdapter = getDbInstance();
		dbAdapter.setPref(index, value, 0);
	}

	private void showMessage(int index, String text) {

		String msg = null;

		switch (index) {

		case 1:
			msg = getString(R.string.msg_error_no_card);
			break;

		case 2:
			msg = getString(R.string.msg_all_ok);
			break;

		case 3:
			msg = getString(R.string.msg_error_no_root);
			break;

		case 4:
			msg = getString(R.string.msg_box_override_enable);
			break;

		case 5:
			msg = getString(R.string.dlg_setup_cache_size_error);
			break;

		case 6:
			msg = getString(R.string.msg_error_no_setup);
			break;

		case 7:
			msg = text;
			break;

		case 8:
			msg = getString(R.string.msg_box_override_disable);
			break;

		case 9:
			msg = getString(R.string.dlg_preference_store);
			break;

		case 10:
			msg = getString(R.string.msg_box_logical);
			break;

		default:
			Log.e(Utils.TAG, "showMessage() index = " + index);
			throw new RuntimeException();
		}

		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

	private void ringtoneChooser() {

		Intent intent = new Intent();

		intent.setAction(RingtoneManager.ACTION_RINGTONE_PICKER);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
				RingtoneManager.TYPE_NOTIFICATION);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);

		try {
			startActivityForResult(intent, 1);
		} catch (Exception e) {
			Log.w(Utils.TAG, "ringtoneChooser(): " + e.toString());
		}
	}

	private void sendReport() {

		Intent intent = new Intent(Intent.ACTION_SENDTO);
		Uri uri = Uri.parse("mailto:daniel.mehrmann@gmx.de?subject=Bugreport");
		intent.setData(uri);

		StringBuilder text = new StringBuilder();

		text.append("Manufacturer = " + Build.MANUFACTURER + "\n");
		text.append("Modell = " + Build.MODEL + "\n");
		text.append("Android version = " + Build.VERSION.RELEASE + "\n");

		String logcat = Kernel.getLogcat();
		if (logcat != null) {
			text.append("Logcat:\n");
			text.append(logcat);
		}

		String dir = Kernel.showSystem();
		if (dir != null) {
			text.append("System:\n");
			text.append(dir);
		}

		intent.putExtra(Intent.EXTRA_TEXT, text.toString());

		try {
			startActivity(Intent.createChooser(intent, "Send bugreport"));
		} catch (ActivityNotFoundException e) {
			Log.w(Utils.TAG, "No activity found: " + e.toString());
		}
	}
}
