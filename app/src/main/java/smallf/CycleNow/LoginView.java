package smallf.CycleNow;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.app.Activity;
import android.os.Message;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.EditText;
import android.content.ContentValues;
import android.database.Cursor;
import java.lang.String;

/**
 * Created by Administrator on 2015/3/19.
 */



public class LoginView extends Activity {

    EditText edittext_user;
    EditText edittext_password;
    Button btn_register;
    Button btn_login;
    ProgressBar progressbar_login;
    TextView textview_login_state;


    int nProgressMax = 100;
    int nThresholdVerify = nProgressMax/3;
    int nThresholdLoad = nProgressMax/3*2;
    int nThresholdOver = nProgressMax-1;

    boolean bTerminateFlag = false; //置为true可停止登陆UI刷新
    boolean bGetRetVerify = false; //登陆信息验证标志，是否已获取验证结果
    boolean bVerifySuccess = false; //验证结果

    /*msg type*/
    int UI_REFRESH = 0;
    int LOGIN_RESULT = 1;

    /*msg arg1, login result*/
    int LOGIN_RESULT_FAIL = 0;
    int LOGIN_RESULT_SUCCESS = 1;

    /*msg arg2, period of login success*/
    int LOGIN_COMPLETE_SUCCESS = 0;
    int LOGIN_VERIFY_SUCCESS = 1;
    int LOGIN_LOADDATA_SUCCESS = 2;

    /*msg arg2, reason of login fail*/
    int LOGIN_NETLINK_BREAK = 0;
    int LOGIN_VERIFY_FAIL = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.loginview);

        edittext_user = (EditText)findViewById(R.id.login_edittext_user);
        edittext_password = (EditText)findViewById(R.id.login_edittext_password);
        btn_register = (Button)findViewById(R.id.login_btn_register);
        btn_login = (Button)findViewById(R.id.login_btn_login);
        progressbar_login = (ProgressBar)findViewById(R.id.login_progressBar_login);
        textview_login_state = (TextView)findViewById(R.id.login_textview_loginstate);


        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String struser = edittext_user.getText().toString();
                String strpassword = edittext_password.getText().toString();
                if(struser.isEmpty() || strpassword.isEmpty()){
                    textview_login_state.setText(R.string.str_login_edittext_null);
                    textview_login_state.setTextColor(Color.rgb(255,0,0));
                    textview_login_state.setVisibility(View.VISIBLE);
                    return;
                }
                edittext_user.setEnabled(false);
                edittext_password.setEnabled(false);
                btn_login.setEnabled(false);

                textview_login_state.setTextColor(Color.rgb(0,0,0));
                textview_login_state.setText(R.string.str_login_state_network);
                progressbar_login.setVisibility(View.VISIBLE);
                textview_login_state.setVisibility(View.VISIBLE);

                bTerminateFlag = false;
                bVerifySuccess = false;
                bGetRetVerify = false;
                new UIThread().start();
                new VerifyThread().start();
            }
        });

        /*开发过程写入nama，password方便登陆，后续删除*/
        edittext_user.setText("李四");
        edittext_password.setText("123");
    }

    private void SendMessage2Handler(int msgtype, int arg1, int arg2){
        Message msg = new Message();
        msg.what = msgtype;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        myhandler.sendMessage(msg);
    }

    class VerifyThread extends Thread{
        @Override
        public void run(){
            NetComunication netlink = new NetComunication(myhandler);
            if(!netlink.InitSocket()){
                bTerminateFlag = true;
                SendMessage2Handler(LOGIN_RESULT, LOGIN_RESULT_FAIL, LOGIN_NETLINK_BREAK);
                return ;
            }
            netlink.start();
            String name = edittext_user.getText().toString();
            String password = edittext_password.getText().toString();
            String msg = "login" + ":" + name + ":" + password;
            netlink.SendMessage(msg);
            SystemClock.sleep(3000);
            /*登陆验证超时*/
            if(!bGetRetVerify){
                bTerminateFlag = true;
                SendMessage2Handler(LOGIN_RESULT, LOGIN_RESULT_FAIL, LOGIN_NETLINK_BREAK);
            }
            netlink.CloseThread();
        }
    }

    class UIThread extends Thread {
        @Override
        public void run() {
            int nProgress = 0;
            while((!bTerminateFlag) && (nProgress < nProgressMax)) {
                SendMessage2Handler(UI_REFRESH, nProgress, -1);
                nProgress++;
                if(nProgress == nThresholdVerify && bVerifySuccess == false){
                    nProgress--;
                }
                SystemClock.sleep(15);
            }

            if(!bTerminateFlag) {
                SendMessage2Handler(LOGIN_RESULT, LOGIN_RESULT_SUCCESS,LOGIN_COMPLETE_SUCCESS);
            }
        }
    }

    Handler myhandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            if(msg.what == UI_REFRESH){
                if(msg.arg1 == nThresholdVerify){
                    textview_login_state.setText(R.string.str_login_state_verify);
                }
                else if(msg.arg1 == nThresholdLoad){
                    textview_login_state.setText(R.string.str_login_state_download);
                }
                else if(msg.arg1 == nThresholdOver){
                    textview_login_state.setText(R.string.str_login_state_ok);
                }
                progressbar_login.setProgress(msg.arg1);
            }
            else if (msg.what == LOGIN_RESULT) {
                bGetRetVerify = true;
                /*登陆失败*/
                if(msg.arg1 == LOGIN_RESULT_FAIL){
                    progressbar_login.setVisibility(View.GONE);
                    edittext_user.setEnabled(true);
                    edittext_password.setEnabled(true);
                    btn_login.setEnabled(true);
                    textview_login_state.setTextColor(Color.rgb(255,0,0));
                    textview_login_state.setVisibility(View.VISIBLE);

                    /*描述登陆失败原因*/
                    if(msg.arg2 == LOGIN_NETLINK_BREAK){
                        textview_login_state.setText(R.string.str_login_network_break);
                    }
                    else if(msg.arg2 == LOGIN_VERIFY_FAIL){
                        textview_login_state.setText(R.string.str_login_verify_fail);
                    }
                }
                else if(msg.arg1 == LOGIN_RESULT_SUCCESS)
                {
                    if(msg.arg2 == LOGIN_VERIFY_SUCCESS){
                        bVerifySuccess = true;
                    }
                    else if(msg.arg2 == LOGIN_COMPLETE_SUCCESS){
                        progressbar_login.setVisibility(View.GONE);
                        textview_login_state.setVisibility(View.GONE);
                        edittext_user.setEnabled(true);
                        edittext_password.setEnabled(true);
                        btn_login.setEnabled(true);

                        Intent in = new Intent();
                        in.setClassName(getApplicationContext(), "smallf.CycleNow.MainFrameView");
                        startActivity(in);
                        LoginView.this.finish();
                    }
                }

            }

            super.handleMessage(msg);
        }
    };
}
