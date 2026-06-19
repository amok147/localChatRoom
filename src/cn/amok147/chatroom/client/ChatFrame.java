package cn.amok147.chatroom.client;

import cn.amok147.chatroom.common.Message;
import cn.amok147.chatroom.common.MessageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 聊天室主窗口（v2）
 *
 * 布局：
 *   NORTH  → 标题栏（渐变蓝）
 *   CENTER → JSplitPane
 *               LEFT  → JTabbedPane（大厅 + 各私聊标签，可关闭）
 *               RIGHT → 在线用户列表（双击发起私聊）
 */
public class ChatFrame extends JFrame {

    private static final Color C_HEADER = new Color(65, 105, 225);
    private static final Color C_JOIN   = new Color(0,  160,   0);
    private static final Color C_LEAVE  = new Color(200, 90,   0);

    // ===== 网络 =====
    private final ChatClient client;
    private final String     nickname;

    // ===== 核心组件 =====
    private final JTabbedPane tabbedPane;
    private final ChatPanel   hallPanel;                           // 大厅（群聊）
    private final Map<String, ChatPanel> privatePanels = new HashMap<>(); // 私聊面板

    // ===== 状态栏 =====
    private final JLabel statusLabel;
    private final JLabel onlineCount;

    // ===== 用户列表 =====
    private final DefaultListModel<String> userListModel;

    // ========================= 构造器 =========================

