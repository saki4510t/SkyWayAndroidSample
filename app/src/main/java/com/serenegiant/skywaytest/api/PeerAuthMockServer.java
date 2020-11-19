package com.serenegiant.skywaytest.api;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.DateTypeAdapter;
import com.serenegiant.skywaytest.Const;
import com.serenegiant.skywaytest.api.model.PeerAuthInfo;
import com.serenegiant.skywaytest.api.model.PeerAuthResult;
import com.serenegiant.utils.ThreadPool;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class PeerAuthMockServer {
	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = PeerAuthMockServer.class.getSimpleName();

	private static final long MIN_CREDENTIAL_TTL_SECS = 600;	// 10分
	private static final long MAX_CREDENTIAL_TTL_SECS = 90000;	// 25時間

	private static final long CREDENTIAL_TTL_SECS
		= DEBUG
		? MIN_CREDENTIAL_TTL_SECS	// DEBUGフラグが立ってれば最小の10分
		: 3600;		// 1時間		// 通常は1時間

	@NonNull
	private final WeakReference<Context> mWeakContext;
	@NonNull
	private final MockWebServer mServer;
	/**
	 * セッショントークン
	 * 今回は共有プレファレンスを介してモックサーバーからSkyWayのクライアントアプリ側へ引き渡しているけど
	 * 本来はユーザー登録時に通知したり暗号化してビデオチャットurlに含めたりしてクライアントアプリ側へ引き渡す
	 */
	@NonNull
	private final String mSessionToken;

	public PeerAuthMockServer(@NonNull final Context context) {
		mWeakContext = new WeakReference<>(context);
		final String sessionToken = APIUtils.getSessionToken(context);
		if (!TextUtils.isEmpty(sessionToken)) {
			mSessionToken = sessionToken;
		} else {
			mSessionToken = UUID.randomUUID().toString();
			APIUtils.setSessionToken(context, mSessionToken);
		}
		mServer = new MockWebServer();
		mServer.setDispatcher(new MyDispatcher());
		try {
			mServer.start();
		} catch (final IOException e) {
			Log.w(TAG, e);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			release();
		} finally {
			super.finalize();
		}
	}

	/**
	 * release related resources
	 */
	public void release() {
		if (DEBUG) Log.v(TAG, "release:");
		try {
			mServer.shutdown();
		} catch (final IOException e) {
			Log.w(TAG, e);
		}
	}

	/**
	 * return base url string for API server
	 * @return
	 */
	@NonNull
	public String getBaseUri() {
		final String result = mServer.url("/").toString();
		if (DEBUG) Log.v(TAG, "getBaseUri:" + result);
		return result;
	}

	@Nullable
	protected Context getContext() throws IllegalStateException{
		return mWeakContext.get();
	}

	@NonNull
	protected Context requireContext() throws IllegalStateException{
		final Context context = getContext();
		if (context == null) {
			throw new IllegalStateException("Context is already released!");
		}
		return context;
	}

	/**
	 * Dispatcher to handle API request
	 */
	private class MyDispatcher extends Dispatcher {
		// JSONのパーサーとしてGsonを使う
		private final Gson mGson = new GsonBuilder()
			.setFieldNamingStrategy(GsonFieldNamingPolicy.LOWER_CAMEL_CASE)
			.registerTypeAdapter(Date.class, new DateTypeAdapter())
			.create();
		private final SecureRandom mRandom = new SecureRandom();
		@Nullable
		private String mAuthCode;

		@Override
		public MockResponse dispatch(final RecordedRequest request) {
			final String path = request != null ? request.getPath() : null;
			if (TextUtils.isEmpty(path)) {
				return new MockResponse().setResponseCode(400);
			}

			final Context context = getContext();
			if (context == null) {
				return new MockResponse().setResponseCode(APIUtils.RESPONSE_SERVER_ERROR);
			}

			final String[] args = path.split("/");
			int i = 0;
			for (final String a: args) {
				Log.i(TAG, "arg" + i++ + ")=" + a);
			}
			// API毎に分岐
			if (path.startsWith("/authenticate")) {
				// ピア認証の時
				return handleAuthenticate(context, request);
			} else {
				// 知らないAPIが呼び出された
				Log.i(TAG, "Unexpected API=" + path + ",body=" + request.getBody().readUtf8());
				mAuthCode = null;
				return new MockResponse().setResponseCode(404)
					.setBodyDelay(mRandom.nextInt(2) + 1, TimeUnit.SECONDS)
					.throttleBody(512, mRandom.nextInt(2) * 5, TimeUnit.SECONDS);
			}
		}

		/**
		 * ピア認証APIコールの処理
		 * @param context
		 * @param request
		 * @return
		 */
		private MockResponse handleAuthenticate(
			@NonNull final Context context,
			@NonNull final RecordedRequest request) {

			if (DEBUG) Log.v(TAG, "handleAuthenticate:");
			final String bodyString = request.getBody().readUtf8();
			final PeerAuthInfo body = mGson.fromJson(bodyString, PeerAuthInfo.class);
			Log.i(TAG, "bodyString=" + bodyString);
			Log.i(TAG, "body=" + body);
			final MockResponse response = new MockResponse()
				.setBodyDelay(mRandom.nextInt(2) + 1, TimeUnit.SECONDS)
				.throttleBody(512, mRandom.nextInt(2) * 5, TimeUnit.SECONDS);
			if (mSessionToken.equals(body.sessionToken)) {
				final long unixTimestampSecs = System.currentTimeMillis() / 1000L;
				final long credentialTTLSecs = CREDENTIAL_TTL_SECS;
				final String token = unixTimestampSecs + ":" + credentialTTLSecs + ":" + body.peerId;
				final String authToken = hmacSHA256(context, token);
				response.setResponseCode(200);
				// Gsonの設定でローワーキャメルケースにしているのが正しく変換されているか確かめるため
				// nextBoolean=falseならJSONObjectを使ってローワーキャメルケースでセットする
				if (mRandom.nextBoolean()) {
					response.setBody(mGson.toJson(
						new PeerAuthResult(body.peerId, authToken, unixTimestampSecs, credentialTTLSecs)));
				} else {
					final JSONObject json = new JSONObject();
					try {
						json.put("peerId", body.peerId)
							.put("authToken", authToken)
							.put("timestamp", unixTimestampSecs)
							.put("ttl", credentialTTLSecs);
						response.setBody(json.toString());
					} catch (final JSONException e) {
						Log.w(TAG, e);
						response.setResponseCode(APIUtils.RESPONSE_SERVER_ERROR);
					}
				}
			} else {
				// セッショントークンが一致しない時
				response.setResponseCode(APIUtils.RESPONSE_DEVICE_NOT_REGISTERED);
			}
			return response;
		}

	} // MyDispatcher

//--------------------------------------------------------------------------------
	private static final String ALGORITHM = "hmacSHA256";

	/**
	 * SkyWayのピア認証用のauthTokenを計算する
	 * @param context
	 * @param token
	 * @return
	 */
	private static String hmacSHA256(
		@NonNull final Context context,
		@NonNull final String token) {

		if (DEBUG) Log.v(TAG, "hmacSHA256:token=" + token);
		final SecretKeySpec secretKeySpec = new SecretKeySpec(
			Const.loadSecretKey(context).getBytes(), ALGORITHM);
		try {
			final Mac mac = Mac.getInstance(ALGORITHM);
			mac.init(secretKeySpec);
			final byte[] hash = mac.doFinal(token.getBytes());
			return Base64.encodeToString(hash, Base64.NO_WRAP);
		} catch (final NoSuchAlgorithmException e) {
			Log.w(TAG, e);
		} catch (InvalidKeyException e) {
			Log.w(TAG, e);
		}
		return "";
	}

}
