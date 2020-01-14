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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.documentfile.provider.DocumentFile;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.Random;

import static android.text.Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL;
import static android.text.Html.fromHtml;

public class MainActivity extends AppCompatActivity {
	private String TAG = "MainActivity";
	private NetworkBroadcastReceiver m_networkBR = null;
	private IntentFilter m_networkIF = null;

	private static final int REQUEST_TARGETDIR = 11;
	private static final int REQUEST_QRCODEEDIT = 12;
	private static final int REQUEST_PERMISSION_STORAGE = 21;
	private static final int REQUEST_PERMISSION_NETWORK = 22;

	private String m_sv_ip = "";
	private Integer m_sv_port = 0;
	private String m_sv_name = "";
	private String m_sv_root_disp = "";
	private String m_sv_root_uri = null;
	private Boolean m_sv_keepalive = true;
	private Boolean m_sv_autokilll = false;
	private int m_sv_maxcache = 1000;
	private Boolean m_sv_delcache = false;

	private Boolean m_sv_run = false;
	private Boolean m_pem_storage = false;
	private Boolean m_pem_network = false;

	private String m_ap_ssid = "";
	private String m_ap_pass = "";
	private String m_ap_hs_ssid = "";
	private String m_ap_hs_pass = "";
	private Boolean m_ap_enable_hotspot = false;
	private Boolean m_ap_use_hotspot = false;
	private WifiManager.LocalOnlyHotspotReservation m_ap_state = null;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		m_networkBR = new NetworkBroadcastReceiver();
		m_networkIF = new IntentFilter();
		m_networkIF.addAction("MY_ACTION");
		//registerReceiver(m_networkBR, m_networkIF);

		setContentView(R.layout.activity_main);

		// 設定のロード
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		m_sv_ip = sharedPreferences.getString("SV_IP", "");
		m_sv_port = sharedPreferences.getInt("SV_PORT", 8088);
		m_sv_name = sharedPreferences.getString("SV_NAME", "EasyPhotoShare");
		m_sv_root_disp = sharedPreferences.getString("SV_ROOT_DISP", "");
		m_sv_root_uri = sharedPreferences.getString("SV_ROOT_URI", "");
		m_sv_keepalive = sharedPreferences.getBoolean("SV_KEEPALIVE", true);
		m_sv_autokilll = sharedPreferences.getBoolean("SV_AUTOKILL", false);
		m_sv_maxcache = sharedPreferences.getInt("SV_MAXCACHE", 1000);
		m_sv_delcache = sharedPreferences.getBoolean("SV_DELCACHE", false);
		m_ap_ssid = sharedPreferences.getString("AP_SSID", "");
		m_ap_pass = sharedPreferences.getString("AP_PASS", "");
		m_ap_use_hotspot = sharedPreferences.getBoolean("AP_USE_HOTSPOT", false);

		m_ap_hs_ssid = sharedPreferences.getString("AP_HS_SSID", null);
		m_ap_hs_pass = sharedPreferences.getString("AP_HS_PASS", null);
		if (m_ap_hs_ssid != null) {
			m_ap_enable_hotspot = true;
		}

		// 不正な設定の修正
		SharedPreferences.Editor editor = sharedPreferences.edit();

		// ポート確認
		if (m_sv_port < 1024 || m_sv_port > 65535) {
			m_sv_port = 8088;
			editor.putInt("SV_PORT", m_sv_port);
		}

