package com.annie.dictionary;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;
import android.view.View;

import com.annie.dictionary.frags.ListDictFragment;
import com.annie.dictionary.frags.NavigatorFragment;
import com.annie.dictionary.utils.Utils;
import com.annie.dictionary.utils.Utils.Def;
import com.mmt.app.SlidingFragmentActivity;
import com.mmt.widget.slidemenu.SlidingMenu;

public class BaseActivity extends SlidingFragmentActivity {

	protected Fragment mFrag;
	protected SharedPreferences mSharedPreferences;
	protected int mThemeIndex;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSharedPreferences = getSharedPreferences(Def.APP_NAME,
				Context.MODE_PRIVATE);
		mThemeIndex = mSharedPreferences.getInt("prefs_key_theme", 0);
		Utils.onActivityCreateSetTheme(this, mThemeIndex, true);
		setTitle(R.string.app_name);

		// set the Behind View
		setBehindContentView(R.layout.menu_frame);
		float density = getResources().getDisplayMetrics().density;
		if (density <= 1.5f) {
			// hdpi = 1.5
			findViewById(R.id.img_header).setVisibility(View.GONE);
		}
		if (savedInstanceState == null) {
			FragmentTransaction t = this.getSupportFragmentManager()
					.beginTransaction();
			mFrag = new NavigatorFragment();
			t.replace(R.id.menu_frame, mFrag);
			t.commit();
		} else {
			mFrag = (Fragment) this.getSupportFragmentManager()
					.findFragmentById(R.id.menu_frame);
		}
		// customize the SlidingMenu
		SlidingMenu sm = getSlidingMenu();
		sm.setShadowWidthRes(R.dimen.shadow_width);
		sm.setShadowDrawable(R.drawable.shadow);
		sm.setBehindOffsetRes(R.dimen.slidingmenu_offset);
		sm.setFadeDegree(0.35f);
		sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	};

	public void backMainMenuFragment() {
		FragmentTransaction t = this.getSupportFragmentManager()
				.beginTransaction();
		t.setCustomAnimations(R.anim.push_right_in, R.anim.push_right_out,
				R.anim.push_left_in, R.anim.push_left_out);
		if (mFrag instanceof ListDictFragment) {
			mFrag = new NavigatorFragment();
		}
		t.replace(R.id.menu_frame, mFrag);
		t.commit();
	}

	@Override
	public void resroteDefaultFragment() {
		if (mFrag instanceof ListDictFragment) {
			FragmentTransaction t = this.getSupportFragmentManager()
					.beginTransaction();
			mFrag = new NavigatorFragment();
			t.replace(R.id.menu_frame, mFrag);
			t.commit();
		}
	}

	public void setMenuFragment(Fragment fragment) {
		FragmentTransaction t = this.getSupportFragmentManager()
				.beginTransaction();
		mFrag = fragment;
		t.setCustomAnimations(R.anim.push_left_in, R.anim.push_left_out,
				R.anim.push_right_in, R.anim.push_right_out);
		t.replace(R.id.menu_frame, fragment);
		t.commit();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			toggle();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
