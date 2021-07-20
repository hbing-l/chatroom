package chatroom;
import java.net.*;
import java.io.*;
import java.util.*;

public class Server {
    int port;
    List<Socket> clients;
    ServerSocket server;
    private static Map<String, Socket>userDB = new HashMap();
    private static Map<Socket, List<String>>msgRecord = new HashMap();
    private BufferedReader bufferedReader = null;
    private PrintWriter pw = null;

    public static void main(String[] args) {
        new Server();
    }

    public Server() {
        try {

            port = 12345;
            clients = new ArrayList<Socket>();
            server = new ServerSocket(port);

            while (true) {
                Socket socket = server.accept();
                clients.add(socket);

                // ReceiveClientThread receiveThread = new ReceiveClientThread(socket);
                SendClientThread sendThread = new SendClientThread(socket);
                // receiveThread.start();
                sendThread.start();
            }


        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    class ReceiveClientThread extends Thread {
        Socket ssocket;

        public ReceiveClientThread(Socket s) {
            ssocket = s;
        }

        public void run() {

            try {
                bufferedReader = new BufferedReader(new InputStreamReader(ssocket.getInputStream(), "UTF-8"));
                //读取一行数据
                String str;
                //通过while循环不断读取信息
                while ((str = bufferedReader.readLine()) != null) {
                    //输出打印
                    System.out.println("客户端" + str);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }
    
    class SendClientThread extends Thread {
        Socket ssocket;
        public String msg;

        public SendClientThread(Socket s) {
            ssocket = s;
        }

        public void run() {

            try {
 
                while(true){

                    bufferedReader = new BufferedReader(new InputStreamReader(ssocket.getInputStream(), "UTF-8"));
                    msg = bufferedReader.readLine();

                    //处理客户端输入的字符串
                    if(msg.startsWith("/login")){

                        String userName = msg.split(" ")[1];
                        if(userDB.containsKey(userName)){
                            sendMsg2Me(ssocket, "Name exist, please choose anthoer name.");
                        }else{
                            userDB.put(userName, ssocket);
                            sendMsg2Me(ssocket, "You have logined");
                            loginMsg(userName);
                        }

                    }

                    if(msg.equals("/quit")){

                        userExit(ssocket);
                        break;
                    
                    }

                    if(!msg.startsWith("/")){
                        groupChat(ssocket, msg);
                    }

                    if(msg.startsWith("//hi")){

                        String myName = getUserName(ssocket);

                        if(msg.split(" ").length > 1){
                            String userName = msg.split(" ")[1];
                            sayHi(myName, userName);
                        }else{
                            String msg2me = "你向大家打招呼，“Hi，大家好！我来咯~”";
                            String msg2other = myName + "向大家打招呼，“Hi，大家好！我来咯~”";
                            sendMsg2Me(ssocket, msg2me);
                            sendMsg2Other(myName, msg2other);
                        }

                    }

                    if(msg.startsWith("/to")){

                        String myName = getUserName(ssocket);
                        String[] msgLine = msg.split(" ");
                        try{
                            String chatName = msgLine[1];
                            String chatMsg = msgLine[2];
                            for(int i = 3; i < msgLine.length; i++){
                                chatMsg = chatMsg + " " + msgLine[i];
                            }
                            privateChat(ssocket, myName, chatName, chatMsg);

                        } catch (Exception e){
                            e.printStackTrace();
                        }   

                    }

                    if(msg.equals("/who")){

                        showAllClient(ssocket);

                    }

                    if(msg.equals("/history")){

                        String[] msgLine = msg.split(" ");
                        int start = 0;
                        int end = 0;
                        if(msgLine.length == 3){
                            start = Integer.parseInt(msgLine[1]);
                            end = Integer.parseInt(msgLine[2]);
                        }
                        showHistoryChatRecord(ssocket, start, end);
                    }

                }

                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
    }

    public void showHistoryChatRecord(Socket socket, int start, int end){
        try{
            if(!msgRecord.containsKey(socket)){
                PrintStream printStream = new PrintStream(socket.getOutputStream());
                String msg = "No history chat record";
                printStream.println(msg);
            }else{
                List<String> chatRecordList = msgRecord.get(socket);
                int listSize = chatRecordList.size();
                if(start == 0 && end == 0){
                    start = 1;
                    end = listSize;
                }
                if(end > listSize){
                    end = listSize;
                }
                for(int i = start - 1; i < end; i++){
                    PrintStream printStream = new PrintStream(socket.getOutputStream());
                    String msg = chatRecordList.get(i);
                    printStream.println(msg);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } 
        
    }

    public void addChatMsg(Socket socket, String msg) throws IOException {

        if(msgRecord.containsKey(socket)){
            List<String> chatRecordList = msgRecord.get(socket);
            int num = chatRecordList.size() + 1;
            String addNumChat = num + msg;
            chatRecordList.add(addNumChat);
        }else{
            List<String> chatRecordList = new ArrayList<String>();
            String msg1 = 1 + msg;
            chatRecordList.add(msg1);
            msgRecord.put(socket, chatRecordList);
        }

    }

    public void loginMsg(String userName) throws IOException {
        //1.将Map集合转换为Set集合
        Set<Map.Entry<String,Socket>> set = userDB.entrySet();

        //3.遍历Set集合将群聊信息发给每一个客户端
        for(Map.Entry<String,Socket> entry:set){
            if(!entry.getKey().equals(userName)){
                //取得客户端的Socket对象
                Socket client = entry.getValue();
                //取得client客户端的输出流
                PrintStream printStream = new PrintStream(client.getOutputStream());
                String msg = userName + " has logined";
                addChatMsg(client, msg);
                printStream.println(msg);
            }
            
        }
    }


    public void groupChat(Socket socket,String msg) throws IOException {
        //1.将Map集合转换为Set集合
        Set<Map.Entry<String,Socket>> set = userDB.entrySet();
        //2.遍历Set集合找到发起群聊信息的用户
        String userName = null;
        for(Map.Entry<String,Socket> entry:set){
            if(entry.getValue().equals(socket)){
                userName = entry.getKey();
                break;
            }
        }
        //3.遍历Set集合将群聊信息发给每一个客户端
        for(Map.Entry<String,Socket> entry:set){
            //取得客户端的Socket对象
            Socket client = entry.getValue();
            //取得client客户端的输出流

            // DataOutputStream dos = new DataOutputStream(client.getOutputStream());
            pw = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), "UTF-8")); 
            if(entry.getKey() == userName){
                String sendMsg = "你说:" + msg;
                addChatMsg(client, sendMsg);
                pw.println(sendMsg);
            }else{
                String sendMsg = userName + "说:" + msg;
                addChatMsg(client, sendMsg);
                pw.println(sendMsg);
            }
            pw.flush();

        }
    }
    
    private void privateChat(Socket socket, String myName,String userName,String msg) throws IOException {
        if(!userDB.containsKey(userName)){
            sendMsg2Me(socket, userName + " is not online.");
        }else if(myName == userName){
            sendMsg2Me(socket, "Stop talking to yourself!");
        }else{
            Socket client = userDB.get(userName);
            //3.获取私聊客户端的输出流,将私聊信息发送到指定客户端
            PrintStream printStream = new PrintStream(client.getOutputStream());
            String sendMsg = myName + "对你说:" + msg;
            addChatMsg(client, sendMsg);
            printStream.println(myName + "对你说:" + msg);
            sendMsg2Me(socket, "你对" + userName + "说:" + msg);
        }
    }

    public String getUserName(Socket socket){
        Set<Map.Entry<String,Socket>> set = userDB.entrySet();
        String userName = null;
        for(Map.Entry<String,Socket> entry:set){
            if(entry.getValue().equals(socket)){
                userName = entry.getKey();
                break;
            }
        }
        return userName;
    }

    public void sendMsg2Me(Socket socket, String msg) throws IOException {
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        pw = new PrintWriter(new OutputStreamWriter(dos)); //不带自动刷新的Writer
        pw.println(msg);
        pw.flush();

        addChatMsg(socket, msg);

	}

    public void sendMsg2Other(String userName, String msg) throws IOException {
        Set<Map.Entry<String,Socket>> set = userDB.entrySet();

        //3.遍历Set集合将群聊信息发给每一个客户端
        for(Map.Entry<String,Socket> entry:set){
            if(!entry.getKey().equals(userName)){
                //取得客户端的Socket对象
                Socket client = entry.getValue();
                //取得client客户端的输出流
                PrintStream printStream = new PrintStream(client.getOutputStream());
                addChatMsg(client, msg);
                printStream.println(msg);
            }
            
        }
	}

    public void sayHi(String myName, String chatName) throws IOException {
        Set<Map.Entry<String,Socket>> set = userDB.entrySet();

        for(Map.Entry<String,Socket> entry:set){
            //取得客户端的Socket对象
            Socket client = entry.getValue();
            //取得client客户端的输出流
            PrintStream printStream = new PrintStream(client.getOutputStream());
            String sendMsg;
            if(entry.getKey() == myName){
                sendMsg = "你向" + chatName + "打招呼：“Hi，你好啊~”";
                addChatMsg(client, sendMsg);
                printStream.println(sendMsg);
            }else if(entry.getKey() == chatName){
                sendMsg = myName + "向你打招呼：“Hi，你好啊~”";
                addChatMsg(client, sendMsg);
                printStream.println(sendMsg);
            }else{
                sendMsg = myName + "向" + chatName + "打招呼：“Hi，你好啊~”";
                addChatMsg(client, sendMsg);
                printStream.println(sendMsg);
            }
        }

    }

    public void showAllClient(Socket socket) throws IOException {
        Set<Map.Entry<String,Socket>> set = userDB.entrySet();
        PrintStream printStream = new PrintStream(socket.getOutputStream());
        int num = 0;

        //3.遍历Set集合将群聊信息发给每一个客户端
        String sendMsg = "";
        for(Map.Entry<String,Socket> entry:set){
            String userName = entry.getKey();
            sendMsg = sendMsg + userName + "\n";
            printStream.println(userName);
            num++;
        }
        String allNumMsg = "Total online user: " + num;
        sendMsg += allNumMsg;
        addChatMsg(socket, sendMsg);
        printStream.println(allNumMsg);
        
    }

    private void userExit(Socket socket){
        //1.利用socket取得对应的Key值
        String userName = null;
        for(String key:userDB.keySet()){
            if(userDB.get(key).equals(socket)){
                userName = key;
                break;
            }
        }
        try{
            String exitMsg = userName + " has quit.";
            sendMsg2Me(socket, "You have quited.");
            sendMsg2Other(userName, exitMsg);
            
            msgRecord.remove(socket);
            clients.remove(socket);
            userDB.remove(userName, socket);
            System.out.println("用户"+userName+"已下线!");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

}