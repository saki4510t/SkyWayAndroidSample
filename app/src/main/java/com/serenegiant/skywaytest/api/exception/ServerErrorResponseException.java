package com.serenegiant.skywaytest.api.exception;

import com.serenegiant.skywaytest.api.APIUtils;

import retrofit2.HttpException;

/**
 * APIサーバーがエラーレスポンスを返したときの例外
 */
public class ServerErrorResponseException extends ApiException {
	private static final long serialVersionUID = 4088435280534657976L;
	
	/**
	 * コンストラクタ
 	 * @param message
	 * @param cause
	 */
	public ServerErrorResponseException(final String message, final Throwable cause) {
		super(message, cause);
	}
	
	/**
	 * コンストラクタ
	 * @param cause
	 */
	public ServerErrorResponseException(final Throwable cause) {
		super(cause);
	}

	public int getResponseCode() {
		final Throwable cause = getCause();
		if (cause instanceof HttpException) {
			return ((HttpException) cause).code();
		} else {
			return APIUtils.RESPONSE_UNKNOWN;
		}
	}
}
