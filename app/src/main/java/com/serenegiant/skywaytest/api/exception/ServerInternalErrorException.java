/*
 * Copyright (c) 2020.  t_saki@serenegiant.com All rights reserved.
 */

package com.serenegiant.skywaytest.api.exception;

/**
 * その他不明なエラー
 */
public class ServerInternalErrorException extends ApiException {
	private static final long serialVersionUID = 8173583655984850752L;

	public ServerInternalErrorException() {
	}

	public ServerInternalErrorException(final String message) {
		super(message);
	}

	public ServerInternalErrorException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public ServerInternalErrorException(final Throwable cause) {
		super(cause);
	}
}
