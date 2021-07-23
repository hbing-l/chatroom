package chatroom;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Server {
    int port;
    List<Socket> clients;
    ServerSocket server;
    private static Map<String, Socket>userDB = new ConcurrentHashMap<String, Socket>();
    private static Map<Socket, List<String>>msgRecord = new ConcurrentHashMap<Socket, List<String>>();
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

                    //定义正则表达式
                    String pLogin = "/login\\s+(\\w+)";
                    String pQuit = "/quit";
                    String pHi = "//hi(\\s+(\\w+))?";
                    String pTo = "/to\\s(\\w+)\\s([^.]*.)";
                    String pWho = "/who";
                    String pHistory = "/history(\\s+(\\d+)\\s+(\\d+))?";
                    
                    Pattern patternLogin = Pattern.compile(pLogin);
                    Pattern patternQuit = Pattern.compile(pQuit);
                    Pattern patternHi = Pattern.compile(pHi);
                    Pattern patternTo = Pattern.compile(pTo);
                    Pattern patternWho = Pattern.compile(pWho);
                    Pattern patternHistory = Pattern.compile(pHistory);

                    Matcher matcherLogin = patternLogin.matcher(msg);
                    Matcher matcherQuit = patternQuit.matcher(msg);
                    Matcher matcherHi = patternHi.matcher(msg);
                    Matcher matcherTo = patternTo.matcher(msg);
                    Matcher matcherWho = patternWho.matcher(msg);
                    Matcher matcherHistory = patternHistory.matcher(msg);

                    if(matcherLogin.matches()){

                        String userName = matcherLogin.group(1);
                        if(userDB.putIfAbsent(userName, ssocket) != null){
                            sendMsg2Me(ssocket, "Name exist, please choose anthoer name.");
                        }else{
                            sendMsg2Me(ssocket, "You have logined");
                            loginMsg(userName);
                        }

                    }else if(matcherQuit.matches()){

                        userExit(ssocket);
                        break;

                    }else if(matcherHi.matches()){

                        String myName = getUserName(ssocket);
                        if(matcherHi.group(1) != null){
                            String userName = matcherHi.group(2);
                            sayHi(myName, userName);
                        }else{
                            String msg2me = "你向大家打招呼，“Hi，大家好！我来咯~”";
                            String msg2other = myName + "向大家打招呼，“Hi，大家好！我来咯~”";
                            sendMsg2Me(ssocket, msg2me);
                            sendMsg2Other(myName, msg2other);
                        }

                    }else if(matcherTo.matches()){

                        String myName = getUserName(ssocket);
                        String chatName = matcherTo.group(1);
                        String chatMsg = matcherTo.group(2);
                        privateChat(ssocket, myName, chatName, chatMsg);

                    }else if(matcherWho.matches()){

                        showAllClient(ssocket);

                    }else if(matcherHistory.matches()){

                        int start = 0;
                        int end = 0;
                        if(matcherHistory.group(1) != null){
                            start = Integer.parseInt(matcherHistory.group(2));
                            end = Integer.parseInt(matcherHistory.group(3));
                        }

                        showHistoryChatRecord(ssocket, start, end);

                    }else{

                        groupChat(ssocket, msg);

                    }

                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //查看历史消息
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

    //保存聊天消息
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

    //登录操作
    public void loginMsg(String userName) throws IOException {

        Set<Map.Entry<String,Socket>> set = userDB.entrySet();

        for(Map.Entry<String,Socket> entry:set){
            if(!entry.getKey().equals(userName)){
                Socket client = entry.getValue();
                PrintStream printStream = new PrintStream(client.getOutputStream());
                String msg = userName + " has logined";
                addChatMsg(client, msg);
                printStream.println(msg);
            }
        }
    }

    //群聊
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
    
    //私聊
    private void privateChat(Socket socket, String myName,String userName,String msg) throws IOException {

        if(!userDB.containsKey(userName)){
            sendMsg2Me(socket, userName + " is not online.");
        }else if(myName.equals(userName)){
            sendMsg2Me(socket, "Stop talking to yourself!");
        }else{
            Socket client = userDB.get(userName);
            //获取私聊客户端的输出流,将私聊信息发送到指定客户端
            PrintStream printStream = new PrintStream(client.getOutputStream());
            String sendMsg = myName + "对你说:" + msg;
            addChatMsg(client, sendMsg);
            printStream.println(myName + "对你说:" + msg);
            sendMsg2Me(socket, "你对" + userName + "说:" + msg);
        }

    }

    //获取用户姓名
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

    //给自己发送消息
    public void sendMsg2Me(Socket socket, String msg) throws IOException {

        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        pw = new PrintWriter(new OutputStreamWriter(dos)); //不带自动刷新的Writer
        pw.println(msg);
        pw.flush();

        addChatMsg(socket, msg);

	}

    //给除自己以外的其他人发送消息
    public void sendMsg2Other(String userName, String msg) throws IOException {

        Set<Map.Entry<String,Socket>> set = userDB.entrySet();

        for(Map.Entry<String,Socket> entry:set){
            if(!entry.getKey().equals(userName)){
                Socket client = entry.getValue();
                PrintStream printStream = new PrintStream(client.getOutputStream());
                addChatMsg(client, msg);
                printStream.println(msg);
            }
        }

	}

    //预设消息hi
    public void sayHi(String myName, String chatName) throws IOException {

        Set<Map.Entry<String,Socket>> set = userDB.entrySet();

        for(Map.Entry<String,Socket> entry:set){
            Socket client = entry.getValue();
            PrintStream printStream = new PrintStream(client.getOutputStream());
            String sendMsg;

            if(entry.getKey().equals(myName)){
                sendMsg = "你向" + chatName + "打招呼：“Hi，你好啊~”";
                addChatMsg(client, sendMsg);
                printStream.println(sendMsg);
            }else if(entry.getKey().equals(chatName)){
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

    //显示所有用户
    public void showAllClient(Socket socket) throws IOException {

        Set<Map.Entry<String,Socket>> set = userDB.entrySet();
        PrintStream printStream = new PrintStream(socket.getOutputStream());
        int num = 0;

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

    //用户退出
    private void userExit(Socket socket){

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
            
            //移除用户信息、消息记录以及客户端
            msgRecord.remove(socket);
            clients.remove(socket);
            userDB.remove(userName, socket);
            System.out.println("用户"+userName+"已下线!");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

}