package com.annie.dictionary.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.webkit.WebSettings;

import com.annie.dictionary.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

public class Utils {
    public static final int ORANGE_500 = Color.parseColor("#ff9800");
    private static final String TAG = "Utils";
    private static final String MADMAN_FB_ID = "419249474927176";
    private static String[] LANGUAGE_SUPPORTS = new String[]{
            "en", "es", "vi", "zh"
    };

    private Utils() {
    }

    public static Typeface getFont(Context context, String value) {
        return Typeface.createFromAsset(context.getAssets(), "fonts/" + value);
    }

    // -----------------------------------------------------------------------------------------------------//
    // Public static class
    public static String getSDCardPath() {
        boolean SDCardExist = Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
        if (SDCardExist) {
            File SDCardFolder = Environment.getExternalStorageDirectory();
            File parent = SDCardFolder.getParentFile();
            if (parent.canRead())
                return SDCardFolder.getParent();
            else
                return SDCardFolder.toString();
        }
        return null;
    }

    public static String getRootDictFolder(SharedPreferences shares) {
        String root = shares.getString(Def.PREF_DATA_SOURCE, "");
        File rootFile = new File(root);
        if (TextUtils.isEmpty(root) || !rootFile.canRead()) {
            boolean SDCardExist = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
            if (SDCardExist) {
                File SDCardFolder = Environment.getExternalStorageDirectory();
                root = SDCardFolder.toString() + "/" + Def.APP_NAME;
                shares.edit().putString(Def.PREF_DATA_SOURCE, root).apply();
                return root;
            } else {
                return null;
            }
        } else {
            return root;
        }
    }

    public static void setRootDictFolder(SharedPreferences shares, String pathRootDict) {
        shares.edit().putString(Def.PREF_DATA_SOURCE, pathRootDict).apply();
    }

