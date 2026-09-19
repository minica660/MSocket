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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MSocketAPI {

    private final String host;
    private final int port;

    private final Gson gson = new Gson();

    // Pythonからブロードキャストされたアクションをイベントとして返すためのもの
    private final List<MSocketListener> listeners = new ArrayList<>();

    public MSocketAPI(String host, int port) {
        this.host = host;
        this.port = port;

    }


    /**
     * 通知受取用のリスナーを登録する
     */
    public void addListener(MSocketListener listener) {
        this.listeners.add(listener);
    }


    /**
     * Pythonサーバーへ常時接続し、ブロードキャスト通知を待ち受けるスレッドを開始
     */
    public void startListening() {

        CompletableFuture.runAsync(() -> {

            try (Socket socket = new Socket(host, port);

                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"))) {

                System.out.println("通知用のソケットサーバーに接続しました");

                String message;

                while ((message = in.readLine()) != null) {

                    final String jsonText = message;

                    try {

                        JsonObject json = JsonParser.parseString(jsonText).getAsJsonObject();

                        String actionType = json.has("action") ? json.get("action").getAsString() : "";

                        // アクションタイプ名から一致するListenerを取得
                        for (MSocketListener listener : listeners) {

                            if (listener.getAction().equalsIgnoreCase(actionType)) {

                                listener.onReceive(jsonText);

                            }

                        }

                    }catch (Exception e) {
                        System.out.println("JSON解析エラーが発生しました: " + e.getMessage());
                    }


                }

            }catch (Exception e) {
                System.out.println("常時接続ソケットエラーが発生しました :  " + e.getMessage());
            }


        });


    }


    /**
     * ソケットへの通信を行うメソッド
     */
    public CompletableFuture<Map<String,Object>> sendRequestAsync(String action  , Map<String, Object> data) {

        return CompletableFuture.supplyAsync(() ->{

            Map<String,Object> requestMap = new HashMap<>();

            requestMap.put("action", action);
            requestMap.put("data", data);

            String json = gson.toJson(requestMap);


            try (Socket socket = new Socket(host, port);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                out.println(json);

                String jsonResponse = in.readLine();

                if (jsonResponse == null){
                    return null;
                }

                return gson.fromJson(jsonResponse, new TypeToken<Map<String, Object>>(){}.getType());


            } catch (Exception e) {
                System.out.println("通信エラーが発生しました: " + e.getMessage());

                return null;
            }

//            } catch (UnknownHostException e) {
//                throw new RuntimeException(e);
//            } catch (IOException e) {
//                throw new RuntimeException(e);



        });

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
