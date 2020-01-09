# Easy Photo Share

閲覧側はWEBブラウザとQRコードリーダーだけで完結する画像共有専用のHTTPサーバー。
テザリングでプライベートネットワークを作成してどこでも高速通信で共有が可能です。
<br>

## 最新パッケージ
[Google Play](https://play.google.com/store/apps/details?id=com.silverintegral.easyphotoshare&hl=ja)
<br>
[GitHib](app/release/app-release.apk)
<br>

#### その１：テザリング（推奨）
テザリングを行う事によって作成されるネットワークを利用します。
現在のAndroidはゲートウェイが機能していないネットワークへの接続を行わないので推奨手段です。

#### その２：一般的なWi-Fi
WANアクセス可能なルーターが存在している場合に限り動作します。
WifiManager.LocalOnlyHotspot()で作成されたネットワークでは正しく動作しません。
<br>

## サポート
[Twitter](https://twitter.com/xenncamcos)
<br>

## 更新期歴

#### 2020-01-06 1.0.5
初ソースコード公開
