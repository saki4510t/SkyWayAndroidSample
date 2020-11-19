package com.serenegiant.skywaytest.api.exception;

/**
 * APIサーバーが応答せずタイム・アウトしたときの例外
 */
public class ServerTimeoutException extends ApiException {
	private static final long serialVersionUID = -1114734565454623276L;
	
	/**
	 * コンストラクタ
 	 */
	public ServerTimeoutException() {
	}
	
	/**
	 * コンストラクタ
 	 * @param message
	 */
	public ServerTimeoutException(final String message) {
		super(message);
	}
	
	/**
	 * コンストラクタ
 	 * @param message
	 * @param cause
	 */
	public ServerTimeoutException(final String message, final Throwable cause) {
		super(message, cause);
	}
	
	/**
	 * コンストラクタ
 	 * @param cause
	 */
	public ServerTimeoutException(final Throwable cause) {
		super(cause);
	}
}
