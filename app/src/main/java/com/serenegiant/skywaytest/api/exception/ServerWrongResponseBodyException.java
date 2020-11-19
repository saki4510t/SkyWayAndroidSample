/*
 * Copyright (c) 2020.  t_saki@serenegiant.com All rights reserved.
 */

package com.serenegiant.skywaytest.api.exception;

/**
 * APIサーバーからの応答が間違っている場合(response bodyの値が欠損しているなど)
 */
public class ServerWrongResponseBodyException extends ApiException {
	private static final long serialVersionUID = -120981838572597958L;

	public ServerWrongResponseBodyException() {
	}

	public ServerWrongResponseBodyException(final String message) {
		super(message);
	}

	public ServerWrongResponseBodyException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public ServerWrongResponseBodyException(final Throwable cause) {
		super(cause);
	}
}
