package com.peculator.vornamecomkosenamen;

import java.util.LinkedList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView.FindListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class MyAdapter extends BaseAdapter {

	LinkedList<String> names;
	LinkedList<Boolean> selected;
	private Context context;
	private LayoutInflater layoutInflater;
	private Bitmap bitmap;
	private Bitmap bitmapSel;

	public MyAdapter(Context c) {
		context = c;
//		bitmap = BitmapFactory.decodeResource(context.getResources(),
//				R.drawable.ic_launcher);
		bitmap = BitmapFactory.decodeResource(context.getResources(),
				android.R.drawable.star_big_off);
		bitmapSel = BitmapFactory.decodeResource(context.getResources(),
				android.R.drawable.star_big_on);
		
		layoutInflater = LayoutInflater.from(context);
		names = new LinkedList<String>();
		selected = new LinkedList<Boolean>();
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
		
		ImageView imageView = (ImageView) grid.findViewById(R.id.image);
		
		if(selected.get(position)== true){
			imageView.setImageBitmap(bitmapSel);
		}else{
			imageView.setImageBitmap(bitmap);
		}

		return grid;
	}

}
