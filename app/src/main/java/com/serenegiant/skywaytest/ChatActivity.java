package com.serenegiant.skywaytest;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.serenegiant.skywaytest.api.APIUtils;
import com.serenegiant.skywaytest.api.PeerAuthMockServer;
import com.serenegiant.skywaytest.api.PeerAuthRunner;
import com.serenegiant.skywaytest.api.model.PeerAuthResult;
import com.serenegiant.utils.ThreadPool;

import org.json.JSONArray;

import java.util.ArrayList;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import io.skyway.Peer.Browser.Canvas;
import io.skyway.Peer.Browser.MediaConstraints;
import io.skyway.Peer.Browser.MediaStream;
import io.skyway.Peer.Browser.Navigator;
import io.skyway.Peer.CallOption;
import io.skyway.Peer.MediaConnection;
import io.skyway.Peer.OnCallback;
import io.skyway.Peer.Peer;
import io.skyway.Peer.PeerCredential;
import io.skyway.Peer.PeerError;
import io.skyway.Peer.PeerOption;
import io.skyway.Peer.Room;
import io.skyway.Peer.RoomOption;

import static com.serenegiant.skywaytest.Const.DOMAIN;
import static com.serenegiant.skywaytest.Const.getAPIKey;
import static com.serenegiant.skywaytest.Const.saveAPIKey;

abstract class ChatActivity extends Activity {
	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = ChatActivity.class.getSimpleName();

	public interface OnListAllPeersCallback {
		public void onListAllPeers(@NonNull final ArrayList<String> peers);
	}

