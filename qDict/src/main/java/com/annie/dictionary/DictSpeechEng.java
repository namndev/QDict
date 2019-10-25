package com.annie.dictionary;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;

import java.util.Locale;

public class DictSpeechEng {
    private static DictSpeechEng mSpeechEng;
    private TextToSpeech mTts;
    private boolean mCanSpeak = false;

    private DictSpeechEng(Context context) {
        mTts = new TextToSpeech(context.getApplicationContext(), new TtsInitListener());
    }

    public static DictSpeechEng getInstance(Context context) {
        if (mSpeechEng == null) {
            mSpeechEng = new DictSpeechEng(context);
        }
        return mSpeechEng;
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
        if (null != text && mCanSpeak && mTts != null) {
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
            new Thread(() -> {
                if (status == TextToSpeech.SUCCESS && mTts.setLanguage(Locale.US) == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                    mCanSpeak = true;
                } else {
                    // Initialization failed.
                    mCanSpeak = false;
                }
            }).start();

        }
    }
}
