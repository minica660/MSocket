
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




## 使い方
```java
package com.minicash;

import org.bukkit.plugin.java.JavaPlugin;

public class MiniCashPlugin extends JavaPlugin {

    // プラグイン全体で1本だけ維持するAPIインスタンス
    private static MSocketAPI socketAPI;

    @Override
    public void onEnable() {
        // ① プラグイン起動時に「1回だけ」インスタンスを作り、常時接続を開始する
        socketAPI = new MSocketAPI("localhost", 12345);
        socketAPI.startListening(); // 【通信枠はここでの1本だけで確定】

        // ② 他のクラス（機能）から通知を受け取りたい場合は、この1本のAPIにリスナーを足していく
        socketAPI.addListener(new ChatMessageListener());
        socketAPI.addListener(new MoneyUpdateListener());
    }

    @Override
    public void onDisable() {
        // プラグイン終了時に安全にクローズ
        if (socketAPI != null) {
            socketAPI.cleanup();
        }
    }

    /**
     * 他のクラス（別のプラグインや機能）から通信を行いたいときは、このメソッド経由でAPIを取得する
     */
    public static MSocketAPI getSocketAPI() {
        return socketAPI;
    }
}

```