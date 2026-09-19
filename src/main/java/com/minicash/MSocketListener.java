package com.minicash;

public interface MSocketListener {

    /**
     * 実行対象のアクションタイプ
     */
    String getAction();

    /**
     * データ受信時に実行される処理
     * @param rawJson 受信した raw JSON 文字列
     */
    void onReceive(String rawJson);

}
