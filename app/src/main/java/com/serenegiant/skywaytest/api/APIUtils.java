package com.serenegiant.skywaytest.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.skywaytest.Const;
import com.serenegiant.utils.KeyStoreUtils;
import com.serenegiant.utils.ObfuscatorException;

import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class APIUtils {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = APIUtils.class.getSimpleName();

	/**
	 * 正常完了時のhttpレスポンス
	 */
	public static final int RESPONSE_OK = 200;
	/**
	 * 端末が登録されていない
	 */
	public static final int RESPONSE_DEVICE_NOT_REGISTERED = 222;
	/**
	 * サーバー側のエラー
	 */
	public static final int RESPONSE_SERVER_ERROR = 229;

	public static final int RESPONSE_UNKNOWN = -9999;

	//--------------------------------------------------------------------------------
	/**
	 * ピア認証を行う際のピアIDを取得する
	 * XXX ピア認証を行う場合はPeerの生成＆Skywayのサーバーとの接続よりも前にピアIDを確定しないといけないのでピアIDの自動生成を使えなない
	 * XXX 今回はランダムUUIDの文字列表現をピアIDとして使用するが、本来はユーザーアカウント登録などにおいて生成したものを使う
	 * @param context
	 * @return
	 */
	@NonNull
	public static synchronized String getPeerId(@NonNull final Context context) {
		final SharedPreferences preferences = getPreferences(context);
		String peerId
			= preferences.getString(Const.PREF_KEY_PEER_ID, null);
		if (TextUtils.isEmpty(peerId)) {
			peerId = UUID.randomUUID().toString();
			peerId = peerId.replace("-", "");
			preferences.edit().putString(Const.PREF_KEY_PEER_ID, peerId).apply();
		}
		return peerId;
	}

//--------------------------------------------------------------------------------
	/**
	 * セッショントークンのキー名
	 * 値自体はKeyStoreUtilsで暗号化する
	 */
	private static final String PREF_KEY_SESSION_TOKEN = "PREF_KEY_SESSION_TOKEN";

	/**
	 * セッショントークンを暗号化して共有プレファレンスに保存する
	 * @param context
	 * @param sessionToken nullまたは空文字列の場合はエントリーを削除する
	 */
	/*package*/ static void setSessionToken(
		@NonNull final Context context,
		@Nullable final String sessionToken) {

		final SharedPreferences preferences = getPreferences(context);
		final String encrypted;
		if (!TextUtils.isEmpty(sessionToken)) {
			encrypted = KeyStoreUtils.encrypt(context, PREF_KEY_SESSION_TOKEN, sessionToken);
		} else {
			encrypted = null;
		}
		if (!TextUtils.isEmpty(encrypted)) {
			preferences.edit().putString(PREF_KEY_SESSION_TOKEN, encrypted).apply();
		} else {
			preferences.edit().remove(PREF_KEY_SESSION_TOKEN).apply();
		}
	}


	/**
	 * 共有プレファレンスに保存してある暗号化されたセッショントークンを取得して
	 * 復号して返す
	 * @param context
	 * @return エントリーが存在しないまたは復号に失敗すればnull
	 */
	@Nullable
	public static String getSessionToken(@NonNull final Context context) {
		final SharedPreferences preferences = getPreferences(context);
		if (preferences.contains(PREF_KEY_SESSION_TOKEN)) {
			final String encrypted
				= preferences.getString(PREF_KEY_SESSION_TOKEN, null);
			if (!TextUtils.isEmpty(encrypted)) {
				try {
					return KeyStoreUtils.decrypt(context, PREF_KEY_SESSION_TOKEN, encrypted);
				} catch (final ObfuscatorException e) {
					if (DEBUG) Log.w(TAG, e);
				}
			}
		}
		return null;
	}

	/**
	 * セッショントークンをクリアする
	 * @param context
	 * @param
	 */
	public static void clearSessionToken(@NonNull final Context context) {
		setSessionToken(context, null);
	}

	@NonNull
	public static SharedPreferences getPreferences(@NonNull final Context context) {
		return context.getSharedPreferences(
			context.getPackageName(), Context.MODE_PRIVATE);
	}
}
