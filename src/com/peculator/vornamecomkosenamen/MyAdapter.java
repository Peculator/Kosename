package com.peculator.vornamecomkosenamen;

import java.util.LinkedList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class MyAdapter extends BaseAdapter {

	LinkedList<String> names;
	private Context context;
	private LayoutInflater layoutInflater;

	public MyAdapter(Context c) {
		context = c;
		
		layoutInflater = LayoutInflater.from(context);
		names = new LinkedList<String>();
	}

	@Override
	public int getCount() {
		return names.size();
	}

	@Override
	public Object getItem(int arg0) {
		return names.get(arg0);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View grid;
		if (convertView == null) {
			grid = new View(context);
			grid = layoutInflater.inflate(R.layout.gridlayout, null);
		} else {
			grid = (View) convertView;
		}

		TextView textView = (TextView) grid.findViewById(R.id.text);
		textView.setText(names.get(position));
		
		return grid;
	}

}
