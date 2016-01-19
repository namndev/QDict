
package com.annie.dictionary;

import java.util.Locale;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;

public class DictSpeechEng {
    private TextToSpeech mTts = null;

    private boolean mCanSpeak = false;

    private static DictSpeechEng mSpeechEng;

    public static DictSpeechEng getInstance(Context context) {
        if (mSpeechEng == null) {
            mSpeechEng = new DictSpeechEng(context);
        }
        return mSpeechEng;
    }

    private DictSpeechEng(Context context) {
        mTts = new TextToSpeech(context.getApplicationContext(), new TtsInitListener());
    }

    public void destroy() {
        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
            mTts = null;
        }
        mSpeechEng = null;
    }

    public void setLocale(String language) {
        if (isCanSpeak() && (mTts != null)) {
            if (TextUtils.isEmpty(language))
                language = "en_US";
            Locale locale = new Locale(language);
            int result = mTts.setLanguage(locale);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                mTts.setLanguage(new Locale(language));
            }
        }
    }

    @SuppressWarnings("deprecation")
    public int speak(String text) {
        if (null != text && true == mCanSpeak && mTts != null) {
            mTts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            return text.length();
        }
        return -1;
    }

    public boolean isCanSpeak() {
        return mCanSpeak;
    }

    class TtsInitListener implements TextToSpeech.OnInitListener {
        @Override
        public void onInit(final int status) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (status == TextToSpeech.SUCCESS) {
                        int result = mTts.setLanguage(Locale.US);
                        if (result == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                            mCanSpeak = true;
                        } else {
                            mCanSpeak = false;
                        }
                    } else {
                        // Initialization failed.
                        mCanSpeak = false;
                    }
                }
            }).start();

        }
    }
}
