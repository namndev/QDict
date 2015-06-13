package com.annie.dictionary.service;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.text.Editable;
import android.text.Selection;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.annie.dictionary.MainActivity;
import com.annie.dictionary.QDictions;
import com.annie.dictionary.R;
import com.annie.dictionary.standout.StandOutFlags;
import com.annie.dictionary.standout.StandOutWindow;
import com.annie.dictionary.standout.Window;
import com.annie.dictionary.utils.Utils;
import com.annie.dictionary.utils.Utils.Def;
import com.annie.dictionary.utils.Utils.RECV_UI;
import com.annie.dictionary.utils.WebViewClientCallback;

public class QDictService extends StandOutWindow {

	private static final int CLOSED = 0, OPENED = 1;

	private static int CLOSED_WIDTH = 500;
	private static int CLOSED_HEIGHT = 700;

	private static int OPENED_WIDTH = 880;
	private static int OPENED_HEIGHT = 960;

	public static boolean RUNNING = false;
	private static int windowState = OPENED;

	private static StandOutLayoutParams closedParams;
	private static StandOutLayoutParams openedParams;

	WebView mDictViewContent;
	EditText mKeywordEdt;
	TextView mKeywordLable;
	ImageButton mSpeakImg;

	private QDictions mQDictions = null;
	private boolean bHasLoadDict = false;
	private Handler mHandler = null;
	private Runnable mClipboardTask = null;
	private String mClipboardText = "";
	private ClipboardManager mClipboardManager = null;
	@SuppressWarnings("deprecation")
	private android.text.ClipboardManager mClipboardManagerGINGER = null;

	public class ServiceWebViewClientCallback extends WebViewClientCallback {

		public ServiceWebViewClientCallback() {
		}

		@Override
		public void shouldOverrideUrlLoading(String word) {
			makeDictContent(word);
			setKeywordLable(word);
		}
	}

	private void makeDictContent(String word) {
		if (!bHasLoadDict) {
			bHasLoadDict = true;
			mQDictions.initDicts();
		}
		String htmlContent = mQDictions.generateHtmlContent(word);
		if (mDictViewContent != null)
			mQDictions.showHtmlContent(htmlContent, mDictViewContent);
	}

	private void setKeywordLable(String word) {
		if (mKeywordEdt == null) {
			mKeywordEdt = getSearchEdt(DEFAULT_ID);
		}
		if (mKeywordEdt != null) {
			mKeywordEdt.setText(word);
			// make sure we keep the caret at the end of the text view
			Editable spannable = mKeywordEdt.getText();
			Selection.setSelection(spannable, spannable.length());
		}
		if (mKeywordLable != null)
			mKeywordLable.setText(word);
	}

	@SuppressWarnings("deprecation")
	private void clipboardCheck() {
		String clipboardText = "";
		if (Build.VERSION.SDK_INT > 10) {
			if (mClipboardManager.hasPrimaryClip()) {
				clipboardText = mClipboardManager.getPrimaryClip().getItemAt(0)
						.getText().toString().trim();
			}
		} else {
			clipboardText = mClipboardManagerGINGER.getText().toString().trim();
		}
		if (clipboardText.length() > Def.LIMIT_TRANSLATE_CHAR)
			clipboardText = clipboardText.substring(0, Def.LIMIT_TRANSLATE_CHAR);
		if (mClipboardText.equalsIgnoreCase(clipboardText))
			return;
		if (clipboardText.length() > 0) {
			mClipboardText = clipboardText;
			if (!MainActivity.active) {
				showCaptureWindow();
			} else {
				Intent intent = new Intent(MainActivity.ACTION_UPDATE_UI);
				intent.putExtra(MainActivity.ACTION_UPDATE_KEY, RECV_UI.SEARCH_WORD);
				intent.putExtra("receiver_keyword", mClipboardText);
				sendBroadcast(intent);
			}
		}
	}

	private void showCaptureWindow() {
		show(DEFAULT_ID);
		setKeywordLable(mClipboardText);
		makeDictContent(mClipboardText);
		if (mDictViewContent != null && !mDictViewContent.hasFocus())
			mDictViewContent.requestFocus();
	}

