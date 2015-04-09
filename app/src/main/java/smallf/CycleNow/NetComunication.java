package smallf.CycleNow;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import android.os.Message;
import android.os.Handler;

/**
 * Created by Administrator on 2015/4/1.
 */
public class NetComunication extends Thread{
    private String ServerIP = "192.168.1.100";
    private int ServerPort = 7890;
    private Socket socket;
    PrintWriter  output;
    BufferedReader input;

    public static final String bm="gbk";

    Handler myhandler;

    Boolean bclose = false;

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

    public NetComunication(Handler handler){
        myhandler = handler;
    }

    public Boolean InitSocket(){
        try{
            socket = new Socket(ServerIP, ServerPort);
            output = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), bm)),
                    true);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();
            return false;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public void run(){
        String line;
        String verify_success = "verify success";
        String verify_fail = "verify fail";
        while(!bclose){
            try
            {
                line = "";
                if (socket != null && input != null) {
                    while((line = input.readLine()) != null) {
                        if (!(line.isEmpty())) {
                            Message msg = new Message();
                            if (line.equals(verify_success)) {
                                msg.what = LOGIN_RESULT;
                                msg.arg1 = LOGIN_RESULT_SUCCESS;
                                msg.arg2 = LOGIN_VERIFY_SUCCESS;
                            } else if (line.equals(verify_fail)) {
                                msg.what = LOGIN_RESULT;
                                msg.arg1 = LOGIN_RESULT_FAIL;
                                msg.arg2 = LOGIN_VERIFY_FAIL;
                            }
                            myhandler.sendMessage(msg);
                            input.close();
                            output.close();
                            socket.close();
                            return;
                        }
                    }

                }

            }
            catch (UnknownHostException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void CloseThread(){
        bclose = true;
        try {
            if (socket != null) {
                input.close();
                output.close();
                socket.close();
            }
        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    public void SendMessage(String msg){
        if(output != null) {
            output.println(msg);
        }
    }
}
