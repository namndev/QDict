
package com.annie.dictionary;

public class QDictEng {
    private final String TAG = "QDictEng";

    public static String[] sDictPaths = null;

    public static String[] sDictNames = null;

    public static int[] sDictTypes = null;

    private static int mReferenct = 0;

    private static QDictEng mQDictEng = null;

    public QDictEng() {
    }

    public static QDictEng createQDictEng() {
        if (null == mQDictEng)
            mQDictEng = new QDictEng();
        mReferenct++;
        return mQDictEng;
    }

    public void releaseQDictEng() {
        mReferenct--;
        if (0 == mReferenct) {
            mQDictEng = null;
            UnloadDicts();
        }
    }

    // This function is called in JNI C code, it must be 'static' function.
    private static void lookupProgressCB(int progress) {
        MainActivity.lookupProgressCB(progress);
    }

    // -----------------------------------------------------------------------------------------------------//

    // Native function in QDictEng.c
    public native void CancelLookup();

    public native String[] Lookup(String word, int type); // This function for
                                                          // DICT_TYPE_INDEX,
                                                          // DICT_TYPE_CAPTURE
                                                          // and
                                                          // DICT_TYPE_MEMORIZE.

    public native String[] ListWords(String word); // This function is only for
                                                   // the type DICT_TYPE_INDEX.

    public native String[] FuzzyListWords(String word); // This function is only
                                                        // for the type
                                                        // DICT_TYPE_INDEX.

    public native String[] PatternListWords(String word); // This function is
                                                          // only for the type
                                                          // DICT_TYPE_INDEX.

    public native String[] FullTextListWords(String word); // This function is
                                                           // only for the type
                                                           // DICT_TYPE_INDEX.

    public native String GetBookName(String ifoPath);

    public native String[] GetInfo(String ifoPath);

    public native boolean LoadDicts(String[] paths, String[] names, int[] types);

    public native void UnloadDicts();

    static {
        System.loadLibrary("qdicteng");
    }
}
