package com.lasthopesoftware.bluewater.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;

class HandleCheckboxPreference implements CompoundButton.OnCheckedChangeListener {
	static void handle(Context context, String settingKey, CheckBox settingCheckbox) {
		settingCheckbox.setEnabled(false);
		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		final boolean preference = sharedPreferences.getBoolean(settingKey, false);
		settingCheckbox.setChecked(preference);
		settingCheckbox.setOnCheckedChangeListener(
			new HandleCheckboxPreference(sharedPreferences, settingKey));
		settingCheckbox.setEnabled(true);
	}

	private final SharedPreferences sharedPreferences;
	private final String settingKey;

	private HandleCheckboxPreference(SharedPreferences sharedPreferences, String settingKey) {
		this.settingKey = settingKey;
		this.sharedPreferences = sharedPreferences;
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		sharedPreferences
			.edit()
			.putBoolean(settingKey, isChecked)
			.apply();
	}
}
