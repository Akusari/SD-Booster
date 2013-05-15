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

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ListAdapter extends ArrayAdapter<MmcModell> {

	private final Context context;
	private final Handler handler;

	private static class RowEntry {
		public TextView partSize;
		public TextView cacheSize;
		public ImageView info;
		public ImageView setup;
	}

	public ListAdapter(Context ctx, Handler uiHandler) {

		super(ctx, R.layout.list_row);
		context = ctx;
		handler = uiHandler;
	}

	// TODO remove me!
	@Override 
	public boolean isEnabled(int position) {
		return false;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {

		RowEntry entry = null;
		View rowView = convertView;

		if (rowView == null) {

			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			rowView = inflater.inflate(R.layout.list_row, null);

			entry = new RowEntry();
			entry.cacheSize = (TextView) rowView
					.findViewById(R.id.card_cache_size);
			entry.partSize = (TextView) rowView
					.findViewById(R.id.card_part_size);
			entry.info = (ImageView) rowView.findViewById(R.id.card_info);
			entry.setup = (ImageView) rowView.findViewById(R.id.card_setup);

			rowView.setTag(entry);

		} else {

			entry = (RowEntry) rowView.getTag();
		}

		final MmcModell card = this.getItem(position);
		final int cardCounter = this.getCount();

		int cacheSize = Integer.parseInt(card.getAheadValue());
		String partSize = Utils.showCardSize(card.getSize(), true);

		colorizeCache(entry.cacheSize, cacheSize, cardCounter);

		if (card.getCid().equals(Utils.VIRTUAL)) {
			entry.partSize.setText(context.getString(R.string.card_size_label));
			entry.partSize.setTextColor(Color.parseColor("#0080FF"));
		} else {
			entry.partSize.setText(partSize + " GB");
			entry.partSize.setTextColor(Color.parseColor("#F2F2F2"));
		}

		entry.cacheSize.setText(String.valueOf(cacheSize));
		entry.info.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				SDdialog dialog = new SDdialog(context, 0, card, handler, position);
				dialog.useIcon();
				dialog.show();
			}
		});

		entry.setup.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				SDdialog dialog = new SDdialog(context, 1, card, handler, position);
				dialog.useIcon();
				dialog.show();
			}
		});

		return rowView;
	}

	public View getDivider() {

		LayoutInflater inflater = ((Activity) context).getLayoutInflater();
		View divider = inflater.inflate(R.layout.list_divider, null);

		return divider;
	}

	private void colorizeCache(final TextView tv, int value, int cardCounter) {

		final int basicSize[] = { 128, 512, 2048 };
		final int extSize[] = { 128, 256, 512 };

		if (cardCounter == 1) {

			if (value <= basicSize[0]) {
				tv.setTextColor(Color.RED);
			} else if (value <= basicSize[1]) {
				tv.setTextColor(Color.YELLOW);
			} else if (value <= basicSize[2]) {
				tv.setTextColor(Color.GREEN);
			} else {
				tv.setTextColor(Color.MAGENTA);
			}
		} else {

			if (value <= extSize[0]) {
				tv.setTextColor(Color.RED);
			} else if (value <= extSize[1]) {
				tv.setTextColor(Color.YELLOW);
			} else if (value <= extSize[2]) {
				tv.setTextColor(Color.GREEN);
			} else {
				tv.setTextColor(Color.MAGENTA);
			}
		}
	}
}
