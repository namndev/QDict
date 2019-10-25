package com.annie.dictionary

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.annie.dictionary.utils.Utils
import com.annie.dictionary.utils.Utils.Def
import com.mmt.app.SystemBarTintManager
import java.util.*

abstract class BaseActivity : AppCompatActivity() {
    protected var mSharedPreferences: SharedPreferences? = null
    private var mThemeIndex: Int = 0
    private var mCurrentLanguage: String? = null

    fun checkPermission(requestCode: Int) {
        when (requestCode) {
            REQUEST_STORAGE_CODE -> if (Utils.hasSelfPermission(this, STORAGE_PERMISSIONS))
                onRequestPermissionResult(requestCode, true)
            else
                if (Utils.hasMmAbove()) {
                    requestPermissions(STORAGE_PERMISSIONS, requestCode)
                }
            REQUEST_ALERT_WINDOW_CODE -> if (!Utils.hasMmAbove()) {
                onRequestPermissionResult(REQUEST_ALERT_WINDOW_CODE, true)
            } else {
                if (Settings.canDrawOverlays(this)) {
                    onRequestPermissionResult(REQUEST_ALERT_WINDOW_CODE, true)
                } else {
                    val drawOverlaysSettingsIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    drawOverlaysSettingsIntent.data = Uri.parse("package:$packageName")
                    startActivityForResult(drawOverlaysSettingsIntent, REQUEST_ALERT_WINDOW_CODE)
                }
            }
            else -> {
            }
        }
    }

    abstract fun onRequestPermissionResult(requestCode: Int, isSuccess: Boolean)

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_STORAGE_CODE, REQUEST_ALERT_WINDOW_CODE -> onRequestPermissionResult(requestCode, Utils.verifyAllPermissions(grantResults))
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_ALERT_WINDOW_CODE && Utils.hasMmAbove()) {
            onRequestPermissionResult(REQUEST_ALERT_WINDOW_CODE, Settings.canDrawOverlays(this))
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        mSharedPreferences = getSharedPreferences(Def.APP_NAME, Context.MODE_PRIVATE)
        setLanguage()
        mThemeIndex = mSharedPreferences?.getInt("prefs_key_theme", 0) ?: 0
        Utils.onActivityCreateSetTheme(this, mThemeIndex, Utils.ThemeActivity.HOME)
        super.onCreate(savedInstanceState)
        setTitle(R.string.app_name)
        if (Utils.hasKk()) {
            setTranslucentStatus(true)
            val tintManager = SystemBarTintManager(this)
            tintManager.isStatusBarTintEnabled = true
            tintManager.setStatusBarTintColor(Utils.getColor(this, R.attr.colorPrimaryDark))
        }

    }

    private fun setLanguage() {
        var language = mSharedPreferences?.getString("prefs_key_languages", "") ?: ""
        val configuration = resources.configuration
        var lang = configuration.locale.language
        if (TextUtils.isEmpty(language)) {
            if (lang.equals("zh", ignoreCase = true)) {
                val country = configuration.locale.country
                lang += "_" + country.toUpperCase(Locale.US)
            }
            language = lang
            if (Utils.checkLanguageSupport(language)) {
                mSharedPreferences?.edit()?.putString("prefs_key_languages", language)?.apply()
                if (language.contains("_")) {
                    val s = language.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    Utils.changeLocale(resources, s[0], s[1])
                } else {
                    Utils.changeLocale(resources, language)
                }

            }
        } else if (!language.contains(lang)) {
            if (language.contains("_")) {
                val s = language.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                Utils.changeLocale(resources, s[0], s[1])
            } else {
                Utils.changeLocale(resources, language)
            }
        }
        mCurrentLanguage = language
    }

    @TargetApi(19)
    private fun setTranslucentStatus(on: Boolean) {
        val win = window
        val winParams = win.attributes
        val bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
        if (on) {
            winParams.flags = winParams.flags or bits
        } else {
            winParams.flags = winParams.flags and bits.inv()
        }
        win.attributes = winParams
    }

    companion object {

        const val REQUEST_STORAGE_CODE = 1001
        const val REQUEST_ALERT_WINDOW_CODE = 1003
        /**
         * Read and write permission for storage listed here.
         */
        val STORAGE_PERMISSIONS = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}
