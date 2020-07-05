package com.annie.dictionary

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.*
import android.content.ClipboardManager.OnPrimaryClipChangedListener
import android.os.*
import android.speech.RecognizerIntent
import android.text.Selection
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.View.OnClickListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.annie.dictionary.frags.ListDictFragment
import com.annie.dictionary.frags.NavigatorFragment
import com.annie.dictionary.frags.RecentFragment
import com.annie.dictionary.frags.SearchFragment
import com.annie.dictionary.service.QDictService
import com.annie.dictionary.standout.StandOutWindow
import com.annie.dictionary.utils.Utils.*
import com.mmt.widget.M2tToast
import com.mmt.widget.SlidingUpPanelLayout.PanelState
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_frame.*
import kotlinx.android.synthetic.main.layout_drag.*
import java.util.*

class MainActivity : BaseActivity(), NavigatorFragment.NavigationCallbacks, OnClickListener {

    private var navFragment: Fragment? = null
    // UX
    var mSpeechEng: DictSpeechEng? = null
    var mDictions: QDictions? = null
    var mCurrentNavPosition = -1
    var tempKeyword: String = ""
    var tempPos: Int = 0
    var onNavig = false
    // dict
    private var mDictKeywordView: DictEditTextView? = null
    private var mProgressDialog: ProgressDialog? = null
    private var mPopupWordsListHandler: Handler? = null
    // keyboard handler
    private var mShowKeyboardHander: Handler? = null
    private var mShowKeyboarRunable: Runnable? = null

    private var mReplaceKeyword = false
    private var mIsTaskRunning = false
    private var mClipboardManager: ClipboardManager? = null
    private var mClipboardText = ""

    private var mClipboardListener: OnPrimaryClipChangedListener? = null

    private var mPopupWordsListRunnable: Runnable = Runnable { this.startKeywordsList() }

