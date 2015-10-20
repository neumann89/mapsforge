/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.applications.android.samples.views;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.mapsforge.applications.android.samples.NearbyPoiMapViewer;
import org.mapsforge.applications.android.samples.R;
import org.mapsforge.poi.searching.ExtendedPoi;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class PoiListAdapter extends ArrayAdapter<ExtendedPoi> {

	private List<ExtendedPoi> poiList;
	private Set<ExtendedPoi> selectedPoi = new TreeSet<ExtendedPoi>();
	private NearbyPoiMapViewer activity;

	public PoiListAdapter(Context context, int textViewResourceId, List<ExtendedPoi> poiList) {
		super(context, textViewResourceId, poiList);
		this.poiList = poiList;
		this.activity = (NearbyPoiMapViewer) context;
	}

	public Collection<ExtendedPoi> getSelectedPoi() {
		return selectedPoi;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.rowlayout, parent, false);

		TextView textView = (TextView) rowView.findViewById(R.id.label);
		textView.setText(poiList.get(position).toString());

		CheckBox checkBox = (CheckBox) rowView.findViewById(R.id.check);
		checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// TODO permanent checked state (if checkbox is not visible it will lose state)
				if (isChecked) {
					selectedPoi.add(poiList.get(position));
					Log.d("SelectedPoi: ", selectedPoi.size() + "");

					buttonView.setChecked(true);
				} else {
					selectedPoi.remove(poiList.get(position));

					buttonView.setChecked(false);
				}
			}
		});

		return rowView;
	}

}