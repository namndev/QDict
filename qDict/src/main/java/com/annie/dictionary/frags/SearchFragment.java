package com.annie.dictionary.frags;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.annie.dictionary.DictSpeechEng;
import com.annie.dictionary.DictWebViewClient;
import com.annie.dictionary.MainActivity;
import com.annie.dictionary.QDictions;
import com.annie.dictionary.R;
import com.annie.dictionary.utils.Utils;
import com.annie.dictionary.utils.Utils.Def;
import com.annie.dictionary.utils.Utils.NAVIG;
import com.annie.dictionary.utils.Utils.RECV_UI;
import com.annie.dictionary.utils.WebViewClientCallback;
import com.annie.dictionary.utils.WordsFileUtils;

public class SearchFragment extends Fragment {

    // UX
    private QDictions mDictions = null;

    private DictSpeechEng mSpeechEng = null;

    private String mKeyword = "";

    private WordsFileUtils mWordsFileUtilsHis, mWordsFileUtilsFavotire;

    private String mReadmeHtml = null;

    private boolean mIsSearch, mBackClick = false, mTts;

    private int mCurrentHisIndex = 0;

    // UI
    private WebView mDictContentView = null;

    private TextView mTvKeyword;

    private Menu mMenu;

    private MenuInflater mMenuInflater;

    private ImageButton mDictBackBtn, mSpeakBtn;

    public SearchFragment() {
        // default constructor
    }

    public static SearchFragment newInstance(DictSpeechEng dictSpeechEng, QDictions dictions, String keyword, boolean search) {
        SearchFragment s = new SearchFragment();
        s.mDictions = dictions;
        s.mSpeechEng = dictSpeechEng;
        s.mKeyword = keyword;
        s.mIsSearch = search;
        return s;
    }

    public void setDictions(QDictions dictions) {
        if (mDictions == null)
            mDictions = dictions;
    }

    public void setSpeechEng(DictSpeechEng dictSpeechEng) {
        if (mSpeechEng == null)
            mSpeechEng = dictSpeechEng;
    }

    public void setKeyword(String keyword, boolean search) {
        mIsSearch = search;
        mKeyword = keyword;
        mTvKeyword.setText(mKeyword);
        if (mMenu != null) {
            onCreateOptionsMenu(mMenu, mMenuInflater);
            getActivity().invalidateOptionsMenu();
        }
        showSearchContent();
    }

    public boolean isSearch() {
        return mIsSearch;
    }

    public String getKeyword() {
        return mKeyword;
    }

    public void setKeyword(String keyword) {
        setKeyword(keyword, true);
    }

