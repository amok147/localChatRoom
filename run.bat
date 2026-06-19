@echo off
chcp 65001 >nul
setlocal

set SRC=src
set OUT=out
set MAIN_SERVER=cn.amok147.chatroom.server.ChatServer
set MAIN_CLIENT=cn.amok147.chatroom.client.ChatClient

echo =======================================
echo   本地聊天室 - 编译 ^& 启动助手
echo =======================================
echo.
echo [1] 编译项目
echo [2] 启动 服务器
echo [3] 启动 客户端
echo [4] 编译 + 启动服务器（一键）
echo [5] 编译 + 启动客户端（一键）
echo [0] 退出
echo.
set /p choice=请输入选项: 

if "%choice%"=="1" goto compile
if "%choice%"=="2" goto server
if "%choice%"=="3" goto client
if "%choice%"=="4" goto compile_server
if "%choice%"=="5" goto compile_client
if "%choice%"=="0" exit /b 0

echo 无效选项，退出。
exit /b 1

:compile
echo.
echo [编译中...]
if not exist "%OUT%" mkdir "%OUT%"
javac -encoding UTF-8 -d "%OUT%" ^
  "%SRC%\cn\amok147\chatroom\common\MessageType.java" ^
  "%SRC%\cn\amok147\chatroom\common\Message.java" ^
  "%SRC%\cn\amok147\chatroom\server\ChatServer.java" ^
  "%SRC%\cn\amok147\chatroom\server\ClientHandler.java" ^
  "%SRC%\cn\amok147\chatroom\client\LoginDialog.java" ^
  "%SRC%\cn\amok147\chatroom\client\ChatFrame.java" ^
  "%SRC%\cn\amok147\chatroom\client\ChatClient.java"
if %errorlevel% equ 0 (
    echo [OK] 编译成功！class 文件输出至 %OUT%\
) else (
    echo [ERROR] 编译失败，请检查 Java 环境（需要 JDK 8+）
)
pause
exit /b

:server
echo.
echo [启动服务器] 端口 8888 ...
java -cp "%OUT%" %MAIN_SERVER%
pause
exit /b

:client
echo.
echo [启动客户端]...
java -cp "%OUT%" %MAIN_CLIENT%
pause
exit /b

:compile_server
call :compile_only
java -cp "%OUT%" %MAIN_SERVER%
pause
exit /b

:compile_client
call :compile_only
java -cp "%OUT%" %MAIN_CLIENT%
pause
exit /b

:compile_only
echo.
echo [编译中...]
if not exist "%OUT%" mkdir "%OUT%"
javac -encoding UTF-8 -d "%OUT%" ^
  "%SRC%\cn\amok147\chatroom\common\MessageType.java" ^
  "%SRC%\cn\amok147\chatroom\common\Message.java" ^
  "%SRC%\cn\amok147\chatroom\server\ChatServer.java" ^
  "%SRC%\cn\amok147\chatroom\server\ClientHandler.java" ^
  "%SRC%\cn\amok147\chatroom\client\LoginDialog.java" ^
  "%SRC%\cn\amok147\chatroom\client\ChatFrame.java" ^
  "%SRC%\cn\amok147\chatroom\client\ChatClient.java"
exit /b
