# Easy Photo Share

閲覧側はWEBブラウザとQRコードリーダーだけで完結する画像共有専用のHTTPサーバー。
テザリングでプライベートネットワークを作成してどこでも高速通信で共有が可能です。
<br>

## 共有に使うネットワークについて
写真の共有にはインターネットに接続されたネットワークが必要です。

#### その１：テザリング（推奨）
テザリングを行う事によって作成されるネットワークを利用します。
現在のAndroidはゲートウェイが機能していないネットワークへの接続を行わないので推奨手段です。

#### その２：一般的なWi-Fi
WANアクセス可能なルーターが存在している場合に限り動作します。
WifiManager.LocalOnlyHotspot()で作成されたネットワークでは正しく動作しません。
<br>


## 最新パッケージ
#### GitHib - 1.1.3
[apkファイル](app/release/app-release.apk)

#### Google Play - 1.1.3
[apkファイル](https://play.google.com/store/apps/details?id=com.silverintegral.easyphotoshare&hl=ja)
<br>

## サポート
[Twitter](https://twitter.com/xenncamcos)
<br>

## 更新期歴

#### 2020-01-06 1.0.5
初ソースコード公開
