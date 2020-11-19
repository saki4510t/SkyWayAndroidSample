package com.serenegiant.skywaytest.api.model;

import androidx.annotation.NonNull;

/**
 * Peer認証時にリクエストボディとして送るピア情報(ピアIDとセッショントークン)のホルダークラス
 */
public class PeerAuthInfo {
	@NonNull
	public final String peerId;
	@NonNull
	public final String sessionToken;

	/**
	 * コンストラクタ
	 * @param peerId
	 * @param sessionToken
	 */
	public PeerAuthInfo(@NonNull final String peerId, @NonNull final String sessionToken) {
		this.peerId = peerId;
		this.sessionToken = sessionToken;
	}

	@NonNull
	@Override
	public String toString() {
		return "PeerAuthInfo{" +
			"peerId='" + peerId + '\'' +
			", sessionToken='" + sessionToken + '\'' +
			'}';
	}
}
