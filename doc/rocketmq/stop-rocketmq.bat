@echo off
:: 获取当前脚本所在目录
set ROCKETMQ_HOME=%~dp0
:: 去掉路径末尾可能的反斜杠
set ROCKETMQ_HOME=%ROCKETMQ_HOME:~0,-1%

echo RocketMQ Home is: %ROCKETMQ_HOME%

:: 1. 停止 Broker
echo Stopping Broker...
call %ROCKETMQ_HOME%\bin\mqshutdown.cmd broker
timeout /t 3 /nobreak >nul

:: 2. 停止 NameServer
echo Stopping NameServer...
call %ROCKETMQ_HOME%\bin\mqshutdown.cmd namesrv

echo RocketMQ Stopped Successfully!
pause