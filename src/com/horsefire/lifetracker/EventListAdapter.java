package com.horsefire.lifetracker;

import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.couchbase.lite.Document;
import com.couchbase.lite.QueryRow;

public class EventListAdapter extends ArrayAdapter<QueryRow> {

	private List<QueryRow> list;
	private final Context context;

	public EventListAdapter(Context context, int resource,
			int textViewResourceId, List<QueryRow> objects) {
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
		QueryRow row = list.get(position);
		Document document = row.getDocument();
		try {
			Object property = document.getCurrentRevision().getProperty("when");
			label.setText(property.toString());
		} catch (Exception e) {
			label.setText("Error");
			Log.e(HomeActivity.LOG_TAG, "Error Displaying document", e);
		}

		return itemView;
	}
}