package com.minicash;

import java.util.Map;

public interface MSocketListener {

    /**
     * 実行対象のアクションタイプ
     */
    String getAction();

    /**
     * データ受信時に実行される処理
     * @param data 受信したバース済みのデータ
     */
    void onReceive(Map<String,Object> data);

}
