package com.annie.dictionary;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.WebView;

import com.annie.dictionary.utils.Utils;
import com.annie.dictionary.utils.Utils.Def;

public class QDictions {
	// private final String FILTER_SYMBOLS =
	// "\"\\^\\$\\*\\+\\{\\}\\[\\]\\?\\(\\)\\|\\\\,.:;/=_!@#%&<>`~0123456789";
	public final static int DICT_TYPE_INDEX = 0x0001;
	String mEmptyList = "";
	private WeakReference<Context> mContext = null;
	private QDictEng mQDictEng = null;
	private SharedPreferences mSharedPrefs;

	public QDictions(Context context) {
		mContext = new WeakReference<Context>(context);
		mSharedPrefs = mContext.get().getSharedPreferences(Def.APP_NAME,
				Context.MODE_PRIVATE);
		mQDictEng = QDictEng.createQDictEng();
	}

	public void initDicts() {
		mQDictEng.UnloadDicts();
		loadDictsFromFolder();
		mQDictEng.LoadDicts(QDictEng.sDictPaths, QDictEng.sDictNames,
				QDictEng.sDictTypes);
	}

	private String[] lookupWord(String word) {
		String strWordsArray[] = null;
		String keyword = word;
		String keyword2 = "";
		if (keyword.length() <= 0)
			return null;
		strWordsArray = mQDictEng.Lookup(keyword, DICT_TYPE_INDEX);
		if (null != strWordsArray && strWordsArray.length > 0)
			return strWordsArray;
		// step 2: to lowercase.
		keyword2 = keyword.toLowerCase(Locale.US);
		if (keyword2.equals(keyword))
			return null;
		strWordsArray = mQDictEng.Lookup(keyword2, DICT_TYPE_INDEX);
		return strWordsArray;
	}

	private void checkDict(String[] dictFolders, int k) {
		String dictIndexAll = mSharedPrefs.getString(Def.PREF_INDEX_ALL,
				mEmptyList);
		String[] dictIndexArray = dictIndexAll.split(";");
		// Check if some dictionaries have been removed from SD card.
		if (!dictIndexAll.equals("")) {
			for (String dictIndex : dictIndexArray) {
				boolean bFound = false;
				for (int i = 0; i < k; i++) {
					if (dictFolders[i] == null)
						continue;
					else if (dictFolders[i].equals(dictIndex)) {
						bFound = true;
						break;
					}
				}
				// Not found this dictionary, it has been
				// removed from the SD card.
				if (false == bFound) { // Remove it from the configuration file.
					removeDictInArrays(Def.PREF_INDEX_CHECKED, dictIndex);
					removeDictInArrays(Def.PREF_INDEX_ALL, dictIndex);
				}
			}
		}
		// Check if some dictionaries have been added to SD card.
		for (int i = 0; i < k; i++) {
			boolean bFound = false;
			for (String dictIndex : dictIndexArray) {
				if (dictFolders[i] == null)
					continue;
				else if (dictFolders[i].equals(dictIndex)) {
					bFound = true;
					break;
				}
			}
			if (false == bFound) {
				addDictInArrays(Def.PREF_INDEX_ALL, dictFolders[i]);
			}
		}
	}

	private void buildDictInfo(String dictsPath, int maxDictCnt,
			String[] dictFolders, String[] dictNames) {
		String dictIndexs = mSharedPrefs.getString(Def.PREF_INDEX_CHECKED,
				mEmptyList);
		String[] dictIndexArray = dictIndexs.split(";");
		int lDictCnt = 0;
		String lDictPaths[] = new String[maxDictCnt];
		String lDictNames[] = new String[maxDictCnt];
		int lDictTypes[] = new int[maxDictCnt];

		if (!dictIndexs.equals("")) {
			int i = 0;
			for (String dictIndex : dictIndexArray) {
				lDictPaths[i] = dictsPath + "/" + dictIndex + "/";
				for (int k = 0; k < dictFolders.length; k++) {
					try {
						if (dictFolders[k] == null)
							continue;
						else if (dictFolders[k].equals(dictIndex))
							lDictNames[i] = dictNames[k];
					} catch (NullPointerException e) {
					}
				}
				lDictTypes[i] = DICT_TYPE_INDEX;
				i++;
			}
			lDictCnt = i;
		}

		if (lDictCnt > 0) {
			QDictEng.sDictPaths = new String[lDictCnt];
			QDictEng.sDictNames = new String[lDictCnt];
			QDictEng.sDictTypes = new int[lDictCnt];

			for (int i = 0; i < lDictCnt; i++) {
				QDictEng.sDictPaths[i] = lDictPaths[i];
				QDictEng.sDictNames[i] = lDictNames[i];
				QDictEng.sDictTypes[i] = lDictTypes[i];
			}
		} else {
			QDictEng.sDictPaths = null;
			QDictEng.sDictNames = null;
			QDictEng.sDictTypes = null;
		}
	}

