package cn.amok147.chatroom.client;

import cn.amok147.chatroom.common.Message;
import cn.amok147.chatroom.common.MessageType;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;

/**
 * 聊天面板组件（群聊大厅 和 私聊 共用同一套 UI）
 *
 * chatTarget == null  → 群聊（大厅），消息广播给所有人
 * chatTarget != null  → 私聊，消息只发给 target
 *
 * 布局（BorderLayout）：
 *   NORTH  → 私聊时显示提示条（群聊时隐藏）
 *   CENTER → 消息显示区（JTextPane + JScrollPane）
 *   SOUTH  → 输入区（表情按钮 + JTextField + 发送按钮）
 */
public class ChatPanel extends JPanel {

    // ===== 颜色 =====
    private static final Color C_TIME   = new Color(160, 160, 160);
    private static final Color C_SELF   = new Color(30,  100, 220);
    private static final Color C_OTHER  = new Color(200,  60,  60);
    private static final Color C_TEXT   = new Color(40,   40,  40);
    private static final Color C_SYSTEM = new Color(130, 130, 130);
    private static final Color C_STATUS = new Color(0,   160,   0);
    private static final Color C_SEND   = new Color(65,  105, 225);

    // ===== 表情 =====
    private static final String[] EMOJIS = {
        "😊","😂","😍","👍","🎉","❤️","😎","🤔","😅","🙏",
        "😁","😢","😡","🤣","😏","😜","🤩","😴","🤗","👋",
        "🔥","💯","✅","🎊","💪","👏","🌹","🍀","⚡","🎯"
    };

    // ===== 字段 =====
    private final String      chatTarget;   // null=群聊；否则=私聊目标昵称
    private final ChatClient  client;
    private final String      myNickname;

    private final JTextPane      chatArea;
    private final StyledDocument doc;
    private final JTextField     inputField;

    private int      unreadCount  = 0;
    private Runnable onUnreadChange;   // 通知父组件更新标签未读数

    // ========================= 构造器 =========================

