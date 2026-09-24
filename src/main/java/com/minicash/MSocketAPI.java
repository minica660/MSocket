package com.minicash;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MSocketAPI {

    private final String host;
    private final int port;

    // リスナー用のSocket、PrintWriter
    private Socket socket;
    private PrintWriter out;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private boolean isConnected = false;

    private final AtomicBoolean isListeningStarted = new AtomicBoolean(false);


    private final Gson gson = new Gson();

    // Pythonからブロードキャストされたアクションをイベントとして返すためのもの
    private final List<MSocketListener> listeners = new ArrayList<>();

    // 返答を待っているリクエストの管理プール (requestId -> 待機中のFuture)
    private final Map<String, CompletableFuture<Map<String, Object>>> pendingRequests = new ConcurrentHashMap<>();

    public MSocketAPI(String host, int port) {
        this.host = host;
        this.port = port;

        // ソケットサーバーが起動中か1分ごとにPINGを送信
        this.scheduler.scheduleAtFixedRate(this::sendPing, 1, 1, TimeUnit.MINUTES);


    }


    /**
     * 通知受取用のリスナーを登録する
     */
    public void addListener(MSocketListener listener) {

        synchronized (listeners) {

            if (!listeners.contains(listener)) {

                this.listeners.add(listener);

            }

        }

    }


    /**
     * Pythonサーバーへ常時接続し、ブロードキャスト通知を待ち受けるスレッドを開始
     */
    public void startListening() {


        if (!isListeningStarted.compareAndSet(false, true)) {
            System.out.println("[SocketAPI] 警告: startListening() は既に実行中のため、呼び出しをスキップしました。");
            return;
        }


        CompletableFuture.runAsync(() -> {

            try {

                this.socket = new Socket(host, port);
                this.out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

                this.isConnected = true;


                System.out.println("通知用のソケットサーバーに接続しました");

                String message;

                while ((message = in.readLine()) != null) {

                    final String jsonText = message;

                    try {

                        JsonObject json = JsonParser.parseString(jsonText).getAsJsonObject();

                        String actionType = json.has("action") ? json.get("action").getAsString() : "";

                        if ("PONG".equalsIgnoreCase(actionType)) {
                            continue;
                        }

                        // アクションタイプ名から一致するListenerを取得

                        if (json.has("data")) {

                            Map<String, Object> dataMap = gson.fromJson(
                                    json.get("data"),
                                    new TypeToken<Map<String, Object>>() {
                                    }.getType()
                            );

                            if (dataMap.containsKey("requestId")) {
                                String requestId = (String) dataMap.get("requestId");
                                CompletableFuture<Map<String, Object>> future = pendingRequests.remove(requestId);
                                if (future != null) {
                                    future.complete(dataMap);// sendRequestAs
                                    continue;
                                }
                            }

                            // ソケットから届いたブロードキャストデータを、合致するすべてのリスナーへ送信
                            synchronized (listeners) {
                                for (MSocketListener listener : listeners) {
                                    if (listener.getAction().equalsIgnoreCase(actionType)) {
                                        listener.onReceive(dataMap);
                                    }
                                }
                            }


                        }

                    } catch (Exception e) {
                        System.out.println("JSON解析エラーが発生しました: " + e.getMessage());
                    }


                }

            } catch (Exception e) {
                System.out.println("常時接続ソケットエラーが発生しました :  " + e.getMessage());
            } finally {

                cleanup();

                // ５秒後に再接続
                this.scheduler.schedule(this::startListening, 5, TimeUnit.SECONDS);


            }


        });


    }


    /**
     * ソケットへの通信を行うメソッド
     */
    public CompletableFuture<Map<String, Object>> sendRequestAsync(String action, Map<String, Object> data) {

        if (!isConnected || out == null) {
            System.out.println("[SocketAPI] 通信エラー: サーバーに接続されていません。");
            return CompletableFuture.failedFuture(new IOException("Server is not connected"));
        }

        String requestId = UUID.randomUUID().toString();

        Map<String, Object> requestData = new HashMap<>(data);
        requestData.put("requestId", requestId);

        Map<String, Object> requestMap = Map.of(
                "action", action,
                "data", requestData
        );

        CompletableFuture<Map<String, Object>> responseFuture = new CompletableFuture<>();
        pendingRequests.put(requestId, responseFuture);

        scheduler.schedule(() -> {
            CompletableFuture<Map<String, Object>> future = pendingRequests.remove(requestId);
            if (future != null) {
                future.completeExceptionally(new java.util.concurrent.TimeoutException(
                        "サーバーからの応答がタイムアウトしました: " + action));
            }
        }, 5, TimeUnit.SECONDS);


        String json = gson.toJson(requestMap);
        synchronized (out) {
            out.println(json);
        }

        return responseFuture;

    }


    /**
     * サーバーへデータを送りっぱなしの処理
     *
     * @param action
     * @param data
     * @return
     */
    public CompletableFuture<Void> sendAndForgetAsync(String action, Map<String, Object> data) {
        return CompletableFuture.runAsync(() -> {
            if (!isConnected || out == null) {
                return;
            }

            Map<String, Object> requestMap = Map.of(
                    "action", action,
                    "data", data
            );

            String json = gson.toJson(requestMap);
            synchronized (out) {
                out.println(json);
            }
        });
    }


    private void sendPing() {
        if (isConnected) {
            sendRequestAsync("PING", Map.of()).thenAccept(result -> {
                    })
                    .exceptionally(ex -> {
                        System.out.println("[SocketAPI] PING疎通確認に失敗しました: " + ex.getMessage());
                        return null;
                    });
        }
    }

    public void cleanup() {

        this.isConnected = false;

        this.isListeningStarted.set(false);


        for (CompletableFuture<Map<String, Object>> future : pendingRequests.values()) {
            future.completeExceptionally(new IOException("Connection closed"));
        }
        pendingRequests.clear();

        try {
            if (out != null) out.close();
        } catch (Exception ignored) {
        }
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {
        }

    }


    /**
     * サーバーの起動状態確認 (Ping)
     */
    public CompletableFuture<Boolean> isServerAlive() {
        return sendRequestAsync("ping", Map.of())
                .thenApply(res -> res != null && "ok".equals(res.get("status")))
                .exceptionally(ex -> false);
    }

}
