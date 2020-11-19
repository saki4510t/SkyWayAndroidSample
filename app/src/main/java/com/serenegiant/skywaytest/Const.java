package com.serenegiant.skywaytest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.utils.KeyStoreUtils;
import com.serenegiant.utils.ObfuscatorException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Const {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = Const.class.getSimpleName();

	private Const() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	private static final String PREF_KEY_API_KEY = "PREF_KEY_API_KEY";
	/*package*/ static final String EXTRA_KEY_API_KEY = "EXTRA_KEY_API_KEY";
	private static final String PREF_KEY_SECRET_KEY = "PREF_KEY_SECRET_KEY";
	/*package*/ static final String PREF_KEY_ROOM_SFU = "PREF_KEY_ROOM_SFU";
	/*package*/ static final String PREF_KEY_ROOM_MESH = "PREF_KEY_ROOM_MESH";
	private static final String PREF_KEY_PEER_AUTH_ENABLED = "PREF_KEY_PEER_AUTH_ENABLED";

	/*package*/ static final String DOMAIN = BuildConfig.SKYWAY_DOMAIN;

	/**
	 * モックサーバーを使ってピア認証を行うかどうか
	 * この設定をtrueにした場合は、SkyWayのアプリケーションの設定で
	 * 「APIキー認証を利用する」にチェックを入れておかないと接続できない。
	 * 逆にこの設定をfalseにした場合は、SkyWayのアプリケーションの設定で
	 * 「APIキー認証を利用する」にチェックをはずしておかないと接続できない。
	 * @param context
	 * @return
	 */
	/*package*/ static boolean isPeerAuthEnabled(@NonNull final Context context) {
		return getPreferences(context).getBoolean(PREF_KEY_PEER_AUTH_ENABLED, true);
	}

	/*package*/ static void setPeerAuthEnabled(@NonNull final Context context, final boolean enabled) {
		getPreferences(context)
			.edit()
			.putBoolean(PREF_KEY_PEER_AUTH_ENABLED, enabled)
			.apply();
	}

//--------------------------------------------------------------------------------
	/**
	 * ActivityのインテントからAPIキーを取得する
	 * @param activity
	 * @return
	 * @throws IllegalArgumentException
	 */
	@NonNull
	/*package*/ static String getAPIKey(@NonNull final Activity activity)
		throws IllegalArgumentException {

		final Intent intent = activity.getIntent();
		if (intent != null) {
			final String apiKey = intent.getStringExtra(EXTRA_KEY_API_KEY);
			if (!TextUtils.isEmpty(apiKey)) {
				return apiKey;
			} else if (!TextUtils.isEmpty(BuildConfig.SKYWAY_API_KEY)) {
				return BuildConfig.SKYWAY_API_KEY;
			}
		}
		throw new IllegalArgumentException("failed to get API key.");
	}

	/**
	 * 最後に使用したskyway APIキーを共有プレファレンスから読み込む
	 * @param context
	 * @return
	 */
	@Nullable
	/*package*/ static String loadAPIKey(@NonNull final Context context) {
		final String lastApiKey
			= getPreferences(context)
				.getString(PREF_KEY_API_KEY, null);
		if (!TextUtils.isEmpty(lastApiKey)) {
			return lastApiKey;
		} else {
			return BuildConfig.SKYWAY_API_KEY;
		}
	}

	/**
	 * 最後に正常に接続できたskyway APIキーを共有プレファレンスに保存する
	 * @param context
	 * @param apiKey
	 */
	/*package*/ static void saveAPIKey(@NonNull final Context context, final String apiKey) {
		getPreferences(context)
			.edit()
			.putString(PREF_KEY_API_KEY, apiKey)
			.apply();

	}

//--------------------------------------------------------------------------------
	/**
	 * 最後に使用したskyway Secretキーを共有プレファレンスから読み込む
	 * XXX このサンプルはモックサーバーでピア認証/クレデンシャル生成をするので共有プレファレンスに保存しているけど
	 * XXX 本来はセキュアなサーバー上でピア認証/クレデンシャル生成をしないといけないので端末にSkyWayの秘密鍵を入力・保存することはない
	 * @param context
	 * @return
	 */
	@Nullable
	public static String loadSecretKey(@NonNull final Context context) {
		String lastSecretKey = null;
		final SharedPreferences preferences = getPreferences(context);
		if (preferences.contains(PREF_KEY_SECRET_KEY)) {
			final String encrypted
				= preferences.getString(PREF_KEY_SECRET_KEY, null);
			if (!TextUtils.isEmpty(encrypted)) {
				try {
					lastSecretKey = KeyStoreUtils.decrypt(context, PREF_KEY_SECRET_KEY, encrypted);
				} catch (final ObfuscatorException e) {
					if (DEBUG) Log.w(TAG, e);
				}
			}
		}
		if (!TextUtils.isEmpty(lastSecretKey)) {
			return lastSecretKey;
		} else {
			return BuildConfig.SKYWAY_SECRET_KEY;
		}
	}

	/**
	 * 最後に正常に接続できたskyway Secretキーを共有プレファレンスに保存する
	 * XXX このサンプルはモックサーバーでピア認証/クレデンシャル生成をするので共有プレファレンスに保存しているけど
	 * XXX 本来はセキュアなサーバー上でピア認証/クレデンシャル生成をしないといけないので端末にSkyWayの秘密鍵を入力・保存することはない
	 * @param context
	 * @param secretKey
	 */
	/*package*/ static void saveSecretKey(
		@NonNull final Context context, @Nullable final String secretKey) {

		final String encrypted;
		if (!TextUtils.isEmpty(secretKey)) {
			encrypted = KeyStoreUtils.encrypt(context, PREF_KEY_SECRET_KEY, secretKey);
		} else {
			encrypted = null;
		}
		if (!TextUtils.isEmpty(encrypted)) {
			getPreferences(context).edit().putString(PREF_KEY_SECRET_KEY, encrypted).apply();
		} else {
			getPreferences(context).edit().remove(PREF_KEY_SECRET_KEY).apply();
		}
	}

//--------------------------------------------------------------------------------
	@NonNull
	public static SharedPreferences getPreferences(@NonNull final Context context) {
		return context.getSharedPreferences(
			context.getPackageName(), Context.MODE_PRIVATE);
	}
}