    public ChatPanel(String chatTarget, ChatClient client, String myNickname) {
        this.chatTarget  = chatTarget;
        this.client      = client;
        this.myNickname  = myNickname;

        setLayout(new BorderLayout());

        // ---- NORTH：私聊提示条 ----
        if (chatTarget != null) {
            JPanel tipBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 5));
            tipBar.setBackground(new Color(235, 245, 255));
            tipBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 220, 240)));
            JLabel tipLbl = new JLabel("🔒  与  " + chatTarget + "  的私聊  —  消息仅你们可见");
            tipLbl.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            tipLbl.setForeground(new Color(70, 130, 180));
            tipBar.add(tipLbl);
            add(tipBar, BorderLayout.NORTH);
        }

        // ---- CENTER：消息区 ----
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        chatArea.setBackground(new Color(250, 251, 252));
        chatArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        doc = chatArea.getStyledDocument();
        JScrollPane scroll = new JScrollPane(chatArea);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        // ---- SOUTH：输入区 ----
        JPanel inputPanel = new JPanel(new BorderLayout(6, 0));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(210, 210, 210)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        inputField = new JTextField();
        inputField.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        inputField.addActionListener(e -> doSend());

        JButton emojiBtn = buildIconBtn("😊", "选择表情");
        emojiBtn.addActionListener(e -> showEmojiPicker());

        JButton sendBtn = new JButton("发 送");
        sendBtn.setFont(new Font("微软雅黑", Font.BOLD, 13));
        sendBtn.setBackground(C_SEND);
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setFocusPainted(false);
        sendBtn.setBorderPainted(false);
        sendBtn.setPreferredSize(new Dimension(74, 34));
        sendBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendBtn.addActionListener(e -> doSend());
        sendBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { sendBtn.setBackground(new Color(100, 149, 237)); }
            public void mouseExited(MouseEvent e)  { sendBtn.setBackground(C_SEND); }
        });

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        rightBtns.setOpaque(false);
        rightBtns.add(emojiBtn);
        rightBtns.add(sendBtn);

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(rightBtns,  BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);
    }

    // ========================= 消息接收（供 ChatFrame 调用） =========================

    /**
     * 接收并渲染一条消息
     * 如果是私聊面板且当前不是焦点，递增未读计数并触发回调
     */
    public void receiveMessage(Message msg) {
        appendChat(msg);
        // 私聊未读计数
        if (chatTarget != null && onUnreadChange != null) {
            unreadCount++;
            onUnreadChange.run();
        }
    }

    // ========================= 追加消息 =========================

    private void appendChat(Message msg) {
        boolean isSelf = msg.getSender().equals(myNickname);
        try {
            appendStyled("\n[" + msg.getTime() + "] ", C_TIME,  11, false, false);
            appendStyled(msg.getSender() + ": ",
                    isSelf ? C_SELF : C_OTHER, 13, true,  false);
            appendStyled(msg.getContent(),
                    isSelf ? C_SELF : C_TEXT,  13, false, false);
            scrollDown();
        } catch (BadLocationException ignored) {}
    }

    public void appendStatus(String text, Color color) {
        try {
            appendStyled("\n  ► " + text, color, 12, false, true);
            scrollDown();
        } catch (BadLocationException ignored) {}
    }

    public void appendSystem(String text) {
        try {
            appendStyled("\n  [系统] " + text, C_SYSTEM, 12, false, true);
            scrollDown();
        } catch (BadLocationException ignored) {}
    }

    private void appendStyled(String text, Color color, int size,
                               boolean bold, boolean italic)
            throws BadLocationException {
        Style s = doc.addStyle(null, null);
        StyleConstants.setForeground(s,   color);
        StyleConstants.setFontSize(s,     size);
        StyleConstants.setBold(s,         bold);
        StyleConstants.setItalic(s,       italic);
        StyleConstants.setFontFamily(s,   "微软雅黑");
        doc.insertString(doc.getLength(), text, s);
    }

    private void scrollDown() {
        chatArea.setCaretPosition(doc.getLength());
    }

    // ========================= 发送消息 =========================

    private void doSend() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        Message msg;
        if (chatTarget == null) {
            msg = new Message(MessageType.TEXT, myNickname, text);
        } else {
            msg = new Message(MessageType.PRIVATE, myNickname, text, chatTarget);
        }
        client.send(msg);
        inputField.setText("");
        inputField.requestFocus();
    }

    // ========================= 表情面板 =========================

    private void showEmojiPicker() {
        Window win = SwingUtilities.getWindowAncestor(this);
        JDialog picker = win instanceof Frame
                ? new JDialog((Frame) win, "选择表情", false)
                : new JDialog((Dialog) win, "选择表情", false);
        picker.setLayout(new GridLayout(3, 10, 2, 2));
        picker.setSize(370, 112);
        picker.setLocationRelativeTo(inputField);
        for (String em : EMOJIS) {
            JButton b = buildIconBtn(em, em);
            b.addActionListener(e -> {
                inputField.setText(inputField.getText() + em);
                inputField.requestFocus();
                picker.dispose();
            });
            picker.add(b);
        }
        picker.setVisible(true);
    }

    // ========================= 工具方法 =========================

    private JButton buildIconBtn(String text, String tip) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        b.setPreferredSize(new Dimension(34, 34));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setBackground(new Color(240, 240, 240));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setToolTipText(tip);
        return b;
    }

    // ========================= Getter / 状态控制 =========================

    public String getChatTarget()   { return chatTarget; }
    public int    getUnreadCount()  { return unreadCount; }
    public void   clearUnread()     { unreadCount = 0; }
    public void   focusInput()      { inputField.requestFocus(); }
    public void   setInputEnabled(boolean e) { inputField.setEnabled(e); }
    public void   setOnUnreadChange(Runnable cb) { this.onUnreadChange = cb; }
}
