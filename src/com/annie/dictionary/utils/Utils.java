
package com.annie.dictionary.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

import com.annie.dictionary.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.view.LayoutInflater;
import android.webkit.WebSettings;

public class Utils {
    private static final String TAG = "Utils";

    /**
     * const::control navigation
     * 
     * @author madman
     */
    public interface NAVIG {
        public static final int HOME = 0;

        public static final int RECENT = 1;

        public static final int FAVORITE = 2;

        public static final int SELECT_DICT = 3;

        public static final int SETTINGS = 4;

        public static final int JOIN_US = 5;

        public static final int SEARCH = 6;

    }

    /**
     * const::control receiver values
     * 
     * @author madman
     */
    public interface RECV_UI {
        public static final int CHANGE_THEME = 1001;

        public static final int SELECT_DICT = 1003;

        public static final int SEARCH_WORD = 1005;

        public static final int RELOAD_DICT = 1007;

        public static final int RUN_SERVICE = 1009;

        public static final int CHANGE_FONT = 1011;
    }

    /**
     * const::dialog Id
     * 
     * @author madman
     */
    public interface DIALOG {
        public static final int ABOUT = 1011;

        public static final int CHANGE_LOG = 1013;

        public static final int RATE = 1015;
    }

    /**
     * const:: qdict
     * 
     * @author madman
     */
    public interface Def {

        public static final String APP_NAME = "QDict";

        // key preference
        public static final String PREF_INDEX_CHECKED = "prefs_key_index_checked";

        public static final String PREF_INDEX_ALL = "prefs_key_index_all";

        public static final String PREF_KEY_FONT = "prefs_key_font_text";

        public static final String PREF_KEY_COLOR_TEXT = "prefs_key_color_text";

        public static final String PREF_KEY_COLOR_WORD = "prefs_key_color_word";

        public static final String PREF_KEY_COLOR_BACKGROUND = "prefs_key_color_background";

        public static final String DICT_FOLDER = "/dicts";

        // max count -- max item in wordlist history
        public final int MAX_COUNT = 99;

        // max speak char == max characters when text-to-speech
        public final int MAX_SPEAK_CHAR = 100;

        // limit character using google translate free
        public final int LIMIT_TRANSLATE_CHAR = 256;

        // word list type
        public static final int TYPE_RECENTWORDS = 101;

        public static final int TYPE_FAVORITEWORDS = 103;

        public final static String WORDSLIST_FOLDER = "hiswords/";

        public final static String FAVORITEWORDS_FILENAME = "favoritewords.qdc";

        public final static String RECENTWORDS_FILENAME = "recentwords.qdc";

        // mime
        public static final String MIME_TYPE = "text/html";

        public static final String HTML_ENCODING = "UTF-8";

        public static final String BWORD_URL = "bword://";

        public static final String HTTP_URL = "http://";

        public static final String HTTPS_URL = "https://";

        // for service
        public static final int CLIPBOARD_TIMER = 1500;

        public static final String DEFAULT_TEXT_COLOR = "#FF000000";

        public static final String DEFAULT_WORD_COLOR = "#FF002DFF";

        public static final String DEFAULT_FONT = "Roboto.ttf";
    }

    private static final long LOW_STORAGE_THRESHOLD = 1024 * 1024 * 10;

    public static final int ORANGE_500 = Color.parseColor("#ff9800");

    private Utils() {
    }

    public static Typeface getFont(Context context, String value) {
        return Typeface.createFromAsset(context.getAssets(), "fonts/" + value);
    }

    // -----------------------------------------------------------------------------------------------------//
    // Public static class

    public static String getRootFolder() {
        File SDCardFolder = null;
        boolean SDCardExist = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
        if (SDCardExist) {
            SDCardFolder = Environment.getExternalStorageDirectory();
            String root = SDCardFolder.toString() + "/" + Def.APP_NAME;
            return root;
        }
        return null;
    }

