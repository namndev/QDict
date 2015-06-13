package com.annie.dictionary;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import com.annie.dictionary.frags.SettingFragment;
import com.annie.dictionary.utils.Utils;
import com.annie.dictionary.utils.Utils.Def;

public class SettingsActivity extends ActionBarActivity {

	protected Fragment mFrag;
	private SharedPreferences mSharedPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mSharedPreferences = getSharedPreferences(Def.APP_NAME,
				Context.MODE_PRIVATE);
		int themeIndex = mSharedPreferences.getInt("prefs_key_theme", 0);
		Utils.onActivityCreateSetTheme(this, themeIndex, false);
		setContentView(R.layout.layout_settings);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		if (savedInstanceState == null) {
			FragmentTransaction t = this.getSupportFragmentManager()
					.beginTransaction();
			mFrag = new SettingFragment(
					DictSpeechEng.getInstance(getApplicationContext()));
			t.replace(R.id.setting_frame, mFrag);
			t.commit();
		} else {
			mFrag = this.getSupportFragmentManager().findFragmentById(
					R.id.setting_frame);
			if (mFrag instanceof SettingFragment) {
				((SettingFragment) mFrag).SetSpeechEng(DictSpeechEng
						.getInstance(getApplicationContext()));
			}
		}
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
			overridePendingTransition(R.anim.push_right_in,
					R.anim.push_right_out);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
