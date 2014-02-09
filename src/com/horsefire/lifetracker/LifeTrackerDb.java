package com.horsefire.lifetracker;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.SavedRevision;
import com.couchbase.lite.replicator.Replication;

public class LifeTrackerDb {

	private static final String LOG_TAG = LifeTrackerDb.class.getSimpleName();
	private static final String EVENT_VIEW = "events";

	private final Database m_database;

	public LifeTrackerDb(Manager manager) throws IOException,
			CouchbaseLiteException {
		m_database = manager.getDatabase("lifetracker");
		registerViews();
	}

	/**
	 * Any time these views change, bump the verson number, or they won't be
	 * regenerated
	 */
	private void registerViews() {
		m_database.getView(EVENT_VIEW).setMap(new Mapper() {
			@Override
			public void map(Map<String, Object> document, Emitter emitter) {
				Object createdAt = document.get("type");
				if (createdAt != null && createdAt.toString().equals("event")) {
					emitter.emit(document.get("_id"), null);
				}
			}
		}, "1.0");
	}

	public Query getEventsQuery() {
		return m_database.getView(EVENT_VIEW).createQuery();
	}

	public static List<LifeTrackerEvent> extractEvents(
			QueryEnumerator queryEnumerator) {
		Log.i(LOG_TAG, "Extracting events: " + queryEnumerator.getCount());
		List<LifeTrackerEvent> rows = new ArrayList<LifeTrackerEvent>();
		for (Iterator<QueryRow> it = queryEnumerator; it.hasNext();) {
			SavedRevision doc = it.next().getDocument().getCurrentRevision();
			rows.add(new LifeTrackerEvent(doc));
		}
		Log.i(LOG_TAG, "Extracted events: " + rows.size());
		return rows;
	}

	public void sync(String remote, Replication.ChangeListener listener) {
		URL syncUrl;
		try {
			syncUrl = new URL(remote);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}

		Replication pullReplication = m_database.createPullReplication(syncUrl);
		Replication pushReplication = m_database.createPushReplication(syncUrl);

		if (listener != null) {
			pullReplication.addChangeListener(listener);
			pushReplication.addChangeListener(listener);
		}

		pullReplication.start();
		pushReplication.start();
	}
}