	@NonNull
	protected final Object mSync = new Object();
	private PeerAuthRunner mRunner;
	private PeerAuthMockServer mServer;
	private Peer _peer;
	private String _strOwnId;
	protected MediaStream _localStream;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// hide window title
		final Window wnd = getWindow();
		wnd.addFlags(Window.FEATURE_NO_TITLE);
	}

	@Override
	protected void onPostCreate(@Nullable final Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		authorize();
	}

	//
	// onRequestPermissionResult
	//
	@Override
	public void onRequestPermissionsResult(
		final int requestCode,
		@NonNull final String[] permissions,
		@NonNull final int[] grantResults) {

		if (requestCode == 0) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				startLocalStream();
			} else {
				Toast.makeText(this, "Failed to access the camera and microphone.\nclick allow when asked for permission.", Toast.LENGTH_LONG).show();
			}
		}
	}

	//
	// Activity Lifecycle
	//
	@Override
	protected void onStart() {
		super.onStart();

		// Disable Sleep and Screen Lock
		final Window wnd = getWindow();
		wnd.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		wnd.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Set volume control stream type to WebRTC audio.
		setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
	}

	@Override
	protected void onPause() {
		// Set default volume control stream type.
		setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
		super.onPause();
	}

	@Override
	protected void onStop() {
		// Enable Sleep and Screen Lock
		Window wnd = getWindow();
		wnd.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		wnd.clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		releasePeerAuthRunner();
		destroyPeer();
		super.onDestroy();
	}

	@Nullable
	protected String peerId() {
		return _strOwnId;
	}

	protected MediaConnection call(
		@NonNull final String peerId,
		final MediaStream stream,
		final CallOption option) throws IllegalStateException {

		if (!isConnected()) {
			throw new IllegalStateException();
		}
		return _peer.call(peerId, stream, option);
	}

	protected Room joinRoom(
		@NonNull final String roomName,
		@NonNull final RoomOption option) throws IllegalStateException {

		if (!isConnected()) {
			throw new IllegalStateException();
		}
		return _peer.joinRoom(roomName, option);
	}

	protected abstract void closeRemoteStream();

	protected void switchCamera() {
		if (_localStream != null) {
			final boolean result = _localStream.switchCamera();
			if (DEBUG) Log.v(TAG, "switchCamera:" + (result ? "success" : "failed"));
		}
	}

	protected void listAllPeers(@NonNull final OnListAllPeersCallback callback) {
		final ArrayList<String> resut = new ArrayList<>();
		final String ownId = peerId();
		if (!isConnected() || TextUtils.isEmpty(ownId)) {
			Toast.makeText(this, "Your PeerID is null or invalid.", Toast.LENGTH_SHORT).show();
			callback.onListAllPeers(resut);
			return;
		}
		// Get all IDs connected to the server
		_peer.listAllPeers(new OnCallback() {
			@Override
			public void onCallback(final Object object) {
				if (!(object instanceof JSONArray)) {
					callback.onListAllPeers(resut);
					return;
				}

				final JSONArray peers = (JSONArray) object;
				// Exclude own ID
				for (int i = 0; peers.length() > i; i++) {
					try {
						final String peerId = peers.getString(i);
						if (!ownId.equals(peerId)) {
							resut.add(peerId);
						}
					} catch (final Exception e) {
						e.printStackTrace();
					}
				}
				callback.onListAllPeers(resut);
			}
		});
	}

	/**
	 * ピア認証実行
	 */
	private void authorize() {
		if (DEBUG) Log.v(TAG, "authorize:");
		if (Const.isPeerAuthEnabled(this)) {
			ThreadPool.queueEvent(new Runnable() {
				@Override
				public void run() {
					synchronized (mSync) {
						// 今回はアプリ側の実装サンプルなのでピア認証APIサーバーはOkhttp3のモックサーバーで代用
						mServer = new PeerAuthMockServer(ChatActivity.this);
						// 本来はここでインターネット上でセキュアに実行されているピア認証APIサーバーのurlを引き渡して実行する
						mRunner = new PeerAuthRunner(ChatActivity.this, mServer.getBaseUri(),
							new PeerAuthRunner.PeerAuthResultCallback() {

							@Override
							public void onResult(@NonNull final PeerAuthResult result) {
								if (isConnected()) {
									// 既に接続しているときはクレデンシャルを更新
									_peer.updateCredential(result.as());
								} else {
									initPeer(result.as());
								}
								releasePeerAuthRunner();
							}

							@Override
							public void onError(@NonNull final Throwable t) {
								releasePeerAuthRunner();
								showErrorAndFinish("Peer authentication failed with error," + t);
							}
						});
						mRunner.authorize();
					}
				}
			});
		} else {
			initPeer(null);
		}
	}

	/**
	 * ピア認証用のAPIRunnerインスタンスを開放する
	 */
	private void releasePeerAuthRunner() {
		if (DEBUG) Log.v(TAG, "releasePeerAuthRunner:");
		synchronized (mSync) {
			if (mRunner != null) {
				mRunner.release();
				mRunner = null;
			}
		}
	}

	private void initPeer(@Nullable final PeerCredential credential) {
		String apiKey = null;
		try {
			apiKey = getAPIKey(this);
		} catch (final Exception e) {
			Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
			if (!BuildConfig.DEBUG) {
				finish();
				return;
			}
		}
		if (TextUtils.isEmpty(apiKey)) {
			finish();
		}
		if (DEBUG) Log.v(TAG, "initPeer:apiKey=" + apiKey);
		if (Const.isPeerAuthEnabled(this) && (credential == null)) {
			showErrorAndFinish("Unexpectedly PeerCredential is not set.");
		}

		//
		// Initialize Peer
		//
		final PeerOption option = new PeerOption();
		option.key = apiKey;
		option.domain = DOMAIN;
		option.debug = Peer.DebugLevelEnum.ALL_LOGS;
		if (Const.isPeerAuthEnabled(this)) {
			// ピア認証する時
			// オプションにクレデンシャルをセットしピアIDを指定してPeerインスタンスを生成
			option.credential = credential;
			_peer = new Peer(this, APIUtils.getPeerId(this), option);
		} else {
			// ピア認証しない時
			// このときにもピアIDを指定することはできるけど
			// ピアIDを指定しなければ自動的に割り振られる
			_peer = new Peer(this, option);
		}

		//
		// Set Peer event callbacks
		//
		setPeerCallback(_peer);
	}

	//
	// Get a local MediaStream & show it
	//
	private void startLocalStream() {
		final MediaConstraints constraints = new MediaConstraints();
		constraints.maxWidth = 960;
		constraints.maxHeight = 540;
		constraints.cameraPosition = MediaConstraints.CameraPositionEnum.FRONT;

		Navigator.initialize(_peer);
		_localStream = Navigator.getUserMedia(constraints);
		final Canvas canvas = findViewById(R.id.svLocalView);
		_localStream.addVideoRenderer(canvas, 0);
	}

	//
	// Clean up objects
	//
	private void destroyPeer() {
		closeRemoteStream();

		if (null != _localStream) {
			final Canvas canvas = findViewById(R.id.svLocalView);
			_localStream.removeVideoRenderer(canvas, 0);
			_localStream.close();
		}

		Navigator.terminate();

		if (_peer != null) {
			unsetPeerCallback(_peer);
//			if (!_peer.isDisconnected()) {
//				_peer.disconnect();
//			}

			if (!_peer.isDestroyed()) {
				_peer.destroy();
			}

			_peer = null;
		}
	}

	@CallSuper
	protected void setPeerCallback(@NonNull final Peer peer) {
		// OPEN
		_peer.on(Peer.PeerEventEnum.OPEN, new OnCallback() {
			@Override
			public void onCallback(final Object object) {

				saveAPIKey(ChatActivity.this, getAPIKey(ChatActivity.this));
				// Show my ID
				_strOwnId = (String) object;
				final TextView tvOwnId = findViewById(R.id.tvOwnId);
				tvOwnId.setText(_strOwnId);

				if (Const.isPeerAuthEnabled(ChatActivity.this)) {
					final String peerId = APIUtils.getPeerId(ChatActivity.this);
					if (!_strOwnId.equals(peerId)) {
						// ここには来ないはず
						Toast.makeText(getApplicationContext(),
							 "Unexpected PeerID,expected=" + peerId + ",received=" + _strOwnId,
							 Toast.LENGTH_SHORT).show();
					}
				}

				// Request permissions
				if (ContextCompat.checkSelfPermission(ChatActivity.this,
					Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(ChatActivity.this,
					Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
					ActivityCompat.requestPermissions(ChatActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 0);
				} else {
					// Get a local MediaStream & show it
					startLocalStream();
				}
			}
		});

		// ERROR
		_peer.on(Peer.PeerEventEnum.ERROR, new OnCallback() {
			@Override
			public void onCallback(final Object object) {
				final PeerError error = (PeerError) object;
				Log.d(TAG, "[On/Error]" + error);
				showErrorAndFinish("Error on connecting peer(API key would be wrong)," + error);
			}
		});

		// CLOSE
		_peer.on(Peer.PeerEventEnum.CLOSE, new OnCallback() {
			@Override
			public void onCallback(final Object object) {
				Log.d(TAG, "[On/Close]");
			}
		});

//		// DISCONNECTED
//		_peer.on(Peer.PeerEventEnum.DISCONNECTED, new OnCallback() {
//			@Override
//			public void onCallback(final Object object) {
//				Log.d(TAG, "[On/Disconnected]");
//			}
//		});

		// 認証トークンの有効期限がまもなく切れる時
		_peer.on(Peer.PeerEventEnum.AUTH_EXPIRES_IN, new OnCallback() {
			@Override
			public void onCallback(final Object object) {
				if (DEBUG) Log.v(TAG, "onCallback:PeerEventEnum.AUTH_EXPIRES_IN," + object);
				if (Const.isPeerAuthEnabled(ChatActivity.this)) {
					// ピア認証を行う時
					authorize();
				}
			}
		});
	}

	@CallSuper
	protected void unsetPeerCallback(@NonNull final Peer peer) {
		peer.on(Peer.PeerEventEnum.OPEN, null);
		peer.on(Peer.PeerEventEnum.CONNECTION, null);
		peer.on(Peer.PeerEventEnum.CLOSE, null);
//		peer.on(Peer.PeerEventEnum.DISCONNECTED, null);
		peer.on(Peer.PeerEventEnum.ERROR, null);
	}

	/**
	 * ピア接続中かどうか
	 * @return
	 */
	protected boolean isConnected() {
		synchronized (mSync) {
			return (_peer != null) && !TextUtils.isEmpty(_strOwnId);
		}
	}

	protected void showErrorAndFinish(@NonNull final CharSequence msg) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
				finish();
			}
		});
	}

}
