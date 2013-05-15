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
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class ServiceStart extends Service {

	private static Context context;
	private static Handler handler;
	private static Database db;

	private boolean onBoot;
	private boolean onMonitor;
	private boolean onChange;

	private boolean allBoot;
	private boolean allMonitor;
	private boolean allCache;
	private boolean license;
	private int allSize;

	private boolean useUpdates;
	private boolean useBootNote;
	private boolean useMonitorNote;
	private boolean useChangeNote;
	private boolean useAlarmNote;

	private boolean propExists;

	private Mmc cards;

	@SuppressLint("HandlerLeak")
	@Override
	public void onCreate() {

		super.onCreate();

		context = this;
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {

				if (msg.arg1 == 0) {

					// kernel

					if (msg.arg2 <= 2) {
						stopSelf();
					} else if (msg.arg2 == 3) {

						// device ok

						MmcModell card = (MmcModell) msg.obj;
						String text = getString(R.string.msg_kernel_cache_p1)
								+ " " + card.getName() + " "
								+ getString(R.string.msg_kernel_cache_p2) + " "
								+ card.getAheadUser() + " "
								+ getString(R.string.msg_kernel_cache_p3);

						Log.i(Utils.TAG, text);

						if (onBoot) {
							if (useBootNote) {
								userNotification(card, 1);
							}
						} else if (onMonitor) {
							if (useMonitorNote) {
								userNotification(card, 1);
							}
						} else if (onChange) {
							if (useChangeNote) {
								userNotification(card, 1);
							}
						} else {
							throw new RuntimeException();
						}

					} else if (msg.arg2 == 4) {

						// device failed

						MmcModell card = (MmcModell) msg.obj;
						String text = getString(R.string.msg_kernel_cache_p1)
								+ " " + card.getName() + " "
								+ getString(R.string.msg_kernel_cache_p4);

						Log.i(Utils.TAG, text);

					} else if (msg.arg2 == 5) {

						// device skipped

						MmcModell card = (MmcModell) msg.obj;
						String text = getString(R.string.msg_kernel_device_skip_p1)
								+ " "
								+ card.getName()
								+ " "
								+ getString(R.string.msg_kernel_device_skip_p2);

						int what = msg.what;

						Log.i(Utils.TAG, text + "." + " (reason = " + what
								+ ")");

						if (what == 1) {
							if (onBoot) {
								if (useBootNote) {
									userNotification(card, 2);
								}
							} else if (onChange) {
								if (useChangeNote) {
									userNotification(card, 2);
								}
							} else if (onMonitor) {
								if (useMonitorNote) {
									userNotification(card, 2);
								}
							} else {
								throw new RuntimeException();
							}
						}
					}

				} else if (msg.arg1 == 1) {

					// MMC detection

					if (msg.arg2 == 0) {

						int what = msg.what;
						updateDbToModell();

						FileSetting prop = new FileSetting(context,
								getDbInstance());
						prop.setCardProperties(cards.getList());

						if (what == 1) {
							setOnBoot();
						} else if (what == 2) {
							setOnMonitor();
						} else if (what == 3) {
							setOnChange();
						} else {
							throw new RuntimeException();
						}

					} else {
						Log.e(Utils.TAG, getString(R.string.msg_error_no_card));
						stopSelf();
					}
				}
			}
		};

		Database dbAdapter = getDbInstance();

		allBoot = dbAdapter.getPref(1, 0) == 1 ? true : false;
		allMonitor = dbAdapter.getPref(2, 0) == 1 ? true : false;
		allCache = dbAdapter.getPref(4, 0) == 1 ? true : false;
		license = dbAdapter.getPref(3, 0) == 1 ? true : false;
		allSize = dbAdapter.getPref(5, 0);

		useUpdates = dbAdapter.getPref(1, 1) == 1 ? true : false;
		useAlarmNote = dbAdapter.getPref(2, 1) == 1 ? true : false;
		useBootNote = dbAdapter.getPref(3, 1) == 1 ? true : false;
		useMonitorNote = dbAdapter.getPref(4, 1) == 1 ? true : false;
		useChangeNote = dbAdapter.getPref(5, 1) == 1 ? true : false;

		if (!useUpdates && onMonitor) {
			throw new RuntimeException();
		}

		propExists = FileSetting.exists(context);

		if (!license && !propExists) {
			Log.i(Utils.TAG, "No license");
			stopSelf();
		} else {
			Log.i(Utils.TAG, getString(R.string.msg_service_start));
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(Utils.TAG, getString(R.string.msg_service_stop));
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if (intent == null) { // Case of low memory: Restart with no intent
			Log.e(Utils.TAG, "ServiceStart(): Intent is null");
			onBoot = false;
			onMonitor = true; // HACK
			onChange = false;
		} else {
			Bundle bundle = intent.getExtras();

			onBoot = bundle.getBoolean(Utils.BOOT);
			onMonitor = bundle.getBoolean(Utils.MONITOR);
			onChange = bundle.getBoolean(Utils.CHANGE);
		}

		int action;

		if (onBoot) {
			action = 1;
		} else if (onMonitor) {
			action = 2;
		} else if (onChange) {
			action = 3;
		} else {
			throw new RuntimeException();
		}

		this.cards = new Mmc(handler, action);

		Thread thread = new Thread(this.cards);
		thread.start();

		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private static Database getDbInstance() {

		if (db == null) {
			db = new Database(context);
		}

		return db;
	}

	private void updateDbToModell() {

		MmcModell dbCard;
		Database dbAdapter = getDbInstance();

		for (MmcModell card : this.cards.getList()) {

			if (!dbAdapter.cardExist(card)) {
				card.setId(dbAdapter.cardInsert(card));

				if (!allCache) {
					if (onBoot) {
						if (useBootNote) {
							userNotification(card, 0);
						}
					} else if (onChange) {
						if (useChangeNote) {
							userNotification(card, 0);
						}
					} else if (onMonitor) {
						if (useMonitorNote) {
							userNotification(card, 0);
						}
					} else {
						throw new RuntimeException();
					}
				}

				if (propExists) {
					userNotification(card, 3);
				}
			} else {
				String serial = card.getSerial();

				if (serial.equals(Utils.VIRTUAL)) {
					dbCard = dbAdapter.getCard(card.getName(), false);
				} else {
					dbCard = dbAdapter.getCard(card.getSerial(), true);
				}

				card.setOnBoot(dbCard.isOnBoot());
				card.setOnMonitor(dbCard.isOnMonitor());
				card.setId(dbCard.getId());
				card.setAheadUser(dbCard.getAheadUser());
				card.setSetup(true);
			}
		}
	}

	private void setOnBoot() {

		if (useUpdates) {
			ServiceSetup setup = new ServiceSetup(context);
			setup.configuration(true);
		}

		boolean job = allBoot;

		if (!job) {
			for (MmcModell card : this.cards.getList()) {
				if (card.isOnBoot()) {
					job = true;
					break;
				}
			}
		}

		if (job) {
			this.cards.setToKernel(allCache == true ? allSize : 0, allBoot);
		} else {
			stopSelf();
		}
	}

	private void setOnMonitor() {

		boolean job = allMonitor;

		if (!job) {
			for (MmcModell card : this.cards.getList()) {
				if (card.isOnMonitor()) {
					job = true;
					break;
				}
			}
		}

		if (job) {
			this.cards.monitorToKernel(allCache == true ? allSize : 0,
					allMonitor);
		} else {
			stopSelf();
		}
	}

	private void setOnChange() {

		boolean job = allCache;

		if (!job) {
			job = allMonitor;
		}

		if (!job) {
			for (MmcModell card : this.cards.getList()) {
				if (card.isOnBoot()) {
					job = true;
					break;
				}
				if (card.isOnMonitor()) {
					job = true;
					break;
				}
			}
		}

		if (job) {
			this.cards.changeToKernel(allCache == true ? allSize : 0, allCache,
					allBoot);
		} else {
			stopSelf();
		}
	}

	private void userNotification(final MmcModell card, int action) {

		String text;

		if (action == 0) {

			// new device

			text = getString(R.string.msg_service_ticker_new);

		} else if (action == 1) {

			// monitor

			text = getString(R.string.msg_service_ticker_monitor_p1) + " "
					+ card.getName() + " "
					+ getString(R.string.msg_service_ticker_monitor_p2) + " "
					+ card.getAheadUser();
		} else if (action == 2) {

			// unmanaged device

			text = getString(R.string.msg_service_ticker_value_p1) + " "
					+ card.getName() + " "
					+ getString(R.string.msg_service_ticker_value_p2);

		} else if (action == 3) {

			// License through properties

			text = getString(R.string.dlg_license_label);

		} else {
			throw new RuntimeException();
		}

		NotificationCompat.Builder builder = new NotificationCompat.Builder(
				context);
		builder.setAutoCancel(true);
		builder.setOnlyAlertOnce(true);
		builder.setContentTitle(Utils.TAG);
		builder.setContentText(text);
		builder.setWhen(System.currentTimeMillis());
		builder.setSmallIcon(R.drawable.ic_launcher);

		Intent intent = new Intent();

		if (action == 2) {

			// unmanaged device

			intent.setClass(context, SDbooster.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.putExtra(Utils.SERIAL, card.getSerial());

		} else if (action == 3) {

			// License

			intent.setClass(context, SDbooster.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		}

		PendingIntent pending = PendingIntent.getActivity(context, 0, intent,
				PendingIntent.FLAG_CANCEL_CURRENT);

		builder.setContentIntent(pending);

		if (useAlarmNote) {
			Database dbAdapter = getDbInstance();
			builder.setSound(Uri.parse(dbAdapter.getRingTone()));
		}

		Notification note = builder.build();

		NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		int id;

		try {
			id = Integer.valueOf((int) card.getId());

		} catch (Exception e) {

			if (onBoot) {
				id = 10;
			} else if (onMonitor) {
				id = 11;
			} else {
				id = 12;
			}
		}

		// license

		if (action == 3) {
			id += 3;
		}

		manager.notify(id, note);
	}
}
