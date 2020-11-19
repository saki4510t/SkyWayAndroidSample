package com.serenegiant.skywaytest.api.exception;

/**
 * サーバーが見つからない時
 */
public class ServerNotFoundException extends ApiException {
	private static final long serialVersionUID = -447676083069040253L;

	public ServerNotFoundException() {
	}

	public ServerNotFoundException(final String message) {
		super(message);
	}

	public ServerNotFoundException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public ServerNotFoundException(final Throwable cause) {
		super(cause);
	}
}
