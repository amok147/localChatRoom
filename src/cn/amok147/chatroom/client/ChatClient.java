package cn.amok147.chatroom.client;

import cn.amok147.chatroom.common.Message;
import cn.amok147.chatroom.common.MessageType;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;

/**
 * 客户端主程序 & 网络层
 * main() → 弹出登录对话框 → 连接服务器 → 启动 GUI + 接收线程
 */
public class ChatClient {

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String nickname;
    private ChatFrame frame;

    // ========================= 程序入口 =========================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginDialog dialog = new LoginDialog(null);
            dialog.setVisible(true);
            if (!dialog.isConfirmed()) return;  // 用户点了取消

            ChatClient client = new ChatClient();
            client.connect(dialog.getHost(), dialog.getPort(), dialog.getNickname());
        });
    }

    // ========================= 连接逻辑 =========================

    public void connect(String host, int port, String nick) {
        this.nickname = nick;
        try {
            socket = new Socket(host, port);

            // 先建 out，再建 in（与服务器端顺序一致，避免死锁）
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in  = new ObjectInputStream(socket.getInputStream());

            // 发送登录消息（昵称即 sender）
            send(new Message(MessageType.JOIN, nickname, nickname + " 加入了聊天室"));

            // 启动 GUI
            frame = new ChatFrame(this, nickname);
            frame.setVisible(true);

            // 后台接收线程
            Thread receiver = new Thread(this::receiveLoop, "receiver");
            receiver.setDaemon(true);
            receiver.start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                    "连接服务器失败：\n" + e.getMessage(),
                    "连接错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ========================= 收发消息 =========================

    /** 发送消息（供 ChatFrame 调用） */
    public void send(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
            out.reset();
        } catch (IOException e) {
            if (frame != null) {
                SwingUtilities.invokeLater(() ->
                        frame.appendSystemMessage("发送失败：" + e.getMessage()));
            }
        }
    }

    /** 后台接收循环 */
    private void receiveLoop() {
        try {
            while (true) {
                Message msg = (Message) in.readObject();
                SwingUtilities.invokeLater(() -> frame.handleMessage(msg));
            }
        } catch (EOFException | SocketException e) {
            SwingUtilities.invokeLater(() -> {
                if (frame != null) {
                    frame.appendSystemMessage("已断开与服务器的连接。");
                    frame.setConnected(false);
                }
            });
        } catch (Exception e) {
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
            }
        } catch (IOException ignored) {}
    }

    public String getNickname() { return nickname; }
}
