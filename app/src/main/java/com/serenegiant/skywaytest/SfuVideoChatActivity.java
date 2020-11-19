package com.serenegiant.skywaytest;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.*;

import com.serenegiant.utils.KeyboardUtils;

import io.skyway.Peer.*;
import io.skyway.Peer.Browser.MediaStream;

import static com.serenegiant.skywaytest.Const.*;

/**
 *
 * MainActivity.java
 * ECL WebRTC sfu video-chat sample
 *
 */
public class SfuVideoChatActivity extends ChatActivity {
	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = SfuVideoChatActivity.class.getSimpleName();

	private Room            _room;
	private RemoteViewAdapter   _adapter;
	private boolean			_bConnected;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sfu_video_chat);

		final Activity activity = this;

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
		if (DEBUG) Log.v(TAG, "onCreate:apiKey=" + apiKey);

		//
		// Set GUI event listeners
		//
		final Button btnAction = findViewById(R.id.btnAction);
		btnAction.setEnabled(true);
		btnAction.setOnClickListener(new View.OnClickListener()	{
			@Override
			public void onClick(View v)	{
				v.setEnabled(false);

				if (!_bConnected) {
					// Join room
					joinRoom();
				}
				else {
					// Leave room
					leaveRoom();
				}

				v.setEnabled(true);
			}
		});

		final Button switchCameraAction = findViewById(R.id.switchCameraAction);
		switchCameraAction.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v)	{
				if (null != _localStream){
					final boolean result = _localStream.switchCamera();
					if (DEBUG) Log.v(TAG, "switchCamera:" + (result ? "success" : "failed"));
				}

			}
		});

		//
		// Set GridView for Remote Video Stream
		//
		final GridView grdRemote = findViewById(R.id.grdRemote);
		if (grdRemote != null) {
			_adapter = new RemoteViewAdapter(this);
			grdRemote.setAdapter(_adapter);
		}

		final String room
			= activity.getPreferences(Context.MODE_PRIVATE)
				.getString(PREF_KEY_ROOM_SFU, null);
		final EditText edtRoomName = findViewById(R.id.txRoomName);
		edtRoomName.setText(room);
	}

	@Override
	protected void closeRemoteStream() {
		leaveRoom();
	}

	/**
	 * Join the room
	 */
	private void joinRoom() {
		if (!isConnected()) {
			Toast.makeText(this, "Your PeerID is null or invalid.", Toast.LENGTH_SHORT).show();
			return;
		}

		// Get room name
		final EditText edtRoomName = findViewById(R.id.txRoomName);
		String roomName = edtRoomName.getText().toString();
		if (TextUtils.isEmpty(roomName)) {
			Toast.makeText(this, "You should input room name.", Toast.LENGTH_SHORT).show();
			return;
		}
		KeyboardUtils.hide(edtRoomName);
		getPreferences(Context.MODE_PRIVATE)
			.edit()
			.putString(PREF_KEY_ROOM_SFU, roomName)
			.apply();

		final RoomOption option = new RoomOption();
		option.mode = RoomOption.RoomModeEnum.SFU;
		option.stream = _localStream;

		// Join Room
		_room = joinRoom(roomName, option);
		_bConnected = true;

		//
		// Set Callbacks
		//
		_room.on(Room.RoomEventEnum.OPEN, new OnCallback() {
			@Override
			public void onCallback(final Object object) {
				if (!(object instanceof String)) return;

				String roomName = (String)object;
				Log.i(TAG, "Enter Room: " + roomName);
				Toast.makeText(SfuVideoChatActivity.this, "Enter Room: " + roomName, Toast.LENGTH_SHORT).show();
			}
		});

		_room.on(Room.RoomEventEnum.CLOSE, new OnCallback() {
			@Override
			public void onCallback(final Object object) {
				String roomName = (String)object;
				Log.i(TAG, "Leave Room: " + roomName);
				Toast.makeText(SfuVideoChatActivity.this, "Leave Room: " + roomName, Toast.LENGTH_LONG).show();

				// Remove all streams
				_adapter.removeAllRenderers();

				// Unset callbacks
				_room.on(Room.RoomEventEnum.OPEN, null);
				_room.on(Room.RoomEventEnum.CLOSE, null);
				_room.on(Room.RoomEventEnum.ERROR, null);
				_room.on(Room.RoomEventEnum.PEER_JOIN, null);
				_room.on(Room.RoomEventEnum.PEER_LEAVE, null);
				_room.on(Room.RoomEventEnum.STREAM, null);
				_room.on(Room.RoomEventEnum.REMOVE_STREAM, null);

				_room = null;
				_bConnected = false;
				updateActionButtonTitle();
			}
		});

		_room.on(Room.RoomEventEnum.ERROR, new OnCallback() {
			@Override
			public void onCallback(final Object object) {
				PeerError error = (PeerError) object;
				Log.d(TAG, "RoomEventEnum.ERROR:" + error);
			}
		});

		_room.on(Room.RoomEventEnum.PEER_JOIN, new OnCallback() {
			@Override
			public void onCallback(final Object object) {
				Log.d(TAG, "RoomEventEnum.PEER_JOIN:");

				if (!(object instanceof String)) return;

				String peerId = (String)object;
				Log.i(TAG, "Join Room: " + peerId);
				Toast.makeText(SfuVideoChatActivity.this, peerId + " has joined.", Toast.LENGTH_LONG).show();
			}
		});

		_room.on(Room.RoomEventEnum.PEER_LEAVE, new OnCallback() {
			@Override
			public void onCallback(final Object object) {
				Log.d(TAG, "RoomEventEnum.PEER_LEAVE:");

				if (!(object instanceof String)) return;

				String peerId = (String)object;
				Log.i(TAG, "Leave Room: " + peerId);
				Toast.makeText(SfuVideoChatActivity.this, peerId + " has left.", Toast.LENGTH_LONG).show();

				_adapter.remove(peerId);
			}
		});

		_room.on(Room.RoomEventEnum.STREAM, new OnCallback() {
			@Override
			public void onCallback(final Object object) {
				Log.d(TAG, "RoomEventEnum.STREAM: + " + object);

				if (!(object instanceof MediaStream)) return;

				final MediaStream stream = (MediaStream)object;
				Log.d(TAG, "peer = " + stream.getPeerId() + ", label = " + stream.getLabel());

				_adapter.add(stream);
			}
		});

		_room.on(Room.RoomEventEnum.REMOVE_STREAM, new OnCallback() {
			@Override
			public void onCallback(final Object object) {
				Log.d(TAG, "RoomEventEnum.REMOVE_STREAM: " + object);

				if (!(object instanceof MediaStream)) return;

				final MediaStream stream = (MediaStream)object;
				Log.d(TAG, "peer = " + stream.getPeerId() + ", label = " + stream.getLabel());

				_adapter.remove(stream);
			}
		});

		// Update UI
		updateActionButtonTitle();
	}

	/**
	 * Leave from the room
	 */
	private void leaveRoom() {
		if (!isConnected() || (_room == null)) {
			return;
		}
		_room.close();
	}

	/**
	 * Update actionButton title
	 */
	private void updateActionButtonTitle() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Button btnAction = findViewById(R.id.btnAction);
				if (null != btnAction) {
					if (!_bConnected) {
						btnAction.setText(R.string.join);
					} else {
						btnAction.setText(R.string.leave);
					}
				}
			}
		});
	}

}
