# 本地聊天室（Java）

> 基于 TCP Socket + Swing GUI 的本地局域网多人聊天室
> 包名：`cn.amok147.chatroom`，端口：`8888`

---

## 项目结构

```
LocalChatRoom/
├── src/
│   └── cn/amok147/chatroom/
│       ├── common/
│       │   ├── MessageType.java   消息类型枚举（TEXT/JOIN/LEAVE/USER_LIST/SYSTEM）
│       │   └── Message.java       可序列化消息实体（类型、发送者、内容、时间）
│       ├── server/
│       │   ├── ChatServer.java    服务器主程序（监听 8888，管理所有连接）
│       │   └── ClientHandler.java 每个客户端对应一个处理线程
│       └── client/
│           ├── ChatClient.java    客户端主程序 + 网络层（main 入口）
│           ├── LoginDialog.java   登录对话框（地址/端口/昵称）
│           └── ChatFrame.java     Swing 主窗口（消息区 + 用户列表 + 输入框）
├── out/                           （编译后自动生成）
├── run.bat                        一键编译/启动脚本
└── README.md
```

---

## 快速开始

### 前提
- 已安装 **JDK 8 或以上**
- 确认 `javac` 和 `java` 命令可用（`java -version`）

### 方式一：使用 run.bat（推荐）

双击 `run.bat` 或在命令行执行：

```bat
cd LocalChatRoom
run.bat
```

按提示选择：
- `1` → 仅编译
- `2` → 启动服务器（需先编译）
- `3` → 启动客户端（需先编译）
- `4` → 编译 + 启动服务器（一键）
- `5` → 编译 + 启动客户端（一键）

### 方式二：手动命令

```bat
cd LocalChatRoom

:: 编译（只需执行一次）
javac -encoding UTF-8 -d out ^
  src/cn/amok147/chatroom/common/MessageType.java ^
  src/cn/amok147/chatroom/common/Message.java ^
  src/cn/amok147/chatroom/server/ChatServer.java ^
  src/cn/amok147/chatroom/server/ClientHandler.java ^
  src/cn/amok147/chatroom/client/LoginDialog.java ^
  src/cn/amok147/chatroom/client/ChatFrame.java ^
  src/cn/amok147/chatroom/client/ChatClient.java

:: 启动服务器（先开）
java -cp out cn.amok147.chatroom.server.ChatServer

:: 启动客户端（可开多个窗口）
java -cp out cn.amok147.chatroom.client.ChatClient
```

---

## 使用流程

```
① 先启动服务器（保持命令行窗口不要关）
② 打开一个或多个客户端窗口
③ 登录对话框：填写地址（默认 localhost）、端口（默认 8888）、昵称
④ 开始聊天！
```

- 同一台电脑：地址填 `localhost`
- 局域网其他设备：地址填服务器电脑的本机 IP（`ipconfig` 查看）

---

## 功能一览

| 功能 | 说明 |
|------|------|
| 多人聊天 | 支持任意数量客户端同时在线 |
| 实时消息 | 消息带时间戳，自己/他人颜色区分 |
| 上线/下线通知 | 有人加入或离开自动广播 |
| 在线用户列表 | 右侧实时显示，自己标注"(我)" |
| 表情支持 | 点击 😊 按钮打开表情面板（30 个） |
| 回车发送 | 输入框按回车等效点击发送 |
| 优雅退出 | 关闭窗口有确认弹窗，服务器收到通知 |

---

## 技术细节

| 项目 | 技术选型 |
|------|---------|
| 网络层 | `java.net.Socket` + `ObjectInputStream/ObjectOutputStream` |
| 消息协议 | `Serializable` 对象序列化传输（Message 类） |
| 多线程 | 每个客户端独立线程；接收线程 daemon 化 |
| GUI | Swing（BorderLayout + JSplitPane + JTextPane + JList） |
| 线程安全 | `CopyOnWriteArrayList` 管理连接；消息发送 `synchronized` |
| 防缓存 | `out.reset()` 防止对象图缓存导致旧数据 |

---

## 常见问题

**Q: 编译报错 "找不到符号"？**
A: 确认 JDK（不是 JRE），`javac -version` 应有输出。

**Q: 连接服务器失败？**
A: 确认服务器已先启动，防火墙是否阻止 8888 端口。

**Q: 中文乱码？**
A: bat 脚本已加 `chcp 65001`，若仍乱码，可在 IDEA/VSCode 中直接运行。

**Q: 如何在局域网使用？**
A: 服务器电脑运行 `ChatServer`，客户端填入服务器的局域网 IP。
