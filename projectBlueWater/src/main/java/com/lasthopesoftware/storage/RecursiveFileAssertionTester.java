package com.lasthopesoftware.storage;

import android.support.annotation.NonNull;

import com.vedsoft.futures.callables.OneParameterCallable;

import java.io.File;

/**
 * Created by david on 7/17/16.
 */
public final class RecursiveFileAssertionTester {

	public static boolean recursivelyTestAssertion(@NonNull File file, OneParameterCallable<File, Boolean> assertion) {
		File testFile = file;
		do {
			if (testFile.exists())
				return assertion.call(testFile);
		} while ((testFile = testFile.getParentFile()) != null);

		return false;
	}
}