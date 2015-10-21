
package com.annie.dictionary;

import java.util.ArrayList;
import java.util.Locale;

import com.annie.dictionary.frags.ListDictFragment;
import com.annie.dictionary.frags.NavigatorFragment.NavigationCallbacks;
import com.annie.dictionary.frags.RecentFragment;
import com.annie.dictionary.frags.SearchFragment;
import com.annie.dictionary.service.QDictService;
import com.annie.dictionary.standout.StandOutWindow;
import com.annie.dictionary.utils.Utils;
import com.annie.dictionary.utils.Utils.DIALOG;
import com.annie.dictionary.utils.Utils.Def;
import com.annie.dictionary.utils.Utils.NAVIG;
import com.annie.dictionary.utils.Utils.RECV_UI;
import com.mmt.widget.DropDownListView;
import com.mmt.widget.SlidingUpPanelLayout;
import com.mmt.widget.SlidingUpPanelLayout.PanelState;
import com.mmt.widget.slidemenu.SlidingMenu;
import com.mmt.widget.slidemenu.SlidingMenu.CanvasTransformer;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.speech.RecognizerIntent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends BaseActivity implements NavigationCallbacks, OnClickListener {

    public static final String ACTION_UPDATE_UI = "com.annie.dictionary.ACTION_UPDATE_UI";

    public static final String ACTION_UPDATE_KEY = "receiver_update_ui";

    private CanvasTransformer mTransformer = new CanvasTransformer() {

        @Override
        public void transformCanvas(Canvas canvas, float percentOpen) {
            canvas.scale(percentOpen, 1, 0, 0);
        }
    };

    // const
    public static final int REQUEST_CODE = 101;

    public static final int POPUPWORDSLIST_TIMER = 200;

    public static final int LIST_WORDS_NORMAL = 0;

    public static final int LIST_WORDS_FUZZY = 1;

    public static final int LIST_WORDS_PATTERN = 2;

    public static final int LIST_WORDS_FULLTEXT = 3;

    // UI
    private SlidingUpPanelLayout mLayout;

    FragmentManager mFragmentManager;

    FragmentTransaction mTransaction;

    Toolbar mToolbar;

    public static boolean hasStoragePermission = false;

    // dict
    private DictEditTextView mDictKeywordView = null;

    private TextView mInfoSearch;

    private LinearLayout mInputLayout;

    private DropDownListView mDictKeywordsPopupList = null;

    private ImageButton mActionMenu, mActionVoice, mActionWordsList;

    private ProgressDialog mProgressDialog = null;

    // UX
    DictSpeechEng mSpeechEng;

    QDictions mDictions;

    private ListWordsTask mListWordsTask = null;

    private Handler mPopupWordsListHandler = null;

    private static Handler mProgressCBHandler = null;

    // keyboard handler
    private Handler mShowKeyboardHander = null;

    private Runnable mShowKeyboarRunable = null;

    //
    public static boolean active = false;

    private Runnable mPopupWordsListRunnable = null;

    private boolean mReplaceKeyword = false;

    private boolean mIsTaskRunning = false;

    int mCurrentNavPosition = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // UX:
        mSpeechEng = DictSpeechEng.getInstance(this);
        mSpeechEng.setLocale("en_US");
        checkPermission(REQUEST_STORAGE_CODE);
        // UI: set the Above View
        setContentView(R.layout.content_frame);
        mToolbar = (Toolbar)findViewById(R.id.main_toolbar);
        mToolbar.setTitle(null);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mLayout = (SlidingUpPanelLayout)findViewById(R.id.sliding_layout);
        mLayout.setPanelState(PanelState.HIDDEN);
        mLayout.setTouchEnabled(false);
        mFragmentManager = getSupportFragmentManager();

        //
        SlidingMenu sm = getSlidingMenu();
        setSlidingActionBarEnabled(true);
        sm.setBehindScrollScale(0.0f);
        sm.setBehindCanvasTransformer(mTransformer);
        // layout_drag

        mActionMenu = (ImageButton)findViewById(R.id.action_menu);
        mActionVoice = (ImageButton)findViewById(R.id.action_voice);
        mActionWordsList = (ImageButton)findViewById(R.id.action_wordslist);
        mActionMenu.setOnClickListener(this);
        mActionVoice.setOnClickListener(this);
        mActionWordsList.setOnClickListener(this);
        mInputLayout = (LinearLayout)findViewById(R.id.layout_input);
        mInfoSearch = (TextView)findViewById(R.id.tv_info_search);
        mDictKeywordView = new DictEditTextView(this);
        mInputLayout.addView(mDictKeywordView, 0,
                new LayoutParams(0, android.view.ViewGroup.LayoutParams.MATCH_PARENT, 1));
        initDropList();

        String keyword = null;
        if (savedInstanceState != null) {
            mCurrentNavPosition = savedInstanceState.getInt("position_fragment");
            keyword = savedInstanceState.getString("search_fragment_keyword", "");
        }
        boolean isFirst = mSharedPreferences.getBoolean("qdict_firt_start", true);
        if (isFirst) {
            setFragment(getString(R.string.guide_lable), NAVIG.HOME);
            mSharedPreferences.edit().putBoolean("qdict_firt_start", false).apply();
        } else {
            boolean isSearch = !TextUtils.isEmpty(keyword);
            String title = (mCurrentNavPosition == NAVIG.HOME || mCurrentNavPosition == NAVIG.SEARCH)
                    ? ((isSearch) ? keyword : getResources().getString(R.string.guide_lable)) : "";
            setFragment(title, (mCurrentNavPosition != -1) ? mCurrentNavPosition : NAVIG.RECENT);
        }

        mShowKeyboardHander = new Handler();
        mProgressCBHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                int progress = msg.arg1;
                if (null != mProgressDialog)
                    mProgressDialog.setProgress(progress);
            }
        };
        mShowKeyboarRunable = new Runnable() {
            @Override
            public void run() {
                mDictKeywordView.requestFocus();
                mDictKeywordView.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                mDictKeywordView.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));
            }
        };
        startService();
        registerReceiver(mUIReceiver, new IntentFilter(ACTION_UPDATE_UI));
    }

    @Override
    public void onRequestPermissionResult(int requestCode, boolean isSucess) {
        if (REQUEST_STORAGE_CODE == requestCode) {
            hasStoragePermission = isSucess;
            if (isSucess) {
                mDictions = new QDictions(this);
                mDictions.initDicts();
            } else {
                finish();
            }
        } else if (REQUEST_ALERT_WINDOW_CODE == requestCode) {
            Intent i = new Intent(Intent.ACTION_RUN);
            i.setClass(MainActivity.this, QDictService.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (!QDictService.RUNNING)
                startService(i);
            if (!isSucess) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, R.string.msg_do_not_show_popup, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("position_fragment", mCurrentNavPosition);
        if (mCurrentNavPosition == NAVIG.SEARCH) {
            Fragment fragment = this.getSupportFragmentManager().findFragmentById(R.id.content_frame);
            if (fragment instanceof SearchFragment) {
                SearchFragment fg = ((SearchFragment)fragment);
                if (fg.isSearch()) {
                    outState.putString("search_fragment_keyword", fg.getKeyword());
                }
            }
        }
    }

    public Toolbar getToolbar() {
        return mToolbar;
    }

    private void initDropList() {
        mDictKeywordsPopupList = (DropDownListView)findViewById(R.id.drop_list);
        mDictKeywordsPopupList.setFocusable(true);
        mDictKeywordsPopupList.setFocusableInTouchMode(true);
        mDictKeywordsPopupList.setListSelectionHidden(false);
        mDictKeywordsPopupList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

                TextView textView = (TextView)v;
                String keyword = textView.getText().toString();

                mReplaceKeyword = true; // Don't response the
                                        // onTextChanged event this
                                        // time.

                mDictKeywordView.setText(keyword);
                mInfoSearch.setVisibility(View.GONE);
                mActionWordsList.setVisibility(View.GONE);
                mInfoSearch.setText(null);
                // make sure we keep the caret at the end of the text
                // view
                Editable spannable = mDictKeywordView.getText();
                Selection.setSelection(spannable, spannable.length());
                showSearchContent();
            }
        });
        mPopupWordsListHandler = new Handler();
        mPopupWordsListRunnable = new Runnable() {
            @Override
            public void run() {
                startKeywordsList();
            }
        };

    }

    private void startService() {
        if (mSharedPreferences.getBoolean(getString(R.string.prefs_key_using_capture), false)) {
            if (!QDictService.RUNNING)
                checkPermission(REQUEST_ALERT_WINDOW_CODE);
        } else {
            if (QDictService.RUNNING)
                StandOutWindow.closeAll(this, QDictService.class);
        }
    }

    private void stopService() {
        if (QDictService.RUNNING) {
            StandOutWindow.closeAll(this, QDictService.class);
        }
    }

    @Override
    protected void onStart() {
        active = true;
        super.onStart();
    }

    @Override
    protected void onStop() {
        active = false;
        super.onStop();
    };

    private void showProgressDialog() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setMessage(getResources().getString(R.string.keywords_search));
        mProgressDialog.setCancelable(false);
        mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        mDictions.cancelLookup();
                        dialog.cancel();
                    }
                });
        mProgressDialog.show();
    }

    public static void lookupProgressCB(int progress) {
        Message m = Message.obtain();
        m.arg1 = progress;
        m.setTarget(mProgressCBHandler);
        m.sendToTarget();
    }

    private void startKeywordsList() {
        int listType = LIST_WORDS_NORMAL;
        String keyword = mDictKeywordView.getText().toString().trim();
        keyword = keyword.trim();

        if (keyword.length() <= 0) {
            return;
        }

        if ((keyword.charAt(0) == '/') || (keyword.charAt(0) == ':') || (keyword.indexOf('*') >= 0)
                || (keyword.indexOf('?') >= 0)) {
            if (keyword.charAt(0) == '/') {
                keyword = keyword.substring(1);
                listType = LIST_WORDS_FUZZY;
            } else if (keyword.charAt(0) == ':') {
                keyword = keyword.substring(1);
                listType = LIST_WORDS_FULLTEXT;
            } else {
                listType = LIST_WORDS_PATTERN;
            }
        }

        if (true == mIsTaskRunning) // One task is running.
        {
            return;
        } else {
            mIsTaskRunning = true;
            if (LIST_WORDS_NORMAL != listType)
                showProgressDialog();
            mListWordsTask = new ListWordsTask(listType);
            if (Build.VERSION.SDK_INT > 10)
                mListWordsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new String[] {
                        keyword
                });
            else {
                mListWordsTask.execute(new String[] {
                        keyword
                });
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSpeechEng != null) {
            mSpeechEng.destroy();
            mSpeechEng = null;
        }
        if (mDictions != null) {
            mDictions.destroy();
            mDictions = null;
        }
        if (mUIReceiver != null)
            unregisterReceiver(mUIReceiver);
        //
        mShowKeyboardHander.removeCallbacks(mShowKeyboarRunable);
        mShowKeyboarRunable = null;
        mPopupWordsListRunnable = null;
    }

    @Override
    protected void onPause() {
        mPopupWordsListHandler.removeCallbacks(mPopupWordsListRunnable);
        super.onPause();
    }

    // start Recognition
    private void startVoiceRecognition() {
        // start voice
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.ENGLISH);
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, Locale.ENGLISH);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.press_on_when_done));
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        // ... put other settings in the Intent
        startActivityForResult(intent, REQUEST_CODE);
    }

    BroadcastReceiver mUIReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int id = intent.getIntExtra(ACTION_UPDATE_KEY, -1);
            if (id == RECV_UI.SELECT_DICT) {
                backMainMenuFragment();
            } else if (id == RECV_UI.CHANGE_THEME || id == RECV_UI.CHANGE_FONT) {
                stopService();
                Utils.changeToTheme(MainActivity.this);
            } else if (id == RECV_UI.SEARCH_WORD) {
                String keyword = intent.getStringExtra("receiver_keyword");
                if (!TextUtils.isEmpty(keyword)) {
                    mDictKeywordView.setText(keyword);
                    showSearchContent();
                }
            } else if (id == RECV_UI.RELOAD_DICT) {
                mDictions.initDicts();
            } else if (id == RECV_UI.RUN_SERVICE) {
                startService();
            } else if (id == RECV_UI.CHANGE_FRAG) {
                int pos = intent.getIntExtra("receiver_frag_position", NAVIG.RECENT);
                if (mCurrentNavPosition == NAVIG.SEARCH) {
                    setFragment("", pos);
                }
                mCurrentNavPosition = pos;
            }
        }
    };

    private void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        // check if no view has focus:
        View view = getCurrentFocus();
        if (view != null) {
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public void showSearchContent() {
        String keyword = mDictKeywordView.getText().toString().trim();
        Fragment fragment = this.getSupportFragmentManager().findFragmentById(R.id.content_frame);
        if (fragment instanceof SearchFragment) {
            SearchFragment sFrag = (SearchFragment)fragment;
            sFrag.setDictions(mDictions);
            sFrag.setSpeechEng(mSpeechEng);
            sFrag.setKeyword(keyword);
            mCurrentNavPosition = NAVIG.SEARCH;
        } else {
            try {
                mTransaction = mFragmentManager.beginTransaction();
                Fragment searchFrag = new SearchFragment(mSpeechEng, mDictions, keyword, true);
                mTransaction.replace(R.id.content_frame, searchFrag).commit();
                mCurrentNavPosition = NAVIG.SEARCH;
            } catch (IllegalStateException ex) {
            }
        }

        if (mLayout != null && (mLayout.getPanelState() != PanelState.HIDDEN)) {
            mLayout.setPanelState(PanelState.HIDDEN);
            mActionMenu.clearFocus();
        }
        hideKeyboard();
    }

    @Override
    public void onBackPressed() {
        if (mLayout != null
                && (mLayout.getPanelState() == PanelState.EXPANDED || mLayout.getPanelState() == PanelState.ANCHORED)) {
            mLayout.setPanelState(PanelState.HIDDEN);
            mActionMenu.clearFocus();
        } else {
            super.onBackPressed();
        }
    }

    public void onActionButtonClick(View v) {
        if (mLayout != null) {
            mLayout.setPanelState(PanelState.EXPANDED);
            mShowKeyboardHander.removeCallbacks(mShowKeyboarRunable);
            int textLength = mDictKeywordView.getText().length();
            if (textLength == 0) {
                mShowKeyboardHander.postDelayed(mShowKeyboarRunable, 200);
            }
        }
    }

    @Override
    public void onNavigationItemSelected(String title, int position) {
        if (position == NAVIG.SELECT_DICT) {
            Fragment fragment = new ListDictFragment(mDictions);
            setMenuFragment(fragment);
        } else if (position == NAVIG.SETTINGS) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            toggle();
        } else if (position == NAVIG.JOIN_US) {
            Intent intent = Utils.getOpenPageFBIntent(getApplicationContext());
            startActivity(intent);
            toggle();
        } else {
            toggle();
            if (mLayout != null && (mLayout.getPanelState() == PanelState.EXPANDED
                    || mLayout.getPanelState() == PanelState.ANCHORED)) {
                mLayout.setPanelState(PanelState.HIDDEN);
            }
            if (mCurrentNavPosition != position)
                setFragment(title, position);
        }
    }

    public void setFragment(String keyword, int position) {
        mTransaction = mFragmentManager.beginTransaction();
        Fragment fragment;
        switch (position) {
            case NAVIG.HOME:
                fragment = new SearchFragment(mSpeechEng, mDictions, keyword, false);
                break;
            case NAVIG.SEARCH:
                fragment = new SearchFragment(mSpeechEng, mDictions, keyword, true);
                break;
            case NAVIG.RECENT:
                fragment = new RecentFragment();
                Bundle b = new Bundle();
                b.putBoolean("qdict_is_favorite", false);
                fragment.setArguments(b);
                break;
            case NAVIG.FAVORITE:
                fragment = new RecentFragment();
                Bundle b2 = new Bundle();
                b2.putBoolean("qdict_is_favorite", true);
                fragment.setArguments(b2);
                break;
            default:
                fragment = null;
                break;
        }
        if (fragment != null) {
            mTransaction.replace(R.id.content_frame, fragment).commit();
            mCurrentNavPosition = position;
        }
    }

    // Extend classes.
    private class DictEditTextView extends EditText {

        int type = LIST_WORDS_NORMAL;

        public DictEditTextView(Context context) {
            super(context, null);
            setSelectAllOnFocus(true);
            setHint(R.string.action_search);
            setImeOptions(EditorInfo.IME_ACTION_SEARCH);
            setPadding(10, 0, 3, 0);
            setGravity(Gravity.CENTER_VERTICAL);
            setSingleLine();
            Typeface mFont = Utils.getFont(context, mSharedPreferences.getString(Def.PREF_KEY_FONT, Def.DEFAULT_FONT));
            setTypeface(mFont);
        }

        @Override
        public boolean onKeyUp(int keyCode, KeyEvent event) {
            if (keyCode != KeyEvent.KEYCODE_SPACE && (mDictKeywordsPopupList.getSelectedItemPosition() >= 0
                    || (keyCode != KeyEvent.KEYCODE_SEARCH && keyCode != KeyEvent.KEYCODE_DPAD_CENTER))) {
                mDictKeywordsPopupList.onKeyUp(keyCode, event);
            }

            switch (keyCode) {
                // avoid passing the focus from the text view to the next
                // component
                case KeyEvent.KEYCODE_SEARCH:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_DPAD_UP:
                    return (type == LIST_WORDS_NORMAL);

            }
            return super.onKeyUp(keyCode, event);
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            if (keyCode != KeyEvent.KEYCODE_SPACE && (mDictKeywordsPopupList.getSelectedItemPosition() >= 0
                    || (keyCode != KeyEvent.KEYCODE_SEARCH && keyCode != KeyEvent.KEYCODE_DPAD_CENTER))) {
                mDictKeywordsPopupList.requestFocusFromTouch();
                mDictKeywordsPopupList.onKeyDown(keyCode, event);
            }
            switch (keyCode) {
                // avoid passing the focus from the text view to the next
                // component
                case KeyEvent.KEYCODE_SEARCH:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_DPAD_UP:
                    mDictKeywordsPopupList.setListSelectionHidden(false);
                    return true;
            }
            if (KeyEvent.KEYCODE_SEARCH == keyCode || KeyEvent.KEYCODE_ENTER == keyCode
                    || KeyEvent.KEYCODE_DPAD_CENTER == keyCode) {
                if (type == LIST_WORDS_NORMAL) {
                    showSearchContent();
                } else {
                    mPopupWordsListHandler.postDelayed(mPopupWordsListRunnable, POPUPWORDSLIST_TIMER);
                }
                hideKeyboard();
            }
            return super.onKeyDown(keyCode, event);
        }

        @Override
        protected void onTextChanged(CharSequence text, int start, int before, int after) {
            if (mPopupWordsListHandler != null)
                mPopupWordsListHandler.removeCallbacks(mPopupWordsListRunnable);

            if (mReplaceKeyword == true) {
                mReplaceKeyword = false;
            } else {
                String keyword = text.toString();
                if (!TextUtils.isEmpty(keyword)) {
                    if (keyword.charAt(0) == '/') {
                        mInfoSearch.setVisibility(View.VISIBLE);
                        mInfoSearch.setText(R.string.fuzzy_query_prompt);
                        mActionWordsList.setVisibility(View.VISIBLE);
                        type = LIST_WORDS_FUZZY;
                    } else if (keyword.charAt(0) == ':') {
                        mInfoSearch.setVisibility(View.VISIBLE);
                        mInfoSearch.setText(R.string.fulltext_query_prompt);
                        mActionWordsList.setVisibility(View.VISIBLE);
                        type = LIST_WORDS_FULLTEXT;
                    } else if (((keyword.indexOf('*') >= 0) || (keyword.indexOf('?') >= 0))) {
                        mInfoSearch.setVisibility(View.VISIBLE);
                        mInfoSearch.setText(R.string.pattern_query_prompt);
                        mActionWordsList.setVisibility(View.VISIBLE);
                        type = LIST_WORDS_PATTERN;
                    } else {
                        mInfoSearch.setVisibility(View.GONE);
                        mActionWordsList.setVisibility(View.GONE);
                        mInfoSearch.setText(null);
                        type = LIST_WORDS_NORMAL;
                        if (mPopupWordsListHandler != null)
                            mPopupWordsListHandler.postDelayed(mPopupWordsListRunnable, POPUPWORDSLIST_TIMER);
                    }
                }
            }
            super.onTextChanged(text, start, before, after);
        }
    }

    private class ListWordsTask extends AsyncTask<String, Void, String[]> {

        int mListType;

        public ListWordsTask(int listType) {
            mListType = listType;
        }

        @Override
        protected String[] doInBackground(String... params) {
            String strWordsList[] = null;
            String keyword = params[0];
            switch (mListType) {
                case LIST_WORDS_NORMAL:
                    strWordsList = mDictions.listWords(keyword);
                    break;
                case LIST_WORDS_FUZZY:
                    strWordsList = mDictions.fuzzyListWords(keyword);
                    break;
                case LIST_WORDS_PATTERN:
                    strWordsList = mDictions.patternListWords(keyword);
                    break;
                case LIST_WORDS_FULLTEXT:
                    strWordsList = mDictions.fullTextListWords(keyword);
                    break;
            }
            return strWordsList;
        }

        @Override
        protected void onPostExecute(String[] strWordsList) {

            mIsTaskRunning = false; // Task has stopped.

            if (null != mProgressDialog && mProgressDialog.isShowing())
                mProgressDialog.cancel();

            // Haven't got any words, not show the words list popup window.
            if (null == strWordsList || strWordsList.length <= 0) {
                if (LIST_WORDS_NORMAL != mListType) {
                    // mDictions.showHtmlContent(
                    // getResources().getString(R.string.spell_error),
                    // mDictContentView);
                }
                return;
            }
            showKeywordsList(strWordsList);
        }
    }

    private void showKeywordsList(String[] strWordsList) {
        MyArrayAdapter keywordsAdapter = new MyArrayAdapter(this, strWordsList);
        mDictKeywordsPopupList.setAdapter(keywordsAdapter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String keyword = results.get(0);
                if (!TextUtils.isEmpty(keyword)) {
                    mDictKeywordView.setText(keyword);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showSearchContent();
                        }
                    });

                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.action_menu:
                toggle();
                break;
            case R.id.action_voice:
                // checkPermission(REQUEST_MICRO_CODE);
                startVoiceRecognition();
                break;
            case R.id.action_wordslist:
                startKeywordsList();
                break;
            case R.id.action_share:
                startActivity(Utils.getIntentShareData(MainActivity.class));
                break;
            case R.id.action_about:
                showDialog(DIALOG.ABOUT);
                break;
            default:
                break;
        }
    }

    // / for menu
    @SuppressWarnings("deprecation")
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG.ABOUT:
                return Utils.createAboutDialog(this);
            case DIALOG.CHANGE_LOG:
                return Utils.createWhatsNewDialog(this);
            default:
                break;
        }
        return super.onCreateDialog(id);
    }

}
