package com.lasthopesoftware.permissions;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;

/**
 * Created by david on 6/13/16.
 */
public class ExternalStorageReadPermissionsArbitrator implements IExternalStorageReadPermissionsArbitrator {
	private final Context context;

	public ExternalStorageReadPermissionsArbitrator(Context context) {
		this.context = context;
	}

	@SuppressLint("InlinedApi") // Suppressed because `or` condition below handles API version checking logic
	@Override
	public boolean isExternalStorageReadPermissionsGranted() {
		return Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN || ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
	}
}