    public ChatFrame(ChatClient client, String nickname) {
        this.client   = client;
        this.nickname = nickname;

        setTitle("本地聊天室  —  " + nickname);
        setSize(860, 580);
        setMinimumSize(new Dimension(660, 420));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                int r = JOptionPane.showConfirmDialog(ChatFrame.this,
                        "确定退出聊天室？", "退出", JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION) {
                    client.disconnect();
                    dispose();
                    System.exit(0);
                }
            }
        });

        setLayout(new BorderLayout());

        // ===================== NORTH：标题栏 =====================
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, C_HEADER,
                        getWidth(), 0, new Color(100, 149, 237)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        header.setPreferredSize(new Dimension(0, 46));
        header.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));

        JLabel titleLbl = new JLabel("🏠  本地聊天室");
        titleLbl.setFont(new Font("微软雅黑", Font.BOLD, 17));
        titleLbl.setForeground(Color.WHITE);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        statusPanel.setOpaque(false);
        onlineCount = new JLabel("在线 1 人");
        onlineCount.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        onlineCount.setForeground(new Color(220, 220, 220));
        statusLabel = new JLabel("● 已连接");
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(144, 238, 144));
        statusPanel.add(onlineCount);
        statusPanel.add(statusLabel);
        header.add(titleLbl,    BorderLayout.WEST);
        header.add(statusPanel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ===================== CENTER：分割面板 =====================
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(645);
        split.setDividerSize(2);
        split.setResizeWeight(1.0);

        // ---- 左：JTabbedPane ----
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFont(new Font("微软雅黑", Font.PLAIN, 12));

        // 大厅面板（始终存在，不可关闭）
        hallPanel = new ChatPanel(null, client, nickname);
        tabbedPane.addTab("🏠 大厅", hallPanel);

        // 切换标签时：清除该标签未读计数，更新标签头
        tabbedPane.addChangeListener(e -> {
            int idx = tabbedPane.getSelectedIndex();
            if (idx < 0) return;
            Component comp = tabbedPane.getComponentAt(idx);
            if (comp instanceof ChatPanel) {
                ChatPanel p = (ChatPanel) comp;
                p.clearUnread();
                refreshTabTitle(p, idx);
                p.focusInput();
            }
        });

        split.setLeftComponent(tabbedPane);

        // ---- 右：在线用户列表 ----
        JPanel userPanel = new JPanel(new BorderLayout());
        userPanel.setPreferredSize(new Dimension(185, 0));
        userPanel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(220, 220, 220)));

        JLabel userLbl = new JLabel("在线用户", SwingConstants.CENTER);
        userLbl.setFont(new Font("微软雅黑", Font.BOLD, 13));
        userLbl.setForeground(C_HEADER);
        userLbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(8, 0, 8, 0)));

        userListModel = new DefaultListModel<>();
        JList<String> userList = new JList<>(userListModel);
        userList.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setCellRenderer(new UserCellRenderer(nickname));

        // 双击用户列表 → 打开私聊
        userList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) return;
                String target = userList.getSelectedValue();
                if (target == null) return;
                if (target.equals(nickname)) {
                    JOptionPane.showMessageDialog(ChatFrame.this,
                            "不能和自己私聊哦 😄", "提示", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    openPrivateChat(target);
                }
            }
        });

        JLabel tipLbl = new JLabel("双击用户可发起私聊", SwingConstants.CENTER);
        tipLbl.setFont(new Font("微软雅黑", Font.PLAIN, 10));
        tipLbl.setForeground(new Color(160, 160, 160));
        tipLbl.setBorder(BorderFactory.createEmptyBorder(3, 0, 4, 0));

        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setBorder(BorderFactory.createEmptyBorder());
        userPanel.add(userLbl,    BorderLayout.NORTH);
        userPanel.add(userScroll, BorderLayout.CENTER);
        userPanel.add(tipLbl,     BorderLayout.SOUTH);
        split.setRightComponent(userPanel);

        add(split, BorderLayout.CENTER);
    }

    // ========================= 消息路由（由 ChatClient 在 EDT 调用） =========================

    public void handleMessage(Message msg) {
        switch (msg.getType()) {
            case TEXT:
                hallPanel.receiveMessage(msg);
                markUnreadIfHidden(hallPanel, 0);
                break;
            case PRIVATE:
                handlePrivateMessage(msg);
                break;
            case JOIN:
                hallPanel.appendStatus(msg.getContent(), C_JOIN);
                break;
            case LEAVE:
                hallPanel.appendStatus(msg.getContent(), C_LEAVE);
                break;
            case USER_LIST:
                updateUserList(msg.getContent());
                break;
            case SYSTEM:
                hallPanel.appendSystem(msg.getContent());
                break;
        }
    }

    /** 处理私聊消息（收到或发出的私聊消息都会回到这里） */
    private void handlePrivateMessage(Message msg) {
        // 判断对话对方是谁（自己发出的消息 target 是对方，收到的 sender 是对方）
        String peer = msg.getSender().equals(nickname) ? msg.getTarget() : msg.getSender();

        ChatPanel panel = privatePanels.get(peer);
        if (panel == null) {
            // 对方主动发来第一条私聊：自动创建标签
            panel = createPrivatePanel(peer);
            hallPanel.appendStatus("收到来自 " + peer + " 的私信，点击上方标签查看",
                    new Color(100, 100, 200));
        }
        panel.receiveMessage(msg);

        // 不在该标签时更新未读提示
        if (tabbedPane.getSelectedComponent() != panel) {
            int idx = tabbedPane.indexOfComponent(panel);
            if (idx >= 0) refreshTabTitle(panel, idx);
        }
    }

    // ========================= 私聊面板管理 =========================

    /** 打开或切换到指定用户的私聊标签 */
    private void openPrivateChat(String target) {
        ChatPanel panel = privatePanels.get(target);
        if (panel == null) {
            panel = createPrivatePanel(target);
            hallPanel.appendStatus("已开启与 " + target + " 的私聊会话", new Color(100, 100, 200));
        }
        tabbedPane.setSelectedComponent(panel);
        panel.focusInput();
    }

    /** 创建私聊面板并添加带关闭按钮的标签头 */
    private ChatPanel createPrivatePanel(String target) {
        ChatPanel panel = new ChatPanel(target, client, nickname);
        panel.appendSystem("这是你与 " + target + " 的私聊，消息仅你们两人可见。");

        // 未读回调：在 EDT 刷新标签标题
        panel.setOnUnreadChange(() -> SwingUtilities.invokeLater(() -> {
            int idx = tabbedPane.indexOfComponent(panel);
            if (idx >= 0) refreshTabTitle(panel, idx);
        }));

        privatePanels.put(target, panel);

        int idx = tabbedPane.getTabCount();
        tabbedPane.addTab("💬 " + target, panel);

        // 安装可关闭标签头
        tabbedPane.setTabComponentAt(idx, buildClosableTabHeader("💬 " + target, panel, target));
        tabbedPane.setSelectedIndex(idx);
        return panel;
    }

    /** 构建带 × 关闭按钮的标签头组件 */
    private JPanel buildClosableTabHeader(String title, ChatPanel panel, String target) {
        JPanel tab = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        tab.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        // 存储引用，供 refreshTabTitle 使用
        tab.putClientProperty("titleLabel", titleLbl);

        JButton closeBtn = new JButton("×");
        closeBtn.setFont(new Font("Dialog", Font.BOLD, 13));
        closeBtn.setPreferredSize(new Dimension(18, 18));
        closeBtn.setFocusPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setForeground(new Color(120, 120, 120));
        closeBtn.setToolTipText("关闭私聊");
        closeBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { closeBtn.setForeground(Color.RED); }
            public void mouseExited(MouseEvent e)  { closeBtn.setForeground(new Color(120, 120, 120)); }
        });
        closeBtn.addActionListener(e -> {
            int i = tabbedPane.indexOfComponent(panel);
            if (i >= 0) tabbedPane.removeTabAt(i);
            privatePanels.remove(target);
        });

        tab.add(titleLbl);
        tab.add(closeBtn);
        return tab;
    }

    /** 刷新标签标题（显示/清除未读数字角标） */
    private void refreshTabTitle(ChatPanel panel, int idx) {
        String target   = panel.getChatTarget();
        String baseTitle = (target == null) ? "🏠 大厅" : "💬 " + target;
        int    unread    = panel.getUnreadCount();
        String full      = unread > 0 ? baseTitle + "  [" + unread + "]" : baseTitle;

        Component tabComp = tabbedPane.getTabComponentAt(idx);
        if (tabComp instanceof JPanel) {
            // 私聊标签：更新内嵌 JLabel
            JLabel lbl = (JLabel) ((JPanel) tabComp).getClientProperty("titleLabel");
            if (lbl != null) {
                lbl.setText(full);
                lbl.setForeground(unread > 0 ? new Color(220, 50, 50) : Color.BLACK);
                return;
            }
        }
        // 大厅标签（无自定义组件）：直接设置
        tabbedPane.setTitleAt(idx, full);
    }

    /** 若面板当前不是活动标签，则高亮提示（不增加 unread，只做标题闪烁） */
    private void markUnreadIfHidden(ChatPanel panel, int idx) {
        if (tabbedPane.getSelectedComponent() == panel) return;
        String cur = tabbedPane.getTitleAt(idx);
        if (cur.contains("●")) return;  // 避免重复
        tabbedPane.setTitleAt(idx, "🏠 大厅 ●");
        // 切换回大厅时自动清除
    }

    // ========================= 用户列表更新 =========================

    private void updateUserList(String csv) {
        userListModel.clear();
        if (csv != null && !csv.isEmpty()) {
            for (String u : csv.split(",")) {
                String name = u.trim();
                if (!name.isEmpty()) userListModel.addElement(name);
            }
        }
        onlineCount.setText("在线 " + userListModel.size() + " 人");
    }

    // ========================= 外部状态更新 =========================

    public void appendSystemMessage(String text) {
        hallPanel.appendSystem(text);
    }

    public void setConnected(boolean connected) {
        if (connected) {
            statusLabel.setText("● 已连接");
            statusLabel.setForeground(new Color(144, 238, 144));
        } else {
            statusLabel.setText("● 已断开");
            statusLabel.setForeground(new Color(255, 100, 100));
            hallPanel.setInputEnabled(false);
            for (ChatPanel p : privatePanels.values()) p.setInputEnabled(false);
        }
    }

    // ========================= 用户列表渲染 =========================

    private static class UserCellRenderer extends DefaultListCellRenderer {
        private static final Color C_SELF = new Color(30, 100, 220);
        private static final Color C_TEXT = new Color(40, 40,  40);
        private final String me;

        UserCellRenderer(String me) { this.me = me; }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean hasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
            String  name   = (String) value;
            boolean isSelf = name.equals(me);
            setText("  ●  " + name + (isSelf ? "  (我)" : ""));
            setFont(new Font("微软雅黑", isSelf ? Font.BOLD : Font.PLAIN, 12));
            if (!isSelected) setForeground(isSelf ? C_SELF : C_TEXT);
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            return this;
        }
    }
}
