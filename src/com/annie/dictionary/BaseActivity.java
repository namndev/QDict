
package com.annie.dictionary;

import com.annie.dictionary.frags.ListDictFragment;
import com.annie.dictionary.frags.NavigatorFragment;
import com.annie.dictionary.utils.Utils;
import com.annie.dictionary.utils.Utils.Def;
import com.mmt.app.SlidingFragmentActivity;
import com.mmt.app.SystemBarTintManager;
import com.mmt.widget.slidemenu.SlidingMenu;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public abstract class BaseActivity extends SlidingFragmentActivity {

    protected Fragment mFrag;

    protected SharedPreferences mSharedPreferences;

    protected int mThemeIndex;

    /**
     * Read and write permission for storage listed here.
     */
    private static String STORAGE_PERMISSIONS[] = new String[] {
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private static String ALERT_WINDOW_PERMISSIONS[] = new String[] {
            Manifest.permission.SYSTEM_ALERT_WINDOW
    };

    public final static int REQUEST_STORAGE_CODE = 1001;

    public final static int REQUEST_ALERT_WINDOW_CODE = 1003;

    public static boolean isActive = false;

    public void checkPermission(int requestCode) {
        switch (requestCode) {
            case REQUEST_STORAGE_CODE:
                if (Utils.hasSelfPermission(this, STORAGE_PERMISSIONS))
                    onRequestPermissionResult(requestCode, true);
                else
                    requestPermissions(STORAGE_PERMISSIONS, requestCode);
                break;
            case REQUEST_ALERT_WINDOW_CODE:
                if (!Utils.hasMmAbove()) {
                    onRequestPermissionResult(REQUEST_ALERT_WINDOW_CODE, true);
                } else {
                    if (Settings.canDrawOverlays(this)) {
                        onRequestPermissionResult(REQUEST_ALERT_WINDOW_CODE, true);
                    } else {
                        Intent drawOverlaysSettingsIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                        drawOverlaysSettingsIntent.setData(Uri.parse("package:" + getPackageName()));
                        startActivityForResult(drawOverlaysSettingsIntent, REQUEST_ALERT_WINDOW_CODE);
                    }
                }
                break;
            default:
                break;
        }
    }

    public abstract void onRequestPermissionResult(int requestCode, boolean isSucess);

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_STORAGE_CODE:
            case REQUEST_ALERT_WINDOW_CODE:
                onRequestPermissionResult(requestCode, Utils.verifyAllPermissions(grantResults));
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ALERT_WINDOW_CODE) {
            onRequestPermissionResult(REQUEST_ALERT_WINDOW_CODE, Settings.canDrawOverlays(this));
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mSharedPreferences = getSharedPreferences(Def.APP_NAME, Context.MODE_PRIVATE);
        mThemeIndex = mSharedPreferences.getInt("prefs_key_theme", 0);
        Utils.onActivityCreateSetTheme(this, mThemeIndex, true);
        super.onCreate(savedInstanceState);
        setTitle(R.string.app_name);
        if (Utils.hasKk()) {
            setTranslucentStatus(true);
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(Utils.getColor(this, R.attr.colorPrimaryDark));
        }
        // set the Behind View
        setBehindContentView(R.layout.menu_frame);
        float density = getResources().getDisplayMetrics().density;
        if (density <= 1.5f) {
            // hdpi = 1.5
            findViewById(R.id.img_header).setVisibility(View.GONE);
        }
        if (savedInstanceState == null) {
            FragmentTransaction t = this.getSupportFragmentManager().beginTransaction();
            mFrag = new NavigatorFragment();
            t.replace(R.id.menu_frame, mFrag);
            t.commit();
        } else {
            mFrag = (Fragment)this.getSupportFragmentManager().findFragmentById(R.id.menu_frame);
        }
        // customize the SlidingMenu
        SlidingMenu sm = getSlidingMenu();
        sm.setShadowWidthRes(R.dimen.shadow_width);
        sm.setShadowDrawable(R.drawable.shadow);
        sm.setBehindOffsetRes(R.dimen.slidingmenu_offset);
        sm.setFadeDegree(0.35f);
        sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
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
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    };

    public void backMainMenuFragment() {
        FragmentTransaction t = this.getSupportFragmentManager().beginTransaction();
        t.setCustomAnimations(R.anim.push_right_in, R.anim.push_right_out, R.anim.push_left_in, R.anim.push_left_out);
        if (mFrag instanceof ListDictFragment) {
            mFrag = new NavigatorFragment();
        }
        t.replace(R.id.menu_frame, mFrag);
        t.commit();
    }

    @Override
    public void resroteDefaultFragment() {
        if (mFrag instanceof ListDictFragment) {
            FragmentTransaction t = this.getSupportFragmentManager().beginTransaction();
            mFrag = new NavigatorFragment();
            t.replace(R.id.menu_frame, mFrag);
            t.commit();
        }
    }

    public void setMenuFragment(Fragment fragment) {
        FragmentTransaction t = this.getSupportFragmentManager().beginTransaction();
        mFrag = fragment;
        t.setCustomAnimations(R.anim.push_left_in, R.anim.push_left_out, R.anim.push_right_in, R.anim.push_right_out);
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
