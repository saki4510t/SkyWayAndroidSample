package com.serenegiant.skywaytest;

import android.app.FragmentManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import io.skyway.Peer.Browser.Canvas;
import io.skyway.Peer.Browser.MediaStream;
import io.skyway.Peer.CallOption;
import io.skyway.Peer.MediaConnection;
import io.skyway.Peer.OnCallback;
import io.skyway.Peer.Peer;
import io.skyway.Peer.PeerError;

public class P2pVideoChatActivity extends ChatActivity {
	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = P2pVideoChatActivity.class.getSimpleName();

	private MediaStream _remoteStream;
	private MediaConnection _mediaConnection;
	private boolean _talking;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_p2p_video_chat);

		//
		// Set GUI event listeners
		//

		// Set GUI event listner for Button (make/hang up a call)
		final Button btnAction = findViewById(R.id.btnAction);
		btnAction.setEnabled(true);
		btnAction.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				v.setEnabled(false);

				if (isTalking()) {
					// Hang up a call
					closeRemoteStream();
				} else {
					// Select remote peer & make a call
					showPeerIDs();
				}

				v.setEnabled(true);
			}
		});

		// Action for switchCameraButton
		Button switchCameraAction = findViewById(R.id.switchCameraAction);
		switchCameraAction.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				switchCamera();
			}
		});
	}

	@Override
	@CallSuper
	protected void setPeerCallback(@NonNull final Peer peer) {
		super.setPeerCallback(peer);
		// CALL (Incoming call)
		peer.on(Peer.PeerEventEnum.CALL, new OnCallback() {
			@Override
			public void onCallback(final Object object) {
				if (!(object instanceof MediaConnection)) {
					return;
				}

				final MediaConnection mediaConnection = (MediaConnection) object;

				if (onCall(mediaConnection)) {
					mediaConnection.answer(_localStream);
				}
			}
		});
	}

	@Override
	@CallSuper
	protected void unsetPeerCallback(@NonNull final Peer peer) {
		peer.on(Peer.PeerEventEnum.CALL, null);
		super.unsetPeerCallback(peer);
	}

	/**
	 * Close a remote MediaStream
	 */
	@Override
	protected void closeRemoteStream() {
		if (_remoteStream == null) {
			return;
		}

		final Canvas canvas = findViewById(R.id.svRemoteView);
		_remoteStream.removeVideoRenderer(canvas, 0);
		_remoteStream.close();

		if (_mediaConnection != null) {
			unsetMediaCallbacks(_mediaConnection);
			if (_mediaConnection.isOpen()) {
				_mediaConnection.close(true);
			}
			_mediaConnection = null;
		}
		_talking = false;
		updateActionButtonTitle();
	}

	/**
	 * 通話中かどうか
	 * @return
	 */
	private boolean isTalking() {
		synchronized (mSync) {
			return isConnected() && _talking;
		}
	}

	/**
	 * p2p通話が着袴したときの処理
	 * @param mediaConnection
	 * @return true: 着呼を許可する, false: 着呼を拒否する
	 */
	private boolean onCall(@NonNull final MediaConnection mediaConnection) {
		_mediaConnection = mediaConnection;
		setMediaCallbacks(_mediaConnection);
		_talking = true;
		updateActionButtonTitle();
		return true;
	}

	/**
	 * Set callbacks for MediaConnection.MediaEvents
	 */
	private void setMediaCallbacks(@NonNull final MediaConnection mediaConnection) {
		// 相手のカメラ映像・マイク音声を受信したときのコールバックを設定
		mediaConnection.on(MediaConnection.MediaEventEnum.STREAM, new OnCallback() {
			@Override
			public void onCallback(final Object object) {
				_remoteStream = (MediaStream) object;
				Canvas canvas = findViewById(R.id.svRemoteView);
				_remoteStream.addVideoRenderer(canvas, 0);
			}
		});
		// 相手がメディアコネクションの切断処理を実行し、実際に切断されたときのコールバックを設定
		mediaConnection.on(MediaConnection.MediaEventEnum.CLOSE, new OnCallback() {
			@Override
			public void onCallback(final Object object) {
				closeRemoteStream();
				updateActionButtonTitle();
			}
		});

		// MediaConnectionでエラーが起こったときのコールバックを設定
		mediaConnection.on(MediaConnection.MediaEventEnum.ERROR, new OnCallback() {
			@Override
			public void onCallback(final Object object) {
				PeerError error = (PeerError) object;
				Log.d(TAG, "[On/MediaError]" + error);
			}
		});
	}

	/**
	 * Unset callbacks for MediaConnection.MediaEvents
	 */
	private void unsetMediaCallbacks(@NonNull final MediaConnection mediaConnection) {
		mediaConnection.on(MediaConnection.MediaEventEnum.STREAM, null);
		mediaConnection.on(MediaConnection.MediaEventEnum.CLOSE, null);
		mediaConnection.on(MediaConnection.MediaEventEnum.ERROR, null);
	}

	/**
	 * p2p通話開始
	 * @param remotePeerId
	 */
	private void startCall(@NonNull final String remotePeerId) {
		if (!isConnected()) {
			return;
		}
		if (_mediaConnection != null) {
			_mediaConnection.close();
		}

		final CallOption option = new CallOption();
		_mediaConnection = call(remotePeerId, _localStream, option);

		if (_mediaConnection != null) {
			setMediaCallbacks(_mediaConnection);
			_talking = true;
		}
		updateActionButtonTitle();
	}

	/**
	 * Listing all peers
	 */
	private void showPeerIDs() {
		if (!isConnected() || TextUtils.isEmpty(peerId())) {
			Toast.makeText(this, "Your PeerID is null or invalid.", Toast.LENGTH_SHORT).show();
			return;
		}

		listAllPeers(new OnListAllPeersCallback() {
			@Override
			public void onListAllPeers(@NonNull final ArrayList<String> peers) {
				// Show IDs using DialogFragment
				if (!peers.isEmpty()) {
					FragmentManager mgr = getFragmentManager();
					PeerListDialogFragment dialog = new PeerListDialogFragment();
					dialog.setListener(
						new PeerListDialogFragment.PeerListDialogFragmentListener() {
							@Override
							public void onItemClick(final String item) {
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										startCall(item);
									}
								});
							}
						});
					dialog.setItems(peers);
					dialog.show(mgr, "peerlist");
				} else {
					Toast.makeText(P2pVideoChatActivity.this.getApplicationContext(),
					"PeerID list (other than your ID) is empty.", Toast.LENGTH_SHORT).show();
				}
			}
		});
	}

	/**
	 * Update actionButton title
	 */
	private void updateActionButtonTitle() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final Button btnAction = findViewById(R.id.btnAction);
				if (btnAction != null) {
					if (!isTalking()) {
						btnAction.setText(R.string.call);
					} else {
						btnAction.setText(R.string.hangup);
					}
				}
			}
		});
	}

}

