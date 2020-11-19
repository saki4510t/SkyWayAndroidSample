# 3 in 1 Sample app for Android using SkyWay Android SDK

# License
  Apache License, Version 2.0

# 内容

基本的にはSkyWayのAndroid SDKに含まれるサンプルアプリと同じです。
ただしSkyWayのアプリを実際に開発する際のテスト通信相手として実行する際に、若干煩雑だったり不便な点があるので一部修正しています。

# オリジナルのSkyWayのサンプルからの変更点

* p2pのビデオチャット・SFUでのビデオチャット・Mesh(p2p)でのビデオチャットの３つを１つとアプリとしてビルド・実行できるようにしています(テキストチャットは入っていません)。
* メイン画面でSkyWayのAPIキーを入力できるようにしています。   
プロジェクトルート直下の`local.properties`ファイルで`SKYWAY_API_KEY`プロパティを設定すると、APIキーのデフォルト値として使用します。   
    また`SKYWAY_DOMAIN`プロパティを設定すると使用するドメイン名を変更できます(変更することはないと思いますので画面には表示/入力するようにはしていませんが)。
* 一番最後に使用したAPIキーを共有プレファレンスに保存し次回起動時に読み込むようににしています。
* SFUまたはmeshでのn:nビデオチャットで一番最後に使用したルーム名共有不レファレンスに保存し次回起動時に読み込むようにしています。
* 現行のAndroid Studioのエディタ上では警告表示が出る部分(例えばcスタイルの配列になっていたりfindViewByIdの返り値をキャストしているところ等)を警告表示ならないように修正しています。
* ピア認証/APIキー認証を行うかどうかをメイン画面で切り替えることができるようにしました。  
  対応するSkyWayのアプリケーション設定の「APIキー認証を利用する」のチェック状態と一致させる必要があります(一致していない場合はエラーになります)。   
  なお、このサンプルではピア認証/APIキー認証を有効にした場合のAndroid側の処理を例示するためだけのものであるため、ピア認証/APIキー認証処理は`OkHttp3`のモックサーバーを利用してアプリ内で行っています。このため本来のピア認証/APIキー認証処理に期待されるAPIキーの不正利用防止機能はほぼありません。   
  実稼働するアプリの場合はセキュアな環境で動作するピア認証/APIキー認証用のAPIサーバーを別途設けそこでピア認証/APIキー認証処理を起こってください。
* p2pのビデオチャット・SFUでのビデオチャット・Mesh(p2p)でのビデオチャットそれぞれでピア認証/APIキー認証処理を実装するのが面倒くさかったのとDIも不要なのでSkyWay関係の共通処理をChartActivityへ分離しました。