package com.mmt.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListView;

public class DropDownListView extends ListView {
    private boolean mListSelectionHidden;

    public DropDownListView(Context context) {
        super(context, null, android.R.attr.dropDownListViewStyle);
    }

    @TargetApi(21)
    public DropDownListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, android.R.attr.dropDownListViewStyle, defStyleRes);
    }

    public DropDownListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, android.R.attr.dropDownListViewStyle);
    }

    public DropDownListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean isInTouchMode() {
        return mListSelectionHidden || super.isInTouchMode();
    }

    @Override
    public boolean hasWindowFocus() {
        return true;
    }

    @Override
    public boolean isFocused() {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mListSelectionHidden = true;
        return super.onTouchEvent(ev);
    }

    @Override
    public boolean hasFocus() {
        return true;
    }

    public boolean isListSelectionHidden() {
        return mListSelectionHidden;
    }

    public void setListSelectionHidden(boolean listSelectionHidden) {
        this.mListSelectionHidden = listSelectionHidden;
    }

}
