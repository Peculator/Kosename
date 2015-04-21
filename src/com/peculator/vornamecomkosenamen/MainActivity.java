package com.peculator.vornamecomkosenamen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class MainActivity extends Activity {

	// TODOs
	// Deutsch - English

	private static String name = "";
	private EditText search;
	private final LinkedList<String> resultList = new LinkedList<String>();

	private AutoCompleteTextView textView;
	private GridView gridView;
	private MyAdapter adapter;
	private boolean vertical = true;
	private DownloadWebpageTask myTask;

	enum State {
		INFO, RESULTS, NAMEERROR, CONERROR, PARSINGERROR, EXCEPTION, ERROR404, NONAMEERROR, SEARCHING, NOINET,
	}

	protected State currentState;

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		// Checks the orientation of the screen
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			vertical = false;
		} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
			vertical = true;
		}
		refreshContent();

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

			} else if (currentState == State.PARSINGERROR) {
				mView.setText(R.string.state_parsing_err);

			} else if (currentState == State.EXCEPTION) {
				mView.setText(R.string.state_exception);

			} else if (currentState == State.ERROR404) {
				mView.setText(R.string.state_name_err404);

			} else if (currentState == State.NONAMEERROR) {
				mView.setText(R.string.state_no_name_err);

			} else if (currentState == State.INFO) {
				mView.setText(R.string.state_info);

			} else if (currentState == State.SEARCHING) {
				mView.setText(R.string.state_searching);

			} else if (currentState == State.NOINET) {
				mView.setText(R.string.state_noInet);

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

		myTask = new DownloadWebpageTask();

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
		if (currentState == State.SEARCHING
				&& myTask.getStatus() == AsyncTask.Status.RUNNING) {
			return;
		}

		MainActivity.name = search.getText().toString();

		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

		try {
			InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

			inputManager.hideSoftInputFromWindow(getCurrentFocus()
					.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

		} catch (NullPointerException e) {
			// There is no Window to hide --> Resume
		}

		if (networkInfo != null && networkInfo.isConnected()) {

			if (MainActivity.name.trim().length() > 0) {
				currentState = State.SEARCHING;
				refreshContent();

				String html_str = TextUtils.htmlEncode(MainActivity.name);

				myTask.cancel(true);
				myTask = new DownloadWebpageTask();
				myTask.execute("http://www.vorname.com/name," + html_str
						+ ".html");

			} else {
				currentState = State.NONAMEERROR;
				refreshContent();
			}
		} else {
			currentState = State.NOINET;
			refreshContent();
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

	private class DownloadWebpageTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... urls) {

			// params comes from the execute() call: params[0] is the url.
			try {
				return downloadUrl(urls[0]);
			} catch (IOException e) {
				currentState = State.EXCEPTION;
				return null;
			}
		}

		private String downloadUrl(String myurl) throws IOException {
			InputStream is = null;
			System.gc();

			try {
				URL url = new URL(myurl);
				HttpURLConnection conn = (HttpURLConnection) url
						.openConnection();
				conn.setRequestMethod("GET");
				// Starts the query
				conn.connect();
				int response = conn.getResponseCode();

				if (response == 200) {
					is = conn.getInputStream();
					// Convert the InputStream into a string
					String contentAsString = readIt(is);
					return contentAsString;
				} else if (response == 404) {
					currentState = State.ERROR404;
				} else {
					currentState = State.CONERROR;
				}

				// Makes sure that the InputStream is closed after the app is
				// finished using it.
			} catch (Exception e) {
				currentState = State.EXCEPTION;
			} finally {
				if (is != null) {
					is.close();
				}
			}

			return null;
		}

		// Reads an InputStream and converts it to a String.
		public String readIt(InputStream stream) {
			boolean started = false;

			BufferedReader br = new BufferedReader(
					new InputStreamReader(stream));
			StringBuffer sb = new StringBuffer();

			String line = "";
			try {
				while ((line = br.readLine()) != null) {
					if (line.contains("Spitznamen & Kosenamen"))
						started = true;
					if (line.contains("trenner40") && started) {
						started = false;
						break;
					}
					if (started)
						sb.append(line);
				}
			} catch (IOException e) {
				currentState = State.EXCEPTION;
				return null;
			}

			return sb.toString();
		}

		// onPostExecute displays the results of the AsyncTask.
		@Override
		protected void onPostExecute(String result) {
			if (currentState != State.SEARCHING) {
				refreshContent();
				return;
			}

			String names = "";
			if (result != null) {
				try {

					if (result.contains("mr5")) {
						result = result.substring(result.indexOf("mr5"));
					} else {
						names = null;
						currentState = State.CONERROR;
						refreshContent();
						System.gc();
						return;
					}
					
					while (true) {
						if (result.contains("mr5")
								&& result.contains("</span>")) {
							names += result.substring(result.indexOf("mr5")+5,result.indexOf("</span>")) + ",";
							result = result.substring(result.indexOf("</span>")+7);
						} else {
							break;
						}
					}
					System.gc();

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

								// trim the string and replace all special html
								// characters
								resultList
										.add(tmp.trim().replaceAll("\\W", ""));

							} else
								break;
						} catch (StringIndexOutOfBoundsException e) {
							currentState = State.PARSINGERROR;
							refreshContent();
							break;
						}
						catch(Exception e){
							e.printStackTrace();
						}

					}
					refreshContent();
					System.gc();

				} catch (Exception e) {
					e.printStackTrace();
					currentState = State.NAMEERROR;
					refreshContent();
				}
			} else {
				currentState = State.NAMEERROR;
				refreshContent();
			}
		}

	}
}