    public static String getFileInfoName(String dictFolderPath) {
        String dictName = null;
        File f = new File(dictFolderPath);
        if (!f.exists())
            return null;
        File[] files = f.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                if (file.getName().endsWith(".ifo")) {
                    dictName = file.getName().replace(".ifo", "");
                    break;
                }
            }
        }
        return dictName;
    }

    public static Dialog createAboutDialog(final Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context, R.style.QDialog);
        builder.setView(inflater.inflate(R.layout.about, null));
        builder.setTitle(R.string.about_lable);
        builder.setNeutralButton(R.string.btn_more_apps, (dialog, which) -> context.startActivity(goToMoreApp()));
        builder.setPositiveButton(R.string.ok, null);
        return builder.create();
    }

    public static Intent goToMoreApp() {
        String url = "madman+team";
        return new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/developer?id=" + url));
    }

    public static Dialog createWhatsNewDialog(Context context) {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context, R.style.QDialog);
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setTitle(R.string.prefs_title_whatsnew);
        builder.setMessage(getTextFromAssets(context, "whatsnew.txt"));
        builder.setNeutralButton(R.string.ok, (dialog, which) -> dialog.cancel());
        return builder.create();

    }

    public static CharSequence getTextFromAssets(Context context, String name) {
        BufferedReader in = null;
        StringBuilder sb = new StringBuilder();
        try {
            in = new BufferedReader(new InputStreamReader(context.getAssets().open(name)));
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb;
    }

    // SDcard
    public static boolean isSdCardWrittenable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static Intent getOpenPageFBIntent(Context context) {
        Intent intent;
        try {
            context.getPackageManager().getPackageInfo("com.facebook.katana", 0);
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("fb://page/" + MADMAN_FB_ID));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } catch (Exception e) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/madman.hust" + MADMAN_FB_ID));
        }
        return intent;
    }

    /**
     * share data to any
     *
     * @param clz
     */
    public static Intent getIntentShareData(Class<?> clz) {
        Intent sendIntent = new Intent();
        String url = clz.getPackage().getName();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT,
                "Hey check out my app at: https://play.google.com/store/apps/details?id=" + url);
        sendIntent.setType("text/plain");
        return sendIntent;
    }

    /**
     * madman: move to app in play store
     */

    public static Intent goToFTPServer() {
        String appId = "com.m2t.ftpserver";
        return new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appId));
    }

    public static Intent goToFTPServerLink() {
        String appId = "com.m2t.ftpserver";
        return new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appId));
    }

    public static Drawable getDrawable(Activity activity, int attrResId) {
        int[] attrs = {
                attrResId
        };
        TypedArray ta = activity.getTheme().obtainStyledAttributes(attrs);
        return ta.getDrawable(0);
    }

    public static int getColor(Context activity, int attrResId) {
        int[] attrs = {
                attrResId
        };
        TypedArray ta = activity.getTheme().obtainStyledAttributes(attrs);
        return ta.getColor(0, ORANGE_500);
    }

    public static int getDimens(Context activity, int attrResId) {
        int[] attrs = {
                attrResId
        };
        TypedArray ta = activity.getTheme().obtainStyledAttributes(attrs);
        return ta.getDimensionPixelOffset(0, 0);
    }

    public static void changeToTheme(Activity activity) {
        activity.finish();
        activity.startActivity(new Intent(activity, activity.getClass()));
    }

    public static void onActivityCreateSetTheme(Activity activity, int themeIndex, int themeActivity) {
        switch (themeIndex) {
            case 1:
                if (themeActivity == ThemeActivity.HOME)
                    activity.setTheme(R.style.AppBlueLightTheme);
                else if (themeActivity == ThemeActivity.SETTING)
                    activity.setTheme(R.style.AppBlueLightThemeWithActionBar);
                else if (themeActivity == ThemeActivity.DIALOG)
                    activity.setTheme(R.style.QDialog_Blue);
                break;
            default:
                if (themeActivity == ThemeActivity.HOME)
                    activity.setTheme(R.style.AppOrangeTheme);
                else if (themeActivity == ThemeActivity.SETTING)
                    activity.setTheme(R.style.AppOrangeThemeWithActionBar);
                else if (themeActivity == ThemeActivity.DIALOG)
                    activity.setTheme(R.style.QDialog_Orange);
                break;
        }
    }

    public static WebSettings.LayoutAlgorithm getLayoutAlgorithm(boolean isSingColumn) {
        if (!isSingColumn) {
            if (hasKkAbove()) {
                return WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING;
            } else {
                return WebSettings.LayoutAlgorithm.NARROW_COLUMNS;
            }
        }
        return WebSettings.LayoutAlgorithm.NORMAL;
    }

    // For Android 6
    public static boolean verifyAllPermissions(int[] permissions) {
        for (int result : permissions) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasSelfPermission(Context activity, String[] permissions) {
        if (!hasMmAbove()) {
            return true;
        }
        // Verify that all the permissions.
        for (String permission : permissions) {
            if (activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Uses static final constants to detect if the device's platform version is
     * Marshmallow or later.
     */
    public static boolean hasMmAbove() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    /**
     * Uses static final constants to detect if the device's platform version is
     * Lollipop or later.
     */
    public static boolean hasLlAbove() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    /**
     * Uses static final constants to detect if the device's platform version is
     * Kitkat or later.
     */
    public static boolean hasKkAbove() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    /**
     * Uses static final constants to detect if the device's platform version is
     * Kikat.
     */
    public static boolean hasKk() {
        int v = Build.VERSION.SDK_INT;
        return v >= Build.VERSION_CODES.KITKAT && v < Build.VERSION_CODES.LOLLIPOP;
    }

    public static void changeLocale(Resources resources, String language) {
        changeLocale(resources, language, null);
    }

    public static void changeLocale(Resources resources, String language, String country) {
        Locale locale;
        if (TextUtils.isEmpty(country)) {
            locale = new Locale(language);
        } else {
            locale = new Locale(language, country);
        }
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    public static boolean checkLanguageSupport(String language) {
        for (String lang : LANGUAGE_SUPPORTS) {
            if (language.contains(lang))
                return true;
        }
        return false;
    }

    /**
     * const::control navigation
     *
     * @author madman
     */
    public interface NAVIG {
        int HOME = 0;
        int RECENT = 1;
        int FAVORITE = 2;
        int SELECT_DICT = 3;
        int SETTINGS = 4;
        int JOIN_US = 5;
        int SEARCH = 6;

    }

    /**
     * const::control receiver values
     *
     * @author madman
     */
    public interface RECV_UI {
        int CHANGE_THEME = 1001;
        int SELECT_DICT = 1003;
        int SEARCH_WORD = 1005;
        int RELOAD_DICT = 1007;
        int RUN_SERVICE = 1009;
        int CHANGE_FONT = 1011;
        int CHANGE_FRAG = 1013;
    }

    /**
     * const::dialog Id
     *
     * @author madman
     */
    public interface DIALOG {
        int ABOUT = 1011;
        int CHANGE_LOG = 1013;
    }

    /**
     * const:: qdict
     *
     * @author madman
     */
    public interface Def {

        String APP_NAME = "QDict";
        // key preference
        String PREF_DATA_SOURCE = "prefs_key_source";
        String PREF_INDEX_CHECKED = "prefs_key_index_checked";
        String PREF_INDEX_ALL = "prefs_key_index_all";
        String PREF_KEY_FONT = "prefs_key_font_text";
        String DICT_FOLDER = "/dicts";
        // max count -- max item in wordlist history
        int MAX_COUNT = 99;
        // limit character using google translate free
        int LIMIT_TRANSLATE_CHAR = 256;
        // word list type
        int TYPE_RECENTWORDS = 101;
        int TYPE_FAVORITEWORDS = 103;
        String WORDSLIST_FOLDER = "hiswords/";
        String FAVORITEWORDS_FILENAME = "favoritewords.qdc";
        String RECENTWORDS_FILENAME = "recentwords.qdc";
        // MIME
        String MIME_TYPE = "text/html";
        String HTML_ENCODING = "UTF-8";
        String BWORD_URL = "bword://";
        String HTTP_URL = "http://";
        String HTTPS_URL = "https://";
        // for service
        int CLIPBOARD_TIMER = 1500;
        String DEFAULT_TEXT_COLOR = "#FF000000";
        String DEFAULT_WORD_COLOR = "#FF002DFF";
        String DEFAULT_FONT = "Roboto.ttf";
    }

    public interface ThemeActivity {
        int HOME = 0;
        int SETTING = 1;
        int DIALOG = 2;
    }
}
