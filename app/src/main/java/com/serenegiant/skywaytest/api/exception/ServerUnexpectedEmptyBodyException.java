/*
 * Copyright (c) 2020.  t_saki@serenegiant.com All rights reserved.
 */

package com.serenegiant.skywaytest.api.exception;

/**
 * API体後と異なって想定外にレスポンスボディが空だった時
 */
public class ServerUnexpectedEmptyBodyException extends ApiException {
	private static final long serialVersionUID = 3915837429540380989L;

	public ServerUnexpectedEmptyBodyException() {
	}

	public ServerUnexpectedEmptyBodyException(final String message) {
		super(message);
	}

	public ServerUnexpectedEmptyBodyException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public ServerUnexpectedEmptyBodyException(final Throwable cause) {
		super(cause);
	}
}
