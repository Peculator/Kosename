package com.peculator.vornamecomkosenamen;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

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
import android.content.res.Configuration;
import android.database.Cursor;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class MainActivity extends Activity {

	// TODOs
	// Offline-Mode bzw. Suchergebnisse offline speichern (auf jeden Fall die
	// Favoriten)
	// Telefonbuch durchsuchen //
	// Seite besuchen - Name muss noch überprüft werden
	// Wartesymbol
	// Liste/Grid (zu costum grid)//
	// Horizontaler + Vertikaler Modus //
	// Fehlerbenachrichtigung: http-response
	// Alphabetisch Sortieren //
	// Favoriten - Stern //
	// Leerzeichen am Anfang entfernen //
	// Standard Begrüßung
	// HTML-Code ersetzen (wird im moment entfernt)/
	// Case-sensitivity bei der vorname.com suche

	private static final CursorAdapter Textadapter = null;
	
	public static String name = "";
	EditText search;
	public final LinkedList<String> resultList = new LinkedList<String>();
	public final LinkedList<Boolean> resultSelected = new LinkedList<Boolean>();
	public AutoCompleteTextView textView;
	private GridView gridView;
	MyAdapter adapter;
	private boolean vertical = true;

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

		if (resultList.size() != 0) {
			gridView = (GridView) findViewById(R.id.grid);

			if (vertical) {
				gridView.setNumColumns(2);
			} else {
				gridView.setNumColumns(3);
			}

			Collections.sort(resultList, new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					return o1.compareToIgnoreCase(o2);
				}
			});

			adapter.names = resultList;
			adapter.selected = resultSelected;
			adapter.notifyDataSetChanged();

			// ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
			// android.R.layout.simple_list_item_1,
			// listToArray(resultList));
			//
			// gridView.setAdapter(adapter);
		} else {
			Toast.makeText(getApplicationContext(), "no name found",
					Toast.LENGTH_SHORT).show();
		}

		// TextView mView = (TextView) findViewById(R.id.textview);
		// String result = "";
		// for (int i = 0; i < resultList.size(); i++) {
		// result += (resultList.get(i)).replaceAll("\\s", "") + "\n";
		// }
		//
		// mView.setText(result);
		// mView.setMovementMethod(new ScrollingMovementMethod());

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

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);

		ActionBar actionBar = getActionBar();
		// add the custom view to the action bar
		actionBar.setCustomView(R.layout.actionbar_view);
		setContentView(R.layout.activity_main);

		gridView = (GridView) findViewById(R.id.grid);
		adapter = new MyAdapter(this.getApplicationContext());
		gridView.setAdapter(adapter);

		gridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				String message = (adapter.selected.get(position)) ? (String) getResources()
						.getString(R.string.remove_fav)
						: (String) getResources()
								.getString(R.string.adding_fav);

				Toast.makeText(
						getApplicationContext(),
						((TextView) v.findViewById(R.id.text)).getText()
								+ message, Toast.LENGTH_SHORT).show();
				adapter.selected.set(position, !adapter.selected.get(position));
				adapter.notifyDataSetChanged();
			}
		});

		View customView = actionBar.getCustomView().findViewById(
				R.id.searchfield);
		
		search = (EditText) customView ;

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
		textView.setOnTouchListener(new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				textView.showDropDown();
				return false;
			}
		});
		
		
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

					// NOTE: In the author's example, he uses an identifier
					// called searchBar. If setting this code on your EditText
					// then use v.getWindowToken() as a reference to your
					// EditText is passed into this callback as a TextView

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

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
		case R.id.action_search:
			MainActivity.name = search.getText().toString();
			ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
			if (networkInfo != null && networkInfo.isConnected()) {
				new DownloadWebpageTask()
						.execute("http://www.vorname.com/name,"
								+ MainActivity.name + ".html");
			} else {
				Log.e("my", "no WLAN");
				startWifi();
			}
			return true;
		case R.id.action_visit:
			visitWebsite();
			return true;
		case R.id.action_visit2:
			visitWebsite();
			return true;
			// case R.id.action_settings:
			// if (MainActivity.name == "") {
			// goToUrl("http://vorname.com");
			// } else {
			// goToUrl("http://www.vorname.com/name,"
			// + MainActivity.name + ".html");
			// }
			// return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void visitWebsite() {
		if (MainActivity.name == "") {
			goToUrl("http://vorname.com");
		} else {
			goToUrl("http://www.vorname.com/name," + MainActivity.name
					+ ".html");
		}
	}

	private void goToUrl(String url) {
		Uri uriUrl = Uri.parse(url);
		Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
		launchBrowser.addCategory(Intent.CATEGORY_BROWSABLE);
		startActivity(launchBrowser);
	}

	public void startWifi() {
		startActivity(new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK));
	}

	private class DownloadWebpageTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... urls) {

			// params comes from the execute() call: params[0] is the url.
			try {
				return downloadUrl(urls[0]);
			} catch (IOException e) {
				return "Unable to retrieve web page. URL may be invalid.";
			}
		}

		private String downloadUrl(String myurl) throws IOException {
			InputStream is = null;
			// Only display the first 500 characters of the retrieved
			// web page content.
			int len = 50000;

			try {
				URL url = new URL(myurl);
				HttpURLConnection conn = (HttpURLConnection) url
						.openConnection();
				conn.setReadTimeout(1000 /* milliseconds */);
				conn.setConnectTimeout(15000 /* milliseconds */);
				conn.setRequestMethod("GET");
				conn.setDoInput(true);
				// Starts the query
				conn.connect();
				int response = conn.getResponseCode();
				Log.d("my", "The response is: " + response);
				is = conn.getInputStream();

				// Convert the InputStream into a string
				String contentAsString = readIt(is, len);
				return contentAsString;

				// Makes sure that the InputStream is closed after the app is
				// finished using it.
			} finally {
				if (is != null) {
					is.close();
				}
			}
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

			try {
				String parsenames = result.substring(
						result.lastIndexOf("Spitzname"), result.length());
				String names = parsenames.substring(
						parsenames.indexOf("<p>") + 3,
						parsenames.indexOf("</p>"));

				resultList.clear();
				resultSelected.clear();

				Log.e("my", names);

				while (true) {
					try {

						if ((names.indexOf(",") != -1)
								&& names.indexOf(",") + 1 < names.length()) {
							String tmp = names.substring(0, names.indexOf(","));
							names = new String(names.substring(
									names.indexOf(",") + 1, names.length()));
							// trim the string and replace all special html
							// characters with
							resultList.add(tmp.trim().replaceAll("\\W", ""));
							resultSelected.add(false);
						} else
							break;
					} catch (StringIndexOutOfBoundsException e) {
						Log.e("my", e.toString());
						break;
					}
				}

				refreshContent();

			} catch (Exception e) {
				TextView mView = (TextView) findViewById(R.id.textview);
				mView.setText("Keine Spitznamen zu "
						+ search.getText().toString() + " gefunden");
				Log.e("my", e.toString());
			}
		}
	}

}
