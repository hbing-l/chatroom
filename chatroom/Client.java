package chatroom;

import java.io.*;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {

    public int port = 12345;
    Socket socket = null;

    public static void main(String[] args) {
        new Client();
    }

    public Client() {

        try {
            socket = new Socket("localhost", port);
            //第一步 登录
            System.out.println("Please login");
            login();
           
            //登录成功后开启用户接收和发送线程
            SendServerthread send = new SendServerthread();
            ReceiveServerthread receive = new ReceiveServerthread();

            send.start();
            receive.start();
            
          
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void login(){

        try{

            while(true){

                BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
                String msg = null;
                msg = br.readLine();

                String pLogin = "/login\\s+(\\w+)";
                String pQuit = "/quit";

                Pattern patternLogin = Pattern.compile(pLogin);
                Pattern patternQuit = Pattern.compile(pQuit);

                Matcher matcherLogin = patternLogin.matcher(msg);
                Matcher matcherQuit = patternQuit.matcher(msg);

                if(matcherLogin.matches()){
                    msg += "\r\n";
                    OutputStream out = socket.getOutputStream();
                    out.write(msg.getBytes());
                    out.flush();
                    break;
                }else if(matcherQuit.matches()){
                    socket.close();
                    //终止程序
                    System.exit(0);
                    return;
                }else{
                    System.out.println("Invalid command");
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    //发送线程
    class SendServerthread extends Thread {

        public void run() {
            try {
                while(true){
                    //获取客户端的输出流
                    OutputStream out = socket.getOutputStream();
                    //从键盘中输入信息
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));

                    String msg = null;
                    msg = br.readLine()+"\r\n";
                    // String s = new String(msg.getBytes("iso8859-1"),"UTF-8");
                    out.write(msg.getBytes());
                    out.flush();
                }
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //接收线程
    class ReceiveServerthread extends Thread {

        public void run() {
            try {
                //获取客户端输入流
                BufferedReader bufferinput;
                InputStream input = socket.getInputStream();
                bufferinput = new BufferedReader(new InputStreamReader(input, "UTF-8"));
                String line;
            
                while(true){
                    //获取消息
                    if((line=bufferinput.readLine())!=null){
                        System.out.println(line);
                        if(line.equals("You have quited.")){
                            socket.close();
                            //终止程序
                            System.exit(0);
                            return;
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

}