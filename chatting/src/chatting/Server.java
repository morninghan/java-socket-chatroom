package chatting;
import sun.misc.BASE64Encoder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.StringTokenizer;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class Server {

    private JFrame frame;
    private JTextArea contentArea;
    private JTextField txt_message;
    private JTextField txt_max;
    private JTextField txt_port;
    private JButton btn_start;
    private JButton btn_stop;
    private JButton btn_send;
    private JButton btn_send_file;
    private JPanel northPanel;
    private JPanel southPanel;
    private JPanel sendPanel;
    private JScrollPane rightPanel;
    private JScrollPane leftPanel;
    private JScrollPane rightPanel2;
    private JSplitPane centerSplit;
    private JSplitPane centerSplit2;
    private JList userList;
    private JList all_userList;
    private DefaultListModel listModel;
    private static DefaultListModel<String> all_listModel;

    private ServerSocket serverSocket;
    private ServerThread serverThread;
    private ArrayList<ClientThread> clients;//客户线程数组

    private boolean isStart = false;//标志服务器是否启动或关闭

    private int sendfor_who = 0;//监听左边jlist，保存给哪个用户发消息

    // 主方法,程序执行入口
    public static void main(String[] args) {

        new Server();

        try {
            Connection con = null; //定义一个MYSQL链接对象
            Class.forName("com.mysql.jdbc.Driver").newInstance();//MYSQL驱动
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/server_db?&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "root", "321321"); //链接本地MYSQL
            Statement stmt; //创建声明
            stmt = con.createStatement();
        } catch (Exception e) {
            System.out.print("MYSQL ERROR:" + e.getMessage());
        }
    }

    /**利用MD5进行加密
     * @param str  待加密的字符串
     * @return  加密后的字符串
     * @throws NoSuchAlgorithmException  没有这种产生消息摘要的算法
     * @throws UnsupportedEncodingException
     */
    public String EncoderByMd5(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        //确定计算方法
        MessageDigest md5=MessageDigest.getInstance("MD5");
        BASE64Encoder base64en = new BASE64Encoder();
        //加密后的字符串
        String newstr=base64en.encode(md5.digest(str.getBytes("utf-8")));
        return newstr;
    }

    /**判断用户密码是否正确
     * @param newpasswd  用户输入的密码
     * @param oldpasswd  数据库中存储的密码－－用户密码的摘要
     * @return
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public boolean checkpassword(String newpasswd,String oldpasswd) throws NoSuchAlgorithmException, UnsupportedEncodingException{
        if(EncoderByMd5(newpasswd).equals(oldpasswd))
            return true;
        else
            return false;
    }

    /**
     * 刚服务器突然关闭时，把所有用户状态置为离线
     */
    public void set_user_state_off() {
        try {
            Connection con = null; //定义一个MYSQL链接对象
            Class.forName("com.mysql.jdbc.Driver").newInstance();//MYSQL驱动
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/server_db?&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "root", "321321"); //链接本地MYSQL
            Statement stmt; //创建声明
            stmt = con.createStatement();

            int id = 0;
            String selectSql = "UPDATE user SET state = 0";
            stmt.executeUpdate(selectSql);
        } catch (Exception e) {
            System.out.print("MYSQL ERROR:" + e.getMessage());
        }
    }

    /**
     * 更新用户状态
     */
    public void user_name_update() {

        try {
            Connection con = null; //定义一个MYSQL链接对象
            Class.forName("com.mysql.jdbc.Driver");//MYSQL驱动
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/server_db?&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "root", "321321"); //链接本地MYSQL
            Statement stmt; //创建声明
            stmt = con.createStatement();

            all_listModel.removeAllElements();
            all_listModel.addElement("全部用户");

            String username_db;
            int state = 0;
            //查询用户名
            String selectSql = "SELECT * FROM user";
            ResultSet selectRes = stmt.executeQuery(selectSql);
            while (selectRes.next()) { //循环输出结果集
                username_db = selectRes.getString("username");
                state = selectRes.getInt("state");
                if (state == 1) {
                    all_listModel.addElement(username_db + "---(在线)");
                }

            }

            selectRes = stmt.executeQuery(selectSql);
            while (selectRes.next()) { //循环输出结果集
                username_db = selectRes.getString("username");
                state = selectRes.getInt("state");
                if (state == 0) {
                    all_listModel.addElement(username_db + "---(离线)");
                }

            }
        } catch (Exception e) {
            System.out.print("MYSQL ERROR:" + e.getMessage());
        }

    }
