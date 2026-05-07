### 💻 场景一：搭建单机 RocketMQ

单机模式非常适合本地开发、功能测试或学习使用。

#### 1. 环境准备
*   **JDK**: 确保已安装 JDK 1.8 或更高版本。
*   **操作系统**: Linux 或 macOS 环境（Windows 环境下需要额外配置，不推荐）。

#### 2. 下载与安装
1.  从 Apache RocketMQ 官网下载最新的二进制包（例如 5.3.0 版本）。
    ```bash
    wget https://archive.apache.org/dist/rocketmq/5.3.0/rocketmq-all-5.3.0-bin-release.zip
    ```
2.  解压安装包并进入目录。
    ```bash
    unzip rocketmq-all-5.3.0-bin-release.zip
    cd rocketmq-all-5.3.0-bin-release
    ```
3.  （可选）为了方便后续操作，可以设置环境变量。
    ```bash
    export ROCKETMQ_HOME=/path/to/rocketmq-all-5.3.0-bin-release
    export PATH=$PATH:$ROCKETMQ_HOME/bin
    ```

#### 3. 调整内存配置
默认的 JVM 内存配置较大，对于开发环境可能会内存不足，建议调小。
*   **修改 NameServer 内存**：编辑 `bin/runserver.sh` 文件。
    ```bash
    # 将默认的 4g 修改为 1g
    JAVA_OPT="${JAVA_OPT} -server -Xms1g -Xmx1g -Xmn512m"
    ```
*   **修改 Broker 内存**：编辑 `bin/runbroker.sh` 文件。
    ```bash
    # 将默认的 8g 修改为 2g
    JAVA_OPT="${JAVA_OPT} -server -Xms2g -Xmx2g"
    ```

#### 4. 启动服务

##### 4.1 手动启动

启动顺序是先启动 NameServer，再启动 Broker。
1.  **启动 NameServer**
    ```bash
    nohup sh bin/mqnamesrv &
    ```
2.  **设置 NameServer 地址**
    ```bash
    export NAMESRV_ADDR=localhost:9876
    ```
3.  **启动 Broker**
    ```bash
    nohup sh bin/mqbroker -n localhost:9876 &
    ```

##### 4.2 脚本启动

**Linux脚本启动**

```bash
chmod +x start-rocketmq.sh
./start-rocketmq.sh
```

**Windows脚本启动**

```bash
# 双击 start-rocketmq.bat 或在命令行运行
start-rocketmq.bat
```

#### 5. 验证安装
可以使用自带的命令行工具发送和消费消息来验证服务是否正常。
```bash
# 发送测试消息
sh bin/tools.sh org.apache.rocketmq.example.quickstart.Producer

# 消费测试消息
sh bin/tools.sh org.apache.rocketmq.example.quickstart.Consumer
```

---

### 🖥️ 场景二：搭建集群 RocketMQ

生产环境强烈建议使用集群模式，以保证高可用性和数据可靠性。这里介绍一种高可用的 **3节点 Dledger Raft 集群** 模式，它能实现自动主从切换和数据强一致。

#### 1. 机器规划
准备三台服务器，每台服务器同时部署 NameServer 和 Broker 角色。

| 机器IP | 部署角色 | 核心端口 |
| :--- | :--- | :--- |
| 192.168.1.101 | NameServer, Broker | 9876, 10911, 10909, 40911 |
| 192.168.1.102 | NameServer, Broker | 9876, 10911, 10909, 40911 |
| 192.168.1.103 | NameServer, Broker | 9876, 10911, 10909, 40911 |

#### 2. 环境准备与系统优化（所有机器执行）
*   **软件要求**: 安装 JDK 17 和 Apache RocketMQ 5.4.0。
*   **操作系统优化**:
    ```bash
    # 1. 关闭防火墙和SELinux
    systemctl stop firewalld && systemctl disable firewalld
    setenforce 0 && sed -i 's/^SELINUX=.*/SELINUX=disabled/' /etc/selinux/config

    # 2. 优化文件句柄数
    echo "* soft nofile 655350" >> /etc/security/limits.conf
    echo "* hard nofile 655350" >> /etc/security/limits.conf
    echo "* soft nproc 655350" >> /etc/security/limits.conf
    echo "* hard nproc 655350" >> /etc/security/limits.conf

    # 3. 虚拟内存优化，关闭swap分区
    echo "vm.swappiness=0" >> /etc/sysctl.conf
    sysctl -p

    # 4. 关闭透明大页
    echo never > /sys/kernel/mm/transparent_hugepage/enabled
    echo never > /sys/kernel/mm/transparent_hugepage/defrag
    ```

#### 3. 集群部署步骤
1.  **启动所有 NameServer**
    在三台机器上分别执行，启动 NameServer。
    ```bash
    nohup sh bin/mqnamesrv &
    ```

2.  **配置并启动所有 Broker**
    在三台机器上创建并编辑 Broker 配置文件（例如 `conf/dledger/cluster-a/broker-a.properties`），配置内容如下：
    ```properties
    # 集群名称
    brokerClusterName=DefaultCluster
    # Broker名称，三台机器分别设置为 broker-a, broker-b, broker-c
    brokerName=broker-a
    # 0表示Master
    brokerId=0
    # NameServer地址，分号分割
    namesrvAddr=192.168.1.101:9876;192.168.1.102:9876;192.168.1.103:9876
    # 开启Dledger模式
    enableDLedger=true
    # Dledger组名，三台机器保持一致
    dLegerGroup=RaftGroup0
    # Dledger节点列表，格式为 nodeId:ip:port
    dLegerPeers=n0-192.168.1.101:40911;n1-192.168.1.102:40911;n2-192.168.1.103:40911
    # 当前节点的ID，三台机器分别设置为 n0, n1, n2
    dLegerSelfId=n0
    # 建议关闭自动创建Topic
    autoCreateTopicEnable=false
    # 建议关闭自动创建订阅组
    autoCreateSubscriptionGroup=false
    ```
    配置文件准备好后，在三台机器上分别启动 Broker。
    ```bash
    nohup sh bin/mqbroker -c conf/dledger/cluster-a/broker-a.properties &
    ```

