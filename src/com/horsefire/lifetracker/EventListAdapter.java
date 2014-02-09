package com.horsefire.lifetracker;

import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class EventListAdapter extends ArrayAdapter<LifeTrackerEvent> {

	private List<LifeTrackerEvent> list;
	private final Context context;

	public EventListAdapter(Context context, int resource,
			int textViewResourceId, List<LifeTrackerEvent> objects) {
		super(context, resource, textViewResourceId, objects);
		this.context = context;
		this.list = objects;
	}

	private static class ViewHolder {
		TextView label;
	}

	@Override
	public View getView(int position, View itemView, ViewGroup parent) {
		if (itemView == null) {
			LayoutInflater vi = (LayoutInflater) parent.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			itemView = vi.inflate(R.layout.event_list_item, null);
			ViewHolder vh = new ViewHolder();
			vh.label = (TextView) itemView.findViewById(R.id.label);
			itemView.setTag(vh);
		}

		TextView label = ((ViewHolder) itemView.getTag()).label;
		try {
			LifeTrackerEvent event = list.get(position);
			label.setText(event.getWhen());
		} catch (Exception e) {
			label.setText("Error");
			Log.e(HomeActivity.LOG_TAG, "Error Displaying document", e);
		}

		return itemView;
	}
}