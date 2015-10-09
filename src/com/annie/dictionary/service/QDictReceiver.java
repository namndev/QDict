package com.annie.dictionary.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.annie.dictionary.R;
import com.annie.dictionary.utils.Utils.Def;

public class QDictReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			SharedPreferences preferenceSettings = context
					.getSharedPreferences(Def.APP_NAME, Context.MODE_PRIVATE);
			boolean isCapture = preferenceSettings.getBoolean(
					context.getResources().getString(
							R.string.prefs_key_using_capture), false);
			if (isCapture) {
				Intent i = new Intent(Intent.ACTION_RUN);
				i.setClass(context, QDictService.class);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startService(i);
			}
		}
	}
}
