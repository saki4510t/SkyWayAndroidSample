package com.serenegiant.skywaytest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CompoundButton;

import com.google.android.material.snackbar.Snackbar;
import com.serenegiant.skywaytest.databinding.ActivityMainBinding;

import static com.serenegiant.skywaytest.Const.*;

public class MainActivity extends AppCompatActivity {

	private ActivityMainBinding mBinding;
	private Snackbar mSnackbar;

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
		mBinding.apiKeyEdittext.setText(loadAPIKey(this));
		mBinding.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				if (isPeerAuthEnabled(MainActivity.this)) {
					final String secretKey = mBinding.secretKeyEdittext.getText().toString();
					if (!TextUtils.isEmpty(secretKey)) {
						saveSecretKey(MainActivity.this, secretKey);
					} else {
						saveSecretKey(MainActivity.this, null);
						showError("秘密鍵がセットされていないよ");
						return;
					}
				}
				final String apiKey = mBinding.apiKeyEdittext.getText().toString();
				if (TextUtils.isEmpty(apiKey)) {
					showError("APIキーがセットされていないよ");
					return;
				}
				Intent intent = null;
				switch (v.getId()) {
				case R.id.one_one_p2p_btn:
					intent = new Intent(MainActivity.this, P2pVideoChatActivity.class);
					break;
				case R.id.multi_p2p_btn:
					intent = new Intent(MainActivity.this, P2pMeshVideoChatActivity.class);
					break;
				case R.id.multi_sfu_btn:
					intent = new Intent(MainActivity.this, SfuVideoChatActivity.class);
					break;
				default:
					break;
				}
				if (intent != null) {
					intent.putExtra(EXTRA_KEY_API_KEY, apiKey);
					startActivity(intent);
				}
			}
		});
		final boolean isPeerAuthEnabled = isPeerAuthEnabled(this);
		mBinding.setPeerAuthEnabled(isPeerAuthEnabled);
		if (isPeerAuthEnabled) {
			mBinding.secretKeyEdittext.setText(loadSecretKey(MainActivity.this));
			mBinding.secretKeyEdittext.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
				}

				@Override
				public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
				}

				@Override
				public void afterTextChanged(final Editable editable) {
					updateButtonState();
				}
			});
		}
		mBinding.peerAuthCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(final CompoundButton compoundButton, final boolean b) {
				setPeerAuthEnabled(MainActivity.this, b);
				mBinding.setPeerAuthEnabled(b);
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
				updateButtonState();
			}
		});
		updateButtonState();
	}

	private void updateButtonState() {
		boolean enabled = true;
		if (isPeerAuthEnabled(this)) {
			final Editable v = mBinding.secretKeyEdittext.getText();
			enabled &= (!TextUtils.isEmpty(v) && (v.length() > 0));
		}
		final Editable v = mBinding.apiKeyEdittext.getText();
		enabled &= (!TextUtils.isEmpty(v) && (v.length() > 0));
		mBinding.setButtonEbavled(enabled);
	}

	private void showError(final CharSequence msg) {
		if (mSnackbar != null) {
			mSnackbar.dismiss();
		}
		mSnackbar = Snackbar.make(mBinding.getRoot(), msg, 2500);
		mSnackbar.show();
	}
}
