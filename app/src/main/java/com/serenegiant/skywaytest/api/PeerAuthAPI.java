package com.serenegiant.skywaytest.api;

import com.serenegiant.skywaytest.api.model.PeerAuthInfo;

import androidx.annotation.NonNull;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * ピア認証API定義
 */
public interface PeerAuthAPI {

	@POST("authenticate")
	Call<ResponseBody> getCredential(@Body @NonNull final PeerAuthInfo peer);
}
