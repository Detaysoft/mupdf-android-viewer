package com.artifex.mupdf.viewer.gp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.artifex.mupdf.viewer.OutlineActivity;
import com.artifex.mupdf.viewer.R;
import com.artifex.mupdf.viewer.gp.util.ThemeColor;
import com.artifex.mupdf.viewer.gp.util.ThemeFont;
import java.util.ArrayList;

public class OutlineAdapter extends BaseAdapter {
	private final ArrayList<OutlineActivity.Item> mItems;
	private final LayoutInflater mInflater;
    private Context mContext;
	public OutlineAdapter(Context context, LayoutInflater inflater, ArrayList<OutlineActivity.Item> items) {
		mContext = context;
        mInflater = inflater;
		mItems    = items;
	}

	public int getCount() {
		return mItems.size();
	}

	public Object getItem(int arg0) {
		return null;
	}

	public long getItemId(int arg0) {
		return 0;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View v;
		if (convertView == null) {
			v = mInflater.inflate(R.layout.outline_entry, parent, false);
		} else {
			v = convertView;
		}

		((TextView)v.findViewById(R.id.title)).setText(mItems.get(position).title);
        ((TextView)v.findViewById(R.id.title)).setTextColor(ThemeColor.getInstance().getThemeColor());
        ((TextView)v.findViewById(R.id.title)).setTypeface(ThemeFont.getInstance().getItalicFont(mContext));

		((TextView)v.findViewById(R.id.page)).setText(String.valueOf(mItems.get(position).page + 1));
        ((TextView)v.findViewById(R.id.page)).setTextColor(ThemeColor.getInstance().getThemeColor());
        ((TextView)v.findViewById(R.id.page)).setTypeface(ThemeFont.getInstance().getSemiBoldFont(mContext));
		return v;
	}

}
