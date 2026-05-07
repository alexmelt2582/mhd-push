#!/bin/bash

# 获取当前脚本所在目录作为 ROCKETMQ_HOME
ROCKETMQ_HOME=$(cd "$(dirname "$0")"; pwd)
export ROCKETMQ_HOME

echo "RocketMQ Home is: $ROCKETMQ_HOME"

# 1. 启动 NameServer
echo "Starting NameServer..."
nohup sh $ROCKETMQ_HOME/bin/mqnamesrv > $ROCKETMQ_HOME/logs/namesrv.log 2>&1 &
echo "NameServer started."

# 等待 NameServer 启动
sleep 3

# 2. 启动 Broker
# 注意：这里指定了 NameServer 地址为 localhost:9876
# 如果是远程访问，建议将 broker.conf 中的 brokerIP1 配置为本机局域网 IP
echo "Starting Broker..."
nohup sh $ROCKETMQ_HOME/bin/mqbroker -n localhost:9876 -c $ROCKETMQ_HOME/conf/broker.conf > $ROCKETMQ_HOME/logs/broker.log 2>&1 &
echo "Broker started."

echo "RocketMQ Single Node Started Successfully!"