package cn.amok147.chatroom.server;

import cn.amok147.chatroom.common.Message;
import cn.amok147.chatroom.common.MessageType;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

/**
 * 每个客户端连接对应一个 ClientHandler 线程
 * 负责：接收消息 → 广播；维护 nickname
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String nickname = "未知用户";

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // 先建 out 再建 in（双端都要如此，否则互相等待头信息会死锁）
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in  = new ObjectInputStream(socket.getInputStream());

            // 第一条消息：客户端发来的 JOIN 消息，携带昵称
            Message joinMsg = (Message) in.readObject();
            this.nickname = joinMsg.getSender();
            ChatServer.onClientJoin(this);

            // 持续接收并路由
            while (true) {
                Message msg = (Message) in.readObject();
                switch (msg.getType()) {
                    case TEXT:
                        System.out.println("[群聊][" + nickname + "] " + msg.getContent());
                        ChatServer.broadcastAll(msg);
                        break;
                    case PRIVATE:
                        ChatServer.routePrivate(msg);
                        break;
                    default:
                        break;
                }
            }

        } catch (EOFException | SocketException e) {
            // 客户端正常断开
        } catch (Exception e) {
            System.err.println("处理用户 [" + nickname + "] 异常：" + e.getMessage());
        } finally {
            ChatServer.onClientLeave(this);
            closeQuietly();
        }
    }

    /** 向该客户端发送一条消息（线程安全） */
    synchronized void send(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
            out.reset(); // 防止对象图缓存导致旧数据
        } catch (IOException e) {
            // 发送失败，连接可能已断开，忽略
        }
    }

    public String getNickname() {
        return nickname;
    }

    private void closeQuietly() {
        try { socket.close(); } catch (IOException ignored) {}
    }
}
