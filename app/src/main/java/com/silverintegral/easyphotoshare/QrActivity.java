package com.silverintegral.easyphotoshare;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

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
	private String m_name = null;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_qr);

		m_view_fragment = new QrViewFragment(this);
		Bundle bundle_view = new Bundle();

		m_edit_fragment = new QrEditFragment(this);
		Bundle bundle_edit = new Bundle();

		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();
		if (bundle != null) {
			m_ip = bundle.getString("HOST_IP", "");
			m_port = bundle.getInt("HOST_PORT", 0);
			m_name = bundle.getString("HOST_NAME", "");
			m_ssid = bundle.getString("AP_SSID", "");
			m_pass = bundle.getString("AP_PASS", "");

			bundle_view.putString("HOST_IP", m_ip);
			bundle_view.putInt("HOST_PORT", m_port);
			bundle_view.putString("HOST_NAME", m_name);
			bundle_view.putString("AP_SSID", m_ssid);
			bundle_view.putString("AP_PASS", m_pass);

			bundle_edit.putString("AP_SSID", m_ssid);
			bundle_edit.putString("AP_PASS", m_pass);
		}

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

		/*
		String barcodeApn = null;
		String barcodeUrl = null;

		if (m_ip.length() > 0) {
			// URL
			barcodeUrl = "http://" + m_ip + ":" + m_port;
		}
		if (m_ssid.length() > 0) {
			// APN
			barcodeApn = "WIFI:T:WPA;S:" + m_ssid + ";P:" + m_pass + ";;";
		}

		//FragmentTransaction transaction_edit = m_edit_fragment.getFragmentManager().beginTransaction();

		try {
			BarcodeEncoder barcodeEncoder = new BarcodeEncoder();

			if (barcodeUrl != null) {
				Bitmap bitmap1 = barcodeEncoder.encodeBitmap(barcodeUrl, BarcodeFormat.QR_CODE, 1000, 1000);
				ImageView imageQr1 = (ImageView)findViewById(R.id.qr_1);
				imageQr1.setImageBitmap(bitmap1);
				findViewById(R.id.txt_1).setVisibility(View.VISIBLE);
				findViewById(R.id.qr_1).setVisibility(View.VISIBLE);
			} else {
				findViewById(R.id.txt_1).setVisibility(View.INVISIBLE);
				findViewById(R.id.qr_1).setVisibility(View.INVISIBLE);
			}

			if (barcodeApn != null) {
				Bitmap bitmap2 = barcodeEncoder.encodeBitmap(barcodeApn, BarcodeFormat.QR_CODE, 1000, 1000);
				ImageView imageQr2 = findViewById(R.id.qr_2);
				imageQr2.setImageBitmap(bitmap2);
				findViewById(R.id.txt_2).setVisibility(View.VISIBLE);
				findViewById(R.id.qr_2).setVisibility(View.VISIBLE);
			} else {
				findViewById(R.id.txt_2).setVisibility(View.INVISIBLE);
				findViewById(R.id.qr_2).setVisibility(View.INVISIBLE);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		*/

		//transaction_edit.commit();
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
			EditText ssid = m_edit_fragment.getActivity().findViewById(R.id.qr_ssid);
			EditText pass = m_edit_fragment.getActivity().findViewById(R.id.qr_pass);
			m_ssid = ssid.getText().toString();
			m_pass = pass.getText().toString();

			Intent intent = new Intent();
			Bundle bundle = new Bundle();
			bundle.putString("AP_SSID", m_ssid);
			bundle.putString("AP_PASS", m_pass);
			intent.putExtras(bundle);
			setResult(RESULT_OK, intent);
		} catch (Exception e) {
		}

		super.onBackPressed();
		finish();
	}

	public void setWifiInfo(String ssid, String pass) {
		m_ssid = ssid;
		m_pass = pass;

		m_view_fragment.setWifiInfo(ssid, pass);
	}
}
