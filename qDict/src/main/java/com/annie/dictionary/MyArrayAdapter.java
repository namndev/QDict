package com.annie.dictionary;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.annie.dictionary.utils.Utils;
import com.annie.dictionary.utils.Utils.Def;

/**
 * navigation adapter
 *
 * @author madman
 */
public class MyArrayAdapter extends ArrayAdapter<String> {
    LayoutInflater inflater;
    private String[] mObjects;
    private Typeface mFont;

    public MyArrayAdapter(Context context, String[] objects) {
        super(context, R.layout.simple_dropdown_item_1line_left, android.R.id.text1, objects);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mObjects = objects;
        SharedPreferences share = context.getSharedPreferences(Def.APP_NAME, Context.MODE_PRIVATE);
        mFont = Utils.getFont(context, share.getString(Def.PREF_KEY_FONT, Def.DEFAULT_FONT));
    }

    public String[] getList() {
        return mObjects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView view = (TextView) convertView;
        if (view == null) {
            view = (TextView) inflater.inflate(R.layout.simple_dropdown_item_1line_left, parent, false);
            view.setTypeface(mFont);
        }
        view.setText(mObjects[position]);
        return view;
    }
}
