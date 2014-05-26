package com.peculator.vornamecomkosenamen;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class MainActivity extends Activity {

	// TODOs
	// Telefonbuch durchsuchen //
	// Deutsch - English
	// Wartesymbol //
	// Standard Begrüßung //
	// HTML-Code ersetzen (wird im moment entfernt)/

	private static String name = "";
	private EditText search;
	private final LinkedList<String> resultList = new LinkedList<String>();

	private AutoCompleteTextView textView;
	private GridView gridView;
	private MyAdapter adapter;
	private boolean vertical = true;

	enum State {
		INFO, RESULTS, NAMEERROR, CONERROR, SEARCHING,
	}

	protected State currentState;

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		// Checks the orientation of the screen
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			vertical = false;
			refreshContent();
		} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
			vertical = true;
			refreshContent();
		}

	}

	public boolean refreshContent() {
		TextView mView = (TextView) findViewById(R.id.textview);

		if (currentState == State.RESULTS) {
			mView.setText(R.string.empty);

			gridView = (GridView) findViewById(R.id.grid);

			if (vertical) {
				gridView.setNumColumns(2);
			} else {
				gridView.setNumColumns(3);
			}

			adapter.names = resultList;
			adapter.notifyDataSetChanged();

		} else {
			resultList.clear();
			adapter.names = resultList;
			adapter.notifyDataSetChanged();

			if (currentState == State.NAMEERROR) {
				mView.setText(R.string.state_name_err);

			} else if (currentState == State.CONERROR) {
				mView.setText(R.string.state_con_err);

			} else if (currentState == State.INFO) {
				mView.setText(R.string.state_info);
			} else if (currentState == State.SEARCHING) {
				mView.setText(R.string.state_searching);
			}
		}

		return true;
	}

	private String[] listToArray(LinkedList<String> input) {
		String[] result = new String[input.size()];

		for (int i = 0; i < result.length; i++) {
			result[i] = input.get(i);
		}

		return result;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// super.onCreate(savedInstanceState);
		// clearSettings();

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);

		ActionBar actionBar = getActionBar();
		// add the custom view to the action bar
		actionBar.setCustomView(R.layout.actionbar_view);
		setContentView(R.layout.activity_main);

		gridView = (GridView) findViewById(R.id.grid);
		adapter = new MyAdapter(this.getApplicationContext());
		gridView.setAdapter(adapter);

		View customView = actionBar.getCustomView().findViewById(
				R.id.searchfield);

		search = (EditText) customView;

		// Set default state
		currentState = State.INFO;

		// Get all Contacts
		Cursor phones = getContentResolver().query(
				ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null,
				null, null);
		LinkedList<String> myContacts = new LinkedList<String>();
		while (phones.moveToNext()) {
			myContacts
					.add(phones.getString(phones
							.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)));
		}
		phones.close();

		String[] names = listToArray(myContacts);
		ArrayAdapter<String> textAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, names);
		textView = (AutoCompleteTextView) customView;
		textView.setThreshold(0);

		textView.setAdapter(textAdapter);

		search.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					MainActivity.name = v.getText().toString();
				} else if (event != null
						&& (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
					InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

					in.hideSoftInputFromWindow(
							search.getApplicationWindowToken(),
							InputMethodManager.HIDE_NOT_ALWAYS);
					// Must return true here to consume event
					return true;
				}
				return false;
			}
		});
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		refreshContent();
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
		case R.id.action_search:
			sendRequest();
			return true;
		case R.id.action_visit:
			visitWebsite();
			return true;
		case R.id.action_visit2:
			visitWebsite();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void sendRequest() {
		MainActivity.name = search.getText().toString();
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

		try {
			InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

			inputManager.hideSoftInputFromWindow(
					getCurrentFocus().getWindowToken(),
					InputMethodManager.HIDE_NOT_ALWAYS);
	
		} catch (NullPointerException e) {
		}
		
		if (networkInfo != null && networkInfo.isConnected()) {
			currentState = State.SEARCHING;
			refreshContent();
			if (MainActivity.name != "") {
				String html_str = TextUtils.htmlEncode(MainActivity.name);
				new DownloadWebpageTask()
						.execute("http://www.vorname.com/name," + html_str
								+ ".html");
			}
			else{
				currentState = State.NAMEERROR;
				refreshContent();
			}
		} else {
			Log.e("my", "no WLAN");
			startWifi();
		}
	}

	private void visitWebsite() {
		if (MainActivity.name == "") {
			goToUrl("http://vorname.com");
		} else {
			String html_str = TextUtils.htmlEncode(MainActivity.name);
			goToUrl("http://www.vorname.com/name," + html_str + ".html");
		}
	}

	private void goToUrl(String url) {
		Uri uriUrl = Uri.parse(url);
		Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
		launchBrowser.addCategory(Intent.CATEGORY_BROWSABLE);
		startActivity(launchBrowser);
	}

	public void startWifi() {
		Intent intent = new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK);
		intent.putExtra("extra_prefs_show_button_bar", true);
		intent.putExtra("wifi_enable_next_on_connect", true);
		startActivity(intent);
	}

	private class DownloadWebpageTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... urls) {

			// params comes from the execute() call: params[0] is the url.
			try {
				return downloadUrl(urls[0]);
			} catch (IOException e) {
				cancel(true);
				return null;
			}
		}

		private String downloadUrl(String myurl) throws IOException {
			InputStream is = null;
			// Only display the first 5000 characters of the retrieved
			// web page content.
			int len = 50000;
			System.gc();

			try {
				URL url = new URL(myurl);
				HttpURLConnection conn = (HttpURLConnection) url
						.openConnection();
				conn.setReadTimeout(4000 /* milliseconds */);
				conn.setConnectTimeout(6000 /* milliseconds */);
				conn.setRequestMethod("GET");
				conn.setDoInput(true);
				// Starts the query
				conn.connect();
				int response = conn.getResponseCode();
				Log.d("my", "The response is: " + response);

				if (response == 200) {
					is = conn.getInputStream();

					// Convert the InputStream into a string
					String contentAsString = readIt(is, len);
					return contentAsString;
				} else if (response == 404) {
					currentState = State.NAMEERROR;
				} else {
					currentState = State.CONERROR;
					cancel(true);
				}

				// Makes sure that the InputStream is closed after the app is
				// finished using it.
			} catch (Exception e) {
				currentState = State.NAMEERROR;
			} finally {
				if (is != null) {
					is.close();
				}
			}

			return null;
		}

		// Reads an InputStream and converts it to a String.
		public String readIt(InputStream stream, int len) throws IOException,
				UnsupportedEncodingException {
			Reader reader = null;
			reader = new InputStreamReader(stream, "UTF-8");
			char[] buffer = new char[len];
			reader.read(buffer);
			return new String(buffer);
		}

		// onPostExecute displays the results of the AsyncTask.
		@Override
		protected void onPostExecute(String result) {
			if (result == null) {
				currentState = State.NAMEERROR;
				refreshContent();
				cancel(true);
				return;
			}

			String names = "";
			if (result != null)
				try {
					if (result.contains("Spitzname")) {
						String parsenames = result.substring(
								result.lastIndexOf("Spitzname"),
								result.length());

						if (parsenames.indexOf("<p>") > -1
								&& parsenames.indexOf("</p>") > -1) {
							names = parsenames.substring(
									parsenames.indexOf("<p>") + 3,
									parsenames.indexOf("</p>"));
						}
					} else {
						names = null;
						currentState = State.NAMEERROR;
						refreshContent();
						cancel(true);
						return;
					}

					resultList.clear();
					refreshContent();
					while (true) {
						try {

							if (names.contains(",")) {
								currentState = State.RESULTS;
								String tmp = names.substring(0,
										names.indexOf(","));
								names = new String(names.substring(
										names.indexOf(",") + 1, names.length()));
								// trim the string and replace all special
								// html
								// characters with
								resultList
										.add(tmp.trim().replaceAll("\\W", ""));

							} else
								break;
						} catch (StringIndexOutOfBoundsException e) {
							currentState = State.NAMEERROR;
							refreshContent();
							break;
						}

					}
					refreshContent();

				} catch (Exception e) {
					currentState = State.NAMEERROR;
					refreshContent();
					cancel(true);
				}
		}
	}
}
