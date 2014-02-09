package com.horsefire.lifetracker;

import com.couchbase.lite.SavedRevision;

public class LifeTrackerEvent {

	private final String m_when;

	public LifeTrackerEvent(SavedRevision doc) {
		m_when = doc.getProperty("when").toString();
	}

	public String getWhen() {
		return m_when;
	}
}
