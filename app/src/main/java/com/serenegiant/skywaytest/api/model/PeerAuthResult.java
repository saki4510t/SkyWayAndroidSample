package com.serenegiant.skywaytest.api.model;

import androidx.annotation.NonNull;
import io.skyway.Peer.PeerCredential;

/**
 * ピア認証APIを叩いときのレスポンスボディ用ホルダークラス
 */
public class PeerAuthResult {
	@NonNull
	public final String peerId;
	@NonNull
	public final String authToken;
	public final long timestamp;
	public final long ttl;

	/**
	 * コンストラクタ
	 * @param authToken
	 * @param timestamp
	 * @param ttl
	 */
	public PeerAuthResult(@NonNull final String peerId,
						  @NonNull final String authToken, final long timestamp, final long ttl) {

		this.peerId = peerId;
		this.authToken = authToken;
		this.timestamp = timestamp;
		this.ttl = ttl;
	}

	/**
	 * SkyWayのPeerCredentialオブジェクトとして取得
	 * @return
	 */
	@NonNull
	public PeerCredential as() {
		final PeerCredential result = new PeerCredential();
		result.authToken = authToken;
		result.timestamp = timestamp;
		result.ttl = ttl;
		return result;
	}

	@NonNull
	@Override
	public String toString() {
		return "PeerAuthResult{" +
			"peerId='" + peerId + '\'' +
			", authToken='" + authToken + '\'' +
			", timestamp=" + timestamp +
			", ttl=" + ttl +
			'}';
	}
}
