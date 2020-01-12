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

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;


public class MainService extends Service {
	private final String HTTP_SERVER_NAME = "EPSS/1.0";
	private final int HTTP_MAX_REQUEST_CONNECTION = 4; // クライアントの最大接続数
	private final int HTTP_MAX_REQUEST_SIZE = 2048; // リクエストデータの最大サイズ
	private final int IMAGE_MAX_REQUEST_CONVERT = 8; // 同時画像最適化数
	private final int IMAGE_MAX_SIZE_S = 300;
	private final int IMAGE_MAX_SIZE_M = 2000;

	private String[] SAF_IDX = new String[] {
			DocumentsContract.Document.COLUMN_DOCUMENT_ID,
			DocumentsContract.Document.COLUMN_DISPLAY_NAME,
			DocumentsContract.Document.COLUMN_MIME_TYPE,
			DocumentsContract.Document.COLUMN_SIZE,
			DocumentsContract.Document.COLUMN_LAST_MODIFIED
	};

	private final int SAF_ID = 0;
	private final int SAF_NAME = 1;
	private final int SAF_MIME = 2;
	private final int SAF_SIZE = 3;
	private final int SAF_DATE = 4;

	private String m_ip;
	private Integer m_port;
	private String m_name;
	private String m_root_uri;
	private Boolean m_keepalive;
	private Boolean m_autokilll;
	//private Integer m_maxcache;
	private Boolean m_delcache;

	private static boolean m_isRun = false;
	private HttpService m_http = null;
	private ImageService m_image = null;
	private NsdManager.RegistrationListener m_nsd_reg_Listener = null;
	private NsdManager m_nsd = null;
	private Context m_context;
	private String m_note;



	public MainService() {
	}

	public static boolean isRun() {
		return m_isRun;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		m_isRun = true;

		m_ip = intent.getExtras().getString("SV_IP");
		m_port = intent.getExtras().getInt("SV_PORT");
		m_name = intent.getExtras().getString("SV_NAME");
		m_root_uri = intent.getExtras().getString("SV_ROOT_URI");
		m_keepalive = intent.getExtras().getBoolean("SV_KEEPALIVE");
		m_autokilll = intent.getExtras().getBoolean("SV_AUTOKILL");
		//m_maxcache = intent.getExtras().getInt("SV_MAXCACHE");
		m_delcache = intent.getExtras().getBoolean("SV_DELCACHE");


		m_context = getApplication().getApplicationContext();
		m_note = "EASY PHOTO SHARE";

		ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
		serviceStart();

		String channelId = getString(R.string.app_name);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(channelId, "MAINSERVICE", NotificationManager.IMPORTANCE_DEFAULT);
			channel.setDescription(channelId);
			channel.setSound(null, null);
			channel.setLightColor(Color.GREEN);
			channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

			NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
			if (manager != null) {
				manager.createNotificationChannel(channel);
			}
		}

		PendingIntent pen = PendingIntent.getActivity(getApplicationContext(), 0,
				new Intent(getApplicationContext(), MainActivity.class),
				PendingIntent.FLAG_CANCEL_CURRENT);

		Notification notification = new NotificationCompat.Builder(this, channelId)
				.setContentTitle("サービス実行中")
				.setSmallIcon(R.drawable.ic_stat_name)
				.setContentText("タップでアプリを表示します")
				.setContentIntent(pen)
				.setAutoCancel(true)
				.setShowWhen(false)
				.build();

		startForeground(1, notification);

		return START_NOT_STICKY;
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		m_isRun = false;
		serviceStop();
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private void serviceStart() {
		//startNsd();
		startImage();
		startHttp();
	}

	private void serviceStop() {
		stopHttp();
		stopImage();
		//stopNsd();
	}

	private void startHttp() {
		m_http = new HttpService(m_ip, m_port, m_root_uri, m_note);
		m_http.start();
	}

	private void stopHttp() {
		m_http.shutdown();
		m_http = null;
	}

	private void startImage() {
		m_image = new ImageService(m_root_uri);
		m_image.start();
	}

	private void stopImage() {
		m_image.shutdown();
	}

