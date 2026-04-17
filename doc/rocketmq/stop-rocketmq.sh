#!/bin/bash

# 获取当前脚本所在目录作为 ROCKETMQ_HOME
ROCKETMQ_HOME=$(cd "$(dirname "$0")"; pwd)
export ROCKETMQ_HOME

echo "RocketMQ Home is: $ROCKETMQ_HOME"

# 1. 停止 Broker
echo "Stopping Broker..."
sh $ROCKETMQ_HOME/bin/mqshutdown broker
# 等待 Broker 停止
sleep 2

# 2. 停止 NameServer
echo "Stopping NameServer..."
sh $ROCKETMQ_HOME/bin/mqshutdown namesrv

echo "RocketMQ Stopped Successfully!"