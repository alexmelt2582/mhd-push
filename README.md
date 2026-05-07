## 3. 目录树

推荐目录结构如下：

```text
mhd-msg-push/
├─ pom.xml
├─ libs/
│  ├─ pom.xml
│  ├─ push-common/
│  │  ├─ pom.xml
│  │  └─ src/main/java/com/mhd/push/common/
│  ├─ push-domain/
│  │  ├─ pom.xml
│  │  └─ src/main/java/com/mhd/push/domain/
│  │     ├─ model/
│  │     ├─ service/
│  │     ├─ repository/
│  │     ├─ event/
│  │     └─ enums/
│  ├─ push-infra/
│  │  ├─ pom.xml
│  │  ├─ src/main/java/com/mhd/push/infra/
│  │  │  ├─ config/
│  │  │  ├─ persistence/
│  │  │  │  ├─ entity/
│  │  │  │  ├─ mapper/
│  │  │  │  └─ repository/
│  │  │  ├─ mq/
│  │  │  ├─ redis/
│  │  │  ├─ http/
│  │  │  ├─ nacos/
│  │  │  └─ thirdparty/
│  │  └─ src/main/resources/
│  │     └─ mapper/
│  └─ push-engine/
│     ├─ pom.xml
│     └─ src/main/java/com/mhd/push/engine/
│        ├─ deduplication/
│        ├─ flowcontrol/
│        ├─ handler/
│        ├─ pending/
│        ├─ receipt/
│        ├─ dispatch/
│        └─ service/
├─ apps/
│  ├─ pom.xml
│  ├─ push-public-api-app/
│  │  ├─ pom.xml
│  │  ├─ src/main/java/com/mhd/push/publicapi/
│  │  │  ├─ controller/
│  │  │  ├─ application/
│  │  │  ├─ domain/
│  │  │  ├─ config/
│  │  │  ├─ interceptor/
│  │  │  ├─ exception/
│  │  │  └─ PublicApiApplication.java
│  │  └─ src/main/resources/
│  ├─ push-admin-api-app/
│  │  ├─ pom.xml
│  │  ├─ src/main/java/com/mhd/push/adminapi/
│  │  │  ├─ controller/
│  │  │  ├─ application/
│  │  │  ├─ security/
│  │  │  ├─ audit/
│  │  │  ├─ config/
│  │  │  └─ AdminApiApplication.java
│  │  └─ src/main/resources/
│  ├─ push-worker-app/
│  │  ├─ pom.xml
│  │  ├─ src/main/java/com/mhd/push/worker/
│  │  │  ├─ receiver/
│  │  │  ├─ config/
│  │  │  ├─ bootstrap/
│  │  │  └─ WorkerApplication.java
│  │  └─ src/main/resources/
│  └─ push-job-app/
│     ├─ pom.xml
│     ├─ src/main/java/com/mhd/push/job/
│     │  ├─ xxl/
│     │  ├─ task/
│     │  ├─ config/
│     │  └─ JobApplication.java
│     └─ src/main/resources/
├─ push-data-house/
│  ├─ src/main/java/
│  └─ src/main/resources/
├─ deploy/
│  ├─ docker/
│  ├─ k8s/
│  ├─ helm/
│  ├─ xxl-job/
│  └─ scripts/
├─ doc/
└─ scripts/
```

### 3.1.2 代码模块依赖矩阵

推荐依赖矩阵如下：

```text
push-common         -> 无内部模块依赖
push-domain         -> push-common
push-infra          -> push-domain, push-common
push-engine         -> push-domain, push-common, 视情况依赖 push-infra
push-public-api-app -> push-domain, push-common, push-infra
push-admin-api-app  -> push-domain, push-common, push-infra, 视情况少量依赖 push-engine
push-worker-app     -> push-engine, push-infra, push-domain, push-common
push-job-app        -> push-engine, push-infra, push-domain, push-common
push-data-house     -> push-domain, push-common, 视场景依赖 push-infra
```