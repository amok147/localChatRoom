package cn.amok147.chatroom.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * 登录对话框：输入服务器地址、端口、昵称
 */
public class LoginDialog extends JDialog {

    private final JTextField hostField;
    private final JTextField portField;
    private final JTextField nickField;
    private boolean confirmed = false;

    public LoginDialog(Frame parent) {
        super(parent, "加入聊天室", true);
        setSize(340, 220);
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // ---- 顶部标题 ----
        JLabel title = new JLabel("🏠  本地聊天室", SwingConstants.CENTER);
        title.setFont(new Font("微软雅黑", Font.BOLD, 18));
        title.setForeground(new Color(70, 130, 180));
        title.setBorder(BorderFactory.createEmptyBorder(15, 0, 10, 0));

        // ---- 表单 ----
        JPanel form = new JPanel(new GridLayout(3, 2, 8, 10));
        form.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));

        JLabel lHost = new JLabel("服务器地址：");
        lHost.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        hostField = new JTextField(detectLocalIP());
        hostField.setFont(new Font("微软雅黑", Font.PLAIN, 13));

        JLabel lPort = new JLabel("端口：");
        lPort.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        portField = new JTextField("8888");
        portField.setFont(new Font("微软雅黑", Font.PLAIN, 13));

        JLabel lNick = new JLabel("昵称：");
        lNick.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        nickField = new JTextField();
        nickField.setFont(new Font("微软雅黑", Font.PLAIN, 13));

        form.add(lHost);  form.add(hostField);
        form.add(lPort);  form.add(portField);
        form.add(lNick);  form.add(nickField);

        // ---- 按钮 ----
        JButton joinBtn   = new JButton("加入");
        JButton cancelBtn = new JButton("取消");
        joinBtn.setFont(new Font("微软雅黑", Font.BOLD, 13));
        joinBtn.setBackground(new Color(70, 130, 180));
        joinBtn.setForeground(Color.WHITE);
        joinBtn.setFocusPainted(false);
        joinBtn.setBorderPainted(false);
        cancelBtn.setFont(new Font("微软雅黑", Font.PLAIN, 13));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 5));
        btnPanel.add(joinBtn);
        btnPanel.add(cancelBtn);

        joinBtn.addActionListener(e -> onJoin());
        cancelBtn.addActionListener(e -> dispose());
        nickField.addActionListener(e -> onJoin());

        // ---- 布局 ----
        setLayout(new BorderLayout());
        add(title,   BorderLayout.NORTH);
        add(form,    BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        // 默认焦点
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) { nickField.requestFocus(); }
        });
    }

    private void onJoin() {
        String nick = nickField.getText().trim();
        if (nick.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入昵称！", "提示", JOptionPane.WARNING_MESSAGE);
            nickField.requestFocus();
            return;
        }
        if (nick.contains(",")) {
            JOptionPane.showMessageDialog(this, "昵称不能包含逗号！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        confirmed = true;
        dispose();
    }

    public boolean isConfirmed() { return confirmed; }
    public String  getHost()     { return hostField.getText().trim(); }
    public int     getPort()     {
        try { return Integer.parseInt(portField.getText().trim()); }
        catch (NumberFormatException e) { return 8888; }
    }
    public String  getNickname() { return nickField.getText().trim(); }

    /** 自动检测本机局域网 IP，作为默认服务器地址 */
    private String detectLocalIP() {
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            while (nets.hasMoreElements()) {
                NetworkInterface net = nets.nextElement();
                if (net.isLoopback() || !net.isUp()) continue;
                Enumeration<InetAddress> addrs = net.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    String ip = addr.getHostAddress();
                    if (ip.contains(":") || ip.startsWith("127.") || ip.startsWith("169.254.")) continue;
                    return ip;  // 返回第一个有效局域网 IP
                }
            }
        } catch (Exception ignored) {}
        return "localhost";  // 检测失败就回退到 localhost
    }
}
