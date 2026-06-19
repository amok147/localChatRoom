package cn.amok147.chatroom.common;

/**
 * 消息类型枚举
 */
public enum MessageType {
    TEXT,       // 群聊文本消息
    PRIVATE,    // 私聊文本消息（含 target 字段）
    JOIN,       // 用户加入通知
    LEAVE,      // 用户离开通知
    USER_LIST,  // 在线用户列表更新
    SYSTEM      // 系统消息
}
