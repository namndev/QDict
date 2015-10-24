
package com.annie.dictionary;

import com.annie.dictionary.utils.Utils;
import com.annie.dictionary.utils.Utils.Def;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * navigation adapter
 * 
 * @author madman
 */
public class MyArrayAdapter extends ArrayAdapter<String> {
    private String[] mObjects;

    private Typeface mFont;

    LayoutInflater inflater;

    public MyArrayAdapter(Context context, String[] objects) {
        super(context, R.layout.simple_dropdown_item_1line_left, android.R.id.text1, objects);
        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mObjects = objects;
        SharedPreferences share = context.getSharedPreferences(Def.APP_NAME, Context.MODE_PRIVATE);
        mFont = Utils.getFont(context, share.getString(Def.PREF_KEY_FONT, Def.DEFAULT_FONT));
    }

    public String[] getList() {
        return mObjects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView view = (TextView)convertView;
        if (view == null) {
            view = (TextView)inflater.inflate(R.layout.simple_dropdown_item_1line_left, parent, false);
            view.setTypeface(mFont);
        }
        view.setText(mObjects[position]);
        return view;
    }
}