		// ディレクトリ確認
		if (m_sv_root_uri.length() > 0) {
			Uri rootUri = Uri.parse(m_sv_root_uri);
			Uri uri = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, DocumentsContract.getTreeDocumentId(rootUri));
			if (uri != null) {
				DocumentFile dir = DocumentFile.fromTreeUri(this, uri);
				if (dir == null || !dir.exists()) {
					m_sv_root_disp = "";
					m_sv_root_uri = "";
					editor.putString("SV_ROOT_DISP", m_sv_root_disp);
					editor.putString("SV_ROOT_DISP", m_sv_root_uri);
				}
			} else {
				m_sv_root_disp = "";
				m_sv_root_uri = "";
				editor.putString("SV_ROOT_DISP", m_sv_root_disp);
				editor.putString("SV_ROOT_DISP", m_sv_root_uri);
			}
		}

		m_sv_name = m_sv_name.trim().replace("　", " ").replace(" ", "");

		editor.apply();

		((EditText)findViewById(R.id.main_txt_name)).setText(m_sv_name);
		((CheckBox)findViewById(R.id.main_chk_hotspot)).setChecked(m_ap_use_hotspot);

		Button btn_start = findViewById(R.id.main_btn_start);
		if (!m_sv_root_disp.equals("")) {
			btn_start.setEnabled(true);
		}

		btn_start.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startService();
			}
		});

		Button btn_stop = findViewById(R.id.main_btn_stop);

		btn_stop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				stopService();
			}
		});

		Button btn_sel = findViewById(R.id.main_btn_sel);
		btn_sel.setEnabled(true);

		btn_sel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				testPermission_storage();

				if (m_pem_storage) {
					Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
					intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
									| Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
									| Intent.FLAG_GRANT_READ_URI_PERMISSION
									| Intent.FLAG_GRANT_WRITE_URI_PERMISSION
					);
					startActivityForResult(intent, REQUEST_TARGETDIR);
				}
			}
		});

		findViewById(R.id.main_btn_copy).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				ClipboardManager clipboardManager =
						(ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

				if (clipboardManager != null) {
					clipboardManager.setPrimaryClip(ClipData.newPlainText("", "http://" + m_sv_ip + ":" + m_sv_port));

					Toast.makeText(getApplicationContext(),"リンクをコピーしました", Toast.LENGTH_SHORT).show();
				}
			}
		});

		findViewById(R.id.main_btn_share).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent shareIntent = new Intent(Intent.ACTION_SEND);
				shareIntent.setType("text/plain");
				shareIntent.putExtra(Intent.EXTRA_TEXT, "http://" + m_sv_ip + ":" + m_sv_port);
				startActivity(Intent.createChooser(shareIntent, "EPSのURL"));
				startActivity(shareIntent);
			}
		});

		findViewById(R.id.main_btn_qr).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showQR();
			}
		});

		if (MainService.isRun())
			m_sv_run = true;
		else
			m_sv_run = false;

		if (m_sv_run && !m_sv_ip.equals(getIPv4())) {
			stopService();
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("IPアドレスエラー")
					.setMessage("IPアドレスが変更されています。\nサービスを終了します。")
					.setPositiveButton("確認", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							refreshUI();
						}
					});
			builder.show();
		} else {
			refreshUI();
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			//case R.id.menu_main_setting:
				//startActivity(new Intent(this, SettingsActivity.class));
				//Toast.makeText(null, "現在は設定可能な項目がありません", Toast.LENGTH_SHORT).show();
			//	return true;

			case R.id.menu_main_license:
				startActivity(new Intent(this, LicenseActivity.class));
				return true;

			case R.id.menu_main_exit:
				stopService();
				finish();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode != RESULT_OK)
			return;

		try {
			if (requestCode == REQUEST_QRCODEEDIT) {
				m_ap_ssid = data.getStringExtra("AP_SSID");
				m_ap_pass = data.getStringExtra("AP_PASS");

				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
				SharedPreferences.Editor editor = sharedPreferences.edit();
				editor.putString("AP_SSID", m_ap_ssid);
				editor.putString("AP_PASS", m_ap_pass);
				editor.apply();
			} else if (requestCode == REQUEST_TARGETDIR) {
				Uri rootUri = data.getData();

				m_sv_root_uri = "";

				// アクセス確認
				Uri uri = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, DocumentsContract.getTreeDocumentId(rootUri));
				if (uri != null) {
					DocumentFile dir = DocumentFile.fromTreeUri(this, uri);
					if (dir != null && dir.exists()) {
						m_sv_root_uri = rootUri.toString();
					}
				}

				if (m_sv_root_uri.length() > 0) {
					// ディレクトリ選択
					m_sv_root_disp = data.getDataString();
					m_sv_root_disp = URLDecoder.decode(m_sv_root_disp, "utf-8");

					// URIを表示用に変換
					if (m_sv_root_disp.indexOf("/primary:") != -1) {
						m_sv_root_disp = m_sv_root_disp.substring(m_sv_root_disp.indexOf("/primary:") + 9);
						m_sv_root_disp = "internal:" + m_sv_root_disp.replace(":", "/");
					} else {
						m_sv_root_disp = m_sv_root_disp.substring(m_sv_root_disp.indexOf("/tree/") + 6);
						m_sv_root_disp = "external:" + m_sv_root_disp.replace(":", "/");
					}

					if (m_sv_root_disp.equals(""))
						findViewById(R.id.main_btn_start).setEnabled(false);
					else
						findViewById(R.id.main_btn_start).setEnabled(true);
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setMessage("指定された場所へのアクセスが拒否されました。\nアプリケーションの権限などを確認して下さい。")
							.setPositiveButton("確認", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									//return;
								}
							});
					builder.show();
				}

				// 設定を保存
				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
				SharedPreferences.Editor editor = sharedPreferences.edit();
				editor.putString("SV_ROOT_DISP", m_sv_root_disp);
				editor.putString("SV_ROOT_URI", m_sv_root_uri);
				editor.apply();
			}
		} catch (UnsupportedEncodingException e) {
			// 例外処理
			e.printStackTrace();
		}
	}


	@Override
	protected void onResume() {
		super.onResume();
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
		//stopService();
		finish();
	}


	private void startService() { startService_in(false); }
	private void startServiceWifi() { startService_in(true); }
	private void startService_in(final Boolean forceWifi) {
		if (m_sv_run)
			return;

		m_sv_name = ((EditText)findViewById(R.id.main_txt_name)).getText().toString();
		m_ap_use_hotspot = ((CheckBox)findViewById(R.id.main_chk_hotspot)).isChecked();

		m_sv_ip = getIPv4();

		//if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			if (m_ap_use_hotspot) {
				if (m_sv_ip == null) {
					new AlertDialog.Builder(this).setCancelable(false)
							.setTitle("IPアドレスが見つかりません")
							.setMessage("ホットスポットを作成しますか？\n\n※一部のAndroidからアクセスが不可能になる可能性があります。")
							.setPositiveButton("作成する", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									testPermission_network();
									if (m_pem_network) {
										if (startAp()) {
											startServiceWifi();
										} else {
											Toast.makeText(MainActivity.this, "作成に失敗しました",Toast.LENGTH_LONG ).show();
										}
									}
								}
							})
							.setNegativeButton("いいえ", null)
							.create().show();
					return;
				}
			}
		//}

		if (m_sv_ip == null) {
			new AlertDialog.Builder(this).setCancelable(false)
					.setTitle("IPアドレスが見つかりません")
					.setMessage("利用可能なIPアドレスが存在しません。\nネットワークを確認して下さい。")
					.setPositiveButton("設定画面の表示", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							startActivity(new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS));
						}
					})
					.setNegativeButton("閉じる", null)
					.create().show();

			return;
		} else if (getWifiState() && !forceWifi) {
			new AlertDialog.Builder(this).setCancelable(false)
					.setTitle("Wi-Fiネットワークの検出")
					.setMessage("同じWi-Fiネットワークに存在するクライアントからのみ閲覧が可能です。\n")
					.setPositiveButton("Wi-Fi接続で開始", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							startServiceWifi();
						}
					})
					.setNegativeButton("閉じる", null)
					.create().show();

			return;
		}


		// 設定の保存
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString("SV_IP", m_sv_ip);
		editor.putString("SV_NAME", m_sv_name);
		editor.putBoolean("AP_USE_HOTSPOT", m_ap_use_hotspot);
		editor.apply();

		// サービスの開始
		Intent serviceIntent = new Intent(getApplication(), MainService.class);
		serviceIntent.putExtra("SV_IP", m_sv_ip);
		serviceIntent.putExtra("SV_PORT", m_sv_port);
		serviceIntent.putExtra("SV_NAME", m_sv_name);
		serviceIntent.putExtra("SV_ROOT_URI", m_sv_root_uri);
		serviceIntent.putExtra("SV_KEEPALIVE", m_sv_keepalive);
		serviceIntent.putExtra("SV_AUTOKILL", m_sv_autokilll);
		serviceIntent.putExtra("SV_MAXCACHE", m_sv_maxcache);
		serviceIntent.putExtra("SV_DELCACHE", m_sv_delcache);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			startForegroundService(serviceIntent);
		} else {
			startService(serviceIntent);
		}

		m_sv_run = true;
		refreshUI();
	}


	private void stopService() {
		if (!m_sv_run)
			return;

		Intent serviceIntent = new Intent(getApplication(), MainService.class);
		stopService(serviceIntent);

		// 実行IPの削除
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString("SV_IP", "");
		editor.apply();

		if (m_ap_enable_hotspot)
			stopAp();

		m_sv_run = false;
		refreshUI();
	}


	private boolean getWifiState() {
		WifiManager mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		return mWifiManager.isWifiEnabled();
	}


	private void refreshUI() {
		if (m_sv_run) {
			findViewById(R.id.main_btn_start).setEnabled(false);
			findViewById(R.id.main_btn_stop).setEnabled(true);
			findViewById(R.id.main_btn_sel).setEnabled(false);
			findViewById(R.id.main_btn_copy).setEnabled(true);
			findViewById(R.id.main_btn_share).setEnabled(true);
			findViewById(R.id.main_btn_qr).setEnabled(true);
			findViewById(R.id.main_txt_name).setEnabled(false);
			findViewById(R.id.main_chk_hotspot).setEnabled(false);

			String port = m_sv_port.toString();
			if (port.equals("80"))
				port = "";
			else
				port = ":" + port;

			String html = "<b>実行中</b><br>"
					+ "<a href=\"http://" + m_sv_ip + ":8088\">http://" + m_sv_ip + port + "</a><br><br>"
					+ "<b>PATH:</b> " + m_sv_root_disp + "<br>"
					+ "<b>TYPE:</b> " + getNetworkType() + "<br>";
			if (m_ap_enable_hotspot && m_ap_hs_ssid != null) {
				html += "<b>SSID:</b> " + m_ap_hs_ssid + "<br>"
						+ "<b>PASS:</b> " + m_ap_hs_pass + "<br>";
			} else if (m_ap_ssid != null && !getNetworkType().equals("wifi")) {
				html += "<b>SSID:</b> " + m_ap_ssid + "<br>"
						+ "<b>PASS:</b> " + m_ap_pass + "<br>";
			} else {
				html += "<b>SSID:</b> -<br>"
						+ "<b>PASS:</b> -<br>";
			}

			TextView tv_info = findViewById(R.id.main_txt_info_view);
			tv_info.setText(fromHtml(html, TO_HTML_PARAGRAPH_LINES_INDIVIDUAL));
			tv_info.setMovementMethod(LinkMovementMethod.getInstance());
		} else {
			if (m_sv_root_disp.equals(""))
				findViewById(R.id.main_btn_start).setEnabled(false);
			else
				findViewById(R.id.main_btn_start).setEnabled(true);

			findViewById(R.id.main_btn_stop).setEnabled(false);
			findViewById(R.id.main_btn_sel).setEnabled(true);
			findViewById(R.id.main_btn_copy).setEnabled(false);
			findViewById(R.id.main_btn_share).setEnabled(false);
			findViewById(R.id.main_btn_qr).setEnabled(false);

			findViewById(R.id.main_txt_name).setEnabled(true);
			findViewById(R.id.main_chk_hotspot).setEnabled(true);


			String html = "<b>停止中</b><br><br><br><br><br>";

			TextView tv_info = findViewById(R.id.main_txt_info_view);
			tv_info.setText(fromHtml(html, TO_HTML_PARAGRAPH_LINES_INDIVIDUAL));
			tv_info.setMovementMethod(LinkMovementMethod.getInstance());
		}
	}


	private void showQR() {
		Intent intent = new Intent(this, QrActivity.class);
		intent.putExtra("HOST_IP", m_sv_ip);
		intent.putExtra("HOST_PORT", m_sv_port);

		if (m_ap_enable_hotspot) {
			intent.putExtra("AP_SSID", m_ap_hs_ssid);
			intent.putExtra("AP_PASS", m_ap_hs_pass);
		} else {
			intent.putExtra("AP_SSID", m_ap_ssid);
			intent.putExtra("AP_PASS", m_ap_pass);
		}

		intent.putExtra("AP_HOTSPOT", m_ap_enable_hotspot);

		if (m_ap_enable_hotspot)
			startActivity(intent);
		else
			startActivityForResult(intent, REQUEST_QRCODEEDIT);
	}


	private Boolean startAp() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			startAp_new();
		} else {
			startAp_old();
		}

		if (waitActiveIP()) {
			//if (m_ap_hs_ssid != null) {
				m_ap_enable_hotspot = true;
				refreshUI();

				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
				SharedPreferences.Editor editor = sharedPreferences.edit();
				editor.putString("AP_HS_SSID", m_ap_hs_ssid);
				editor.putString("AP_HS_PASS", m_ap_hs_pass);
				editor.apply();

				return true;
			//}
		}

		m_ap_enable_hotspot = false;
		m_ap_hs_ssid = null;
		m_ap_hs_pass = null;

		if (m_ap_state != null) {
			m_ap_state.close();
			m_ap_state = null;
		}
		m_ap_state = null;

		refreshUI();
		return false;
	}


	@RequiresApi(api = Build.VERSION_CODES.O)
	private void startAp_new() {
		WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		if (manager == null) {
			return;
		}

		manager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {
			@Override
			public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
				super.onStarted(reservation);
				m_ap_state = reservation;
				WifiConfiguration conf = m_ap_state.getWifiConfiguration();
				m_ap_hs_ssid = conf.SSID;
				m_ap_hs_pass = conf.preSharedKey;
				refreshUI();
			}

			@Override
			public void onStopped() {
				super.onStopped();
			}

			@Override
			public void onFailed(int reason) {
				super.onFailed(reason);
			}
		}, new Handler());
	}


	private void startAp_old() {
		WifiManager mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		if (mWifiManager == null) {
			return;
		}

		Method setWifiApState;
		try {
			setWifiApState = mWifiManager.getClass().getMethod("setWifiApState", WifiConfiguration.class, boolean.class);
			if (setWifiApState == null)
				return;
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			return;
		}

		Random rand = new Random();
		String ssid = "EPS_";
		String pass = "";

		for (int i = 0; i < 6; i++) {
			ssid += String.valueOf(rand.nextInt(8) + 1);
		}

		for (int i = 0; i < 20; i++) {
			pass += String.valueOf(rand.nextInt(9));
		}

		WifiConfiguration conf = new WifiConfiguration();
		conf.SSID = ssid;
		conf.preSharedKey = pass;
		conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
		conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
		conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
		mWifiManager.addNetwork(conf);

		try {
			setWifiApState.invoke(mWifiManager, null,false);
			Boolean ret = (Boolean)setWifiApState.invoke(mWifiManager, null, true);
			if (ret != null && ret) {
				m_ap_hs_ssid = ssid;
				m_ap_hs_pass = pass;
			}
		} catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}


	private void stopAp() {
		if (!m_ap_enable_hotspot)
			return;

		m_ap_enable_hotspot = false;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			stopAp_new();
		} else {
			stopAp_old();
		}

		refreshUI();
	}


	@RequiresApi(api = Build.VERSION_CODES.O)
	private void stopAp_new() {
		m_ap_hs_ssid = null;
		m_ap_hs_pass = null;

		try {
			if (m_ap_state != null) {
				m_ap_state.close();
				m_ap_state = null;
			}
		} catch (Exception ignored) {
		}
	}


	private void stopAp_old() {
		m_ap_hs_ssid = null;
		m_ap_hs_pass = null;

		WifiManager mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		if (mWifiManager == null) {
			return;
		}

		int stat = mWifiManager.getWifiState();
		if (stat == WifiManager.WIFI_STATE_ENABLED || stat == WifiManager.WIFI_STATE_ENABLING) {
			try {
				Method setWifiApState = mWifiManager.getClass().getMethod("setWifiApState", WifiConfiguration.class, boolean.class);
				setWifiApState.invoke(mWifiManager, null, false);
			} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
				e.printStackTrace();
			}
		}
	}


	// 現在アクティブなネットワークの種別を取得
	private String getNetworkType() {
		if (m_ap_enable_hotspot) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
				return "hotspot (localonly)";
			else
				return "hotspot (tethering)";
		}

		ConnectivityManager cm = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm == null)
			return "error";

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		if (activeNetwork == null)
			return "error";

		if (!activeNetwork.isConnected())
			return "error";

		switch (activeNetwork.getType()) {
			case ConnectivityManager.TYPE_WIFI:
				return "wifi";
			case ConnectivityManager.TYPE_WIMAX:
				return "wimax";
			case ConnectivityManager.TYPE_ETHERNET:
				return "ethernet";
			case ConnectivityManager.TYPE_VPN:
				return "vpn";
			case ConnectivityManager.TYPE_BLUETOOTH:
				return "bluetooth";
			case ConnectivityManager.TYPE_MOBILE:
			case ConnectivityManager.TYPE_MOBILE_DUN:
				return "mobile";
			default:
				return "unknown";
		}
	}


	private Boolean waitActiveIP() {
		long maxtime = System.currentTimeMillis() + 5000;

		while (true) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			}

			if (getIPv4() != null)
				return true;

			if (maxtime < System.currentTimeMillis())
				return false;
		}
	}


	// 現在有効なIPアドレスを取得
	private String getIPv4() {
		Enumeration<NetworkInterface> interfaces = null;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {
			e.printStackTrace();
			return null;
		}

		while (interfaces.hasMoreElements()) {
			NetworkInterface net_face = interfaces.nextElement();

			// 無効なインターフェスをスキップ
			String netname = net_face.getName();
			if (netname.contains("bridge") || netname.contains("p2p")
					|| netname.contains("rmnet_") || netname.contains("dummy")
					|| netname.equals("lo")) {
				continue;
			}

			Enumeration<InetAddress> addresses = net_face.getInetAddresses();

			while (addresses.hasMoreElements()) {
				InetAddress addr_elem = addresses.nextElement();

				if (!addr_elem.isLoopbackAddress() && addr_elem instanceof Inet4Address) {
					// ループバック以外のv4アドレス
					return addr_elem.getHostAddress();
				}
			}
		}

		return null;
	}


	// ストレージの権限確認
	private void testPermission_storage() {
		if (
				ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
				&& ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
		) {

			// 既に許可がされていた
			m_pem_storage = true;
		} else {
			// 未確認なので許可を得る
			m_pem_storage = false;

			// チェック
			ActivityCompat.requestPermissions(this,
					new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_STORAGE);

			if (ActivityCompat.shouldShowRequestPermissionRationale(this,
					Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
				// 再度要求をしても大丈夫
				ActivityCompat.requestPermissions(this,
						new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_STORAGE);
			}
		}
	}


	// ネットワークの権限確認
	private void testPermission_network() {
		if (
				ActivityCompat.checkSelfPermission(this, Manifest.permission.CHANGE_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED
				&& ActivityCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED
				&& ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
		 ) {

			// 既に許可がされていた
			m_pem_network = true;
		} else {
			// 未確認なので許可を得る
			m_pem_network = false;

			// チェック
			ActivityCompat.requestPermissions(this,
					new String[] {Manifest.permission.CHANGE_NETWORK_STATE, Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_COARSE_LOCATION},
					REQUEST_PERMISSION_NETWORK);

			if (ActivityCompat.shouldShowRequestPermissionRationale(this,
					Manifest.permission.CHANGE_WIFI_STATE)) {
				// 再度要求をしても大丈夫
				ActivityCompat.requestPermissions(this,
						new String[] {Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_COARSE_LOCATION},
						REQUEST_PERMISSION_NETWORK);
			}
		}
	}


	// 権限確認の結果受け取り
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == REQUEST_PERMISSION_STORAGE) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				// 許可が得られた
				m_pem_storage = true;
				Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
				intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
						| Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
						| Intent.FLAG_GRANT_READ_URI_PERMISSION
						| Intent.FLAG_GRANT_WRITE_URI_PERMISSION
				);
				startActivityForResult(intent, REQUEST_TARGETDIR);

			} else {
				// "二度と表示しない"で拒否をされている
				m_pem_storage = false;
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("権限エラー")
						.setMessage("サービスの起動にはストレージへのアクセスが必ず必要です。\n設定を確認して下さい。")
						.setPositiveButton("確認", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								String uriString = "package:" + getPackageName();
								Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse(uriString));
								startActivity(intent);
								finish();
							}
						});
				builder.show();
			}
		} else if (requestCode == REQUEST_PERMISSION_NETWORK) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				m_pem_network = true;
				if (startAp()) {
					startServiceWifi();
				} else {
					Toast.makeText(MainActivity.this, "作成に失敗しました",Toast.LENGTH_LONG ).show();
				}
			} else {
				m_pem_network = false;
				Toast.makeText(MainActivity.this, "作成に必要な権限がありません",Toast.LENGTH_LONG ).show();
			}
		}
	}
}
