package com.annie.dictionary;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.annie.dictionary.frags.SettingFragment;
import com.annie.dictionary.utils.Utils;
import com.annie.dictionary.utils.Utils.Def;
import com.mmt.app.SystemBarTintManager;

public class SettingsActivity extends AppCompatActivity {

    protected Fragment mFrag;

    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mSharedPreferences = getSharedPreferences(Def.APP_NAME, Context.MODE_PRIVATE);
        int themeIndex = mSharedPreferences.getInt("prefs_key_theme", 0);
        Utils.onActivityCreateSetTheme(this, themeIndex, Utils.ThemeActivity.SETTING);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_settings);
        setTitle(R.string.settings_lable);
        if (Utils.hasKk()) {
            setTranslucentStatus(true);
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(Utils.getColor(this, R.attr.colorPrimaryDark));
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        if (savedInstanceState == null) {
            FragmentTransaction t = this.getSupportFragmentManager().beginTransaction();
            mFrag = SettingFragment.newInstance(DictSpeechEng.getInstance(getApplicationContext()));
            t.replace(R.id.setting_frame, mFrag);
            t.commit();
        } else {
            mFrag = this.getSupportFragmentManager().findFragmentById(R.id.setting_frame);
            if (mFrag instanceof SettingFragment) {
                ((SettingFragment) mFrag).SetSpeechEng(DictSpeechEng.getInstance(getApplicationContext()));
            }
        }
    }

    @TargetApi(19)
    private void setTranslucentStatus(boolean on) {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
        win.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
