package com.annie.dictionary;

public class QDictEng {

    public static String[] sDictPaths = null;

    public static String[] sDictNames = null;

    public static int[] sDictTypes = null;

    private static int mReferenct = 0;

    private static QDictEng mQDictEng = null;

    static {
        System.loadLibrary("qdicteng");
    }

    public QDictEng() {
    }

    public static QDictEng createQDictEng() {
        if (null == mQDictEng)
            mQDictEng = new QDictEng();
        mReferenct++;
        return mQDictEng;
    }

    // This function is called in JNI C code, it must be 'static' function.
    private static void lookupProgressCB(int progress) {
        MainActivity.Companion.lookupProgressCB(progress);
    }

    // -----------------------------------------------------------------------------------------------------//

    public void releaseQDictEng() {
        mReferenct--;
        if (0 == mReferenct) {
            mQDictEng = null;
            UnloadDicts();
        }
    }

    // Native function in QDictEng.c
    public native void CancelLookup();
    // DICT_TYPE_INDEX,
    // DICT_TYPE_CAPTURE
    // and
    // DICT_TYPE_MEMORIZE.

    public native String[] Lookup(String word, int type); // This function for
    // the type DICT_TYPE_INDEX.

    public native String[] ListWords(String word); // This function is only for
    // for the type
    // DICT_TYPE_INDEX.

    public native String[] FuzzyListWords(String word); // This function is only
    // only for the type
    // DICT_TYPE_INDEX.

    public native String[] PatternListWords(String word); // This function is
    // only for the type
    // DICT_TYPE_INDEX.

    public native String[] FullTextListWords(String word); // This function is

    public native String GetBookName(String ifoPath);

    public native boolean LoadDicts(String[] paths, String[] names, int[] types);

    public native void UnloadDicts();
}
