package com;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

//import socket_chat_server.Server.ClientThread;
//import socket_chat_server.Server.SelecTry;

public class Client{

    private JPanel sendPanel;
    private JButton btn_send_file;
    private JFrame frame;
    private JList userList;
    private JTextArea textArea;
    private JTextField textField;
    private JTextField txt_port;
    private JTextField txt_hostIp;
    private JTextField txt_name;
    private JButton btn_start;
    private JButton btn_stop;
    private JButton btn_send;
    private JPanel northPanel;
    private JPanel southPanel;
    private JScrollPane rightScroll;
    private JScrollPane leftScroll;
    private JSplitPane centerSplit;

    private JPanel logPanle;
    private JFrame loginframe;
    private JLabel label_username;
    private JLabel label_password;
    private JTextField txt_login_name;
    private JTextField txt_password;
    private JTextField txt_login_ip;
    private JTextField txt_login_port;
    private JTextField txt_login_forget;
    private JButton btn_submit;
    private JButton btn_zhuce;
    private JButton btn_forget_pass;

    private DefaultListModel listModel;
    private boolean isConnected = false;

    private int sendfor_who;
    private int server_port=0;

    private ServerSocket serverSocket;
    private ServerThread serverThread;
    private Socket socketfor_p2p;
    private boolean isConnected_p2p = false;
    private ArrayList<ClientThread> clients;//客户线程数组
    private PrintWriter P2P_printWriter;//点对点服务的输出流
    private BufferedReader P2P_bufferReader;//点对点服务的输入流
    private MessageThread_P2P messageThread_for_p2p;// 负责接收p2p消息的线程
    private Map<String, Boolean> P2P_connected_user = new HashMap<String, Boolean>();

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private MessageThread messageThread;// 负责接收消息的线程
    private Map<String, User> onLineUsers = new HashMap<String, User>();// 所有在线用户
    private String myIP = "";//每一个客户端都有唯一的IP地址

    // 主方法,程序入口
    public static void main(String[] args) throws BindException {
        new Client();

    }

    class SelecTry implements ListSelectionListener
    {
        int change=0,who;
        public void valueChanged(ListSelectionEvent e){
            //System.out.println("you selected:"+listModel.getElementAt(userList.getSelectedIndex()));
            sendfor_who=userList.getSelectedIndex();
            isConnected_p2p = false;
        }

    }

    /**
     * 连接服务器
     *
     * @param port
     * @param hostIp
     * @param name
     */
    public boolean connectServer_p2p(int port, String hostIp, String name) {
        // 连接服务器
        try {
            socketfor_p2p = new Socket(hostIp, port);// 根据端口号和服务器ip建立连接
            P2P_printWriter = new PrintWriter(socketfor_p2p.getOutputStream());
            P2P_bufferReader = new BufferedReader(new InputStreamReader(socketfor_p2p
                    .getInputStream()));

            messageThread_for_p2p = new MessageThread_P2P(P2P_bufferReader);
            messageThread_for_p2p.start();
            P2P_connected_user.put(name,true);
            isConnected_p2p = true;// 已经连接上了
            return true;
        } catch (Exception e) {
            textArea.append("与端口号为：" + port + "    IP地址为：" + hostIp
                    + "   的服务连接失败!" + "\r\n");
            isConnected_p2p = false;// 未连接上
            return false;
        }
    }

    /**
     * 关闭服务
     */
    @SuppressWarnings("deprecation")
    public void closeServer() {
        try {
            if (serverThread != null)
                serverThread.stop();// 停止服务器线程

            for (int i = clients.size() - 1; i >= 0; i--) {
                // 给所有在线用户发送关闭命令
                clients.get(i).getWriter().println("CLOSE#"+frame.getTitle());
                clients.get(i).getWriter().flush();
                // 释放资源
                clients.get(i).stop();// 停止此条为客户端服务的线程
                clients.get(i).reader_ptp.close();
                clients.get(i).writer_ptp.close();
                clients.get(i).socket.close();
                clients.remove(i);
            }
            if (serverSocket != null) {
                serverSocket.close();// 关闭服务器端连接
            }
            listModel.removeAllElements();// 清空用户列表
//            isStart = false;
        } catch (IOException e) {
            e.printStackTrace();
//            isStart = true;
        }
    }

    // 不断接收消息的线程
    class MessageThread_P2P extends Thread {
        private BufferedReader reader_ptp;

        // 接收消息线程的构造方法
        public MessageThread_P2P(BufferedReader reader) {
            this.reader_ptp = reader;

        }

