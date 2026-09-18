package com.minicash.test;

import java.io.PrintWriter;
import java.net.Socket;

public class EasyClient {

    public static void main(String[] args){

        System.out.println("クライアント　サーバーに接続します...");

        try(Socket socket = new Socket("localhost",50001);

            PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("クライアント　接続が完了しました！");

            printWriter.println("Hellow world");
            System.out.println("クライアント　メッセージを送信しました");

        } catch (Exception e) {
            System.out.println("予期せぬエラーが発生しました : " + e.getMessage());
        }

        System.out.println("クライアント　通信が終了したため接続を終了しました");

    }

}
