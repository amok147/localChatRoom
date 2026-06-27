package cn.amok147.chatroom.client;

import cn.amok147.chatroom.common.Message;
import cn.amok147.chatroom.common.MessageType;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;


/**
 * 客户端主程序
 * main() → 弹出登录对话框 → 连接服务器 → 启动 GUI + 接收线程
 */
public class ChatClient {
    //核心成员变量
    private Socket socket;            //网络通道
    private ObjectOutputStream out;   //发送口
    private ObjectInputStream in;     //接收口
    private String nickname;          //当前用户名
    private ChatFrame frame;          //主窗口的引用

    //程序入口

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginDialog dialog = new LoginDialog(null);//弹出登录框
            dialog.setVisible(true);
            if (!dialog.isConfirmed()) return;  // 用户点了取消，直接关闭窗口

            ChatClient client = new ChatClient();//创建客户端窗口
            client.connect(dialog.getHost(), dialog.getPort(), dialog.getNickname());
        });
    }

    //连接逻辑

    public void connect(String host, int port, String nick) {
        this.nickname = nick;
        try {
            socket = new Socket(host, port);
            //发起TCP三次握手建立联系通道（呼叫、应答、确认）
            //建立对象流
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();//必须要刷新，不然会和服务器互相等待导致死锁
            //服务器等待接收，如果不是先out在in的话，服务器和客户端会相互等待
            //flush()强制先发（呼叫），解决可能死锁的问题
            in  = new ObjectInputStream(socket.getInputStream());

            //发送登录消息（上线提示）
            send(new Message(MessageType.JOIN, nickname, nickname + " 加入了聊天室"));

            //启动GUI窗口
            frame = new ChatFrame(this, nickname);
            //把自身(this)传递给窗口，窗口就可以调用send方法了
            frame.setVisible(true);

            //后台接收线程
            Thread receiver = new Thread(this::receiveLoop, "receiver");
            receiver.setDaemon(true);
            receiver.start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                    "连接服务器失败：\n" + e.getMessage(),
                    "连接错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    //收发消息

    /** 发送消息（供 ChatFrame 调用） */
    public void send(Message msg) {
        try {
            out.writeObject(msg);//把Message对象变成二进制发送
            out.flush();         //强制把缓冲区数据推上网络
            out.reset();         //清空对象缓存，防止内存溢出。
            /*连续发送大量消息会造成内存泄漏，每次都释放缓存，可以保证每次发送的都是新的数据*/
        } catch (IOException e) {
            //如果断网了，显示错误
            if (frame != null) {
                SwingUtilities.invokeLater(() ->
                        frame.appendSystemMessage("发送失败：" + e.getMessage()));
            }
        }
    }

    /** 后台接收循环 */
    private void receiveLoop() {
        try {
            while (true) { //不报错就一直接听
                //阻塞监听，等待服务器发送包裹
                Message msg = (Message) in.readObject();
                //把包裹给UI去处理
                SwingUtilities.invokeLater(() -> frame.handleMessage(msg));
            }
        } catch (EOFException | SocketException e) {
            //网络断开，或者服务器断开
            SwingUtilities.invokeLater(() -> {
                if (frame != null) {
                    frame.appendSystemMessage("已断开与服务器的连接。");
                    frame.setConnected(false);
                    //禁用输入框
                }
            });
        } catch (Exception e) {
            //发生其他未知网络错误
            SwingUtilities.invokeLater(() -> {
                if (frame != null)
                    frame.appendSystemMessage("网络错误：" + e.getMessage());
            });
        }
    }
    /** 主动断开（窗口关闭时调用） */
    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                //断开TCP连接
            }
        } catch (IOException ignored) {}//关闭出异常忽略
    }

    public String getNickname() { return nickname; }
}
