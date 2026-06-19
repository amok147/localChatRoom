package cn.amok147.chatroom.server;

import cn.amok147.chatroom.common.Message;
import cn.amok147.chatroom.common.MessageType;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 聊天室服务器主程序
 * 运行后监听 8888 端口，支持多客户端同时连接
 * v2：新增私聊路由能力（nicknameMap + routePrivate）
 */
public class ChatServer {

    private static final int PORT = 8888;

    /** 线程安全的客户端列表（用于广播） */
    static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    /** 昵称 → 处理器 的映射（用于私聊精确路由） */
    static final Map<String, ClientHandler> nicknameMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║     本地聊天室 服务器 v2          ║");
        System.out.println("╚══════════════════════════════════╝");
        System.out.println("监听端口：" + PORT + " ...");
        printLocalIPs();

        Runtime.getRuntime().addShutdownHook(new Thread(() ->
                broadcast(new Message(MessageType.SYSTEM, "Server", "服务器已关闭，连接断开。"), null)
        ));

        try (ServerSocket server = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = server.accept();
                System.out.println("[新连接] " + socket.getInetAddress().getHostAddress());
                ClientHandler handler = new ClientHandler(socket);
                new Thread(handler, "client-" + socket.getPort()).start();
            }
        } catch (IOException e) {
            System.err.println("服务器错误：" + e.getMessage());
        }
    }

    /** 用户成功登录后调用 */
    static synchronized void onClientJoin(ClientHandler handler) {
        clients.add(handler);
        nicknameMap.put(handler.getNickname(), handler);

        broadcast(new Message(MessageType.JOIN, "Server",
                handler.getNickname() + " 加入了聊天室"), handler);
        handler.send(new Message(MessageType.SYSTEM, "Server",
                "欢迎进入聊天室，" + handler.getNickname() + "！当前在线 " + clients.size() + " 人。"));
        broadcastUserList();
        System.out.println("[加入] " + handler.getNickname() + "，当前在线：" + clients.size());
    }

    /** 用户断线后调用 */
    static synchronized void onClientLeave(ClientHandler handler) {
        clients.remove(handler);
        nicknameMap.remove(handler.getNickname());

        broadcast(new Message(MessageType.LEAVE, "Server",
                handler.getNickname() + " 离开了聊天室"), null);
        broadcastUserList();
        System.out.println("[离开] " + handler.getNickname() + "，当前在线：" + clients.size());
    }

    /** 广播给除 exclude 之外的所有人 */
    static void broadcast(Message msg, ClientHandler exclude) {
        for (ClientHandler c : clients) {
            if (c != exclude) c.send(msg);
        }
    }

    /** 广播给所有人（含发送者） */
    static void broadcastAll(Message msg) {
        for (ClientHandler c : clients) {
            c.send(msg);
        }
    }

    /**
     * 私聊路由：将消息发送给目标用户，同时回显给发送者自己
     * 这样发送方可以在自己的私聊窗口看到发出去的消息
     */
    static void routePrivate(Message msg) {
        String targetNick = msg.getTarget();
        String senderNick = msg.getSender();

        ClientHandler targetHandler = nicknameMap.get(targetNick);
        ClientHandler senderHandler = nicknameMap.get(senderNick);

        if (targetHandler == null) {
            // 目标不在线，通知发送者
            if (senderHandler != null) {
                senderHandler.send(new Message(MessageType.SYSTEM, "Server",
                        "私聊失败：用户 " + targetNick + " 不在线。"));
            }
            return;
        }

        // 发给对方
        targetHandler.send(msg);

        // 回显给自己（发送者也能在私聊面板看到自己的消息）
        if (senderHandler != null && senderHandler != targetHandler) {
            senderHandler.send(msg);
        }

        System.out.println("[私聊] " + senderNick + " → " + targetNick + ": " + msg.getContent());
    }

    /** 推送最新在线用户列表给所有人 */
    static void broadcastUserList() {
        StringBuilder sb = new StringBuilder();
        for (ClientHandler c : clients) {
            if (sb.length() > 0) sb.append(",");
            sb.append(c.getNickname());
        }
        Message msg = new Message(MessageType.USER_LIST, "Server", sb.toString());
        for (ClientHandler c : clients) {
            c.send(msg);
        }
    }

    /** 打印本机所有局域网 IP，方便告诉别人连接地址 */
    private static void printLocalIPs() {
        System.out.println("──────────────────────────────────────");
        System.out.println("  局域网连接地址（选一个告诉对方）：");
        System.out.println("──────────────────────────────────────");
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            boolean found = false;
            while (nets.hasMoreElements()) {
                NetworkInterface net = nets.nextElement();
                if (net.isLoopback() || !net.isUp()) continue;
                Enumeration<InetAddress> addrs = net.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr.isLoopbackAddress()) continue;
                    String ip = addr.getHostAddress();
                    // 过滤 IPv6 和常见虚拟网卡
                    if (ip.contains(":")) continue;
                    if (ip.startsWith("127.") || ip.startsWith("169.254.")) continue;
                    System.out.println("    → " + ip + ":" + PORT);
                    found = true;
                }
            }
            if (!found) {
                System.out.println("    （未检测到局域网 IP，请检查网络）");
            }
        } catch (Exception e) {
            System.out.println("    （检测 IP 失败：" + e.getMessage() + "）");
        }
        System.out.println("──────────────────────────────────────");
    }
}
