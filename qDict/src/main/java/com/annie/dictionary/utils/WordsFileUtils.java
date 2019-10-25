package com.annie.dictionary.utils;

import android.content.SharedPreferences;
import android.util.Log;

import com.annie.dictionary.utils.Utils.Def;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class WordsFileUtils {

    SharedPreferences mShares;
    int maxWords = Def.MAX_COUNT;
    private String mPath;
    private String mName = null;
    private boolean mChanged;
    private ArrayList<String> mWordsArrayList;

    public WordsFileUtils(SharedPreferences shares, int typeHis) {
        mPath = Utils.getRootDictFolder(shares) + "/" + Def.WORDSLIST_FOLDER;
        if (typeHis == Def.TYPE_FAVORITEWORDS) {
            mName = Def.FAVORITEWORDS_FILENAME;
            maxWords = shares.getInt("prefs_key_max_recent_word", 100);
        } else if (typeHis == Def.TYPE_RECENTWORDS) {
            mName = Def.RECENTWORDS_FILENAME;
        }
        mChanged = false;
        String[] wordsList;
        mShares = shares;
        // String emptySet = "";
        mWordsArrayList = new ArrayList<>();
        String data = read();
        if (null == data)
            return;
        wordsList = data.split(";");
        mWordsArrayList.addAll(Arrays.asList(wordsList));
    }

    public ArrayList<String> getArrayList() {
        return mWordsArrayList;
    }

    public String getBeforeWord(int pos) {
        if (pos < mWordsArrayList.size())
            return mWordsArrayList.get(pos);
        return "";
    }

    public int size() {
        if (mWordsArrayList.isEmpty())
            return 0;
        return mWordsArrayList.size();
    }

    public boolean canBackSearch(int index) {
        return (!(mWordsArrayList.isEmpty() || mWordsArrayList.size() <= 1) && (index < mWordsArrayList.size()));
    }

    public boolean contains(String word) {
        return mWordsArrayList.contains(word);
    }

    public void addWord(String word) {
        String newword = word.replace(";", "").toLowerCase(Locale.ENGLISH); // remove
        // ';'
        // if
        // it
        // exists
        // in
        // the word.
        if (newword.length() <= 0) {
            return;
        }

        remove(newword);
        mChanged = true;
        if (mWordsArrayList.size() > maxWords) {
            mWordsArrayList.remove(mWordsArrayList.size() - 1);
        }

        if (null != mWordsArrayList) {
            mWordsArrayList.add(0, newword);
        }
    }

    public boolean remove(String word) {
        String checkWord = word;
        for (String str : mWordsArrayList) {
            if (str.equalsIgnoreCase(word)) {
                checkWord = str;
                break;
            }
        }
        mChanged = mWordsArrayList.remove(checkWord);
        return mChanged;
    }

    public void removeAll() {
        mWordsArrayList.clear();
        mWordsArrayList = new ArrayList<>();
        mChanged = true;
    }

    public void save() {
        BufferedWriter writer = null;
        String data = "";
        int cnt = mWordsArrayList.size();
        String mDictIndexAll = mShares.getString(Def.PREF_INDEX_ALL, "");
        if (mDictIndexAll.isEmpty()) {
            return;
        }
        if (cnt < 0 || !mChanged) {
            return;
        }

        if (cnt > maxWords)
            cnt = maxWords;

        File folder = new File(mPath);
        if (!folder.exists()) {
            if (!folder.mkdirs())
                return;
        }

        for (int i = 0; i < cnt; i++) {
            data += mWordsArrayList.get(i) + ";";
        }
        try {
            File f = new File(mPath + mName);
            if (f.exists()) {
                if (!f.delete())
                    return;
            }
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)), 8192);
            writer.write(data);
            writer.flush();
        } catch (IOException e) {
            Log.e("WordsFileUtils", e.toString());
        } finally {
            try {
                if (writer != null)
                    writer.close();
            } catch (IOException e) {
                Log.e("WordsFileUtils", e.toString());
            }
        }
    }

    private String read() {
        BufferedReader reader = null;
        String data = null;

        try {
            File f = new File(mPath + mName);
            if (f.exists()) {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)), 8192);
                data = reader.readLine();
            }
        } catch (IOException e) {
            Log.e("WordsFileUtils", e.toString());
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (IOException e) {
                Log.e("WordsFileUtils", e.toString());
            }
        }
        return data;
    }
}