        // 被动的关闭连接
        public synchronized void closeCon() throws Exception {
            System.out.println("close :*************");
            // 被动的关闭连接释放资源
            if (reader_ptp != null) {
                reader_ptp.close();
            }
            if (P2P_printWriter != null) {
                P2P_printWriter.close();
            }
            if (socketfor_p2p != null) {
                socketfor_p2p.close();
            }
            isConnected_p2p = false;// 修改状态为断开

        }

        public void run() {
            String message = "";
            while (true) {
                try {
                    message = reader_ptp.readLine();
                    StringTokenizer stringTokenizer = new StringTokenizer(
                            message, "/#");
                    String command = stringTokenizer.nextToken();// 命令
                    if (command.equals("CLOSE"))// 服务器已关闭命令
                    {
                        String user = stringTokenizer.nextToken();
                        textArea.append("用户 "+user+"  已下线，p2p服务已关闭!\r\n");
                        closeCon();// 被动的关闭连接
                        JOptionPane.showMessageDialog(frame, "用户 "+user+"  已下线，p2p服务已关闭!", "错误",
                                JOptionPane.ERROR_MESSAGE);
                        return;// 结束线程
                    } else if (command.equals("FILE")) {
                        int portNumber = Integer.parseInt(stringTokenizer.nextToken());
                        String fileName = stringTokenizer.nextToken();
                        long fileSize = Long.parseLong(stringTokenizer.nextToken());
                        String ip = stringTokenizer.nextToken();
                        String Nickname = stringTokenizer.nextToken();
                        ReceiveFileThread receiveFile = new ReceiveFileThread(textArea,frame,ip, portNumber, fileName, fileSize, Nickname);
                        receiveFile.start();
                        textArea.append("从 "+Nickname+" 接受文件:"+fileName+",大小为:"+fileSize
                                +"ip:"+ip+"port:"+portNumber+"\r\n");
                    } else {// 普通消息
                        textArea.append(""+message + "\r\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 启动服务器
     *
     * @param port
     * @throws java.net.BindException
     */
    public void serverStart(int port) throws java.net.BindException {
        try {
            clients = new ArrayList<ClientThread>();
            serverSocket = new ServerSocket(port);
            serverThread = new ServerThread(serverSocket);
            serverThread.start();
            server_port = serverSocket.getLocalPort();
            InetAddress addr = InetAddress.getLocalHost();
            myIP = addr.getHostAddress();//获得本机IP
//            myIP = serverSocket.getInetAddress().getHostAddress();
            System.out.println("mmyIP=="+myIP+"\r\n");
        } catch (BindException e) {
            throw new BindException("端口号已被占用，请换一个！");
        } catch (Exception e1) {
            e1.printStackTrace();
            throw new BindException("启动服务器异常！");
        }
    }

    /**
     * 为另一个主动链接的客户端提供服务的线程
     */
    class ClientThread extends Thread {
        private Socket socket;
        private BufferedReader reader_ptp;
        private PrintWriter writer_ptp;
        private User user;

        public BufferedReader getReader() {
            return reader_ptp;
        }

        public PrintWriter getWriter() {
            return writer_ptp;
        }

        public User getUser() {
            return user;
        }

        // 客户端线程的构造方法
        public ClientThread(Socket socket) {
            try {
                this.socket = socket;
                reader_ptp = new BufferedReader(new InputStreamReader(socket
                        .getInputStream()));
                writer_ptp = new PrintWriter(socket.getOutputStream());

                // 接收客户端的基本用户信息
                String inf = reader_ptp.readLine();
                StringTokenizer st = new StringTokenizer(inf, "#");
                user = new User(st.nextToken(), socket.getLocalAddress().toString());
                // 反馈连接成功信息
                writer_ptp.println(frame.getTitle()+"  对你说：  "+user.getName()+"/"+user.getIp()+"你好！"+"与我"+frame.getTitle()+"建立链接成功！");
                writer_ptp.flush();
//                // 反馈当前在线用户信息
//                if (clients.size() > 0) {
//                    String temp = "";
//                    for (int i = clients.size() - 1; i >= 0; i--) {
//                        temp += (clients.get(i).getUser().getName() + "/" + clients
//                                .get(i).getUser().getIp())
//                                + "#";
//                    }
//                    writer.println("USERLIST#" + clients.size() + "#" + temp);
//                    writer.flush();
//                }
//                // 向所有在线用户发送该用户上线命令
//                for (int i = clients.size() - 1; i >= 0; i--) {
//                    clients.get(i).getWriter().println(
//                            "ADD#" + user.getName() + user.getIp());
//                    clients.get(i).getWriter().flush();
//                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @SuppressWarnings("deprecation")
        public void run() {// 不断接收客户端的消息，进行处理。
            String message = null;
            while (true) {
                try {
                    message = reader_ptp.readLine();// 接收客户端消息
                    StringTokenizer stringTokenizer = new StringTokenizer(message,"/#");
                    String command = stringTokenizer.nextToken();
                    if (command.equals("CLOSE"))// 下线命令
                    {
                        textArea.append("与"+this.getUser().getName()
                                + this.getUser().getIp() + "建立连接成功!\r\n");
                        // 断开连接释放资源
                        this.getUser().setState(0);
                        reader.close();
                        writer.close();
                        socket.close();

                    } else if (command.equals("FILE")) {
                        int portNumber = Integer.parseInt(stringTokenizer.nextToken());
                        String fileName = stringTokenizer.nextToken();
                        long fileSize = Long.parseLong(stringTokenizer.nextToken());
                        String ip = stringTokenizer.nextToken();
                        String Nickname = stringTokenizer.nextToken();
                        ReceiveFileThread receiveFile = new ReceiveFileThread(textArea,frame,ip, portNumber, fileName, fileSize, Nickname);
                        receiveFile.start();
                        textArea.append("从 "+Nickname+" 接受文件 :"+fileName+",大小为:  "+fileSize
                                +"   ip: "+ip+"    port:"+portNumber+"\r\n");
                    }else {
                        textArea.append(user.getName()+"  对你说： "+message+"\r\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * 服务线程
     */
    class ServerThread extends Thread {
        private ServerSocket serverSocket;

        // 服务器线程的构造方法
        public ServerThread(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        public void run() {
            while (true) {// 不停的等待客户端的链接
                try {
                    Socket socket = serverSocket.accept();
                    ClientThread client = new ClientThread(socket);
                    client.start();// 开启对此客户端服务的线程
                    clients.add(client);
                    textArea.append("有新用户p2p链接\r\n");
//                    user_name_update();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public void sendFile() {
        //文件选择对话框启动，当选择了文件以后给每一个client发送文件
        JFileChooser sourceFileChooser = new JFileChooser(".");
        sourceFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int status = sourceFileChooser.showOpenDialog(frame);
        File sourceFile = new File(sourceFileChooser.getSelectedFile().getPath());
        //服务器text area提示
        textArea.append("发送文件：" + sourceFile.getName() + "\r\n");

        if(sendfor_who==0){
            textArea.append("对服务器发送文件!");
        }else{
            StringTokenizer st = new StringTokenizer(listModel.getElementAt(sendfor_who)+"", "---()");
            String user_name = st.nextToken();
            String user_state = st.nextToken();
            if (user_state.equals("在线")) {
                for (int i = clients.size()-1; i >= 0; i--) {
                    if (clients.get(i).getUser().getName().equals(user_name)) {
                        SendFileThread sendFile = new SendFileThread(frame, clients.get(i).socket, frame.getTitle(), sourceFileChooser, status);
                        sendFile.start();
                        //client端提示
                        textArea.append("给  "+user_name+"  发送一个文件：" + sourceFile.getName() + "\r\n");
                        return;
                    }
                }
                SendFileThread sendFile = new SendFileThread(frame, socketfor_p2p, frame.getTitle(), sourceFileChooser, status);
                sendFile.start();
                //client端提示
                textArea.append("给  "+user_name+"  发送一个文件：" + sourceFile.getName() + "\r\n");
            }else{
                JOptionPane.showMessageDialog(frame, "用户不在线，不能发送文件！");
            }
        }

    }



    // 执行发送
    public void send() {
        if (!isConnected) {
            JOptionPane.showMessageDialog(frame, "还没有连接服务器，无法发送消息！", "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        String message = textField.getText().trim();
        if (message == null || message.equals("")) {
            JOptionPane.showMessageDialog(frame, "消息不能为空！", "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        //sendMessage(frame.getTitle() + "#" + "ALL" + "#" + message);
        if(sendfor_who==0){
            sendMessage(frame.getTitle() + "#" + "ALL" + "#" + message);
            textField.setText(null);
        }else{
            StringTokenizer st = new StringTokenizer(listModel.getElementAt(sendfor_who)+"", "---()");
            String user_name = st.nextToken();
            String user_state = st.nextToken();
            System.out.print("user_state:" + user_state);
            if (user_state.equals("在线")) {
                for (int i = clients.size()-1; i >= 0; i--) {
                    if (clients.get(i).getUser().getName().equals(user_name)) {
                        clients.get(i).writer_ptp.println("对 "+user_name+"  说：  "+message+"\r\n");
                        clients.get(i).writer_ptp.flush();
                        textArea.append("对  "+user_name+"  说： "+message+"\r\n");
                        textField.setText(null);
                        return;
                    }
                }
                if (!isConnected_p2p) {
                    JOptionPane.showMessageDialog(frame, "点对点即将连接！");
                    sendMessage("P2P#"+user_name);
                }else{
                    P2P_printWriter.println(message);
                    P2P_printWriter.flush();
                    textArea.append("对  "+user_name+"  说： "+message+"\r\n");
                    textField.setText(null);
                }

            }else{
                JOptionPane.showMessageDialog(frame, "用户不在线，已存为留言！");
                sendMessage("LIXIAN#"+frame.getTitle() + "#" + user_name + "#" + message);
                textArea.append("对  "+user_name+"  留言： "+message+"\r\n");
                textField.setText(null);
            }
        }
    }

    public void Login(){
        Font font = new Font("宋体", 1, 16);

        logPanle = new JPanel();
        //logPanle.setLayout(new GridLayout(2, 2));
        logPanle.setBounds(2, 45, 250, 225);
        logPanle.setBackground(Color.lightGray);
        logPanle.setLayout(new GridLayout(5, 2, 20, 20));

        label_username = new JLabel("用户名:");
        label_username.setFont(font);
        label_username.setHorizontalAlignment(SwingConstants.CENTER);
        logPanle.add(label_username);

        txt_login_name = new JTextField("name");
        txt_login_name.setFont(font);
        logPanle.add(txt_login_name);

        label_password = new JLabel("密 码:");
        label_password.setFont(font);
        label_password.setHorizontalAlignment(SwingConstants.CENTER);
        logPanle.add(label_password);

        txt_password = new JTextField("");
        txt_password.setFont(font);
        logPanle.add(txt_password);

        txt_login_ip = new JTextField("127.0.0.1");
        txt_login_ip.setFont(font);
        logPanle.add(txt_login_ip);

        txt_login_port = new JTextField("6666");
        txt_login_port.setFont(font);
        logPanle.add(txt_login_port);

        logPanle.add(txt_login_ip);
        logPanle.add(txt_login_port);

        btn_submit = new JButton("登陆");
        btn_submit.setFont(font);
        btn_submit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int port;
                String message_name = txt_login_name.getText().trim();
                if (message_name == null || message_name.equals("")) {
                    JOptionPane.showMessageDialog(logPanle, "用户名不能为空！", "错误",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String message_pw = txt_password.getText().trim();
                if (message_pw == null || message_pw.equals("")) {
                    JOptionPane.showMessageDialog(logPanle, "密码不能为空！", "错误",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (isConnected) {
                    String message1 = txt_login_name.getText().trim();
                    if (message1 == null || message1.equals("")) {
                        JOptionPane.showMessageDialog(logPanle, "用户名不能为空！", "错误",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    String message2 = txt_password.getText().trim();
                    if (message2 == null || message2.equals("")) {
                        JOptionPane.showMessageDialog(logPanle, "密码不能为空！", "错误",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    sendMessage("USERLOGIN#"+message1+"#"+message2+"#"+server_port+"#"+myIP);
                    return;
                }else{
                    try {
                        try {
                            port = Integer.parseInt(txt_login_port.getText().trim());
                        } catch (NumberFormatException e2) {
                            throw new Exception("端口号不符合要求!端口为整数!");
                        }
                        String hostIp = txt_login_ip.getText().trim();
                        String name = txt_login_name.getText().trim();
                        if (name.equals("") || hostIp.equals("")) {
                            throw new Exception("姓名、服务器IP不能为空!");
                        }
                        boolean flag = connectServer(port, hostIp, name);
                        if (flag == false) {
                            throw new Exception("与服务器连接失败!");
                        }
                        frame.setTitle(name);
                        listModel.addElement("全部用户");
                    } catch (Exception exc) {
                        JOptionPane.showMessageDialog(loginframe, exc.getMessage(),
                                "错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                sendMessage("USERLOGIN#"+message_name+"#"+message_pw+"#"+server_port+"#"+myIP);
            }
        });
        logPanle.add(btn_submit);

        btn_zhuce = new JButton("注册");
        btn_zhuce.setFont(font);
        btn_zhuce.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int port;
                String message_name = txt_login_name.getText().trim();
                if (message_name == null || message_name.equals("")) {
                    JOptionPane.showMessageDialog(logPanle, "用户名不能为空！", "错误",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String message_pw = txt_password.getText().trim();
                if (message_pw == null || message_pw.equals("")) {
                    JOptionPane.showMessageDialog(logPanle, "密码不能为空！", "错误",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String message_yx = txt_login_forget.getText().trim();
                if (message_yx == null || message_yx.equals("")) {
                    JOptionPane.showMessageDialog(logPanle, "注册邮箱不能为空！", "错误",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (isConnected) {
                    String message1 = txt_login_name.getText().trim();
                    if (message1 == null || message1.equals("")) {
                        JOptionPane.showMessageDialog(logPanle, "用户名不能为空！", "错误",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    String message2 = txt_password.getText().trim();
                    if (message2 == null || message2.equals("")) {
                        JOptionPane.showMessageDialog(logPanle, "密码不能为空！", "错误",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    String message3 = txt_login_forget.getText().trim();
                    if (message3 == null || message3.equals("")) {
                        JOptionPane.showMessageDialog(logPanle, "注册邮箱不能为空！", "错误",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    sendMessage("USERZHUCE#"+message1+"#"+message2+"#"+message3);
                    return;
                }else{
                    try {
                        try {
                            port = Integer.parseInt(txt_login_port.getText().trim());
                        } catch (NumberFormatException e2) {
                            throw new Exception("端口号不符合要求!端口为整数!");
                        }
                        String hostIp = txt_login_ip.getText().trim();
                        String name = txt_login_name.getText().trim();
                        if (name.equals("") || hostIp.equals("")) {
                            throw new Exception("姓名、服务器IP不能为空!");
                        }
                        boolean flag = connectServer(port, hostIp, name);
                        if (flag == false) {
                            throw new Exception("与服务器连接失败!");
                        }
                        frame.setTitle(name);
                        listModel.addElement("全部用户");
                    } catch (Exception exc) {
                        JOptionPane.showMessageDialog(frame, exc.getMessage(),
                                "错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }


                sendMessage("USERZHUCE#"+message_name+"#"+message_pw+"#"+message_yx);
            }
        });
        logPanle.add(btn_zhuce);



        txt_login_forget = new JTextField("");
        txt_login_forget.setFont(font);
        logPanle.add(txt_login_forget);

        btn_forget_pass = new JButton("密码找回");
        btn_forget_pass.setFont(font);
        btn_forget_pass.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int port;

                String message_name = txt_login_name.getText().trim();
                if (message_name == null || message_name.equals("")) {
                    JOptionPane.showMessageDialog(logPanle, "用户名不能为空！", "错误",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String message_yx = txt_login_forget.getText().trim();
                if (message_yx == null || message_yx.equals("")) {
                    JOptionPane.showMessageDialog(logPanle, "注册邮箱不能为空！", "错误",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String message_pw = txt_password.getText().trim();
                if (message_pw == null || message_pw.equals("")) {
                    JOptionPane.showMessageDialog(logPanle, "修改密码不能为空！", "错误",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (isConnected) {
                    String message1 = txt_login_name.getText().trim();
                    if (message1 == null || message1.equals("")) {
                        JOptionPane.showMessageDialog(logPanle, "用户名不能为空！", "错误",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    String message2 = txt_login_forget.getText().trim();
                    if (message2 == null || message2.equals("")) {
                        JOptionPane.showMessageDialog(logPanle, "注册邮箱不能为空！", "错误",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    String message3 = txt_password.getText().trim();
                    if (message3 == null || message3.equals("")) {
                        JOptionPane.showMessageDialog(logPanle, "修改密码不能为空！", "错误",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    sendMessage("USERFORGET#"+message1+"#"+message2+"#"+message3);
                    return;
                }else{
                    try {
                        try {
                            port = Integer.parseInt(txt_login_port.getText().trim());
                        } catch (NumberFormatException e2) {
                            throw new Exception("端口号不符合要求!端口为整数!");
                        }
                        String hostIp = txt_login_ip.getText().trim();
                        String name = txt_login_name.getText().trim();
                        if (name.equals("") || hostIp.equals("")) {
                            throw new Exception("姓名、服务器IP不能为空!");
                        }
                        boolean flag = connectServer(port, hostIp, name);
                        
                        if (flag == false) {
                            throw new Exception("与服务器连接失败!");
                        }
                        frame.setTitle(name);
                        listModel.addElement("全部用户");
                    } catch (Exception exc) {
                        JOptionPane.showMessageDialog(frame, exc.getMessage(),
                                "错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                sendMessage("USERFORGET#"+message_name+"#"+message_yx+"#"+message_pw);
            }
        });
        logPanle.add(btn_forget_pass);


        logPanle.setBorder(new TitledBorder("登陆"));
        loginframe = new JFrame("登陆窗口");
        loginframe.add(logPanle,"Center");
        loginframe.setSize(300, 300);
        int screen_width = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screen_height = Toolkit.getDefaultToolkit().getScreenSize().height;
        loginframe.setLocation((screen_width - loginframe.getWidth()) / 2,
                (screen_height - loginframe.getHeight()) / 2);
        loginframe.setVisible(true);

        // 关闭窗口时事件
        loginframe.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (isConnected) {
                    closeConnection();// 关闭连接
                    closeServer();//关闭服务程序
                }
                System.exit(0);// 退出程序
            }
        });
    }

    // 构造方法
    public Client() throws BindException {

        serverStart(0);

        SelecTry selectIndex=new SelecTry();

        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setForeground(Color.blue);
        textField = new JTextField();
        txt_port = new JTextField("6666");
        txt_hostIp = new JTextField("127.0.0.1");
        txt_name = new JTextField("姓名");
        btn_start = new JButton("连接");
        btn_stop = new JButton("断开");
        btn_send = new JButton("发送");
        btn_send_file = new JButton("文件");
        listModel = new DefaultListModel();
        userList = new JList(listModel);
        //listModel.addElement("全部用户");
        userList.addListSelectionListener(selectIndex);
        northPanel = new JPanel();
        northPanel.setLayout(new GridLayout(1, 7));
        northPanel.add(new JLabel("     端口"));
        northPanel.add(txt_port);
        northPanel.add(new JLabel("    服务器IP"));
        northPanel.add(txt_hostIp);
        northPanel.add(new JLabel("     姓名"));
        northPanel.add(txt_name);
        northPanel.add(btn_start);
        northPanel.add(btn_stop);
        northPanel.setBorder(new TitledBorder("连接信息"));

        rightScroll = new JScrollPane(textArea);
        rightScroll.setBorder(new TitledBorder("消息显示区"));
        leftScroll = new JScrollPane(userList);
        leftScroll.setBorder(new TitledBorder("用户列表"));
        southPanel = new JPanel(new BorderLayout());
        sendPanel = new JPanel(new BorderLayout());
        southPanel.setBorder(new TitledBorder("写消息"));
        southPanel.add(textField, "Center");
        sendPanel.add(btn_send, BorderLayout.NORTH);
        sendPanel.add(btn_send_file, BorderLayout.SOUTH);
        southPanel.add(sendPanel, "East");


        centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll,
                rightScroll);
        centerSplit.setDividerLocation(150);
        
        System.out.print("客户机");
        frame = new JFrame("客户机");
        // 更改JFrame的图标：
        //frame.setIconImage(Toolkit.getDefaultToolkit().createImage(Client.class.getResource("qq.png")));
        frame.setLayout(new BorderLayout());
        frame.add(northPanel, "North");
        frame.add(centerSplit, "Center");
        frame.add(southPanel, "South");
        frame.setSize(600, 400);
        int screen_width = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screen_height = Toolkit.getDefaultToolkit().getScreenSize().height;
        frame.setLocation((screen_width - frame.getWidth()) / 2,
                (screen_height - frame.getHeight()) / 2);
        frame.setVisible(false);

        // 写消息的文本框中按回车键时事件
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
            	System.out.print("send");
                send();
            }
        });
        
        // 单击发送按钮时事件
        btn_send.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	System.out.print("send1");
                send();
            }
        });

        //单机文件按钮时事件
        btn_send_file.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
            	System.out.print("sendfile");
                sendFile();
            }
        });

        // 单击连接按钮时事件
//        btn_start.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                int port;
//                if (isConnected) {
//                    JOptionPane.showMessageDialog(frame, "已处于连接上状态，不要重复连接!",
//                            "错误", JOptionPane.ERROR_MESSAGE);
//                    return;
//                }
//                try {
//                    try {
//                        port = Integer.parseInt(txt_port.getText().trim());
//                    } catch (NumberFormatException e2) {
//                        throw new Exception("端口号不符合要求!端口为整数!");
//                    }
//                    String hostIp = txt_hostIp.getText().trim();
//                    String name = txt_name.getText().trim();
//                    if (name.equals("") || hostIp.equals("")) {
//                        throw new Exception("姓名、服务器IP不能为空!");
//                    }
//                    boolean flag = connectServer(port, hostIp, name);
//                    if (flag == false) {
//                        throw new Exception("与服务器连接失败!");
//                    }
//                    listModel.addElement("全部用户");
//                    frame.setTitle(name);
//                    JOptionPane.showMessageDialog(frame, "成功连接!");
//                } catch (Exception exc) {
//                    JOptionPane.showMessageDialog(frame, exc.getMessage(),
//                            "错误", JOptionPane.ERROR_MESSAGE);
//                }
//            }
//        });

        // 单击断开按钮时事件
        btn_stop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	System.out.print("stop");
                if (!isConnected) {
                    JOptionPane.showMessageDialog(frame, "已处于断开状态，不要重复断开!",
                            "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    boolean flag = closeConnection();// 断开连接
                    closeServer();
                    if (flag == false) {
                        throw new Exception("断开连接发生异常！");
                    }
                    JOptionPane.showMessageDialog(frame, "成功断开!");
                    frame.setVisible(false);
                    textArea.setText("");
                    loginframe.setVisible(true);
                    listModel.removeAllElements();
                } catch (Exception exc) {
                    JOptionPane.showMessageDialog(frame, exc.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // 关闭窗口时事件
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
            	System.out.print("close");
                if (isConnected) {
                    closeConnection();// 关闭连接
                    closeServer();//关闭服务程序
                }
                System.exit(0);// 退出程序
            }
        });
        System.out.print("login");
        Login();
    }

    /**
     * 连接服务器
     *
     * @param port
     * @param hostIp
     * @param name
     */
    public boolean connectServer(int port, String hostIp, String name) {
        // 连接服务器
        try {
            socket = new Socket(hostIp, port);// 根据端口号和服务器ip建立连接
            writer = new PrintWriter(socket.getOutputStream());
            reader = new BufferedReader(new InputStreamReader(socket
                    .getInputStream()));
            // 发送客户端用户基本信息(用户名和ip地址)
            sendMessage(name + "#" + socket.getLocalAddress().toString());
            // 开启接收消息的线程
            messageThread = new MessageThread(reader, textArea);
            messageThread.start();
            isConnected = true;// 已经连接上了
            return true;
        } catch (Exception e) {
            textArea.append("与端口号为：" + port + "    IP地址为：" + hostIp
                    + "   的服务器连接失败!" + "\r\n");
            isConnected = false;// 未连接上
            return false;
        }
    }

    /**
     * 发送消息
     *
     * #param message
     */
    public void sendMessage(String message) {
        writer.println(message);
        writer.flush();
    }

    /**
     * 客户端主动关闭连接
     */
    @SuppressWarnings("deprecation")
    public synchronized boolean closeConnection() {
        try {
            sendMessage("CLOSE");// 发送断开连接命令给服务器
            messageThread.stop();// 停止接受消息线程
            // 释放资源
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
            if (socket != null) {
                socket.close();
            }
            isConnected = false;
            listModel.removeAllElements();
            return true;
        } catch (IOException e1) {
            e1.printStackTrace();
            isConnected = true;
            return false;
        }
    }

    // 不断接收消息的线程
    class MessageThread extends Thread {
        private BufferedReader reader;
        private JTextArea textArea;

        // 接收消息线程的构造方法
        public MessageThread(BufferedReader reader, JTextArea textArea) {
            this.reader = reader;
            this.textArea = textArea;
        }

        // 被动的关闭连接
        public synchronized void closeCon() throws Exception {
            // 清空用户列表
            listModel.removeAllElements();
            // 被动的关闭连接释放资源
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
            if (socket != null) {
                socket.close();
            }
            isConnected = false;// 修改状态为断开

        }

        public void run() {
            String message = "";
            while (true) {
                try {
                    message = reader.readLine();
                    StringTokenizer stringTokenizer = new StringTokenizer(
                            message, "/#");
                    String command = stringTokenizer.nextToken();// 命令
                    if (command.equals("CLOSE"))// 服务器已关闭命令
                    {
                        textArea.append("服务器已关闭!\r\n");
                        closeCon();// 被动的关闭连接
                        JOptionPane.showMessageDialog(frame, "服务器已关闭！", "错误",
                                JOptionPane.ERROR_MESSAGE);
                        frame.setVisible(false);
                        textArea.setText("");
                        loginframe.setVisible(true);
                        return;// 结束线程
                    } else if (command.equals("ADD")) {// 有用户上线更新在线列表
                        String username = "";
                        String userIp = "";
                        if ((username = stringTokenizer.nextToken()) != null
                                && (userIp = stringTokenizer.nextToken()) != null) {
                            User user = new User(username, userIp);
                            onLineUsers.put(username, user);
                            listModel.addElement(username);
                        }
                    } else if (command.equals("DELETE")) {// 有用户下线更新在线列表
                        String username = stringTokenizer.nextToken();
                        User user = (User) onLineUsers.get(username);
                        onLineUsers.remove(user);
                        listModel.removeElement(username);
                    } else if (command.equals("USERLIST")) {// 加载在线用户列表
                        listModel.removeAllElements();
                        listModel.addElement("全部好友");
                        StringTokenizer strToken ;
                        String user ;// 命令
                        int size = Integer
                                .parseInt(stringTokenizer.nextToken());
                        String username = null;
                        String userIp = null;
                        for (int i = 0; i < size-1; i++) {
                            username = stringTokenizer.nextToken();
                            strToken = new StringTokenizer(username, "---()");
                            if (strToken.nextToken().equals(frame.getTitle())) {
                                continue;
                            }else{
                                listModel.addElement(username);
                            }
                            //userIp = stringTokenizer.nextToken();
                            //User user = new User(username, userIp);
                            //onLineUsers.put(username, user);
                        }
                    } else if (command.equals("MAX")) {// 人数已达上限
                        textArea.append(stringTokenizer.nextToken()
                                + stringTokenizer.nextToken() + "\r\n");
                        closeCon();// 被动的关闭连接
                        JOptionPane.showMessageDialog(frame, "服务器缓冲区已满！", "错误",
                                JOptionPane.ERROR_MESSAGE);
                        return;// 结束线程
                    } else if(command.equals("FILE")){
                        int portNumber = Integer.parseInt(stringTokenizer.nextToken());
                        String fileName = stringTokenizer.nextToken();
                        long fileSize = Long.parseLong(stringTokenizer.nextToken());
                        String ip = stringTokenizer.nextToken();
                        String Nickname = stringTokenizer.nextToken();
                        ReceiveFileThread receiveFile = new ReceiveFileThread(textArea,frame,ip, portNumber, fileName, fileSize, Nickname);
                        receiveFile.start();
                        textArea.append("从"+Nickname+"接受文件:"+fileName+",大小为:"+fileSize
                                +"ip:"+ip+"port:"+portNumber+"\r\n");
                    }else if(command.equals("USERLOGIN")){
                        String st = stringTokenizer.nextToken();
                        if(st.equals("OK")){
                            JOptionPane.showMessageDialog(loginframe, "登陆成功!" );
                            loginframe.setVisible(false);
                            txt_name.setText(txt_login_name.getText());
                            frame.setVisible(true);
                            int count = stringTokenizer.countTokens();
                            while(true){
                                if(count==0){
                                    break;
                                }
                                textArea.append(stringTokenizer.nextToken()+"  对你留言 ，");
                                textArea.append("时间： "+stringTokenizer.nextToken()+"\r\n   ");
                                textArea.append("留言内容： "+stringTokenizer.nextToken()+"\r\n");
                                count-=3;
                            }

                        }else if(st.equals("ALREADY")){
                            JOptionPane.showMessageDialog(loginframe, "账号已登陆!" );
                        }else{
                            JOptionPane.showMessageDialog(loginframe, "登陆失败!" );
                        }
                    }else if(command.equals("USERZHUCE")){
                        String st = stringTokenizer.nextToken();
                        if(st.equals("OK")){
                            JOptionPane.showMessageDialog(loginframe, "注册成功!" );

                        }else if(st.equals("exict")){
                            JOptionPane.showMessageDialog(loginframe, "用户名已存在!" );
                        }else{
                            JOptionPane.showMessageDialog(loginframe, "注册失败!" );
                        }
                    }else if(command.equals("USERFORGET")){
                        String st = stringTokenizer.nextToken();
                        if(st.equals("OK")){
                            JOptionPane.showMessageDialog(loginframe, "修改密码成功!" );

                        }else if(st.equals("YOUXIANG_WRONG")){
                            JOptionPane.showMessageDialog(loginframe, "邮箱错误!" );
                        }else if(st.equals("NAME_NO_exict")){
                            JOptionPane.showMessageDialog(loginframe, "用户不存在!" );
                        }else{
                            JOptionPane.showMessageDialog(loginframe, "找回密码失败!" );
                        }
                    } else if (command.equals("P2P")) {
                        String st = stringTokenizer.nextToken();
                        if(st.equals("OK")){
                            String username = stringTokenizer.nextToken();
                            int serverPort = Integer.parseInt(stringTokenizer.nextToken());
                            String ip = stringTokenizer.nextToken();
                            boolean cn = connectServer_p2p(serverPort,ip,username);
                            if (cn) {
                                JOptionPane.showMessageDialog(frame, "与"+username+"的连接成功，端口号为："+serverPort+"IP:"+ip );
                                P2P_printWriter.println(frame.getTitle()+"#"+myIP);
                                P2P_printWriter.flush();

                                String msg = textField.getText().trim();
                                P2P_printWriter.println(msg);
                                P2P_printWriter.flush();

                                textArea.append("对  "+username+"  说： "+msg+"\r\n");

                                textField.setText(null);
                            }

                        }else{
                            String username = stringTokenizer.nextToken();
                            JOptionPane.showMessageDialog(frame, "与"+username+"的连接失败！");
                        }
                    } else {// 普通消息
                        textArea.append(message + "\r\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