	@Override
	public void onCreate() {
		mQDictions = new QDictions(this);
		mHandler = new Handler();
		float densityDpi = getResources().getDisplayMetrics().densityDpi + 0.1f;
		float scale = (densityDpi / DisplayMetrics.DENSITY_XXHIGH);
		CLOSED_WIDTH = (int) (500 * scale);
		CLOSED_HEIGHT = (int) (700 * scale);

		OPENED_WIDTH = (int) (880 * scale);
		OPENED_HEIGHT = (int) (960 * scale);
		mClipboardTask = new Runnable() {
			@Override
			public void run() {
				clipboardCheck();
				mHandler.postDelayed(mClipboardTask, Def.CLIPBOARD_TIMER);
			}
		};
		Runnable initServiceTask = new Runnable() {
			@Override
			public void run() {
				initService();
				mHandler.postDelayed(mClipboardTask, Def.CLIPBOARD_TIMER);
			}
		};
		mHandler.postDelayed(initServiceTask, Def.CLIPBOARD_TIMER);
		super.onCreate();
		RUNNING = true;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void initService() {
		if (Build.VERSION.SDK_INT > 10) {
			mClipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			if (mClipboardManager.hasPrimaryClip()) {
				mClipboardText = mClipboardManager.getPrimaryClip()
						.getItemAt(0).getText().toString().trim();
			}
		} else {
			mClipboardManagerGINGER = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			if (mClipboardManagerGINGER.hasText())
				mClipboardText = mClipboardManagerGINGER.getText().toString()
						.trim();
		}
	}

	@Override
	public void onDestroy() {
		mHandler.removeCallbacks(mClipboardTask);
		mHandler = null;
		mClipboardTask = null;
		mQDictions.destroy();
		if (mDictViewContent != null) {
			mDictViewContent.destroy();
			mDictViewContent = null;
		}
		super.onDestroy();
		RUNNING = false;
	}

	public QDictService() {
	}

	// This is the object that receives interactions from clients.
	private final IBinder mBinder = new LocalBinder();

	public class LocalBinder extends Binder {
		QDictService getService() {
			return QDictService.this;
		}
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}

	@Override
	public IBinder onBind(Intent intent) {

		return mBinder;
	}

	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}

	@Override
	public String getAppName() {
		return getResources().getString(R.string.prefs_title_capture);
	}

	@Override
	public int getAppIcon() {
		return R.drawable.ic_launcher;
	}

	@Override
	public int getFlags(int id) {

		return super.getFlags(id) | StandOutFlags.FLAG_DECORATION_SYSTEM
				| StandOutFlags.FLAG_BODY_MOVE_ENABLE
				| StandOutFlags.FLAG_DECORATION_MAXIMIZE_DISABLE;
	}

	@Override
	public void onSearch(String keyword) {
		makeDictContent(keyword);
		mKeywordLable.setText(keyword);
	}

	@Override
	public void createAndAttachView(int id, FrameLayout frame) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.capture_window, frame, true);
		mDictViewContent = (WebView) view.findViewById(R.id.dictContentView);
		mKeywordLable = (TextView) view.findViewById(R.id.tv_title);
		WebSettings webSettings = mDictViewContent.getSettings();
		webSettings.setLayoutAlgorithm(Utils.getLayoutAlgorithm(true));
		webSettings.setJavaScriptEnabled(true);
		webSettings.setDefaultTextEncodingName("UTF-8");
		// webSettings.setSupportZoom(true);
		mSpeakImg = (ImageButton) view.findViewById(R.id.action_speak);
		mSpeakImg.setVisibility(/* tts ? View.VISIBLE : */View.GONE);
	}

	private void setOpenedState(int id) {

		transition(id, OPENED);

	}

	@Override
	public boolean onClose(int id, Window window) {
		setOpenedState(id);
		stopSelf();
		return false;
	}

	private void transition(int id, int state) {

		synchronizePositions(id, closedParams, openedParams);

		windowState = state;

		updateViewLayout(id, getParams(id));
	}

	private void synchronizePositions(int id, StandOutLayoutParams... params) {

		StandOutLayoutParams currentParam = getParams(id);
		for (int i = 0; i < params.length; i++) {
			if (params[i] != null) {
				params[i].x = currentParam.x;
				params[i].y = currentParam.y;
			}
		}
	}

	@Override
	public StandOutLayoutParams getParams(int id, Window window) {
		return getParams(id);
	}

	public StandOutLayoutParams getParams(int id) {

		if (windowState == CLOSED) {
			return getClosedParams(id);
		}
		return getOpenedParams(id);
	}

	private StandOutLayoutParams getClosedParams(int id) {

		if (closedParams == null) {
			closedParams = new StandOutLayoutParams(id, CLOSED_WIDTH,
					CLOSED_HEIGHT);
		}
		closedParams.minWidth = CLOSED_WIDTH;
		closedParams.minHeight = CLOSED_HEIGHT;
		closedParams.maxWidth = CLOSED_WIDTH;
		closedParams.maxHeight = CLOSED_HEIGHT;
		return closedParams;
	}

	private StandOutLayoutParams getOpenedParams(int id) {

		if (openedParams == null) {
			openedParams = new StandOutLayoutParams(id, OPENED_WIDTH,
					OPENED_HEIGHT);
			openedParams.minWidth = OPENED_WIDTH;
			openedParams.minHeight = OPENED_HEIGHT;
		}
		return openedParams;
	}
}
