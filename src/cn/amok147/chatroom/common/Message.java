package cn.amok147.chatroom.common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 聊天消息实体类（通过 ObjectOutputStream 序列化传输）
 * v2：新增 target 字段，支持私聊路由
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 2L;

    private final MessageType type;    // 消息类型
    private final String sender;       // 发送者昵称
    private final String content;      // 消息内容
    private final String time;         // 发送时间（HH:mm:ss）
    private final String target;       // 私聊目标昵称；null 表示群聊/广播

    /** 群聊 / 系统消息构造器（target = null） */
    public Message(MessageType type, String sender, String content) {
        this(type, sender, content, null);
    }

    /** 私聊消息构造器 */
    public Message(MessageType type, String sender, String content, String target) {
        this.type    = type;
        this.sender  = sender;
        this.content = content;
        this.target  = target;
        this.time    = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public MessageType getType()    { return type; }
    public String getSender()       { return sender; }
    public String getContent()      { return content; }
    public String getTime()         { return time; }
    public String getTarget()       { return target; }

    @Override
    public String toString() {
        if (target != null) {
            return "[" + time + "] [私聊→" + target + "] " + sender + ": " + content;
        }
        return "[" + time + "] " + sender + ": " + content;
    }
}
