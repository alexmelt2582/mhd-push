@echo off
:: 获取当前脚本所在目录
set ROCKETMQ_HOME=%~dp0
:: 去掉路径末尾可能的反斜杠
set ROCKETMQ_HOME=%ROCKETMQ_HOME:~0,-1%

echo RocketMQ Home is: %ROCKETMQ_HOME%

:: 1. 启动 NameServer
echo Starting NameServer...
start /min cmd /c "cd /d %ROCKETMQ_HOME% && bin\mqnamesrv.cmd"
echo NameServer started.

:: 等待 NameServer 启动
timeout /t 5 /nobreak >nul

:: 2. 启动 Broker
echo Starting Broker...
start /min cmd /c "cd /d %ROCKETMQ_HOME% && bin\mqbroker.cmd -n localhost:9876 -c conf\broker.conf"
echo Broker started.

echo RocketMQ Single Node Started Successfully!
pause