	private void loadDictsFromFolder() {
		String dictsPath = Utils.getRootFolder() + Def.DICT_FOLDER;
		File f = new File(dictsPath);

		if (!f.exists()) {
			f.mkdirs();
		}
		if (!f.exists() || !f.isDirectory()) {
			Log.d("QDictions", "file is not exists ");
			QDictEng.sDictPaths = null;
			QDictEng.sDictNames = null;
			QDictEng.sDictTypes = null;
			setShareDictEmpty();
			return;
		}

		int k = 0;
		File[] files = f.listFiles();
		String dictPaths[] = new String[files.length];
		String dictFolders[] = new String[files.length];
		String dictNames[] = new String[files.length];

		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				String dictName = Utils.getFileInfoName(files[i].getPath());
				if (null != dictName) {
					dictPaths[k] = files[i].getPath();
					dictFolders[k] = files[i].getName();
					dictNames[k] = dictName;
					k++;
				}
			}
		}
		if (k == 0) {
			setShareDictEmpty();
			return;
		} else if (k > 0) {
			// Check if there are any dictionaries have been removed from the SD
			// card or some new dictionaries have been added to SD card.
			checkDict(dictFolders, k);
			String mDictIndexAll = mSharedPrefs.getString(Def.PREF_INDEX_ALL,
					mEmptyList);
			if (mDictIndexAll.equals("")) {
				for (int m = 0; m < k; m++) {
					mDictIndexAll = addDictInArrays(Def.PREF_INDEX_ALL,
							dictFolders[m]);
				}
			}
		}
		buildDictInfo(dictsPath, k, dictFolders, dictNames);
	}

	private void setShareDictEmpty() {
		Editor editor = mSharedPrefs.edit();
		editor.putString(Def.PREF_INDEX_CHECKED, mEmptyList);
		editor.putString(Def.PREF_INDEX_ALL, mEmptyList);
		editor.apply();
	}

	public void removeDictInArrays(String key, String dictIndex) {
		String set = mSharedPrefs.getString(key, mEmptyList);
		if (set.contains(dictIndex)) {
			set = set.replaceAll(dictIndex + ";", "");
			mSharedPrefs.edit().putString(key, set).apply();
		}
	}

	public String addDictInArrays(String key, String dictIndex) {
		String set = mSharedPrefs.getString(key, mEmptyList);
		if (!set.contains(dictIndex)) {
			set += dictIndex + ";";
			mSharedPrefs.edit().putString(key, set).apply();
		}
		return set;
	}

	/**
	 * 
	 * Replace label attribute content.
	 * 
	 * @param str
	 *            : the string to be replaced
	 * @param beforeTag
	 *            : the label to be replaced
	 * @param tagAttrib
	 *            : the lable's attribute to be replaced
	 * @param startTag
	 *            : the begin of the new label
	 * @param endTag
	 *            : the end of the new label
	 */
	private String replaceHtmlTag(String str, String beforeTag,
			String tagAttrib, String startTag, String endTag) {
		String regxpForTag = "<\\s*" + beforeTag + "\\s+([^>]*)>";
		String regxpForTagAttrib = tagAttrib + "=([^\\s]+)\\s*";
		Pattern patternForTag = Pattern.compile(regxpForTag,
				Pattern.CASE_INSENSITIVE);
		Pattern patternForAttrib = Pattern.compile(regxpForTagAttrib,
				Pattern.CASE_INSENSITIVE);
		Matcher matcherForTag = patternForTag.matcher(str);
		StringBuffer sb = new StringBuffer();
		boolean result = matcherForTag.find();
		// go through all <img> lable.
		while (result) {
			StringBuffer sbreplace = new StringBuffer("<img ");
			Matcher matcherForAttrib = patternForAttrib.matcher(matcherForTag
					.group(1));
			if (matcherForAttrib.find()) {
				if (null == startTag) // Just remove tag.
				{
					matcherForAttrib.appendReplacement(sbreplace, "");
				} else {
					String matcherString = matcherForAttrib.group(1);
					matcherString = matcherString.replace("'", "");
					matcherString = matcherString.replace("\"", "");
					// replace '1E'
					matcherString = matcherString.replace("", "");
					// replace '1F'
					matcherString = matcherString.replace("", "");
					matcherForAttrib.appendReplacement(sbreplace, startTag
							+ matcherString + endTag);
				}
			}
			matcherForAttrib.appendTail(sbreplace);
			matcherForTag.appendReplacement(sb, sbreplace.toString() + ">");
			result = matcherForTag.find();
		}
		matcherForTag.appendTail(sb);
		return sb.toString();
	}

	public String getTextHtmlColor() {
		return "#" + Def.DEFAULT_TEXT_COLOR.substring(3);
	}

	public String getWordHtmlColor() {
		return "#" + Def.DEFAULT_WORD_COLOR.substring(3);
	}

	public int getDensityDpi() {
		DisplayMetrics displayMetrics = new DisplayMetrics();
		((WindowManager) (mContext.get()
				.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay()
				.getMetrics(displayMetrics);
		return displayMetrics.densityDpi;
	}

	public String generateHtmlContent(String word) {
		String html = "";
		String dictHtmlData = "";
		String dictCheckeds = mEmptyList;

		String strWordsArray[] = lookupWord(word);
		if (null == strWordsArray)
			return mContext.get().getResources()
					.getString(R.string.keywords_null);

		dictCheckeds = mSharedPrefs.getString(Def.PREF_INDEX_CHECKED,
				mEmptyList);

		String[] dictCheckedArrays = dictCheckeds.split(";");
		// This is for dictionary order.
		String dictContentArray[] = new String[dictCheckedArrays.length];
		for (int i = 0; i < strWordsArray.length;) {
			String dictContent = "";
			if (null != strWordsArray[i] && null != strWordsArray[i + 1]
					&& null != strWordsArray[i + 2]) {
				int dictID = Integer.parseInt(strWordsArray[i]);
				String strResPath = "file://" + QDictEng.sDictPaths[dictID]
						+ "res/";
				String strDictID = "conID" + strWordsArray[i++];
				String strDictName = strWordsArray[i++];
				String strDictContent = strWordsArray[i++];

				// Add "hide/show" script to dictionary name <div>, this can
				// hide/show the dictionary content.
				strDictName = "<div onclick=\"javascript:var obj=document.getElementById('"
						+ strDictID
						+ "');if(obj.innerHTML==''){obj.innerHTML="
						+ strDictID
						+ ";}else{"
						+ strDictID
						+ "=obj.innerHTML;obj.innerHTML='';}\">"
						+ strDictName
						+ "</div>";
				int backgBorder = Utils.getColor(mContext.get(),
						R.attr.colorPrimary);
				String strColor = String
						.format("#%06X", 0xFFFFFF & backgBorder);
				// Change the style of dictionary name.
				strDictName = "<TABLE border=0 cellSpacing=0 cellPadding=0><TR><TD style=\"PADDING-LEFT:6px;PADDING-RIGHT:6px;BORDER-BOTTOM:#92b0dd 1px solid;"
						+ "BORDER-LEFT:#92b0dd 1px solid;BACKGROUND:"
						+ strColor
						+ ";COLOR:#2b2721;BORDER-TOP:#92b0dd 1px solid;BORDER-RIGHT:#92b0dd 1px solid\" noWrap>"
						+ strDictName
						+ "</TD><TD style=\"BORDER-BOTTOM:#92b0dd 1px solid\" height=\"36px\" width=\"100%\">&nbsp;</TD></TR></TABLE>";

				// Change '<img src="8CB0DC57.png">' to '<img
				// src="/sdcard/.../res/8CB0DC57.png">'
				// Change '<IMG src='8CB0DC57.png'>' to '<img
				// src='/sdcard/.../res/8CB0DC57.png'>'
				strDictContent = replaceHtmlTag(strDictContent, "img", "src",
						"src='" + strResPath, "' ");
				strDictContent = replaceHtmlTag(strDictContent, "img", "width",
						null, null);
				strDictContent = replaceHtmlTag(strDictContent, "img",
						"height", null, null);
				// Wrap a <div> to the dictionary content.
				strDictContent = "<div id='" + strDictID + "'>"
						+ strDictContent + "</div>";
				dictContent = strDictName + strDictContent + "<br>";
				// This is for dictionary order.
				int k = 0;
				for (String dictChecked : dictCheckedArrays) {
					if (QDictEng.sDictPaths[dictID].indexOf(dictChecked) >= 0) {
						dictContentArray[k] = dictContent;
						break;
					}
					k++;
				}
			} else {
				i += 3; // Ignore this dictionary.
			}
		}
		for (int k = 0; k < dictContentArray.length; k++) {
			if (null != dictContentArray[k])
				dictHtmlData += dictContentArray[k];
		}
		if (TextUtils.isEmpty(dictHtmlData)) {
			return mContext.get().getResources()
					.getString(R.string.keywords_null);
		}

		String textColor = getTextHtmlColor();
		String wordColor = getWordHtmlColor();
		String font = mSharedPrefs.getString(Def.PREF_KEY_FONT,
				Def.DEFAULT_FONT);
		String head = "<head><style>@font-face {font-family:'Unicode';src:url('file:///android_asset/fonts/"
				+ font + "');}";
		head += "@font-face {font-family:'KPhonetic';src:url('file:///android_asset/fonts/KPhonetic.ttf');}";
		head += " body,font{font-family:'Unicode';} i,b{font-family: sans-serif;}</style></head>";
		html = "<html>" + head + "<body style='color:" + textColor + "'>"
				+ dictHtmlData + "</body></html>";
		html = html.replace("color:#TOBEREPLACE;", "color:" + wordColor + ";");
		return html;
	}

	public String getReadmeHtml() {
		// CharSequence content = Utils.getTextFromAssets(mContext, "help.txt");
		String content = mContext.get().getResources()
				.getString(R.string.readme_text);
		String textColor = getTextHtmlColor();
		String wordColor = getWordHtmlColor();
		String font = mSharedPrefs.getString(Def.PREF_KEY_FONT,
				Def.DEFAULT_FONT);
		String head = "<head><style>@font-face {font-family:'Unicode';src:url('file:///android_asset/fonts/"
				+ font + "');}";
		head += "@font-face {font-family:'KPhonetic';src:url('file:///android_asset/fonts/KPhonetic.ttf');}";
		head += " body,font{font-family:'Unicode';} i,b{font-family: sans-serif;}</style></head>";

		String html = "<html>" + head + "<body style='color:" + textColor
				+ "'>" + content + "</body></html>";

		html = html.replace("color:#TOBEREPLACE;", "color:" + wordColor + ";");

		return html;
	}

	public void showHtmlByResId(int resId, WebView webView) {
		showHtmlContent(mContext.get().getResources().getString(resId), webView);
	}

	public void showHtmlContent(String content, WebView webView) {
		if (webView != null) {
			if (content.indexOf("<body style='color:") < 0) {
				content = "<body style='color:" + getTextHtmlColor() + "'>"
						+ content + "</body>";
			}
			try {
				webView.loadDataWithBaseURL(null, content, Def.MIME_TYPE,
						Def.HTML_ENCODING, null);
			} catch (Exception ex) {
				Log.e("QDictions", ex.toString());
			}
			webView.scrollTo(0, 0);
		}
	}

	public String[] listWords(String word) {
		return mQDictEng.ListWords(word);
	}

	public String[] fuzzyListWords(String word) {
		return mQDictEng.FuzzyListWords(word);
	}

	public String[] patternListWords(String word) {
		return mQDictEng.PatternListWords(word);
	}

	public String[] fullTextListWords(String word) {
		return mQDictEng.FullTextListWords(word);
	}

	public String getBookName(String ifoPath) {
		return mQDictEng.GetBookName(ifoPath);
	}

	public String[] getDictInfo(String ifoPath) {
		return mQDictEng.GetInfo(ifoPath);
	}

	public void cancelLookup() {
		mQDictEng.CancelLookup();
	}

	public void destroy() {
		mQDictEng.releaseQDictEng();
	}
}
