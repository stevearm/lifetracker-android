package com.horsefire.lifetracker;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.couchbase.lite.Database;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.support.CouchbaseLiteApplication;

public class HomeActivity extends Activity implements
		Replication.ChangeListener, OnClickListener {

	public static final String LOG_TAG = HomeActivity.class.getSimpleName();

	// constants
	public static final String DATABASE_NAME = "lifetracker";
	public static final String designDocName = "ui";
	public static final String byDateViewName = "events";

	// public static final String SYNC_URL =
	// "http://192.168.1.8:5984/lifetracker-dev";
	public static final String SYNC_URL = "http://192.168.1.8:5984/phone-test";

	// splash screen
	protected SplashScreenDialog splashDialog;

	// main screen
	protected ListView itemListView;
	protected EventListAdapter itemListViewAdapter;

	// couch internals
	protected static Manager manager;
	private Database database;
	private LiveQuery liveQuery;

	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_home);

		// connect items from layout
		((Button) findViewById(R.id.syncButton)).setOnClickListener(this);
		itemListView = (ListView) findViewById(R.id.itemListView);

		// show splash and start couch
		showSplashScreen();
		removeSplashScreen();

		try {
			startCBLite();
		} catch (Exception e) {
			Toast.makeText(getApplicationContext(),
					"Error Initializing CBLIte, see logs for details",
					Toast.LENGTH_LONG).show();
			Log.e(LOG_TAG, "Error initializing CBLite", e);
		}

	}

	protected void onDestroy() {
		if (manager != null) {
			manager.close();
		}
		super.onDestroy();
	}

	protected void startCBLite() throws Exception {

		manager = new Manager(getApplicationContext().getFilesDir(),
				Manager.DEFAULT_OPTIONS);

		// install a view definition needed by the application
		database = manager.getDatabase(DATABASE_NAME);
		com.couchbase.lite.View viewItemsByDate = database
				.getView("phone/events");
		viewItemsByDate.setMap(new Mapper() {
			@Override
			public void map(Map<String, Object> document, Emitter emitter) {
				Object createdAt = document.get("when");
				if (createdAt != null) {
					emitter.emit(document.get("_id"), null);
				}
			}
		}, "1.0");

		CouchbaseLiteApplication application = (CouchbaseLiteApplication) getApplication();
		application.setManager(manager);

		startLiveQuery(viewItemsByDate);

		startSync();

	}

	private void startSync() {

		URL syncUrl;
		try {
			syncUrl = new URL(SYNC_URL);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}

		Replication pullReplication = database.createPullReplication(syncUrl);
		pullReplication.addChangeListener(this);

		Replication pushReplication = database.createPushReplication(syncUrl);
		pushReplication.addChangeListener(this);

		pullReplication.start();
		pushReplication.start();
	}

	private void startLiveQuery(com.couchbase.lite.View view) throws Exception {

		final ProgressDialog progressDialog = showLoadingSpinner();

		if (liveQuery == null) {

			liveQuery = view.createQuery().toLiveQuery();

			liveQuery.addChangeListener(new LiveQuery.ChangeListener() {
				@Override
				public void changed(LiveQuery.ChangeEvent event) {
					displayRows(event.getRows());
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							progressDialog.dismiss();
						}
					});
				}
			});

			liveQuery.start();

		}

	}

	private void displayRows(QueryEnumerator queryEnumerator) {

		final List<QueryRow> rows = getRowsFromQueryEnumerator(queryEnumerator);
		Log.i(LOG_TAG, "Displaying " + rows.size() + " rows");

		runOnUiThread(new Runnable() {
			@Override
			public void run() {

				itemListViewAdapter = new EventListAdapter(
						getApplicationContext(), R.layout.event_list_item,
						R.id.label, rows);
				itemListView.setAdapter(itemListViewAdapter);

			}
		});
	}

	private List<QueryRow> getRowsFromQueryEnumerator(
			QueryEnumerator queryEnumerator) {
		List<QueryRow> rows = new ArrayList<QueryRow>();
		for (Iterator<QueryRow> it = queryEnumerator; it.hasNext();) {
			QueryRow row = it.next();
			rows.add(row);
		}
		return rows;
	}

	private ProgressDialog showLoadingSpinner() {
		ProgressDialog progress = new ProgressDialog(this);
		progress.setTitle("Loading");
		progress.setMessage("Wait while loading...");
		progress.show();
		return progress;
	}

	/**
	 * Handle typing item text
	 */
	public void onClick(View v) {
		Toast.makeText(getApplicationContext(), "Syncing", Toast.LENGTH_LONG)
				.show();
		startSync();
	}

	/**
	 * Shows the splash screen over the full Activity
	 */
	private void showSplashScreen() {
		splashDialog = new SplashScreenDialog(this);
		splashDialog.show();
	}

	/**
	 * Removes the Dialog that displays the splash screen
	 */
	protected void removeSplashScreen() {
		if (splashDialog != null) {
			splashDialog.dismiss();
			splashDialog = null;
		}
	}

	/**
	 * Add settings item to the menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, 0, 0, "Settings");
		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * Launch the settings activity
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			startActivity(new Intent(this, LifeTrackerPreferencesActivity.class));
			return true;
		}
		return false;
	}

	@Override
	public void changed(Replication.ChangeEvent event) {

		Replication replication = event.getSource();
		Log.d(LOG_TAG, "Replication : " + replication + " changed.");
		if (!replication.isRunning()) {
			Log.d(LOG_TAG,
					String.format("Replicator %s not running", replication));
		} else {
			int processed = replication.getCompletedChangesCount();
			int total = replication.getChangesCount();
			Log.d(LOG_TAG, String.format("Replicator processed %d / %d",
					processed, total));
		}

	}
}
