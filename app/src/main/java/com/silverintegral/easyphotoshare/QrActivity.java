/**
 * Copyright 2019 silverintegral, xenncam
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.silverintegral.easyphotoshare;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

public class QrActivity extends AppCompatActivity
		implements
		QrViewFragment.OnFragmentInteractionListener,
		QrEditFragment.OnFragmentInteractionListener {

	private QrViewFragment m_view_fragment;
	private QrEditFragment m_edit_fragment;

	private String m_ip = null;
	private int m_port = 0;
	private String m_ssid = null;
	private String m_pass = null;
	private Boolean m_hotspot = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_qr);

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		Boolean app_qr_alert_not_again = sharedPreferences.getBoolean("APP_QR_ALERT_NOT_AGAIN", false);

		m_view_fragment = new QrViewFragment(this);
		Bundle bundle_view = new Bundle();

		m_edit_fragment = new QrEditFragment(this);
		Bundle bundle_edit = new Bundle();

		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();
		if (bundle != null) {
			m_ip = bundle.getString("HOST_IP", "");
			m_port = bundle.getInt("HOST_PORT", 0);
			m_ssid = bundle.getString("AP_SSID", "");
			m_pass = bundle.getString("AP_PASS", "");
			m_hotspot = bundle.getBoolean("AP_HOTSPOT", false);

			if (m_port == 0)
				m_port = 8088;

			bundle_view.putString("HOST_IP", m_ip);
			bundle_view.putInt("HOST_PORT", m_port);
			bundle_view.putString("AP_SSID", m_ssid);
			bundle_view.putString("AP_PASS", m_pass);

			bundle_edit.putString("AP_SSID", m_ssid);
			bundle_edit.putString("AP_PASS", m_pass);

			if (m_ssid.length() == 0 && !app_qr_alert_not_again && !m_hotspot) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("下のメニューからQRの設定を行うとテザリングやWi-Fiへの接続情報もQRコードにできます。")
						.setPositiveButton("確認", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								//return;
							}
						})
						.setNegativeButton("次回から表示しない", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
								SharedPreferences.Editor editor = sharedPreferences.edit();
								editor.putBoolean("APP_QR_ALERT_NOT_AGAIN", true);
								editor.remove("");
								editor.apply();
							}
						})
						.show();
			}
		}

		// ホットスポットの場合は自分で編集できない
		if (m_hotspot)
			findViewById(R.id.qr_nav).setVisibility(View.INVISIBLE);
		else
			findViewById(R.id.qr_nav).setVisibility(View.VISIBLE);


		m_view_fragment.setArguments(bundle_view);
		m_edit_fragment.setArguments(bundle_edit);

		// 初期表示
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.add(R.id.qr_frame, m_view_fragment);
		transaction.commit();

		BottomNavigationView bottomNavigationView = findViewById(R.id.qr_nav);
		bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(@NonNull MenuItem item) {
				FragmentTransaction transaction;
				switch (item.getItemId()) {
					case R.id.qr_menu_1:
						transaction = getSupportFragmentManager().beginTransaction();
						transaction.replace(R.id.qr_frame, m_view_fragment);
						transaction.commit();
						return true;
					case R.id.qr_menu_2:
						transaction = getSupportFragmentManager().beginTransaction();
						transaction.replace(R.id.qr_frame, m_edit_fragment);
						transaction.commit();
						return true;
				}
				return false;
			}
		});
	}

	@Override
	public void onPause() {
		super.onPause();
	}


	@Override
	public void onFragmentInteraction(Uri uri) {
	}

	@Override
	public void onBackPressed() {
		try {
			m_ssid = m_edit_fragment.m_edit_ssid.getText().toString();
			m_pass = m_edit_fragment.m_edit_pass.getText().toString();

			Intent intent = new Intent();
			Bundle bundle = new Bundle();
			intent.putExtra("AP_SSID", m_ssid);
			intent.putExtra("AP_PASS", m_pass);
			//intent.putExtras(bundle);
			setResult(RESULT_OK, intent);
		} catch (Exception e) {
		}

		super.onBackPressed();
		finish();
	}
}
