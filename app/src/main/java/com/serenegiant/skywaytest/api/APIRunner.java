package com.serenegiant.skywaytest.api;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.DateTypeAdapter;
import com.serenegiant.skywaytest.BuildConfig;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public abstract class APIRunner {
	private static final boolean DEBUG = false;    // set false on production
	private static final String TAG = APIRunner.class.getSimpleName();

	/**
	 * connection time out in millis
	 */
	private static final long HTTP_CONNECT_TIMEOUT_MS
		= SingletonOkHttpClient.HTTP_CONNECT_TIMEOUT_MS;
	/**
	 * read time out in millis
	 */
	private static final long HTTP_READ_TIMEOUT_MS
		= SingletonOkHttpClient.HTTP_READ_TIMEOUT_MS;
	/**
	 * write time out in millis
	 */
	private static final long HTTP_WRITE_TIMEOUT_MS
		= SingletonOkHttpClient.HTTP_WRITE_TIMEOUT_MS;

//--------------------------------------------------------------------------------
	@NonNull
	protected final Object mSync = new Object();
	@NonNull
	private final WeakReference<Context> mWeakContext;
	@NonNull
	protected final Gson mGson;

	@Nullable
	private Call<?> mCurrentCall;

	public APIRunner(
		@NonNull final Context context) {

		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mWeakContext = new WeakReference<>(context);
		mGson = new GsonBuilder()
			.setFieldNamingStrategy(GsonFieldNamingPolicy.LOWER_CAMEL_CASE)
			.registerTypeAdapter(Date.class, new DateTypeAdapter())
			.create();

	}

	/**
	 * デストラクタ
	 *
	 * @throws Throwable
	 */
	@Override
	protected void finalize() throws Throwable {
		try {
			release();
		} finally {
			super.finalize();
		}
	}

	public void release() {
		if (DEBUG) Log.v(TAG, "release:");
		cancelCall();
	}

	/**
	 * cancel call if call is in progress
	 */
	public void cancelCall() {
		if (DEBUG) Log.v(TAG, "cancelCall:");
		synchronized (mSync) {
			if ((mCurrentCall != null) && !mCurrentCall.isCanceled()) {
				try {
					mCurrentCall.cancel();
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
			}
			mCurrentCall = null;
		}
	}
//--------------------------------------------------------------------------------
	/**
	 * get Context that this instance is related.
	 *
	 * @return Context
	 */
	@Nullable
	protected Context getContext() {
		return mWeakContext.get();
	}

	@NonNull
	protected Context requireContext() throws IllegalStateException {
		final Context result = getContext();
		if (result == null) {
			throw new IllegalStateException("invalid context, already released?");
		}
		return result;
	}

	/**
	 * ResponseBodyから指定した型のオブジェクトの読み込みを試みる
	 * @param body
	 * @param clazz
	 * @param <T>
	 * @return
	 */
	@Nullable
	protected <T> T as(@Nullable final ResponseBody body, Class<? extends  T> clazz) {
		String bodyString = null;
		try {
			bodyString = body != null ? body.string() : null;
			return bodyString != null
				? mGson.fromJson(bodyString, clazz)
				: null;
		} catch (final Exception e) {
			if (DEBUG) Log.w(TAG, "as:" + body + ",/" + bodyString, e);
			return null;
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * set call that is currently in progress
	 *
	 * @param call
	 */
	protected void setCall(@Nullable final Call<?> call) {
		if (DEBUG) Log.v(TAG, "setCall:" + call);
		synchronized (mSync) {
			mCurrentCall = call;
		}
	}

	/**
	 * 進行中のCallがセットされているかどうか
	 * @return
	 */
	protected boolean hasPendingCall() {
		synchronized (mSync) {
			return mCurrentCall != null;
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * APIサーバーとの通信用にOkHttpClientを設定する
	 *
	 * @return
	 */
	protected OkHttpClient setupHttpClient() {
		if (DEBUG) Log.v(TAG, "setupHttpClient:");
		final OkHttpClient.Builder builder = SingletonOkHttpClient.getBuilder();
		builder
			.addInterceptor(new Interceptor() {    // ヘッダーの設定
				@NonNull
				@Override
				public okhttp3.Response intercept(@NonNull Chain chain)
					throws IOException {

					final Request original = chain.request();
					// header設定
					final Request request = original.newBuilder()
						.header("Accept", "application/json")
						.method(original.method(), original.body())
						.build();

					okhttp3.Response response = chain.proceed(request);
					return response;
				}
			})
			.connectTimeout(HTTP_CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)	// 接続タイムアウト
			.readTimeout(HTTP_READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)		// 読み込みタイムアウト
			.writeTimeout(HTTP_WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS);	// 書き込みタイムアウト
		if (DEBUG || BuildConfig.DEBUG) {
			// ログ出力設定
			final HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
			logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
			builder.addInterceptor(logging);
		}
//		// MockServerはHttps接続の設定をしていないのでコメントに
//		builder.connectionSpecs(Collections.singletonList(
//			new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)	// TLS_1_3, TLS_1_2, TLS_1_1, TLS_1_0
//				.cipherSuites(
//					CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
//					CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
//					CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256)
//				.build()
//			)
//		);
		return builder.build();
	}

	/**
	 * APIサーバーとの通信用にRetrofit2を設定する
	 *
	 * @param client
	 * @param baseUrl
	 * @return
	 */
	protected Retrofit setupRetrofit(
		@NonNull final OkHttpClient client,
		@NonNull final String baseUrl) {

		if (DEBUG) Log.v(TAG, "setupRetrofit:baseUrl=" + baseUrl);
		// JSONのパーサーとしてGsonを使う
		return new Retrofit.Builder()
			.baseUrl(baseUrl)
			.addConverterFactory(GsonConverterFactory.create(mGson))
			.client(client)
			.build();
	}
}
