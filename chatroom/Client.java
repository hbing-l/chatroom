package chatroom;

import java.io.*;
import java.net.*;

public class Client {
    public int port = 12345;
    Socket socket = null;

    public static void main(String[] args) {
        new Client();
    }

    public Client() {

        try {
            socket = new Socket("localhost", port);
            System.out.println("Please login");
            login();
          
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void login(){
        try{
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
            String msg = null;
            msg = br.readLine()+"\r\n";
            String firstLoginName = msg.split(" ")[0];

    
            if (!firstLoginName.equals("/login") && !firstLoginName.equals("/quit")) {
                System.out.println("Invalid command");
                return;
            }else if(firstLoginName.equals("/quit")){
                return;
            }else if(firstLoginName.equals("/login")){
                OutputStream out = socket.getOutputStream();
                out.write(msg.getBytes());
                out.flush();

                Thread send = new SendServerthread();
                Thread receive = new ReceiveServerthread();

                send.start();
                receive.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

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

    class ReceiveServerthread extends Thread {

        public void run() {

            //获取客户端输入流
            try {

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