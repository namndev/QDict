package com.mmt.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.annie.dictionary.R;

public class M2tToast extends Toast {

    private static int SHADOW_TEXTVIEW = 1;

    private static M2tToast mInstance;

    private static int mToastYOffset;
    private static TextView tvMessage;

    public M2tToast(Context context) {
        super(context);
    }

    public static M2tToast makeText(Context context, CharSequence text, int duration) {
        if (mInstance == null) {
            mInstance = new M2tToast(context);
        }

        LayoutInflater inflate = (LayoutInflater) context.getApplicationContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflate.inflate(R.layout.transient_notification, null);
        tvMessage = v.findViewById(android.R.id.message);
        tvMessage.setGravity(Gravity.CENTER_VERTICAL);
        tvMessage.setText(text);
        tvMessage.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        tvMessage.setShadowLayer(SHADOW_TEXTVIEW, SHADOW_TEXTVIEW, SHADOW_TEXTVIEW, Color.BLACK);

        mToastYOffset = context.getResources().getDimensionPixelSize(R.dimen.toast_y_offset);

        mInstance.setView(v);
        mInstance.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, mToastYOffset);
        mInstance.setDuration(duration);
        return mInstance;
    }

    public static M2tToast makeText(Context context, int resId, int duration) throws Resources.NotFoundException {
        return makeText(context, context.getResources().getText(resId), duration);
    }

    public void setTypeface(Typeface typeface) {
        tvMessage.setTypeface(typeface);
    }

    public M2tToast setToastOnTop() {
        if (mInstance == null) {
            throw new RuntimeException("Must call makeText method first");
        }

        mInstance.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, mToastYOffset);

        return mInstance;
    }
}