	/*
	private void startNsd() {
		if (m_nsd != null) {
			stopNsd();
		}

		NsdServiceInfo serviceInfo = new NsdServiceInfo();
		serviceInfo.setPort(80);
		serviceInfo.setServiceType("_http._tcp.");
		serviceInfo.setServiceName("EASYPHOTOSHARE");


		m_nsd_reg_Listener = new NsdManager.RegistrationListener() {
			@Override
			public void onServiceRegistered(NsdServiceInfo serviceInfo) { }

			@Override
			public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
				m_nsd = null;
			}

			@Override
			public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
				m_nsd = null;
			}

			@Override
			public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
				m_nsd = null;
			}
		};

		m_nsd = (NsdManager)getSystemService(NSD_SERVICE);
		if (m_nsd != null) {
			m_nsd.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, m_nsd_reg_Listener);
		}
	}

	private void stopNsd() {
		if (m_nsd != null) {
			m_nsd.unregisterService(m_nsd_reg_Listener);
			m_nsd = null;
		}
	}
	 */



	// HTTPサーバー
	private class HttpService extends Thread {
		private ExecutorService s_exec_worker;
		private ServerSocket s_server_sock = null;

		private String s_ip;
		private int s_port;
		private String s_root_uri;
		private String s_note;

		private long m_last_access = 0;


		public HttpService(String ip, int port, String root_uri, String note) {
			s_ip = ip;
			s_port = port;
			s_root_uri = root_uri;
			s_note = note;

			s_exec_worker = Executors.newFixedThreadPool(HTTP_MAX_REQUEST_CONNECTION);
			s_server_sock = null;
		}

		public void run() {
			try {
				s_server_sock = new ServerSocket(s_port);
			} catch (IOException e) {
				// 起動に失敗ネットワーク権限？
				e.printStackTrace();
				s_server_sock = null;
				return;
			}

			if (m_autokilll) {
				// 30分未接続で自動停止
				new Thread(new Runnable() {
					@Override
					public void run() {
						Date dt = new Date();
						m_last_access = dt.getTime();

						while (true) {
							try {
								sleep(1000 * 60);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}

							long now = dt.getTime();
							if (m_last_access + 60 * 30 < now) {
								serviceStop();
								return;
							}

							m_last_access = now;
						}
					}
				}).start();
			}

			Date dt = new Date();

			while (true) {
				try {
					s_exec_worker.submit(new HttpWorker(s_server_sock.accept()));
					m_last_access = dt.getTime();
				} catch (SocketException e) {
					// NOTE: accept()中にclose()で必ず出る？
					break;
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}

				if (s_server_sock == null || s_server_sock.isClosed())
					break;
			}

			shutdown();
		}