    public static String getFileInfoName(String dictFolderPath) {
        String dictName = null;
        File f = new File(dictFolderPath);
        if (!f.exists())
            return null;
        File[] files = f.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                if (files[i].getName().endsWith(".ifo")) {
                    dictName = files[i].getName().replace(".ifo", "");
                    break;
                }
            }
        }
        return dictName;
    }

    public static double roundDouble(double value, int scale, int roundingMode) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(scale, roundingMode);
        double d = bd.doubleValue();
        bd = null;
        return d;
    }

    public static int argbToColor(String color) {
        return Color.parseColor(color);
    }

    public static String colorToARGB(int color) {
        String alpha = Integer.toHexString(Color.alpha(color));
        String red = Integer.toHexString(Color.red(color));
        String green = Integer.toHexString(Color.green(color));
        String blue = Integer.toHexString(Color.blue(color));

        if (alpha.length() == 1) {
            alpha = "0" + alpha;
        }
        if (red.length() == 1) {
            red = "0" + red;
        }
        if (green.length() == 1) {
            green = "0" + green;
        }
        if (blue.length() == 1) {
            blue = "0" + blue;
        }
        return "#" + alpha + red + green + blue;
    }

    public static Dialog createAboutDialog(final Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.QDialog);
        builder.setView(inflater.inflate(R.layout.about, null));
        builder.setTitle(R.string.about_lable);
        builder.setNeutralButton(R.string.btn_more_apps, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                context.startActivity(goToMoreApp());
            }
        });
        builder.setPositiveButton(R.string.ok, null);
        return builder.create();
    }

    public static Intent goToMoreApp() {
        String url = "madman+team";
        return new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/developer?id=" + url));
    }

    public static Dialog createWhatsNewDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.QDialog);
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setTitle(R.string.prefs_title_whatsnew);
        builder.setMessage(getTextFromAssets(context, "whatsnew.txt"));
        builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        return builder.create();

    }

    public static String getFileNameFromUrl(String url) {
        int index = url.lastIndexOf('?');
        String filename;
        if (index > 1) {
            filename = url.substring(url.lastIndexOf('/') + 1, index);
        } else {
            filename = url.substring(url.lastIndexOf('/') + 1);
        }

        if (filename == null || "".equals(filename.trim())) {
            filename = UUID.randomUUID() + ".apk";//
        }
        return filename;
    }

    public static String getLocaleName(Context context, String name) {
        String localeSuffix = getLocaleSuffix(context);
        if (localeSuffix == null)
            return name;
        StringBuilder sb = new StringBuilder(name);
        int lastDotIdx = name.lastIndexOf('.');
        if (lastDotIdx > -1) {
            sb.insert(lastDotIdx, localeSuffix);
        } else {
            sb.append(localeSuffix);
        }
        return sb.toString();

    }

    /**
     * read txt
     * 
     * @param context
     * @return null if current locale is not in [vi_VN, ja_JP]
     */
    private static String getLocaleSuffix(Context context) {
        Locale locale = context.getResources().getConfiguration().locale;
        String localeSuffix = null;
        if (locale.equals(new Locale("vi", "VN"))) {
            localeSuffix = "_vi_VN";
        } else if (locale.equals(Locale.JAPAN)) {
            localeSuffix = "_ja_JP";
        }
        return localeSuffix;
    }

    public static CharSequence getTextFromAssets(Context context, String name) {
        String filename = name;// getLocaleName(context, name);
        BufferedReader in = null;
        StringBuilder sb = new StringBuilder();
        try {
            in = new BufferedReader(new InputStreamReader(context.getAssets().open(filename)));
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
        } catch (IOException e) {
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
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    public static long getAvailableStorage() {

        String storageDirectory = null;
        storageDirectory = Environment.getExternalStorageDirectory().toString();

        try {
            StatFs stat = new StatFs(storageDirectory);
            long avaliableSize = ((long)stat.getAvailableBlocks() * (long)stat.getBlockSize());
            Log.d(TAG, "getAvailableStorage. avaliableSize : " + avaliableSize);
            return avaliableSize;
        } catch (RuntimeException ex) {
            Log.e(TAG, "getAvailableStorage - exception. return 0");
            return 0;
        }
    }

    public static boolean checkAvailableStorage() {
        Log.d(TAG, "checkAvailableStorage E");

        if (getAvailableStorage() < LOW_STORAGE_THRESHOLD) {
            return false;
        }

        return true;
    }

    public static boolean isSDCardPresent() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static Bitmap getLoacalBitmap(String url) {
        try {
            FileInputStream fis = new FileInputStream(url);
            return BitmapFactory.decodeStream(fis);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static final String MADMAN_FB_ID = "419249474927176";

    public static Intent getOpenPageFBIntent(Context context) {
        Intent intent = null;
        try {
            context.getPackageManager().getPackageInfo("com.facebook.katana", 0);
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("fb://page/" + MADMAN_FB_ID));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } catch (Exception e) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/madman.hust" + MADMAN_FB_ID));
        }
        return intent;
    }

    // public static Intent getTwitterIntent(Context context) {
    // Intent intent = null;
    // try {
    // // get the Twitter app if possible
    // context.getPackageManager()
    // .getPackageInfo("com.twitter.android", 0);
    // intent = new Intent(Intent.ACTION_VIEW,
    // Uri.parse("twitter://user?user_id=" + STAVIRA_TWITTER_ID));
    // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    // } catch (Exception e) {
    // // no Twitter app, revert to browser
    // intent = new Intent(Intent.ACTION_VIEW,
    // Uri.parse("https://twitter.com/staviravn"));
    // }
    // return intent;
    // }

    /**
     * share data to any
     * 
     * @param context
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

    public static Intent sendMailIntent(Context context, String sub) {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO); // it's not
                                                              // ACTION_SEND
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, sub);
            intent.putExtra(Intent.EXTRA_TEXT, "Your message here");
            intent.setData(Uri.parse("mailto:" + context.getString(R.string.app_email))); // or
                                                                                          // just
            // "mailto:"
            // for blank
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            return intent;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * madman: move to app in play store
     */

    public static Intent goToPlayStore(Class<?> clz) {
        String url = clz.getPackage().getName();
        return new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + url));
    }

    public static Intent goToFTPServer() {
        String appId = "com.m2t.ftpserver";
        return new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appId));
    }

    public static Intent goToFTPServerLink() {
        String appId = "com.m2t.ftpserver";
        return new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appId));
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

    public static void onActivityCreateSetTheme(Activity activity, int themeIndex, boolean home) {
        switch (themeIndex) {
            case 1:
                activity.setTheme(home ? R.style.AppBlueLightTheme : R.style.AppBlueLightThemeWithActionBar);
                break;
            default:
                activity.setTheme(home ? R.style.AppOrangeTheme : R.style.AppOrangeThemeWithActionBar);
                break;
        }
    }

    public static void onServiceSetTheme(Service service, int themeIndex) {
        switch (themeIndex) {
            case 1:
                service.setTheme(R.style.AppBlueLightTheme);
                break;
            default:
                service.setTheme(R.style.AppOrangeTheme);
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

}
