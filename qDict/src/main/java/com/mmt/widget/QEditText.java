package com.mmt.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.EditText;

import com.annie.dictionary.utils.Utils;
import com.annie.dictionary.utils.Utils.Def;

/**
 * Extends widget.EditText: Custom font for EditText
 */
public class QEditText extends EditText {

    public QEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    @TargetApi(21)
    public QEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public QEditText(Context context) {
        super(context);
    }

    private void init(Context context, AttributeSet attrs) {
        SharedPreferences mSharedPreferences = context.getSharedPreferences(Def.APP_NAME, Context.MODE_PRIVATE);
        Typeface mFont = Utils.getFont(context, mSharedPreferences.getString(Def.PREF_KEY_FONT, Def.DEFAULT_FONT));
        setTypeface(mFont);
    }
}