    private void makeDictContent(String word) {
        mTvKeyword.setText(word);
        mSpeakBtn.setVisibility(mTts ? View.VISIBLE : View.GONE);
        String htmlContent = mDictions.generateHtmlContent(word);
        QDictions.showHtmlContent(htmlContent, mDictContentView);
        if (null != word && word.length() > 0) {
            if (!mBackClick)
                mWordsFileUtilsHis.addWord(word);
            if (mWordsFileUtilsHis.canBackSearch(mCurrentHisIndex + 1)) {
                mDictBackBtn.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showSearchContent() {
        if (mKeyword.length() <= 0) {
            QDictions.showHtmlContent(mReadmeHtml, mDictContentView);
            mDictBackBtn.setVisibility(View.INVISIBLE);
            mSpeakBtn.setVisibility(View.GONE);
        } else if (mKeyword.charAt(0) == '/') {
            QDictions.showHtmlContent(getResources().getString(R.string.fuzzy_query_prompt), mDictContentView);
        } else if (mKeyword.charAt(0) == ':') {
            QDictions.showHtmlContent(getResources().getString(R.string.fulltext_query_prompt), mDictContentView);
        } else if ((mKeyword.indexOf('*') >= 0) || (mKeyword.indexOf('?') >= 0)) {
            QDictions.showHtmlContent(getResources().getString(R.string.pattern_query_prompt), mDictContentView);
        } else {
            makeDictContent(mKeyword);
        }
        if (!mBackClick) {
            mCurrentHisIndex = 0;
        }
        mBackClick = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.layout_search, container, false);
        mDictBackBtn = root.findViewById(R.id.back_word);
        mTvKeyword = root.findViewById(R.id.tv_title);
        mSpeakBtn = root.findViewById(R.id.action_speak);
        mDictContentView = root.findViewById(R.id.dictContentView);
        DictWebViewClient webclient = new DictWebViewClient(getActivity().getApplicationContext(),
                new MyWebViewClientCallback());
        mDictContentView.setWebViewClient(webclient);
        WebSettings webSettings = mDictContentView.getSettings();
        webSettings.setLayoutAlgorithm(Utils.getLayoutAlgorithm(true));
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDefaultTextEncodingName("UTF-8");
        setHasOptionsMenu(true);

        // /
        mDictBackBtn.setOnClickListener(v -> {
            mCurrentHisIndex++;
            String text = mWordsFileUtilsHis.getBeforeWord(mCurrentHisIndex);
            if (!TextUtils.isEmpty(text)) {
                mBackClick = true;
                mKeyword = text;
                mTvKeyword.setText(mKeyword);
                if (mMenu != null) {
                    onCreateOptionsMenu(mMenu, mMenuInflater);
                    getActivity().invalidateOptionsMenu();
                }
                showSearchContent();
            }
            if (!mWordsFileUtilsHis.canBackSearch(mCurrentHisIndex + 1)) {
                mDictBackBtn.setVisibility(View.INVISIBLE);
            }
        });
        mSpeakBtn.setOnClickListener(v -> mSpeechEng.speak(mKeyword.trim()));
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        SharedPreferences mSharedPreferences = getActivity().getSharedPreferences(Def.APP_NAME, Context.MODE_PRIVATE);

        if (mDictions == null && MainActivity.Companion.getHasStoragePermission()) {
            mDictions = new QDictions(getActivity());
            mDictions.initDicts();
        }
        if (mSpeechEng == null) {
            mSpeechEng = DictSpeechEng.getInstance(getActivity().getApplicationContext());
        }
        mTts = mSharedPreferences.getBoolean(getResources().getString(R.string.prefs_key_using_tts), true)
                && mSpeechEng.isCanSpeak();
        mWordsFileUtilsHis = new WordsFileUtils(mSharedPreferences, Def.TYPE_RECENTWORDS);
        mWordsFileUtilsFavotire = new WordsFileUtils(mSharedPreferences, Def.TYPE_FAVORITEWORDS);
        mTvKeyword.setText(mKeyword);
        mReadmeHtml = QDictions.getReadmeHtml(getActivity(), mSharedPreferences);
        if (mIsSearch) {
            showSearchContent();
        } else {
            QDictions.showHtmlContent(mReadmeHtml, mDictContentView);
            mDictBackBtn.setVisibility(View.INVISIBLE);
            mSpeakBtn.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mCurrentHisIndex = 0;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.search_menu, menu);
        mMenu = menu;
        mMenuInflater = inflater;
        if (!mIsSearch) {
            menu.removeItem(R.id.action_favorite);
            menu.removeItem(R.id.action_recent);
        } else if (mWordsFileUtilsFavotire.contains(mKeyword)) {
            menu.findItem(R.id.action_favorite).setIcon(R.drawable.unfavorite);
        } else {
            menu.findItem(R.id.action_favorite).setIcon(Utils.getDrawable(getActivity(), R.attr.icon_actionFavorite));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_favorite) {
            if (!mWordsFileUtilsFavotire.contains(mKeyword)) {
                mWordsFileUtilsFavotire.addWord(mKeyword);
                item.setIcon(R.drawable.unfavorite);
                return true;
            } else {
                if (mWordsFileUtilsFavotire.remove(mKeyword)) {
                    item.setIcon(Utils.getDrawable(getActivity(), R.attr.icon_actionFavorite));
                }
                return true;
            }
        } else if (id == R.id.action_recent) {
            Intent intent = new Intent(MainActivity.ACTION_UPDATE_UI);
            intent.putExtra(MainActivity.ACTION_UPDATE_KEY, RECV_UI.CHANGE_FRAG);
            intent.putExtra("receiver_frag_position", NAVIG.RECENT);
            getActivity().sendBroadcast(intent);

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        mWordsFileUtilsFavotire.save();
        mWordsFileUtilsHis.save();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        mWordsFileUtilsFavotire.save();
        mWordsFileUtilsFavotire = null;
        mWordsFileUtilsHis.save();
        mWordsFileUtilsHis = null;
        super.onDestroyView();
    }

    public class MyWebViewClientCallback extends WebViewClientCallback {
        public MyWebViewClientCallback() {
        }

        @Override
        public void shouldOverrideUrlLoading(String word) {
            makeDictContent(word);
        }
    }
}
