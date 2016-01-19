
package com.mmt.widget;

import com.annie.dictionary.utils.Utils;
import com.annie.dictionary.utils.Utils.Def;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Extends View.TextView: Custom font for TextView
 */
public class QTextView extends TextView {

    public QTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    @TargetApi(21)
    public QTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        SharedPreferences mSharedPreferences = context.getSharedPreferences(Def.APP_NAME, Context.MODE_PRIVATE);
        Typeface mFont = Utils.getFont(context, mSharedPreferences.getString(Def.PREF_KEY_FONT, Def.DEFAULT_FONT));
        setTypeface(mFont);
    }

    public QTextView(Context context) {
        super(context);
        init(context);
    }

    public void updateFont(Context context) {
        init(context);
    }

}
