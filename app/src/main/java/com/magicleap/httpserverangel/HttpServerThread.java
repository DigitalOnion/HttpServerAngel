package com.magicleap.httpserverangel;

import android.net.Uri;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServerThread extends Thread {
    static final int HttpServerPORT = 8888;

    private ServerSocket httpServerSocket;
    private Uri uri = null;
    private String message = null;

    private HostCallback hostCallback = null;

    public void setHostCallback(HostCallback hostCallback) {
        this.hostCallback = hostCallback;
    }

    public void setUri(Uri uri) {
        this.message = null;
        this.uri = uri;
    }

    public void setMessage(String message) {
        this.message = message;
        this.uri = null;
    }

    public void close() {
        if (httpServerSocket != null) {
            try {
                httpServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        Socket socket = null;
        try {
            httpServerSocket = new ServerSocket(HttpServerPORT);

            while(true){
                socket = httpServerSocket.accept();
                HttpResponseThread httpResponseThread = null;

                if(message != null) {
                    httpResponseThread =
                            new HttpResponseThread(
                                    hostCallback,
                                    socket,
                                    message);
                } else if(uri != null) {
                    httpResponseThread =
                            new HttpResponseThread(
                                    hostCallback,
                                    socket,
                                    uri);
                }

                if(httpResponseThread != null) {
                    httpResponseThread.start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
