package com.serenegiant.skywaytest.api.exception;

/**
 * APIアクセス時のエラー通知例外の基底クラス
 */
public class ApiException extends RuntimeException {
	private static final long serialVersionUID = 8793503979792089345L;
	
	/**
	 * コンストラクタ
	 */
	public ApiException() {
	}
	
	/**
	 * コンストラクタ
 	 * @param message
	 */
	public ApiException(final String message) {
		super(message);
	}
	
	/**
	 * コンストラクタ
 	 * @param message
	 * @param cause
	 */
	public ApiException(final String message, final Throwable cause) {
		super(message, cause);
	}
	
	/**
	 * コンストラクタ
 	 * @param cause
	 */
	public ApiException(final Throwable cause) {
		super(cause);
	}
	
}
