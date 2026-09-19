
  ```java
MSocketAPI api = new MSocketAPI("127.0.0.1", 5000);

// 1. 他サーバーからのブロードキャスト通知を処理するリスナーを登録
api.addListener(new MSocketListener() {
@Override
public String getAction() {
return "bank_update_admin";
}

    @Override
    public void onReceive(String rawJson) {
        // 通知を受信したときのマイクラ側処理
        System.out.println("他サーバーで銀行データが更新されました: " + rawJson);
    }
});

// 2. ブロードキャスト通知の常時待ち受けを開始
api.startListening();

// 3. 自分からPingを送る場合は sendRequestAsync を使用（イベントは発生しない）
api.isServerAlive().thenAccept(alive -> {
if (alive) {
System.out.println("Pythonサーバーは正常に動作しています。");
}
});

```