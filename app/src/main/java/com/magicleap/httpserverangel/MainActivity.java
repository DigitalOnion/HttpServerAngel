package com.magicleap.httpserverangel;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity implements HostCallback {

    private static final String SOMETHING_WRONG = "Something went wrong!";
    private static final String SERVER_ADDRESS = "Server address: ";

    private static final int READ_REQUEST_CODE = 42;

    private HttpServerThread httpServerThread = null;

    EditText welcomeMsg;
    TextView infoIp;
    TextView infoMsg;
    Switch btnChoose;

    Uri uri = null;
    Uri uriPrevious = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        welcomeMsg = findViewById(R.id.message_text);
        infoIp = findViewById(R.id.infoip);
        infoMsg = findViewById(R.id.msg);
        btnChoose = findViewById(R.id.choose_function);

        infoIp.setText(getIpInfo());

        welcomeMsg.addTextChangedListener(new myTextWatcher());

        httpServerThread = new HttpServerThread();
        httpServerThread.setHostCallback(this);
        httpServerThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        httpServerThread.close();
    }

    private class myTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
        @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

        @Override
        public void afterTextChanged(Editable editable) {
            String s = editable.toString();
            if(s != null && s.length() > 0) {
                httpServerThread.setMessage(s);
            } else {
                httpServerThread.setUri(uri);
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
                httpServerThread.setUri(uri);
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

    @Override
    public void logHttpEvent(String httpEvent) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                infoMsg.setText(httpEvent);
            }
        });
    }

    @Override
    public ContentResolver getHostContentResolver() {
        return this.getContentResolver();
    }

}