#### 4. 其他集群模式选择
除了上述的 Dledger Raft 模式，RocketMQ 还支持其他集群模式，您可以根据业务对性能和数据一致性的要求进行选择：

| 集群模式 | 核心优势 | 核心劣势 | 适用场景 |
| :--- | :--- | :--- | :--- |
| **多主多从（异步复制）** | 性能极高，吞吐量优秀 | Master宕机时可能丢失少量消息 | 绝大多数互联网核心业务，允许极少量消息丢失 |
| **多主多从（同步复制）** | 数据零丢失，金融级可靠性 | 发送延迟更高，吞吐量有损耗 | 金融、支付、交易等对数据可靠性要求极高的场景 |
| **Dledger Raft模式** | 自动主从切换，数据强一致 | 部署复杂度略高，性能略有损耗 | 核心业务，要求故障自动转移、服务零中断 |

---

### 🐳 场景三：单机 Docker 模拟集群

**答案是肯定的。** 即使只有一台机器，你完全可以使用 Docker 来模拟一个完整的 RocketMQ 集群（例如 1 个 NameServer + 2 个 Broker 主从，或者 2 主 2 从）。

这是开发环境测试高可用（HA）和故障转移的最佳方式。

我们使用 `docker-compose` 来快速部署一个 **1 NameServer + 2 Brokers (Master/Slave)** 的伪集群。

#### 1. 准备工作
确保你的机器安装了 Docker 和 Docker Compose。

#### 2. 编写配置文件
创建一个文件夹 `rocketmq-cluster`，并在其中创建 `docker-compose.yml` 和 `conf` 目录。

**目录结构：**
```text
rocketmq-cluster/
├── docker-compose.yml
└── conf/
    ├── broker-a.conf
    └── broker-b.conf
```

**配置文件内容 (`conf/broker-a.conf`):**
```properties
brokerClusterName=DefaultCluster
brokerName=broker-a
brokerId=0
# 监听端口
listenPort=10911
# 开启DLedger或主从复制，这里演示传统主从
brokerRole=SYNC_MASTER
# 这里填写 Docker 内部 NameServer 的服务名
namesrvAddr=namesrv:9876
```

**配置文件内容 (`conf/broker-b.conf`):**
```properties
brokerClusterName=DefaultCluster
brokerName=broker-b
brokerId=0
listenPort=20911
brokerRole=SYNC_MASTER
namesrvAddr=namesrv:9876
```

#### 3. 编写 Docker Compose 脚本
在 `docker-compose.yml` 中定义服务。我们将使用 Host 网络模式或者自定义 Bridge 网络，这里为了端口映射清晰，使用 Bridge 网络并映射端口。

```yaml
version: '3'
services:
  # 1. NameServer
  namesrv:
    image: apache/rocketmq:5.3.2
    container_name: rmq-namesrv
    ports:
      - "9876:9876"
    command: sh mqnamesrv
    networks:
      - rocketmq-net

  # 2. Broker A (Master)
  broker-a:
    image: apache/rocketmq:5.3.2
    container_name: rmq-broker-a
    ports:
      - "10911:10911" # Broker Port
      - "10909:10909" # FastBroker Port
    environment:
      - NAMESRV_ADDR=namesrv:9876
    volumes:
      - ./conf/broker-a.conf:/home/rocketmq/rocketmq-5.3.2/conf/broker.conf
      - ./data/broker-a/store:/home/rocketmq/store
      - ./data/broker-a/logs:/home/rocketmq/logs
    depends_on:
      - namesrv
    command: sh mqbroker -c /home/rocketmq/rocketmq-5.3.2/conf/broker.conf
    networks:
      - rocketmq-net

  # 3. Broker B (Slave/Another Master)
  broker-b:
    image: apache/rocketmq:5.3.2
    container_name: rmq-broker-b
    ports:
      - "20911:20911"
      - "20909:20909"
    environment:
      - NAMESRV_ADDR=namesrv:9876
    volumes:
      - ./conf/broker-b.conf:/home/rocketmq/rocketmq-5.3.2/conf/broker.conf
      - ./data/broker-b/store:/home/rocketmq/store
      - ./data/broker-b/logs:/home/rocketmq/logs
    depends_on:
      - namesrv
    command: sh mqbroker -c /home/rocketmq/rocketmq-5.3.2/conf/broker.conf
    networks:
      - rocketmq-net

  # 4. Dashboard (可选，方便查看集群状态)
  dashboard:
    image: apacherocketmq/rocketmq-dashboard:latest
    container_name: rmq-dashboard
    ports:
      - "8080:8080"
    environment:
      - JAVA_OPTS=-Drocketmq.namesrv.addr=namesrv:9876
    depends_on:
      - namesrv
    networks:
      - rocketmq-net

networks:
  rocketmq-net:
    driver: bridge
```

#### 4. 启动集群
在 `rocketmq-cluster` 目录下执行：

```bash
docker-compose up -d
```

#### 5. 验证集群
启动后，你可以进入 Broker 容器查看集群状态：

```bash
# 进入 broker-a 容器
docker exec -it rmq-broker-a bash

# 执行集群列表命令
sh bin/mqadmin clusterList -n namesrv:9876
```

如果看到输出中包含 `broker-a` 和 `broker-b` 两行信息，说明你的单机多节点集群已经搭建成功！