		public void shutdown() {
			if (s_exec_worker != null) {
				s_exec_worker.shutdown();
				s_exec_worker = null;
			}

			if (s_server_sock != null) {
				try {
					s_server_sock.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

				s_server_sock = null;
			}
		}

		private byte[] httpServerExec(byte[] request) {
			byte[] response = null;
			String type = "";

			String name = getRequestFileName(request);

			// 例外対応
			if (name == null) {
				// ファイル名が存在しない
				response = getAssetFile("_badrequest.html");
				return addHeader(response, "400 Bad Request", "text/html", null);
			} else if (name.length() == 0) {
				// トップページ
				response = m_image.getIndex();
				return addHeader(response, "200 OK", "text/html", null);
			} else if (name.equals("license")) {
				// ライセンスページ
				response = getAssetFile("_license.html");
				return addHeader(response, "200 OK", "text/plain", null);
			} else {
				// アクセス禁止ファイル
				String is = name.substring(0, 1);
				if (is.equals(".") || is.equals("_")) {
					response = getAssetFile("_forbidden.html");
					return addHeader(response, "403 Forbidden", "text/html", null);
				}
			}

			// レスポンス作成
			if (name.length() > 7 && name.startsWith("data/s/")) {
				// サムネイルS
				response = m_image.getImage_s(name.substring(7));
				if (response == null)
					response = getAssetFile("err_s.webp");
				type = "image/jpeg";
			} else if (name.length() > 7 && name.startsWith("data/m/")) {
				// サムネイルM
				response = m_image.getImage_m(name.substring(7));
				if (response == null)
					response = getAssetFile("err_m.webp");
				type = "image/jpeg";
			} else if (name.length() > 4 && name.startsWith("data/d/")) {
				// スマホ内の画像データ（ダウンロード）
				response = m_image.getImage(name.substring(7));
				if (response == null)
					response = getAssetFile("err_m.webp");

				if (name.endsWith(".jpg") || name.endsWith(".jpeg"))
					type = "image/jpeg";
				else if (name.endsWith(".webp"))
					type = "image/webp";
				else if (name.endsWith(".png"))
					type = "image/png";
				else if (name.endsWith(".bmp"))
					type = "image/bitmap";

				if (response != null) {
					return addHeader_dl(response, "200 OK", type, name.substring(3));
				}
			} else if (name.length() > 5 && name.startsWith("data/")) {
				// スマホ内の画像データ
				response = m_image.getImage(name.substring(5));
				if (response == null)
					response = getAssetFile("err_m.webp");

				if (name.endsWith(".jpg") || name.endsWith(".jpeg"))
					type = "image/jpeg";
				else if (name.endsWith(".png"))
					type = "image/png";
				else if (name.endsWith(".bmp"))
					type = "image/bitmap";
			} else {
				// HTML等のシステムデータ
				if (name.lastIndexOf(".") == -1) {
					// 拡張子が存在しない場合はhtmとして対応
					response = getAssetFile(name + ".htm");
					type = "text/html";
				} else {
					response = getAssetFile(name);
					type = name.substring(name.lastIndexOf("."));

					if (type.equals(".htm") || type.equals(".html")) {
						type = "text/html";
					} else if (type.equals(".css")) {
						type = "text/css";
					} else if (type.equals(".js")) {
						type = "text/javascript";
					} else if (type.equals(".txt")) {
						type = "text/plain";
					} else if (type.equals(".png")) {
						type = "image/png";
					} else if (type.equals(".jpg") || type.equals(".jpeg")) {
						type = "image/jpeg";
					} else {
						type = "application/octet-stream";
					}
				}
			}

			// 要求データが存在しなかった
			if (response == null) {
				response = getAssetFile("_notfound.html");
				return addHeader(response, "404 Not Found", "text/html", null);
			}

			return addHeader(response, "200 OK", type, null);
		}

		private byte[] addHeader(byte[] data, String code, String type, String addHead) {
			String str_head =
				"HTTP/1.1 " + code + "\r\n"
				+ "Server: " + HTTP_SERVER_NAME + "\r\n"
				+ "Content-Type: " + type + "; charset=utf-8\r\n"
				+ "Content-Length: " + data.length + "\r\n"
				+ addHead;

			if (m_keepalive)
				str_head += "Keep-Alive: timeout=5, max=20\r\nConnection: Keep-Alive\r\n";
			else
				str_head += "Connection: close\r\n";

			str_head += "\r\n";

			byte[] head = str_head.getBytes();

			ByteBuffer ret = ByteBuffer.allocate(head.length + data.length);
			ret.put(head);
			ret.put(data);
			return ret.array();
		}

		private byte[] addHeader_dl(byte[] data, String code, String type, String name) {
			String str_head =
				"HTTP/1.1 " + code + "\r\n"
				+ "Server: " + HTTP_SERVER_NAME + "\r\n"
				+ "Content-Type: " + type + "; charset=utf-8\r\n"
				+ "Content-Length: " + data.length + "\r\n"
				+ "Content-Disposition: attachment; filename=\"" + name + "\"\r\n"
				+ "Connection: close\r\n\r\n";

			byte[] head = str_head.getBytes();

			ByteBuffer ret = ByteBuffer.allocate(head.length + data.length);
			ret.put(head);
			ret.put(data);
			return ret.array();
		}

		private String getRequestFileName(byte[] request) {
			String head;

			try {
				head = new String(request,"UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return null;
			}

			if (!head.substring(0, 5).equals("GET /")) {
				return null;
			}

			int idx = head.indexOf(" ", 5);
			if (idx == 5) {
				return "";
			} else if (idx < 5) {
				return null;
			}

			head = head.substring(5, head.indexOf(" ", 5));

			if (head.substring(0, 1).equals("."))
				return ".";

			try {
				return URLDecoder.decode(head, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return null;
			}
		}

		private byte[] getAssetFile(String name) {
			AssetManager assets = m_context.getAssets();
			try (InputStream file = assets.open("html/" + name)) {
				BufferedInputStream bin = new BufferedInputStream(file);

				byte[] data = new byte[1024 * 1024 * 100];
				int readSize = bin.read(data, 0, 1024 * 1024 * 100);
				data = Arrays.copyOf(data, readSize);

				return data;
			} catch (IOException e) {
				//e.printStackTrace();
			}

			return null;
		}


		// クライアント対応ワーカースレッド
		private class HttpWorker implements Runnable {
			private Socket s_sock;

			public HttpWorker(Socket sock) {
				s_sock = sock;
			}

			@Override
			public void run() {
				// プール中に切断されている可能性がある
				if (s_sock == null || !s_sock.isConnected())
					return;

				// 通信の前処理
				try {
					if (m_keepalive)
						s_sock.setKeepAlive(true);

					s_sock.setSoTimeout(30);
				} catch (SocketException e) {
					e.printStackTrace();
					try {
						s_sock.close();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
					return;
				}

				BufferedInputStream in = null;
				BufferedOutputStream out = null;
				try {
					in = new BufferedInputStream(s_sock.getInputStream());
					out = new BufferedOutputStream(s_sock.getOutputStream());
				} catch (IOException e) {
					e.printStackTrace();
					try {
						s_sock.close();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
					return;
				}

				Boolean use_keepalive = false;

				// HTTP通信
				while (true) {
					if (!s_sock.isConnected())
						return;

					// リクエスト受信
					int readSize = 0;
					byte[] data = new byte[HTTP_MAX_REQUEST_SIZE];
					try {
						readSize = in.read(data, 0, HTTP_MAX_REQUEST_SIZE);
					} catch (SocketTimeoutException e) {
						break;
					} catch (IOException e) {
						e.printStackTrace();
						try {
							s_sock.close();
						} catch (IOException ex) {
							ex.printStackTrace();
						}
						return;
					}

					if (readSize == 0 || readSize == HTTP_MAX_REQUEST_SIZE) {
						try {
							s_sock.close();
						} catch (IOException ex) {
							ex.printStackTrace();
						}
						return;
					}

					// リクエストヘッダー受信
					data = Arrays.copyOf(data, readSize);

					if (m_keepalive) {
						String data_str = new String(data);
						if (data_str.indexOf("\r\nConnection: close\r\n") == -1) {
							use_keepalive = false;
						} else {
							if (data_str.indexOf("keep-alive") != -1)
								use_keepalive = true;
							else
								use_keepalive = false;
						}
					}

					// 処理
					data = httpServerExec(data);

					// レスポンス送信
					try {
						out.write(data);
						out.flush();
					} catch (IOException e) {
						e.printStackTrace();
						try {
							s_sock.close();
						} catch (IOException ex) {
							ex.printStackTrace();
						}
						return;
					}

					if (!use_keepalive || !m_keepalive)
						break;

					try {
						// Keep-Aliveのリクエスト待ち
						for (int i = 0; i < 11; i++) {
							if (in.available() != 0) {
								use_keepalive = true;
								break;
							}

							try {
								sleep(500);
							} catch (InterruptedException e) {
								break;
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
						try {
							in.close();
							out.close();
							s_sock.close();
							break;
						} catch (IOException ex) {
							ex.printStackTrace();
							break;
						}
					}

					if (!use_keepalive)
						break;
				}

				// 通信の後処理
				try {
					in.close();
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
					try {
						s_sock.close();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
					return;
				}

				try {
					s_sock.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}
	}



	// 画像最適化用サーバー
	private class ImageService extends Thread {
		private ExecutorService s_exec_worker;
		private String s_root_uri = null;
		private String s_cachedir = null;
		private WatchService s_watcher = null;
		private String s_index = null;
		private TreeMap<String, String> s_images = null;
		ArrayList<String> m_err_files;


		public ImageService(String root_uri) {
			s_exec_worker = Executors.newFixedThreadPool(IMAGE_MAX_REQUEST_CONVERT);
			s_root_uri = root_uri;

			m_err_files = new ArrayList<String>();
		}

		@Override
		public void run() {
			// キャッシュ用ディレクトリの作成
			// TODO: 外部外レージに変更したい
			String hash;
			try {
				MessageDigest md = MessageDigest.getInstance("SHA-256");
				hash = String.format("%040x",  new BigInteger(1, md.digest(s_root_uri.getBytes())));
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				hash = "0";
			}

			s_cachedir = "cache/self/" + hash + "/";
			File cd = new File(getExternalFilesDir(null), s_cachedir);
			if (!cd.isDirectory()) {
				deleteSelfCache();
				cd.mkdirs();
			}

			init();

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				//startWatch();
			}
		}

		private void deleteSelfCache() {
			deleteFolder("cache/self");

		}

		private void deleteFolder(String name) {
			File dir = new File(getExternalFilesDir(null), name);

			if (dir != null && dir.isDirectory()) {
				File[] tars = dir.listFiles();
				if (tars != null) {
					for (int i = 0; i < tars.length; i++) {
						deleteFolder(name + "/" + tars[i].getName());
					}
				}
			}

			dir.delete();
		}

		private void init() {
			s_images = new TreeMap<>();

			ContentResolver contentResolver = getContentResolver();

			Uri rootUri = Uri.parse(m_root_uri);
			Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, DocumentsContract.getTreeDocumentId(rootUri));
			Cursor files = contentResolver.query(childrenUri, SAF_IDX, null, null, null);

			try {
				String id;
				String name;
				String lname;
				String mime;

				while (files != null && files.moveToNext()) {
					id = files.getString(SAF_ID);
					name = files.getString(SAF_NAME);
					mime = files.getString(SAF_MIME);
					lname = name.toLowerCase();

					if (id.length() > 0 && !DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
						if (lname.endsWith(".jpg") || lname.endsWith(".jpeg") || lname.endsWith(".png") || lname.endsWith(".bmp")) {
							s_images.put(name, id);

							s_exec_worker.submit(new ImageWorker(id));
						}
					}
				}
			} finally {
				if (files != null) {
					try {
						files.close();
					} catch (RuntimeException e) {
						throw e;
					} catch (Exception e) {
					}
				}
			}

			// indexの作成
			createIndex(s_images);

			// 不要サムネイルの削除
			try {
				String name;
				File[] cacheFiles = new File(getExternalFilesDir(null), s_cachedir).listFiles(new FilenameFilter() {
					public boolean accept(File file, String str) {
						return str.endsWith(".s") ? true : false;
					}
				});

				if (cacheFiles != null) {
					for (int i = 0; i < cacheFiles.length; i++) {
						if (cacheFiles[i].isFile()) {
							name = cacheFiles[i].getName();
							name = name.substring(0, name.length() - 2);
							if (s_images.get(name) == null) {
								new File(getExternalFilesDir(null), s_cachedir + name + ".t").delete();
								new File(getExternalFilesDir(null), s_cachedir + name + ".s").delete();
								new File(getExternalFilesDir(null), s_cachedir + name + ".m").delete();
							}
						}
					}
				}
			} catch (Exception e) {
			}
		}

		public void shutdown() {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				//stopWatch();
			}

			if (s_exec_worker != null) {
				s_exec_worker.shutdown();
				s_exec_worker = null;
			}

			if (m_delcache) {
				new File(getExternalFilesDir(null) , s_cachedir).delete();
			}
		}

		@RequiresApi(api = Build.VERSION_CODES.O)
		private void startWatch() {
			if (s_watcher != null)
				return;

			Path dir = Paths.get(m_root_uri + "/");

			try {
				s_watcher = FileSystems.getDefault().newWatchService();
				dir.register(s_watcher, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

			WatchKey watchKey = null;

			while (true) {
				try {
					watchKey = s_watcher.take();
				} catch (InterruptedException e) {
					e.printStackTrace();
					stopWatch();
					return;
				} catch (ClosedWatchServiceException e) {
					// NOTE: take()中にclose()すると必ずエラーになるらしいが…
					stopWatch();
					return;
				}

				for (WatchEvent<?> event : watchKey.pollEvents()) {
					if (event.kind() == OVERFLOW)
						continue;

					WatchEvent.Kind<?> kind = event.kind();
					String name = (String) event.context();
					String lname = name.toLowerCase();

					if (new File(getExternalFilesDir(null), s_cachedir + name).isFile()) {
						if (lname.endsWith(".jpg") || lname.endsWith(".jpeg") || lname.endsWith(".png") || lname.endsWith(".bmp")) {
							if (kind == ENTRY_CREATE || kind == ENTRY_MODIFY) {
								s_exec_worker.submit(new ImageWorker(name));
							} else if (kind == ENTRY_DELETE) {
								new File(getExternalFilesDir(null), s_cachedir + name + ".t").delete();
								new File(getExternalFilesDir(null), s_cachedir + name + ".s").delete();
								new File(getExternalFilesDir(null), s_cachedir + name + ".m").delete();
							}
						}
					}

					if (s_watcher == null)
						return;
				}

				if (s_watcher == null || !watchKey.reset()) {
					//stopWatch();
					return;
				}
			}
		}

		@RequiresApi(api = Build.VERSION_CODES.O)
		private void stopWatch() {
			if (s_watcher == null)
				return;

			try {
				s_watcher.close();
				s_watcher = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// index.htmlの作成
		private void createIndex(TreeMap<String, String> images) {
			String index_src = "";

			AssetManager assets = m_context.getAssets();
			try (InputStream file = assets.open("html/_index.html")) {
				BufferedInputStream bin = new BufferedInputStream(file);

				byte[] data = new byte[1024 * 1024];
				int readSize = bin.read(data, 0, 1024 * 1024);
				data = Arrays.copyOf(data, readSize);
				index_src = new String(data);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

			index_src = index_src.replace("<!--#TITLE#-->", "EASY PHOTO SHARE");

			String names = "<script>var imgp = -1; var imgs = [";

			String content = "<!--START-->\n";
			for (String key : images.keySet()) {
				content += "<div data-long-press-delay=\"500\" class=\"sbox\" url=\"data/" + key + "\">";
				content += "<a m=\"data/m/" + key + "\"><img title=\"" + key + "\" class=\"simg\" style=\"opacity:0\" b=\"data/" + key + "\" s=\"data/s/" + key + "\" /></a>";
				content += "<div class=\"bbox\" fname=\"" + key + "\"></div></div>\n";
				names += "'" + key + "', ";
			}
			content += "<!--END-->\n";

			names = names.substring(0, names.length() - 2) + "];</script>";

			index_src = index_src.replace("<!--#CONTENT#-->", content + names);
			s_index = index_src.replace("<!--#COUNT#-->", String.valueOf(images.size()) + "枚の画像");
		}

		// index.htmlを返す
		public byte[] getIndex() {
			return s_index.getBytes();
		}

		public byte[] getImage(String name) {
			try {
				if (m_err_files.indexOf(s_images.get(name)) != -1) {
					// エラーファイルを返す
					AssetManager assets = m_context.getAssets();
					try (InputStream file = assets.open("html/err_m.webp")) {
						BufferedInputStream bin = new BufferedInputStream(file);

						byte[] data = new byte[1024 * 1024 * 100];
						int readSize = bin.read(data, 0, 1024 * 1024 * 100);
						data = Arrays.copyOf(data, readSize);

						return data;
					} catch (IOException e) {
						e.printStackTrace();
						return null;
					}
				}

				// 存在確認とDocID取得
				Uri docUri = DocumentsContract.buildDocumentUriUsingTree(Uri.parse(s_root_uri), s_images.get(name));
				Cursor c = getContentResolver().query(docUri, SAF_IDX, null, null, null, null);

				if (c == null || !c.moveToFirst())
					return null;

				int datasize = Integer.parseInt(c.getString(SAF_SIZE));

				c.close();

				ParcelFileDescriptor parcelFileDescriptor = null;
				FileDescriptor fileDescriptor = null;

				try {
					parcelFileDescriptor = getContentResolver().openFileDescriptor(docUri, "r");
					fileDescriptor = parcelFileDescriptor.getFileDescriptor();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					//parcelFileDescriptor.close();
					return null;
				}

				FileInputStream file = new FileInputStream(fileDescriptor);
				BufferedInputStream bin = new BufferedInputStream(file);

				byte[] data;
				if (datasize == 0) {
					data = new byte[1024 * 1024 * 30];
					int readSize = bin.read(data, 0, 1024 * 1024 * 30);

					bin.close();
					file.close();

					if (readSize == 0)
						return null;
				} else {
					data = new byte[datasize];
					int readSize = bin.read(data, 0, datasize);

					if (datasize != readSize) {
						bin.close();
						file.close();
						parcelFileDescriptor.close();
						return null;
					}
				}

				bin.close();
				file.close();
				parcelFileDescriptor.close();

				return data;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				return null;
			}

			return null;
		}

		public byte[] getImage_s(String name) {
			return getImage_min(name, "s");
		}

		public byte[] getImage_m(String name) {
			return getImage_min(name, "m");
		}

		public byte[] getImage_min(String name, String type) {
			if (s_images.get(name) == null) {
				return null;
			} else if (m_err_files.indexOf(s_images.get(name)) != -1) {
				// エラーファイルを返す
				AssetManager assets = m_context.getAssets();
				try (InputStream file = assets.open("html/_err_" + type + ".webp")) {
					BufferedInputStream bin = new BufferedInputStream(file);

					byte[] data = new byte[1024 * 1024 * 100];
					int readSize = bin.read(data, 0, 1024 * 1024 * 100);
					data = Arrays.copyOf(data, readSize);

					return data;
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}

			try {
				File t = new File(getExternalFilesDir(null) , s_cachedir + name + "." + type);
				if (!t.exists()) {
					Boolean is_exists = false;
					// 作成を少しだけ待機する
					for (int i = 0; i < 30; i++) {
						try {
							Thread.sleep(300);
						} catch (InterruptedException e) {
							e.printStackTrace();
							return null;
						}

						t = new File(getExternalFilesDir(null) , s_cachedir + name + "." + type);
						if (t.exists()) {
							is_exists = true;
							break;
						}
					}

					if (!is_exists)
						return null;
				}

				FileInputStream file = new FileInputStream(new File(getExternalFilesDir(null) , s_cachedir + name + "." + type));
				BufferedInputStream bin = new BufferedInputStream(file);

				int datasize = (int)t.length();

				byte[] data = new byte[datasize];
				int readSize = bin.read(data, 0, datasize);

				if (readSize != datasize) {
					bin.close();
					file.close();
					return null;
				}

				bin.close();
				file.close();

				return data;
			} catch (IOException e) {
				return null;
			} catch (Exception e) {
				return null;
			}
		}

		private void createThumbs(String docID) {
			// サムネイルの作成
			Uri docUri = DocumentsContract.buildDocumentUriUsingTree(Uri.parse(s_root_uri), docID);

			// 名前とサイスを取得
			Cursor c = getContentResolver()
					.query(docUri, SAF_IDX, null, null, null, null);

			if (c == null || !c.moveToFirst()) {
				return;
			}

			String ts_new = c.getString(SAF_DATE);
			String ts_old = null;
			String name = c.getString(SAF_NAME);
			c.close();

			if (name.startsWith("jit_")) {
				name = name.toString();
			}

			// タイムスタンプ
			File file_t = new File(getExternalFilesDir(null) , s_cachedir + name + ".t");
			if (file_t.exists()) {
				try {
					StringBuilder builder = new StringBuilder();
					BufferedReader reader = new BufferedReader(new FileReader(getExternalFilesDir(null) + "/" + s_cachedir + name + ".t"));
					String str = reader.readLine();
					while (str != null) {
						builder.append(str + System.getProperty("line.separator"));
						str = reader.readLine();
					}
					ts_old = str;
				} catch (IOException e) {
					e.printStackTrace();
				}

				if (ts_new.equals(ts_old)) {
					m_err_files.add(name);
					return;
				}
			}

			try {
				FileWriter filewriter = new FileWriter(file_t);
				filewriter.write(ts_new);
				filewriter.close();
			} catch (IOException e) {
				e.printStackTrace();
				m_err_files.add(name);
				return;
			}

			ParcelFileDescriptor parcelFileDescriptor = null;
			FileDescriptor fileDescriptor = null;

			try {
				parcelFileDescriptor = getContentResolver().openFileDescriptor(docUri, "r");
				fileDescriptor = parcelFileDescriptor.getFileDescriptor();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				try {
					file_t.delete();
					parcelFileDescriptor.close();
				} catch (IOException ex) {
				}
				m_err_files.add(name);
				return;
			}

			// サムネイル作成
			Bitmap srcImg = BitmapFactory.decodeFileDescriptor(fileDescriptor);
			if (srcImg == null) {
				try {
					file_t.delete();
					parcelFileDescriptor.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				m_err_files.add(name);
				return;
			}


			ExifInterface exif;
			try {
				exif = new ExifInterface(fileDescriptor);
			} catch (IOException e) {
				e.printStackTrace();
				m_err_files.add(name);
				file_t.delete();
				return;
			}
			int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

			if (orientation == ExifInterface.ORIENTATION_ROTATE_90)
				orientation = 90;
			else if (orientation == ExifInterface.ORIENTATION_ROTATE_180)
				orientation = 180;
			else if (orientation == ExifInterface.ORIENTATION_ROTATE_270)
				orientation = 270;
			else
				orientation = 0;

			if (orientation != 0) {
				Matrix matrix = new Matrix();
				matrix.postRotate(orientation);
				srcImg = Bitmap.createBitmap(srcImg, 0, 0, srcImg.getWidth(), srcImg.getHeight(), matrix, true);
			}

			float src_w = srcImg.getWidth();
			float src_h = srcImg.getHeight();
			float dst_x;
			float dst_y;
			float dst_w;
			float dst_h;

			Bitmap dstImg = null;
			Canvas dstCv = null;

			Paint p = new Paint();
			p.setAntiAlias(true);

			// Sサムネイルのサイズを計算
			if (src_w / IMAGE_MAX_SIZE_S > src_h / IMAGE_MAX_SIZE_S) {
				dst_w = IMAGE_MAX_SIZE_S;
				dst_h = IMAGE_MAX_SIZE_S / src_w * src_h;
				dst_x = 0;
				dst_y = (IMAGE_MAX_SIZE_S - dst_h) / 2;
			} else {
				dst_h = IMAGE_MAX_SIZE_S;
				dst_w = IMAGE_MAX_SIZE_S / src_h * src_w;
				dst_x = (IMAGE_MAX_SIZE_S - dst_w) / 2;
				dst_y = 0;
			}

			// Sサムネイルの作成と保存
			dstImg = Bitmap.createBitmap(IMAGE_MAX_SIZE_S, IMAGE_MAX_SIZE_S, Bitmap.Config.ARGB_8888);
			dstCv = new Canvas(dstImg);
			dstCv.drawBitmap(srcImg, new Rect(0,0, (int)src_w, (int)src_h), new Rect((int)dst_x, (int)dst_y,(int)(dst_x + dst_w),(int)(dst_y + dst_h)), p);

			File file_s = new File(getExternalFilesDir(null) , s_cachedir + name + ".s");
			if (file_s.exists()) {
				file_s.delete();
			}

			try {
				file_s.createNewFile();
				FileOutputStream fOut = new FileOutputStream(file_s);
				dstImg.compress(Bitmap.CompressFormat.JPEG, 75, fOut);

				fOut.flush();
				fOut.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				m_err_files.add(name);
				file_t.delete();
				file_s.delete();
				return;
			} catch (IOException e) {
				e.printStackTrace();
				m_err_files.add(name);
				file_t.delete();
				file_s.delete();
				return;
			}


			// Mサムネイルのサイズを計算
			if (src_w <= IMAGE_MAX_SIZE_M && src_h <= IMAGE_MAX_SIZE_M ) {
				dst_w = src_w;
				dst_h = src_h;
			} else {
				if (src_w / IMAGE_MAX_SIZE_M > src_h / IMAGE_MAX_SIZE_M) {
					dst_w = IMAGE_MAX_SIZE_M;
					dst_h = IMAGE_MAX_SIZE_M / src_w * src_h;
				} else {
					dst_h = IMAGE_MAX_SIZE_M;
					dst_w = IMAGE_MAX_SIZE_M / src_h * src_w;
				}
			}

			// Mサムネイルの作成と保存
			dstImg = Bitmap.createBitmap((int)dst_w, (int)dst_h, Bitmap.Config.ARGB_8888);
			dstCv = new Canvas(dstImg);
			dstCv.drawBitmap(srcImg, new Rect(0,0, (int)src_w, (int)src_h), new Rect(0, 0,(int)dst_w,(int)dst_h), p);

			File file_m = new File(getExternalFilesDir(null) , s_cachedir + name + ".m");
			if (file_m.exists()) {
				file_m.delete();
			}

			try {
				file_m.createNewFile();
				FileOutputStream fOut = new FileOutputStream(file_m);
				dstImg.compress(Bitmap.CompressFormat.JPEG, 75, fOut);

				fOut.flush();
				fOut.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				m_err_files.add(name);
				file_t.delete();
				file_s.delete();
				file_m.delete();
				return;
			} catch (IOException e) {
				e.printStackTrace();
				m_err_files.add(name);
				file_t.delete();
				file_s.delete();
				file_m.delete();
				return;
			}

			try {
				parcelFileDescriptor.close();
			} catch (IOException e) {
				e.printStackTrace();
				m_err_files.add(name);
			}
		}

		// 最適化用ワーカースレッド
		private class ImageWorker implements Runnable {
			String s_docID = null;

			public ImageWorker (String docID) {
				s_docID = docID;
			}

			@Override
			public void run() {
				createThumbs(s_docID);
			}
		}
	}
}

