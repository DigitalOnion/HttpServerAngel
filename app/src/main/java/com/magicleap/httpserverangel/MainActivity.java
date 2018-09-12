package com.magicleap.httpserverangel;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.util.Enumeration;

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

                // test the file!
                File external = android.os.Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                Log.d("LUIS", external.toString());
                if(external.isDirectory()) {
                    for (File file : external.listFiles()) {
                        Log.d("LUIS", file.getName());
                    }
                }
                Log.d("LUIS", "count of files:" + external.length());
                Log.d("LUIS", external.getAbsolutePath());
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
        HttpResponseThread(Socket socket, String message){
            this.socket = socket;
            this.message = message;
        }

        HttpResponseThread(Socket socket, Uri uri) {
            this.socket = socket;
            try {
                RandomAccessFile file = new RandomAccessFile(uri.getPath(), "r");
                byte[] fileBytes = new byte[(int) file.length()];
                file.readFully(fileBytes);
                this.message = new String(fileBytes);
            } catch (IOException e) {
                this.message = null;
            }
        }

        @Override
        public void run() {
            BufferedReader is;
            PrintWriter os;
            String request;
            try {
                is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                request = is.readLine();

                os = new PrintWriter(socket.getOutputStream(), true);

                String response =
                        "<html><head></head>" +
                                "<body>" +
                                "<h1>" + message + "</h1>" +
                                "</body></html>";

                os.print("HTTP/1.0 200" + "\r\n");
                os.print("Content type: text/html" + "\r\n");
                os.print("Content length: " + response.length() + "\r\n");
                os.print("\r\n");
                os.print(response + "\r\n");
                os.flush();
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
    }

}
