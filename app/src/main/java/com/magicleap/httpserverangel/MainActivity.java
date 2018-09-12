package com.magicleap.httpserverangel;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.LinkedList;

public class MainActivity extends AppCompatActivity {

    private static final String SOMETHING_WRONG = "Something went wrong!";
    private static final String SERVER_ADDRESS = "Server address: ";

    private static final int READ_REQUEST_CODE = 42;

    EditText welcomeMsg;
    TextView infoIp;
    TextView infoMsg;
    Switch btnChoose;
    String msgLog = "";

    Uri uri = null;
    Uri uriPrevious = null;

    ServerSocket httpServerSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        welcomeMsg = findViewById(R.id.message_text);
        infoIp = findViewById(R.id.infoip);
        infoMsg = findViewById(R.id.msg);
        btnChoose = findViewById(R.id.choose_function);

        infoIp.setText(getIpInfo());

        HttpServerThread httpServerThread = new HttpServerThread();
        httpServerThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (httpServerSocket != null) {
            try {
                httpServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getIpInfo() {
        String ipAddress = getIpAddress();
        StringBuilder sb = new StringBuilder();
        if(ipAddress != null) {
            sb.append(SERVER_ADDRESS)
                    .append(ipAddress)
                    .append(':')
                    .append(HttpServerThread.HttpServerPORT);
            if(uri != null && btnChoose.isChecked()) {
                sb.append('\n')
                        .append(uri.getPath());
            }
        } else {
            sb.append(SOMETHING_WRONG);
        }
        return sb.toString();
    }

    public void onClickChooseFunction(View view) {
        Button btnPickFile = findViewById(R.id.btn_pick_file);
        TextInputLayout layout = findViewById(R.id.welcome_layout);
        boolean visibility = btnChoose.isChecked();
        btnPickFile.setVisibility(visibility ? View.VISIBLE : View.GONE);
        layout.setVisibility( !visibility ? View.VISIBLE : View.GONE);

        if(!btnChoose.isChecked() && !welcomeMsg.getText().toString().isEmpty()) {
            uriPrevious = uri;
            uri = null;
        } else {
            uri = uriPrevious;
        }
        infoIp.setText(getIpInfo());
    }

    public void onClickBtnCredits(View view) {
        Intent intent = new Intent(this, CreditsActivity.class);
        startActivity(intent);
    }

    public void onClickBtnPickFile(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent resultData) {
        if(requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            uri = null;
            if(resultData != null) {
                uriPrevious = null;
                uri = resultData.getData();
                infoIp.setText(getIpInfo());
            }
        }
    }

    private String getIpAddress() {
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces
                    = NetworkInterface.getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces.nextElement();
                Enumeration<InetAddress> enumInetAddress
                        = networkInterface.getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    private class HttpServerThread extends Thread {

        static final int HttpServerPORT = 8888;

        @Override
        public void run() {
            Socket socket = null;
            try {
                httpServerSocket = new ServerSocket(HttpServerPORT);

                while(true){
                    socket = httpServerSocket.accept();
                    HttpResponseThread httpResponseThread;

                    if(uri == null) {
                        httpResponseThread =
                                new HttpResponseThread(
                                        socket,
                                        welcomeMsg.getText().toString());
                    } else {
                        httpResponseThread =
                                new HttpResponseThread(
                                        socket,
                                        uri);
                    }

                    httpResponseThread.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class HttpResponseThread extends Thread {
        Socket socket;
        String message;
        Uri uri;

        HttpResponseThread(Socket socket, String message){
            this.socket = socket;
            this.message = message;
            uri = null;
        }

        HttpResponseThread(Socket socket, Uri uri) {
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

                msgLog += "Request of " + request
                        + " from " + socket.getInetAddress().toString() + "\n";
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        infoMsg.setText(msgLog);
                    }
                });
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
                ParcelFileDescriptor descriptor = getContentResolver().openFileDescriptor(uri, "r");
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

}
