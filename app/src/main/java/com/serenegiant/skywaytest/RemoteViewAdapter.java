package com.serenegiant.skywaytest;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import io.skyway.Peer.Browser.Canvas;
import io.skyway.Peer.Browser.MediaStream;

class RemoteViewAdapter extends ArrayAdapter<RemoteViewAdapter.RemoteView> {
	private static final String TAG = RemoteViewAdapter.class.getSimpleName();

	static class RemoteView {
		String peerId;
		MediaStream stream;
		Canvas canvas;
		View viewHolder;
	}

	private final LayoutInflater inflater;

	RemoteViewAdapter(final Context context) {
		super(context, 0);
		this.inflater = LayoutInflater.from(context);
	}

	@NonNull
	@Override
	public View getView(int position, View convertView, @NonNull ViewGroup parent) {
		View view;

		Log.d(TAG, "getView(" + position + ")");

		RemoteView item = getItem(position);
		if (null != item) {
			if (null == item.viewHolder) {
				item.viewHolder = inflater.inflate(R.layout.view_remote, parent, false);
				TextView txvRemotePeerId = item.viewHolder.findViewById(R.id.txvRemotePeerId);
				if (null != txvRemotePeerId) {
					txvRemotePeerId.setText(item.peerId);
				}
				item.canvas = item.viewHolder.findViewById(R.id.cvsRemote);
				item.stream.addVideoRenderer(item.canvas, 0);
				view = item.viewHolder;
			} else {
				view = item.viewHolder;
				item.canvas.requestLayout();
			}
		}
		else if (null == convertView) {
			view = inflater.inflate(R.layout.view_unknown_remote, parent, false);
		}
		else {
			view = convertView;
		}

		return view;
	}

	public void add(MediaStream stream) {
		RemoteView item = new RemoteView();
		item.peerId = stream.getPeerId();
		item.stream = stream;
		add(item);
	}

	public void remove(String peerId) {
		RemoteView target = null;

		int count = getCount();
		for (int i = 0; i < count; ++i) {
			RemoteView item = getItem(i);
			if (null != item && item.peerId.equals(peerId)) {
				target = item;
				break;
			}
		}

		if (null != target) {
			removeRenderer(target);
			remove(target);
		}
	}

	public void remove(MediaStream stream) {
		RemoteView target = null;

		int count = getCount();

		for (int i = 0; i < count; ++i) {
			RemoteView item = getItem(i);
			if (null != item && item.stream == stream) {
				target = item;
				break;
			}
		}

		if (null != target) {
			removeRenderer(target);
			remove(target);
		}
	}

	private void removeRenderer(RemoteView item) {
		if (null == item) return;

		if (null != item.canvas) {
			item.stream.removeVideoRenderer(item.canvas, 0);
			item.canvas = null;
		}
		item.stream.close();
		item.viewHolder = null;
	}

	void removeAllRenderers() {
		int count = getCount();
		for (int i = 0; i < count; ++i) {
			removeRenderer(getItem(i));
		}
		clear();
	}
}