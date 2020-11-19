/*
 * Copyright (c) 2020.  t_saki@serenegiant.com All rights reserved.
 */

package com.serenegiant.skywaytest.api.exception;

/**
 * onResponseが呼ばれたときにサーバーが正常応答として未知の応答をした時
 */
public class ServerUnknownResponseException extends ApiException {
	private static final long serialVersionUID = -2553801313921135615L;

	public final int responseCode;

	public ServerUnknownResponseException(final int responseCode) {
		this.responseCode = responseCode;
	}

	public ServerUnknownResponseException(final String message, final int responseCode) {
		super(message);
		this.responseCode = responseCode;
	}

	public ServerUnknownResponseException(final String message, final int responseCode, final Throwable cause) {
		super(message, cause);
		this.responseCode = responseCode;
	}

	public ServerUnknownResponseException(final int responseCode, final Throwable cause) {
		super(cause);
		this.responseCode = responseCode;
	}
}