    private var mUIReceiver: BroadcastReceiver? = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getIntExtra(ACTION_UPDATE_KEY, -1)
            if (id == RECV_UI.SELECT_DICT) {
                setMainMenuFragment()
            } else if (id == RECV_UI.CHANGE_THEME || id == RECV_UI.CHANGE_FONT) {
                stopService()
                changeToTheme(this@MainActivity)
            } else if (id == RECV_UI.SEARCH_WORD) {
                val keyword = intent.getStringExtra("receiver_keyword")
                if (!TextUtils.isEmpty(keyword)) {
                    mDictKeywordView?.setText(keyword)
                    showSearchContent()
                }
            } else if (id == RECV_UI.RELOAD_DICT) {
                mDictions!!.initDicts()
            } else if (id == RECV_UI.RUN_SERVICE) {
                startService()
            } else if (id == RECV_UI.CHANGE_FRAG) {
                val pos = intent.getIntExtra("receiver_frag_position", NAVIG.RECENT)
                if (mCurrentNavPosition == NAVIG.SEARCH) {
                    setFragment("", pos)
                }
                mCurrentNavPosition = pos
            }
        }
    }


    fun setMainMenuFragment() {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frame_container, NavigatorFragment())
        transaction.commit()
    }

    fun getToolbar(): Toolbar {
        return main_toolbar
    }

    private fun initClipboard() {
        if (mClipboardManager == null) {
            mClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            mClipboardManager!!.addPrimaryClipChangedListener(mClipboardListener)
        }
    }

    private fun releaseClipboard() {
        mClipboardManager?.removePrimaryClipChangedListener(mClipboardListener)
        mClipboardManager = null
    }

    private fun clipboardCheck() {
        var clipboardText: String
        var s: CharSequence? = null
        mClipboardManager?.let {
            if (it.hasPrimaryClip()) {
                s = it.primaryClip?.getItemAt(0)?.text
            }
        }

        if (TextUtils.isEmpty(s)) {
            return
        }
        clipboardText = s.toString().trim { it <= ' ' }
        if (clipboardText.length > Def.LIMIT_TRANSLATE_CHAR)
            clipboardText = clipboardText.substring(0, Def.LIMIT_TRANSLATE_CHAR)
        if (mClipboardText.equals(clipboardText, ignoreCase = true))
            return
        if (clipboardText.isNotEmpty()) {
            mClipboardText = clipboardText
            mDictKeywordView?.setText(mClipboardText)
            showSearchContent()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // UX:
        mSpeechEng = DictSpeechEng.getInstance(this)
        mSpeechEng?.setLocale("en_US")
        checkPermission(REQUEST_STORAGE_CODE)
        // UI: set the Above View
        setContentView(R.layout.activity_main)
        main_toolbar.title = null
        setSupportActionBar(main_toolbar)
        supportActionBar?.let {
            it.setDefaultDisplayHomeAsUpEnabled(true)
            it.setHomeButtonEnabled(true)
        }

        sliding_layout.panelState = PanelState.HIDDEN
        sliding_layout.isTouchEnabled = false
        val toggle = ActionBarDrawerToggle(this, drawer_layout, main_toolbar, R.string.ok, R.string.close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        toggle.drawerArrowDrawable.color = getColor(this, R.attr.colorPrimary)
        // layout_drag
        action_menu.setOnClickListener(this)
        action_voice.setOnClickListener(this)
        action_wordslist.setOnClickListener(this)
        mDictKeywordView = DictEditTextView(this)
        layout_input.addView(mDictKeywordView, 0,
                LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        initDropList()

        var keyword: String? = null
        if (savedInstanceState == null) {
            navFragment = NavigatorFragment()
            navFragment?.let {
                supportFragmentManager.beginTransaction().replace(R.id.frame_container, it).commit()
            }
        } else {
            navFragment = supportFragmentManager.findFragmentById(R.id.frame_container)
            mCurrentNavPosition = savedInstanceState.getInt("position_fragment")
            keyword = savedInstanceState.getString("search_fragment_keyword", "")
        }

        val isFirst = mSharedPreferences?.getBoolean("qdict_firt_start", true) ?: true
        if (isFirst) {
            setFragment(getString(R.string.guide_lable), NAVIG.HOME)
            mSharedPreferences?.edit()?.putBoolean("qdict_firt_start", false)?.apply()
        } else {
            val isSearch = !TextUtils.isEmpty(keyword)
            val title = if (mCurrentNavPosition == NAVIG.HOME || mCurrentNavPosition == NAVIG.SEARCH)
                if (isSearch) keyword else resources.getString(R.string.guide_lable)
            else
                ""
            setFragment(title
                    ?: "", if (mCurrentNavPosition != -1) mCurrentNavPosition else NAVIG.RECENT)
        }
        mShowKeyboardHander = Handler()
        mProgressCBHandler = @SuppressLint("HandlerLeak")
        object : Handler() {
            override fun handleMessage(msg: Message) {
                val progress = msg.arg1
                mProgressDialog?.let {
                    it.progress = progress
                }
            }
        }

        mShowKeyboarRunable = Runnable {
            mDictKeywordView?.requestFocus()
            mDictKeywordView?.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                    SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0f, 0f, 0))
            mDictKeywordView?.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                    SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0f, 0f, 0))
        }
        mClipboardListener = OnPrimaryClipChangedListener { this.clipboardCheck() }

        startService()
        registerReceiver(mUIReceiver, IntentFilter(ACTION_UPDATE_UI))
    }

    override fun onRequestPermissionResult(requestCode: Int, isSucess: Boolean) {
        if (REQUEST_STORAGE_CODE == requestCode) {
            hasStoragePermission = isSucess
            if (isSucess) {
                mDictions = QDictions(this)
                mDictions?.initDicts()
            } else {
                finish()
            }
        } else if (BaseActivity.Companion.REQUEST_ALERT_WINDOW_CODE == requestCode) {
            val i = Intent(Intent.ACTION_RUN)
            i.setClass(this@MainActivity, QDictService::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (!QDictService.RUNNING)
                startService(i)
            if (!isSucess) {
                runOnUiThread { Toast.makeText(this@MainActivity, R.string.msg_do_not_show_popup, Toast.LENGTH_SHORT).show() }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("position_fragment", mCurrentNavPosition)
        if (mCurrentNavPosition == NAVIG.SEARCH) {
            val fragment = this.supportFragmentManager.findFragmentById(R.id.content_frame)
            if (fragment is SearchFragment) {
                val fg = fragment as SearchFragment?
                if (fg?.isSearch == true) {
                    outState.putString("search_fragment_keyword", fg.keyword)
                }
            }
        }
    }

    private fun initDropList() {
        drop_list.isFocusable = true
        drop_list.isFocusableInTouchMode = true
        drop_list.isListSelectionHidden = false
        drop_list.setOnItemClickListener { _, v, _, _ ->

            val textView = v as TextView
            val keyword = textView.text.toString()

            mReplaceKeyword = true // Don't response the
            // onTextChanged event this
            // time.

            mDictKeywordView?.setText(keyword)
            tv_info_search.visibility = View.GONE
            action_wordslist.visibility = View.GONE
            tv_info_search.text = null
            // make sure we keep the caret at the end of the text
            // view
            val spannable = mDictKeywordView?.text
            Selection.setSelection(spannable, spannable?.length ?: 0)
            showSearchContent()
        }
        mPopupWordsListHandler = Handler()

    }

    private fun startService() {
        if (mSharedPreferences?.getBoolean(getString(R.string.prefs_key_using_capture), false) == true) {
            if (!QDictService.RUNNING)
                checkPermission(REQUEST_ALERT_WINDOW_CODE)
        } else {
            if (QDictService.RUNNING)
                StandOutWindow.closeAll(this, QDictService::class.java)
        }
        checkUseClipboard()
    }

    private fun checkUseClipboard() {
        if (!QDictService.RUNNING) {
            initClipboard()
        } else {
            releaseClipboard()
        }
    }

    private fun stopService() {
        if (QDictService.RUNNING) {
            StandOutWindow.closeAll(this, QDictService::class.java)
        }
        checkUseClipboard()
    }

    override fun onStart() {
        active = true
        super.onStart()
    }

    override fun onStop() {
        active = false
        super.onStop()
    }

    @Suppress("DEPRECATION")
    private fun showProgressDialog() {
        mProgressDialog = ProgressDialog(this)
        mProgressDialog?.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        mProgressDialog?.setMessage(resources.getString(R.string.keywords_search))
        mProgressDialog?.setCancelable(false)
        mProgressDialog?.setButton(DialogInterface.BUTTON_NEGATIVE, resources.getString(R.string.cancel)
        ) { dialog, _ ->
            mDictions?.cancelLookup()
            dialog.cancel()
        }
        mProgressDialog?.show()
    }

    private fun startKeywordsList() {
        var listType = LIST_WORDS_NORMAL
        var keyword = mDictKeywordView?.text?.toString()?.trim { it <= ' ' } ?: ""
        keyword = keyword.trim { it <= ' ' }

        if (keyword.isEmpty()) {
            return
        }

        if (keyword[0] == '/' || keyword[0] == ':' || keyword.indexOf('*') >= 0
                || keyword.indexOf('?') >= 0) {
            if (keyword[0] == '/') {
                keyword = keyword.substring(1)
                listType = LIST_WORDS_FUZZY
            } else if (keyword[0] == ':') {
                keyword = keyword.substring(1)
                listType = LIST_WORDS_FULLTEXT
            } else {
                listType = LIST_WORDS_PATTERN
            }
        }

        if (!mIsTaskRunning)
        // One task is running.
        {
            mIsTaskRunning = true
            if (LIST_WORDS_NORMAL != listType)
                showProgressDialog()
            val mListWordsTask = ListWordsTask(listType)
            mListWordsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, keyword)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mSpeechEng != null) {
            mSpeechEng!!.destroy()
            mSpeechEng = null
        }
        if (mDictions != null) {
            mDictions!!.destroy()
            mDictions = null
        }
        if (mUIReceiver != null)
            unregisterReceiver(mUIReceiver)
        //
        mShowKeyboarRunable?.let {
            mShowKeyboardHander?.removeCallbacks(it)
        }
        mShowKeyboarRunable = null
        releaseClipboard()
    }

    override fun onPause() {
        mPopupWordsListHandler?.removeCallbacks(mPopupWordsListRunnable)
        super.onPause()
    }

    // start Recognition
    private fun startVoiceRecognition() {
        // start voice
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.ENGLISH)
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, Locale.ENGLISH)
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.press_on_when_done))
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        // ... put other settings in the Intent
        try {
            startActivityForResult(intent, REQUEST_CODE)
        } catch (e: ActivityNotFoundException) {
            M2tToast.makeText(this, e.localizedMessage, M2tToast.LENGTH_SHORT).show()
        }
    }

    private fun hideKeyboard() {
        val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(currentFocus?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    fun showSearchContent() {
        val keyword = mDictKeywordView?.text?.toString()?.trim { it <= ' ' } ?: ""
        val fragment = this.supportFragmentManager.findFragmentById(R.id.content_frame)
        if (fragment is SearchFragment) {
            val sFrag = fragment as SearchFragment?
            sFrag?.setDictions(mDictions)
            sFrag?.setSpeechEng(mSpeechEng)
            sFrag?.keyword = keyword
            mCurrentNavPosition = NAVIG.SEARCH
        } else {
            try {
                val searchFrag = SearchFragment.newInstance(mSpeechEng, mDictions, keyword, true)
                supportFragmentManager.beginTransaction().replace(R.id.content_frame, searchFrag).commit()
                mCurrentNavPosition = NAVIG.SEARCH
            } catch (ex: IllegalStateException) {
                Log.e("MainActivity", ex.toString())
            }

        }
        if (sliding_layout.panelState != PanelState.HIDDEN) {
            sliding_layout.panelState = PanelState.HIDDEN
            action_menu.clearFocus()
        }

        hideKeyboard()
    }

    override fun onBackPressed() {
        when {
            drawer_layout.isDrawerOpen(GravityCompat.START) -> drawer_layout.closeDrawer(GravityCompat.START)
            sliding_layout.panelState == PanelState.EXPANDED || sliding_layout.panelState == PanelState.ANCHORED -> {
                sliding_layout.panelState = PanelState.HIDDEN
                action_menu.clearFocus()
            }
            else -> super.onBackPressed()
        }

    }

    // call from layout xml
    fun onActionButtonClick(v: View) {
        sliding_layout.panelState = PanelState.EXPANDED
        mShowKeyboarRunable?.let {
            mShowKeyboardHander?.removeCallbacks(it)
            val textLength = mDictKeywordView?.text?.length ?: 0
            if (textLength == 0) {
                mShowKeyboardHander?.postDelayed(it, 200)
            }
        }

    }


    override fun onNavigationItemSelected(title: String, position: Int) =
            if (position == NAVIG.SELECT_DICT) {
                setMenuFragment(ListDictFragment.newInstance(mDictions))
            } else {
                onNavig = true
                tempKeyword = title
                tempPos = position
                drawer_layout.closeDrawer(GravityCompat.START)
                onMenuClose()
            }

    private fun setMenuFragment(fragment: Fragment) {
        val t = supportFragmentManager.beginTransaction()
        t.setCustomAnimations(R.anim.push_left_in, R.anim.push_left_out, R.anim.push_right_in, R.anim.push_right_out)
        t.replace(R.id.frame_container, fragment).commit()
    }

    private fun onMenuClose() {
        if (onNavig) {
            when (tempPos) {
                NAVIG.SETTINGS -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out)
                }
                NAVIG.JOIN_US -> {
                    startActivity(getOpenPageFBIntent(applicationContext))
                }
                else -> {
                    if (sliding_layout.panelState == PanelState.EXPANDED || sliding_layout.panelState == PanelState.ANCHORED) {
                        sliding_layout.panelState = PanelState.HIDDEN
                    }
                    if (mCurrentNavPosition != tempPos)
                        setFragment(tempKeyword, tempPos)
                }
            }
            onNavig = false
        }
    }

    fun setFragment(keyword: String, position: Int) {
        var fragment: Fragment?
        when (position) {
            NAVIG.HOME, NAVIG.SEARCH -> fragment = SearchFragment.newInstance(mSpeechEng, mDictions, keyword, position == NAVIG.SEARCH)
            NAVIG.RECENT, NAVIG.FAVORITE -> {
                fragment = this.supportFragmentManager.findFragmentById(R.id.content_frame)
                val favorite = position == NAVIG.FAVORITE
                if (fragment is RecentFragment) {
                    fragment.setFavorite(favorite)
                } else {
                    fragment = RecentFragment()
                    val b = Bundle()
                    b.putBoolean("qdict_is_favorite", favorite)
                    fragment.arguments = b
                }
            }
            else -> fragment = null
        }
        if (fragment != null) {
            supportFragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit()
            mCurrentNavPosition = position
        }
    }

    private fun showKeywordsList(strWordsList: Array<String>) {
        drop_list.adapter = MyArrayAdapter(this, strWordsList)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.let { results ->
                    val keyword = results[0]
                    if (!TextUtils.isEmpty(keyword)) {
                        mDictKeywordView?.setText(keyword)
                        runOnUiThread { showSearchContent() }

                    }
                }

            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.action_menu -> {
                if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
                    drawer_layout.closeDrawer(GravityCompat.START)
                } else {
                    drawer_layout.openDrawer(GravityCompat.START)
                }
            }
            R.id.action_voice -> startVoiceRecognition()
            R.id.action_wordslist -> startKeywordsList()
            R.id.action_share -> startActivity(getIntentShareData(MainActivity::class.java))
            R.id.action_about -> showDialog(DIALOG.ABOUT)
            else -> {
            }
        }
    }

    // / for menu
    override fun onCreateDialog(id: Int): Dialog {
        when (id) {
            DIALOG.ABOUT -> return createAboutDialog(this)
            DIALOG.CHANGE_LOG -> return createWhatsNewDialog(this)
            else -> {
            }
        }
        return super.onCreateDialog(id)
    }

    // Extend classes.
    private inner class DictEditTextView(context: Context) : AppCompatEditText(context, null) {

        internal var type = LIST_WORDS_NORMAL

        init {
            setSelectAllOnFocus(true)
            setHint(R.string.action_search)
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setPadding(10, 0, 3, 0)
            gravity = Gravity.CENTER_VERTICAL
            setSingleLine()
            val mFont = getFont(context, mSharedPreferences?.getString(Def.PREF_KEY_FONT, Def.DEFAULT_FONT)
                    ?: Def.DEFAULT_FONT)
            typeface = mFont
        }

        override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
            when (keyCode) {
                // avoid passing the focus from the text view to the next
                // component
                KeyEvent.KEYCODE_SEARCH, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_UP -> return type == LIST_WORDS_NORMAL
            }
            return super.onKeyUp(keyCode, event)
        }

        override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
            when (keyCode) {
                // avoid passing the focus from the text view to the next
                // component
                KeyEvent.KEYCODE_SEARCH, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_UP -> {
                    drop_list.isListSelectionHidden = false
                    return true
                }
            }
            if (KeyEvent.KEYCODE_ENTER == keyCode) {
                if (type == LIST_WORDS_NORMAL) {
                    showSearchContent()
                } else {
                    mPopupWordsListHandler?.postDelayed(mPopupWordsListRunnable, POPUPWORDSLIST_TIMER.toLong())
                }
                hideKeyboard()
            }
            return super.onKeyDown(keyCode, event)
        }

        override fun onTextChanged(text: CharSequence, start: Int, before: Int, after: Int) {
            mPopupWordsListHandler?.removeCallbacks(mPopupWordsListRunnable)
            if (mReplaceKeyword) {
                mReplaceKeyword = false
            } else {
                val keyword = text.toString()
                if (!TextUtils.isEmpty(keyword)) {
                    if (keyword[0] == '/') {
                        tv_info_search.visibility = View.VISIBLE
                        tv_info_search.setText(R.string.fuzzy_query_prompt)
                        action_wordslist.visibility = View.VISIBLE
                        type = LIST_WORDS_FUZZY
                    } else if (keyword[0] == ':') {
                        tv_info_search.visibility = View.VISIBLE
                        tv_info_search.setText(R.string.fulltext_query_prompt)
                        action_wordslist.visibility = View.VISIBLE
                        type = LIST_WORDS_FULLTEXT
                    } else if (keyword.indexOf('*') >= 0 || keyword.indexOf('?') >= 0) {
                        tv_info_search.visibility = View.VISIBLE
                        tv_info_search.setText(R.string.pattern_query_prompt)
                        action_wordslist.visibility = View.VISIBLE
                        type = LIST_WORDS_PATTERN
                    } else {
                        tv_info_search.visibility = View.GONE
                        action_wordslist.visibility = View.GONE
                        tv_info_search.text = null
                        type = LIST_WORDS_NORMAL
                        mPopupWordsListHandler?.postDelayed(mPopupWordsListRunnable, POPUPWORDSLIST_TIMER.toLong())
                    }
                }
            }
            super.onTextChanged(text, start, before, after)
        }
    }

    private inner class ListWordsTask(internal var mListType: Int) : AsyncTask<String, Void, Array<String>>() {

        override fun doInBackground(vararg params: String): Array<String>? {
            var strWordsList: Array<String>? = null
            val keyword = params[0]
            mDictions?.let {
                when (mListType) {
                    LIST_WORDS_NORMAL -> strWordsList = it.listWords(keyword)
                    LIST_WORDS_FUZZY -> strWordsList = it.fuzzyListWords(keyword)
                    LIST_WORDS_PATTERN -> strWordsList = it.patternListWords(keyword)
                    LIST_WORDS_FULLTEXT -> strWordsList = it.fullTextListWords(keyword)
                }
            }

            return strWordsList
        }

        override fun onPostExecute(strWordsList: Array<String>?) {
            mIsTaskRunning = false // Task has stopped.
            if (mProgressDialog?.isShowing == true)
                mProgressDialog?.cancel()
            if (null == strWordsList || strWordsList.isEmpty()) {
                return
            }
            showKeywordsList(strWordsList)
        }
    }

    companion object {

        const val ACTION_UPDATE_UI = "com.annie.dictionary.ACTION_UPDATE_UI"

        const val ACTION_UPDATE_KEY = "receiver_update_ui"
        // const
        const val REQUEST_CODE = 101
        const val POPUPWORDSLIST_TIMER = 200
        const val LIST_WORDS_NORMAL = 0
        const val LIST_WORDS_FUZZY = 1
        const val LIST_WORDS_PATTERN = 2
        const val LIST_WORDS_FULLTEXT = 3
        var hasStoragePermission = false
        //
        var active = false
        private var mProgressCBHandler: Handler? = null

        fun lookupProgressCB(progress: Int) {
            val m = Message.obtain()
            m.arg1 = progress
            m.target = mProgressCBHandler
            m.sendToTarget()
        }
    }

}
