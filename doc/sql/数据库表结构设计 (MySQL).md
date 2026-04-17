1. 用户基础信息表 (user_info)
   存储注册用户的基本画像及绑定关系

```sql
CREATE TABLE `user_info` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `open_id` VARCHAR(64) NOT NULL COMMENT '微信公众号 OpenID (唯一标识)',
  `union_id` VARCHAR(64) DEFAULT NULL COMMENT '微信 UnionID (多应用互通)',
  `nick_name` VARCHAR(64) DEFAULT '' COMMENT '昵称',
  `avatar_url` VARCHAR(255) DEFAULT '' COMMENT '头像',
  `gender` TINYINT(4) DEFAULT '0' COMMENT '性别 0:未知 1:男 2:女',
  `is_follow` TINYINT(4) DEFAULT '0' COMMENT '是否关注公众号 0:否 1:是',
  `phone` VARCHAR(20) DEFAULT '' COMMENT '绑定手机号 (加密存储)',
  `email` VARCHAR(100) DEFAULT '' COMMENT '绑定邮箱',
  `token` VARCHAR(64) NOT NULL COMMENT '用户 API Token (登录/发送凭证)',
  `integral` BIGINT(20) DEFAULT '0' COMMENT '剩余积分 (用于短信/语音扣费)',
  `is_vip` TINYINT(4) DEFAULT '0' COMMENT '是否会员 (影响 to 参数人数限制)',
  `created` BIGINT(20) NOT NULL,
  `updated` BIGINT(20) NOT NULL,
  `is_deleted` TINYINT(4) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_open_id` (`open_id`),
  UNIQUE KEY `uk_token` (`token`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基础信息表';
```

2. 群组信息表 (topic_group)
   对应“一对多消息”的群组实体

```sql
CREATE TABLE `topic_group` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `group_code` VARCHAR(32) NOT NULL COMMENT '群组编码 (topic 参数值，唯一)',
  `group_name` VARCHAR(100) NOT NULL COMMENT '群组名称',
  `description` VARCHAR(500) DEFAULT '' COMMENT '群组描述',
  `owner_user_id` BIGINT(20) NOT NULL COMMENT '创建者用户 ID',
  `qr_code_path` VARCHAR(255) DEFAULT '' COMMENT '二维码图片路径',
  `qr_expire_days` INT(10) DEFAULT '0' COMMENT '二维码有效期天数',
  `qr_scan_limit` INT(10) DEFAULT '0' COMMENT '二维码最大扫码次数',
  `current_scan_count` INT(10) DEFAULT '0' COMMENT '当前已扫码次数',
  `member_count` INT(10) DEFAULT '0' COMMENT '当前成员数量',
  `created` BIGINT(20) NOT NULL,
  `updated` BIGINT(20) NOT NULL,
  `is_deleted` TINYINT(4) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_code` (`group_code`),
  KEY `idx_owner` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群组信息表';
```

3. 群组成员关系表 (topic_user_rel)
   记录谁在哪个群里，以及用户的渠道绑定状态快照

```sql
CREATE TABLE `topic_user_rel` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `group_id` BIGINT(20) NOT NULL,
  `user_id` BIGINT(20) NOT NULL,
  `join_time` BIGINT(20) NOT NULL,
  `status` TINYINT(4) DEFAULT '1' COMMENT '状态 1:正常 0:退出/被踢',
  -- 冗余字段：加入时的渠道绑定状态，避免发送时频繁查 user_info
  `snapshot_phone` VARCHAR(20) DEFAULT '', 
  `snapshot_email` VARCHAR(100) DEFAULT '',
  `snapshot_openid` VARCHAR(64) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
  KEY `idx_group` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群组成员关系表';
```

4. 好友关系表 (friend_rel)
   对应“好友消息”，解决灵活一对一发送问题

```sql
CREATE TABLE `friend_rel` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `owner_user_id` BIGINT(20) NOT NULL COMMENT '好友拥有者 (发送方)',
  `friend_user_id` BIGINT(20) NOT NULL COMMENT '好友目标用户 (接收方)',
  `friend_token` VARCHAR(64) NOT NULL COMMENT '好友令牌 (to 参数值，由拥有者生成)',
  `remark` VARCHAR(64) DEFAULT '' COMMENT '备注名',
  `qr_code_param` VARCHAR(100) DEFAULT '' COMMENT '生成该好友关系的二维码自定义参数',
  `created` BIGINT(20) NOT NULL,
  `is_deleted` TINYINT(4) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_friend_token` (`friend_token`),
  UNIQUE KEY `uk_owner_friend` (`owner_user_id`, `friend_user_id`),
  KEY `idx_owner` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系表';
```

5. 消息发送流水表 (message_record)
   全链路核心，支持异步回调查询

```sql
CREATE TABLE `message_record` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `short_code` VARCHAR(32) NOT NULL COMMENT '短流水号 (返回给用户的 data)',
  `user_id` BIGINT(20) NOT NULL COMMENT '发起用户 ID',
  `message_type` TINYINT(4) NOT NULL COMMENT '10:给自己 20:给好友 30:给群组',
  `target_ids` VARCHAR(2000) DEFAULT '' COMMENT '目标对象 ID 集合 (好友 token 串 或 group_code)',
  `channel` VARCHAR(20) NOT NULL COMMENT '发送渠道 wechat/sms/mail...',
  `template_type` VARCHAR(20) DEFAULT 'html' COMMENT '模板类型',
  `title` VARCHAR(200) DEFAULT '',
  `content` TEXT NOT NULL,
  `send_status` TINYINT(4) DEFAULT '0' COMMENT '0:未发送 1:发送中 2:部分成功 3:全部成功 4:全部失败',
  `total_count` INT(10) DEFAULT '1' COMMENT '计划发送总人数',
  `success_count` INT(10) DEFAULT '0',
  `fail_count` INT(10) DEFAULT '0',
  `callback_url` VARCHAR(255) DEFAULT '' COMMENT '回调地址',
  `ext_params` JSON DEFAULT NULL COMMENT '原始请求参数备份 (option, pre, timestamp 等)',
  `error_msg` VARCHAR(500) DEFAULT '',
  `created` BIGINT(20) NOT NULL,
  `updated` BIGINT(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_code` (`short_code`),
  KEY `idx_user_created` (`user_id`, `created`),
  KEY `idx_status` (`send_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息发送流水表';
```

6. 消息子任务详情表 (message_task_detail)
   如果是群发消息，记录每个具体用户的发送结果，用于精细化回调

```sql
CREATE TABLE `message_task_detail` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `record_id` BIGINT(20) NOT NULL COMMENT '关联 message_record.id',
  `receiver_user_id` BIGINT(20) DEFAULT '0' COMMENT '接收者 ID (群组成员或好友)',
  `receiver_identity` VARCHAR(100) NOT NULL COMMENT '接收标识 (手机号/邮箱/openid)',
  `channel` VARCHAR(20) NOT NULL,
  `status` TINYINT(4) DEFAULT '0' COMMENT '0:待发送 1:成功 2:失败',
  `error_msg` VARCHAR(255) DEFAULT '',
  `send_time` BIGINT(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_record` (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息子任务详情表';
```


三、功能实现流程清单

发送消息核心流程 (API -> MQ -> Consumer)

步骤 1.1：接入层 (Controller)
接收请求：解析 token, content, topic, to, channel 等参数。
基础校验：
token 有效性校验 (查 user_info)。
timestamp 防重放校验。
content 非空校验。
权限/限流校验：
若 channel=sms/voice，检查用户积分是否充足。
若 to 参数存在多人，检查是否为会员 (VIP 100 人，普通 10 人)。
生成流水号：生成 short_code (如 UUID 缩短)，插入 message_record 表，状态=0 (未发送)。
发送 MQ：构造 SendMessageEvent (包含 recordId, 原始参数)，发送到 RocketMQ Topic austin-send-request。
返回响应：立即返回 {code: 200, msg: "请求成功", data: "short_code"}。

步骤 1.2：消息解析与路由 (Consumer - 责任链节点 1)
拉取消息。
解析接收人列表 (核心逻辑)：
场景 A：给自己发送 (topic 空，to 空)
接收人 = token 对应的用户。
根据 channel 获取该用户的绑定信息 (如 wechat -> open_id, sms -> phone)。若未绑定，标记失败。
场景 B：好友消息 (to 有值)
解析 to (逗号分隔)。
查 friend_rel 表，根据 friend_token 找到真实的 friend_user_id。
获取好友的绑定信息。若好友未绑定对应渠道，标记失败。
场景 C：群组消息 (topic 有值)
查 topic_group 获取 group_id。
查 topic_user_rel 获取所有有效成员。
渠道过滤：
若 channel=sms，只保留 snapshot_phone 不为空的成员。
若 channel=mail，只保留 snapshot_email 不为空的成员。
若 channel=wechat，只保留 snapshot_openid 不为空且 is_follow=1 的成员。
生成子任务：批量插入 message_task_detail 表，状态=0。更新 message_record 的 total_count。

步骤 1.3：内容渲染与预处理 (Consumer - 责任链节点 2)
Pre 处理：若 pre 参数存在，执行用户自定义脚本/逻辑修改 content。
模板渲染：根据 template (html/markdown/json) 包装 content 和 title。
敏感词过滤：调用风控服务。

步骤 1.4：渠道分发 (Consumer - 策略模式)
根据 channel 选择策略 (WechatStrategy, SmsStrategy, WebhookStrategy...)。
并发发送：使用线程池并发调用第三方 API (注意控制 QPS，参考 channel_flow_config)。
结果回写：
成功：更新 message_task_detail 状态=1。
失败：更新状态=2，记录 error_msg。
扣减积分：若涉及收费渠道，异步扣减 user_info.integral。

步骤 1.5：聚合与回调 (Consumer - 收尾节点)
统计状态：检查该 record_id 下所有子任务状态。
全成功 -> message_record.send_status = 3
全失败 -> message_record.send_status = 4
部分成功 -> message_record.send_status = 2
触发回调：
若 callback_url 不为空，构造 JSON (event: message_complate, sendStatus, shortCode)。
使用 HTTP Client 异步 POST 请求用户回调地址 (需支持重试)。
ACK 消息。

群组管理流程

创建群组：
校验群组名称、描述。
生成唯一 group_code。
生成二维码图片 (调用微信 API 或本地生成)，设置过期时间和次数限制。
存入 topic_group。
订阅群组 (扫码)：
用户扫码 -> 回调微信服务器 -> Austin 接收事件。
校验二维码有效性 (过期/次数)。
获取用户 OpenID，查 user_info (不存在则注册)。
插入 topic_user_rel (记录当前手机/邮箱快照)。
触发回调：若用户配置了回调地址，推送 event: add_topic_user。
退出/移除：
更新 topic_user_rel.status = 0。

好友管理流程

生成好友令牌：
用户在个人中心生成 friend_token (关联 owner_user_id)。
生成带参数的二维码 (qr_code_param)。
添加好友 (扫码)：
扫描者 (B) 扫码 -> 解析参数 -> 创建 friend_rel 记录 (Owner=A, Friend=B, token=xxx)。
触发回调：推送 event: add_friend 给 A (Owner)。
发送好友消息：
见“发送消息核心流程”场景 B。

四、关键技术难点与解决方案
难点   解决方案
高并发下的群发性能   1. 批量解析：群组解析接收人时，一次性加载所有成员到内存，避免 N+1 查询。2. 并发发送：使用 CompletableFuture 或 自定义线程池，按渠道并发调用第三方 API。3. 削峰填谷：RocketMQ 积压时，消费者自动降速，保护第三方接口不被打挂。

渠道绑定状态不一致   在 topic_user_rel 中冗余快照 (snapshot_phone 等)。用户加入群组时固化一次状态。发送时直接读快照，避免发送瞬间用户解绑导致的数据不一致，也减少联表查询。

收费渠道的原子性   预扣费 + 最终一致性。1. 发送前检查并冻结积分 (Redis)。2. 发送成功后扣除 DB 积分。3. 发送失败/重试超限后解冻积分。4. 定时任务对账冻结超时记录。

回调可靠性   1. 本地重试：回调失败存入“回调重试表”，定时任务指数退避重试。2. 幂等设计：回调 URL 接收端需自行保证幂等 (通过 shortCode 去重)。

Webhook 的通用性   设计 WebhookStrategy，将 option 参数作为 URL，将 content 组装成 JSON/XML/Form 表单，利用 RestTemplate 或 OkHttp 通用发送。支持配置不同的 Content-Type。

五、项目实施阶段规划

第一阶段：基础骨架与单点发送 (MVP)
完成数据库建表 (user_info, message_record 等)。
实现 SendMessageRequest 校验器链。
实现 给自己发送 (wechat, webhook) 的全流程 (API->MQ->DB->Send)。
实现基础的同步/异步回调机制。

第二阶段：关系链与群发功能
完成群组 (topic_group, rel) 和 好友 (friend_rel) 的 CRUD 及扫码逻辑。
实现 群组消息 解析逻辑 (过滤未绑定用户)。
实现 好友消息 解析逻辑 (token 转 userId)。
引入 message_task_detail 支持批量发送状态追踪。

第三阶段：高级特性与商业化
集成 SMS/Voice 收费渠道，实现积分扣减逻辑。
实现 pre 预处理脚本引擎 (可考虑 Groovy 或 Aviator)。
完善监控大盘 (发送量、成功率、积分消耗)。
压力测试与调优 (线程池参数、MQ 堆积处理)。

六、给开发者的特别提示

关于 to 参数的解析：
不要直接在 SQL 中使用 FIND_IN_SET。建议在 Java 代码中将 to 字符串 split 成 List，然后分批查询 friend_rel 表。
关于 timestamp：
这是一个安全字段。务必在 Controller 层第一时间校验 System.currentTimeMillis() > request.timestamp，防止旧请求重放消耗积分或骚扰用户。
关于 channel=webhook：
Webhook 是最灵活的，但也最容易出错。建议增加“连通性测试”功能，并在发送失败时详细记录 HTTP 响应码和 Body，方便用户排查。
数据归档：
message_record 和 message_task_detail 增长极快。务必设计好按月分表或定期归档到 HBase/ES 的策略，保持 MySQL 主表轻量。

这份建议书涵盖了你描述的所有功能点，并补充了企业级应用必须的稳定性设计。按照此方案执行，你的项目将从一个“小工具”进化为一个真正的“消息推送平台”。