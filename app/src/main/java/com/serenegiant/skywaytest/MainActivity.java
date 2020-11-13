package com.serenegiant.skywaytest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;

import com.serenegiant.skywaytest.databinding.ActivityMainBinding;

import static com.serenegiant.skywaytest.Const.API_KEY;
import static com.serenegiant.skywaytest.Const.EXTRA_KEY_API_KEY;
import static com.serenegiant.skywaytest.Const.loadAPIKey;

public class MainActivity extends AppCompatActivity {

	private ActivityMainBinding mBinding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
		initView();
	}

	/**
	 * Viewを初期化する
	 */
	private void initView() {
		// 最後に使用したskyway APIキーを共有プレファレンスから読み込む
		final String lastApiKey = loadAPIKey(this);
		mBinding.apiKeyEdittext.setHint(R.string.label_api_key);
		if (!TextUtils.isEmpty(lastApiKey)) {
			mBinding.apiKeyEdittext.setText(API_KEY);
			setEnableButtons(true);
		} else {
			mBinding.apiKeyEdittext.setText("");
			setEnableButtons(false);
		}

		mBinding.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				Intent intnt = null;
				switch (v.getId()) {
				case R.id.one_one_p2p_btn:
					intnt = new Intent(MainActivity.this, P2pVideoChatActivity.class);
					break;
				case R.id.multi_p2p_btn:
					intnt = new Intent(MainActivity.this, P2pMeshVideoChatActivity.class);
					break;
				case R.id.multi_sfu_btn:
					intnt = new Intent(MainActivity.this, SfuVideoChatActivity.class);
					break;
				default:
					break;
				}
				if (intnt != null) {
					intnt.putExtra(EXTRA_KEY_API_KEY, mBinding.apiKeyEdittext.getText().toString());
					startActivity(intnt);
				}
			}
		});
		mBinding.apiKeyEdittext.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {

			}

			@Override
			public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {

			}

			@Override
			public void afterTextChanged(final Editable s) {
				final String v = s != null ? s.toString() : null;
				setEnableButtons(!TextUtils.isEmpty(v) && (v.length() > 0));
			}
		});
	}

	private void setEnableButtons(final boolean enable) {
		mBinding.oneOneP2pBtn.setEnabled(enable);
		mBinding.multiP2pBtn.setEnabled(enable);
		mBinding.multiSfuBtn.setEnabled(enable);
	}
}
