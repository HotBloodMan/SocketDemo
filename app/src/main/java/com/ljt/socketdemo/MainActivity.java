package com.ljt.socketdemo;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int MESSAGE_RECEIVE_NEW_MSG = 1;
    private static final int MESSAGE_SOCKET_CONNECTED = 2;

    private Button mSendButton;
    private TextView mMessageTextView;
    private EditText mMessageEditText;

    private PrintWriter mPrintWriter;
    private Socket mClientSocket;


    private Handler mHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MESSAGE_RECEIVE_NEW_MSG:
                    mMessageTextView.setText(mMessageTextView.getText()+(String)msg.obj);
                    break;
                case MESSAGE_SOCKET_CONNECTED:
                    mSendButton.setEnabled(true);
                    break;
                default:
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMessageTextView = (TextView) findViewById(R.id.msg_container);
        mSendButton = (Button) findViewById(R.id.send);
        mSendButton.setOnClickListener(this);
        mMessageEditText = (EditText) findViewById(R.id.msg);
        Intent intent = new Intent(this,TcpServerService.class);
        startService(intent);
        new Thread(){
            @Override
            public void run() {
                super.run();
                //开启一个子线程，连接服务端
                connectTcpServer();
            }
        }.start();

    }
    //格式化时间
    private String formatDataTime(long time){
        return new SimpleDateFormat("(HH:mm:ss)").format(new Date(time));
    }

    private void connectTcpServer() {
        Socket socket=null;
        while(socket==null){
            try {
                socket=new Socket("localhost",8688);
                mClientSocket=socket;
                mPrintWriter=new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),true);
                //当连接服务器端成功时，发送按钮是可以点的
                mHandler.sendEmptyMessage(MESSAGE_SOCKET_CONNECTED);
                System.out.println("connect server success");
            } catch (IOException e) {
                SystemClock.sleep(1000);
                System.out.println("connect faild.....");
            }
        }
        try {
            //接收服务器端的消息
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (!MainActivity.this.isFinishing()){
                String msg = br.readLine();
                System.out.println("receive :"+msg);
                if(msg !=null){
                    String time=formatDataTime(System.currentTimeMillis());
                    final String showedMsg ="server " + time + ":" + msg
                            + "\n";
                    //服务端发来消息后，发到主线程，显示界面上。
                    mHandler.obtainMessage(MESSAGE_RECEIVE_NEW_MSG,showedMsg).sendToTarget();
                }
            }
            System.out.println("quit...");
            MyUtils.close(mPrintWriter);
            MyUtils.close(br);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onClick(View v) {
        if (v == mSendButton) {
            final String msg = mMessageEditText.getText().toString();
            if (!TextUtils.isEmpty(msg) && mPrintWriter != null) {
                mPrintWriter.println(msg);
                mMessageEditText.setText("");
                String time = formatDataTime(System.currentTimeMillis());
                final String showedMsg = "self " + time + ":" + msg + "\n";
                mMessageTextView.setText(mMessageTextView.getText() + showedMsg);
            }
        }
    }

    @Override
    protected void onDestroy() {
        //退出界面时，关闭socket
        if (mClientSocket != null) {
            try {
                mClientSocket.shutdownInput();
                mClientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();

    }
}
