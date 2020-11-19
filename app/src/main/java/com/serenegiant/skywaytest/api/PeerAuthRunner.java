package com.serenegiant.skywaytest.api;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.skywaytest.api.exception.ServerErrorResponseException;
import com.serenegiant.skywaytest.api.exception.ServerInternalErrorException;
import com.serenegiant.skywaytest.api.exception.ServerNotFoundException;
import com.serenegiant.skywaytest.api.exception.ServerTimeoutException;
import com.serenegiant.skywaytest.api.exception.ServerUnexpectedEmptyBodyException;
import com.serenegiant.skywaytest.api.exception.ServerUnknownResponseException;
import com.serenegiant.skywaytest.api.exception.ServerWrongResponseBodyException;
import com.serenegiant.skywaytest.api.model.PeerAuthInfo;
import com.serenegiant.skywaytest.api.model.PeerAuthResult;
import com.serenegiant.utils.ThreadPool;

import java.io.EOFException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * ピア認証APIサーバーとの接続用
 */
public class PeerAuthRunner extends APIRunner {
	private static final boolean DEBUG = true;    // set false on production
	private static final String TAG = PeerAuthRunner.class.getSimpleName();

	public interface PeerAuthResultCallback {
		public void onResult(@NonNull final PeerAuthResult result);
		public void onError(@NonNull final Throwable t);
	}

//--------------------------------------------------------------------------------
	@NonNull
	private final PeerAuthResultCallback mCallback;
	private final PeerAuthAPI mAPI;

	@Nullable
	private Call<?> mCurrentCall;

	public PeerAuthRunner(
		@NonNull final Context context,
		@NonNull final String baseUri,
		@NonNull final PeerAuthResultCallback callback) {

		super(context);
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mCallback = callback;
		mAPI = setupRetrofit(setupHttpClient(), baseUri)
			.create(PeerAuthAPI.class);
	}

//--------------------------------------------------------------------------------
	@NonNull
	private String requireSessionToken() throws IllegalStateException {
		final String sessionToken = APIUtils.getSessionToken(requireContext());
		if (TextUtils.isEmpty(sessionToken)) {
			throw new IllegalStateException("Session Token not found!");
		}
		return sessionToken;
	}

	/**
	 * ピア認証実行
	 */
	public void authorize() {
		if (DEBUG) Log.v(TAG, "authorize:");
		cancelCall();
		ThreadPool.queueEvent(new Runnable() {
			@Override
			public void run() {
				try {
					final PeerAuthInfo info = new PeerAuthInfo(
						APIUtils.getPeerId(requireContext()), requireSessionToken());
					final Call<ResponseBody> call = mAPI.getCredential(info);
					setCall(call);
					call.enqueue(new retrofit2.Callback<ResponseBody>() {
						@Override
						public void onResponse(
							@NonNull final Call<ResponseBody> call,
							@NonNull final Response<ResponseBody> response) {

							if (DEBUG) Log.v(TAG, "onResponse:response=" + response);
							setCall(null);
							if (response.isSuccessful()) {
								// レスポンスコードが200番台,300番台の時
								switch (response.code()) {
								case APIUtils.RESPONSE_OK:
									final PeerAuthResult body = as(response.body(), PeerAuthResult.class);
									if (DEBUG) Log.v(TAG, "onResponse:body=" + body);
									if (body != null) {
										if (!TextUtils.isEmpty(body.peerId)
											&& !TextUtils.isEmpty(body.authToken)
											&& (body.timestamp > 0L)
											&& (body.ttl >= 600) && (body.ttl <= 90000)) {

											mCallback.onResult(body);
										} else {
											onError(new ServerWrongResponseBodyException(response.toString()));
										}
									} else {
										// RESPONSE_OKなのにレスポンスボディが無い
										onError(new ServerUnexpectedEmptyBodyException(response.toString()));
									}
									break;
								default:
									onError(new ServerUnknownResponseException(response.toString(), response.code()));
									break;
								}
							} else {
								onError(new ServerUnknownResponseException(response.toString(), response.code()));
							}
						}

						@Override
						public void onFailure(
							@NonNull final Call<ResponseBody> call,
							@NonNull final Throwable t) {

							if (DEBUG) Log.v(TAG, "onFailure:" + t);
							setCall(null);
							if (t instanceof EOFException) {
								// レスポンスコードが200番台300番台で本来はonResponseが呼ばれるはずが
								// レスポンスボディーセットされていなくてエラーになった時
								onError(new ServerUnexpectedEmptyBodyException(t));
							} else if (t instanceof UnknownHostException) {
								onError(new ServerNotFoundException(t));
							} else if (t instanceof SocketTimeoutException) {
								onError(new ServerTimeoutException(t));
							} else {
								onError(new ServerErrorResponseException(t));
							}
						}
					});
				} catch (final Exception e) {
					setCall(null);
					onError(new ServerInternalErrorException(e));
				}
			}
		});
	}

	private void onError(@NonNull final Throwable t) {
		ThreadPool.queueEvent(new Runnable() {
			@Override
			public void run() {
				mCallback.onError(t);
			}
		});
	}
}
