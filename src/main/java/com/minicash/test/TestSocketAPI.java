package com.minicash.test;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class TestSocketAPI {

    private static final Gson GSON = new Gson();

    public static void main(String[] args){

        System.out.println("TestSocket 起動しました！");


        try (ServerSocket server = new ServerSocket(50001)){

            Socket socket = server.accept();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("サーバー クライアント接続");

            String message = bufferedReader.readLine();

            System.out.println("受信したメッセージ：" + message);


        } catch (Exception e) {
            System.out.println("予期せぬエラーが発生しました：" + e.getMessage());
        }

        System.out.println("通信が終了したため閉じました");


    }

    public static void sendDataAsync(String key , String value){


    }


}
