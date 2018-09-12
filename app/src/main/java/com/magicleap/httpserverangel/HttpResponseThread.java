package com.magicleap.httpserverangel;

import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedList;

class HttpResponseThread extends Thread {
    HostCallback host;

    Socket socket;
    String message;
    Uri uri;

    HttpResponseThread(HostCallback host, Socket socket, String message){
        this.host = host;
        this.socket = socket;
        this.message = message;
        uri = null;
    }

    HttpResponseThread(HostCallback host, Socket socket, Uri uri) {
        this.host = host;
        this.socket = socket;
        this.uri = uri;
        message = null;
    }

    @Override
    public void run() {
        BufferedReader is;
        String request;
        try {
            is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            request = is.readLine();

            if(message != null) {
                dumpResponse(message, socket);
            } else {
                dumpResponse(uri, socket);
            }
            socket.close();

            String msgLog = "Request of " + request
                    + " from " + socket.getInetAddress().toString();
            host.logHttpEvent(msgLog);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void dumpResponse(String message, Socket socket) {
        try {
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            String response = "<html><head></head>" +
                    "<body>" +
                    "<h1>" + message + "</h1>" +
                    "</body></html>";

            writer.print("HTTP/1.0 200" + "\r\n");
            writer.print("Content type: text/html" + "\r\n");
            writer.print("Content length: " + response.length() + "\r\n");
            writer.print("\r\n");
            writer.print(response + "\r\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void dumpResponse(Uri uri, Socket socket) {
        try {
            ParcelFileDescriptor descriptor = host.getHostContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = descriptor.getFileDescriptor();
            FileInputStream stream = new FileInputStream(fileDescriptor);

            LinkedList<byte[]> listByteArray = new LinkedList<>();
            int total = 0;
            int available = stream.available();
            while(available > 0) {
                byte[] readBytes = new byte[available];
                int n = stream.read(readBytes);
                listByteArray.add(readBytes);
                total += n;
                available = stream.available();
            }

            OutputStream os = socket.getOutputStream();
            String s = "HTTP/1.0 200" + "\r\n" +
                    "Content type: image/jpeg" + "\r\n" +
                    "Content length: " + total + "\r\n" +
                    "\r\n";
            os.write(s.getBytes());
            for(byte[] bytes : listByteArray) {
                os.write(bytes);
            }
            os.write('\r');
            os.write('\n');
            os.flush();

        } catch ( NullPointerException | IOException e) {

        }
    }
}