//    /**
//     * 更新好友状态
//     */
//    public void user_friend_update(String name) {
//
//        try {
//            Connection con = null; //定义一个MYSQL链接对象
//            Class.forName("com.mysql.jdbc.Driver");//MYSQL驱动
//            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/server_db?&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "root", "321321"); //链接本地MYSQL
//            Statement stmt; //创建声明
//            stmt = con.createStatement();
//
//            all_listModel.removeAllElements();
//            all_listModel.addElement("全部好友");
//
//            String username_db, idtype = null;
//            int state = 0;
//            int id = 0;
//            int[] d;
//            d = new int [30];
//            String selectSql = "SELECT * FROM user";
//            ResultSet selectRes = stmt.executeQuery(selectSql);
//            while (selectRes.next()) { //循环输出结果集
//                username_db = selectRes.getString("username");
//                if (username_db.equals(name)) {
//                    idtype = selectRes.getString("friend");
//                }
//            }
//            StringTokenizer stringTokenizer = new StringTokenizer(
//                    idtype, " ");
//            String i;
//            int k = 0;
//            while(stringTokenizer.hasMoreTokens())
//            {
//            	i = stringTokenizer.nextToken();
//            	System.out.print("friend:" + i +"\n");
//            	d[k] = Integer.parseInt(i);
//            	System.out.print("friend1:" + d[k]+"\n");
//            	k++;
//            }
//            int flag = 0, t = 0;
//            //查询用户名
//            selectRes = stmt.executeQuery(selectSql);
//            while (selectRes.next()) { //循环输出结果集
//                username_db = selectRes.getString("username");
//                state = selectRes.getInt("state");
//                id = selectRes.getInt("Id");
//                for(t = 0; t < k; t++)
//                {
//                	if(d[t] == id) {
//                		flag = 0;
//                		break;
//                	}
//                	else flag = 1;
//                }
//                if(flag == 1) continue;
//                if (state == 1) {
//                    all_listModel.addElement(username_db + "---(在线)");
//                }
//
//            }
//                
//            selectRes = stmt.executeQuery(selectSql);
//            while (selectRes.next()) { //循环输出结果集
//                username_db = selectRes.getString("username");
//                state = selectRes.getInt("state");
//                id = selectRes.getInt("Id");
//                for(t = 0; t < k; t++)
//                {
//                	if(d[t] == id) {
//                		flag = 0;
//                		break;
//                	}
//                	else flag = 1;
//                }
//                if(flag == 1) continue;
//                if (state == 0) {
//                    all_listModel.addElement(username_db + "---(离线)");
//                }
//
//            }
//        } catch (Exception e) {
//            System.out.print("MYSQL ERROR:" + e.getMessage());
//        }
//
//    }
    /**
     * 执行消息发送
     */
    public void send() {
        if (!isStart) {
            JOptionPane.showMessageDialog(frame, "服务器还未启动,不能发送消息！", "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
//        if (clients.size() == 0) {
//            JOptionPane.showMessageDialog(frame, "没有用户在线,不能发送消息！", "错误",
//                    JOptionPane.ERROR_MESSAGE);
//            return;
//        }
        String message = txt_message.getText().trim();
        if (message == null || message.equals("")) {
            JOptionPane.showMessageDialog(frame, "消息不能为空！", "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        sendServerMessage(message, sendfor_who);// 群发服务器消息
        //contentArea.append("对  "+listModel.getElementAt(sendfor_who)+"  说：" + txt_message.getText() + "\r\n");
        txt_message.setText(null);
    }

    // 构造放法
    public Server() {
        SelecTry selectIndex = new SelecTry();
        frame = new JFrame("服务器");
        contentArea = new JTextArea();
        contentArea.setEditable(false);
        contentArea.setForeground(Color.blue);
        txt_message = new JTextField();
        txt_max = new JTextField("30");
        txt_port = new JTextField("6666");
        btn_start = new JButton("启动");
        btn_stop = new JButton("停止");
        btn_send = new JButton("发送");
        btn_send_file = new JButton("文件");
        btn_stop.setEnabled(false);
        listModel = new DefaultListModel();
        all_listModel = new DefaultListModel();
        //listModel.addElement("全部用户");
        userList = new JList(all_listModel);//listModel
        userList.addListSelectionListener(selectIndex);
        user_name_update();//更新用户状态      

//        all_userList = new JList(all_listModel);

        southPanel = new JPanel(new BorderLayout());
        sendPanel = new JPanel(new BorderLayout());
        southPanel.setBorder(new TitledBorder("写消息"));
        southPanel.add(txt_message, "Center");
        sendPanel.add(btn_send, BorderLayout.NORTH);
        sendPanel.add(btn_send_file, BorderLayout.SOUTH);

        southPanel.add(sendPanel, "East");

        leftPanel = new JScrollPane(userList);
        leftPanel.setBorder(new TitledBorder("用户列表"));

//        rightPanel2 = new JScrollPane(all_userList);
//        rightPanel2.setBorder(new TitledBorder("全部用户"));

        rightPanel = new JScrollPane(contentArea);
        rightPanel.setBorder(new TitledBorder("消息显示区"));

        centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel,
                rightPanel);
        centerSplit.setDividerLocation(150);

//        centerSplit2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centerSplit,
//                rightPanel2);
//        centerSplit2.setDividerLocation(450);

        northPanel = new JPanel();
        northPanel.setLayout(new GridLayout(1, 6));
        northPanel.add(new JLabel("          人数上限"));
        northPanel.add(txt_max);
        northPanel.add(new JLabel("           端口"));
        northPanel.add(txt_port);
        northPanel.add(btn_start);
        northPanel.add(btn_stop);
        northPanel.setBorder(new TitledBorder("配置信息"));

        frame.setLayout(new BorderLayout());
        frame.add(northPanel, "North");
        frame.add(centerSplit, "Center");
        //frame.add(rightPanel2,BorderLayout.EAST);
        frame.add(southPanel, "South");
        frame.setSize(600, 400);
        //frame.setSize(Toolkit.getDefaultToolkit().getScreenSize());//设置全屏
        int screen_width = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screen_height = Toolkit.getDefaultToolkit().getScreenSize().height;
        frame.setLocation((screen_width - frame.getWidth()) / 2,
                (screen_height - frame.getHeight()) / 2);
        frame.setVisible(true);

        // 关闭窗口时事件
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (isStart) {
                    closeServer();// 关闭服务器
                }
                System.exit(0);// 退出程序
            }
        });

        // 文本框按回车键时事件
        txt_message.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                send();
            }
        });

        // 单击发送按钮时事件
        btn_send.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                send();
            }
        });

        //单机文件按钮时事件
        btn_send_file.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                //文件选择对话框启动，当选择了文件以后给每一个client发送文件
                JFileChooser sourceFileChooser = new JFileChooser(".");
                sourceFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                int status = sourceFileChooser.showOpenDialog(frame);
                File sourceFile = new File(sourceFileChooser.getSelectedFile().getPath());
                //服务器text area提示
                contentArea.append("发送文件：" + sourceFile.getName() + "\r\n");
                for (int i = clients.size() - 1; i >= 0; i--) {
                    SendFileThread sendFile = new SendFileThread(frame, clients.get(i).socket, "服务器", sourceFileChooser, status);
                    sendFile.start();
                    //client端提示
                    clients.get(i).getWriter().println("服务器发送一个文件：" + sourceFile.getName() + "(多人发送)");
                    clients.get(i).getWriter().flush();
                }

            }
        });

        // 单击启动服务器按钮时事件
        btn_start.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                if (isStart) {
                    JOptionPane.showMessageDialog(frame, "服务器已处于启动状态，不要重复启动！",
                            "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int max;//人数
                int port;//端口号
                try {
                    try {
                        max = Integer.parseInt(txt_max.getText());
                    } catch (Exception e1) {
                        throw new Exception("人数上限为正整数！");
                    }
                    if (max <= 0) {
                        throw new Exception("人数上限为正整数！");
                    }
                    try {
                        port = Integer.parseInt(txt_port.getText());
                    } catch (Exception e1) {
                        throw new Exception("端口号为正整数！");
                    }
                    if (port <= 0) {
                        throw new Exception("端口号 为正整数！");
                    }
                    serverStart(max, port);
                    contentArea.append("服务器已成功启动!   人数上限：" + max + ",  端口：" + port
                            + "\r\n");
                    JOptionPane.showMessageDialog(frame, "服务器成功启动!");
                    btn_start.setEnabled(false);
                    txt_max.setEnabled(false);
                    txt_port.setEnabled(false);
                    btn_stop.setEnabled(true);
                    listModel.addElement("全部用户");
                    user_name_update();
                } catch (Exception exc) {
                    JOptionPane.showMessageDialog(frame, exc.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // 单击停止服务器按钮时事件
        btn_stop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!isStart) {
                    JOptionPane.showMessageDialog(frame, "服务器还未启动，无需停止！", "错误",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    closeServer();
                    btn_start.setEnabled(true);
                    txt_max.setEnabled(true);
                    txt_port.setEnabled(true);
                    btn_stop.setEnabled(false);
                    contentArea.append("服务器成功停止!\r\n");
                    JOptionPane.showMessageDialog(frame, "服务器成功停止！");
                } catch (Exception exc) {
                    JOptionPane.showMessageDialog(frame, "停止服务器发生异常！", "错误",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    /**
     * 监听左边jlist选择的是哪一个用户
     */
    class SelecTry implements ListSelectionListener {
        int change = 0, who;

        public void valueChanged(ListSelectionEvent e) {
            //System.out.println("you selected:"+listModel.getElementAt(userList.getSelectedIndex()));
            sendfor_who = userList.getSelectedIndex();
        }

    }

    /**
     * 找回密码模块
     *
     * @param username
     * @param youxiang
     * @param new_password
     * @return
     */
    public int user_forget(String username, String youxiang, String new_password) {
        try {
            Connection con = null; //定义一个MYSQL链接对象
            Class.forName("com.mysql.jdbc.Driver").newInstance();//MYSQL驱动
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/server_db?&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "root", "321321"); //链接本地MYSQL
            Statement stmt; //创建声明
            stmt = con.createStatement();

            String codingpassword = EncoderByMd5(new_password);

            //查询数据，不能有相同的用户名
            String selectSql = "SELECT * FROM user";
            ResultSet selectRes = stmt.executeQuery(selectSql);
            while (selectRes.next()) { //循环输出结果集
                int userid = selectRes.getInt("Id");
                String username_db = selectRes.getString("username");
                String youxiang_db = selectRes.getString("youxiang");
                if (username.equals(username_db)) {
                    if (youxiang_db.equals(youxiang)) {
                        //更新一条数据
                        String updateSql = "UPDATE user SET password = '" + codingpassword + "' WHERE Id = " + userid + "";
                        long updateRes = stmt.executeUpdate(updateSql);
                        return 1;
                    }

                }
            }
            return 0;
        } catch (Exception e) {
            System.out.print("MYSQL ERROR:" + e.getMessage());
        }
        return 0;
    }


    /**
     * 注册模块
     *
     * @param username
     * @param password
     * @param youxiang
     * @return
     */
    public int user_register(String username, String password, String youxiang) {

        try {
            Connection con = null; //定义一个MYSQL链接对象
            Class.forName("com.mysql.jdbc.Driver").newInstance();//MYSQL驱动
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/server_db?&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "root", "321321"); //链接本地MYSQL
            Statement stmt; //创建声明
            stmt = con.createStatement();

            String codingPassword = EncoderByMd5(password);

            //查询数据，不能有相同的用户名
            String selectSql = "SELECT * FROM user";
            ResultSet selectRes = stmt.executeQuery(selectSql);
            int id;
            selectRes.last();
            id = selectRes.getRow() + 1;
            while (selectRes.next()) { //循环输出结果集
                String username_db = selectRes.getString("username");
                if (username.equals(username_db)) {
                    return 2;
                }
            }
            String friend = null;
            //新增一条数据
            stmt.executeUpdate("INSERT INTO user (id, username, password,youxiang,friend) VALUES ('" + id + "', '" + username + "', '" + codingPassword + "','" + youxiang + "','" + friend + "')");
            all_listModel.addElement(username);
            return 1;
        } catch (Exception e) {
            System.out.print("MYSQL ERROR:" + e.getMessage());
        }
        return 0;
    }

    /**
     * 当有用户下线时，在服务器改变状态
     *
     * @param name
     * @return
     */
    public int user_offLine(String name) {
        try {
            Connection con = null; //定义一个MYSQL链接对象
            Class.forName("com.mysql.jdbc.Driver").newInstance();//MYSQL驱动
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/server_db?&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "root", "321321"); //链接本地MYSQL
            Statement stmt; //创建声明
            stmt = con.createStatement();

            //
            String username_fromDb;
            int id = 0;
            String selectSql = "SELECT * FROM user";
            ResultSet selectRes = stmt.executeQuery(selectSql);
            while (selectRes.next()) { //循环输出结果集
                username_fromDb = selectRes.getString("username");
                id = selectRes.getInt("Id");
                if (name.equals(username_fromDb)) {
                    selectSql = "UPDATE user SET state = 0  WHERE Id = " + id + "";
                    stmt.executeUpdate(selectSql);
                    selectSql = "UPDATE user SET serverPort = 0  WHERE Id = " + id + "";
                    stmt.executeUpdate(selectSql);
                    selectSql = "UPDATE user SET ipAdres = ''  WHERE Id = " + id + "";
                    stmt.executeUpdate(selectSql);
                    return 1;
                }
            }
        } catch (Exception e) {
            System.out.print("MYSQL ERROR:" + e.getMessage());
        }
        return 0;
    }

    /**
     * 登陆模块
     *
     * @param username
     * @param password
     * @return
     */
    public int user_login(String username, String password,int serverPort,String myIP) {
        try {
            Connection con = null; //定义一个MYSQL链接对象
            Class.forName("com.mysql.jdbc.Driver").newInstance();//MYSQL驱动
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/server_db?&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "root", "321321"); //链接本地MYSQL
            Statement stmt; //创建声明
            stmt = con.createStatement();
            String username_fromDb;
            String password_fromDb;
            String codingPassword;
            int state = 0, id = 0;
            String selectSql = "SELECT * FROM user";
            ResultSet selectRes = stmt.executeQuery(selectSql);
            while (selectRes.next()) { //循环输出结果集
                username_fromDb = selectRes.getString("username");
                password_fromDb = selectRes.getString("password");
                codingPassword = EncoderByMd5(password);
                id = selectRes.getInt("Id");
                state = selectRes.getInt("state");
                if (username.equals(username_fromDb) && codingPassword.equals(password_fromDb)) {
                    if (state == 0) {
                        selectSql = "UPDATE user SET state = 1  WHERE Id = " + id + "";
                        stmt.executeUpdate(selectSql);
                        selectSql = "UPDATE user SET serverPort = " + serverPort + "  WHERE Id = " + id + "";
                        stmt.executeUpdate(selectSql);
                        selectSql = "UPDATE user SET ipAdres = '" + myIP + "'  WHERE Id = " + id + "";
                        stmt.executeUpdate(selectSql);
                        return 1;//还没有登陆，可以登陆
                    } else {
                        return 2;//已登陆状态，无法登陆
                    }

                }
            }
        } catch (Exception e) {
            System.out.print("MYSQL ERROR:" + e.getMessage());
        }
        return 0;
    }

    /**
     * 启动服务器
     *
     * @param max
     * @param port
     * @throws java.net.BindException
     */
    public void serverStart(int max, int port) throws java.net.BindException {
        try {
            clients = new ArrayList<ClientThread>();
            serverSocket = new ServerSocket(port);
            serverThread = new ServerThread(serverSocket, max);
            serverThread.start();
            isStart = true;
        } catch (BindException e) {
            isStart = false;
            throw new BindException("端口号已被占用，请换一个！");
        } catch (Exception e1) {
            e1.printStackTrace();
            isStart = false;
            throw new BindException("启动服务器异常！");
        }
    }

    /**
     * 关闭服务器
     */
    @SuppressWarnings("deprecation")
    public void closeServer() {
        try {
            if (serverThread != null)
                serverThread.stop();// 停止服务器线程

            for (int i = clients.size() - 1; i >= 0; i--) {
                // 给所有在线用户发送关闭命令
                clients.get(i).getWriter().println("CLOSE");
                clients.get(i).getWriter().flush();
                // 释放资源
                clients.get(i).stop();// 停止此条为客户端服务的线程
                clients.get(i).reader.close();
                clients.get(i).writer.close();
                clients.get(i).socket.close();
                clients.remove(i);
            }
            if (serverSocket != null) {
                serverSocket.close();// 关闭服务器端连接
            }
            listModel.removeAllElements();// 清空用户列表
            isStart = false;
            set_user_state_off();
            user_name_update();
        } catch (IOException e) {
            e.printStackTrace();
            isStart = true;
        }
    }

    /**
     * 群发服务器消息
     *
     * @param message
     * @param who
     */
    public void sendServerMessage(String message, int who) {
        if (who == 0) {
            StringTokenizer stringTokenizer;
            int flag = 0;
            for (int i = all_listModel.size(); i > 0; i--) {
                flag = 0;
                String msg = all_listModel.getElementAt(i - 1) + "";
                stringTokenizer = new StringTokenizer(
                        msg, "---");
                String user_name = stringTokenizer.nextToken();
                for (int j = clients.size() - 1; j >= 0; j--) {
                    if (user_name.equals(clients.get(j).getUser().getName())) {
                        clients.get(j).getWriter().println("服务器对你说   " + message);
                        clients.get(j).getWriter().flush();
                        flag = 1;//该用户在线状态，已发出去
                        break;
                    }
                }
                if (flag == 0) {
                    //用户离线状态，则留言
                    send_messageTo_board("服务器", user_name, message);
                }
            }
            contentArea.append("对  全部用户   发送：" + message + "\r\n");
        } else {
            int flag = 0;
            String msg = "" + all_listModel.getElementAt(who);
            StringTokenizer stringTokenizer = new StringTokenizer(
                    msg, "---");
            String user_name = stringTokenizer.nextToken();
            for (int i = clients.size() - 1; i >= 0; i--) {
                if (user_name.equals(clients.get(i).getUser().getName())) {
                    clients.get(i).getWriter().println("服务器对你说   " + message);
                    clients.get(i).getWriter().flush();
                    flag = 1;//该用户在线状态，已发出去
                    break;
                }
            }
            if (flag == 0) {
//                JOptionPane.showMessageDialog(frame, "该用户不在线，已存为留言板！", "错误",
//                        JOptionPane.ERROR_MESSAGE);
                send_messageTo_board("服务器", user_name, message);
                contentArea.append("对  " + user_name + "  留言：" + message + "\r\n");
            } else {
                contentArea.append("对  " + user_name + "  说：" + message + "\r\n");
            }
        }

    }

    /**
     * 用户不在线时，离线消息保存到服务器里面
     * @param send_from
     * @param send_for
     * @param message
     * @return
     */
    public int send_messageTo_board(String send_from, String send_for, String message) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        String msg = send_from + "#" + df.format(new Date()) + "#" + message + "#";
        try {
            Connection con = null; //定义一个MYSQL链接对象
            Class.forName("com.mysql.jdbc.Driver").newInstance();//MYSQL驱动
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/server_db?&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "root", "321321"); //链接本地MYSQL
            Statement stmt; //创建声明
            stmt = con.createStatement();

            //查询数据，不能有相同的用户名
            String selectSql = "SELECT * FROM user";
            ResultSet selectRes = stmt.executeQuery(selectSql);
            while (selectRes.next()) { //循环输出结果集
                int Id = selectRes.getInt("Id");
                String username_db = selectRes.getString("username");
                if (send_for.equals(username_db)) {
                    String old_message = selectRes.getString("message");
                    String updateSql = "UPDATE user SET message = '" + old_message + msg + "' WHERE Id = " + Id + "";
                    stmt.executeUpdate(updateSql);
                    return 1;
                }
            }
            return 0;
        } catch (Exception e) {
            System.out.print("MYSQL ERROR:" + e.getMessage());
        }
        return 0;
    }

    /**
     * 服务器线程
     */
    class ServerThread extends Thread {
        private ServerSocket serverSocket;
        private int max;// 人数上限

        // 服务器线程的构造方法
        public ServerThread(ServerSocket serverSocket, int max) {
            this.serverSocket = serverSocket;
            this.max = max;
        }

        public void run() {
            while (true) {// 不停的等待客户端的链接
                try {
                    Socket socket = serverSocket.accept();
                    if (clients.size() == max) {// 如果已达人数上限
                        BufferedReader r = new BufferedReader(
                                new InputStreamReader(socket.getInputStream()));
                        PrintWriter w = new PrintWriter(socket
                                .getOutputStream());
                        // 接收客户端的基本用户信息
                        String inf = r.readLine();
                        StringTokenizer st = new StringTokenizer(inf, "#");
                        User user = new User(st.nextToken(), st.nextToken());
                        // 反馈连接成功信息
                        w.println("MAX#服务器：对不起，" + user.getName()
                                + user.getIp() + "，服务器在线人数已达上限，请稍后尝试连接！");
                        w.flush();
                        // 释放资源
                        r.close();
                        w.close();
                        socket.close();
                        continue;
                    }
                    ClientThread client = new ClientThread(socket);
                    client.start();// 开启对此客户端服务的线程
                    client.getUser().setState(1);//在线状态
                    clients.add(client);
                    listModel.addElement(client.getUser().getName());// 更新在线列表
                    contentArea.append(client.getUser().getName()
                            + client.getUser().getIp() + "上线!\r\n");
//                    user_name_update();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 为一个客户端服务的线程
     */
    class ClientThread extends Thread {
        private Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        private User user;

        public BufferedReader getReader() {
            return reader;
        }

        public PrintWriter getWriter() {
            return writer;
        }

        public User getUser() {
            return user;
        }

        // 客户端线程的构造方法
        public ClientThread(Socket socket) {
            try {
                this.socket = socket;
                reader = new BufferedReader(new InputStreamReader(socket
                        .getInputStream()));
                writer = new PrintWriter(socket.getOutputStream());
                // 接收客户端的基本用户信息
                String inf = reader.readLine();
                StringTokenizer st = new StringTokenizer(inf, "#");
                user = new User(st.nextToken(), st.nextToken());
                // 反馈连接成功信息
                writer.println(user.getName() + user.getIp() + "与服务器连接成功!");
                writer.flush();


            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @SuppressWarnings("deprecation")
        public void run() {// 不断接收客户端的消息，进行处理。
            String message = null;
            while (true) {
                try {
                    message = reader.readLine();// 接收客户端消息
                    StringTokenizer stringTokenizer = new StringTokenizer(
                            message, "#");
                    String command = stringTokenizer.nextToken();// 命令
                    if (command.equals("CLOSE"))// 下线命令
                    {
                        contentArea.append(this.getUser().getName()
                                + this.getUser().getIp() + "下线!\r\n");
                        // 断开连接释放资源
                        user_offLine(this.getUser().getName());
                        this.getUser().setState(0);
                        reader.close();
                        writer.close();
                        socket.close();

                        user_name_update();//更新用户状态


                        //反馈用户状态
                        String liststr = "";
                        for (int j = 1; j < all_listModel.size(); j++) {
                            liststr += all_listModel.get(j) + "#";
                        }
                        // 向所有在线用户发送该用户上线命令
                        for (int j = clients.size()-1 ; j >= 0; j--) {
                            clients.get(j).getWriter().println(
                                    "USERLIST#" + all_listModel.size() + "#" + liststr);
                            clients.get(j).getWriter().flush();
                        }
                        user_name_update();//更新用户状态

                        listModel.removeElement(user.getName());// 更新在线列表

                        // 删除此条客户端服务线程
                        for (int i = clients.size() - 1; i >= 0; i--) {
                            if (clients.get(i).getUser() == user) {
                                ClientThread temp = clients.get(i);
                                clients.remove(i);// 删除此用户的服务线程
                                temp.stop();// 停止这条服务线程
                                return;
                            }
                        }
                    } else if (command.equals("USERLOGIN")) {
                        String username = stringTokenizer.nextToken();
                        String password = stringTokenizer.nextToken();
                        int serverPort = Integer.parseInt(stringTokenizer.nextToken());
                        String myIP = stringTokenizer.nextToken();
                        int i = user_login(username, password,serverPort,myIP);
                        if (1 == i) {
                            user_name_update();
                            String msg = get_message(username);
                            if (msg == null) {
                                writer.println("USERLOGIN#OK#");
                                writer.flush();
                            } else {
                                writer.println("USERLOGIN#OK#" + msg);
                                writer.flush();
                            }


                            //反馈用户状态
                            String temp = "";
                            for (int j = 1; j < all_listModel.size(); j++) {
                                temp += all_listModel.get(j) + "#";
                            }
                            // 向所有在线用户发送该用户上线命令
                            for (int j = clients.size()-1 ; j >= 0; j--) {
                                clients.get(j).getWriter().println(
                                        "USERLIST#" + all_listModel.size() + "#" + temp);
                                clients.get(j).getWriter().flush();
                            }

                        } else if (2 == i) {
                            writer.println("USERLOGIN#ALREADY");
                            writer.flush();
                        } else {
                            writer.println("USERLOGIN#NO");
                            writer.flush();

                        }
                        user_name_update();
                    } else if (command.equals("USERZHUCE")) {
                        String username = stringTokenizer.nextToken();
                        String password = stringTokenizer.nextToken();
                        String youxiang = stringTokenizer.nextToken();
                        int i = user_register(username, password, youxiang);
                        if (1 == i) {
                            writer.println("USERZHUCE#OK");
                            writer.flush();
                            contentArea.append("有新用户注册！     用户名：" + username + "\r\n");
                            user_name_update();//更新用户状态
                        } else if (i == 2) {
                            writer.println("USERZHUCE#exict");
                            writer.flush();
                        } else {
                            writer.println("USERZHUCE#NO");
                            writer.flush();
                        }
                    } else if (command.equals("USERFORGET")) {
                        String username = stringTokenizer.nextToken();
                        String youxiang = stringTokenizer.nextToken();
                        String new_password = stringTokenizer.nextToken();
                        int i = user_forget(username, youxiang, new_password);
                        if (1 == i) {
                            //JOptionPane.showMessageDialog(frame, "登陆成功!" );
                            writer.println("USERFORGET#OK");
                            writer.flush();
                            contentArea.append("   用户：" + username + "  修改密码！\r\n");
                        } else if (i == 2) {
                            writer.println("USERFORGET#YOUXIANG_WRONG");
                            writer.flush();
                        } else if (i == 3) {
                            writer.println("USERFORGET#NAME_NO_exict");
                            writer.flush();
                        } else {
                            writer.println("USERFORGET#NO");
                            writer.flush();
                        }
                    } else if (command.equals("P2P")) {
                        String username = stringTokenizer.nextToken();
                        int i = get_user_serverPort(username);
                        String ip = get_user_serverIP(username);
                        if(i!=0){
                            writer.println("P2P#OK#"+username+"#"+i+"#"+ip);
                            writer.flush();
                        }else{
                            writer.println("P2P#NO#"+username);
                            writer.flush();
                        }
                    }else if(command.equals("LIXIAN")){
                        String username_sent = stringTokenizer.nextToken();
                        String username_receive = stringTokenizer.nextToken();
                        String msg = stringTokenizer.nextToken();
                        send_messageTo_board(username_sent,username_receive,msg);
                        System.out.println("离线发送ok");
                    } else {
                        dispatcherMessage(message);// 转发消息
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public int get_user_serverPort(String user_name){
            try {
                Connection con = null; //定义一个MYSQL链接对象
                Class.forName("com.mysql.jdbc.Driver").newInstance();//MYSQL驱动
                con = DriverManager.getConnection("jdbc:mysql://localhost:3306/server_db?&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "root", "321321"); //链接本地MYSQL
                Statement stmt; //创建声明
                stmt = con.createStatement();

                //查询数据，不能有相同的用户名
                String selectSql = "SELECT * FROM user";
                ResultSet selectRes = stmt.executeQuery(selectSql);
                while (selectRes.next()) { //循环输出结果集
                    String username_db = selectRes.getString("username");
                    if (user_name.equals(username_db)) {
                        int serverPort = selectRes.getInt("serverPort");
                        return serverPort;
                    }
                }
            } catch (Exception e) {
                System.out.print("MYSQL ERROR:" + e.getMessage());
            }
            return 0;
        }

        public String get_user_serverIP(String user_name){
            try {
                Connection con = null; //定义一个MYSQL链接对象
                Class.forName("com.mysql.jdbc.Driver").newInstance();//MYSQL驱动
                con = DriverManager.getConnection("jdbc:mysql://localhost:3306/server_db?&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "root", "321321"); //链接本地MYSQL
                Statement stmt; //创建声明
                stmt = con.createStatement();

                //查询数据，不能有相同的用户名
                String selectSql = "SELECT * FROM user";
                ResultSet selectRes = stmt.executeQuery(selectSql);
                while (selectRes.next()) { //循环输出结果集
                    String username_db = selectRes.getString("username");
                    if (user_name.equals(username_db)) {
                        String serverIP = selectRes.getString("ipAdres");
                        return serverIP;
                    }
                }
            } catch (Exception e) {
                System.out.print("MYSQL ERROR:" + e.getMessage());
            }
            return "";
        }

        public String get_message(String name) {
            try {
                Connection con = null; //定义一个MYSQL链接对象
                Class.forName("com.mysql.jdbc.Driver").newInstance();//MYSQL驱动
                con = DriverManager.getConnection("jdbc:mysql://localhost:3306/server_db?&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "root", "321321"); //链接本地MYSQL
                Statement stmt; //创建声明
                stmt = con.createStatement();

                //查询数据，不能有相同的用户名
                String selectSql = "SELECT * FROM user";
                ResultSet selectRes = stmt.executeQuery(selectSql);
                while (selectRes.next()) { //循环输出结果集
                    int Id = selectRes.getInt("Id");
                    String username_db = selectRes.getString("username");
                    if (name.equals(username_db)) {
                        String message = selectRes.getString("message");
                        String updateSql = "UPDATE user SET message = '' WHERE Id = " + Id + "";
                        stmt.executeUpdate(updateSql);
                        return message;
                    }
                }
                return "";
            } catch (Exception e) {
                System.out.print("MYSQL ERROR:" + e.getMessage());
            }
            return "";
        }

        // 转发消息
        public void dispatcherMessage(String message) {
            StringTokenizer stringTokenizer = new StringTokenizer(message, "#");
            String source = stringTokenizer.nextToken();
            String owner = stringTokenizer.nextToken();
            String content = stringTokenizer.nextToken();

            if (owner.equals("ALL")) {// 群发
                message = source + "说：" + content;
                contentArea.append(message + "\r\n");
                for (int i = clients.size() - 1; i >= 0; i--) {
                    clients.get(i).getWriter().println(message + "(多人发送)");
                    clients.get(i).getWriter().flush();
                }
            } else {
                for (int i = clients.size() - 1; i >= 0; i--) {
                    if (clients.get(i).user.getName().equals(owner)) {
                        clients.get(i).getWriter().println(owner + "  对你说: " + content);
                        clients.get(i).getWriter().flush();
                        //contentArea.append(owner+"  对    "+ clients.get(i).user.getName()+ "  说  :"+ content+"\r\n");
                    }
                    if (clients.get(i).user.getName().equals(source)) {
                        clients.get(i).getWriter().println("对   " + source + "  说: " + content);
                        clients.get(i).getWriter().flush();
                    }
                }
            }
        }
    }
}