/*
 * Copyright (c) 2018.  saki t_saki@serenegiant.com
 */

package com.serenegiant.skywaytest.api;

import android.util.Log;

import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import okhttp3.OkHttpClient;

/**
 * Hold SingletonOkHttpClient to handle it as singleton
 */
public class SingletonOkHttpClient {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = SingletonOkHttpClient.class.getSimpleName();

	/** connection time out in millis */
	public static final long HTTP_CONNECT_TIMEOUT_MS = 3000;
	/** read time out in millis */
	public static final long HTTP_READ_TIMEOUT_MS = 3000;
	/** write time out in millis */
	public static final long HTTP_WRITE_TIMEOUT_MS = 3000;

	private static final SingletonOkHttpClient sInstance
		= new SingletonOkHttpClient();
	
	/**
	 * get singleton OkHttpClient
	 * @return singleton OkHttpClient instance
	 */
	@NonNull
	public static synchronized OkHttpClient getsInstance() {
		return sInstance.getClient();
	}
	
	/**
	 * get new OkHttpClient.Builder from singleton OkHttpClient
	 * @return new OkHttpClient.Builder based singleton OkHttpClient instance
	 */
	@NonNull
	public static synchronized OkHttpClient.Builder getBuilder() {
		return sInstance.getClient().newBuilder();
	}

	/**
	 * singleton OkHttpClient instance
	 */
	private final OkHttpClient mOkHttpClient;
	
	/**
	 * Constructor
	 */
	private SingletonOkHttpClient() {
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		final OkHttpClient.Builder builder = new OkHttpClient.Builder();
		// デフォルトではタイムアウト競ってだけ行う。必要に応じてoverride可能
		builder
			.connectTimeout(HTTP_CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)	// 接続タイムアウト
			.readTimeout(HTTP_READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)		// 読み込みタイムアウト
			.writeTimeout(HTTP_WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS);	// 書き込みタイムアウト
		
		mOkHttpClient = builder.build();
	}
	
	/**
	 * get OkHttpClient as singleton
	 * @return
	 */
	@NonNull
	private OkHttpClient getClient() {
		if (DEBUG) Log.v(TAG, "getClient:");
		return mOkHttpClient;
	}
	
}
