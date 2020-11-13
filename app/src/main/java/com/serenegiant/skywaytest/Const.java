package com.serenegiant.skywaytest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.NonNull;

/*package*/ class Const {
	private Const() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをｐrivateに
	}

	/*package*/ static final String PREF_KEY_API_KEY = "PREF_KEY_API_KEY";
	/*package*/ static final String EXTRA_KEY_API_KEY = "EXTRA_KEY_API_KEY";
	/*package*/ static final String PREF_KEY_ROOM_SFU = "PREF_KEY_ROOM_SFU";
	/*package*/ static final String PREF_KEY_ROOM_MESH = "PREF_KEY_ROOM_MESH";

	/*package*/ static final String API_KEY = BuildConfig.SKYWAY_API_KEY;
	/*package*/ static final String DOMAIN = BuildConfig.SKYWAY_DOMAIN;

	@NonNull
	/*package*/ static String getAPIKey(@NonNull final Activity activity)
		throws IllegalArgumentException {

		final Intent intent = activity.getIntent();
		if (intent != null) {
			final String apiKey = intent.getStringExtra(EXTRA_KEY_API_KEY);
			if (!TextUtils.isEmpty(apiKey)) {
				return apiKey;
			}
		}
		throw new IllegalArgumentException("failed to get API key.");
	}

	/**
	 * 最後に使用したskyway APIキーを共有プレファレンスから読み込む
	 * @param activity
	 * @return
	 */
	/*package*/ static String loadAPIKey(@NonNull final Activity activity) {
		final String lastApiKey
			= activity.getPreferences(Context.MODE_PRIVATE)
				.getString(PREF_KEY_API_KEY, null);
		if (!TextUtils.isEmpty(lastApiKey)) {
			return lastApiKey;
		} else if (BuildConfig.DEBUG) {
			return API_KEY;
		}
		return null;
	}

	/**
	 * 最後に正常に接続できたskyway APIキーを共有プレファレンスに保存する
	 * @param activity
	 * @param apiKey
	 */
	/*package*/ static void saveAPIKey(@NonNull final Activity activity, final String apiKey) {
		activity.getPreferences(Context.MODE_PRIVATE)
			.edit()
			.putString(PREF_KEY_API_KEY, apiKey)
			.apply();

	}
}
