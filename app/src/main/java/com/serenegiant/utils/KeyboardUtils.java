/*
 * Copyright (c) 2020.  t_saki@serenegiant.com All rights reserved.
 */

package com.serenegiant.utils;

import android.app.Activity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public class KeyboardUtils {
	private KeyboardUtils() {
		// インスタンス化をエラーとするためにデフォルトコンストラクタをprivateにする
	}

	/**
	 * ソフトウエアキーボードを非表示にする
	 * @param view
	 */
	public static void hide(@NonNull View view) {
		final InputMethodManager imm = ContextCompat.getSystemService(view.getContext(), InputMethodManager.class);
		if (imm != null) {
			view.clearFocus();
		    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}

	/**
	 * ソフトウエアキーボードを非表示にする
	 * @param activity
	 */
	public static void hide(@NonNull final Activity activity) {
		final InputMethodManager imm = ContextCompat.getSystemService(activity, InputMethodManager.class);
		if (imm != null) {
			//Find the currently focused view, so we can grab the correct window token from it.
			View view = activity.getCurrentFocus();
			   // If no view currently has focus, create a new one, just so we can grab a window token from it
			if (view == null) {
		        view = new View(activity);
		    }
		    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}
}
